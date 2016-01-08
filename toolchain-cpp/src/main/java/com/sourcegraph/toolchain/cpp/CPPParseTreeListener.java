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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static com.sourcegraph.toolchain.cpp.antlr4.CPP14Parser.*;

class CPPParseTreeListener extends CPP14BaseListener {

    private static final char PATH_SEPARATOR = '.';

    private static final String UNKNOWN = "?";

    private static final String BASE_CLASS = "baseClass";

    private LanguageImpl support;

    private Context<ObjectInfo> context = new Context<>();

    private Stack<Stack<String>> typeStack = new Stack<>();

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
        ClassheadnameContext classheadnameCtx = head.classheadname();
        if (classheadnameCtx == null) {
            // TODO typedef struct {} foo;
            context.enterScope(context.currentScope().next(PATH_SEPARATOR));
            return;
        }
        ParserRuleContext name = classheadnameCtx.classname();
        String className = name.getText();

        Scope<ObjectInfo> scope = new Scope<>(className, context.getPrefix(PATH_SEPARATOR));

        String kind = head.classkey().getText();
        Def def = support.def(name, kind);
        def.defKey = new DefKey(null, context.currentScope().getPathTo(className, PATH_SEPARATOR));
        def.format(kind, kind, DefData.SEPARATOR_SPACE);
        support.emit(def);

        String path = scope.getPath();

        BaseclauseContext base = head.baseclause();
        support.infos.setData(path, scope);

        if (base != null) {
            processBaseClasses(base.basespecifierlist(), path, scope);
        }

        context.enterScope(scope);

        currentClass = path;

        // we should handle members here instead of enterMemberdeclaration()
        // because they may appear after method declaration while we need to know this info
        processMembers(ctx.memberspecification());
    }

    @Override
    public void exitClassspecifier(ClassspecifierContext ctx) {
        context.exitScope();
        currentClass = null;
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
        if (returnType == null && currentClass != null) {
            returnType = currentClass;
        }

        String path = context.currentScope().getPath();

        FunctionParameters params = new FunctionParameters();
        ParametersandqualifiersContext paramsCtx = getParametersAndQualifiers(ctx.declarator());
        if (paramsCtx != null) {
            processFunctionParameters(
                    paramsCtx.parameterdeclarationclause().parameterdeclarationlist(),
                    params);
        }

        IdexpressionContext ident = getIdentifier(ctx.declarator());
        String fnPath;

        boolean isRef = false;

        if (ident.unqualifiedid() != null) {

            String functionName = ident.getText();
            fnPath = functionName + '(' + params.getSignature() + ')';

            ObjectInfo inherited = support.infos.getProperty(currentClass, DefKind.FUNCTION, fnPath);
            if (inherited != null) {
                Ref methodRef = support.ref(ident);
                String defPath = getPath(inherited, context.currentScope(), fnPath);
                methodRef.defKey = new DefKey(null, defPath);
                support.emit(methodRef);
            } else {
                Def fnDef = support.def(ident, DefKind.FUNCTION);
                fnDef.defKey = new DefKey(null, context.currentScope().getPathTo(fnPath, PATH_SEPARATOR));
                StringBuilder repr = new StringBuilder().append('(').append(params.getRepresentation()).append(')');
                repr.append(' ').append(returnType);
                fnDef.format(StringUtils.EMPTY, repr.toString(), DefData.SEPARATOR_EMPTY);
                fnDef.defData.setKind(DefKind.FUNCTION);
                support.emit(fnDef);
            }
            context.enterScope(new Scope<>(fnPath, context.currentScope().getPrefix()));


        } else {
            // class method definition foo::bar

            isRef = true;

            List<TerminalNode> pathElements = getNestedComponents(ident.qualifiedid().nestednamespecifier());
            StringBuilder typePathBuilder = new StringBuilder();
            TerminalNode typeNode = null;
            for (TerminalNode pathElement : pathElements) {
                if (typeNode != null) {
                    typePathBuilder.append(PATH_SEPARATOR);
                }
                typeNode = pathElement;
                typePathBuilder.append(pathElement.getText());
            }
            // TODO namespace?
            String typeName = null;
            typeName = typePathBuilder.toString();
            if (typeNode != null) {
                Ref typeRef = support.ref(typeNode.getSymbol());
                typeRef.defKey = new DefKey(null, typeName);
                support.emit(typeRef);
            }

            currentClass = typeName;
            context.enterScope(new Scope<>(typeName, context.currentScope().getPrefix()));

            ParserRuleContext fnIdentCtx = ident.qualifiedid().unqualifiedid();
            Ref methodRef = support.ref(fnIdentCtx);
            fnPath = fnIdentCtx.getText() + '(' + params.getSignature() + ')';

            ObjectInfo inherited = support.infos.getProperty(currentClass, DefKind.FUNCTION, fnPath);
            String defPath = getPath(inherited, context.currentScope(), fnPath);
            methodRef.defKey = new DefKey(null, defPath);
            support.emit(methodRef);

            Scope<ObjectInfo> scope = new Scope<>(fnPath, context.currentScope().getPrefix());
            inheritScope(scope, typeName);
            context.enterScope(scope);

        }


        for (FunctionParameter param : params.params) {
            param.def.defKey = new DefKey(null, context.currentScope().getPathTo(param.def.name, PATH_SEPARATOR));
            support.emit(param.def);
            context.currentScope().put(param.name, new ObjectInfo(param.type));
        }
        if (!isRef) {
            support.infos.setProperty(path, DefKind.FUNCTION, fnPath, new ObjectInfo(returnType));
        }

    }

    @Override
    public void exitFunctiondefinition(FunctiondefinitionContext ctx) {
        IdexpressionContext ident = getIdentifier(ctx.declarator());
        if (ident.unqualifiedid() == null) {
            // exit from foo::bar definition
            currentClass = null;
            context.exitScope();
        }
        context.exitScope();

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
            typeStack.peek().push(currentClass == null ? UNKNOWN : currentClass);
            return;
        }

        if (idexpr == null) {
            typeStack.peek().push(UNKNOWN);
            return;
        }

        String varName = idexpr.getText();
        LookupResult<ObjectInfo> lookup = context.lookup(varName);
        String type;
        if (lookup == null) {
            // TODO: namespaces
            if (support.infos.get(varName) != null) {
                // type name like "Foo" in Foo.instance.bar()
                type = varName;
                Ref typeRef = support.ref(ident);
                typeRef.defKey = new DefKey(null, type);
                support.emit(typeRef);
            } else {
                type = UNKNOWN;
            }
        } else {
            type = lookup.getValue().getType();
            Ref identRef = support.ref(idexpr);
            identRef.defKey = new DefKey(null, getPath(lookup, varName));
            support.emit(identRef);
        }
        typeStack.peek().push(type);
    }

    @Override
    public void exitMemberaccessexpression(MemberaccessexpressionContext ctx) {

        // foo.bar, foo.bar(), foo->bar, and foo->bar()
        boolean isFnCall = ctx.getParent() instanceof FuncallexpressionContext;
        IdexpressionContext ident = ctx.idexpression();

        String parent = typeStack.peek().pop();
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
        // TODO: namespaces
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
                // TODO: namespaces
                typeStack.peek().push(processDeclarationType(typeNameSpec));
            }
        }
    }

    @Override
    public void exitFuncallexpression(FuncallexpressionContext ctx) {
        String signature = signature(ctx.expressionlist());

        if (!(ctx.postfixexpression() instanceof PrimarypostfixexpressionContext)) {
            // foo.bar()
            String parent = typeStack.peek().pop();
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
            className = currentClass;
        }
        processFnCallRef(props, signature, isCtor, className);
    }

    @Override
    public void enterMeminitializerid(MeminitializeridContext ctx) {
        String ident = ctx.getText();
        // TODO: namespaces?
        TypeInfo<Scope, ObjectInfo> info = support.infos.get(ident);
        if (info != null) {
            Ref typeRef = support.ref(ctx);
            typeRef.defKey = new DefKey(null, info.getData().getPath());
            support.emit(typeRef);
        } else {
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
        // TODO : namespaces?
        ClassnameContext classnameCtx = classes.basespecifier().basetypespecifier().classordecltype().classname();
        TerminalNode identifier = classnameCtx.Identifier();
        if (identifier == null) {
            return;
        }
        Token baseName = identifier.getSymbol();
        String name = baseName.getText();
        Ref typeRef = support.ref(baseName);
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
                    ObjectInfo inherited = new ObjectInfo(info.getType(), baseInfo.getData().getPath() + PATH_SEPARATOR);
                    support.infos.setProperty(scope.getPath(), category, property, inherited);
                    if (category == DefKind.VARIABLE) {
                        scope.put(property, inherited);
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
            TerminalNode identifier = classnameSpec.Identifier();
            if (identifier != null) {
                return processTypeRef(identifier);
            }
            // template
            // TODO
            return classnameSpec.getText();
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
        context.currentScope().put(varDef.name, new ObjectInfo(typeName));
        support.emit(varDef);
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

        DeclaratorContext decl = member.declarator();
        ParserRuleContext ident = getIdentifier(decl);
        if (ident == null) {
            return;
        }
        String name = ident.getText();

        ParametersandqualifiersContext paramsAndQualifiers = getParametersAndQualifiers(decl);
        if (paramsAndQualifiers == null) {
            // int foo;
            Def memberDef = support.def(ident, DefKind.MEMBER);
            memberDef.defKey = new DefKey(null, context.currentScope().getPathTo(name, PATH_SEPARATOR));
            memberDef.format(StringUtils.EMPTY, type, DefData.SEPARATOR_SPACE);
            memberDef.defData.setKind(DefKind.MEMBER);
            support.emit(memberDef);
            ObjectInfo objectInfo = new ObjectInfo(type);
            context.currentScope().put(name, objectInfo);
            support.infos.setProperty(context.getPath(PATH_SEPARATOR), DefKind.VARIABLE, name, new ObjectInfo(type));
        } else {
            // int foo();
            if (type == null) {
                type = currentClass;
            }

            String path = context.currentScope().getPath();

            FunctionParameters params = new FunctionParameters();
            processFunctionParameters(
                    paramsAndQualifiers.parameterdeclarationclause().parameterdeclarationlist(),
                    params);

            Def fnDef = support.def(ident, DefKind.FUNCTION);

            String fnPath = fnDef.name + '(' + params.getSignature() + ')';
            fnDef.defKey = new DefKey(null, context.currentScope().getPathTo(fnPath, PATH_SEPARATOR));

            StringBuilder repr = new StringBuilder().append('(').append(params.getRepresentation()).append(')');
            repr.append(' ').append(type);
            fnDef.format(StringUtils.EMPTY, repr.toString(), DefData.SEPARATOR_EMPTY);
            fnDef.defData.setKind(DefKind.FUNCTION);
            support.emit(fnDef);

            support.infos.setProperty(path, DefKind.FUNCTION, fnPath, new ObjectInfo(type));
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
    private List<TerminalNode> getNestedComponents(ParserRuleContext ctx) {
        List<TerminalNode> ret = new LinkedList<>();
        collectNestedComponents(ctx, ret);
        return ret;
    }

    /**
     * Extracts nested components (identiiers) (foo, bar, baz from foo::bar::baz)
     */
    private void collectNestedComponents(ParseTree ctx, List<TerminalNode> ret) {
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