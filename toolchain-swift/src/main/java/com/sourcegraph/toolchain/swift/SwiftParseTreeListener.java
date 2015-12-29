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

    private static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    private Context context = new Context();

    private Stack<String> typeStack = new Stack<>();

    SwiftParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterProtocol_declaration(Protocol_declarationContext ctx) {
        // protocol Foo
        Def protoDef = support.def(ctx.protocol_name(), DefKind.PROTOCOL);
        protoDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + protoDef.name);
        protoDef.format("protocol", "protocol", DefData.SEPARATOR_SPACE);
        support.emit(protoDef);
        // protocol Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());

        context.enterScope(new Scope(protoDef.name));
    }

    @Override
    public void exitProtocol_declaration(Protocol_declarationContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterExtension_declaration(Extension_declarationContext ctx) {
        // extension Foo
        Type_nameContext typeName = ctx.type_identifier().type_name();
        Ref extensionRef = support.ref(typeName);
        extensionRef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + typeName.getText());
        support.emit(extensionRef);
        // extension Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());

        context.enterScope(new Scope(typeName.getText()));
    }

    @Override
    public void exitExtension_declaration(Extension_declarationContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterClass_declaration(Class_declarationContext ctx) {
        // class Foo
        Def classDef = support.def(ctx.class_name(), DefKind.CLASS);
        classDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + classDef.name);
        classDef.format("class", "class", DefData.SEPARATOR_SPACE);
        support.emit(classDef);
        // class Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());
        // class Foo<Bar>
        emitGenericParameterRefs(ctx.generic_parameter_clause());

        context.enterScope(new Scope(classDef.name));
        support.infos.put(classDef.name, new TypeInfo<>());
    }

    @Override
    public void exitClass_declaration(Class_declarationContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterStruct_declaration(Struct_declarationContext ctx) {
        // struct Foo
        Def structDef = support.def(ctx.struct_name(), DefKind.STRUCT);
        structDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + structDef.name);
        structDef.format("struct", "struct", DefData.SEPARATOR_SPACE);
        support.emit(structDef);
        // struct Foo: Bar, Baz
        emitParentTypeRefs(ctx.type_inheritance_clause());
        // struct Foo<Bar>
        emitGenericParameterRefs(ctx.generic_parameter_clause());

        context.enterScope(new Scope(structDef.name));
        support.infos.put(structDef.name, new TypeInfo<>());
    }

    @Override
    public void exitStruct_declaration(Struct_declarationContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterEnum_declaration(Enum_declarationContext ctx) {

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

        enumDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + enumDef.name);
        enumDef.format("enum", "enum", DefData.SEPARATOR_SPACE);

        support.emit(enumDef);
        context.enterScope(new Scope(enumDef.name));
    }

    @Override
    public void exitEnum_declaration(Enum_declarationContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterFunction_declaration(Function_declarationContext ctx) {

        emitGenericParameterRefs(ctx.generic_parameter_clause());

        IdentifierContext nameCtx = ctx.function_name().identifier();
        if (nameCtx == null) {
            return;
        }
        Def fnDef = support.def(nameCtx, DefKind.FUNC);

        String path = context.getPath(PATH_SEPARATOR);
        fnDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + fnDef.name);

        context.enterScope(new Scope(fnDef.name));

        Function_signatureContext signatureContext = ctx.function_signature();
        Function_resultContext resultContext = signatureContext.function_result();
        Collection<String> paramRepr = new LinkedList<>();
        processFunctionParameters(signatureContext.parameter_clauses(), paramRepr);
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
        StringBuilder signature = new StringBuilder().append('(').append(StringUtils.join(paramRepr, ", ")).append(')');
        signature.append(' ').append(type);
        fnDef.format("func",
                signature.toString(),
                DefData.SEPARATOR_EMPTY);

        support.emit(fnDef);
        support.infos.get(path).addProperty(DefKind.FUNC, fnDef.name, type);
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

        emitGenericParameterRefs(ctx.generic_parameter_clause());

        Def fnDef = support.def(ctx.initializer_head().init_kw(), DefKind.FUNC);
        fnDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + fnDef.name);

        context.enterScope(new Scope(fnDef.name));

        Collection<String> paramRepr = new LinkedList<>();
        processFunctionParameters(ctx.parameter_clause(), paramRepr);
        String type = context.currentScope().getName();
        StringBuilder signature = new StringBuilder().append('(').append(StringUtils.join(paramRepr, ", ")).append(')');
        signature.append(' ').append(type);
        fnDef.format("func",
                signature.toString(),
                DefData.SEPARATOR_EMPTY);

        support.emit(fnDef);
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
            varDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + varDef.name);
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
        enumCaseDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + enumCaseDef.name);
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
        // foo
        IdentifierContext ident = ctx.identifier();
        if (ident == null) {
            typeStack.push(UNKNOWN);
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
            identRef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR, lookup.getDepth()) + varName);
            support.emit(identRef);
        }
        typeStack.push(type);
    }

    @Override
    public void exitExplicit_member_expression2(Explicit_member_expression2Context ctx) {
        // foo.bar
        String parent = typeStack.pop();
        if (parent == UNKNOWN) {
            typeStack.push(UNKNOWN);
            return;
        }
        TypeInfo<String> props = support.infos.get(parent);
        if (props == null) {
            typeStack.push(UNKNOWN);
            return;
        }
        IdentifierContext ident = ctx.identifier();
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
    public void exitInitializer_expression(Initializer_expressionContext ctx) {
        // foo.init
        String parent = typeStack.pop();
        if (parent == UNKNOWN) {
            typeStack.push(UNKNOWN);
            return;
        }
        TypeInfo<String> props = support.infos.get(parent);
        if (props == null) {
            typeStack.push(UNKNOWN);
            return;
        }
        Init_kwContext ident = ctx.init_kw();
        Ref propRef = support.ref(ident);
        propRef.defKey = new DefKey(null, parent + PATH_SEPARATOR + ident.getText()); // TODO
        support.emit(propRef);
        typeStack.push(parent);
    }

    @Override
    public void exitPostfix_self_expression(Postfix_self_expressionContext ctx) {
        // foo.self
        String parent = typeStack.pop();
        if (parent == UNKNOWN) {
            typeStack.push(UNKNOWN);
            return;
        }
        TypeInfo<String> props = support.infos.get(parent);
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
        // foo.bar()
        String parent = typeStack.pop();
        if (parent == UNKNOWN) {
            typeStack.push(UNKNOWN);
            return;
        }
        TypeInfo<String> props = support.infos.get(parent);
        if (props == null) {
            typeStack.push(UNKNOWN);
            return;
        }
        // TODO
        typeStack.push(UNKNOWN);
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

    private void processFunctionParameters(Parameter_clausesContext ctx, Collection<String> repr) {
        if (ctx == null) {
            return;
        }
        processFunctionParameters(ctx.parameter_clause(), repr);
        processFunctionParameters(ctx.parameter_clauses(), repr);
    }

    private void processFunctionParameters(Parameter_clauseContext paramClause, Collection<String> repr) {
        if (paramClause != null) {
            Parameter_listContext paramList = paramClause.parameter_list();
            if (paramList != null) {
                List<ParameterContext> params = paramList.parameter();
                if (params != null) {
                    for (ParameterContext param : params) {
                        Local_parameter_nameContext localParameterName = param.local_parameter_name();
                        External_parameter_nameContext externalParameterName = param.external_parameter_name();
                        Def paramDef = support.def(localParameterName, DefKind.PARAM);
                        paramDef.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + paramDef.name);
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
            def.defKey = new DefKey(null, context.getPrefix(PATH_SEPARATOR) + def.name);
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


}