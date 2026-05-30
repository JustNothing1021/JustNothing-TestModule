package com.justnothing.methodsclient.tailtip;

import org.jline.console.CmdDesc;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.widget.TailTipWidgets;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TailTipManager {

    private static CmdDesc desc(String description) {
        List<AttributedString> mainDesc = Collections.singletonList(
                new AttributedString(description)
        );
        return new CmdDesc(mainDesc, Collections.emptyList(), Collections.emptyMap());
    }

    public static TailTipWidgets setupJavaTailTips(LineReader reader) {
        Map<String, CmdDesc> tailTips = new HashMap<>();

        // tailTips.put("public",     desc("访问修饰符：公开可见"));
        // tailTips.put("private",    desc("访问修饰符：仅类内可见"));
        // tailTips.put("protected",  desc("访问修饰符：包内及子类可见"));
        // tailTips.put("static",     desc("静态修饰符：属于类而非实例"));
        // tailTips.put("final",      desc("不可变修饰符：变量/方法/类不可被修改/继承"));
        // tailTips.put("class",      desc("定义一个类"));
        // tailTips.put("interface",  desc("定义一个接口"));
        // tailTips.put("extends",    desc("继承父类"));
        // tailTips.put("implements", desc("实现接口"));
        // tailTips.put("new",        desc("创建对象实例"));
        // tailTips.put("return",     desc("返回方法结果"));
        // tailTips.put("void",       desc("无返回值类型"));
        // tailTips.put("if",         desc("条件判断语句"));
        // tailTips.put("else",       desc("条件分支语句"));
        // tailTips.put("for",        desc("循环语句"));
        // tailTips.put("while",      desc("循环语句"));
        // tailTips.put("try",        desc("异常捕获语句"));
        // tailTips.put("catch",      desc("异常处理语句"));
        // tailTips.put("throw",      desc("抛出异常"));

        var res = new TailTipWidgets(reader, tailTips, 0, TailTipWidgets.TipType.COMBINED);
        res.enable();
        return res;
    }
}
