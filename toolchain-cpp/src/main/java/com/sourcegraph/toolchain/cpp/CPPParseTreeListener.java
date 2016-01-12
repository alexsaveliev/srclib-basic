package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.cpp.antlr4.CPP14BaseListener;
import com.sourcegraph.toolchain.language.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.sourcegraph.toolchain.cpp.antlr4.CPP14Parser.*;

class CPPParseTreeListener extends CPP14BaseListener {

    private static final char PATH_SEPARATOR = '.';

    private static final String UNKNOWN = "?";

    private static final String BASE_CLASS = "baseClass";

    private LanguageImpl support;

    private Context<ObjectInfo> context = new Context<>();

    private Stack<Stack<String>> typeStack = new Stack<>();

    private Stack<ParserRuleContext> fnCallStack = new Stack<>();

    private Stack<String> classes = new Stack<>();

    private NamespaceContext namespaceContext = new NamespaceContext();

    private static int idCounter;

    CPPParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterOriginalnamespacedefinition(OriginalnamespacedefinitionContext ctx) {
        String name = ctx.Identifier().getText();
        context.enterScope(new Scope<>(name, context.currentScope().getPrefix()));
        namespaceContext.enter(name);
    }

    @Override
    public void exitOriginalnamespacedefinition(OriginalnamespacedefinitionContext ctx) {
        context.exitScope();
        namespaceContext.exit();
    }

    @Override
    public void enterExtensionnamespacedefinition(ExtensionnamespacedefinitionContext ctx) {
        String name = ctx.originalnamespacename().getText();
        context.enterScope(new Scope<>(name, context.currentScope().getPrefix()));
        namespaceContext.enter(name);
    }

    @Override
    public void exitExtensionnamespacedefinition(ExtensionnamespacedefinitionContext ctx) {
        context.exitScope();
        namespaceContext.exit();
    }

    @Override
    public void enterUnnamednamespacedefinition(UnnamednamespacedefinitionContext ctx) {
        Scope<ObjectInfo> uniq = context.currentScope().uniq(PATH_SEPARATOR);
        context.enterScope(uniq);
        namespaceContext.enter(uniq.getName());
    }

    @Override
    public void exitUnnamednamespacedefinition(UnnamednamespacedefinitionContext ctx) {
        context.exitScope();
        namespaceContext.exit();
    }

    @Override
    public void enterClassspecifier(ClassspecifierContext ctx) {

        ClassheadContext head = ctx.classhead();
        ClassheadnameContext classheadnameCtx = head.classheadname();
        if (classheadnameCtx == null) {
            // TODO typedef struct {} foo;
            context.enterScope(context.currentScope().next(PATH_SEPARATOR));
            return;
        }
        NSPath nsPath = new NSPath(classheadnameCtx);
        String className = namespaceContext.resolve(nsPath);

        Scope<ObjectInfo> scope = new Scope<>(className);

        String kind = head.classkey().getText();
        Def def = support.def(nsPath.localCtx.getSymbol(), kind);
        def.defKey = new DefKey(null, className);
        def.format(kind, kind, DefData.SEPARATOR_SPACE);
        support.emit(def);

        BaseclauseContext base = head.baseclause();
        support.infos.setData(className, scope);

        if (base != null) {
            processBaseClasses(base.basespecifierlist(), className, scope);
        }

        context.enterScope(scope);

        classes.push(className);

        // we should handle members here instead of enterMemberdeclaration()
        // because they may appear after method declaration while we need to know this info
        processMembers(ctx.memberspecification());
    }

    @Override
    public void exitClassspecifier(ClassspecifierContext ctx) {

        ClassheadContext head = ctx.classhead();
        ClassheadnameContext classheadnameCtx = head.classheadname();

        if (classheadnameCtx == null) {
            // TODO typedef struct {} foo;
            return;
        }
        context.exitScope();
        classes.pop();
    }

    @Override
    public void enterSimpledeclaration(SimpledeclarationContext ctx) {
        processVarsAndFunctions(ctx.initdeclaratorlist(),
                processDeclarationType(ctx));

    }

    @Override
    public void enterFunctiondefinition(FunctiondefinitionContext ctx) {
        TypespecifierContext typeCtx = getDeclTypeSpecifier(ctx.declspecifierseq());

        IdexpressionContext ident = getIdentifier(ctx.declarator());

        NSPath nsPath = new NSPath(ident);

        String className;

        String returnType;
        if (typeCtx != null) {
            returnType = processTypeSpecifier(typeCtx);
            if (returnType != null) {
                String localClass;
                int pos = returnType.lastIndexOf(PATH_SEPARATOR);
                if (pos > 0) {
                    localClass = returnType.substring(pos + 1);
                } else {
                    localClass = returnType;
                }
                // Grammar does not differentiate foo :bar() and foo::foo()
                if (localClass.equals(nsPath.local)) {
                    className = returnType;
                } else {
                    NSPath parent = nsPath.parent();
                    if (parent.components.isEmpty()) {
                        className = classes.isEmpty() ? StringUtils.EMPTY : classes.peek();
                    } else {
                        className = namespaceContext.resolve(parent);
                    }
                }
            } else {
                className = context.currentScope().getPath();
            }
        } else {
            returnType = className = context.currentScope().getPath();
        }

        FunctionParameters params = new FunctionParameters();
        ParametersandqualifiersContext paramsCtx = getParametersAndQualifiers(ctx.declarator());
        if (paramsCtx != null) {
            processFunctionParameters(
                    paramsCtx.parameterdeclarationclause().parameterdeclarationlist(),
                    params);
        }

        String functionName = nsPath.local;
        if (functionName == null) {
            // TODO: operators
            functionName = "**" + ++idCounter;
        }
        String functionPath = functionName + '(' + params.getSignature() + ')';

        // is it a ref or def?
        ObjectInfo info = support.infos.getProperty(className, DefKind.FUNCTION, functionPath);
        if (info != null) {
            // ref
            Ref methodRef = support.ref(nsPath.localCtx.getSymbol());
            String refPath;
            if (info.getPrefix() != null) {
                refPath = info.getPrefix() + functionPath;
            } else {
                refPath = className + PATH_SEPARATOR + functionPath;
            }
            methodRef.defKey = new DefKey(null, refPath);
            support.emit(methodRef);
        } else {
            // def
            if (nsPath.localCtx != null) {
                Def methodDef = support.def(nsPath.localCtx.getSymbol(), DefKind.FUNCTION);
                methodDef.defKey = new DefKey(null, className + PATH_SEPARATOR + functionPath);
                StringBuilder repr = new StringBuilder().append('(').append(params.getRepresentation()).append(')');
                repr.append(' ').append(returnType);
                methodDef.format(StringUtils.EMPTY, repr.toString(), DefData.SEPARATOR_EMPTY);
                methodDef.defData.setKind(DefKind.FUNCTION);
                support.emit(methodDef);

                support.infos.setProperty(className, DefKind.FUNCTION, functionPath, new ObjectInfo(returnType));
            }
        }

        Scope<ObjectInfo> functionScope = new Scope<>(functionPath,
                StringUtils.isEmpty(className) ? StringUtils.EMPTY : className + PATH_SEPARATOR);
        context.enterScope(functionScope);
        classes.push(className);

        if (info != null && !StringUtils.isEmpty(className)) {
            inheritScope(functionScope, className);
        }

        for (FunctionParameter param : params.params) {
            param.def.defKey = new DefKey(null, context.currentScope().getPathTo(param.def.name, PATH_SEPARATOR));
            support.emit(param.def);
            context.currentScope().put(param.name, new ObjectInfo(param.type));
        }
    }

    @Override
    public void exitFunctiondefinition(FunctiondefinitionContext ctx) {
        context.exitScope();
        classes.pop();
    }

    @Override
    public void exitPrimarypostfixexpression(PrimarypostfixexpressionContext ctx) {

        PrimaryexpressionContext ident = ctx.primaryexpression();
        IdexpressionContext idexpr = ident.idexpression();

        // foo or foo()
        if (ctx.getParent() instanceof FuncallexpressionContext) {
            fnCallStack.push(idexpr == null ? ident : idexpr);
            return;
        }


        if (ident.This() != null) {
            // TODO (alexsaveliev) - should "this" refer to type?
            typeStack.peek().push(classes.isEmpty() ? UNKNOWN : classes.peek());
            return;
        }

        if (idexpr == null) {
            typeStack.peek().push(UNKNOWN);
            return;
        }

        NSPath nsPath = new NSPath(idexpr);

        LookupResult<ObjectInfo> lookup = context.lookup(nsPath.local);
        String type;
        if (lookup == null) {
            NSPath nsTypePath = nsPath.parent();
            String className = namespaceContext.resolve(nsTypePath);
            ObjectInfo oInfo = support.infos.getProperty(className, DefKind.VARIABLE, nsPath.local);
            if (oInfo != null) {
                // foo::bar
                type = oInfo.getType();
                Ref classRef = support.ref(nsTypePath.localCtx.getSymbol());
                classRef.defKey = new DefKey(null, className);
                support.emit(classRef);

                Ref memberRef = support.ref(nsPath.localCtx.getSymbol());
                String memberPrefix = oInfo.getPrefix();
                if (memberPrefix == null) {
                    memberPrefix = className + PATH_SEPARATOR;
                }
                memberRef.defKey = new DefKey(null, memberPrefix + nsPath.local);
                support.emit(memberRef);

            } else {
                type = UNKNOWN;
            }
        } else {
            type = lookup.getValue().getType();
            Ref identRef = support.ref(idexpr);
            identRef.defKey = new DefKey(null, getPath(lookup, nsPath.local));
            support.emit(identRef);
        }
        typeStack.peek().push(type);
    }

    @Override
    public void exitMemberaccessexpression(MemberaccessexpressionContext ctx) {

        // foo.bar, foo.bar(), foo->bar, and foo->bar()
        boolean isFnCall = ctx.getParent() instanceof FuncallexpressionContext;
        IdexpressionContext ident = ctx.idexpression();

        Stack<String> stack = typeStack.peek();
        if (stack.isEmpty()) {
            // TODO: there are not supported branches of postfixexpression
            return;
        }
        String parent = stack.pop();
        if (parent == UNKNOWN) {
            // cannot resolve parent
            if (isFnCall) {
                fnCallStack.push(ident);
            }
            typeStack.peek().push(UNKNOWN);
            return;
        }
        TypeInfo<Scope, ObjectInfo> props = support.infos.get(parent);
        if (props == null) {
            if (isFnCall) {
                fnCallStack.push(ident);
            }
            typeStack.peek().push(UNKNOWN);
            return;
        }

        if (isFnCall) {
            // will deal later
            fnCallStack.push(ident);
            typeStack.peek().push(parent);
            return;
        }

        String varOrPropName = ident.getText();
        ObjectInfo info = props.getProperty(DefKind.VARIABLE, varOrPropName);
        String type;
        if (info == null) {
            type = UNKNOWN;
        } else {
            type = info.getType();
            Ref propRef = support.ref(ident);
            propRef.defKey = new DefKey(null, getPath(info, props.getData(), varOrPropName));
            support.emit(propRef);
        }
        typeStack.peek().push(type);
    }

    @Override
    public void exitCastpostfixexpression(CastpostfixexpressionContext ctx) {
        TypespecifierContext typeCtx = getTypeSpecifier(ctx.typeid().typespecifierseq());
        if (typeCtx == null) {
            typeStack.peek().push(UNKNOWN);
            return;
        }
        typeStack.peek().push(processTypeSpecifier(typeCtx));
    }

    @Override
    public void exitExplicittypeconversionexpression(ExplicittypeconversionexpressionContext ctx) {
        SimpletypespecifierContext simpleTypeCtx = ctx.simpletypespecifier();
        if (simpleTypeCtx != null) {
            TypenameContext typeNameSpec = simpleTypeCtx.typename();
            if (typeNameSpec == null) {
                // basic types
                typeStack.peek().push(simpleTypeCtx.getText());
            } else {
                typeStack.peek().push(processDeclarationType(simpleTypeCtx, typeNameSpec));
            }
        }
    }

    @Override
    public void exitFuncallexpression(FuncallexpressionContext ctx) {
        String signature = signature(ctx.expressionlist());

        if (!(ctx.postfixexpression() instanceof PrimarypostfixexpressionContext)) {
            // foo.bar()
            Stack<String> stack = typeStack.peek();
            if (stack.isEmpty()) {
                // TODO: there are not supported branches of postfixexpression
                return;
            }
            String parent = stack.pop();
            if (parent == UNKNOWN) {
                typeStack.peek().push(UNKNOWN);
                return;
            }
            TypeInfo<Scope, ObjectInfo> props = support.infos.get(parent);
            if (props == null) {
                typeStack.peek().push(UNKNOWN);
                return;
            }
            processFnCallRef(props, signature, false, null);
            return;
        }
        // bar() or Bar() - function or ctor
        ParserRuleContext fnCallNameCtx = fnCallStack.isEmpty() ? null : fnCallStack.peek();

        TypeInfo<Scope, ObjectInfo> props;

        String className;
        boolean isCtor = false;

        if (fnCallNameCtx != null) {
            isCtor = true;
            className = fnCallNameCtx.getText();
            props = support.infos.get(className);
            if (props == null) {
                // maybe inner class?
                className = context.currentScope().getPathTo(className, PATH_SEPARATOR);
                props = support.infos.get(className);
            }
        } else {
            props = null;
            className = null;
        }

        if (props == null) {
            props = support.infos.getRoot();
            isCtor = false;
            className = classes.isEmpty() ? null : classes.peek();
        }
        processFnCallRef(props, signature, isCtor, className);
    }

    @Override
    public void enterMeminitializerid(MeminitializeridContext ctx) {
        NSPath nsPath = new NSPath(ctx);
        String ident = namespaceContext.resolve(nsPath);
        TypeInfo<Scope, ObjectInfo> info = support.infos.get(ident);
        if (info != null) {
            // base initializer
            MeminitializerContext meminitializerCtx = (MeminitializerContext) ctx.getParent();
            String ctorPath = ident + PATH_SEPARATOR + nsPath.local +
                    '(' + signature(meminitializerCtx.expressionlist()) + ')';
            Ref ctorRef = support.ref(nsPath.localCtx.getSymbol());
            ctorRef.defKey = new DefKey(null, ctorPath);
            support.emit(ctorRef);
        } else {
            // member initializer
            ident = nsPath.path;
            LookupResult<ObjectInfo> result = context.lookup(ident);
            if (result != null) {
                Ref memberRef = support.ref(ctx);
                memberRef.defKey = new DefKey(null, getPath(result, ident));
                support.emit(memberRef);
            }
        }
    }

    @Override
    public void enterCompoundstatement(CompoundstatementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitCompoundstatement(CompoundstatementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterIterationstatement(IterationstatementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitIterationstatement(IterationstatementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterUnaryexpression(UnaryexpressionContext ctx) {
        // unary expression may contain a chain like foo.bar->baz().qux...
        // preparing new stack for it
        typeStack.push(new Stack<>());
    }

    @Override
    public void exitUnaryexpression(UnaryexpressionContext ctx) {
        // clearing current stack, no longer needed
        typeStack.pop();
        fnCallStack.empty();
    }

    /**
     * Emits base classes in "class foo: bar"
     */
    private void processBaseClasses(BasespecifierlistContext classes, String path, Scope<ObjectInfo> scope) {
        if (classes == null) {
            return;
        }

        ClassordecltypeContext baseClassCtx = classes.basespecifier().basetypespecifier().classordecltype();
        if (baseClassCtx.classname() == null) {
            return;
        }
        NSPath nsPath = new NSPath(baseClassCtx);
        String name = namespaceContext.resolve(nsPath);
        Ref typeRef = support.ref(nsPath.localCtx.getSymbol());
        typeRef.defKey = new DefKey(null, name);
        support.emit(typeRef);

        TypeInfo<Scope, ObjectInfo> typeInfo = support.infos.get(path);
        typeInfo.setProperty(BASE_CLASS, name, null);
        inheritScope(scope, name);
        processBaseClasses(classes.basespecifierlist(), path, scope);
    }

    /**
     * Copies properties from parent type to given scope.
     * class A : B => class A should have all B's properties
     */
    private void inheritScope(Scope<ObjectInfo> scope, String parentType) {
        TypeInfo<Scope, ObjectInfo> baseInfo = support.infos.get(parentType);
        if (baseInfo != null) {
            Collection<String> baseClasses = baseInfo.getProperties(BASE_CLASS);
            for (String baseClass : baseClasses) {
                inheritScope(scope, baseClass);
            }
            // copying base properties to current scope
            for (String category : baseInfo.getCategories()) {
                for (String property : baseInfo.getProperties(category)) {
                    ObjectInfo info = baseInfo.getProperty(category, property);
                    if (info == null) {
                        support.infos.setProperty(scope.getPath(), category, property, null);
                        continue;
                    }
                    // already inherited, should not override
                    if (info.getPrefix() != null) {
                        continue;
                    }
                    // inherited objects have different path
                    Scope baseScope = baseInfo.getData();
                    if (baseScope != null) {
                        ObjectInfo inherited = new ObjectInfo(info.getType(), baseScope.getPath() + PATH_SEPARATOR);
                        support.infos.setProperty(scope.getPath(), category, property, inherited);
                        if (category == DefKind.VARIABLE) {
                            scope.put(property, inherited);
                        }
                    }
                }
            }
        }
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

    /**
     * Extracts type specifier from type specifier sequence
     */
    private TypespecifierContext getTypeSpecifier(TypespecifierseqContext ctx) {
        if (ctx == null) {
            return null;
        }
        TypespecifierContext typeSpec = ctx.typespecifier();
        if (typeSpec != null) {
            return typeSpec;
        }
        return getTypeSpecifier(ctx.typespecifierseq());
    }

    /**
     * Extracts type specifier from type specifier sequence
     */
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
            TrailingtypespecifierContext trailingtypespecifierContext = typeSpec.trailingtypespecifier();
            if (trailingtypespecifierContext == null) {
                // TODO: add support of class {...} or enum {...}
                return getDeclTypeSpecifier(ctx.declspecifierseq());
            }
            if (trailingtypespecifierContext.simpletypespecifier() == null) {
                return getDeclTypeSpecifier(ctx.declspecifierseq());
            }
            return typeSpec;
        }
        return getDeclTypeSpecifier(ctx.declspecifierseq());
    }


    /**
     * Handles type specifier
     */
    private String processTypeSpecifier(TypespecifierContext typeSpec) {
        if (typeSpec == null) {
            return null;
        }
        TrailingtypespecifierContext trailingTypeSpec = typeSpec.trailingtypespecifier();
        if (trailingTypeSpec == null) {
            return null;
        }
        SimpletypespecifierContext simpleTypeSpec = trailingTypeSpec.simpletypespecifier();
        if (simpleTypeSpec == null) {
            // TODO support for elaboratedtypespecifier | typenamespecifier | cvqualifier?
            return null;
        }
        TypenameContext typeNameSpec = simpleTypeSpec.typename();
        if (typeNameSpec == null) {
            // basic types
            return simpleTypeSpec.getText();
        }

        return processDeclarationType(simpleTypeSpec, typeNameSpec);
    }

    /**
     * Handles type part of simple declaration
     */
    private String processDeclarationType(SimpletypespecifierContext ctx, TypenameContext typenameCtx) {

        if (typenameCtx.enumname() == null && typenameCtx.classname() == null) {
            // TODO not supported yet
            return null;
        }
        NSPath nsPath = new NSPath(ctx);
        return namespaceContext.resolve(nsPath);
    }

    /**
     * Handles variables and functions in "foo bar,baz" or "foo bar();" statements
     */
    private void processVarsAndFunctions(InitdeclaratorlistContext variables, String typeName) {
        if (variables == null) {
            return;
        }
        processVarOrFunction(variables.initdeclarator(), typeName);
        processVarsAndFunctions(variables.initdeclaratorlist(), typeName);
    }

    /**
     * Handles single variable or function in "foo bar,baz" or "foo bar();" statements
     */
    private void processVarOrFunction(InitdeclaratorContext var, String typeName) {
        DeclaratorContext decl = var.declarator();
        IdexpressionContext ident = getIdentifier(decl);
        if (ident == null) {
            return;
        }
        ParametersandqualifiersContext paramsAndQualifiers = getParametersAndQualifiers(decl);
        if (paramsAndQualifiers == null) {
            // variable
            processVar(typeName, ident);
        } else {
            processFunctionOrMethod(ident, paramsAndQualifiers, typeName);
        }
    }

    /**
     * Handles single variable from declaration
     */
    private void processVar(String typeName, IdexpressionContext ident) {
        String varName = ident.getText();
        LookupResult<ObjectInfo> info = context.lookup(varName);
        if (info == null) {
            Def varDef = support.def(ident, DefKind.VARIABLE);
            varDef.defKey = new DefKey(null, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));
            varDef.format(StringUtils.EMPTY, typeName == null ? StringUtils.EMPTY : typeName, DefData.SEPARATOR_SPACE);
            varDef.defData.setKind(DefKind.VARIABLE);
            context.currentScope().put(varDef.name, new ObjectInfo(typeName));
            support.emit(varDef);
        } else {
            Ref varRef = support.ref(ident);
            varRef.defKey = new DefKey(null, getPath(info, varName));
            support.emit(varRef);
        }
    }

    /**
     * Extracts identifier information
     */
    private IdexpressionContext getIdentifier(DeclaratorContext ctx) {

        NoptrdeclaratorContext noPtr = ctx.noptrdeclarator();
        if (noPtr != null) {
            return getIdentifier(noPtr);
        }
        return getIdentifier(ctx.ptrdeclarator());
    }

    /**
     * Extracts identifier information
     */
    private IdexpressionContext getIdentifier(PtrdeclaratorContext ctx) {
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
    private IdexpressionContext getIdentifier(NoptrdeclaratorContext ctx) {
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
        DeclaratorContext declarator = param.declarator();
        if (declarator == null) {
            return;
        }
        ParserRuleContext paramNameCtx = getIdentifier(param.declarator());
        TypespecifierContext paramTypeCtx = getDeclTypeSpecifier(param.declspecifierseq());
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

    /**
     * Handles class members
     */
    private void processMembers(MemberspecificationContext ctx) {
        if (ctx == null) {
            return;
        }
        // head
        MemberdeclarationContext member = ctx.memberdeclaration();
        if (member != null) {
            TypespecifierContext typeCtx = getDeclTypeSpecifier(member.declspecifierseq());
            String type = null;
            if (typeCtx != null) {
                type = processTypeSpecifier(typeCtx);
            }
            processMembers(member.memberdeclaratorlist(), type);
        }
        // tail
        processMembers(ctx.memberspecification());
    }

    /**
     * Handles class members
     */
    private void processMembers(MemberdeclaratorlistContext members, String type) {
        if (members == null) {
            return;
        }
        MemberdeclaratorContext member = members.memberdeclarator();
        if (member != null) {
            processMember(member, type);
        }
        processMembers(members.memberdeclaratorlist(), type);
    }

    /**
     * Handles single class member
     */
    private void processMember(MemberdeclaratorContext member, String type) {

        IdexpressionContext ident = null;
        Token identToken = null;
        String name;
        ParametersandqualifiersContext paramsAndQualifiers;

        DeclaratorContext decl = member.declarator();
        if (decl == null) {
            // unsigned char Version : 3;
            TerminalNode identifier = member.Identifier();
            if (identifier == null) {
                // TODO: enum State : uint32_t {..}
                return;
            }
            identToken = identifier.getSymbol();
            name = identToken.getText();
            paramsAndQualifiers = null;
        } else {
            // "foo bar;" or "foo bar();"
            ident = getIdentifier(decl);
            if (ident == null) {
                return;
            }
            name = ident.getText();
            paramsAndQualifiers = getParametersAndQualifiers(decl);
        }


        if (paramsAndQualifiers == null) {
            // int foo;
            Def memberDef;
            if (ident != null) {
                memberDef = support.def(ident, DefKind.MEMBER);
            } else {
                memberDef = support.def(identToken, DefKind.MEMBER);
            }
            memberDef.defKey = new DefKey(null, context.currentScope().getPathTo(name, PATH_SEPARATOR));
            memberDef.format(StringUtils.EMPTY, type, DefData.SEPARATOR_SPACE);
            memberDef.defData.setKind(DefKind.MEMBER);
            support.emit(memberDef);
            ObjectInfo objectInfo = new ObjectInfo(type);
            context.currentScope().put(name, objectInfo);
            support.infos.setProperty(context.currentScope().getPath(),
                    DefKind.VARIABLE,
                    name,
                    new ObjectInfo(type));
        } else {
            // int foo();
            if (type == null) {
                type = classes.isEmpty() ? null : classes.peek();
            }

            processFunctionOrMethod(ident, paramsAndQualifiers, type);
        }
    }

    /**
     * Constructs function signature based on parameters
     */
    private String signature(ExpressionlistContext ctx) {
        if (ctx == null) {
            return StringUtils.EMPTY;
        }
        Collection<String> params = new LinkedList<>();
        processSignature(ctx.initializerlist(), params);
        return StringUtils.join(params, ',');
    }

    /**
     * Recursively processes function call arguments to build a signature
     */
    private void processSignature(InitializerlistContext ctx, Collection<String> params) {
        if (ctx == null) {
            return;
        }
        if (ctx.initializerclause() != null) {
            params.add("_");
        }
        processSignature(ctx.initializerlist(), params);
    }

    /**
     * Emits function ref
     */
    private void processFnCallRef(TypeInfo<Scope, ObjectInfo> props,
                                  String signature,
                                  boolean isCtor,
                                  String className) {
        if (fnCallStack.isEmpty()) {
            return;
        }
        ParserRuleContext fnIdent = fnCallStack.pop();

        String methodName;
        if (isCtor) {
            int pos = className.lastIndexOf(PATH_SEPARATOR);
            if (pos >= 0) {
                methodName = className.substring(pos + 1);
            } else {
                methodName = className;
            }
        } else {
            methodName = fnIdent.getText();
        }
        // looking for matching function
        String fnPath = methodName + '(' + signature + ')';

        ObjectInfo info = null;

        if (className != null) {
            // lookup in current class
            TypeInfo<Scope, ObjectInfo> currentProps = support.infos.get(className);
            info = currentProps.getProperty(DefKind.FUNCTION, fnPath);
            if (info != null) {
                props = currentProps;
            }
        }

        if (info == null) {
            if (isCtor) {
                info = new ObjectInfo(className);
            } else {
                info = props.getProperty(DefKind.FUNCTION, fnPath);
            }
        }

        if (info != null) {
            Ref methodRef = support.ref(fnIdent);
            Scope scope = props.getData();
            if (scope == null) {
                scope = context.getRoot();
            }
            methodRef.defKey = new DefKey(null, getPath(info, scope, fnPath));
            support.emit(methodRef);
            typeStack.peek().push(info.getType());
        } else {
            typeStack.peek().push(UNKNOWN);
        }
    }

    /**
     * Extracts nested components (identifiers) (foo, bar, baz from foo::bar::baz)
     */
    private static List<TerminalNode> getNestedComponents(ParserRuleContext ctx) {
        List<TerminalNode> ret = new LinkedList<>();
        collectNestedComponents(ctx, ret);
        return ret;
    }

    /**
     * Extracts nested components (identiiers) (foo, bar, baz from foo::bar::baz)
     */
    private static void collectNestedComponents(ParseTree ctx, List<TerminalNode> ret) {
        int count = ctx.getChildCount();
        for (int i = 0; i < count; i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) child;
                if (terminalNode.getSymbol().getType() == Identifier) {
                    ret.add(terminalNode);
                }
            } else {
                collectNestedComponents(child, ret);
            }
        }
    }

    /**
     * Makes path to object taking into account that it might be defined in a base class
     */
    private String getPath(LookupResult<ObjectInfo> result, String id) {
        return getPath(result.getValue(), result.getScope(), id);
    }


    /**
     * Makes path to object taking into account that it might be defined in a base class
     */
    private String getPath(ObjectInfo info, Scope scope, String id) {

        if (info == null) {
            return scope.getPathTo(id, PATH_SEPARATOR);
        }

        String prefix = info.getPrefix();
        if (prefix == null) {
            return scope.getPathTo(id, PATH_SEPARATOR);
        }
        if (prefix.isEmpty()) {
            return id;
        }
        return prefix + id;
    }

    /**
     * Handles function or method definition
     */
    private void processFunctionOrMethod(IdexpressionContext ident,
                                         ParametersandqualifiersContext paramsAndQualifiers,
                                         String typeName) {
        FunctionParameters params = new FunctionParameters();
        processFunctionParameters(
                paramsAndQualifiers.parameterdeclarationclause().parameterdeclarationlist(),
                params);

        NSPath nsPath = new NSPath(ident);
        if (nsPath.localCtx == null) {
            return;
        }

        Def fnDef = support.def(nsPath.localCtx.getSymbol(), DefKind.FUNCTION);

        String fnPath = fnDef.name + '(' + params.getSignature() + ')';
        fnDef.defKey = new DefKey(null, context.currentScope().getPathTo(fnPath, PATH_SEPARATOR));

        StringBuilder repr = new StringBuilder().append('(').append(params.getRepresentation()).append(')');
        repr.append(' ').append(typeName);
        fnDef.format(StringUtils.EMPTY, repr.toString(), DefData.SEPARATOR_EMPTY);
        fnDef.defData.setKind(DefKind.FUNCTION);
        support.emit(fnDef);

        support.infos.setProperty(context.currentScope().getPath(),
                DefKind.FUNCTION,
                fnPath,
                new ObjectInfo(typeName));
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

    /**
     * Namespace-aware path
     */
    private static class NSPath {

        /**
         * Individual path components (foo, bar, baz) for ::foo::bar::baz
         */
        LinkedList<String> components = new LinkedList<>();

        /**
         * Individual terminal nodes (foo, bar, baz) for ::foo::bar::baz
         */
        LinkedList<TerminalNode> nodes = new LinkedList<>();
        /**
         * Indicates if path is absolute (starts with ::)
         */
        boolean absolute;
        /**
         * Normalized path foo.bar.baz for (foo::bar::baz)
         */
        String path;

        /**
         * Local name (last component)
         */
        String local;

        /**
         * Local name (last component)
         */
        TerminalNode localCtx;

        NSPath(ParserRuleContext ctx) {

            if (ctx == null) {
                absolute = false;
                path = StringUtils.EMPTY;
                return;
            }

            List<TerminalNode> nodes = getNestedComponents(ctx);
            absolute = "::".equals(ctx.getStart().getText());
            StringBuilder pathBuilder = new StringBuilder();
            for (TerminalNode node : nodes) {
                String ident = node.getText();
                components.add(ident);
                this.nodes.add(node);
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(PATH_SEPARATOR);
                }
                pathBuilder.append(ident);
                local = ident;
                localCtx = node;
            }
            path = pathBuilder.toString();
        }

        NSPath parent() {
            NSPath ret = new NSPath(null);
            ret.absolute = absolute;
            if (!components.isEmpty()) {
                ret.components = new LinkedList<>(components.subList(0, components.size() - 1));
                if (!ret.components.isEmpty()) {
                    ret.local = ret.components.peek();
                }
            }
            if (!nodes.isEmpty()) {
                ret.nodes = new LinkedList<>(nodes.subList(0, nodes.size() - 1));
                if (!ret.nodes.isEmpty()) {
                    ret.localCtx = ret.nodes.peek();
                }
            }
            ret.path = StringUtils.join(ret.components, PATH_SEPARATOR);
            return ret;
        }
    }

    private class NamespaceContext {

        private LinkedList<String> namespaces = new LinkedList<>();

        private String current = StringUtils.EMPTY;

        NamespaceContext() {
            enter(StringUtils.EMPTY);
        }

        void enter(String name) {
            String id;
            if (current.isEmpty()) {
                id = name;
            } else {
                id = current + name;
            }
            if (!id.isEmpty()) {
                id += PATH_SEPARATOR;
            }
            namespaces.push(id);
            current = id;
        }

        void exit() {
            namespaces.pop();
            current = namespaces.peek();
        }

        String resolve(NSPath path) {
            return resolve(path.path, path.absolute);
        }

        String resolve(String path, boolean absolute) {
            if (absolute) {
                // ::foo::bar::baz
                return path;
            }
            Iterator<String> it = namespaces.descendingIterator();
            while (it.hasNext()) {
                String namespace = it.next();
                String fqn = namespace + path;
                if (support.infos.get(fqn) != null) {
                    return fqn;
                }
            }
            // TODO: using, using namespace
            return current + path;

        }

        String fqn(String id) {
            return current + id;
        }
    }

}