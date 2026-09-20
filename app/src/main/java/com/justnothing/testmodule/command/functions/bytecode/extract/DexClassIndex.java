package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.utils.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 只读 dex 的<b>类名索引</b>：回答"这个文件里有没有这个类"和"这个文件里有哪些类"。
 *
 * <p>定位层（{@link DexSourceLocator}）需要遍历很多候选文件（boot classpath 里十几个 vdex、
 * 一堆 odex、若干 apk）才能确定一个类的家在哪儿，所以这里必须做两件事：</p>
 * <ul>
 *   <li><b>不解析代码</b>：只看 {@code string_ids} 和 {@code class_defs} 两张表，不碰指令 ——
 *       因此对<b>被 quicken 过的 dex 也完全有效</b>（ART 改写的是代码段和字段索引，类名表不动）。</li>
 *   <li><b>不整文件读进内存</b>：vdex / odex 动辄几十 MB，走 {@link RandomAccessFile} 按需 seek，
 *       内存占用与文件大小无关。</li>
 * </ul>
 *
 * <p>为什么不直接用 {@code dalvik.system.DexFile}：它的构造器在新版本 Android 上属于
 * hidden API，且没有"只读一段"的能力 —— 我们要扫几十个候选文件，用它既不稳也重。</p>
 */
public final class DexClassIndex {

    private static final Logger logger = Logger.getLoggerForName("DexClassIndex");

    // ---- dex header 里用得到的字段偏移（dex 格式固定，不受版本影响） ----
    private static final int HEADER_SIZE = 112;
    private static final int OFF_FILE_SIZE = 0x20;
    private static final int OFF_HEADER_SIZE_FIELD = 0x24;
    private static final int OFF_ENDIAN_TAG = 0x28;
    private static final int OFF_STRING_IDS_SIZE = 0x38;
    private static final int OFF_STRING_IDS_OFF = 0x3C;
    private static final int OFF_TYPE_IDS_OFF = 0x44;
    private static final int OFF_CLASS_DEFS_SIZE = 0x60;
    private static final int OFF_CLASS_DEFS_OFF = 0x64;

    /** class_def_item 固定 32 字节。 */
    private static final int CLASS_DEF_ITEM_SIZE = 32;

    /** {@code dex\n} 小端读成 int。 */
    private static final int DEX_MAGIC_LE = 0x0A786564;

    /** dex header 的 header_size 恒为 112。 */
    private static final int EXPECTED_HEADER_SIZE = 0x70;

    /** endian_tag 恒为 0x12345678（小端常量）。 */
    private static final int EXPECTED_ENDIAN_TAG = 0x12345678;

    /** 扫描魔数时的块大小。 */
    private static final int SCAN_CHUNK = 64 * 1024;

    /** 单个 dex 的大小上限（防呆）。 */
    private static final long MAX_DEX_BYTES = 64L * 1024 * 1024;

    /** 单条字符串的长度上限（防呆；类名不会接近这个量级）。 */
    private static final int MAX_STRING_BYTES = 512;

    private static final Pattern DEX_ENTRY = Pattern.compile("classes\\d*\\.dex");

    private DexClassIndex() {
    }

    /** 把类名转成 dex 里的 descriptor：{@code com.foo.Bar} → {@code Lcom/foo/Bar;}。 */
    public static String descriptorOf(String className) {
        if (className == null) {
            return null;
        }
        String name = className.trim();
        if (name.isEmpty()) {
            return null;
        }
        // 容忍调用方直接传 descriptor（顺手把 '.' 换成 '/'，以防传的是 Lcom.foo.Bar;）
        if (name.charAt(0) == 'L' && name.endsWith(";")) {
            return name.replace('.', '/');
        }
        return "L" + name.replace('.', '/') + ";";
    }

    /**
     * 文件（裸 dex / vdex / odex / apk / jar）里是否含有这个类。
     *
     * <p>对 apk/jar 是查 zip 里的 {@code classes*.dex}；对其余文件是按 {@code dex\n} 魔数
     * 找出所有内嵌 dex 段再逐个查。</p>
     */
    public static boolean containsClass(File file, String className) throws IOException {
        if (file == null || !file.isFile()) {
            return false;
        }
        String descriptor = descriptorOf(className);
        if (descriptor == null) {
            return false;
        }

        if (isArchive(file)) {
            return archiveContainsClass(file, descriptor);
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            DexData data = new FileDexData(raf);
            boolean[] found = {false};
            forEachDexSegment(data, (dexOffset, dexSize) -> {
                if (containsClassInDex(data, dexOffset, descriptor)) {
                    found[0] = true;
                    return true;
                }
                return false;
            });
            return found[0];
        }
    }

    /**
     * 单个 dex（{@code byte[]}）里是否含有这个类。
     *
     * <p>给"已经拿到 dex 字节"的调用方用，免得为了判断再落一次盘。</p>
     */
    public static boolean containsClass(byte[] dex, String className) {
        if (dex == null || dex.length == 0) {
            return false;
        }
        String descriptor = descriptorOf(className);
        if (descriptor == null) {
            return false;
        }
        return containsClassInDex(new ArrayDexData(dex), 0, descriptor);
    }

    /**
     * 这个来源里<b>定义</b>了该类吗 —— 而不只是"引用过它"？
     *
     * <p><b>为什么必须区分开</b>：{@link #containsClass} 查的是 {@code string_ids}，
     * 只要类名在字符串池里出现过就算命中，而"被别的类引用"同样会留下字符串。
     * 多 dex 的 APK 里一个类常常在好几个 dex 里被引用、只在其中一个里定义，
     * 于是一旦用 {@code containsClass} 去挑 dex，就会挑到"只引用、不定义"的那份 ——
     * 反编译器在里面找不到类定义，直接返回空，表现成"类和来源都找到了，却什么都反编译不出来"。
     * （实测 i3launcher：{@code ayj} 在 classes2 被引用、定义在 classes4。）</p>
     *
     * <p>实现上先用 {@link #containsClass} 快速排除（二分，便宜），再遍历 {@code class_defs} 确认。</p>
     */
    public static boolean definesClass(File file, String className) throws IOException {
        if (file == null || !file.isFile()) {
            return false;
        }
        String descriptor = descriptorOf(className);
        if (descriptor == null || !containsClass(file, className)) {
            return false;
        }

        if (isArchive(file)) {
            try (ZipFile zipFile = new ZipFile(file)) {
                for (ZipEntry entry : sortedDexEntries(zipFile)) {
                    byte[] dex = readEntry(zipFile, entry);
                    if (dex != null && definesClassInDex(new ArrayDexData(dex), 0, descriptor)) {
                        return true;
                    }
                }
            }
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            DexData data = new FileDexData(raf);
            boolean[] found = {false};
            forEachDexSegment(data, (dexOffset, dexSize) -> {
                if (definesClassInDex(data, dexOffset, descriptor)) {
                    found[0] = true;
                    return true;
                }
                return false;
            });
            return found[0];
        }
    }

    /** {@link #definesClass(File, String)} 的内存版：{@code dex} 必须是一份完整的 dex。 */
    public static boolean definesClass(byte[] dex, String className) {
        if (dex == null || dex.length < HEADER_SIZE) {
            return false;
        }
        String descriptor = descriptorOf(className);
        if (descriptor == null) {
            return false;
        }
        return definesClassInDex(new ArrayDexData(dex), 0, descriptor);
    }

    /**
     * 遍历 {@code class_defs} 找匹配的 descriptor，找到即返回。
     *
     * <p>和 {@link #collectClassNames} 走同一条三级跳（class_defs → type_ids → string_ids），
     * 区别只在这里找到目标就停，不必把整张表列出来。</p>
     */
    private static boolean definesClassInDex(DexData data, long dexOffset, String descriptor) {
        long classDefsOff = u32(data.readInt(dexOffset + OFF_CLASS_DEFS_OFF));
        int classDefsSize = data.readInt(dexOffset + OFF_CLASS_DEFS_SIZE);
        long typeIdsOff = u32(data.readInt(dexOffset + OFF_TYPE_IDS_OFF));
        long stringIdsOff = u32(data.readInt(dexOffset + OFF_STRING_IDS_OFF));
        if (classDefsOff < 0 || classDefsSize <= 0 || typeIdsOff < 0 || stringIdsOff < 0) {
            return false;
        }

        long classDefsBase = dexOffset + classDefsOff;
        long typeIdsBase = dexOffset + typeIdsOff;
        long stringIdsBase = dexOffset + stringIdsOff;

        for (int i = 0; i < classDefsSize; i++) {
            int classIdx = data.readInt(classDefsBase + (long) i * CLASS_DEF_ITEM_SIZE);
            if (classIdx < 0) {
                return false;
            }
            int descriptorIdx = data.readInt(typeIdsBase + (long) classIdx * 4);
            if (descriptorIdx < 0) {
                continue;
            }
            long stringDataOff = u32(data.readInt(stringIdsBase + (long) descriptorIdx * 4));
            if (stringDataOff < 0) {
                continue;
            }
            if (descriptor.equals(data.readString(dexOffset + stringDataOff))) {
                return true;
            }
        }
        return false;
    }

    /** 列出文件里的类名；{@code limit} 为上限（多 dex 按顺序累计）。 */
    public static List<String> listClasses(File file, int limit) throws IOException {
        List<String> result = new ArrayList<>();
        if (file == null || !file.isFile() || limit <= 0) {
            return result;
        }

        if (isArchive(file)) {
            try (ZipFile zipFile = new ZipFile(file)) {
                for (ZipEntry entry : sortedDexEntries(zipFile)) {
                    byte[] dex = readEntry(zipFile, entry);
                    if (dex == null) {
                        continue;
                    }
                    collectClassNames(new ArrayDexData(dex), 0, limit - result.size(), result);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
            return result;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            DexData data = new FileDexData(raf);
            forEachDexSegment(data, (dexOffset, dexSize) -> {
                collectClassNames(data, dexOffset, limit - result.size(), result);
                return result.size() >= limit;
            });
        }
        return result;
    }

    // ---------- 内部：dex 解析 ----------

    /**
     * 在某个 dex 段里查类名。
     *
     * <p>用二分而不是线性扫：dex 规范强制 {@code string_ids} 按字符串内容排序，
     * 而类名是 ASCII，其排序顺序与 {@link String#compareTo} 一致，所以可以直接二分。
     * 一个几十万字符串的 dex 也只需要约 18 次读取。</p>
     */
    private static boolean containsClassInDex(DexData data, long dexOffset, String descriptor) {
        long stringIdsOff = u32(data.readInt(dexOffset + OFF_STRING_IDS_OFF));
        int stringIdsSize = data.readInt(dexOffset + OFF_STRING_IDS_SIZE);
        if (stringIdsOff < 0 || stringIdsSize <= 0) {
            return false;
        }

        long tableBase = dexOffset + stringIdsOff;
        int lo = 0;
        int hi = stringIdsSize - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long stringDataOff = u32(data.readInt(tableBase + (long) mid * 4));
            if (stringDataOff < 0) {
                return false;
            }
            String value = data.readString(dexOffset + stringDataOff);
            if (value == null) {
                return false;
            }
            int cmp = value.compareTo(descriptor);
            if (cmp == 0) {
                return true;
            }
            if (cmp < 0) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return false;
    }

    /** 遍历 {@code class_defs}，把类名（非 descriptor 形式）追加到 {@code out}。 */
    private static void collectClassNames(DexData data, long dexOffset, int max, List<String> out) {
        if (max <= 0) {
            return;
        }
        long classDefsOff = u32(data.readInt(dexOffset + OFF_CLASS_DEFS_OFF));
        int classDefsSize = data.readInt(dexOffset + OFF_CLASS_DEFS_SIZE);
        long typeIdsOff = u32(data.readInt(dexOffset + OFF_TYPE_IDS_OFF));
        long stringIdsOff = u32(data.readInt(dexOffset + OFF_STRING_IDS_OFF));
        if (classDefsOff < 0 || classDefsSize <= 0 || typeIdsOff < 0 || stringIdsOff < 0) {
            return;
        }

        long classDefsBase = dexOffset + classDefsOff;
        long typeIdsBase = dexOffset + typeIdsOff;
        long stringIdsBase = dexOffset + stringIdsOff;

        for (int i = 0; i < classDefsSize && out.size() < max; i++) {
            int classIdx = data.readInt(classDefsBase + (long) i * CLASS_DEF_ITEM_SIZE);
            if (classIdx < 0) {
                return;
            }
            int descriptorIdx = data.readInt(typeIdsBase + (long) classIdx * 4);
            if (descriptorIdx < 0) {
                continue;
            }
            long stringDataOff = u32(data.readInt(stringIdsBase + (long) descriptorIdx * 4));
            if (stringDataOff < 0) {
                continue;
            }
            String name = classNameOf(data.readString(dexOffset + stringDataOff));
            if (name != null) {
                out.add(name);
            }
        }
    }

    /** {@code Lcom/foo/Bar;} → {@code com.foo.Bar}；不是类 descriptor 时返回 null。 */
    private static String classNameOf(String descriptor) {
        if (descriptor == null || descriptor.length() < 2) {
            return null;
        }
        if (descriptor.charAt(0) != 'L' || !descriptor.endsWith(";")) {
            return null;
        }
        return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }

    /**
     * 按 {@code dex\n} 魔数找出文件里所有内嵌 dex 段并回调。
     *
     * <p>之所以要"找"而不是"按固定偏移读"：vdex 的头部长度随版本变化（vdex006/010/019/021
     * 各不相同），odex 外面还套着 ELF/OAT。而 dex 段本身在哪儿、长什么样是稳定的，
     * 所以直接扫魔数是最省心、最不受版本影响的定位方式。</p>
     *
     * <p>为了确认扫到的是<b>真 dex 头</b>而不是恰好等于 "dex\n" 的普通数据，
     * 用 header_size / endian_tag / file_size 三个字段交叉验证。</p>
     */
    private static void forEachDexSegment(DexData data, SegmentVisitor visitor) throws IOException {
        long length = data.length();
        if (length < HEADER_SIZE) {
            return;
        }

        byte[] buf = new byte[SCAN_CHUNK + 4];
        long pos = 0;
        int carry = 0;
        while (pos < length) {
            int read = data.read(pos, buf, carry, SCAN_CHUNK);
            if (read <= 0) {
                break;
            }
            int total = carry + read;
            long base = pos - carry;

            for (int i = 0; i + 4 <= total; i++) {
                if (buf[i] != 'd' || buf[i + 1] != 'e' || buf[i + 2] != 'x' || buf[i + 3] != '\n') {
                    continue;
                }
                long dexOffset = base + i;
                if (!isValidDexHeader(data, dexOffset)) {
                    continue;
                }
                long dexSize = u32(data.readInt(dexOffset + OFF_FILE_SIZE));
                if (visitor.visit(dexOffset, dexSize)) {
                    return;
                }
            }

            // 保留末尾 4 字节，避免魔数跨块被漏掉
            carry = Math.min(4, total);
            System.arraycopy(buf, total - carry, buf, 0, carry);
            pos += read;
        }
    }

    private static boolean isValidDexHeader(DexData data, long dexOffset) {
        if (dexOffset < 0 || dexOffset + HEADER_SIZE > data.length()) {
            return false;
        }
        if (data.readInt(dexOffset) != DEX_MAGIC_LE) {
            return false;
        }
        if (data.readInt(dexOffset + OFF_HEADER_SIZE_FIELD) != EXPECTED_HEADER_SIZE) {
            return false;
        }
        if (data.readInt(dexOffset + OFF_ENDIAN_TAG) != EXPECTED_ENDIAN_TAG) {
            return false;
        }
        long size = u32(data.readInt(dexOffset + OFF_FILE_SIZE));
        return size >= HEADER_SIZE && dexOffset + size <= data.length();
    }

    // ---------- 内部：zip ----------

    private static boolean isArchive(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".apk") || name.endsWith(".jar") || name.endsWith(".zip");
    }

    private static boolean archiveContainsClass(File archive, String descriptor) throws IOException {
        try (ZipFile zipFile = new ZipFile(archive)) {
            for (ZipEntry entry : sortedDexEntries(zipFile)) {
                byte[] dex = readEntry(zipFile, entry);
                if (dex != null && containsClassInDex(new ArrayDexData(dex), 0, descriptor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** classes.dex → classes2.dex → classes3.dex … 的顺序（按名字排序恰好就是这个顺序）。 */
    private static List<ZipEntry> sortedDexEntries(ZipFile zipFile) {
        List<ZipEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> all = zipFile.entries();
        while (all.hasMoreElements()) {
            ZipEntry entry = all.nextElement();
            if (!entry.isDirectory() && DEX_ENTRY.matcher(entry.getName()).matches()) {
                entries.add(entry);
            }
        }
        Collections.sort(entries, Comparator.comparing(ZipEntry::getName));
        return entries;
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
            logger.warn("读取 " + entry.getName() + " 失败: " + e.getMessage());
            return null;
        }
    }

    // ---------- 内部：数据访问抽象（文件 / 内存两块实现） ----------

    /** 让同一套解析逻辑既能跑在文件上，也能跑在 zip 条目解出来的内存上。 */
    private interface DexData {

        long length();

        /** 从 {@code offset} 读最多 {@code len} 字节到 {@code buf} 的 {@code destPos}；返回实读数。 */
        int read(long offset, byte[] buf, int destPos, int len) throws IOException;

        /** 读 4 字节小端；越界或读失败返回 -1。 */
        int readInt(long offset);

        /**
         * 读一个 {@code string_data_item}（uleb128 长度 + MUTF-8 内容 + 0x00 结尾）。
         *
         * <p>按 UTF-8 解码即可：MUTF-8 与 UTF-8 在 ASCII 上完全一致，而类名基本都是 ASCII。
         * 超过 {@link #MAX_STRING_BYTES} 或遇到 EOF 时返回 null。</p>
         */
        String readString(long offset);
    }

    private static final class FileDexData implements DexData {

        private final RandomAccessFile raf;
        private final long length;

        FileDexData(RandomAccessFile raf) throws IOException {
            this.raf = raf;
            this.length = raf.length();
        }

        @Override
        public long length() {
            return length;
        }

        @Override
        public int read(long offset, byte[] buf, int destPos, int len) throws IOException {
            if (offset < 0 || offset >= length) {
                return -1;
            }
            raf.seek(offset);
            return raf.read(buf, destPos, len);
        }

        @Override
        public int readInt(long offset) {
            if (offset < 0 || offset + 4 > length) {
                return -1;
            }
            try {
                raf.seek(offset);
                int b0 = raf.read();
                int b1 = raf.read();
                int b2 = raf.read();
                int b3 = raf.read();
                if ((b0 | b1 | b2 | b3) < 0) {
                    return -1;
                }
                return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public String readString(long offset) {
            if (offset < 0 || offset >= length) {
                return null;
            }
            try {
                raf.seek(offset);
                if (readUleb128(raf) < 0) {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream(64);
                int b;
                while (out.size() < MAX_STRING_BYTES && (b = raf.read()) > 0) {
                    out.write(b);
                }
                return out.size() >= MAX_STRING_BYTES
                        ? null
                        : new String(out.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }

        /** 读 uleb128；失败返回 -1（返回 0 也是合法值，所以用负数表示失败）。 */
        private static long readUleb128(RandomAccessFile raf) throws IOException {
            long value = 0;
            int shift = 0;
            while (shift <= 28) {
                int b = raf.read();
                if (b < 0) {
                    return -1;
                }
                value |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return value;
                }
                shift += 7;
            }
            return -1;
        }
    }

    private static final class ArrayDexData implements DexData {

        private final byte[] dex;

        ArrayDexData(byte[] dex) {
            this.dex = dex;
        }

        @Override
        public long length() {
            return dex.length;
        }

        @Override
        public int read(long offset, byte[] buf, int destPos, int len) {
            if (offset < 0 || offset >= dex.length) {
                return -1;
            }
            int n = (int) Math.min(len, dex.length - offset);
            System.arraycopy(dex, (int) offset, buf, destPos, n);
            return n;
        }

        @Override
        public int readInt(long offset) {
            if (offset < 0 || offset + 4 > dex.length) {
                return -1;
            }
            int i = (int) offset;
            return (dex[i] & 0xFF) | ((dex[i + 1] & 0xFF) << 8)
                    | ((dex[i + 2] & 0xFF) << 16) | ((dex[i + 3] & 0xFF) << 24);
        }

        @Override
        public String readString(long offset) {
            if (offset < 0 || offset >= dex.length) {
                return null;
            }
            int pos = (int) offset;
            // 跳过 uleb128 的 utf16 长度
            int shift = 0;
            while (shift <= 28) {
                if (pos >= dex.length) {
                    return null;
                }
                int b = dex[pos++] & 0xFF;
                if ((b & 0x80) == 0) {
                    break;
                }
                shift += 7;
            }
            if (shift > 28) {
                return null;
            }
            int end = pos;
            while (end < dex.length && dex[end] != 0 && end - pos < MAX_STRING_BYTES) {
                end++;
            }
            if (end - pos >= MAX_STRING_BYTES || end >= dex.length) {
                return null;
            }
            return new String(dex, pos, end - pos, StandardCharsets.UTF_8);
        }
    }

    private interface SegmentVisitor {
        /** @return true 表示停止扫描 */
        boolean visit(long dexOffset, long dexSize) throws IOException;
    }

    /** 把 int 当无符号解释；负数（读失败 / 越界）透传为 -1。 */
    private static long u32(int value) {
        return value < 0 ? -1L : (value & 0xFFFFFFFFL);
    }
}
