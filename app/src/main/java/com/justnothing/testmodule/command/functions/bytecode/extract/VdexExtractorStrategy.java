package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.command.functions.bytecode.util.BundledToolProvider;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用 vendored 的 {@code vdexExtractor} 把 vdex 里的 dex 反 quicken（de-odex）。
 *
 * <p>这是"能拿到可信代码"的关键一步：ART 会把 dex 写进 vdex 时就地 quicken，
 * 此前的实测里，未还原的 dex 被 jadx 打开后会报 {@code UNKNOWN(0x52E5)} 这类保留 opcode。</p>
 *
 * <p><b>覆盖范围有限，必须允许回退。</b>上游停更于 2020 年，只支持到 vdex021
 * （Android 10/11 左右）。更高版本的 vdex 会失败（退出码非 0 或没有产出），
 * 此时 {@link DexExtractionManager} 会继续尝试后面的策略（比如 {@link RawDexSlicer}），
 * 而不是直接放弃。</p>
 */
public final class VdexExtractorStrategy implements DexExtractor {

    /** 与 assets/bin/&lt;abi&gt;/ 下的文件名、以及构建脚本保持一致。 */
    public static final String TOOL_NAME = "vdexExtractor";

    private static final Logger logger = Logger.getLoggerForName("VdexExtractorStrategy");

    /** 一个 vdex 的还原耗时取决于体积；给足上限，超时直接杀掉而不是无限等。 */
    private static final long TIMEOUT_MS = 180_000L;

    private static final long MAX_DEX_BYTES = 64L * 1024 * 1024;

    @Override
    public String name() {
        return "vdexExtractor（de-quicken）";
    }

    @Override
    public boolean canHandle(DexSource source) {
        DexSource.Kind kind = source.getKind();
        // odex 也交给它：上游支持直接把 odex（ELF/OAT 容器）喂进去，它会自己取出里面的 vdex。
        // 万一失败也不损失什么 —— 提取失败会回退到 RawDexSlicer。
        if ((kind != DexSource.Kind.VDEX && kind != DexSource.Kind.ODEX) || !source.exists()) {
            return false;
        }
        // 工具还没就绪（比如构建时没有 NDK、assets 里没有对应 ABI）时不要认领，
        // 留给后面的兜底策略。
        return BundledToolProvider.ensure(TOOL_NAME) != null;
    }

    @Override
    public List<Result> extract(DexSource source) throws IOException {
        File tool = BundledToolProvider.ensure(TOOL_NAME);
        if (tool == null) {
            return Collections.emptyList();
        }

        // 每次用独立的输出目录，避免上一次的产物混进来（否则会拿到过期的 dex）。
        File outDir = new File(FileDirectory.METHODS_DATA_DIR + "/vdex_out",
                sanitize(source.getFile().getName()));
        deleteRecursively(outDir);
        if (!outDir.mkdirs()) {
            logger.warn("无法创建输出目录: " + outDir);
            return Collections.emptyList();
        }

        if (!run(tool, source.getFile(), outDir)) {
            return Collections.emptyList();
        }

        File[] produced = outDir.listFiles((dir, n) -> n.endsWith(".dex"));
        if (produced == null || produced.length == 0) {
            logger.warn("vdexExtractor 没有产出 dex: " + source);
            return Collections.emptyList();
        }

        List<Result> results = new ArrayList<>();
        for (File dex : produced) {
            if (dex.length() > MAX_DEX_BYTES) {
                logger.warn("产物过大已跳过: " + dex.getName());
                continue;
            }
            results.add(new Result(
                    Files.readAllBytes(dex.toPath()),
                    DexTrust.DEQUICKENED,
                    "由 vdexExtractor 还原（" + dex.getName() + "）"));
        }
        logger.info("vdexExtractor 还原出 " + results.size() + " 个 dex: " + source);
        return results;
    }

    /** 跑一次 vdexExtractor。失败（超时 / 非 0 退出码）返回 false，由上层回退到别的策略。 */
    private boolean run(File tool, File vdex, File outDir) {
        Process process = null;
        try {
            process = new ProcessBuilder(
                    tool.getAbsolutePath(),
                    "-i", vdex.getAbsolutePath(),
                    "-o", outDir.getAbsolutePath(),
                    "-f",
                    "--ignore-crc-error")
                    .redirectErrorStream(true)
                    // 输出重定向到文件：既避免管道写满导致卡死，也留下可排查的现场
                    .redirectOutput(new File(outDir, TOOL_NAME + ".log"))
                    .start();

            if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                logger.warn("vdexExtractor 超时（" + TIMEOUT_MS + "ms），已终止");
                return false;
            }
            int code = process.exitValue();
            if (code != 0) {
                logger.warn("vdexExtractor 退出码 " + code
                        + "（该 vdex 版本可能不在它 2020 年的支持范围内），交给下一条策略");
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("等待 vdexExtractor 时被中断");
            return false;
        } catch (IOException e) {
            // 常见于 SELinux 拦住了 exec（例如在普通应用进程里跑 /data/local/tmp 下的文件）
            logger.error("无法启动 vdexExtractor: " + e.getMessage(), e);
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            logger.warn("删除失败（可能被占用）: " + file.getAbsolutePath());
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
