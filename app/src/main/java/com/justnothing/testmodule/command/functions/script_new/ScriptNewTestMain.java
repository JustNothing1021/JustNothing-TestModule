package com.justnothing.testmodule.command.functions.script_new;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

public class ScriptNewTestMain extends CommandBase {

    public ScriptNewTestMain() {
        super("ScriptNewTest");
    }

    @Override
    public String getHelpText() {
        return "Test the script_new architecture";
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        try {
            SimpleScriptTest.runTests();
            return "All tests passed!";
        } catch (Exception e) {
            return "Test failed: " + e.getMessage();
        }
    }
}
