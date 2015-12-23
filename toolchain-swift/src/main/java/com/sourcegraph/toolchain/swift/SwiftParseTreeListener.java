package com.sourcegraph.toolchain.swift;

import com.sourcegraph.toolchain.swift.antlr4.SwiftBaseListener;

class SwiftParseTreeListener extends SwiftBaseListener {

    private LanguageImpl support;

    SwiftParseTreeListener(LanguageImpl support) {
        this.support = support;
    }
}