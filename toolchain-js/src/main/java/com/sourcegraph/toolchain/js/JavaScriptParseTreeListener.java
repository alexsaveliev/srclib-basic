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

    private Context ctxt;

    public JavaScriptParseTreeListener(LanguageImpl support) {
        this.support = support;
        this.ctxt = new Context();
        ctxt.pushScope(); //global scope
    }

    @Override
    public void enterFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        Def funcDef = support.def(ctx.Identifier().getSymbol(), "function");
        funcDef.defKey = new DefKey(null, funcDef.name);
        support.emit(funcDef);
        ctxt.addToCurrentScope(funcDef);
        ctxt.pushScope();
    }

    @Override
    public void enterFormalParameterList(JavaScriptParser.FormalParameterListContext ctx) {
        List<TerminalNode> params = ctx.Identifier();
        for (TerminalNode param : params) {
            Def paramDef = support.def(param.getSymbol(), "param");
            paramDef.defKey = new DefKey(null, paramDef.name);
            support.emit(paramDef);
            ctxt.addToCurrentScope(paramDef);
        }
    }

    @Override
    public void enterVariableDeclaration(JavaScriptParser.VariableDeclarationContext ctx) {
        Def varDef = support.def(ctx.Identifier().getSymbol(), "var");
        varDef.defKey = new DefKey(null, varDef.name);
        support.emit(varDef);
        ctxt.addToCurrentScope(varDef);
    }

    @Override
    public void enterIdentifierExpression(JavaScriptParser.IdentifierExpressionContext ctx) {
        Def d = ctxt.find(ctx.Identifier().getSymbol().getText());
        if (d != null) {
            Ref ref = support.ref(ctx.Identifier().getSymbol());
            ref.defKey = d.defKey;
            support.emit(ref);
        }
    }

    @Override
    public void enterFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            Def funcDef = support.def(ctx.Identifier().getSymbol(), "function");
            funcDef.defKey = new DefKey(null, funcDef.name);
            support.emit(funcDef);
            //should we do this?
            //ctxt.addToCurrentScope(funcDef);
        }
        ctxt.pushScope();
    }

    @Override
    public void enterBlock(JavaScriptParser.BlockContext ctx) {
        ctxt.pushScope();
    }

    @Override
    public void exitBlock(JavaScriptParser.BlockContext ctx) {
        ctxt.pushScope();
    }

    @Override
    public void exitFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx) {
        ctxt.popScope();
    }

    @Override
    public void exitFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        ctxt.popScope();
    }

    @Override
    public void enterProgram(JavaScriptParser.ProgramContext ctx) {
        //support.emit();
    }


}