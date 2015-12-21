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

    private static Context ctxt = new Context();

    public JavaScriptParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        Def funcDef = support.def(ctx.Identifier().getSymbol(), "function");
        funcDef.defKey = new DefKey(null, ctxt.getName() + "@" + ctx.Identifier().getSymbol().getText());
        support.emit(funcDef);
        Prototype p = new Prototype(ctx.Identifier().getSymbol().getText());
        p.setDef(funcDef);
        ctxt.addToCurrentScope(p);
        ctxt.pushScope(ctxt.getName() + "@" + ctx.Identifier().getSymbol().getText());
        ctxt.setProtoDecl();
        ctxt.setCurProtoDecl(p);
    }

    @Override
    public void enterFormalParameterList(JavaScriptParser.FormalParameterListContext ctx) {
        List<TerminalNode> params = ctx.Identifier();
        for (TerminalNode param : params) {
            Def paramDef = support.def(param.getSymbol(), "param");
            paramDef.defKey = new DefKey(null, ctxt.getName() + "@" + paramDef.name);
            support.emit(paramDef);
            Variable v = new Variable(param.getSymbol().getText());
            v.setDef(paramDef);
            /*if (!ctxt.isProtoDecl()) {
                ctxt.getCurMethod().addParam(v);
            }*/
            ctxt.addToCurrentScope(v);
        }
    }

    @Override
    public void enterVariableDeclaration(JavaScriptParser.VariableDeclarationContext ctx) {
        Def varDef = support.def(ctx.Identifier().getSymbol(), "var");
        varDef.defKey = new DefKey(null, ctxt.getName() + "@" + varDef.name);
        support.emit(varDef);
        Variable v = new Variable(ctx.Identifier().getSymbol().getText());
        v.setDef(varDef);
        ctxt.addToCurrentScope(v);
    }

    @Override
    public void enterIdentifierExpression(JavaScriptParser.IdentifierExpressionContext ctx) {
        SemaElement e = ctxt.find(ctx.Identifier().getSymbol().getText());
        if (e != null) {
            Ref ref = support.ref(ctx.Identifier().getSymbol());
            ref.defKey = e.getDef().defKey;
            support.emit(ref);
        }
        ctxt.setResolved(e);
    }

    @Override
    public void enterThisExpression(JavaScriptParser.ThisExpressionContext ctx) {
        if (ctxt.isProtoDecl()) {
            ctxt.setResolved(ctxt.getCurProtoDecl());
            Ref ref = support.ref(ctx.This().getSymbol());
            ref.defKey = ctxt.getCurProtoDecl().getDef().defKey;
            support.emit(ref);
        } else {
            Prototype p = ctxt.getPrototype();
            if (p != null) {
                ctxt.setResolved(p);
                Ref ref = support.ref(ctx.This().getSymbol());
                ref.defKey = p.getDef().defKey;
                support.emit(ref);
            }
        }
    }

    /*@Override
    public void exitAssignmentExpression(JavaScriptParser.AssignmentExpressionContext ctx) {
        if (ctxt.isProtoDecl()) {
            //check if left hand is this.field
            if ((ctx.singleExpression() instanceof JavaScriptParser.MemberDotExpressionContext) && (((JavaScriptParser.MemberDotExpressionContext) ctx.singleExpression()).singleExpression() instanceof JavaScriptParser.ThisExpressionContext)) {
                if (ctx.expressionSequence().singleExpression(0) instanceof JavaScriptParser.FunctionExpressionContext) {
                    //if right hand is function decl - add method
                } else {
                    //otherwise add field
                    Variable v = new Variable(((JavaScriptParser.MemberDotExpressionContext) ctx.singleExpression()).identifierName().Identifier().getSymbol().getText());
                    ctxt.getCurProtoDecl().addField(v);
                    Def def = support.def(((JavaScriptParser.MemberDotExpressionContext) ctx.singleExpression()).identifierName().Identifier().getSymbol(), "var");
                    def.defKey = new DefKey(null, ctxt.getName() + "~" + def.name);
                    support.emit(def);
                    v.setDef(def);
                }
            }
        }
    }*/

    @Override
    public void exitAssignmentExpression(JavaScriptParser.AssignmentExpressionContext ctx) {
        ctxt.unsetPrototype();
    }

    @Override
    public void exitMemberDotExpression(JavaScriptParser.MemberDotExpressionContext ctx) {
        SemaElement e = ctxt.getResolved();
        if (e != null) {
            if (e.isPrototype()) {
                Prototype p = (Prototype) e;
                SemaElement el = p.getField(ctx.identifierName().Identifier().getSymbol().getText());
                if (el != null) {
                    if (ctx.identifierName().Identifier().getSymbol().getText().equals("prototype")) {
                        ctxt.setPrototype((Prototype) el);
                    }
                    Ref ref = support.ref(ctx.identifierName().Identifier().getSymbol());
                    ref.defKey = el.getDef().defKey;
                    support.emit(ref);
                } else {
                    el = new Variable(ctx.identifierName().Identifier().getSymbol().getText());
                    p.addField(el);
                    Def def = support.def(ctx.identifierName().Identifier().getSymbol(), "var");
                    def.defKey = new DefKey(null, p.getName() + "~" + def.name);
                    support.emit(def);
                    el.setDef(def);
                }
                ctxt.setResolved(el);
            }
        }
    }

    @Override
    public void enterFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            Def funcDef = support.def(ctx.Identifier().getSymbol(), "function");
            funcDef.defKey = new DefKey(null, ctxt.getName() + "@" + funcDef.name);
            support.emit(funcDef);
            Method m = new Method(ctx.Identifier().getSymbol().getText());
            m.setDef(funcDef);
            //should we do this?
            //ctxt.addToCurrentScope(funcDef);
            ctxt.pushScope(ctxt.getName() + "@" + ctx.Identifier().getSymbol().getText());
            ctxt.addToCurrentScope(m);
        } else {
            ctxt.pushScope(ctxt.makeAnonScope(ctxt.getName()));
        }
    }

    @Override
    public void enterBlock(JavaScriptParser.BlockContext ctx) {
        ctxt.pushScope(ctxt.getName() + "@");
    }

    @Override
    public void exitBlock(JavaScriptParser.BlockContext ctx) {
        ctxt.popScope();
    }

    @Override
    public void exitFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx) {
        ctxt.popScope();
    }

    @Override
    public void exitFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        ctxt.popScope();
        ctxt.unsetProtoDecl();
    }

    @Override
    public void enterProgram(JavaScriptParser.ProgramContext ctx) {
        //support.emit();
    }


}