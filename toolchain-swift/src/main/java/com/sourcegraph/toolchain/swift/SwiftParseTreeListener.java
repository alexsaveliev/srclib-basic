package com.sourcegraph.toolchain.swift;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.LookupResult;
import com.sourcegraph.toolchain.language.Scope;
import com.sourcegraph.toolchain.language.TypeInfo;
import com.sourcegraph.toolchain.swift.antlr4.SwiftBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.sourcegraph.toolchain.swift.antlr4.SwiftParser.*;

class SwiftParseTreeListener extends SwiftBaseListener {

    private static final String VOID = "Void";
    private static final String UNKNOWN = "?";

    private static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    private Context context = new Context();

    private Stack<String> typeStack = new Stack<>();

    private Stack<ParserRuleContext> fnCallStack = new Stack<>();

    private String currentClass;

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
        support.emit(protoDef);
        // protocol Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());

        context.enterScope(new Scope(protoDef.name, prefix));
        support.infos.put(protoDef.name,
                new TypeInfo<Scope, String>().setData(new Scope(protoDef.name, prefix)));

        currentClass = protoDef.name;
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
        support.emit(extensionRef);
        // extension Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());

        context.enterScope(new Scope(typeName.getText(), prefix));

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
        support.emit(classDef);
        // class Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());
        // class Foo<Bar>
        emitGenericParameterRefs(ctx.generic_parameter_clause());

        context.enterScope(new Scope(classDef.name, prefix));
        support.infos.put(classDef.name,
                new TypeInfo<Scope, String>().setData(new Scope(classDef.name, prefix)));

        currentClass = classDef.name;
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
        support.emit(structDef);
        // struct Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());
        // struct Foo<Bar>
        emitGenericParameterRefs(ctx.generic_parameter_clause());

        context.enterScope(new Scope(structDef.name, prefix));
        support.infos.put(structDef.name,
                new TypeInfo<Scope, String>().setData(new Scope(structDef.name, prefix)));

        currentClass = structDef.name;
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

        support.emit(enumDef);
        context.enterScope(new Scope(enumDef.name, prefix));
        support.infos.put(enumDef.name,
                new TypeInfo<Scope, String>().setData(new Scope(enumDef.name, prefix)));

        currentClass = enumDef.name;
    }

    @Override
    public void exitEnum_declaration(Enum_declarationContext ctx) {
        context.exitScope();
        currentClass = null;
    }

    @Override
    public void enterFunction_declaration(Function_declarationContext ctx) {

        emitGenericParameterRefs(ctx.generic_parameter_clause());

        IdentifierContext nameCtx = ctx.function_name().identifier();
        if (nameCtx == null) {
            return;
        }
        Def fnDef = support.def(nameCtx, DefKind.FUNC);

        String prefix = context.getPrefix(PATH_SEPARATOR);

        String path = context.getPath(PATH_SEPARATOR);

        context.enterScope(new Scope(fnDef.name, prefix));

        Function_signatureContext signatureContext = ctx.function_signature();
        Function_resultContext resultContext = signatureContext.function_result();
        Collection<String> paramRepr = new LinkedList<>();
        Collection<String> signature = new LinkedList<>();
        processFunctionParameters(signatureContext.parameter_clauses(), signature, paramRepr);
        String type;
        if (resultContext != null) {
            Type_identifierContext typeNameCtx = extractTypeName(resultContext.type());
            if (typeNameCtx != null) {
                type = typeNameCtx.getText();
                Ref typeRef = support.ref(typeNameCtx);
                typeRef.defKey = new DefKey(null, type);
                support.emit(typeRef);
            } else {
                type = VOID;
            }
        } else {
            type = VOID;
        }
        StringBuilder repr = new StringBuilder().append('(').append(StringUtils.join(paramRepr, ", ")).append(')');
        repr.append(' ').append(type);


        String fnPath = fnDef.name + '(' + StringUtils.join(signature, ',') + ')';
        fnDef.defKey = new DefKey(null, prefix + fnPath);
        fnDef.format("func",
                repr.toString(),
                DefData.SEPARATOR_EMPTY);

        support.emit(fnDef);
        support.infos.get(path).addProperty(DefKind.FUNC, fnPath, type);
    }

    @Override
    public void exitFunction_declaration(Function_declarationContext ctx) {

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

        context.enterScope(new Scope(fnDef.name, prefix));

        Collection<String> paramRepr = new LinkedList<>();
        Collection<String> signature = new LinkedList<>();
        processFunctionParameters(ctx.parameter_clause(), signature, paramRepr);
        fnDef.format(StringUtils.EMPTY,
                type + '(' + StringUtils.join(paramRepr, ", ") + ')',
                DefData.SEPARATOR_EMPTY);

        String fnPath = "init(" + StringUtils.join(signature, ',') + ')';
        fnDef.defKey = new DefKey(null, prefix + fnPath);

        support.emit(fnDef);

        support.infos.get(path).addProperty(DefKind.FUNC, fnPath, type);
    }

    @Override
    public void exitInitializer_declaration(Initializer_declarationContext ctx) {
        context.exitScope();
    }


    @Override
    public void enterConstant_declaration(Constant_declarationContext ctx) {
        processVariableOrConstants(ctx.pattern_initializer_list().pattern_initializer(),
                DefKind.LET, "let", "constant");
    }

    @Override
    public void enterVariable_declaration(Variable_declarationContext ctx) {
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
                typeRef.defKey = new DefKey(null, typeName);
                support.emit(typeRef);
            } else {
                typeName = guessType(ctx.initializer());
            }
            varDef.format(StringUtils.EMPTY, typeName, DefData.SEPARATOR_SPACE);
            varDef.defData.setKind("variable");
            support.emit(varDef);
            context.currentScope().put(varDef.name, typeName);
            support.infos.get(context.getPath(PATH_SEPARATOR)).addProperty(DefKind.VAR, varDef.name, typeName);
        }
    }

    @Override
    public void enterEnum_case_name(Enum_case_nameContext ctx) {
        if (ctx.getParent() instanceof Enum_case_patternContext) {
            return;
        }
        Def enumCaseDef = support.def(ctx, DefKind.CASE);
        enumCaseDef.defKey = new DefKey(null, context.currentScope().getPathTo(enumCaseDef.name, PATH_SEPARATOR));
        support.emit(enumCaseDef);
    }

    @Override
    public void enterBinary_expression(Binary_expressionContext ctx) {
        // handling a = b case only
        Binary_operatorContext op = ctx.binary_operator();
        if (op == null) {
            return;
        }
        if (!"=".equals(op.getText())) {
            return;
        }
    }

    @Override
    public void exitPrimary_expression(Primary_expressionContext ctx) {

        // foo or foo()
        IdentifierContext ident = ctx.identifier();
        if (ctx.getParent().getParent() instanceof Function_call_expressionContext) {
            fnCallStack.push(ident);
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
            return;
        }
        String varName = ident.getText();
        LookupResult lookup = context.lookup(varName);
        String type;
        if (lookup == null) {
            type = UNKNOWN;
        } else {
            type = lookup.getValue();
            Ref identRef = support.ref(ident);
            identRef.defKey = new DefKey(null, lookup.getScope().getPathTo(varName, PATH_SEPARATOR));
            support.emit(identRef);
        }
        typeStack.push(type);
    }

    @Override
    public void exitExplicit_member_expression2(Explicit_member_expression2Context ctx) {
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
            support.emit(propRef);
        }
        typeStack.push(type);
    }

    @Override
    public void exitPostfix_self_expression(Postfix_self_expressionContext ctx) {
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
        support.emit(propRef);
        typeStack.push(parent);
    }

    @Override
    public void exitFunction_call_expression(Function_call_expressionContext ctx) {

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
            processFnCallRef(props, signature, null);
            return;
        }
        // bar() or Bar() - function or ctor
        TypeInfo<Scope, String> props = support.infos.get(fnCallStack.peek().getText());
        String methodName;
        if (props != null) {
            methodName = "init";
        } else {
            props = support.infos.getRoot();
            methodName = null;
        }
        processFnCallRef(props, signature, methodName);
    }

    private void processFnCallRef(TypeInfo<Scope, String> props, String signature, String methodName) {
        ParserRuleContext fnIdent = fnCallStack.pop();
        // looking for matching function
        if (methodName == null) {
            methodName = fnIdent.getText();
        }
        String fnPath = methodName + '(' + signature + ')';
        String type = props.getProperty(DefKind.FUNC, fnPath);
        if (type != null) {
            Ref methodRef = support.ref(fnIdent);
            methodRef.defKey = new DefKey(null, props.getData().getPathTo(fnPath, PATH_SEPARATOR));
            support.emit(methodRef);
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
            typeRef.defKey = new DefKey(null, typeName.getText());
            support.emit(typeRef);
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
            typeRef.defKey = new DefKey(null, typeName.getText());
            support.emit(typeRef);
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
            typeRef.defKey = new DefKey(null, argument.getText());
            support.emit(typeRef);
        }
    }

    private void processFunctionParameters(Parameter_clausesContext ctx,
                                           Collection<String> signature,
                                           Collection<String> repr) {
        if (ctx == null) {
            return;
        }
        processFunctionParameters(ctx.parameter_clause(), signature, repr);
        processFunctionParameters(ctx.parameter_clauses(), signature, repr);
    }

    private void processFunctionParameters(Parameter_clauseContext paramClause,
                                           Collection<String> signature,
                                           Collection<String> repr) {
        if (paramClause != null) {
            Parameter_listContext paramList = paramClause.parameter_list();
            if (paramList != null) {
                List<ParameterContext> params = paramList.parameter();
                if (params != null) {
                    for (ParameterContext param : params) {
                        Local_parameter_nameContext localParameterName = param.local_parameter_name();
                        External_parameter_nameContext externalParameterName = param.external_parameter_name();
                        if (externalParameterName == null) {
                            signature.add("_");
                        } else {
                            signature.add(externalParameterName.getText());
                        }
                        Def paramDef = support.def(localParameterName, DefKind.PARAM);
                        paramDef.defKey = new DefKey(null, context.currentScope().getPathTo(paramDef.name, PATH_SEPARATOR));
                        Type_identifierContext typeNameCtx = extractTypeName(param.type_annotation().type());
                        String typeName;
                        if (typeNameCtx != null) {
                            typeName = typeNameCtx.getText();
                            Ref typeRef = support.ref(typeNameCtx);
                            typeRef.defKey = new DefKey(null, typeName);
                            support.emit(typeRef);
                        } else {
                            typeName = VOID;
                        }
                        paramDef.format(StringUtils.EMPTY, typeName, DefData.SEPARATOR_SPACE);
                        paramDef.defData.setKind("argument");
                        support.emit(paramDef);
                        String argName = localParameterName.getText();
                        context.currentScope().put(argName, typeName);
                        repr.add(typeName + ' ' + argName);
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
            Identifier_patternContext ident = pattern.identifier_pattern();
            Def def = support.def(ident, defKind);
            def.defKey = new DefKey(null, context.currentScope().getPathTo(def.name, PATH_SEPARATOR));
            Type_annotationContext typeAnnotationContext = pattern.type_annotation();
            TypeContext typeContext;
            if (typeAnnotationContext == null) {
                typeContext = null;
            } else {
                typeContext = typeAnnotationContext.type();
            }
            Type_identifierContext type = extractTypeName(typeContext);
            String typeName;
            if (type == null) {
                typeName = guessType(item.initializer());
            } else {
                emitGenericArgumentRefs(type.generic_argument_clause());
                Type_nameContext typeNameContext = type.type_name();
                Ref typeRef = support.ref(typeNameContext);
                typeName = typeNameContext.getText();
                typeRef.defKey = new DefKey(null, typeName);
                support.emit(typeRef);
            }
            def.format(keyword,
                    typeName,
                    DefData.SEPARATOR_SPACE);
            def.defData.setKind(printableKind);
            support.emit(def);
            context.currentScope().put(def.name, typeName);
            support.infos.get(context.getPath(PATH_SEPARATOR)).addProperty(DefKind.VAR, def.name, typeName);
        }
    }

    private String guessType(InitializerContext ctx) {
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
                propertyRef.defKey = new DefKey(null, typeInfo.getData().getPathTo(propertyName, PATH_SEPARATOR));
                support.emit(propertyRef);
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

}