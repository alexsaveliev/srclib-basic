package com.sourcegraph.toolchain.language;

public class Variable {

    private String type;
    private String path;

    public Variable(String type) {
        this(type, null);
    }

    public Variable(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

}