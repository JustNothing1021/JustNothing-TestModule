package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.Adler32;

/**
 * 兜底策略：直接从 vdex 里按 dex 魔数把 dex 切出来，并<b>判断它有没有被 ART 改写</b>。
 *
 * <p>为什么需要它：vendored 的 vdexExtractor 停更于 2020 年，只覆盖到 vdex021
 * （Android 10/11 左右）。更高版本的 vdex 格式又改过，它大概率直接失败。但 vdex 里的
 * dex 段本身格式稳定（就是个标准 dex），所以"切出来"这一步永远做得到 ——
 * 真正的问题是<b>切出来的那份可能已经被 quicken 过</b>。</p>
 *
 * <p>于是这里的做法是：切出来之后立刻校验 dex 头部，据此标注可信度：</p>
 * <ul>
 *   <li><b>校验和自洽</b> → 内容与头部一致 → 标 {@link DexTrust#ORIGINAL}。
 *       实测确实存在这种设备/组件：vdex 里的 dex 就是原始的，此时什么都不用做就能用。</li>
 *   <li><b>校验和对不上</b> → 内容被改写（绝大多数情况就是 quicken）→ 标
 *       {@link DexTrust#QUICKENED}，并明确提示"这份只能读结构，别信代码"。</li>
 * </ul>
 *
 * <h3>为什么只校验 checksum、不校验 signature</h3>
 * 这是实测出来的，不是偷懒：
 * <ul>
 *   <li>APK 里那份<b>完全正常、jadx 能读</b>的 dex，checksum 自洽，但 signature 对不上；</li>
 *   <li>vdex 里被 quicken 的 dex，两者都对不上。</li>
 * </ul>
 * 也就是说 signature 字段在实际产物里并不可靠（厂商重新打包系统应用时常见），
 * 而 checksum 才是有效判据 —— 这也正是 jadx / dexlib2 / baksmali 的做法，
 * 它们校验的都只是 checksum。
 */
public final class RawDexSlicer implements DexExtractor {

    private static final Logger logger = Logger.getLoggerForName("RawDexSlicer");

    private static final byte[] DEX_MAGIC = {'d', 'e', 'x', '\n'};

    /** dex 头部固定 112 字节，短于它就不可能是个完整 dex。 */
    private static final int DEX_HEADER_SIZE = 112;

    /** 单个 dex 上限（防呆）。 */
    private static final long MAX_DEX_BYTES = 64L * 1024 * 1024;

    /** 一个文件里最多切出多少份（防呆）。 */
    private static final int MAX_DEX_COUNT = 64;

    @Override
    public String name() {
        return Text.zhEn("vdex 直接切片（无法 de-quicken，只做校验与标注）",
                "raw vdex slicing (no de-quicken; verify and annotate only)").text();
    }

    @Override
    public boolean canHandle(DexSource source) {
        DexSource.Kind kind = source.getKind();
        // 裸 dex 也走这条：按魔数切出来、校验头部，得到的判定和 vdex 里的 dex 是同一套。
        return (kind == DexSource.Kind.VDEX || kind == DexSource.Kind.DEX) && source.exists();
    }

    @Override
    public List<Result> extract(DexSource source) throws IOException {
        File file = source.getFile();
        byte[] raw = file.length() > MAX_DEX_BYTES * MAX_DEX_COUNT
                ? null : Files.readAllBytes(file.toPath());
        if (raw == null) {
            logger.warn("文件过大，跳过切片: " + file.getAbsolutePath());
            return Collections.emptyList();
        }

        List<Result> results = new ArrayList<>();
        int searchFrom = 0;
        while (results.size() < MAX_DEX_COUNT) {
            int start = indexOf(raw, DEX_MAGIC, searchFrom);
            if (start < 0 || start + DEX_HEADER_SIZE > raw.length) {
                break;
            }
            int fileSize = le32(raw, start + 32);
            if (fileSize < DEX_HEADER_SIZE || start + fileSize > raw.length) {
                // 这不是一份完整 dex 的起点（也可能是被截断），往后挪一点继续找
                searchFrom = start + DEX_MAGIC.length;
                continue;
            }

            byte[] dex = Arrays.copyOfRange(raw, start, start + fileSize);
            String version = new String(dex, 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            boolean intact = isChecksumConsistent(dex);

            results.add(new Result(
                    dex,
                    intact ? DexTrust.ORIGINAL : DexTrust.QUICKENED,
                    intact
                            ? Text.zhEn("文件内偏移 %s，dex %s，头部校验和自洽（未被改写）",
                                    "offset %s in file, dex %s, header checksum is consistent (not modified)")
                                    .format(start, version)
                            : Text.zhEn("文件内偏移 %s，dex %s，头部校验和对不上 —— 内容被改写（很可能被 ART quicken 过），只能读结构，代码不可信；需要 de-quicken 才能反编译",
                                    "offset %s in file, dex %s, header checksum mismatch — content was modified (most likely quickened by ART); structure only, code is not trustworthy; de-quicken is required to decompile")
                                    .format(start, version)));

            searchFrom = start + fileSize;
        }

        if (results.isEmpty()) {
            logger.warn("没能在文件里找到完整的 dex: " + file.getAbsolutePath());
        }
        return results;
    }

    /**
     * dex 的 checksum 是 Adler-32 over bytes[12, file_size)。与头部声明值一致即说明内容未被改动。
     */
    private static boolean isChecksumConsistent(byte[] dex) {
        int declared = le32(dex, 8);
        Adler32 adler = new Adler32();
        adler.update(dex, 12, dex.length - 12);
        return declared == (int) adler.getValue();
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int le32(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }
}
