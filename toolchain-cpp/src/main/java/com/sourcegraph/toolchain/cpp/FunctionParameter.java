package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.core.objects.Def;

/**
 * Single function parameter
 */
class FunctionParameter {

    /**
     * Parameter's name
     */
    String name;
    /**
     * Parameter's type (noptr)
     */
    String type;
    /**
     * Parameter's display representation
     */
    String repr;
    /**
     * Parameter's signature (_)
     */
    String signature;
    /**
     * Parameter def's object
     */
    Def def;

    FunctionParameter(String name, String type, String repr, String signature, Def def) {
        this.name = name;
        this.type = type;
        this.repr = repr;
        this.signature = signature;
        this.def = def;
    }

}
