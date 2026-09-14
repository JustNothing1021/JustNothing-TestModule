package com.justnothing.testmodule.command.functions.bytecode.extract;

import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 把"定位"和"提取"串起来：给一个类名，直接拿到它所在的 dex。
 *
 * <p>分成 {@link DexSourceLocator}（类在哪个文件）和 {@link DexExtractionManager}
 * （怎么把 dex 弄出来）两层，是为了在提取能力失效时<b>退化而不是失能</b>：
 * 定位只看类名表，永远有效；提取依赖 vdex 格式，会随 Android 版本失效。
 * 所以即使提取全挂，{@code bytecode locate} 依然能告诉用户类在哪儿。</p>
 *
 * <p>"第一个能取出东西的来源就够了"：{@link DexSourceLocator} 的候选已按可信度排序
 * （APK 里的 dex 是原件 → vdex → odex），所以先成功的那个必然是质量最高的。</p>
 */
public final class ClassDexExtractor {

    private static final Logger logger = Logger.getLoggerForName("ClassDexExtractor");

    /** 一次提取：来源 + 产物。 */
    public record Outcome(DexSource source, DexExtractor.Result result) {
    }

    private ClassDexExtractor() {
    }

    /**
     * 定位类所在的来源并提取 dex。
     *
     * @return 提取结果；类找不到、或所有来源都取不出来时返回空列表
     */
    public static List<Outcome> extract(String className) {
        return extract(className, null);
    }

    /**
     * 带扫描进度的 {@link #extract(String)}。
     *
     * @param listener 进度回调，会转给 {@link DexSourceLocator#locate(String, ScanListener)}；
     *                 可为 {@code null}
     */
    public static List<Outcome> extract(String className, DexSourceLocator.ScanListener listener) {
        List<Outcome> outcomes = new ArrayList<>();

        for (DexSource source : DexSourceLocator.locate(className, listener)) {
            for (DexExtractor.Result result : DexExtractionManager.extract(source)) {
                outcomes.add(new Outcome(source, result));
            }
            if (!outcomes.isEmpty()) {
                break;
            }
            logger.warn("来源 " + source + " 里确认有类 " + className + "，但取不出 dex，继续试下一个来源");
        }

        return outcomes;
    }
}
