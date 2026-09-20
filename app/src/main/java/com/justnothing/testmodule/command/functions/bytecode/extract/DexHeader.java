package com.justnothing.testmodule.command.functions.bytecode.extract;

import java.nio.charset.StandardCharsets;
import java.util.zip.Adler32;

/**
 * dex 文件头（固定 112 字节）的解析与校验。
 *
 * <p>用来替掉以前那套"按 JVM class 格式解析"的实现：JVM class 头部是
 * {@code 0xCAFEBABE} + 常量池，而 dex 是 {@code dex\n035\0} + 各类 id 表，
 * 两者毫无关系 —— 拿前者去校验后者，结果必然是"魔数错误"。</p>
 *
 * <p>关于 checksum：它是 Adler-32 over {@code bytes[12, file_size)}。
 * 之所以只用它判断"内容有没有被改写"、而不看 signature（SHA-1），是实测结论：
 * 正常 APK 里的 dex 也经常 signature 对不上（厂商重打包），但 checksum 一直自洽；
 * 而被 quicken 过的 dex 两者都会对不上。jadx / dexlib2 / baksmali 也都只校验 checksum。</p>
 */
public record DexHeader(
        String version,
        long fileSize,
        int checksum,
        int headerSize,
        int stringIdsSize,
        int typeIdsSize,
        int protoIdsSize,
        int fieldIdsSize,
        int methodIdsSize,
        int classDefsSize
) {

    private static final byte[] MAGIC = {'d', 'e', 'x', '\n'};

    private static final int HEADER_SIZE = 112;

    /**
     * 解析 dex 头部。
     *
     * @return 解析结果；不是 dex（魔数不符）或长度不足时返回 {@code null}
     */
    public static DexHeader parse(byte[] dex) {
        if (dex == null || dex.length < HEADER_SIZE) {
            return null;
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (dex[i] != MAGIC[i]) {
                return null;
            }
        }
        // 魔数是 "dex\n" + 3 位版本号 + '\0'
        String version = new String(dex, 4, 3, StandardCharsets.US_ASCII);
        return new DexHeader(
                version,
                u32(dex, 0x20),
                le32(dex, 0x08),
                le32(dex, 0x24),
                le32(dex, 0x38),
                le32(dex, 0x40),
                le32(dex, 0x48),
                le32(dex, 0x50),
                le32(dex, 0x58),
                le32(dex, 0x60));
    }

    /** 头部声明的 checksum 是否与内容自洽（自洽 ⇒ 这份 dex 没被改写过）。 */
    public boolean checksumMatches(byte[] dex) {
        if (dex == null || dex.length < HEADER_SIZE) {
            return false;
        }
        Adler32 adler = new Adler32();
        adler.update(dex, 12, dex.length - 12);
        return checksum == (int) adler.getValue();
    }

    /** 头部合法性的基本自检：header_size 必须是 112，file_size 不能大于实际长度。 */
    public boolean plausible(int actualLength) {
        return headerSize == HEADER_SIZE && fileSize >= HEADER_SIZE && fileSize <= actualLength;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("dex 版本: ").append(version).append("\n");
        sb.append("声明大小: ").append(fileSize).append(" 字节\n");
        sb.append("字符串: ").append(stringIdsSize).append("\n");
        sb.append("类型: ").append(typeIdsSize).append("\n");
        sb.append("原型(方法签名): ").append(protoIdsSize).append("\n");
        sb.append("字段: ").append(fieldIdsSize).append("\n");
        sb.append("方法: ").append(methodIdsSize).append("\n");
        sb.append("类: ").append(classDefsSize).append("\n");
        return sb.toString();
    }

    private static int le32(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8)
                | ((b[offset + 2] & 0xFF) << 16) | ((b[offset + 3] & 0xFF) << 24);
    }

    private static long u32(byte[] b, int offset) {
        return le32(b, offset) & 0xFFFFFFFFL;
    }
}
