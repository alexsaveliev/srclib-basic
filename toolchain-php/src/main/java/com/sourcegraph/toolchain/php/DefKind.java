package com.sourcegraph.toolchain.php;

interface DefKind {

    public static final String FUNCTION = "func";
    public static final String ARGUMENT = "argument";
    public static final String VARIABLE = "variable";
    public static final String INTERFACE = "interface";
    public static final String TRAIT = "trait";
    public static final String CLASS = "class";
    public static final String CONSTANT = "constant";
    public static final String LABEL = "label";
    public static final String METHOD = "method";
}
