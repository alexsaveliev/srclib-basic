package com.sourcegraph.toolchain.swift;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.swift.antlr4.SwiftBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static com.sourcegraph.toolchain.swift.antlr4.SwiftParser.*;

class SwiftParseTreeListener extends SwiftBaseListener {

    private static final String VOID = "Void";
    private static final String UNKNOWN = "?";
    private static final String CTOR = "init";

    private static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    private Context<ObjectInfo> context = new Context<>();

    private Stack<String> typeStack = new Stack<>();

    private Stack<ParserRuleContext> fnCallStack = new Stack<>();

    private String currentClass;

    private boolean isInFunction;

    SwiftParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterProtocol_declaration(Protocol_declarationContext ctx) {
        // protocol Foo
        String prefix = context.getPrefix(PATH_SEPARATOR);

        Def protoDef = support.def(ctx.protocol_name(), DefKind.PROTOCOL);
        protoDef.defKey = new DefKey(null, prefix + protoDef.name);
        protoDef.format("protocol", "protocol", DefData.SEPARATOR_SPACE);
        emit(protoDef);
        // protocol Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());


        Scope<ObjectInfo> scope = new Scope<>(protoDef.name, prefix);
        context.enterScope(scope);

        String path = context.getPath(PATH_SEPARATOR);
        support.infos.setData(path, scope);

        currentClass = path;
    }

    @Override
    public void exitProtocol_declaration(Protocol_declarationContext ctx) {
        context.exitScope();
        currentClass = null;
    }

    @Override
    public void enterExtension_declaration(Extension_declarationContext ctx) {
        // extension Foo
        String prefix = context.getPrefix(PATH_SEPARATOR);


        Type_nameContext typeName = ctx.type_identifier().type_name();
        Ref extensionRef = support.ref(typeName);
        extensionRef.defKey = new DefKey(null, prefix + typeName.getText());
        emit(extensionRef);
        // extension Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());

        context.enterScope(new Scope<>(typeName.getText(), prefix));

        currentClass = typeName.getText();
    }

    @Override
    public void exitExtension_declaration(Extension_declarationContext ctx) {
        context.exitScope();
        currentClass = null;
    }

    @Override
    public void enterClass_declaration(Class_declarationContext ctx) {
        // class Foo
        String prefix = context.getPrefix(PATH_SEPARATOR);

        Def classDef = support.def(ctx.class_name(), DefKind.CLASS);
        classDef.defKey = new DefKey(null, prefix + classDef.name);
        classDef.format("class", "class", DefData.SEPARATOR_SPACE);
        emit(classDef);
        // class Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());
        // class Foo<Bar>
        emitGenericParameterRefs(ctx.generic_parameter_clause());

        Scope<ObjectInfo> scope = new Scope<>(classDef.name, prefix);
        context.enterScope(scope);

        String path = context.getPath(PATH_SEPARATOR);
        support.infos.setData(path, scope);
        currentClass = path;
    }

    @Override
    public void exitClass_declaration(Class_declarationContext ctx) {
        context.exitScope();
        currentClass = null;
    }

    @Override
    public void enterStruct_declaration(Struct_declarationContext ctx) {
        // struct Foo
        String prefix = context.getPrefix(PATH_SEPARATOR);

        Def structDef = support.def(ctx.struct_name(), DefKind.STRUCT);
        structDef.defKey = new DefKey(null, prefix + structDef.name);
        structDef.format("struct", "struct", DefData.SEPARATOR_SPACE);
        emit(structDef);
        // struct Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());
        // struct Foo<Bar>
        emitGenericParameterRefs(ctx.generic_parameter_clause());

        Scope<ObjectInfo> scope = new Scope<>(structDef.name, prefix);
        context.enterScope(scope);

        String path = context.getPath(PATH_SEPARATOR);
        support.infos.setData(path, scope);

        currentClass = path;
    }

    @Override
    public void exitStruct_declaration(Struct_declarationContext ctx) {
        context.exitScope();
        currentClass = null;
    }

    @Override
    public void enterEnum_declaration(Enum_declarationContext ctx) {

        String prefix = context.getPrefix(PATH_SEPARATOR);

        Def enumDef;
        Raw_value_style_enumContext rawValueEnum = ctx.raw_value_style_enum();
        if (rawValueEnum != null) {
            emitGenericParameterRefs(rawValueEnum.generic_parameter_clause());
            emitParentTypeRefs(rawValueEnum.type_inheritance_clause());
            enumDef = support.def(rawValueEnum.enum_name(), DefKind.ENUM);
        } else {
            Union_style_enumContext unionEnum = ctx.union_style_enum();
            emitGenericParameterRefs(unionEnum.generic_parameter_clause());
            emitParentTypeRefs(unionEnum.type_inheritance_clause());
            enumDef = support.def(unionEnum.enum_name(), DefKind.ENUM);
        }

        enumDef.defKey = new DefKey(null, prefix + enumDef.name);
        enumDef.format("enum", "enum", DefData.SEPARATOR_SPACE);

        emit(enumDef);

        Scope<ObjectInfo> scope = new Scope<>(enumDef.name, prefix);
        context.enterScope(scope);

        String path = context.getPath(PATH_SEPARATOR);
        support.infos.setData(path, scope);

        currentClass = path;
    }

    @Override
    public void exitEnum_declaration(Enum_declarationContext ctx) {
        context.exitScope();
        currentClass = null;
    }

    @Override
    public void enterFunction_declaration(Function_declarationContext ctx) {

        isInFunction = true;

        emitGenericParameterRefs(ctx.generic_parameter_clause());

        IdentifierContext nameCtx = ctx.function_name().identifier();
        if (nameCtx == null) {
            return;
        }
        Def fnDef = support.def(nameCtx, DefKind.FUNC);

        String prefix = context.getPrefix(PATH_SEPARATOR);

        String path = context.getPath(PATH_SEPARATOR);

        Function_signatureContext signatureContext = ctx.function_signature();
        Function_resultContext resultContext = signatureContext.function_result();
        FunctionParameters params = new FunctionParameters();
        processFunctionParameters(signatureContext.parameter_clauses(), params);
        String type;
        if (resultContext != null) {
            Type_identifierContext typeNameCtx = extractTypeName(resultContext.type());
            if (typeNameCtx != null) {
                type = typeNameCtx.getText();
                Ref typeRef = support.ref(typeNameCtx);
                typeRef.defKey = new DefKey(null, typePath(type));
                emit(typeRef);
            } else {
                type = VOID;
            }
        } else {
            type = VOID;
        }
        StringBuilder repr = new StringBuilder().append('(').append(params.getRepresentation()).append(')');
        repr.append(' ').append(type);


        String fnPath = fnDef.name + '(' + params.getSignature() + ')';
        fnDef.defKey = new DefKey(null, prefix + fnPath);
        fnDef.format("func",
                repr.toString(),
                DefData.SEPARATOR_EMPTY);
        context.enterScope(new Scope<>(fnPath, prefix));
        emit(fnDef);
        for (FunctionParameter param : params.params) {
            param.def.defKey = new DefKey(null, context.currentScope().getPathTo(param.def.name, PATH_SEPARATOR));
            emit(param.def);
            context.currentScope().put(param.name, new ObjectInfo(param.type));
        }
        support.infos.setProperty(path, DefKind.FUNC, fnPath, type);
    }

    @Override
    public void exitFunction_declaration(Function_declarationContext ctx) {

        isInFunction = false;

        IdentifierContext nameCtx = ctx.function_name().identifier();
        if (nameCtx == null) {
            return;
        }

        context.exitScope();
    }

    @Override
    public void enterInitializer_declaration(Initializer_declarationContext ctx) {

        String prefix = context.getPrefix(PATH_SEPARATOR);

        emitGenericParameterRefs(ctx.generic_parameter_clause());

        Def fnDef = support.def(ctx.initializer_head().init_kw(), DefKind.FUNC);

        String path = context.getPath(PATH_SEPARATOR);
        String type = context.currentScope().getName();

        FunctionParameters params = new FunctionParameters();

        processFunctionParameters(ctx.parameter_clause(), params);
        fnDef.format(StringUtils.EMPTY,
                type + '(' + params.getRepresentation() + ')',
                DefData.SEPARATOR_EMPTY);

        String fnPath = "init(" + params.getSignature() + ')';
        fnDef.defKey = new DefKey(null, prefix + fnPath);

        emit(fnDef);
        context.enterScope(new Scope<>(fnPath, prefix));

        for (FunctionParameter param : params.params) {
            param.def.defKey = new DefKey(null, context.currentScope().getPathTo(param.def.name, PATH_SEPARATOR));
            emit(param.def);
            context.currentScope().put(param.name, new ObjectInfo(param.type));
        }

        support.infos.setProperty(path, DefKind.FUNC, fnPath, type);
    }

    @Override
    public void exitInitializer_declaration(Initializer_declarationContext ctx) {
        context.exitScope();
    }


    @Override
    public void exitConstant_declaration(Constant_declarationContext ctx) {
        processVariableOrConstants(ctx.pattern_initializer_list().pattern_initializer(),
                DefKind.LET, "let", "constant");
    }

    @Override
    public void exitVariable_declaration(Variable_declarationContext ctx) {
        Pattern_initializer_listContext list = ctx.pattern_initializer_list();
        if (list != null) {
            processVariableOrConstants(list.pattern_initializer(),
                    DefKind.VAR, "var", "variable");
        } else {
            Def varDef = support.def(ctx.variable_name(), DefKind.VAR);
            varDef.defKey = new DefKey(null, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));
            Type_annotationContext typeAnnotationContext = ctx.type_annotation(0);
            Type_identifierContext typeNameCtx = extractTypeName(typeAnnotationContext.type());
            String typeName;
            if (typeNameCtx != null) {
                typeName = typeNameCtx.getText();
                Ref typeRef = support.ref(typeNameCtx);
                typeRef.defKey = new DefKey(null, typePath(typeName));
                emit(typeRef);
            } else {
                typeName = guessType(ctx.initializer());
            }
            varDef.format(StringUtils.EMPTY, typeName, DefData.SEPARATOR_SPACE);
            varDef.defData.setKind("variable");
            emit(varDef);
            context.currentScope().put(varDef.name, new ObjectInfo(typeName));
            if (!isInFunction) {
                support.infos.setProperty(context.getPath(PATH_SEPARATOR), DefKind.VAR, varDef.name, typeName);
            }
        }
    }

    @Override
    public void enterEnum_case_name(Enum_case_nameContext ctx) {
        if (ctx.getParent() instanceof Enum_case_patternContext) {
            return;
        }
        Def enumCaseDef = support.def(ctx, DefKind.CASE);
        enumCaseDef.defKey = new DefKey(null, context.currentScope().getPathTo(enumCaseDef.name, PATH_SEPARATOR));
        emit(enumCaseDef);
    }

    @Override
    public void exitPrimary_expression(Primary_expressionContext ctx) {

        if (support.firstPass) {
            return;
        }

        // foo or foo()
        IdentifierContext ident = ctx.identifier();
        if (ctx.getParent().getParent() instanceof Function_call_expressionContext) {
            fnCallStack.push(ident);
            return;
        }

        if (ident == null) {
            Self_expressionContext selfExpr = ctx.self_expression();
            if (selfExpr != null) {
                // self or self.foo
                processSelfExpression(selfExpr);
                return;
            }
            Superclass_expressionContext superclassExpr = ctx.superclass_expression();
            if (superclassExpr != null) {
                // super.foo
                processSuperclassExpression(superclassExpr);
                return;
            }
            // may be literal, closure etc - not supported for now
            typeStack.push(UNKNOWN);
            fnCallStack.push(null);
            return;
        }
        String varName = ident.getText();
        LookupResult<ObjectInfo> lookup = context.lookup(varName);
        String type;
        if (lookup == null) {
            // type name? Like "Foo" in Foo.instance.bar()
            if (support.infos.get(varName) != null) {
                type = varName;
                Ref typeRef = support.ref(ident);
                typeRef.defKey = new DefKey(null, type);
                emit(typeRef);
            } else {
                varName = context.currentScope().getPathTo(varName, PATH_SEPARATOR);
                if (support.infos.get(varName) == null) {
                    type = UNKNOWN;
                } else {
                    type = varName;
                    Ref typeRef = support.ref(ident);
                    typeRef.defKey = new DefKey(null, type);
                    emit(typeRef);
                }
            }
        } else {
            type = lookup.getValue().getType();
            Ref identRef = support.ref(ident);
            identRef.defKey = new DefKey(null, lookup.getScope().getPathTo(varName, PATH_SEPARATOR));
            emit(identRef);
        }
        typeStack.push(type);
    }

    @Override
    public void exitExplicit_member_expression2(Explicit_member_expression2Context ctx) {

        if (support.firstPass) {
            return;
        }

        // foo.bar and foo.bar()
        boolean isFnCall = (ctx.getParent() instanceof Function_call_expressionContext);
        IdentifierContext ident = ctx.identifier();

        String parent = typeStack.pop();
        if (parent == UNKNOWN) {
            if (isFnCall) {
                fnCallStack.push(ident);
            }
            typeStack.push(UNKNOWN);
            return;
        }
        TypeInfo<Scope, String> props = support.infos.get(parent);
        if (props == null) {
            if (isFnCall) {
                fnCallStack.push(ident);
            }
            typeStack.push(UNKNOWN);
            return;
        }

        if (isFnCall) {
            // will deal later
            fnCallStack.push(ident);
            typeStack.push(parent);
            return;
        }

        String type = props.getProperty(DefKind.VAR, ident.getText());
        if (type == null) {
            type = UNKNOWN;
        } else {
            Ref propRef = support.ref(ident);
            propRef.defKey = new DefKey(null, parent + PATH_SEPARATOR + ident.getText());
            emit(propRef);
        }
        typeStack.push(type);
    }

    @Override
    public void exitPostfix_self_expression(Postfix_self_expressionContext ctx) {

        if (support.firstPass) {
            return;
        }

        // foo.self
        String parent = typeStack.pop();
        if (parent == UNKNOWN) {
            typeStack.push(UNKNOWN);
            return;
        }
        TypeInfo<Scope, String> props = support.infos.get(parent);
        if (props == null) {
            typeStack.push(UNKNOWN);
            return;
        }
        Self_kwContext ident = ctx.self_kw();
        Ref propRef = support.ref(ident);
        propRef.defKey = new DefKey(null, parent);
        emit(propRef);
        typeStack.push(parent);
    }

    @Override
    public void exitFunction_call_expression(Function_call_expressionContext ctx) {

        if (support.firstPass) {
            return;
        }

        String signature = signature(ctx.parenthesized_expression());

        if (!(ctx.postfix_expression() instanceof PrimaryContext)) {
            // foo.bar()
            String parent = typeStack.pop();
            if (parent == UNKNOWN) {
                typeStack.push(UNKNOWN);
                fnCallStack.pop();
                return;
            }
            TypeInfo<Scope, String> props = support.infos.get(parent);
            if (props == null) {
                typeStack.push(UNKNOWN);
                fnCallStack.pop();
                return;
            }
            processFnCallRef(props, signature, null, null);
            return;
        }
        // bar() or Bar() - function or ctor
        ParserRuleContext fnCallNameCtx = fnCallStack.peek();

        TypeInfo<Scope, String> props;

        String className;

        if (fnCallNameCtx != null) {
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
        String methodName;

        if (props != null) {
            methodName = CTOR;
        } else {
            props = support.infos.getRoot();
            methodName = null;
            className = currentClass;
        }
        processFnCallRef(props, signature, methodName, className);
    }

    @Override
    public void exitExpression(ExpressionContext ctx) {
        // employees must wash hands before returning to work
        fnCallStack.empty();
    }

    @Override
    public void enterIf_statement(If_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
        processBindingConditions(ctx.condition_clause());
    }

    @Override
    public void exitIf_statement(If_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterWhile_statement(While_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
        processBindingConditions(ctx.condition_clause());
    }

    @Override
    public void exitWhile_statement(While_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterRepeat_while_statement(Repeat_while_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitRepeat_while_statement(Repeat_while_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterDo_statement(Do_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitDo_statement(Do_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterDefer_statement(Defer_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitDefer_statement(Defer_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterElse_statement(Else_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitElse_statement(Else_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterCatch_clause(Catch_clauseContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitCatch_clause(Catch_clauseContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterGuard_statement(Guard_statementContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
        processBindingConditions(ctx.condition_clause());
    }

    @Override
    public void exitGuard_statement(Guard_statementContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterSwitch_case(Switch_caseContext ctx) {
        context.enterScope(context.currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitSwitch_case(Switch_caseContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterType_casting_operator(Type_casting_operatorContext ctx) {
        emitTypeRef(ctx.type());
    }

    private void processFnCallRef(TypeInfo<Scope, String> props,
                                  String signature,
                                  String methodName,
                                  String currentClass) {
        ParserRuleContext fnIdent = fnCallStack.pop();
        if (fnIdent == null) {
            // TODO(alexsaveliev) ".file(a, b)"
            return;
        }
        // looking for matching function
        if (methodName == null) {
            methodName = fnIdent.getText();
        }
        String fnPath = methodName + '(' + signature + ')';

        String type = null;

        if (currentClass != null) {
            // lookup in current class
            TypeInfo<Scope, String> currentProps = support.infos.get(currentClass);
            type = currentProps.getProperty(DefKind.FUNC, fnPath);
            if (type != null) {
                props = currentProps;
            }
        }

        if (type == null) {
            if (CTOR.equals(methodName)) {
                type = currentClass;
            } else {
                type = props.getProperty(DefKind.FUNC, fnPath);
            }
        }

        if (type != null) {
            Ref methodRef = support.ref(fnIdent);
            Scope scope = props.getData();
            if (scope != null) {
                methodRef.defKey = new DefKey(null, scope.getPathTo(fnPath, PATH_SEPARATOR));
                emit(methodRef);
            }
            typeStack.push(type);
        } else {
            typeStack.push(UNKNOWN);
        }
    }

    /**
     * Emits parent type(s)
     */
    private void emitParentTypeRefs(Type_inheritance_clauseContext ctx) {
        if (ctx == null) {
            return;
        }
        emitParentTypeRefs(ctx.type_inheritance_list());
    }

    /**
     * Emits parent type(s)
     */
    private void emitParentTypeRefs(Type_inheritance_listContext ctx) {
        if (ctx == null) {
            return;
        }
        Type_identifierContext type = ctx.type_identifier();
        if (type != null) {
            Type_nameContext typeName = type.type_name();
            Ref typeRef = support.ref(typeName);
            typeRef.defKey = new DefKey(null, typePath(typeName.getText()));
            emit(typeRef);
            emitParentTypeRefs(ctx.type_inheritance_list());
        }
    }

    /**
     * Emits generic parameter refs in statements class Foo<A,B,C>
     */
    private void emitGenericParameterRefs(Generic_parameter_clauseContext ctx) {
        if (ctx == null) {
            return;
        }
        for (Generic_parameterContext param : ctx.generic_parameter_list().generic_parameter()) {
            Type_nameContext typeName = param.type_name();
            Ref typeRef = support.ref(typeName);
            typeRef.defKey = new DefKey(null, typePath(typeName.getText()));
            emit(typeRef);
        }
    }

    /**
     * Emits generic argument refs in generic types
     */
    private void emitGenericArgumentRefs(Generic_argument_clauseContext ctx) {
        if (ctx == null) {
            return;
        }
        for (Generic_argumentContext argument : ctx.generic_argument_list().generic_argument()) {
            Ref typeRef = support.ref(argument);
            typeRef.defKey = new DefKey(null, typePath(argument.getText()));
            emit(typeRef);
        }
    }

    private void processFunctionParameters(Parameter_clausesContext ctx,
                                           FunctionParameters params) {
        if (ctx == null) {
            return;
        }
        processFunctionParameters(ctx.parameter_clause(), params);
        processFunctionParameters(ctx.parameter_clauses(), params);
    }

    private void processFunctionParameters(Parameter_clauseContext paramClause,
                                           FunctionParameters params) {
        if (paramClause != null) {
            Parameter_listContext paramList = paramClause.parameter_list();
            if (paramList != null) {
                List<ParameterContext> paramz = paramList.parameter();
                if (paramz != null) {

                    String signature;

                    int counter = 0;
                    for (ParameterContext param : paramz) {
                        Local_parameter_nameContext localParameterName = param.local_parameter_name();
                        External_parameter_nameContext externalParameterName = param.external_parameter_name();
                        if (externalParameterName == null) {
                            if (counter == 0) {
                                // fist parameter may be _
                                signature = "_";
                            } else {
                                signature = localParameterName.getText();
                            }
                        } else {
                            signature = externalParameterName.getText();
                        }
                        Def paramDef = support.def(localParameterName, DefKind.PARAM);
                        Type_identifierContext typeNameCtx = extractTypeName(param.type_annotation().type());
                        String typeName;
                        if (typeNameCtx != null) {
                            typeName = typeNameCtx.getText();
                            Ref typeRef = support.ref(typeNameCtx);
                            typeRef.defKey = new DefKey(null, typePath(typeName));
                            emit(typeRef);
                        } else {
                            typeName = VOID;
                        }
                        paramDef.format(StringUtils.EMPTY, typeName, DefData.SEPARATOR_SPACE);
                        paramDef.defData.setKind("argument");
                        String argName = localParameterName.getText();
                        context.currentScope().put(argName, new ObjectInfo(typeName));
                        String representation = argName + ": " + typeName;
                        params.params.add(new FunctionParameter(argName,
                                typeName,
                                representation,
                                signature,
                                paramDef));
                        counter++;
                    }
                }
            }
        }
    }

    private Type_identifierContext extractTypeName(TypeContext ctx) {
        if (ctx == null) {
            return null;
        }
        Type_identifierContext typeIdentifierContext = ctx.type_identifier();
        if (typeIdentifierContext != null) {
            return typeIdentifierContext;
        }
        return extractTypeName(ctx.type(0));
    }

    private void processVariableOrConstants(List<Pattern_initializerContext> list,
                                            String defKind,
                                            String keyword,
                                            String printableKind) {
        for (Pattern_initializerContext item : list) {
            PatternContext pattern = item.pattern();
            Type_annotationContext typeAnnotation = pattern.type_annotation();
            InitializerContext initializer = item.initializer();
            processVariableOrConstant(pattern, typeAnnotation, initializer, defKind, keyword, printableKind);
        }
    }

    private void processVariableOrConstant(PatternContext pattern,
                                           Type_annotationContext typeAnnotation,
                                           InitializerContext initializer,
                                           String defKind,
                                           String keyword,
                                           String printableKind) {
        Identifier_patternContext ident = pattern.identifier_pattern();
        if (ident == null) {
            // TODO (alexsaveliev) "case .PropertyList(let format, let options):"
            return;
        }
        Def def = support.def(ident, defKind);
        def.defKey = new DefKey(null, context.currentScope().getPathTo(def.name, PATH_SEPARATOR));
        TypeContext typeContext;
        if (typeAnnotation == null) {
            typeContext = null;
        } else {
            typeContext = typeAnnotation.type();
        }
        Type_identifierContext type = extractTypeName(typeContext);
        String typeName;
        if (type == null) {
            typeName = guessType(initializer);
        } else {
            emitGenericArgumentRefs(type.generic_argument_clause());
            Type_nameContext typeNameContext = type.type_name();
            Ref typeRef = support.ref(typeNameContext);
            typeName = typeNameContext.getText();
            typeRef.defKey = new DefKey(null, typePath(typeName));
            emit(typeRef);
        }
        def.format(keyword,
                typeName,
                DefData.SEPARATOR_SPACE);
        def.defData.setKind(printableKind);
        emit(def);
        context.currentScope().put(def.name, new ObjectInfo(typeName));
        if (!isInFunction) {
            support.infos.setProperty(context.getPath(PATH_SEPARATOR), DefKind.VAR, def.name, typeName);
        }
    }

    private String guessType(InitializerContext ctx) {

        if (!typeStack.empty()) {
            String ret = typeStack.peek();
            if (ret != UNKNOWN) {
                return ret;
            }
        }
        if (ctx == null) {
            return UNKNOWN;
        }
        ExpressionContext expr = ctx.expression();
        Prefix_expressionContext prefixExpr = expr.prefix_expression();
        if (prefixExpr == null) {
            return UNKNOWN;
        }
        Postfix_expressionContext postfixExpr = prefixExpr.postfix_expression();
        if (postfixExpr == null) {
            return UNKNOWN;
        }
        if (postfixExpr instanceof Function_call_expressionContext) {
            Function_call_expressionContext fnCallExpr = (Function_call_expressionContext) postfixExpr;
            postfixExpr = fnCallExpr.postfix_expression();
        }
        ParseTree firstChild = postfixExpr.getChild(0);
        if (firstChild == null || !(firstChild instanceof Primary_expressionContext)) {
            return UNKNOWN;
        }
        Primary_expressionContext primaryExpr = (Primary_expressionContext) firstChild;
        Literal_expressionContext literalExpr = primaryExpr.literal_expression();
        if (literalExpr == null) {
            return UNKNOWN;
        }
        LiteralContext literal = literalExpr.literal();
        if (literal == null) {
            if (literalExpr.array_literal() != null) {
                return "Array";
            }
            if (literalExpr.dictionary_literal() != null) {
                return "Dictionary";
            }
            return UNKNOWN;
        }
        if (literal.boolean_literal() != null) {
            return "Bool";
        }

        if (literal.string_literal() != null) {
            return "String";
        }

        Numeric_literalContext numeric = literal.numeric_literal();
        if (numeric != null) {
            if (numeric.integer_literal() != null) {
                return "Int";
            }
            return "Double";
        }

        return UNKNOWN;
    }

    /**
     * Handles self and self.foo expressions
     */
    private void processSelfExpression(Self_expressionContext ctx) {
        IdentifierContext ident = ctx.identifier();
        if (ident != null) {
            // self.foo
            // is it a method call or property access?
            if (ctx.getParent().getParent() instanceof Function_call_expressionContext) {
                // method call, will deal with it later
                fnCallStack.push(ident);
                typeStack.push(currentClass == null ? UNKNOWN : currentClass);
            } else {
                // property access
                String propertyName = ident.getText();
                TypeInfo<Scope, String> typeInfo = support.infos.get(currentClass);
                if (typeInfo == null) {
                    typeStack.push(UNKNOWN);
                    return;
                }
                String type = typeInfo.getProperty(DefKind.VAR, propertyName);
                typeStack.push(type == null ? UNKNOWN : type);
                Ref propertyRef = support.ref(ident);
                Scope scope = typeInfo.getData();
                if (scope != null) {
                    propertyRef.defKey = new DefKey(null, scope.getPathTo(propertyName, PATH_SEPARATOR));
                }
                emit(propertyRef);
            }
            return;
        }
        Init_kwContext initKwContext = ctx.init_kw();
        if (initKwContext != null) {
            // self.init, will deal with it later
            fnCallStack.push(initKwContext);
            typeStack.push(currentClass == null ? UNKNOWN : currentClass);
            return;
        }
        // self or self[]
        typeStack.push(currentClass == null ? UNKNOWN : currentClass);
    }

    /**
     * Handles super.foo, super.init, and super[]
     */
    private void processSuperclassExpression(Superclass_expressionContext ctx) {
        Superclass_method_expressionContext superMethodExpr = ctx.superclass_method_expression();
        if (superMethodExpr != null) {
            // super.foo() will deal with it later
            fnCallStack.push(superMethodExpr.identifier());
            typeStack.push(currentClass == null ? UNKNOWN : currentClass);
            return;
        }
        Superclass_initializer_expressionContext superInitExpr = ctx.superclass_initializer_expression();
        if (superInitExpr != null) {
            // super.init, will deal with it later
            fnCallStack.push(superInitExpr.init_kw());
            typeStack.push(currentClass == null ? UNKNOWN : currentClass);
            return;
        }
        // super[]
        typeStack.push(currentClass == null ? UNKNOWN : currentClass);
    }

    private String signature(Parenthesized_expressionContext ctx) {
        if (ctx == null) {
            return StringUtils.EMPTY;
        }
        Expression_element_listContext elList = ctx.expression_element_list();
        if (elList == null) {
            return StringUtils.EMPTY;
        }
        List<Expression_elementContext> elements = elList.expression_element();
        if (elements == null) {
            return StringUtils.EMPTY;
        }
        Collection<String> params = new LinkedList<>();

        for (Expression_elementContext element : elements) {

            String name;
            IdentifierContext ident = element.identifier();
            if (ident == null) {
                name = "_";
            } else {
                name = ident.getText();
            }
            params.add(name);
        }
        return StringUtils.join(params, ',');
    }

    private void emit(Def def) {
        if (support.firstPass) {
            support.emit(def);
        }
    }

    private void emit(Ref ref) {
        if (!support.firstPass) {
            support.emit(ref);
        }
    }

    private void processBindingConditions(Condition_clauseContext ctx) {
        Condition_listContext conditions = ctx.condition_list();
        if (conditions == null) {
            return;
        }
        for (ConditionContext condition : conditions.condition()) {
            Optional_binding_conditionContext binding = condition.optional_binding_condition();
            if (binding == null) {
                continue;
            }
            Optional_binding_headContext head = binding.optional_binding_head();
            PatternContext pattern = head.pattern();

            String keyword;
            String kind;
            String printable;
            if (head.let() != null) {
                keyword = "let";
                kind = DefKind.LET;
                printable = "constant";
            } else {
                keyword = "var";
                kind = DefKind.VAR;
                printable = "variable";
            }
            processVariableOrConstant(pattern,
                    pattern.type_annotation(),
                    head.initializer(),
                    kind,
                    keyword,
                    printable);
        }
    }

    /**
     * Tries to resolve type name. It might refer to inner class
     * @param typeName
     * @return
     */
    private String typePath(String typeName) {
        String fqn = context.currentScope().getPathTo(typeName, PATH_SEPARATOR);
        if(support.infos.get(fqn) != null) {
            return fqn;
        }
        return typeName;
    }

    private void emitTypeRef(TypeContext ctx) {
        if (ctx == null) {
            return;
        }
        Type_identifierContext ident = ctx.type_identifier();
        if (ident != null) {
            Type_nameContext typeName = ident.type_name();
            if (typeName != null) {
                Ref typeRef = support.ref(typeName);
                typeRef.defKey = new DefKey(null, typePath(typeName.getText()));
                emit(typeRef);
            }
            emitGenericArgumentRefs(ident.generic_argument_clause());
        }
        List<TypeContext> types = ctx.type();
        if (types == null) {
            return;
        }
        for (TypeContext t : types) {
            emitTypeRef(t);
        }
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