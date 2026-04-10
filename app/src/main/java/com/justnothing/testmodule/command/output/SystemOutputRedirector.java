package com.justnothing.testmodule.command.output;

import java.io.OutputStream;
import java.io.PrintStream;


public class SystemOutputRedirector {
    private final ICommandOutputHandler outputHandler;
    private final ICommandOutputHandler errorHandler;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream redirectedOut;
    private PrintStream redirectedErr;

    public SystemOutputRedirector(ICommandOutputHandler output) {
        this.outputHandler = output;
        this.errorHandler = output;
    }

    public SystemOutputRedirector(ICommandOutputHandler output, ICommandOutputHandler error) {
        this.outputHandler = output;
        this.errorHandler = error;
    }


    public void startRedirect() {
        originalOut = System.out;
        originalErr = System.err;

        redirectedOut = new PrintStream(new OutputStream() {

            @Override
            public void write(byte[] b, int off, int len) {
                outputHandler.print(new String(b, off, len));
            }

            @Override
            public void write(int b) {
                outputHandler.print(((char) b) + "");
            }

            @Override
            public void flush() {
                outputHandler.flush();
            }
        }, true);

        redirectedErr = new PrintStream(new OutputStream() {

            @Override
            public void write(byte[] b, int off, int len) {
                errorHandler.print(new String(b, off, len));
            }

            @Override
            public void write(int b) {
                errorHandler.print(((char) b) + "");
            }

            @Override
            public void flush() {
                errorHandler.flush();
            }
        }, true);

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

        if (redirectedErr != null) {
            redirectedErr.close();
        }
    }
}