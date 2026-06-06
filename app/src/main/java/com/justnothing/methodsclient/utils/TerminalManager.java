package com.justnothing.methodsclient.utils;

import com.justnothing.methodsclient.completer.JavaCompleter;
import com.justnothing.methodsclient.highlighter.JavaSyntaxHighlighter;
import com.justnothing.methodsclient.tailtip.TailTipManager;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.spi.SystemStream;
import org.jline.utils.InfoCmp;

import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Objects;

public class TerminalManager {

    public static final Logger logger = Logger.getLoggerForName("TerminalManager");

    private static final LineReader reader;
    private static final Terminal terminal;

    // 终端模式枚举
    public enum TerminalMode {
        JNI,      // JNI原生模式（最快）
        JNA,      // JNA动态调用模式（兼容性好）
        EXEC,     // Exec外部命令模式（中等）
        DUMB      // Dumb简单模式（最基本功能）
    }

    private static TerminalMode currentMode = TerminalMode.JNI;  // 默认使用Exec


    private static final String CAP = """
            #       Reconstructed via infocmp from file: /usr/share/terminfo/78/xterm-256color
            xterm-256color|xterm with 256 colors,
                    am, bce, OTbs, ccc, xenl, km, mir, msgr, npc, mc5i,
                    cols#80, it#8, lines#24, colors#0x100, pairs#0x10000,
                    acsc=``aaffggiijjkkllmmnnooppqqrrssttuuvvwwxxyyzz{{||}}~~,
                    cbt=\\E[Z, bel=^G, cr=\\r, csr=\\E[%i%p1%d;%p2%dr, tbc=\\E[3g,
                    mgc=\\E[?69l, clear=\\E[H\\E[2J, el1=\\E[1K, el=\\E[K, ed=\\E[J,
                    hpa=\\E[%i%p1%dG, cup=\\E[%i%p1%d;%p2%dH, cud1=\\n,
                    home=\\E[H, civis=\\E[?25l, cub1=^H, cnorm=\\E[?12l\\E[?25h,
                    cuf1=\\E[C, cuu1=\\E[A, cvvis=\\E[?12;25h, dch1=\\E[P,
                    dl1=\\E[M, smacs=\\E(0, smam=\\E[?7h, blink=\\E[5m, bold=\\E[1m,
                    smcup=\\E[?1049h\\E[22;0;0t, dim=\\E[2m, smir=\\E[4h,
                    sitm=\\E[3m, rev=\\E[7m, invis=\\E[8m, smso=\\E[7m, smul=\\E[4m,
                    ech=\\E[%p1%dX, rmacs=\\E(B, rmam=\\E[?7l, sgr0=\\E(B\\E[m,
                    rmcup=\\E[?1049l\\E[23;0;0t, rmir=\\E[4l, ritm=\\E[23m,
                    rmso=\\E[27m, rmul=\\E[24m, flash=\\E[?5h$<100/>\\E[?5l,
                    is2=\\E[!p\\E[?3;4l\\E[4l\\E>,
                    initc=\\E]4;%p1%d;rgb:%p2%{255}%*%{1000}%/%2.2X/%p3%{255}%*%{1000}%/%2.2X/%p4%{255}%*%{1000}%/%2.2X\\E\\\\,
                    il1=\\E[L, ka1=\\EOw, ka3=\\EOy, kb2=\\EOu, kbs=^H, kbeg=\\EOE,
                    kcbt=\\E[Z, kc1=\\EOq, kc3=\\EOs, kdch1=\\E[3~, kcud1=\\EOB,
                    kend=\\EOF, kent=\\EOM, kf1=\\EOP, kf10=\\E[21~, kf11=\\E[23~,
                    kf12=\\E[24~, kf13=\\E[1;2P, kf14=\\E[1;2Q, kf15=\\E[1;2R,
                    kf16=\\E[1;2S, kf17=\\E[15;2~, kf18=\\E[17;2~,
                    kf19=\\E[18;2~, kf2=\\EOQ, kf20=\\E[19;2~, kf21=\\E[20;2~,
                    kf22=\\E[21;2~, kf23=\\E[23;2~, kf24=\\E[24;2~,
                    kf25=\\E[1;5P, kf26=\\E[1;5Q, kf27=\\E[1;5R, kf28=\\E[1;5S,
                    kf29=\\E[15;5~, kf3=\\EOR, kf30=\\E[17;5~, kf31=\\E[18;5~,
                    kf32=\\E[19;5~, kf33=\\E[20;5~, kf34=\\E[21;5~,
                    kf35=\\E[23;5~, kf36=\\E[24;5~, kf37=\\E[1;6P, kf38=\\E[1;6Q,
                    kf39=\\E[1;6R, kf4=\\EOS, kf40=\\E[1;6S, kf41=\\E[15;6~,
                    kf42=\\E[17;6~, kf43=\\E[18;6~, kf44=\\E[19;6~,
                    kf45=\\E[20;6~, kf46=\\E[21;6~, kf47=\\E[23;6~,
                    kf48=\\E[24;6~, kf49=\\E[1;3P, kf5=\\E[15~, kf50=\\E[1;3Q,
                    kf51=\\E[1;3R, kf52=\\E[1;3S, kf53=\\E[15;3~, kf54=\\E[17;3~,
                    kf55=\\E[18;3~, kf56=\\E[19;3~, kf57=\\E[20;3~,
                    kf58=\\E[21;3~, kf59=\\E[23;3~, kf6=\\E[17~, kf60=\\E[24;3~,
                    kf61=\\E[1;4P, kf62=\\E[1;4Q, kf63=\\E[1;4R, kf7=\\E[18~,
                    kf8=\\E[19~, kf9=\\E[20~, khome=\\EOH, kich1=\\E[2~,
                    kcub1=\\EOD, kmous=\\E[<, knp=\\E[6~, kpp=\\E[5~, kcuf1=\\EOC,
                    kDC=\\E[3;2~, kEND=\\E[1;2F, kind=\\E[1;2B, kHOM=\\E[1;2H,
                    kIC=\\E[2;2~, kLFT=\\E[1;2D, kNXT=\\E[6;2~, kPRV=\\E[5;2~,
                    kri=\\E[1;2A, kRIT=\\E[1;2C, kcuu1=\\EOA, rmkx=\\E[?1l\\E>,
                    smkx=\\E[?1h\\E=, meml=\\El, memu=\\Em, rmm=\\E[?1034l,
                    smm=\\E[?1034h, nel=\\EE, oc=\\E]104\\007, op=\\E[39;49m,
                    dch=\\E[%p1%dP, dl=\\E[%p1%dM, cud=\\E[%p1%dB,
                    ich=\\E[%p1%d@, indn=\\E[%p1%dS, il=\\E[%p1%dL,
                    cub=\\E[%p1%dD, cuf=\\E[%p1%dC, rin=\\E[%p1%dT,
                    cuu=\\E[%p1%dA, mc0=\\E[i, mc4=\\E[4i, mc5=\\E[5i,
                    rep=%p1%c\\E[%p2%{1}%-%db, rs1=\\Ec\\E]104\\007,
                    rs2=\\E[!p\\E[?3;4l\\E[4l\\E>, rc=\\E8, vpa=\\E[%i%p1%dd,
                    sc=\\E7, ind=\\n, ri=\\EM,
                    setab=\\E[%?%p1%{8}%<%t4%p1%d%e%p1%{16}%<%t10%p1%{8}%-%d%e48;5;%p1%d%;m,
                    setaf=\\E[%?%p1%{8}%<%t3%p1%d%e%p1%{16}%<%t9%p1%{8}%-%d%e38;5;%p1%d%;m,
                    sgr=%?%p9%t\\E(0%e\\E(B%;\\E[0%?%p6%t;1%;%?%p5%t;2%;%?%p2%t;4%;%?%p1%p3%|%t;7%;%?%p4%t;5%;%?%p7%t;8%;m,
                    smglp=\\E[?69h\\E[%i%p1%ds,
                    smglr=\\E[?69h\\E[%i%p1%d;%p2%ds,
                    smgrp=\\E[?69h\\E[%i;%p1%ds, hts=\\EH, ht=^I,
                    u6=\\E[%i%d;%dR, u7=\\E[6n, u8=\\E[?%[;0123456789]c,
                    u9=\\E[c,
            """;

    static {
        Terminal builtTerminal = null;

        if (System.getenv("TERM") == null || Objects.requireNonNull(System.getenv("TERM")).isEmpty()) {
            System.setProperty("org.jline.terminal.type", "xterm-256color");
        }

        boolean jniLoaded = false;
        try {
            String jarPath = FileDirectory.METHODS_CLIENT_JAR;
            File jarFile = new File(jarPath);
            if (jarFile.exists()) {
                String abi = android.os.Build.SUPPORTED_ABIS[0];
                File libFile = extractNativeLibFromApk(jarPath, abi, "libjlinenative.so");
                if (libFile != null) {
                    System.load(libFile.getAbsolutePath());
                    jniLoaded = true;
                    try {
                        Class<?> loaderClass = Class.forName("org.jline.nativ.JLineNativeLoader");
                        Field loadedField = loaderClass.getDeclaredField("loaded");
                        loadedField.setAccessible(true);
                        loadedField.setBoolean(null, true);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        InfoCmp.setLoadedInfoCmp("unix", CAP);
        System.setProperty("org.jline.terminal.type", "unix");

        if (jniLoaded) {
            try {
                builtTerminal = TerminalBuilder.builder()
                        .system(true)
                        .type("unix")
                        .provider("jni")
                        .build();
                currentMode = TerminalMode.JNI;
            } catch (Exception ignored) {
                jniLoaded = false;
            }
        }

        if (!jniLoaded) {
            try {
                String providerClassName = org.jline.terminal.impl.jna.JnaTerminalProvider.class.getName();
                Class<?> providerClass = Class.forName(providerClassName);
                Object providerInstance = providerClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Method sysTerminalMethod = providerClass.getMethod(
                        "sysTerminal",
                        String.class, String.class, boolean.class,
                        Charset.class, Charset.class, Charset.class,
                        boolean.class, Terminal.SignalHandler.class,
                        boolean.class, SystemStream.class
                );
                builtTerminal = (org.jline.terminal.Terminal) sysTerminalMethod.invoke(
                        providerInstance,
                        "JLine-Android", "xterm-256color", false,
                        StandardCharsets.UTF_8, StandardCharsets.UTF_8, StandardCharsets.UTF_8,
                        true, org.jline.terminal.Terminal.SignalHandler.SIG_DFL,
                        false, Enum.valueOf(SystemStream.class, "Output")
                );
                currentMode = TerminalMode.JNA;
            } catch (Throwable ignored) {
            }
        }

        if (builtTerminal == null) {
            try {
                builtTerminal = TerminalBuilder.builder()
                        .system(true)
                        .type("unix")
                        .exec(true)
                        .jna(false)
                        .jansi(false)
                        .build();
                currentMode = TerminalMode.EXEC;
            } catch (Throwable ignored) {
                try {
                    builtTerminal = TerminalBuilder.builder()
                            .system(true)
                            .type("dumb")
                            .dumb(true)
                            .build();
                    currentMode = TerminalMode.DUMB;
                } catch (Throwable ignored2) {
                }
            }
        }

        terminal = builtTerminal;

        logger.info("Terminal initialized: mode=" + currentMode);

        // 根据终端模式优化LineReader配置
        Completer completer = new JavaCompleter();

        LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer);

        // Dumb模式下禁用语法高亮（避免性能问题）
        if (currentMode != TerminalMode.DUMB) {
            readerBuilder.highlighter(new JavaSyntaxHighlighter());
        }

        reader = readerBuilder.build();

        // 只在非Dumb模式下启用TailTip（需要终端支持）
        if (currentMode != TerminalMode.DUMB) {
            TailTipManager.setupJavaTailTips(reader);
        }

        KeyMap<Binding> keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        assert keyMap != null;
        keyMap.bind(new org.jline.reader.Reference("up-line-or-history"), "\033[A");
        keyMap.bind(new org.jline.reader.Reference("up-line-or-history"), "\033OA");
        keyMap.bind(new org.jline.reader.Reference("down-line-or-history"), "\033[B");
        keyMap.bind(new org.jline.reader.Reference("down-line-or-history"), "\033OB");
        keyMap.bind(new org.jline.reader.Reference("backward-char"), "\033[D");
        keyMap.bind(new org.jline.reader.Reference("backward-char"), "\033OD");
        keyMap.bind(new org.jline.reader.Reference("forward-char"), "\033[C");
        keyMap.bind(new org.jline.reader.Reference("forward-char"), "\033OC");
        keyMap.bind(new org.jline.reader.Reference("beginning-of-line"), "\033[1~");
        keyMap.bind(new org.jline.reader.Reference("end-of-line"), "\033[4~");
        keyMap.bind(new org.jline.reader.Reference("beginning-of-line"), "\033[H");
        keyMap.bind(new org.jline.reader.Reference("end-of-line"), "\033[F");
        keyMap.bind(new org.jline.reader.Reference("delete-char"), "\033[3~");
        keyMap.bind(new org.jline.reader.Reference("up-line-or-history"), "\033[5~");
        keyMap.bind(new org.jline.reader.Reference("down-line-or-history"), "\033[6~");
        keyMap.bind(new org.jline.reader.Reference("self-insert"), "\\");
        keyMap.setAmbiguousTimeout(10); // 防止JLine在遇到有歧义的转义（比如一个单独的"\"）会等很长一段时间，用着难受
        reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION); // 防止JLine自己给别的特殊字符转义掉了

        Objects.requireNonNull(reader.getKeyMaps().get(LineReader.MAIN)).setAmbiguousTimeout(10);

    }

    public static String readLine() {
        return readLine("", null);
    }

    public static String readLine(String prompt) {
        return readLine(prompt, null);
    }

    public static String readLine(String prompt, Character mask) {
        return reader.readLine(prompt, mask);
    }


    public static TerminalMode getCurrentMode() {
        return currentMode;
    }


    public static boolean isHighPerformance() {
        return currentMode == TerminalMode.JNI;
    }


    public static String getTerminalInfo() {
        return String.format("模式=%s, 类型=%s, Provider=%s",
                currentMode,
                terminal.getType(),
                terminal.getClass().getSimpleName());
    }

    /**
     * 获取已初始化的 Terminal 实例（供 REPL 等外部组件复用）
     */
    public static org.jline.terminal.Terminal getTerminal() {
        return terminal;
    }

    private static volatile boolean autopairInitialized = false;

    /**
     * 获取已初始化的 LineReader 实例
     * 首次调用时会延迟启用 AutopairWidgets（需要在 readLine 上下文之外安全初始化）
     */
    public static LineReader getLineReader() {
        if (!autopairInitialized && reader != null && currentMode != TerminalMode.DUMB) {
            autopairInitialized = true;
            try {
                new org.jline.widget.AutopairWidgets(reader).enable();
            } catch (IllegalStateException e) {
                // AutopairWidgets.enable() 内部调用 callWidget()，
                // 某些 JLine 版本要求必须在 readLine() 调用期间才能调用。
                // 非致命错误，降级为不启用自动括号配对即可。
                autopairInitialized = false; // 允许下次重试
            }
        }
        return reader;
    }

    /**
     * 检查终端是否支持ANSI
     */
    private static boolean terminalSupportsAnsi(Terminal term) {
        try {
            return term.getAttributes().getOutputFlags().contains(Attributes.OutputFlag.OPOST);
        } catch (Exception e) {
            return false;
        }
    }

    private static File extractNativeLibFromApk(String apkPath, String abi, String libName) {
        String entryName = "lib/" + abi + "/" + libName;

        try (ZipFile zipFile = new ZipFile(apkPath)) {
            ZipEntry entry = zipFile.getEntry(entryName);

            // 尝试当前ABI
            if (entry == null) {
                System.out.println("[Debug] APK中未找到: " + entryName + ", 尝试其他ABI...");

                // 尝试所有支持的ABI
                String[] supportedAbis = android.os.Build.SUPPORTED_ABIS;
                for (String supportedAbi : supportedAbis) {
                    String altEntry = "lib/" + supportedAbi + "/" + libName;
                    entry = zipFile.getEntry(altEntry);
                    if (entry != null) {
                        System.out.println("[Debug] 使用备用ABI: " + supportedAbi);
                        break;
                    }
                }
            }

            if (entry == null) {
                System.out.println("[错误] APK中未找到任何匹配的native库: " + libName);
                return null;
            }

            // 确定输出目录
            File outputDir = new File("/data/local/tmp");
            if (!outputDir.exists() || !outputDir.canWrite()) {
                outputDir = DataBridge.getDataDir();
            }

            File libFile = new File(outputDir, libName);

            // 提取文件
            try (InputStream is = zipFile.getInputStream(entry);
                 FileOutputStream fos = new FileOutputStream(libFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }

            // 设置可执行权限
            libFile.setReadable(true, false);
            libFile.setExecutable(true, false);

            return libFile;

        } catch (Exception e) {
            System.out.println("[错误] 提取native库失败: " + e.getMessage());
            e.printStackTrace(System.out);
            return null;
        }
    }
}