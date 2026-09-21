package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.command.framework.i18n.Text;

/**
 * 一份 dex 的"可信度"。
 *
 * <p>这个枚举是调试 vdex 时换来的教训：<b>"拿到了 dex"和"这份 dex 可信"是两件事。</b>
 * ART 会把 dex 写进 vdex 时就地 quicken（把字段/方法访问改写成带 vtable 索引的保留 opcode），
 * 这种 dex 能被 jadx 正常打开、也能显示出"源码"，但指令语义是错的 —— 比直接报错危险得多。</p>
 *
 * <p>所以把可信度做成结果的一部分：调用方和用户一眼就能判断这份 dex 能不能用，
 * 而不是等反编译出一个看似正常、实则错误的答案才发现问题。</p>
 */
public enum DexTrust {

    /** 原件。例如 APK 里的 classes*.dex —— dx/d8 的产物，从未被 ART 改写。可直接反编译。 */
    ORIGINAL,

    /** 已被 de-quicken 还原（经 vdexExtractor 处理）。代码可信。 */
    DEQUICKENED,

    /** 被 quicken 且未能还原。只能用来读类/方法等结构信息，<b>里面的代码不可信</b>。 */
    QUICKENED,

    /** 判断不了（来源格式不认识、或校验信息缺失）。 */
    UNKNOWN;

    /** 这份 dex 里的代码是否可信到可以拿去反编译。 */
    public boolean isCodeTrustworthy() {
        return this == ORIGINAL || this == DEQUICKENED;
    }

    /** 给用户看的中文说明。 */
    public String describe() {
        switch (this) {
            case ORIGINAL:
                return Text.zhEn("原件，代码可信", "original, code is trustworthy").text();
            case DEQUICKENED:
                return Text.zhEn("已还原（已 de-quicken），代码可信",
                        "restored (de-quickened), code is trustworthy").text();
            case QUICKENED:
                return Text.zhEn("被 ART 改写且未还原，只能看结构，代码不可信",
                        "rewritten by ART and not restored; structure only, code is not trustworthy").text();
            default:
                return Text.zhEn("可信度未知", "trust level unknown").text();
        }
    }
}
