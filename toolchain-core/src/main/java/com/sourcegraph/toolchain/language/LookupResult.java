package com.sourcegraph.toolchain.language;

public class LookupResult {

    private String value;
    private int depth;

    public LookupResult(String value, int depth) {
        this.value = value;
        this.depth = depth;
    }

    public String getValue() {
        return value;
    }

    public int getDepth() {
        return depth;
    }
}
