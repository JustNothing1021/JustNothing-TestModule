package com.justnothing.testmodule.command.functions.didyouknow.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.didyouknow.CliTips;
import com.justnothing.testmodule.command.functions.didyouknow.request.DidYouKnowRequest;
import com.justnothing.testmodule.command.functions.didyouknow.response.DidYouKnowResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.didyouknow.DidYouKnowTexts;
import com.justnothing.testmodule.utils.format.TerminalFormatter;

import java.util.ArrayList;
import java.util.List;

@SubCommandInfo(
    description = DidYouKnowTexts.SUB_DID_YOU_KNOW_DESC,
    usage = "did-you-know [options]",
    examples = {
        "did-you-know",              // 随机一条（蓝色）
        "did-you-know -i 7",         // 第 7 条
        "did-you-know -l",           // 列出所有
        "did-you-know -c",           // 统计信息
        "did-you-know -s 启动",      // 搜索关键词
        "did-you-know --special",    // 今日特殊提示
        "did-you-know --special-list" // 所有特殊提示
    },
    optionsDesc = DidYouKnowTexts.SUB_DID_YOU_KNOW_OPTIONS
)
public class DidYouKnowCommand extends AbstractCommand<DidYouKnowRequest, DidYouKnowResult> {

    /** 盒子宽度（终端列数，不含边框字符） */
    private static final int BOX_WIDTH = 100;

    public DidYouKnowCommand() {
        super("did-you-know", DidYouKnowRequest.class, DidYouKnowResult.class);
    }

    @Override
    protected DidYouKnowResult executeInternal(CommandExecutor.CmdExecContext<DidYouKnowRequest> context) throws Exception {
        DidYouKnowRequest request = context.getCommandRequest();

        if (request.isShowHelp()) {
            context.println(getHelpText(), Colors.CYAN);
            return ok(Text.zhEn("帮助信息", "Help").text());
        }
        if (request.isShowCount()) return showCount(context);
        if (request.isShowList()) return showList(context);
        if (request.isShowSpecial()) return showSpecial(context);
        if (request.isShowSpecialList()) return showSpecialList(context);
        if (request.getSearchKeyword() != null && !request.getSearchKeyword().isEmpty()) {
            return search(context, request.getSearchKeyword());
        }
        if (request.getTipIndex() != null) return showById(context, request.getTipIndex());

        // 默认：从所有提示中随机选一条
        return showRandom(context);
    }

    // ==================== 子命令实现 ====================

    private DidYouKnowResult showRandom(CommandExecutor.CmdExecContext<?> ctx) {
        CliTips.TipEntry tip = CliTips.randomTip();
        int idx = findTipIndex(tip);
        printTipCard(ctx, tip, Colors.CYAN, false, idx);
        return ok(Text.zhEn("随机提示", "Random tip").text(),
                tip.content, tip.author, false, tip.isCliExclusive);
    }

    private DidYouKnowResult showById(CommandExecutor.CmdExecContext<?> ctx, int id) {
        CliTips.TipEntry tip = CliTips.getTip(id);
        if (tip == null) {
            int total = CliTips.totalCount();
            ctx.println(Text.zhEn("[错误] 找不到第 %s 条提示（总共 %s 条）",
                    "[Error] No tip found with index %s (out of %s)").format(id, total), Colors.RED);
            throw new IllegalCommandLineArgumentException(
                    Text.zhEn("索引越界: #%s", "Index out of range: #%s").format(id));
        }
        printTipCard(ctx, tip, Colors.CYAN, false, id);
        DidYouKnowResult r = ok(Text.zhEn("指定提示 #%s", "Tip #%s").format(id),
                tip.content, tip.author, false, tip.isCliExclusive);
        r.setTipIndex(id);
        return r;
    }

    private DidYouKnowResult showList(CommandExecutor.CmdExecContext<?> ctx) {
        List<CliTips.TipEntry> all = CliTips.allTips();
        int total = all.size();

        ctx.println("", Colors.WHITE);

        for (int i = 0; i < all.size(); i++) {
            CliTips.TipEntry tip = all.get(i);
            String title = "#" + (i + 1) + "/" + total;
            String footer = buildFooter(tip, i + 1, total);
            String boxStr = TerminalFormatter.box(title, tip.content, footer, BOX_WIDTH);
            ctx.println(boxStr, tip.isCliExclusive ? Colors.MAGENTA : Colors.CYAN);
            if (i < all.size() - 1) ctx.println("", Colors.WHITE);
        }

        DidYouKnowResult r = ok(Text.zhEn("列出 %s 条提示", "Listed %s tips").format(total));
        r.setTotalCount(total);
        r.setAppTipCount(CliTips.getAppTipCount());
        r.setCliTipCount(CliTips.getCliExclusiveCount());
        return r;
    }

    private DidYouKnowResult showCount(CommandExecutor.CmdExecContext<?> ctx) {
        int app = CliTips.getAppTipCount();
        int cli = CliTips.getCliExclusiveCount();
        int total = CliTips.totalCount();

        List<TerminalFormatter.InfoRow> rows = new ArrayList<>();
        rows.add(new TerminalFormatter.InfoRow(Text.zhEn("App 端共享:   ", "App shared:   ").text(),
                DidYouKnowTexts.VALUE_TIP_COUNT.format(app)));
        rows.add(new TerminalFormatter.InfoRow(Text.zhEn("CLI 专属:     ", "CLI-only:     ").text(),
                DidYouKnowTexts.VALUE_TIP_COUNT.format(cli)));
        rows.add(new TerminalFormatter.InfoRow("───────────── ", "──────────"));
        rows.add(new TerminalFormatter.InfoRow(Text.zhEn("总计:         ", "Total:        ").text(),
                DidYouKnowTexts.VALUE_TIP_COUNT.format(total)));

        CliTips.TipEntry special = CliTips.getSpecialForToday();
        if (special != null) {
            rows.add(new TerminalFormatter.InfoRow("───────────── ", "──────────"));
            rows.add(new TerminalFormatter.InfoRow(Text.zhEn("[*] 今日特殊:  ", "[*] Today's special:  ").text(),
                    truncate(special.content, 24)));
        } else {
            rows.add(new TerminalFormatter.InfoRow("───────────── ", "──────────"));
            rows.add(new TerminalFormatter.InfoRow(Text.zhEn("[ ] 今日无特殊 ", "[ ] No special today ").text(), ""));
        }

        String panel = TerminalFormatter.infoPanel(
                Text.zhEn("提示系统统计", "Tip system statistics").text(), rows, BOX_WIDTH);
        ctx.println("", Colors.WHITE);
        ctx.println(panel, Colors.CYAN);

        DidYouKnowResult r = ok(Text.zhEn("统计信息", "Statistics").text());
        r.setTotalCount(total);
        r.setAppTipCount(app);
        r.setCliTipCount(cli);
        r.setHasSpecialToday(special != null);
        return r;
    }

    private DidYouKnowResult search(CommandExecutor.CmdExecContext<?> ctx, String keyword) {
        List<CliTips.TipEntry> results = CliTips.search(keyword);

        ctx.println("", Colors.WHITE);

        if (results.isEmpty()) {
            String noResult = TerminalFormatter.box(
                    Text.zhEn("[搜索结果]", "[Search results]").text(),
                    Text.zhEn("没有找到包含 \"%s\" 的提示", "No tips found containing \"%s\"").format(keyword),
                    Text.zhEn("提示: 尝试其他关键词或用 did-you-know --list",
                            "Tip: try another keyword or use did-you-know --list").text(),
                    BOX_WIDTH);
            ctx.println(noResult, Colors.YELLOW);
            return ok(Text.zhEn("无搜索结果", "No search results").text());
        }

        ctx.println(Text.zhEn("[搜索] 关键字: \"%s\" — 找到 %s 条",
                "[Search] Keyword: \"%s\" — %s found").format(keyword, results.size()), Colors.CYAN);
        ctx.println("", Colors.WHITE);

        for (int i = 0; i < results.size(); i++) {
            CliTips.TipEntry tip = results.get(i);
            int globalIdx = findTipIndex(tip);
            String title = Text.zhEn("#%s (匹配 %s/%s)", "#%s (match %s/%s)")
                    .format(globalIdx, i + 1, results.size());
            String footer = buildFooter(tip, globalIdx, CliTips.totalCount());
            String boxStr = TerminalFormatter.box(title, tip.content, footer, BOX_WIDTH);
            ctx.println(boxStr, tip.isCliExclusive ? Colors.MAGENTA : Colors.CYAN);
            if (i < results.size() - 1) ctx.println("", Colors.WHITE);
        }

        DidYouKnowResult r = ok(Text.zhEn("找到 %s 条结果", "Found %s results").format(results.size()));
        r.setSearchResultCount(results.size());
        return r;
    }

    /**
     * 显示今日特殊提示。
     */
    private DidYouKnowResult showSpecial(CommandExecutor.CmdExecContext<?> ctx) {
        CliTips.TipEntry special = CliTips.getSpecialForToday();

        ctx.println("", Colors.WHITE);

        if (special == null) {
            String noSpecial = TerminalFormatter.box(
                    Text.zhEn("[今日特殊]", "[Today's special]").text(),
                    Text.zhEn("今天没有什么特殊的~\n就是普普通通的一天",
                            "Nothing special today~\njust an ordinary day").text(),
                    Text.zhEn("用 did-you-know 随机看看别的吧",
                            "Use did-you-know to browse something else at random").text(),
                    BOX_WIDTH);
            ctx.println(noSpecial, Colors.YELLOW);
            return ok(Text.zhEn("今日无特殊提示", "No special tip today").text());
        }

        printTipCard(ctx, special, Colors.MAGENTA, true, findTipIndex(special));

        DidYouKnowResult r = ok(Text.zhEn("今日特殊提示", "Today's special tip").text());
        r.setSpecial(true);
        r.setTipContent(special.content);
        r.setTipAuthor(special.author);
        return r;
    }

    /**
     * 列出所有特殊提示（节日、生日等）。
     */
    private DidYouKnowResult showSpecialList(CommandExecutor.CmdExecContext<?> ctx) {
        List<CliTips.TipEntry> specials = CliTips.getAllSpecialTips();

        ctx.println("", Colors.WHITE);

        if (specials.isEmpty()) {
            ctx.println(Text.zhEn("[信息] 当前没有已注册的特殊提示",
                    "[Info] No special tips are registered yet").text(), Colors.YELLOW);
            return ok(Text.zhEn("无特殊提示列表", "No special tips").text());
        }

        ctx.println(Text.zhEn("[特殊提示] 共 %s 条", "[Special tips] %s in total").format(specials.size()), Colors.MAGENTA);
        ctx.println("", Colors.WHITE);

        for (int i = 0; i < specials.size(); i++) {
            CliTips.TipEntry tip = specials.get(i);
            String title = Text.zhEn("* 特殊 #%s/%s *", "* Special #%s/%s *").format(i + 1, specials.size());
            String footer = "-- " + tip.author
                    + (tip.isCliExclusive ? " [CLI]" : "");
            String boxStr = TerminalFormatter.box(title, tip.content, footer, BOX_WIDTH);
            ctx.println(boxStr, Colors.MAGENTA);
            if (i < specials.size() - 1) ctx.println("", Colors.WHITE);
        }

        DidYouKnowResult r = ok(Text.zhEn("列出 %s 条特殊提示", "Listed %s special tips").format(specials.size()));
        r.setSpecial(true);
        return r;
    }

    // ==================== 卡片绘制 ====================

    /**
     * 绘制单张提示卡片。
     *
     * @param ctx      执行上下文
     * @param tip      提示数据
     * @param color    输出颜色
     * @param isSpecial 是否为特殊提示
     * @param index    全局索引（1-based），-1 表示未知
     */
    private void printTipCard(CommandExecutor.CmdExecContext<?> ctx,
                              CliTips.TipEntry tip, byte color,
                              boolean isSpecial, int index) {
        String title = isSpecial ? Text.zhEn("* 今日特别 *", "* Today's Special *").text()
                : Text.zhEn("> 你知道吗 <", "> Did You Know <").text();
        String footer = buildFooterSingleLine(tip, index, CliTips.totalCount());

        String boxStr = TerminalFormatter.box(title, tip.content + "\n", footer, BOX_WIDTH);

        ctx.println("", Colors.WHITE);
        ctx.println(boxStr, color);
        ctx.println("", Colors.WHITE);
    }

    /**
     * 构建单行底部信息：作者 + 索引，始终在一行内完成。
     * 格式:  ---- 作者名 [CLI]          (第N/M条)
     */
    private String buildFooterSingleLine(CliTips.TipEntry tip, int index, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- ").append(tip.author);
        if (tip.isCliExclusive) sb.append(" [CLI]");

        // 计算剩余空间用于右对齐索引
        String authorPart = sb.toString();
        int usedWidth = TerminalFormatter.displayWidth(authorPart);
        int available = BOX_WIDTH - TerminalFormatter.DEFAULT_PADDING * 2 - usedWidth - 2; // -2 for safety margin

        if (index > 0 && total > 0 && available >= 10) {
            String indexStr = Text.zhEn("(第%s/%s条)", "(tip %s/%s)").format(index, total);
            sb.append(TerminalFormatter.spaces(available - TerminalFormatter.displayWidth(indexStr)));
            sb.append(indexStr);
        }

        return sb.toString();
    }

    /**
     * 构建底部信息行（用于 list/search 等多卡片场景）。
     * 始终保持单行。
     */
    private String buildFooter(CliTips.TipEntry tip, int index, int total) {
        return buildFooterSingleLine(tip, index, total);
    }

    private int findTipIndex(CliTips.TipEntry target) {
        List<CliTips.TipEntry> all = CliTips.allTips();
        for (int i = 0; i < all.size(); i++) {
            CliTips.TipEntry t = all.get(i);
            if (t.content.equals(target.content) && t.author.equals(target.author)) {
                return i + 1; // 1-based
            }
        }
        return -1;
    }

    // ==================== 辅助工厂方法 ====================

    private static DidYouKnowResult ok(String msg) {
        DidYouKnowResult r = new DidYouKnowResult();
        r.setMessage(msg);
        return r;
    }

    private static DidYouKnowResult ok(String msg, String content, String author,
                                        boolean isSpecial, boolean isCliExclusive) {
        DidYouKnowResult r = new DidYouKnowResult();
        r.setMessage(msg);
        r.setTipContent(content);
        r.setTipAuthor(author);
        r.setSpecial(isSpecial);
        r.setCliExclusive(isCliExclusive);
        return r;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }
}
