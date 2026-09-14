package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.utils.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 从 APK / jar 里取出 classes*.dex。
 *
 * <p><b>这是最可靠的一条路。</b>APK 里的 dex 是 dx/d8 编译出来的原件；ART 只会把
 * "改写过的副本"写进 vdex，<b>永远不会动 APK 里的那一份</b>。所以这里的产物可以直接标
 * {@link DexTrust#ORIGINAL}：不需要 de-quicken、不依赖任何外部二进制，任何 Android 版本都成立。</p>
 *
 * <p>多 dex 用 {@code classes\d*\.dex} 匹配，按文件名排序以保证顺序稳定
 * （classes.dex → classes2.dex → classes3.dex …），避免每次跑出来的顺序都不一样。</p>
 */
public final class ApkDexExtractor implements DexExtractor {

    private static final Logger logger = Logger.getLoggerForName("ApkDexExtractor");

    private static final Pattern DEX_ENTRY = Pattern.compile("classes\\d*\\.dex");

    /** dex 的魔数，取文件时顺手校验一下，避免把别的条目当成 dex。 */
    private static final byte[] DEX_MAGIC = {'d', 'e', 'x', '\n'};

    /** 单个 dex 的大小上限（防呆；正常 dex 不会超过这个量级）。 */
    private static final long MAX_DEX_BYTES = 64L * 1024 * 1024;

    @Override
    public String name() {
        return "APK/JAR 内的 classes*.dex";
    }

    @Override
    public boolean canHandle(DexSource source) {
        DexSource.Kind kind = source.getKind();
        return (kind == DexSource.Kind.APK || kind == DexSource.Kind.JAR) && source.exists();
    }

    @Override
    public List<Result> extract(DexSource source) throws IOException {
        List<Result> results = new ArrayList<>();
        File file = source.getFile();

        try (ZipFile zipFile = new ZipFile(file)) {
            List<ZipEntry> entries = new ArrayList<>();
            Enumeration<? extends ZipEntry> all = zipFile.entries();
            while (all.hasMoreElements()) {
                ZipEntry entry = all.nextElement();
                if (!entry.isDirectory() && DEX_ENTRY.matcher(entry.getName()).matches()) {
                    entries.add(entry);
                }
            }
            // 按文件名排序：classes.dex → classes2.dex → classes3.dex …，保证多 dex 顺序稳定
            Collections.sort(entries, Comparator.comparing(ZipEntry::getName));

            if (entries.isEmpty()) {
                logger.warn("APK/JAR 里没有 classes*.dex: " + file.getAbsolutePath());
                return results;
            }

            for (ZipEntry entry : entries) {
                byte[] bytes = readEntry(zipFile, entry);
                if (bytes == null) {
                    continue;
                }
                if (!hasDexMagic(bytes)) {
                    logger.warn("条目不是合法 dex（魔数不对），已跳过: " + entry.getName());
                    continue;
                }
                results.add(new Result(bytes, DexTrust.ORIGINAL, "来自 " + entry.getName()));
            }
        }
        return results;
    }

    private static byte[] readEntry(ZipFile zipFile, ZipEntry entry) {
        if (entry.getSize() > MAX_DEX_BYTES) {
            logger.warn("条目过大已跳过: " + entry.getName() + " (" + entry.getSize() + " 字节)");
            return null;
        }
        try (InputStream in = zipFile.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(
                    entry.getSize() > 0 ? (int) entry.getSize() : 8192);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("读取 " + entry.getName() + " 失败", e);
            return null;
        }
    }

    private static boolean hasDexMagic(byte[] bytes) {
        if (bytes == null || bytes.length < DEX_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < DEX_MAGIC.length; i++) {
            if (bytes[i] != DEX_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
