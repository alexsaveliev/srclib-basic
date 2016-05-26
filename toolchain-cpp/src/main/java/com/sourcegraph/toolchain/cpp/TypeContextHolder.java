package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.cpp.antlr4.CPP14Parser;

/**
 * Holder for misc type specifiers (type, enum, ...)
 */
class TypeContextHolder {

    CPP14Parser.TypespecifierContext typeSpecifier;
    CPP14Parser.EnumspecifierContext enumspecifier;

    TypeContextHolder(CPP14Parser.TypespecifierContext typeSpecifier) {
        this.typeSpecifier = typeSpecifier;
    }

    TypeContextHolder(CPP14Parser.EnumspecifierContext enumspecifier) {
        this.enumspecifier = enumspecifier;
    }

}
