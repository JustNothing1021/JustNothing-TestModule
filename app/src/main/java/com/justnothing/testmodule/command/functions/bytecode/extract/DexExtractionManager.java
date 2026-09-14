package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.utils.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 按优先级挑选提取策略，并把结果（含可信度）汇总给调用方。
 *
 * <p>存在的意义：不同来源、不同 Android 版本能走的路不一样，而且<b>能走的路会随时间失效</b>
 * （vendored 的 vdexExtractor 停更于 2020 年，只覆盖到 vdex021）。把策略做成可插拔的列表，
 * 将来某个格式不支持时，只需要加一条策略或调低它的优先级，调用方不用改。</p>
 *
 * <p>降级原则：某条策略失败时<b>不抛异常</b>，而是记录原因并返回空列表 ——
 * 让上层能给出"这个来源暂时取不出 dex，原因是 xxx"这种可读的提示，
 * 而不是一个栈。</p>
 */
public final class DexExtractionManager {

    private static final Logger logger = Logger.getLoggerForName("DexExtractionManager");

    /**
     * 策略列表，<b>顺序即优先级</b>：
     * <ol>
     *   <li>{@link ApkDexExtractor} —— APK/JAR 里的 {@code classes*.dex}。最可靠：
     *       ART 永远不会动 APK 里的原件，任何 Android 版本都成立 → {@link DexTrust#ORIGINAL}</li>
     *   <li>{@link VdexExtractorStrategy} —— 调 vendored 的 vdexExtractor 把 vdex/odex 里的
     *       dex de-quicken → {@link DexTrust#DEQUICKENED}。它停更于 2020 年、只覆盖到 vdex021，
     *       高版本会失败，于是回退到第 3 条</li>
     *   <li>{@link RawDexSlicer} —— 兜底：按 {@code dex\n} 魔数把 dex 切出来，再用头部
     *       checksum 判断有没有被改写。没被改写标 {@link DexTrust#ORIGINAL}
     *       （确实存在这种设备/组件），被改写标 {@link DexTrust#QUICKENED} 并提示"只能读结构"</li>
     * </ol>
     */
    private static final List<DexExtractor> EXTRACTORS = Arrays.asList(
            new ApkDexExtractor(),
            // 顺序即优先级。vdex 先走 vdexExtractor（能 de-quicken → 代码可信）；
            // 它只覆盖到 vdex021，高版本会失败，于是回退到 RawDexSlicer：
            // 至少把 dex 切出来，并如实标注"有没有被改写"。
            new VdexExtractorStrategy(),
            new RawDexSlicer()
    );

    private DexExtractionManager() {
    }

    /** 找出第一条认领该来源的策略；没有则返回 null。 */
    public static DexExtractor findExtractor(DexSource source) {
        if (source == null) {
            return null;
        }
        for (DexExtractor extractor : EXTRACTORS) {
            try {
                if (extractor.canHandle(source)) {
                    return extractor;
                }
            } catch (RuntimeException e) {
                logger.warn("策略 " + extractor.name() + " 判定来源时出错: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 提取 dex。
     *
     * @return 提取结果；没有可用策略或提取失败时返回<b>空列表</b>（不返回 null，调用方少一处判空）
     */
    public static List<DexExtractor.Result> extract(DexSource source) {
        if (source == null) {
            return Collections.emptyList();
        }

        // 注意这里是"逐条试、谁先出结果用谁的"，而不是"第一条认领的策略赢"。
        // 两条的区别在高版本 vdex 上很关键：vdexExtractor 认领了但会失败，
        // 必须能回退到 RawDexSlicer，否则就变成"什么也拿不到"。
        boolean anyClaimed = false;
        for (DexExtractor extractor : EXTRACTORS) {
            try {
                if (!extractor.canHandle(source)) {
                    continue;
                }
            } catch (RuntimeException e) {
                logger.warn("策略 " + extractor.name() + " 判定来源时出错: " + e.getMessage());
                continue;
            }

            anyClaimed = true;
            try {
                List<DexExtractor.Result> results = extractor.extract(source);
                if (results != null && !results.isEmpty()) {
                    logger.info("用 " + extractor.name() + " 从 " + source
                            + " 取出 " + results.size() + " 个 dex");
                    return results;
                }
                logger.warn("策略 " + extractor.name() + " 没能从 " + source + " 取出 dex，改用下一条");
            } catch (IOException | RuntimeException e) {
                logger.error("策略 " + extractor.name() + " 提取失败: " + source, e);
                logger.warn("改用下一条策略重试");
            }
        }

        logger.warn(anyClaimed
                ? "所有认领该来源的策略都没能取出 dex: " + source
                : "没有策略能处理这个来源: " + source);
        return Collections.emptyList();
    }
}
