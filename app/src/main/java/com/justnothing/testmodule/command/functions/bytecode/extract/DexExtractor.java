package com.justnothing.testmodule.command.functions.bytecode.extract;

import java.io.IOException;
import java.util.List;

/**
 * 从一个 {@link DexSource} 里取出 dex 字节。
 *
 * <p>实现方各自声明自己能处理哪类来源（{@link #canHandle}），由
 * {@link DexExtractionManager} 按优先级挑选并回退。</p>
 *
 * <p>实现约定：</p>
 * <ul>
 *   <li>认领不了就 {@code canHandle} 返回 false，不要去碰文件；</li>
 *   <li>认出得了但取不出来（文件损坏、格式不认识），返回空列表或抛 IOException，
 *       <b>不要返回"看起来像 dex 但其实是别的东西"的字节</b> —— 静默的错误结果比失败更糟；</li>
 *   <li>每个结果的 {@link Result#trust()} 必须如实标注，尤其是没法 de-quicken 的时候。</li>
 * </ul>
 */
public interface DexExtractor {

    /**
     * 一次提取的产物。
     *
     * @param dex   完整 dex 字节
     * @param trust 这份 dex 的可信度（见 {@link DexTrust} 里为什么这个字段是必须的）
     * @param note  给用户看的补充说明，例如"来自 classes2.dex"或"未能 de-quicken 的原因"
     */
    record Result(byte[] dex, DexTrust trust, String note) {

        public boolean usable() {
            return dex != null && dex.length > 0 && trust.isCodeTrustworthy();
        }
    }

    /** 策略名，用于日志与提示。 */
    String name();

    /** 能否处理这个来源。 */
    boolean canHandle(DexSource source);

    /** 执行提取；可能产出多个 dex（多 dex 的 APK、多 dex 的 vdex）。 */
    List<Result> extract(DexSource source) throws IOException;
}
