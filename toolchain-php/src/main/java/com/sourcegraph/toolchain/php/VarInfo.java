package com.sourcegraph.toolchain.php;

/**
 * Variable information
 */
class VarInfo {

    String type;
    boolean local;

    VarInfo(String type) {
        this(type, true);
    }

    VarInfo(String type, boolean local) {
        this.type = type;
        this.local = local;
    }

}