package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.Variable;
import com.sourcegraph.toolchain.objc.antlr4.CPP14BaseListener;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Stack;

class CPPParseTreeListener extends CPP14BaseListener {

    private static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    private Context<Variable> context = new Context<>();

    private Stack<String> typeStack = new Stack<>();

    private Stack<ParserRuleContext> fnCallStack = new Stack<>();

    private String currentClass;

    private boolean isInFunction;

    CPPParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

}