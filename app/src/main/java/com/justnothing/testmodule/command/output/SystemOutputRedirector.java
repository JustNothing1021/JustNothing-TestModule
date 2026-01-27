package com.justnothing.testmodule.command.output;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;


public class SystemOutputRedirector {
    private final IOutputHandler output;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream redirectedOut;
    private PrintStream redirectedErr;

    public SystemOutputRedirector(IOutputHandler output) {
        this.output = output;
    }


    public void startRedirect() {
        originalOut = System.out;
        originalErr = System.err;

        redirectedOut = new PrintStream(new OutputStream() {

            @Override
            public void write(byte[] b, int off, int len) {
                output.print(new String(b, off, len));
            }

            @Override
            public void write(int b) throws IOException {
                output.print(((char) b) + "");
            }

            @Override
            public void flush() {
                output.flush();
            }
        }, true);

        redirectedErr = redirectedOut;

        System.setOut(redirectedOut);
        System.setErr(redirectedErr);
    }

    /**
     * 停止重定向，恢复原始输出
     */
    public void stopRedirect() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }

        if (redirectedOut != null) {
            redirectedOut.close();
        }
    }
}