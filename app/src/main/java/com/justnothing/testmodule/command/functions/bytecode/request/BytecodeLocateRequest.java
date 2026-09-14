package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

/**
 * 只做定位：类在哪个文件里。
 *
 * <p>单独成一个子命令是因为这一步<b>永远不会失效</b>：它只读 dex 的类名表，
 * 不依赖 vdex 格式、不需要外部二进制。提取失败时它是唯一的排查手段。</p>
 */
public class BytecodeLocateRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "className",
        position = 1,
        required = true,
        description = "类名"
    )
    private String className;

    public BytecodeLocateRequest() {
        super();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}
