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
        DeclspecifierContext spec = ctx.declspecifierseq().declspecifier();
        TypespecifierContext typeSpec = spec.typespecifier();
        if (typeSpec == null) {
            return null;
        }
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
        DeclaratorContext decl = var.declarator();
        PtrdeclaratorContext ptr = decl.ptrdeclarator();
        if (ptr == null) {
            return;
        }
        NoptrdeclaratorContext noPtr = ptr.noptrdeclarator();
        if (noPtr == null) {
            return;
        }
        DeclaratoridContext declId = noPtr.declaratorid();
        if (declId == null) {
            return;
        }
        Def varDef = support.def(declId.idexpression(), DefKind.VARIABLE);
        varDef.defKey = new DefKey(null, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));
        varDef.format(StringUtils.EMPTY, typeName == null ? StringUtils.EMPTY : typeName, DefData.SEPARATOR_SPACE);
        varDef.defData.setKind(DefKind.VARIABLE);
        context.currentScope().put(varDef.name, new Variable(varDef.name, typeName));
        support.emit(varDef);
    }

}