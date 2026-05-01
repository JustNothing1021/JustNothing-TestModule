package com.justnothing.testmodule.command.output;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.javainterpreter.security.BasicPermissionChecker;
import com.justnothing.javainterpreter.security.IPermissionChecker;
import com.justnothing.javainterpreter.security.SandboxGuard;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.io.File;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class DisclaimerManager {

    private static final String TAG = "DisclaimerManager";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static final String DISCLAIMER_FILE = "disclaimer_accepted.json";
    private static final String KEY_ACCEPTED = "accepted";
    private static final String KEY_ACCEPTED_AT = "acceptedAt";
    private static final String KEY_VERSION = "version";
    private static final String KEY_SKILL_VERIFIED = "skillVerified";
    private static IPermissionChecker exprChecker = BasicPermissionChecker.createMinimal();

    private static final int DISCLAIMER_VERSION = 2;

    private static final String DISCLAIMER_TEXT = """
            
            ╔═══════════════════════════════════════════════════════════════════════════════════════════╗
            ║                                                                                           ║
            ║                                 免责声明 / DISCLAIMER                                     ║
            ║                                                                                           ║
            ║   这个模块是一个权限相当高的系统调试工具，在使用前请仔细阅读以下内容!!!                   ║
            ║                                                                                           ║
            ║   1. 本工具需要Root权限和Xposed框架才能正常工作 (我去不早说)                              ║
            ║   2. 不当使用可能导致系统爆炸, 数据丢失或者其他更严重的负面效果                           ║
            ║   3. 不要在你的重要设备上使用本工具 (除非你知道你在做什么并且足够相信这个模块)            ║
            ║   4. 这个项目仅供学习用, 请不要用于任何非法用途, 作者不会承担任何责任                     ║
            ║   5. 这个项目对Java的基本功有一定的要求, 确保你对Java有一定的了解, 不然瞎整大概率会爆炸   ║
            ║   6. 由于命令接口目前对所有身份开放, 其他应用也可以调用本工具, 请注意设备安全             ║
            ║   7. 这个项目的代码相当烂, 作者不会对任何因为看到脑溢血代码而导致的高血压等症状负责       ║
            ║   8. 使用本工具所产生的一切后果由用户自行承担 (主要是我也承担不起)                        ║
            ║                                                                                           ║
            ║                                                                                           ║
            ║   This module is a high-level-permission required system debugging tool.                  ║
            ║   Before using it, please read the content above carefully!                               ║
            ║                                                                                           ║
            ║   (Sorry my English is TERRIBLE but I will provide an English version in future)          ║
            ╚═══════════════════════════════════════════════════════════════════════════════════════════╝
            
            """;

    private static volatile boolean sessionAccepted = false;
    private static volatile boolean sessionSkillVerified = false;

    private DisclaimerManager() {
    }

    public static boolean isAccepted() {
        if (sessionAccepted) {
            return true;
        }

        File file = getDisclaimerFile();
        if (!file.exists()) {
            return false;
        }

        try {
            String content = IOManager.readFile(file);
            JSONObject json = new JSONObject(content);
            int version = json.optInt(KEY_VERSION, 0);
            boolean accepted = json.optBoolean(KEY_ACCEPTED, false);

            if (accepted && version >= DISCLAIMER_VERSION) {
                sessionAccepted = true;
                sessionSkillVerified = json.optBoolean(KEY_SKILL_VERIFIED, false);
                return true;
            }
        } catch (Exception e) {
            logger.error("读取免责声明状态失败", e);
        }

        return false;
    }

    public static boolean isSkillVerified() {
        if (sessionSkillVerified) {
            return true;
        }
        isAccepted();
        return sessionSkillVerified;
    }

    public static void accept() {
        sessionAccepted = true;

        try {
            JSONObject json = new JSONObject();
            json.put(KEY_ACCEPTED, true);
            json.put(KEY_ACCEPTED_AT, System.currentTimeMillis());
            json.put(KEY_VERSION, DISCLAIMER_VERSION);
            json.put(KEY_SKILL_VERIFIED, sessionSkillVerified);

            File file = getDisclaimerFile();
            IOManager.writeFile(file, json.toString(2));
            logger.info("免责声明已接受");
        } catch (Exception e) {
            logger.error("保存免责声明状态失败", e);
        }
    }

    public static void setSkillVerified(boolean verified) {
        sessionSkillVerified = verified;
        if (sessionAccepted) {
            accept();
        }
    }

    public static void reset() {
        sessionAccepted = false;
        sessionSkillVerified = false;
        File file = getDisclaimerFile();
        if (file.exists()) {
            IOManager.deleteFile(file);
        }
    }

    public static void showDisclaimer(ICommandOutputHandler output) {
        output.println(DISCLAIMER_TEXT, Colors.YELLOW);
    }

    public static boolean showDisclaimerIfNeeded(ICommandOutputHandler output) {
        if (isAccepted()) {
            return true;
        }

        showDisclaimer(output);
        return false;
    }

    public static boolean promptForAcceptance(ICommandOutputHandler output) {
        if (isAccepted()) {
            return true;
        }

        showDisclaimer(output);

        output.println("", Colors.DEFAULT);
        output.print("是否接受以上条款？", Colors.CYAN);
        output.print(" (yes/no) ", Colors.GRAY);

        String response = output.readLineFromClient("");

        output.println("", Colors.GRAY);


        if (response == null) {
            output.println("未收到响应，默认拒绝访问", Colors.RED);
            return false;
        }

        response = response.trim().toLowerCase();

        if ("yes".equals(response) || "y".equals(response) || "是".equals(response)) {
            accept();
            output.println("", Colors.DEFAULT);
            output.println("感谢理解，欢迎使用本工具！", Colors.LIGHT_GREEN);
            output.println("", Colors.DEFAULT);
            return true;
        } else {
            output.println("", Colors.DEFAULT);
            output.println("可以理解，毕竟这个工具还是有点危险的", Colors.YELLOW);
            output.println("但是毕竟是个免责声明，所以你可能暂时无法使用本工具", Colors.RED);
            output.println("", Colors.DEFAULT);
            return false;
        }
    }


    private static Object evaluateCode(String expression) {
        ScriptRunner runner = new ScriptRunner();
        runner.getExecutionContext().setPermissionChecker(exprChecker);
        AtomicReference<Object> resultRef = new AtomicReference<>();
        SandboxGuard.executeMinimal(() -> resultRef.set(runner.executeWithResult(expression)));
        return resultRef.get();
    }

    public static boolean promptForSkillVerification(ICommandOutputHandler output) {
        if (isSkillVerified()) {
            return true;
        }

        output.println("", Colors.DEFAULT);
        output.println("╔══════════════════════════════════════════════════════════════════════════════╗", Colors.CYAN);
        output.println("║                    编程能力验证 / Skill Verification                         ║", Colors.CYAN);
        output.println("╠══════════════════════════════════════════════════════════════════════════════╣", Colors.CYAN);
        output.println("║  为了确保您具备基本的编程能力，请完成以下验证：                              ║", Colors.CYAN);
        output.println("║                                                                              ║", Colors.CYAN);
        output.println("║  请输入一段表达式代码，使其计算结果等于指定的值。                            ║", Colors.CYAN);
        output.println("║  注意：只能使用基本运算，不能调用函数或方法。                                ║", Colors.CYAN);
        output.println("╚══════════════════════════════════════════════════════════════════════════════╝", Colors.CYAN);
        output.println("", Colors.DEFAULT);

        Random random = new Random();
        int targetValue = random.nextInt(1000);

        output.println("目标值: " + targetValue + " (类型为int/java.lang.Integer)", Colors.LIGHT_GREEN);
        output.println("要求: 表达式去掉两端空格后长度>=10", Colors.YELLOW);
        output.println("", Colors.DEFAULT);
        output.println("请输入表达式 (要记得在末尾加个分号)", Colors.CYAN);

        String expression = output.readLineFromClient(">>> ");
        
        if (expression == null || expression.trim().isEmpty()) {
            output.println("未输入表达式，验证失败", Colors.RED);
            return false;
        }

        expression = expression.trim();

        if (expression.length() < 10) {
            output.println("表达式长度不足10个字符，验证失败", Colors.RED);
            return false;
        }

        try {
            Object result = evaluateCode(expression);
            
            if (result == null) {
                output.println("表达式结果为空，验证失败", Colors.RED);
                return false;
            }

            double resultValue;
            if (result instanceof Number) {
                resultValue = ((Number) result).doubleValue();
            } else {
                output.println("表达式结果不是数字: " + result, Colors.RED);
                return false;
            }

            if (Math.abs(resultValue - targetValue) < 0.0001) {
                output.println("", Colors.DEFAULT);
                output.println("验证通过！Let's rock！", Colors.LIGHT_GREEN);
                setSkillVerified(true);
                output.println("按Enter键继续...");
                output.readLineFromClient("");
                return true;
            } else {
                output.println("", Colors.DEFAULT);
                output.println("结果不正确，期望值: " + targetValue + ", 实际值: " + resultValue, Colors.RED);
                output.println("不过没关系，你可以暂时跳过验证直接使用（但请务必小心，别给系统炸了）", Colors.YELLOW);
                setSkillVerified(false);
                output.println("按Enter键继续...");
                output.readLineFromClient("");
                return true;
            }
        } catch (SecurityException e) {
            output.println("", Colors.DEFAULT);
            output.println("检测到不安全的操作: " + e.getMessage(), Colors.RED);
            output.println("请不要尝试调用函数或方法！", Colors.YELLOW);
            return false;
        } catch (Exception e) {
            output.println("", Colors.DEFAULT);
            output.println("表达式执行失败: " + e.getMessage(), Colors.RED);
            output.println("不过没关系，你可以跳过验证直接使用（但请务必小心，别给系统炸了）", Colors.YELLOW);
            output.println("按Enter键继续...");
            output.readLineFromClient("");
            return true;
        }
    }

    public static boolean fullVerification(ICommandOutputHandler output) {
        if (!promptForAcceptance(output)) {
            return false;
        }
        
        if (!isSkillVerified()) {
            return promptForSkillVerification(output);
        }
        
        return true;
    }

    private static File getDisclaimerFile() {
        return new File(DataBridge.getDataDir(), DISCLAIMER_FILE);
    }
}
