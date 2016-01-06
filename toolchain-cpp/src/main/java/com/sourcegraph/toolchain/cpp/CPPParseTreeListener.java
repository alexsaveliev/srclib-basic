package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.Scope;
import com.sourcegraph.toolchain.language.Variable;
import com.sourcegraph.toolchain.objc.antlr4.CPP14BaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;

import static com.sourcegraph.toolchain.objc.antlr4.CPP14Parser.*;

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

    @Override
    public void enterOriginalnamespacedefinition(OriginalnamespacedefinitionContext ctx) {
        context.enterScope(new Scope<>(ctx.Identifier().getText(), context.getPrefix(PATH_SEPARATOR)));
    }

    @Override
    public void exitOriginalnamespacedefinition(OriginalnamespacedefinitionContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterExtensionnamespacedefinition(ExtensionnamespacedefinitionContext ctx) {
        context.enterScope(new Scope<>(ctx.originalnamespacename().getText(), context.getPrefix(PATH_SEPARATOR)));
    }

    @Override
    public void exitExtensionnamespacedefinition(ExtensionnamespacedefinitionContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterUnnamednamespacedefinition(UnnamednamespacedefinitionContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitUnnamednamespacedefinition(UnnamednamespacedefinitionContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterClassspecifier(ClassspecifierContext ctx) {

        ClassheadContext head = ctx.classhead();
        ParserRuleContext name = head.classheadname().classname();
        String className = name.getText();

        Scope<Variable> scope = new Scope<>(className, context.getPrefix(PATH_SEPARATOR));

        String kind = head.classkey().getText();
        Def def = support.def(name, kind);
        def.defKey = new DefKey(null, context.currentScope().getPathTo(className, PATH_SEPARATOR));
        def.format(kind, kind, DefData.SEPARATOR_SPACE);
        support.emit(def);

        BaseclauseContext base = head.baseclause();
        if (base != null) {
            emitBaseClasses(base.basespecifierlist());
        }

        context.enterScope(scope);

        String path = context.getPath(PATH_SEPARATOR);
        support.infos.setData(path, scope);
        currentClass = path;

    }

    @Override
    public void exitClassspecifier(ClassspecifierContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterSimpledeclaration(SimpledeclarationContext ctx) {
        processDeclarationVariables(ctx.initdeclaratorlist(),
                processDeclarationType(ctx));

    }

    @Override
    public void enterFunctiondefinition(FunctiondefinitionContext ctx) {
        TypespecifierContext typeCtx = getDeclTypeSpecifier(ctx.declspecifierseq());
        String returnType;
        if (typeCtx != null) {
            returnType = processTypeSpecifier(typeCtx);
        } else {
            // ctor?
            returnType = context.currentScope().getPath();
        }

        String path = context.currentScope().getPath();

        FunctionParameters params = new FunctionParameters();
        ParametersandqualifiersContext paramsCtx = getParametersAndQualifiers(ctx.declarator());
        if (paramsCtx != null) {
            processFunctionParameters(
                    paramsCtx.parameterdeclarationclause().parameterdeclarationlist(),
                    params);
        }

        ParserRuleContext ident = getIdentifier(ctx.declarator());
        Def fnDef = support.def(ident, DefKind.FUNCTION);

        String fnPath = fnDef.name + '(' + params.getSignature() + ')';
        fnDef.defKey = new DefKey(null, context.currentScope().getPathTo(fnPath, PATH_SEPARATOR));

        context.enterScope(new Scope<>(fnPath, context.currentScope().getPrefix()));


        StringBuilder repr = new StringBuilder().append('(').append(params.getRepresentation()).append(')');
        repr.append(' ').append(returnType);
        fnDef.format(StringUtils.EMPTY, repr.toString(), DefData.SEPARATOR_EMPTY);
        fnDef.defData.setKind(DefKind.FUNCTION);
        support.emit(fnDef);

        for (FunctionParameter param : params.params) {
            param.def.defKey = new DefKey(null, context.currentScope().getPathTo(param.def.name, PATH_SEPARATOR));
            support.emit(param.def);
            context.currentScope().put(param.name, new Variable(param.type));
        }
        support.infos.setProperty(path, DefKind.FUNCTION, fnPath, returnType);

    }

    @Override
    public void exitFunctiondefinition(FunctiondefinitionContext ctx) {
        context.exitScope();
    }

    /**
     * Emits base classes in "class foo: bar"
     */
    private void emitBaseClasses(BasespecifierlistContext classes) {
        if (classes == null) {
            return;
        }
        // TODO : namespaces?
        Token name = classes.basespecifier().basetypespecifier().classordecltype().classname().Identifier().getSymbol();
        Ref typeRef = support.ref(name);
        typeRef.defKey = new DefKey(null, name.getText());
        support.emit(typeRef);
        emitBaseClasses(classes.basespecifierlist());
    }

    /**
     * Handles type part of simple declaration
     */
    private String processDeclarationType(SimpledeclarationContext ctx) {
        TypespecifierContext typeSpec = getDeclTypeSpecifier(ctx.declspecifierseq());
        if (typeSpec == null) {
            return null;
        }
        return processTypeSpecifier(typeSpec);
    }

    private TypespecifierContext getDeclTypeSpecifier(DeclspecifierseqContext ctx) {
        if (ctx == null) {
            return null;
        }
        DeclspecifierContext spec = ctx.declspecifier();
        if (spec == null) {
            return null;
        }
        TypespecifierContext typeSpec = spec.typespecifier();
        if (typeSpec != null) {
            return typeSpec;
        }
        return getDeclTypeSpecifier(ctx.declspecifierseq());
    }

    /**
     * Handles type specifier
     */
    private String processTypeSpecifier(TypespecifierContext typeSpec) {
        TrailingtypespecifierContext trailingTypeSpec = typeSpec.trailingtypespecifier();
        if (trailingTypeSpec == null) {
            return null;
        }
        SimpletypespecifierContext simpleTypeSpec = trailingTypeSpec.simpletypespecifier();
        if (simpleTypeSpec == null) {
            return null;
        }
        TypenameContext typeNameSpec = simpleTypeSpec.typename();
        if (typeNameSpec == null) {
            // basic types
            return simpleTypeSpec.getText();
        }

        // TODO: namespaces
        return processDeclarationType(typeNameSpec);
    }

    /**
     * Handles type part of simple declaration
     */
    private String processDeclarationType(TypenameContext typeNameSpec) {
        ClassnameContext classnameSpec = typeNameSpec.classname();
        if (classnameSpec != null) {
            return processTypeRef(classnameSpec.Identifier());
        }
        EnumnameContext enumnameSpec = typeNameSpec.enumname();
        if (enumnameSpec != null) {
            return processTypeRef(enumnameSpec.Identifier());
        }
        TypedefnameContext typedefSpec = typeNameSpec.typedefname();
        if (typedefSpec != null) {
            return processTypeRef(typedefSpec.Identifier());
        }
        SimpletemplateidContext templateIdSpec = typeNameSpec.simpletemplateid();
        if (templateIdSpec != null) {
            return processTypeRef(templateIdSpec.templatename().Identifier());
        }
        return null;
    }

    /**
     * Emits type ref denoted by given identifier, returns type name
     */
    private String processTypeRef(TerminalNode identifier) {
        Token token = identifier.getSymbol();
        String typeName = token.getText();
        Ref typeRef = support.ref(token);
        // TODO: namespaces
        typeRef.defKey = new DefKey(null, typeName);
        support.emit(typeRef);
        return typeName;
    }

    /**
     * Handles variables in "foo bar,baz" statements
     */
    private void processDeclarationVariables(InitdeclaratorlistContext variables, String typeName) {
        if (variables == null) {
            return;
        }
        processDeclarationVariable(variables.initdeclarator(), typeName);
        processDeclarationVariables(variables.initdeclaratorlist(), typeName);
    }

    /**
     * Handles single variable in "foo bar,baz" statements
     */
    private void processDeclarationVariable(InitdeclaratorContext var, String typeName) {
        ParserRuleContext ident = getIdentifier(var.declarator());
        if (ident == null) {
            return;
        }
        Def varDef = support.def(ident, DefKind.VARIABLE);
        varDef.defKey = new DefKey(null, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));
        varDef.format(StringUtils.EMPTY, typeName == null ? StringUtils.EMPTY : typeName, DefData.SEPARATOR_SPACE);
        varDef.defData.setKind(DefKind.VARIABLE);
        context.currentScope().put(varDef.name, new Variable(varDef.name, typeName));
        support.emit(varDef);
    }

    /**
     * Extracts identifier information
     */
    private ParserRuleContext getIdentifier(DeclaratorContext ctx) {

        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getIdentifier(noPtr);
        }
        return getIdentifier(ctx.ptrdeclarator());
    }

    /**
     * Extracts identifier information
     */
    private ParserRuleContext getIdentifier(PtrdeclaratorContext ctx) {
        if (ctx == null) {
            return null;
        }
        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getIdentifier(noPtr);
        }
        return getIdentifier(ctx.ptrdeclarator());
    }

    /**
     * Extracts identifier information
     */
    private ParserRuleContext getIdentifier(NoptrdeclaratorContext ctx) {
        if (ctx == null) {
            return null;
        }
        DeclaratoridContext declId = ctx.declaratorid();
        if (declId != null) {
            return declId.idexpression();
        }
        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getIdentifier(noPtr);
        }
        return getIdentifier(ctx.ptrdeclarator());
    }

    /**
     * Extracts parameters information
     */
    private ParametersandqualifiersContext getParametersAndQualifiers(DeclaratorContext ctx) {
        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getParametersAndQualifiers(noPtr);
        }
        return getParametersAndQualifiers(ctx.ptrdeclarator());
    }

    /**
     * Extracts parameters information
     */
    private ParametersandqualifiersContext getParametersAndQualifiers(PtrdeclaratorContext ctx) {
        if (ctx == null) {
            return null;
        }
        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getParametersAndQualifiers(noPtr);
        }
        return getParametersAndQualifiers(ctx.ptrdeclarator());
    }

    /**
     * Extracts parameters information
     */
    private ParametersandqualifiersContext getParametersAndQualifiers(NoptrdeclaratorContext ctx) {
        if (ctx == null) {
            return null;
        }
        ParametersandqualifiersContext params = ctx.parametersandqualifiers();
        if (params != null) {
            return params;
        }
        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getParametersAndQualifiers(noPtr);
        }
        return getParametersAndQualifiers(ctx.ptrdeclarator());
    }

    /**
     * Collects function parameters
     */
    private void processFunctionParameters(ParameterdeclarationlistContext ctx,
                                           FunctionParameters params) {
        if (ctx == null) {
            return;
        }
        processFunctionParameters(ctx.parameterdeclaration(), params);
        processFunctionParameters(ctx.parameterdeclarationlist(), params);
    }

    /**
     * Collects function parameters. Handles single parameter
     */
    private void processFunctionParameters(ParameterdeclarationContext param,
                                           FunctionParameters params) {
        ParserRuleContext paramNameCtx = getIdentifier(param.declarator());
        TypespecifierContext paramTypeCtx = getDeclTypeSpecifier(param.declspecifierseq());
        // TODOL: namespaces
        String paramType = processTypeSpecifier(paramTypeCtx);
        Def paramDef = support.def(paramNameCtx, DefKind.ARGUMENT);
        paramDef.format(StringUtils.EMPTY, paramType, DefData.SEPARATOR_SPACE);
        paramDef.defData.setKind(DefKind.ARGUMENT);
        FunctionParameter fp = new FunctionParameter(paramDef.name,
                paramType,
                paramType + ' ' + paramDef.name,
                "_",
                paramDef);
        params.params.add(fp);
    }

    private static class FunctionParameters {
        Collection<FunctionParameter> params = new LinkedList<>();

        String getRepresentation() {
            StringBuilder ret = new StringBuilder();
            boolean first = true;
            for (FunctionParameter param : params) {
                if (!first) {
                    ret.append(", ");
                } else {
                    first = false;
                }
                ret.append(param.repr);
            }
            return ret.toString();
        }

        String getSignature() {
            StringBuilder ret = new StringBuilder();
            boolean first = true;
            for (FunctionParameter param : params) {
                if (!first) {
                    ret.append(',');
                } else {
                    first = false;
                }
                ret.append(param.signature);
            }
            return ret.toString();
        }

    }

    private static class FunctionParameter {

        String name;
        String type;
        String repr;
        String signature;
        Def def;

        FunctionParameter(String name, String type, String repr, String signature, Def def) {
            this.name = name;
            this.type = type;
            this.repr = repr;
            this.signature = signature;
            this.def = def;
        }
    }

}