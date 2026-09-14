package com.justnothing.testmodule.command.functions.bytecode.extract;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link DexClassIndex} 与 {@link DexHeader} 的单测（纯 Java，无需 Android 运行时）。
 *
 * <p>这两个类都是手写的二进制解析，偏移写错一点就会"看起来正常但答案是错的"，
 * 所以用手工构造的 dex 把关键路径固化下来：</p>
 * <ul>
 *   <li>string_ids 二分查找（含"类不存在"的情况）</li>
 *   <li>class_defs 遍历列类名</li>
 *   <li>从更大的容器文件里按魔数找出内嵌 dex —— 包括<b>魔数跨读块边界</b>的情况</li>
 *   <li>头部字段解析与 checksum 校验（含"内容被改过"的负例）</li>
 * </ul>
 */
public class DexClassIndexTest {

    private static final List<String> CLASSES = List.of("Lcom/example/Bar;", "Lcom/example/Foo;");

    @Test
    public void findsClassesInsideDex() throws IOException {
        File file = dexFile("classes.dex", buildMinimalDex(CLASSES));

        assertTrue(DexClassIndex.containsClass(file, "com.example.Bar"));
        assertTrue(DexClassIndex.containsClass(file, "com.example.Foo"));
        assertFalse(DexClassIndex.containsClass(file, "com.example.Baz"));
        // 传 descriptor 形式也应接受
        assertTrue(DexClassIndex.containsClass(file, "Lcom/example/Foo;"));
        assertTrue(DexClassIndex.containsClass(file, "com/example/Foo"));
    }

    @Test
    public void findsClassesInRawDexBytes() {
        // 直接喂 byte[] 的这条给"已经拿到 dex 字节"的调用方用（多 dex 里挑出含目标类的那一份
        // 就靠它）—— 结论必须和走文件的那条完全一致。
        byte[] dex = buildMinimalDex(CLASSES);

        assertTrue(DexClassIndex.containsClass(dex, "com.example.Bar"));
        assertTrue(DexClassIndex.containsClass(dex, "Lcom/example/Foo;"));
        assertFalse(DexClassIndex.containsClass(dex, "com.example.Baz"));
        assertFalse(DexClassIndex.containsClass(new byte[0], "com.example.Bar"));
        assertFalse(DexClassIndex.containsClass((byte[]) null, "com.example.Bar"));
    }

    /**
     * "被引用"和"被定义"必须区分开。
     *
     * <p>这是真机上踩到的坑：多 dex 的 APK 里，一个类常在某些 dex 里只被引用、
     * 只在其中一个里定义。containsClass 查 string_ids 会把引用也算命中，
     * 用它挑 dex 就会挑到"只引用"的那份，反编译器找不到类定义、返回空。</p>
     */
    @Test
    public void distinguishesDefinitionFromReference() throws IOException {
        List<String> strings = List.of(
                "Lcom/example/Bar;",
                "Lcom/example/Foo;",
                "Lcom/example/OnlyReferenced;");
        List<String> defined = List.of("Lcom/example/Bar;", "Lcom/example/Foo;");

        File file = dexFile("classes.dex", buildMinimalDex(strings, defined));

        // 字符串池里有 → containsClass 命中
        assertTrue(DexClassIndex.containsClass(file, "com.example.OnlyReferenced"));
        // 但没有 class_defs 条目 → 不算定义
        assertFalse(DexClassIndex.definesClass(file, "com.example.OnlyReferenced"));

        // 真正定义了的两个都能认出来
        assertTrue(DexClassIndex.definesClass(file, "com.example.Foo"));
        assertTrue(DexClassIndex.definesClass(file, "com.example.Bar"));

        // 完全没有的类，两个判据都为 false
        assertFalse(DexClassIndex.containsClass(file, "com.example.Nowhere"));
        assertFalse(DexClassIndex.definesClass(file, "com.example.Nowhere"));
    }

    @Test
    public void listsClassNamesFromClassDefs() throws IOException {
        File file = dexFile("classes.dex", buildMinimalDex(CLASSES));

        assertEquals(List.of("com.example.Bar", "com.example.Foo"),
                DexClassIndex.listClasses(file, 10));
        assertEquals(List.of("com.example.Bar"), DexClassIndex.listClasses(file, 1));
        assertTrue(DexClassIndex.listClasses(file, 0).isEmpty());
    }

    @Test
    public void findsDexEmbeddedInLargerContainer() throws IOException {
        byte[] dex = buildMinimalDex(CLASSES);
        byte[] container = embed(dex, 100);

        File file = dexFile("fake.vdex", container);
        assertTrue(DexClassIndex.containsClass(file, "com.example.Foo"));
        assertFalse(DexClassIndex.containsClass(file, "com.example.Nope"));
    }

    /**
     * 魔数跨读块边界：把 dex 起点放在 64KB 块的最后一个字节上，
     * 这样 "dex\n" 会被劈成两块 —— 扫描逻辑必须靠 carry 把它拼回来。
     */
    @Test
    public void findsDexWhoseMagicStraddlesScanChunk() throws IOException {
        byte[] dex = buildMinimalDex(CLASSES);
        byte[] container = embed(dex, 64 * 1024 - 1);

        File file = dexFile("straddle.vdex", container);
        assertTrue(DexClassIndex.containsClass(file, "com.example.Foo"));
    }

    /** 填充字节里出现 "dex\n" 但没有合法头部时不能误判。 */
    @Test
    public void ignoresFakeMagicWithoutValidHeader() throws IOException {
        byte[] container = new byte[64 * 1024];
        byte[] magic = {'d', 'e', 'x', '\n'};
        System.arraycopy(magic, 0, container, 1000, 4);

        File file = dexFile("bogus.vdex", container);
        assertTrue(DexClassIndex.listClasses(file, 10).isEmpty());
    }

    @Test
    public void parsesDexHeader() {
        byte[] dex = buildMinimalDex(CLASSES);
        DexHeader header = DexHeader.parse(dex);

        assertNotNull(header);
        assertEquals("035", header.version());
        assertEquals(dex.length, header.fileSize());
        assertEquals(2, header.stringIdsSize());
        assertEquals(2, header.classDefsSize());
        assertEquals(0, header.methodIdsSize());
        assertTrue(header.plausible(dex.length));
        assertTrue(header.checksumMatches(dex));
    }

    @Test
    public void rejectsNonDexBytes() {
        assertNull(DexHeader.parse(new byte[112]));
        assertNull(DexHeader.parse("definitely not a dex".getBytes(StandardCharsets.UTF_8)));
        assertNull(DexHeader.parse(null));
    }

    @Test
    public void detectsTamperedDex() {
        byte[] dex = buildMinimalDex(CLASSES);
        // 改一个字节但不更新头部 checksum —— 这正是被 ART quicken 之后的特征
        dex[dex.length - 1] ^= 0xFF;
        assertFalse(DexHeader.parse(dex).checksumMatches(dex));
    }

    @Test
    public void descriptorOfAcceptsEveryCommonForm() {
        assertEquals("Lcom/foo/Bar;", DexClassIndex.descriptorOf("com.foo.Bar"));
        assertEquals("Lcom/foo/Bar;", DexClassIndex.descriptorOf("com/foo/Bar"));
        assertEquals("Lcom/foo/Bar;", DexClassIndex.descriptorOf("Lcom/foo/Bar;"));
        assertNull(DexClassIndex.descriptorOf(null));
        assertNull(DexClassIndex.descriptorOf("  "));
    }

    // ---------- 测试辅助 ----------

    private static File dexFile(String name, byte[] content) throws IOException {
        File file = File.createTempFile("dexclassindex-", "-" + name);
        file.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content);
        }
        return file;
    }

    /** 在前面垫 {@code prefixLength} 字节无关数据，模拟 vdex / odex 这种"dex 不在偏移 0"的容器。 */
    private static byte[] embed(byte[] dex, int prefixLength) {
        byte[] container = new byte[prefixLength + dex.length + 32];
        Arrays.fill(container, (byte) 0x7F);
        System.arraycopy(dex, 0, container, prefixLength, dex.length);
        return container;
    }

    private static byte[] buildMinimalDex(List<String> descriptors) {
        return buildMinimalDex(descriptors, descriptors);
    }

    /**
     * 造一个最小但结构完整、checksum 自洽的 dex。
     *
     * <p>只包含 string_ids / type_ids / class_defs 三张表，proto/field/method 表大小为 0 ——
     * 对"查类名"和"列类名"足够了。{@code descriptors} 必须已按字符串顺序排好
     * （dex 规范要求 string_ids 有序，二分查找依赖这一点）。</p>
     *
     * @param definedClasses {@code descriptors} 的<b>子集</b>：只有它们会有 class_defs 条目。
     *                       其余 descriptor 只出现在字符串池和 type_ids 里，
     *                       相当于"被引用、但没有定义"。
     */
    private static byte[] buildMinimalDex(List<String> descriptors, List<String> definedClasses) {
        int count = descriptors.size();
        int definedCount = definedClasses.size();
        int stringIdsOff = 112;
        int typeIdsOff = stringIdsOff + count * 4;
        int classDefsOff = typeIdsOff + count * 4;
        int dataOff = classDefsOff + definedCount * 32;

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] stringDataOffsets = new int[count];
        for (int i = 0; i < count; i++) {
            stringDataOffsets[i] = dataOff + data.size();
            byte[] utf8 = descriptors.get(i).getBytes(StandardCharsets.UTF_8);
            writeUleb128(data, descriptors.get(i).length());
            data.write(utf8, 0, utf8.length);
            data.write(0);
        }

        int fileSize = dataOff + data.size();
        byte[] dex = new byte[fileSize];

        dex[0] = 'd';
        dex[1] = 'e';
        dex[2] = 'x';
        dex[3] = '\n';
        byte[] version = "035".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(version, 0, dex, 4, 3);

        putLe32(dex, 0x20, fileSize);
        putLe32(dex, 0x24, 112);
        putLe32(dex, 0x28, 0x12345678);
        putLe32(dex, 0x38, count);
        putLe32(dex, 0x3C, stringIdsOff);
        putLe32(dex, 0x40, count);
        putLe32(dex, 0x44, typeIdsOff);
        putLe32(dex, 0x60, definedCount);
        putLe32(dex, 0x64, classDefsOff);

        for (int i = 0; i < count; i++) {
            putLe32(dex, stringIdsOff + i * 4, stringDataOffsets[i]);
            putLe32(dex, typeIdsOff + i * 4, i);
        }
        for (int i = 0; i < definedCount; i++) {
            // class_def_item 的第一个字段 class_idx 指向 type_ids
            putLe32(dex, classDefsOff + i * 32, descriptors.indexOf(definedClasses.get(i)));
        }

        byte[] dataBytes = data.toByteArray();
        System.arraycopy(dataBytes, 0, dex, dataOff, dataBytes.length);

        Adler32 adler = new Adler32();
        adler.update(dex, 12, fileSize - 12);
        putLe32(dex, 0x08, (int) adler.getValue());
        return dex;
    }

    private static void putLe32(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }

    private static void writeUleb128(ByteArrayOutputStream out, int value) {
        int remaining = value;
        do {
            int b = remaining & 0x7F;
            remaining >>>= 7;
            if (remaining != 0) {
                b |= 0x80;
            }
            out.write(b);
        } while (remaining != 0);
    }
}
