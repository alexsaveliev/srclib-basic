package com.sourcegraph.toolchain.swift;

class Variable {

    private String type;
    private String path;

    Variable(String type) {
        this(type, null);
    }

    Variable(String type, String path) {
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