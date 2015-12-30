package com.sourcegraph.toolchain.language;

public class LookupResult {

    private String value;
    private Scope scope;
    private int depth;

    public LookupResult(String value, Scope scope, int depth) {
        this.value = value;
        this.scope = scope;
        this.depth = depth;
    }

    public String getValue() {
        return value;
    }

    public Scope getScope() {
        return scope;
    }

    public int getDepth() {
        return depth;
    }
}
