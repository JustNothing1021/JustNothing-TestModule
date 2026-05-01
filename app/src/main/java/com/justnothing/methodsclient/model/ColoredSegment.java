package com.justnothing.methodsclient.model;

import androidx.annotation.NonNull;

public record ColoredSegment(byte color, String text) {

    @NonNull
    @Override
    public String toString() {
        return "ColoredSegment[color=" + (int) color + ", text='" + text + "']";
    }
}
