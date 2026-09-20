package com.justnothing.testmodule.command.functions.didyouknow;

import com.justnothing.testmodule.utils.tips.SimpleTipCallback;
import com.justnothing.testmodule.utils.tips.SpecialTipCallback;
import com.justnothing.testmodule.utils.tips.TipCallback;
import com.justnothing.testmodule.utils.tips.TipSystem;
import com.justnothing.testmodule.utils.tips.lang.ChineseTips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * CLI 版"你知道吗"提示的数据桥接层。
 * <p>
 * 数据来源有两个：
 * <ol>
 *   <li><b>App 端共享数据</b> — 通过 {@link TipSystem} 复用 {@link ChineseTips} 中
 *       的所有 DidYouKnow 提示和特殊日期提示，避免重复维护</li>
 *   <li><b>CLI 专属彩蛋</b> — 终端/REPL/命令系统相关的趣味内容，
 *       只在 CLI 中显示</li>
 * </ol>
 */
public class CliTips {

    private CliTips() {}  // 工具类

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    /** 共享的 TipSystem 实例（自动根据 Locale 选择中文/英文） */
    private static final TipSystem TIP_SYSTEM = new TipSystem();

    /** CLI 专属彩蛋（终端相关内容，不在 App 端显示） */
    private static final List<TipEntry> CLI_EXCLUSIVE_TIPS = new ArrayList<>();

    static {
        initCliExclusiveTips();
    }

    // ==================== CLI 专属彩蛋初始化 ====================

    private static void initCliExclusiveTips() {
        // 也许以后会考虑的...?
        
        // String glmName = "GLM AI Assistant";

        // cli("你知道吗？你正在使用的终端端口默认是 11451 —— 这数字看起来有点眼熟？", glmName);
        // cli("Tab 补全不是魔法，是 JLine3 库在背后默默工作", glmName);
        // cli("每一条命令都会新建一个 Socket 连接，用完就关。就像一次性筷子一样环保（bushi）", glmName);
        // cli("你的上下左右方向键被重新绑定了，因为 Android 终端的转义序列和标准 VT100 不一样", glmName);
        // cli("TailTipWidgets 会在你首次按 Tab 时才启用 —— 这叫懒加载，一种优雅的 procrastination", glmName);
        // cli("这个 REPL 的欢迎界面用了 Unicode 盒画字符，如果你的终端不支持就会乱码（那你现在看到了吗？）", glmName);
        // cli("Ctrl+R 可以搜索历史命令，这是 readline 时代遗留下来的宝藏功能", glmName);
        // cli("你知道为什么 prompt 是绿色的 methods> 吗？因为绿色代表 Go... 我是说代表「可以输入了」", glmName);
        // cli("methods 客户端和服务端之间用的是 LocalSocket IPC，比 HTTP 快得多", glmName);
        // cli("这个命令系统的路由是用 Trie 树实现的，O(k) 复杂度查找", glmName);
        // cli("你已经在终端里待了多久了？记得眨眼 —— 这是来自 AI 的真诚建议", glmName);
        // cli("这个项目的 slogan 是\"一个啥也不是的 Xposed模块\" —— 谦虚是一种美德", glmName);
        // cli("engine_new 引擎有完整的运算符系统，支持 50+ 个测试用例。我们修了很多 bug 才做到的", glmName);
        // cli("App 端有一个\"你知道吗\"页面，有 30+ 条提示和闪烁动画。CLI 版本是其精神续作", glmName);
        // cli("如果你看到了这条，说明你已经翻了至少 20 条提示了。坚持就是胜利！", glmName);
        // cli("这个项目有 20+ 个 codebase 脚本，包括 blackjack、修仙、地牢...... 和网络监控工具", glmName);
        // cli("Magisk 模块的 post-fs-data.sh 会在每次启动时执行。小心别写死循环", glmName);
        // cli("HookManager 支持三阶段 Hook: before → after → replace。replace 是最狠的，直接替换方法体", glmName);
        // cli("NetworkInterceptor 可以同时 Hook OkHttp、HttpURLConnection 和 Retrofit。三倍快乐", glmName);
        // cli("HierarchicalSampler 用 Thread.getAllStackTraces() 采样性能数据。轻量但有效", glmName);
        // cli("如果你把所有提示都看完了，恭喜你 —— 你可能比我更闲（bushi）", glmName);
        // cli("REPL 的 ANSI 清屏命令是 \\033[H\\033[2J。现在你知道怎么清屏了", glmName);
        // cli("你用的这个 CLI 工具，其祖先是一个叫\"啥也不是的工具箱\"的 Python 脚本 —— 进化了！", glmName);
    }

    private static void cli(String content, String author) {
        CLI_EXCLUSIVE_TIPS.add(new TipEntry(content, author, true));
    }

    // ==================== 公共 API ====================

    /**
     * 获取随机一条提示（合并 App 共享数据 + CLI 专属）。
     *
     * @return 随机提示条目
     */
    public static TipEntry randomTip() {
        int appCount = getAppTipCount();
        int cliCount = CLI_EXCLUSIVE_TIPS.size();
        int total = appCount + cliCount;

        if (total == 0) {
            return new TipEntry("（暂无提示内容）", "System", false);
        }

        int idx = RANDOM.nextInt(total);
        if (idx < appCount) {
            // App 共享数据
            Map<Integer, SimpleTipCallback> map = ChineseTips.DidYouKnowTips.getDidYouKnowTips();
            List<SimpleTipCallback> ordered = new ArrayList<>(map.values());
            SimpleTipCallback cb = ordered.get(idx);
            return new TipEntry(cb.getContent(), cb.getAuthor(), false);
        } else {
            // CLI 专属
            return CLI_EXCLUSIVE_TIPS.get(idx - appCount);
        }
    }

    /**
     * 获取指定索引的提示（1-based）。
     * <p>
     * 索引 1~N 对应 App 端 DidYouKnow 提示，
     * 索引 N+1~end 对应 CLI 专属提示。
     *
     * @param index 提示序号（从 1 开始）
     * @return 提示条目，或 null 如果索引越界
     */
    public static TipEntry getTip(int index) {
        if (index < 1) return null;

        int appCount = getAppTipCount();
        if (index <= appCount) {
            Map<Integer, SimpleTipCallback> map = ChineseTips.DidYouKnowTips.getDidYouKnowTips();
            SimpleTipCallback cb = map.get(index);
            if (cb == null) return null;
            return new TipEntry(cb.getContent(), cb.getAuthor(), false);
        }

        int cliIndex = index - appCount - 1;
        if (cliIndex < CLI_EXCLUSIVE_TIPS.size()) {
            return CLI_EXCLUSIVE_TIPS.get(cliIndex);
        }
        return null;
    }

    /**
     * 获取所有提示的不可修改列表。
     *
     * @return 合并后的提示列表（App 数据在前，CLI 专属在后）
     */
    public static List<TipEntry> allTips() {
        List<TipEntry> result = new ArrayList<>();

        // App 端 DidYouKnow 数据
        Map<Integer, SimpleTipCallback> map = ChineseTips.DidYouKnowTips.getDidYouKnowTips();
        for (Map.Entry<Integer, SimpleTipCallback> entry : map.entrySet()) {
            SimpleTipCallback cb = entry.getValue();
            result.add(new TipEntry(cb.getContent(), cb.getAuthor(), false));
        }

        // CLI 专属数据
        result.addAll(CLI_EXCLUSIVE_TIPS);

        return Collections.unmodifiableList(result);
    }

    /**
     * 获取提示总数（App + CLI）。
     */
    public static int totalCount() {
        return getAppTipCount() + CLI_EXCLUSIVE_TIPS.size();
    }

    /**
     * 仅获取 App 端 DidYouKnow 提示数量。
     */
    public static int getAppTipCount() {
        return ChineseTips.DidYouKnowTips.getDidYouKnowTips().size();
    }

    /**
     * 仅获取 CLI 专属提示数量。
     */
    public static int getCliExclusiveCount() {
        return CLI_EXCLUSIVE_TIPS.size();
    }

    /**
     * 获取当前应该显示的特殊日期提示（如果有）。
     * <p>
     * 直接委托给 {@link TipSystem#getDisplayTipForWelcome()}，
     * 复用 App 端已有的节日/生日等全部特殊逻辑。
     *
     * @return 特殊提示条目，或 null 如果今天没有特殊提示
     */
    public static TipEntry getSpecialForToday() {
        TipCallback special = TIP_SYSTEM.getDisplayTipForWelcome();
        if (special == null) return null;
        return new TipEntry(special.getContent(), special.getAuthor(), false);
    }

    /**
     * 获取所有特殊提示（节日/生日等）。
     *
     * @return 不可修改的特殊提示列表
     */
    public static List<TipEntry> getAllSpecialTips() {
        List<TipEntry> result = new ArrayList<>();
        for (SpecialTipCallback cb : ChineseTips.SpecialTips.getSpecialTips()) {
            String author = cb.getAuthor();
            if (author == null || author.isEmpty()) {
                author = "TipSystem";
            }
            result.add(new TipEntry(cb.getContent(), author, false));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 搜索包含关键词的提示（搜索范围：App + CLI 全部数据）。
     *
     * @param keyword 搜索关键词（不区分大小写）
     * @return 匹配的提示列表
     */
    public static List<TipEntry> search(String keyword) {
        String lower = keyword.toLowerCase();
        List<TipEntry> results = new ArrayList<>();

        for (TipEntry tip : allTips()) {
            if (tip.content.toLowerCase().contains(lower)
                    || tip.author.toLowerCase().contains(lower)) {
                results.add(tip);
            }
        }
        return results;
    }

    // ==================== 数据类 ====================

    /** 提示条目 */
    public static class TipEntry {
        /** 提示文本内容 */
        public final String content;
        /** 作者 */
        public final String author;
        /** 是否为 CLI 专属（不在 App 端显示） */
        public final boolean isCliExclusive;

        public TipEntry(String content, String author, boolean isCliExclusive) {
            this.content = content;
            this.author = author;
            this.isCliExclusive = isCliExclusive;
        }

        @Override
        public String toString() {
            String tag = isCliExclusive ? " [CLI]" : "";
            return content + "  -- " + author + tag;
        }
    }
}
