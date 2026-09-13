package com.justnothing.testmodule.command.functions.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.inspect.Inspect;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.scope.Scope;
import com.justnothing.richconsole.syntax.Syntax;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.constants.FileDirectory;

/**
 * RichConsole 新功能专项测试：Scope / Inspect / 语法主题 / Progress.track / Progress.wrapFile /
 * 录制与导出（beginRecord / exportHtml / exportSvg / save / time）。
 */
@Cmd(name = "featuredemo", description = "RichConsole new-features demo (Scope/Inspect/Themes/track/wrapFile/Record&Export)")
public class FeatureDemoMain extends MainCommand<CommandResult> {

    public FeatureDemoMain() {
        super("FeatureDemo", CommandResult.class);
    }

    /** 供 Inspect 演示的小对象。 */
    private static final class DemoPoint {
        final int x;
        final int y;
        final String label;

        DemoPoint(int x, int y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }

        int sum() {
            return x + y;
        }
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        Console console = context.console();
        if (console == null) console = Console.of(cfg -> cfg.withForceTerminal(true));

        // =====================================================================
        // 1. Scope — 渲染一组命名变量（对齐 Python rich 的 render_scope(locals())）
        // =====================================================================
        console.rule("Scope");
        Map<String, Object> locals = new LinkedHashMap<>();
        locals.put("name", "RichConsole");
        locals.put("version", "0.1.0");
        locals.put("rows", 42);
        locals.put("ratio", 3.14);
        locals.put("active", true);
        console.println(Scope.of(locals, cfg -> cfg.title("demo locals")));
        console.println();

        // =====================================================================
        // 2. Inspect — 反射对象的字段和方法
        // =====================================================================
        console.rule("Inspect");
        console.println(Inspect.of(new DemoPoint(3, 4, "origin"), cfg -> cfg.methods(true)));
        console.println();
        // 自定义主题：类型红、方法名青、修饰符绿、边框黄
        console.println(Inspect.of(new DemoPoint(3, 4, "origin"), cfg -> cfg.methods(true).styles(Map.of(
                "modifier", "green",
                "name", "cyan",
                "border", "yellow"))));
        console.println();

        // =====================================================================
        // 3. 语法主题 — 同一份代码切换 monokai / ansi_dark / ansi_light
        // =====================================================================
        console.rule("Syntax Themes");
        String code = """
                public class Demo {
                    private static final int MAX = 100;
                    public static void main(String[] args) {
                        System.out.println("hi");
                    }
                }
                """;
        console.println(Syntax.of(code, cfg -> cfg.lexerName("java").lineNumbers(true).themeName("monokai")));
        console.println(Syntax.of(code, cfg -> cfg.lexerName("java").lineNumbers(true).themeName("ansi_dark")));
        console.println(Syntax.of(code, cfg -> cfg.lexerName("java").lineNumbers(true).themeName("ansi_light")));
        console.println();

        // =====================================================================
        // 4. Progress.track() — 遍历 Collection，total 自动检测
        // =====================================================================
        console.rule("Progress.track()");
        List<String> steps = Arrays.asList(
                "Build", "Test", "Package", "Upload", "Verify", "Configure", "Done!");
        try (Progress progress = console.progress()) {
            progress.start();
            for (String step : progress.track(steps, "Deploying", null, 0.0)) {
                Thread.sleep(200);
            }
        }
        console.println();

        // =====================================================================
        // 5. Progress.wrapFile() — 读文件按字节推进，total 取文件长度
        // =====================================================================
        console.rule("Progress.wrapFile()");
        File dir = new File(FileDirectory.METHODS_DATA_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建目录: " + dir);
        }
        File target = new File(dir, "wrapfile_demo.bin");
        byte[] payload = new byte[512 * 1024];
        new Random().nextBytes(payload);
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(payload);
        }
        try (Progress progress = console.progress()) {
            progress.start();
            try (InputStream in = progress.wrapFile(
                    new FileInputStream(target), target.length(), "Reading wrapfile_demo.bin")) {
                byte[] buffer = new byte[4096];
                int total = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                    Thread.sleep(3);
                }
                console.log("[green]" + total + "[/green] bytes read");
            }
        }
        if (!target.delete()) {
            console.log("[yellow]警告:[/yellow] 临时文件删除失败 " + target);
        }
        console.println();

        // =====================================================================
        // 6. 录制与导出 — beginRecord / exportHtml / exportSvg / time()
        // =====================================================================
        console.rule("Record & Export");
        console.println("下面这段演示 beginRecord 运行时开启录制，再导出 HTML / SVG 到 "
                + FileDirectory.METHODS_DATA_DIR);
        console.println(new Panel("这段 Panel 也会被导出", "Export Panel"));
        console.beginRecord();
        console.println("[bold cyan]Recorded[/] [red]content[/] [dim]dimmed[/] [reverse]reverse[/]");
        try (AutoCloseable timer = console.time("export")) {
            // 先导出 HTML（不清空），再导出 SVG（清空）
            String html = console.exportHtml(false, null, null, false);
            String svg = console.exportSvg("Export Demo", null, true, null, 0.61, null);
            File htmlFile = new File(dir, "export_demo.html");
            File svgFile = new File(dir, "export_demo.svg");
            try (FileWriter writer = new FileWriter(htmlFile)) {
                writer.write(html);
            }
            try (FileWriter writer = new FileWriter(svgFile)) {
                writer.write(svg);
            }
            console.log("HTML 导出 " + html.length() + " 字符 -> " + htmlFile);
            console.log("SVG 导出 " + svg.length() + " 字符 -> " + svgFile);
        }
        console.endRecord();
        console.println("isRecording=" + console.isRecording() + " (应为 false)");
        console.println();

        return createSuccessResult("完成");
    }
}
