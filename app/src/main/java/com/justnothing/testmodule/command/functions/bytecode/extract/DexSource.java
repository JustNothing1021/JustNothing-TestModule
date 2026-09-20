package com.justnothing.testmodule.command.functions.bytecode.extract;

import java.io.File;

/**
 * 一处"可能装着 dex"的来源。
 *
 * <p>把"来源"抽象出来，是为了让提取策略（{@link DexExtractor}）可以按能力认领：
 * 比如 {@code ApkDexExtractor} 只认 APK/JAR，而 vdex 的处理要交给另一条策略。
 * 这样将来某个格式不被支持时，<b>定位层仍然能告诉用户"这个类在这个文件里"</b>——
 * 这一条能力不依赖任何外部二进制，永远不会失效。</p>
 */
public final class DexSource {

    public enum Kind {
        /** 应用 APK（含 split apk），dex 在 classes*.dex 里。 */
        APK,
        /** 普通 jar（部分老设备的 framework jar 里还有 classes.dex）。 */
        JAR,
        /** 裸 dex 文件（例如从 vdex 里切出来落盘的、或 ART 动态生成的）。 */
        DEX,
        /** ART 的 vdex —— 里面的 dex 可能被 quicken 过。 */
        VDEX,
        /** ART 的 odex（ELF/OAT 容器），通常需要先取出内嵌的 vdex。 */
        ODEX
    }

    private final Kind kind;
    private final File file;
    private final String label;

    private DexSource(Kind kind, File file, String label) {
        this.kind = kind;
        this.file = file;
        this.label = label;
    }

    public static DexSource of(Kind kind, File file) {
        return new DexSource(kind, file, file == null ? "(null)" : file.getAbsolutePath());
    }

    public Kind getKind() {
        return kind;
    }

    public File getFile() {
        return file;
    }

    public String getLabel() {
        return label;
    }

    public boolean exists() {
        return file != null && file.isFile();
    }

    @Override
    public String toString() {
        return kind + "[" + label + "]";
    }
}
