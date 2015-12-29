package com.sourcegraph.toolchain.swift;

interface DefKind {

    public static final String PROTOCOL = "protocol";
    public static final String STRUCT = "struct";
    public static final String CLASS = "class";
    public static final String ENUM = "enum";
    public static final String FUNC = "func";
    public static final String PARAM = "param";
    public static final String LET = "let";
    public static final String VAR = "var";
    public static final String CASE = "case";
}