package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

/**
 * 按关键词模糊搜索类名。
 *
 * <p>和 {@code list_classes} 互补：那个回答"这个来源里有哪些类"，这个回答
 * "哪儿有名字长这样的类" —— 逆向时通常是后者先需要。</p>
 */
public class BytecodeFindRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "keyword",
        required = true,
        description = "类名关键词，不区分大小写，匹配类名的任意片段（如 ActivityManager、com.xtc）"
    )
    private String keyword;

    @CmdParam(
        name = "source",
        aliases = {"-s", "--source"},
        required = false,
        description = "只在这个来源里搜（apk/jar/dex/vdex/odex）；不填则搜本进程所有代码来源"
    )
    private String source;

    @CmdParam(
        name = "limit",
        aliases = {"-l", "--limit"},
        required = false,
        defaultValue = "50",
        description = "最多列出多少个命中"
    )
    private int limit = 50;

    public BytecodeFindRequest() {
        super();
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
