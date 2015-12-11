package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptBaseListener;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptLexer;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class JavaScriptParseTreeListener extends JavaScriptBaseListener {

    private LanguageImpl support;

    public JavaScriptParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        Def funcDef = support.def(ctx.Identifier().getSymbol(), "function");
        funcDef.defKey = new DefKey(null, funcDef.name);
        support.emit(funcDef);
    }

    @Override
    public void enterProgram(JavaScriptParser.ProgramContext ctx) {
    }


}