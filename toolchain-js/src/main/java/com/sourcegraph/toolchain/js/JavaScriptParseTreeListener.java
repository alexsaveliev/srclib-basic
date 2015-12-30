package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptBaseListener;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;


class JavaScriptParseTreeListener extends JavaScriptBaseListener {

    private LanguageImpl support;

    private static Context ctxt = new Context();

    public JavaScriptParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx) {
        Token t = ctx.Identifier().getSymbol();
        String ident = t.getText();
        Def funcDef = support.def(t, "function");
        funcDef.defKey = new DefKey(null, ctxt.getName() + '@' + ident);
        support.emit(funcDef);
        Prototype p = new Prototype(ident);
        p.setDef(funcDef);
        ctxt.addToCurrentScope(p);
        ctxt.pushScope(ctxt.getName() + '@' + ident);
        ctxt.setCurProtoDecl(p);
    }

    @Override
    public void enterFormalParameterList(JavaScriptParser.FormalParameterListContext ctx) {
        List<TerminalNode> params = ctx.Identifier();
        for (TerminalNode param : params) {
            Def paramDef = support.def(param.getSymbol(), "param");
            paramDef.defKey = new DefKey(null, ctxt.getName() + '@' + paramDef.name);
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
        Token t = ctx.Identifier().getSymbol();
        String ident = t.getText();
        Def varDef = support.def(t, "var");
        varDef.defKey = new DefKey(null, ctxt.getName() + '@' + varDef.name);
        support.emit(varDef);
        Variable v = new Variable(ident);
        v.setDef(varDef);
        ctxt.addToCurrentScope(v);
    }

    @Override
    public void enterIdentifierExpression(JavaScriptParser.IdentifierExpressionContext ctx) {
        Token t = ctx.Identifier().getSymbol();
        String ident = t.getText();
        SemaElement e = ctxt.find(ident);
        if (e != null) {
            Ref ref = support.ref(t);
            ref.defKey = e.getDef().defKey;
            support.emit(ref);
        }
        ctxt.setResolved(e);
    }

    @Override
    public void enterThisExpression(JavaScriptParser.ThisExpressionContext ctx) {
        Prototype protodecl = ctxt.getCurProtoDecl();
        if (protodecl != null) {
            ctxt.setResolved(protodecl);
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

    @Override
    public void exitAssignmentExpression(JavaScriptParser.AssignmentExpressionContext ctx) {
        ctxt.setPrototype(null);
    }

    @Override
    public void exitMemberDotExpression(JavaScriptParser.MemberDotExpressionContext ctx) {
        SemaElement e = ctxt.getResolved();
        if (e != null) {
            if (e.isPrototype()) {
                Prototype p = (Prototype) e;
                Token t = ctx.identifierName().Identifier().getSymbol();
                String ident = t.getText();
                SemaElement el = p.getField(ident);
                if (el != null) {
                    if (ident.equals("prototype")) {
                        ctxt.setPrototype((Prototype) el);
                    }
                    Ref ref = support.ref(t);
                    ref.defKey = el.getDef().defKey;
                    support.emit(ref);
                } else {
                    el = new Variable(ident);
                    p.addField(el);
                    Def def = support.def(t, "var");
                    def.defKey = new DefKey(null, p.getName() + '@' + def.name);
                    support.emit(def);
                    el.setDef(def);
                }
                ctxt.setResolved(el);
            }
        } else {
            SemaElement el = ctxt.find(ctx.identifierName().Identifier().getSymbol().getText());
            if (el != null) {
                Ref ref = support.ref(ctx.identifierName().Identifier().getSymbol());
                ref.defKey = el.getDef().defKey;
                support.emit(ref);
            }
        }
    }

    @Override
    public void enterFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            Token t = ctx.Identifier().getSymbol();
            String funcname = t.getText();
            Def funcDef = support.def(t, "function");
            funcDef.defKey = new DefKey(null, ctxt.getName() + '@' + funcDef.name);
            support.emit(funcDef);
            Method m = new Method(funcname);
            m.setDef(funcDef);
            //should we do this?
            //ctxt.addToCurrentScope(funcDef);
            ctxt.pushScope(ctxt.getName() + '@' + funcname);
            ctxt.addToCurrentScope(m);
        } else if (ctxt.getPropertyToken() != null) {
            Def funcDef = support.def(ctxt.getPropertyToken(), "function");
            funcDef.defKey = new DefKey(null, ctxt.getName() + "@" + ctxt.getPropertyToken().getText());
            support.emit(funcDef);
            Method m = new Method(ctxt.getPropertyToken().getText());
            m.setDef(funcDef);
            ctxt.addToCurrentScope(m);
            ctxt.pushScope(ctxt.getName() + "@" + ctxt.getPropertyToken().getText());
            Prototype p = ctxt.getPrototype();
            if (p != null) {
                p.addField(m);
            }
        } else {
            ctxt.pushScope(ctxt.makeAnonScope(ctxt.getName()));
        }
    }

    @Override
    public void enterBlock(JavaScriptParser.BlockContext ctx) {
        ctxt.pushScope(ctxt.getName() + '@');
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
        ctxt.setCurProtoDecl(null);
    }

    @Override
    public void exitStatement(JavaScriptParser.StatementContext ctx) {
        ctxt.setResolved(null);
    }

    @Override
    public void enterPropertyExpressionAssignment(JavaScriptParser.PropertyExpressionAssignmentContext ctx) {
        if (ctx.propertyName() != null) {
            if (ctx.propertyName().identifierName() != null) {
                if (ctx.propertyName().identifierName().Identifier() != null) {
                    ctxt.setPropertyToken(ctx.propertyName().identifierName().Identifier().getSymbol());
                }
            }
        }
    }

    @Override
    public void exitPropertyExpressionAssignment(JavaScriptParser.PropertyExpressionAssignmentContext ctx) {
        ctxt.setPropertyToken(null);
    }


    @Override
    public void enterProgram(JavaScriptParser.ProgramContext ctx) {
    }


}