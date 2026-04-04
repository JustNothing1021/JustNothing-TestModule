package com.justnothing.methodsclient.executor;

public record ColoredSegment(byte color, String text) {

    @Override
    public String toString() {
        return "ColoredSegment[color=" + (int) color + ", text='" + text + "']";
    }
}
