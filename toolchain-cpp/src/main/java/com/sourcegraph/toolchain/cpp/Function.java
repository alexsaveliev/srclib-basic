package com.sourcegraph.toolchain.cpp;

/**
 * Function information
 */
class Function {

    /**
     * Signature (so far it looks like foo(_,_,_) (underscore for each parameter)
     */
    String signature;

    /**
     * Return type
     */
    String type;

    /**
     * Function's def path prefix, for example if A extends B (which defines f()) and there is a call A::f() then
     * prefix is "B."
     */
    String prefix;

    Function(String signature, String type, String prefix) {
        this.signature = signature;
        this.type = type;
        this.prefix = prefix;
    }
}
