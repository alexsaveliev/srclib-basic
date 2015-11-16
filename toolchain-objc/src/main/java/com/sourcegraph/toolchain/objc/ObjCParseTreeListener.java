package com.sourcegraph.toolchain.objc;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.objc.antlr4.ObjCBaseListener;
import com.sourcegraph.toolchain.objc.antlr4.ObjCLexer;
import com.sourcegraph.toolchain.objc.antlr4.ObjCParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class ObjCParseTreeListener extends ObjCBaseListener {

    private static final String[] PREDEFINED_TYPES = new String[]{
            "id", "void", "char", "short", "int", "long", "float", "double", "signed", "unsigned"
    };

    private LanguageImpl support;

    private String currentClassName;
    private String currentMethodName;

    private Map<String, String> paramsVars = new HashMap<>();
    private Stack<Map<String, Var>> localVars = new Stack<>();

    private int blockCounter;

    public ObjCParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterPreprocessor_declaration(ObjCParser.Preprocessor_declarationContext ctx) {

        String prefix;
        if (ctx.IMPORT() != null) {
            prefix = "#import";
        } else if (ctx.INCLUDE() != null) {
            prefix = "#include";
        } else {
            return;
        }
        String fileName = ctx.getText();
        if (!fileName.startsWith(prefix)) {
            return;
        }
        fileName = fileName.substring(prefix.length()).trim();
        // cut <> or ""
        fileName = fileName.substring(1, fileName.length() - 1);
        File include = new File(new File(support.getCurrentFile()).getParentFile(), fileName);
        if (include.isFile()) {
            support.process(include);
        }
    }

    @Override
    public void enterClass_implementation(ObjCParser.Class_implementationContext ctx) {

        localVars.push(new HashMap<>());
        currentClassName = ctx.class_name().getText();

        Ref interfaceRef = support.ref(ctx.class_name());
        interfaceRef.defKey = new DefKey(null, currentClassName);
        support.emit(interfaceRef);

        // registering "self" variable
        Map<String, String> currentClassVars = support.instanceVars.get(currentClassName);
        if (currentClassVars == null) {
            currentClassVars = new HashMap<>();
            support.instanceVars.put(currentClassName, currentClassVars);
        }
        currentClassVars.put("self", currentClassName);
    }

    @Override
    public void exitClass_implementation(ObjCParser.Class_implementationContext ctx) {
        localVars.pop();
        currentClassName = null;
    }

    @Override
    public void enterCategory_implementation(ObjCParser.Category_implementationContext ctx) {

        localVars.push(new HashMap<>());
        currentClassName = ctx.class_name().getText();

        Ref interfaceRef = support.ref(ctx.class_name());
        interfaceRef.defKey = new DefKey(null, currentClassName);
        support.emit(interfaceRef);

        // registering "self" variable
        Map<String, String> currentClassVars = support.instanceVars.get(currentClassName);
        if (currentClassVars == null) {
            currentClassVars = new HashMap<>();
            support.instanceVars.put(currentClassName, currentClassVars);
        }
        currentClassVars.put("self", currentClassName);
    }

    @Override
    public void exitCategory_implementation(ObjCParser.Category_implementationContext ctx) {
        localVars.pop();
        currentClassName = null;
    }

    @Override
    public void enterClass_method_definition(ObjCParser.Class_method_definitionContext ctx) {
        currentMethodName = getFuncName(ctx.method_definition().method_selector());
        localVars.push(new HashMap<>());
        processMethodDefinition(ctx.method_definition());
    }

    @Override
    public void exitClass_method_definition(ObjCParser.Class_method_definitionContext ctx) {
        currentMethodName = null;
        localVars.pop();
        paramsVars.clear();
    }

    @Override
    public void enterInstance_method_definition(ObjCParser.Instance_method_definitionContext ctx) {
        currentMethodName = getFuncName(ctx.method_definition().method_selector());
        localVars.push(new HashMap<>());
        processMethodDefinition(ctx.method_definition());
    }

    @Override
    public void exitInstance_method_definition(ObjCParser.Instance_method_definitionContext ctx) {
        currentMethodName = null;
        localVars.pop();
        paramsVars.clear();
    }

    @Override
    public void enterDeclaration(ObjCParser.DeclarationContext ctx) {
        List<ObjCParser.Storage_class_specifierContext> storageClassSpecifierContexts =
                ctx.declaration_specifiers().storage_class_specifier();
        boolean extern = storageClassSpecifierContexts != null && !storageClassSpecifierContexts.isEmpty() &&
                storageClassSpecifierContexts.get(0).getText().equals("extern");

        String typeName = null;
        for (ObjCParser.Type_specifierContext typeSpecifierContext : ctx.declaration_specifiers().type_specifier()) {
            String type = processTypeSpecifier(typeSpecifierContext);
            if (type != null) {
                typeName = type;
            }
        }
        ObjCParser.Init_declarator_listContext initDeclaratorListContext = ctx.init_declarator_list();
        if (initDeclaratorListContext == null) {
            List<ObjCParser.Type_specifierContext> typeSpecifierContexts = ctx.declaration_specifiers().type_specifier();
            if (typeSpecifierContexts.isEmpty()) {
                return;
            }
            ObjCParser.Type_specifierContext ident = typeSpecifierContexts.get(typeSpecifierContexts.size() - 1);
            if (ident.struct_or_union_specifier() == null &&
                    ident.class_name() == null &&
                    ident.enum_specifier() == null) {
                Def varDef = support.def(ident, "VAR");
                Map<String, String> vars = null;
                String defKey;
                if (currentClassName == null) {
                    if (currentMethodName == null) {
                        vars = support.globalVars;
                        defKey = varDef.name;
                    } else {
                        Var var = new Var(varDef.name, typeName);
                        localVars.peek().put(varDef.name, var);
                        defKey = var.defKey;
                    }
                } else {
                    if (currentMethodName == null) {
                        vars = support.instanceVars.get(currentClassName);
                        if (vars == null) {
                            vars = new HashMap<>();
                            support.instanceVars.put(currentClassName, vars);
                        }
                        defKey = currentDefKey(varDef.name);
                    } else {
                        Var var = new Var(varDef.name, typeName);
                        localVars.peek().put(varDef.name, var);
                        defKey = var.defKey;
                    }
                }
                varDef.defKey = new DefKey(null, defKey);
                support.emit(varDef);
                if (vars != null) {
                    vars.put(varDef.name, typeName);
                }
            }
        } else {
            for (ObjCParser.Init_declaratorContext context : initDeclaratorListContext.init_declarator()) {
                ObjCParser.DeclaratorContext declaratorContext = context.declarator();
                ParserRuleContext ident = ident(declaratorContext);
                if (ident == null) {
                    continue;
                }
                if (declaratorContext.direct_declarator().identifier() == null) {
                    // NSLog(a), looking at "(a)" here
                    Ref argRef = support.ref(ident);
                    String defKey = currentDefKey(ident.getText());
                    argRef.defKey = new DefKey(null, defKey);
                    support.emit(argRef);
                    continue;
                }

                if (extern) {
                    Ref externRef = support.ref(ident);
                    externRef.defKey = new DefKey(null, ident.getText());
                    support.emit(externRef);
                    support.globalVars.put(ident.getText(), typeName);
                } else {
                    Def varDef = support.def(ident, "VAR");
                    Map<String, String> vars = null;
                    String defKey;
                    if (currentClassName == null) {
                        if (currentMethodName == null) {
                            vars = support.globalVars;
                            defKey = varDef.name;
                        } else {
                            Var var = new Var(varDef.name, typeName);
                            localVars.peek().put(varDef.name, var);
                            defKey = var.defKey;
                        }
                    } else {
                        if (currentMethodName == null) {
                            vars = support.instanceVars.get(currentClassName);
                            if (vars == null) {
                                vars = new HashMap<>();
                                support.instanceVars.put(currentClassName, vars);
                            }
                            defKey = currentDefKey(varDef.name);
                        } else {
                            Var var = new Var(varDef.name, typeName);
                            localVars.peek().put(varDef.name, var);
                            defKey = var.defKey;
                        }
                    }
                    varDef.defKey = new DefKey(null, defKey);
                    support.emit(varDef);
                    if (vars != null) {
                        vars.put(varDef.name, typeName);
                    }
                }
            }
        }
    }

    @Override
    public void enterMessage_expression(ObjCParser.Message_expressionContext ctx) {

        // TODO (alexsaveliev): support property getter/setter methods
        if (currentMethodName == null) {
            return;
        }

        ObjCParser.Message_selectorContext messageSelectorContext = ctx.message_selector();

        String funcName = getFuncName(messageSelectorContext);
        String receiver = ctx.receiver().getText();
        String messageKey;

        if (receiver.equals("self") || receiver.equals("super")) {
            // TODO (alexsaveliev): separate super
            messageKey = currentClassName + '/' + funcName;
        } else {
            // class method?
            Var var = getLocalVariable(receiver);
            if (var != null) {
                messageKey = var.type + '/' + funcName;
            } else {
                String type = paramsVars.get(receiver);
                if (type != null) {
                    messageKey = type + '/' + funcName;
                } else {
                    Map<String, String> currentInstanceVars = support.instanceVars.get(currentClassName);
                    type = currentInstanceVars != null ? currentInstanceVars.get(receiver) : null;
                    if (type != null) {
                        messageKey = type + '/' + funcName;
                    } else {
                        type = support.globalVars.get(receiver);
                        if (type != null) {
                            messageKey = type + '/' + funcName;
                        } else {
                            messageKey = guessMessageKey(ctx.receiver(), funcName);
                        }
                    }
                }
            }
        }
        if (messageKey != null) {
            ParserRuleContext fnCallCtx;
            if (messageSelectorContext.selector() == null) {
                ObjCParser.Keyword_argumentContext keywordArgumentContext = messageSelectorContext.
                        keyword_argument(0);
                if (keywordArgumentContext != null) {
                    fnCallCtx = keywordArgumentContext.selector();
                } else {
                    // [x retain]
                    fnCallCtx = messageSelectorContext;
                }
            } else {
                fnCallCtx = messageSelectorContext.selector();
            }
            Ref fnCallRef = support.ref(fnCallCtx);
            fnCallRef.defKey = new DefKey(null, messageKey);
            support.emit(fnCallRef);
        }
    }

    private String guessMessageKey(ParseTree receiver, String funcName) {
        ObjCParser.ReceiverContext messageReceiver = getMessageReceiver(receiver);
        if (messageReceiver != null) {
            return guessMessageKey(messageReceiver, funcName);
        }
        String text = receiver.getText();
        int len = text.length();
        int state = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            switch (state) {
                case 0:
                    if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                        state = 1;
                    } else {
                        return null;
                    }
                    break;
                case 1:
                    if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
                        // ok
                    } else {
                        return null;
                    }
            }
        }
        return text + '/' + funcName;
    }

    private ObjCParser.ReceiverContext getMessageReceiver(ParseTree ctx) {
        if (ctx instanceof ObjCParser.Message_expressionContext) {
            return ((ObjCParser.Message_expressionContext) ctx).receiver();
        }
        if (ctx.getChildCount() == 1) {
            return getMessageReceiver(ctx.getChild(0));
        }
        return null;
    }

    @Override
    public void enterClass_interface(ObjCParser.Class_interfaceContext ctx) {

        // interface definition
        Def interfaceDef = support.def(ctx.class_name(), "CLASS");
        interfaceDef.defKey = new DefKey(null, interfaceDef.name);
        support.emit(interfaceDef);

        currentClassName = interfaceDef.name;
        Map<String, String> currentClassVars = support.instanceVars.get(currentClassName);
        if (currentClassVars == null) {
            currentClassVars = new HashMap<>();
            support.instanceVars.put(currentClassName, currentClassVars);
        }

        support.types.add(interfaceDef.name);

        // reference to superclass if any
        ObjCParser.Superclass_nameContext superclassNameContext = ctx.superclass_name();
        if (superclassNameContext != null) {
            Ref superInterfaceRef = support.ref(superclassNameContext);
            superInterfaceRef.defKey = new DefKey(null, superclassNameContext.getText());
            support.emit(superInterfaceRef);
        }

        // reference to protocols if any
        ObjCParser.Protocol_reference_listContext protocolReferenceListContext = ctx.protocol_reference_list();
        processProtocolReferences(protocolReferenceListContext);

        // instance variables
        ObjCParser.Instance_variablesContext instanceVariablesContext = ctx.instance_variables();
        processInstanceVariables(currentClassVars, instanceVariablesContext);

        // class and instance methods
        ObjCParser.Interface_declaration_listContext interfaceDeclarationListContext = ctx.interface_declaration_list();
        processDeclarationList(interfaceDeclarationListContext);
    }

    @Override
    public void exitClass_interface(ObjCParser.Class_interfaceContext ctx) {
        currentClassName = null;
    }

    @Override
    public void enterCategory_interface(ObjCParser.Category_interfaceContext ctx) {

        String interfaceName = ctx.class_name().getText();
        Ref interfaceRef = support.ref(ctx.class_name());
        interfaceRef.defKey = new DefKey(null, interfaceName);
        support.emit(interfaceRef);

        currentClassName = interfaceName;
        Map<String, String> currentClassVars = support.instanceVars.get(currentClassName);
        if (currentClassVars == null) {
            currentClassVars = new HashMap<>();
            support.instanceVars.put(currentClassName, currentClassVars);
        }

        support.types.add(interfaceName);

        // reference to protocols if any
        ObjCParser.Protocol_reference_listContext protocolReferenceListContext = ctx.protocol_reference_list();
        processProtocolReferences(protocolReferenceListContext);

        // instance variables
        ObjCParser.Instance_variablesContext instanceVariablesContext = ctx.instance_variables();
        processInstanceVariables(currentClassVars, instanceVariablesContext);

        // class and instance methods
        ObjCParser.Interface_declaration_listContext interfaceDeclarationListContext = ctx.interface_declaration_list();
        processDeclarationList(interfaceDeclarationListContext);
    }

    @Override
    public void exitCategory_interface(ObjCParser.Category_interfaceContext ctx) {
        currentClassName = null;
    }

    @Override
    public void enterProtocol_declaration(ObjCParser.Protocol_declarationContext ctx) {
        // TODO (alexsaveliev)
    }

    @Override
    public void exitProtocol_declaration(ObjCParser.Protocol_declarationContext ctx) {
        currentClassName = null;
    }

    @Override
    public void enterProtocol_declaration_list(ObjCParser.Protocol_declaration_listContext ctx) {

        ObjCParser.Protocol_listContext protocolListContext = ctx.protocol_list();
        if (protocolListContext != null) {
            List<ObjCParser.Protocol_nameContext> protocolNameContexts = protocolListContext.protocol_name();
            if (protocolNameContexts != null) {
                for (ObjCParser.Protocol_nameContext protocolNameContext : protocolNameContexts) {
                    Ref protocolRef = support.ref(protocolNameContext);
                    protocolRef.defKey = new DefKey(null, protocolNameContext.getText());
                    support.emit(protocolRef);
                }
            }
        }
    }

    @Override
    public void enterClass_declaration_list(ObjCParser.Class_declaration_listContext ctx) {

        ObjCParser.Class_listContext classListContext = ctx.class_list();
        if (classListContext != null) {
            List<ObjCParser.Class_nameContext> classNameContexts = classListContext.class_name();
            if (classNameContexts != null) {
                for (ObjCParser.Class_nameContext classNameContext : classNameContexts) {
                    Ref classRef = support.ref(classNameContext);
                    classRef.defKey = new DefKey(null, classNameContext.getText());
                    support.emit(classRef);
                }
            }
        }
    }

    @Override
    public void enterFunction_definition(ObjCParser.Function_definitionContext ctx) {

        blockCounter = 0;

        Def fnDef = support.def(ctx.identifier(), "METHOD");
        fnDef.defKey = new DefKey(null, fnDef.name);
        support.emit(fnDef);
        support.functions.add(fnDef.name);

        currentMethodName = fnDef.name;

        ObjCParser.Declaration_specifiersContext declarationSpecifiersContext = ctx.declaration_specifiers();
        if (declarationSpecifiersContext != null) {
            declarationSpecifiersContext.
                    type_specifier().forEach(this::processTypeSpecifier);
        }

        ObjCParser.Parameter_listContext parameterListContext = ctx.parameter_list();
        if (parameterListContext == null) {
            return;
        }

        for (ObjCParser.Parameter_declarationContext parameterDeclarationContext : parameterListContext.
                parameter_declaration_list().parameter_declaration()) {
            String typeName = null;
            for (ObjCParser.Type_specifierContext typeSpecifierContext : parameterDeclarationContext.
                    declaration_specifiers().type_specifier()) {
                String type = processTypeSpecifier(typeSpecifierContext);
                if (type != null) {
                    typeName = type;
                }
            }

            ParserRuleContext ident = ident(parameterDeclarationContext.declarator());
            if (ident == null) {
                List<ObjCParser.Type_specifierContext> typeSpecifierContexts = parameterDeclarationContext.
                        declaration_specifiers().type_specifier();
                if (typeSpecifierContexts.isEmpty()) {
                    return;
                }
                ident = typeSpecifierContexts.get(typeSpecifierContexts.size() - 1);
            }
            Def argDef = support.def(ident, "VAR");
            argDef.defKey = new DefKey(null, currentDefKey(argDef.name));
            support.emit(argDef);
            paramsVars.put(argDef.name, typeName);
        }

    }

    @Override
    public void exitFunction_definition(ObjCParser.Function_definitionContext ctx) {
        paramsVars.clear();
        currentMethodName = null;
    }

    @Override
    public void enterProperty_declaration(ObjCParser.Property_declarationContext ctx) {
        // TODO (alexsaveliev): custom getter and setter
        ObjCParser.Struct_declaratorContext structDeclaratorContext =
                ctx.struct_declaration().struct_declarator_list().struct_declarator(0);

        // property def
        Def propertyDef = support.def(structDeclaratorContext.declarator().direct_declarator(), "VAR");
        // adding () to distinguish from private members
        propertyDef.defKey = new DefKey(null, currentClassName + '/' + propertyDef.name + "()");
        support.emit(propertyDef);

        // type refs
        ctx.struct_declaration().
                specifier_qualifier_list().type_specifier().forEach(this::processTypeSpecifier);
    }

    @Override
    public void enterPostfix_expression(ObjCParser.Postfix_expressionContext ctx) {

        ObjCParser.Primary_expressionContext primaryExpressionContext = ctx.primary_expression();
        String id = primaryExpressionContext.getText();
        if (primaryExpressionContext.identifier() == null &&
                !id.equals("self") &&
                !id.equals("super")) {
            // we can't parse complex expressions yet
            return;
        }
        List<ObjCParser.IdentifierContext> identifierContext = ctx.identifier();
        List<ObjCParser.Argument_expression_listContext> argumentExpressionListContext = ctx.argument_expression_list();
        List<ObjCParser.ExpressionContext> expressionContext = ctx.expression();

        if (identifierContext.isEmpty() && argumentExpressionListContext.isEmpty() && expressionContext.isEmpty() &&
                ctx.getStop().getType() != ObjCLexer.RPAREN) {
            // a or a++ or a--
            if (id.equals("self")) {
                Ref varRef = support.ref(primaryExpressionContext);
                varRef.defKey = new DefKey(null, currentClassName + "/self");
                support.emit(varRef);

            }
            return;
        }
        if (!identifierContext.isEmpty()) {
            // a.b or a->b
            String varName = identifierContext.get(0).getText();
            String propertyKey = null;
            if (id.equals("self") || id.equals("super")) {
                // TODO (alexsaveliev): separate super?
                propertyKey = currentClassName + '/' + varName;
            } else {
                Var var = getLocalVariable(id);
                if (var != null) {
                    propertyKey = var.type + '/' + varName;
                } else {
                    String type = paramsVars.get(id);
                    if (type != null) {
                        propertyKey = type + '/' + varName;
                    } else {
                        Map<String, String> currentInstanceVars = support.instanceVars.get(currentClassName);
                        type = currentInstanceVars != null ? currentInstanceVars.get(id) : null;
                        if (type != null) {
                            propertyKey = type + '/' + varName;
                        } else {
                            type = support.globalVars.get(id);
                            if (type != null) {
                                propertyKey = type + '/' + varName;
                            }
                        }
                    }
                }
            }
            if (propertyKey != null) {
                // ref to method
                Ref propertyRef = support.ref(identifierContext.get(0));
                // adding () to distinguish private members from properties
                propertyRef.defKey = new DefKey(null, propertyKey + "()");
                support.emit(propertyRef);
            }
        }
    }

    @Override
    public void enterPrimary_expression(ObjCParser.Primary_expressionContext ctx) {
        if (ctx.identifier() == null) {
            return;
        }
        String id = ctx.identifier().getText();

        // ref to variable?
        String key;
        Var var = getLocalVariable(id);
        if (var != null) {
            key = var.defKey;
        } else if (paramsVars.containsKey(id)) {
            key = currentDefKey(id);
        } else {
            Map<String, String> currentClassVars = support.instanceVars.get(currentClassName);
            if (currentClassVars != null && currentClassVars.containsKey(id)) {
                key = currentClassName + '/' + id;
            } else { // global var or type
                key = id;
            }
        }
        Ref varOrTypeRef = support.ref(ctx.identifier());
        varOrTypeRef.defKey = new DefKey(null, key);
        support.emit(varOrTypeRef);
    }

    @Override
    public void enterEnum_specifier(ObjCParser.Enum_specifierContext ctx) {

        if (ctx.type_name() != null) {
            Ref typeRef = support.ref(ctx.type_name());
            typeRef.defKey = new DefKey(null, ctx.type_name().getText());
            support.emit(typeRef);
        }

        String typeName;
        if (ctx.identifier() != null) {
            Def typeDef = support.def(ctx.identifier(), "ENUM");
            // TODO (alexsaveliev): encapsulate enums
            typeDef.defKey = new DefKey(null, typeDef.name);
            support.emit(typeDef);
            typeName = typeDef.name;
        } else {
            typeName = "int";
        }
        ObjCParser.Enumerator_listContext enumeratorListContext = ctx.enumerator_list();
        if (enumeratorListContext == null) {
            return;
        }
        for (ObjCParser.EnumeratorContext enumeratorContext : enumeratorListContext.enumerator()) {
            Def enumeratorDef = support.def(enumeratorContext.identifier(), "VAR");
            Map<String, String> vars = null;
            String defKey;
            if (currentClassName == null) {
                if (currentMethodName == null) {
                    vars = support.globalVars;
                    defKey = enumeratorDef.name;
                } else {
                    Var var = new Var(enumeratorDef.name, typeName);
                    localVars.peek().put(enumeratorDef.name, var);
                    defKey = var.defKey;
                }
            } else {
                if (currentMethodName == null) {
                    vars = support.instanceVars.get(currentClassName);
                    if (vars == null) {
                        vars = new HashMap<>();
                        support.instanceVars.put(currentClassName, vars);
                    }
                    defKey = currentDefKey(enumeratorDef.name);
                } else {
                    Var var = new Var(enumeratorDef.name, typeName);
                    localVars.peek().put(enumeratorDef.name, var);
                    defKey = var.defKey;
                }
            }
            enumeratorDef.defKey = new DefKey(null, defKey);
            support.emit(enumeratorDef);
            if (vars != null) {
                vars.put(enumeratorDef.name, typeName);
            }
        }
    }

    @Override
    public void enterType_variable_declarator(ObjCParser.Type_variable_declaratorContext ctx) {
        ObjCParser.Direct_declaratorContext directDeclaratorContext = ctx.declarator().direct_declarator();

        Def varDef = support.def(directDeclaratorContext, "VAR");

        for (ObjCParser.Type_specifierContext typeSpecifierContext : ctx.declaration_specifiers().type_specifier()) {
            String type = processTypeSpecifier(typeSpecifierContext);
            if (type != null) {
                if (currentMethodName != null) {
                    // TODO  (alexsaveliev)
                    Var var = new Var(varDef.name, type);
                    localVars.peek().put(varDef.name, var);
                    varDef.defKey = new DefKey(null, var.defKey);
                } else {
                    if (currentClassName != null) {
                        // class
                        varDef.defKey = new DefKey(null, currentClassName + '/' + varDef.name);
                        support.instanceVars.get(currentClassName).put(varDef.name, type);
                    } else {
                        // global
                        varDef.defKey = new DefKey(null, varDef.name);
                        support.globalVars.put(varDef.name, type);
                    }
                }
            }
        }
        if (varDef.defKey != null) {
            support.emit(varDef);
        }
    }

    @Override
    public void enterFor_statement(ObjCParser.For_statementContext ctx) {
        localVars.push(new HashMap<>());

        ObjCParser.Declaration_specifiersContext declarationSpecifiersContext = ctx.declaration_specifiers();
        if (declarationSpecifiersContext == null) {
            return;
        }

        String typeName = null;
        for (ObjCParser.Type_specifierContext typeSpecifierContext : declarationSpecifiersContext.type_specifier()) {
            String type = processTypeSpecifier(typeSpecifierContext);
            if (type != null) {
                typeName = type;
            }
        }

        for (ObjCParser.Init_declaratorContext initDeclaratorContext : ctx.init_declarator_list().init_declarator()) {
            ParserRuleContext ident = ident(initDeclaratorContext.declarator());
            if (ident == null) {
                continue;
            }

            Def varDef = support.def(ident, "VAR");
            Var var = new Var(varDef.name, typeName);
            localVars.peek().put(varDef.name, var);
            varDef.defKey = new DefKey(null, var.defKey);
            support.emit(varDef);
        }
    }

    @Override
    public void exitFor_statement(ObjCParser.For_statementContext ctx) {
        localVars.pop();
    }

    @Override
    public void enterCatch_statement(ObjCParser.Catch_statementContext ctx) {
        localVars.push(new HashMap<>());
    }

    @Override
    public void exitCatch_statement(ObjCParser.Catch_statementContext ctx) {
        localVars.pop();
    }

    @Override
    public void enterCompound_statement(ObjCParser.Compound_statementContext ctx) {
        localVars.push(new HashMap<>());
        blockCounter++;
    }

    @Override
    public void exitCompound_statement(ObjCParser.Compound_statementContext ctx) {
        localVars.pop();
    }

    @Override
    public void enterMethod_definition(ObjCParser.Method_definitionContext ctx) {

    }

    @Override
    public void enterCast_expression(ObjCParser.Cast_expressionContext ctx) {
        ObjCParser.Type_nameContext typeNameContext = ctx.type_name();
        if (typeNameContext == null) {
            return;
        }
        Ref typeRef = support.ref(typeNameContext);
        typeRef.defKey = new DefKey(null, typeNameContext.getText());
        support.emit(typeRef);
    }

    @Override
    public void enterProperty_implementation(ObjCParser.Property_implementationContext ctx) {
        List<ObjCParser.Property_synthesize_itemContext> items = ctx.property_synthesize_list().
                property_synthesize_item();
        if (items == null) {
            return;
        }
        for (ObjCParser.Property_synthesize_itemContext item : items) {
            TerminalNode prop = item.IDENTIFIER(0);
            TerminalNode var = item.IDENTIFIER(1);
            Ref propRef = support.ref(prop.getSymbol());
            propRef.defKey = new DefKey(null, currentDefKey(prop.getText()) + "()");
            support.emit(propRef);
            if (var != null) {
                Ref varRef = support.ref(var.getSymbol());
                varRef.defKey = new DefKey(null, currentDefKey(var.getText()));
                support.emit(varRef);
            }
        }
    }

    protected void processMethodDeclaration(String className,
                                            ObjCParser.Method_declarationContext ctx) {
        Def methodDef;
        ObjCParser.Method_selectorContext methodSelectorContext = ctx.method_selector();
        ObjCParser.SelectorContext selectorContext = methodSelectorContext.selector();
        if (selectorContext != null) {
            methodDef = support.def(selectorContext, "METHOD");
        } else {
            methodDef = support.def(methodSelectorContext.keyword_declarator().get(0).selector(), "METHOD");
        }

        String key = className + '/' + getFuncName(methodSelectorContext);
        support.functions.add(key);
        methodDef.defKey = new DefKey(null, key);
        support.emit(methodDef);

        Ref typeRef = support.ref(ctx.method_type().type_name());
        typeRef.defKey = new DefKey(null, ctx.method_type().type_name().getText());
        support.emit(typeRef);

        if (selectorContext == null) {
            // args
            boolean first = true;
            for (ObjCParser.Keyword_declaratorContext declaratorCtx : methodSelectorContext.keyword_declarator()) {
                ObjCParser.SelectorContext sContext = declaratorCtx.selector();
                if (sContext != null && sContext.IDENTIFIER() != null && !first) {
                    Def argDef = support.def(sContext, "VAR");
                    // using /@ to distinguish parameter name from parameter prefix
                    // in the following cases: "reuseIdentifier:(NSString *)reuseIdentifier"
                    argDef.defKey = new DefKey(null, key + "/@" + sContext.IDENTIFIER().getText());
                    support.emit(argDef);
                }
                List<ObjCParser.Method_typeContext> methodTypeContexts = declaratorCtx.method_type();
                if (methodTypeContexts != null) {
                    for (ObjCParser.Method_typeContext methodTypeContext : methodTypeContexts) {
                        ObjCParser.Type_nameContext typeNameContext = methodTypeContext.type_name();
                        Ref argTypeRef = support.ref(typeNameContext);
                        argTypeRef.defKey = new DefKey(null, typeNameContext.getText());
                        support.emit(argTypeRef);
                    }
                }
                first = false;
            }
        }

    }

    private String getFuncName(ObjCParser.Method_selectorContext methodSelectorContext) {
        StringBuilder ret = new StringBuilder();
        ObjCParser.SelectorContext selectorContext = methodSelectorContext.selector();
        if (selectorContext != null) {
            ret.append(selectorContext.getText()).append(':');
        } else {
            List<ObjCParser.Keyword_declaratorContext> keywordDeclaratorContexts = methodSelectorContext.
                    keyword_declarator();
            if (!keywordDeclaratorContexts.isEmpty()) {
                for (ObjCParser.Keyword_declaratorContext ctx : keywordDeclaratorContexts) {
                    ObjCParser.SelectorContext sc = ctx.selector();
                    if (sc != null) {
                        ret.append(sc.getText());
                    }
                    ret.append(':');
                }
            } else {
                ret.append(methodSelectorContext.getText()).append(':');
            }
        }
        return ret.toString();
    }

    private String getFuncName(ObjCParser.Message_selectorContext messageSelectorContext) {
        StringBuilder ret = new StringBuilder();
        ObjCParser.SelectorContext selectorContext = messageSelectorContext.selector();
        if (selectorContext != null) {
            ret.append(selectorContext.getText()).append(':');
        } else {
            List<ObjCParser.Keyword_argumentContext> keywordArgumentContexts = messageSelectorContext.keyword_argument();
            if (!keywordArgumentContexts.isEmpty()) {
                for (ObjCParser.Keyword_argumentContext ctx : keywordArgumentContexts) {
                    ObjCParser.SelectorContext sc = ctx.selector();
                    if (sc != null) {
                        ret.append(sc.getText());
                    }
                    ret.append(':');
                }
            } else {
                ret.append(messageSelectorContext.getText()).append(':');
            }
        }
        return ret.toString();
    }

    protected void processMethodDefinition(ObjCParser.Method_definitionContext methodDefinitionContext) {
        paramsVars.clear();
        blockCounter = 0;
        // TODO (alexsaveliev): implementation of parent interface
        ObjCParser.SelectorContext selectorContext = methodDefinitionContext.method_selector().selector();
        Ref methodRef;
        String defKey = currentClassName + '/' +
                getFuncName(methodDefinitionContext.method_selector());
        if (selectorContext == null) {
            List<ObjCParser.Keyword_declaratorContext> keywordDeclaratorContexts = methodDefinitionContext.
                    method_selector().keyword_declarator();
            if (!keywordDeclaratorContexts.isEmpty()) {
                methodRef = support.ref(keywordDeclaratorContexts.get(0).selector());
                for (ObjCParser.Keyword_declaratorContext keywordDeclaratorContext : methodDefinitionContext.
                        method_selector().keyword_declarator()) {
                    ObjCParser.Method_typeContext methodTypeContext = keywordDeclaratorContext.method_type(0);
                    String argTypeName;
                    if (methodTypeContext != null) {
                        ObjCParser.Type_nameContext typeNameContext = keywordDeclaratorContext.method_type(0).type_name();
                        Ref typeRef = support.ref(typeNameContext);
                        typeRef.defKey = new DefKey(null, typeNameContext.getText());
                        support.emit(typeRef);
                        argTypeName = typeNameContext.getText();
                    } else {
                        // example
                        // (void)animationWithSpriteFrames:animFrames delay:(float)delay...
                        argTypeName = "id";
                    }
                    paramsVars.put(keywordDeclaratorContext.getStop().getText(), argTypeName);
                    Def argDef = support.def(keywordDeclaratorContext.getStop(), "VAR");
                    argDef.defKey = new DefKey(null, defKey + '/' + keywordDeclaratorContext.getStop().getText());
                    support.emit(argDef);
                }
            } else {
                methodRef = support.ref(methodDefinitionContext.method_selector());
            }
        } else {
            methodRef = support.ref(selectorContext);
        }
        methodRef.defKey = new DefKey(null, defKey);
        support.emit(methodRef);
        ObjCParser.Type_nameContext typeNameContext = methodDefinitionContext.method_type().type_name();
        Ref typeRef = support.ref(typeNameContext);
        typeRef.defKey = new DefKey(null, typeNameContext.getText());
        support.emit(typeRef);
    }

    private ParserRuleContext ident(ObjCParser.DeclaratorContext context) {
        if (context == null) {
            return null;
        }
        ObjCParser.Direct_declaratorContext directDeclaratorContext = context.direct_declarator();
        ObjCParser.IdentifierContext identifierContext = directDeclaratorContext.identifier();
        if (identifierContext != null) {
            return identifierContext;
        }
        ObjCParser.DeclaratorContext declaratorContext = directDeclaratorContext.declarator();
        if (declaratorContext == null) {
            return null;
        }
        return ident(declaratorContext);
    }

    private String processTypeSpecifier(ObjCParser.Type_specifierContext ctx) {
        ObjCParser.Protocol_reference_listContext protocolReferenceListContext = ctx.
                protocol_reference_list();
        if (protocolReferenceListContext != null) {
            for (ObjCParser.Protocol_nameContext protocolNameContext : protocolReferenceListContext.
                    protocol_list().protocol_name()) {
                Ref typeRef = support.ref(protocolNameContext);
                typeRef.defKey = new DefKey(null, protocolNameContext.getText());
                support.emit(typeRef);
            }
        }
        ObjCParser.Class_nameContext classNameContext = ctx.class_name();
        if (classNameContext != null && !isReservedSpecifier(classNameContext.getText())) {
            Ref typeRef = support.ref(classNameContext);
            typeRef.defKey = new DefKey(null, classNameContext.getText());
            support.emit(typeRef);
            return classNameContext.getText();
        }
        ObjCParser.IdentifierContext identifierContext = ctx.identifier();
        if (identifierContext != null && !isReservedSpecifier(identifierContext.getText())) {
            Ref typeRef = support.ref(identifierContext);
            typeRef.defKey = new DefKey(null, identifierContext.getText());
            support.emit(typeRef);
            return identifierContext.getText();
        }

        String maybePredefined = ctx.getText();
        if (ArrayUtils.indexOf(PREDEFINED_TYPES, maybePredefined) >= 0) {
            Ref typeRef = support.ref(ctx);
            typeRef.defKey = new DefKey(null, maybePredefined);
            support.emit(typeRef);
            return maybePredefined;
        }
        return null;
    }

    private boolean isReservedSpecifier(String text) {
        return text.equals("inline") || text.equals("static");
    }

    private void processInstanceVariables(Map<String, String> currentClassVars,
                                          ObjCParser.Instance_variablesContext instanceVariablesContext) {
        if (instanceVariablesContext != null) {
            for (ObjCParser.Struct_declarationContext structDeclarationContext : instanceVariablesContext.struct_declaration()) {

                String typeName = null;
                // type refs
                for (ObjCParser.Type_specifierContext typeSpecifierContext : structDeclarationContext.
                        specifier_qualifier_list().type_specifier()) {
                    String type = processTypeSpecifier(typeSpecifierContext);
                    if (type != null) {
                        typeName = type;
                    }
                }

                // variable defs
                for (ObjCParser.Struct_declaratorContext structDeclaratorContext : structDeclarationContext.
                        struct_declarator_list().struct_declarator()) {
                    Def propertyDef = support.def(structDeclaratorContext.declarator().direct_declarator(), "VAR");
                    propertyDef.defKey = new DefKey(null, currentClassName + '/' + propertyDef.name);
                    support.emit(propertyDef);
                    currentClassVars.put(propertyDef.name, typeName);
                }

            }
        }
    }

    private void processProtocolReferences(ObjCParser.Protocol_reference_listContext protocolReferenceListContext) {
        if (protocolReferenceListContext != null) {
            ObjCParser.Protocol_listContext protocolListContext = protocolReferenceListContext.protocol_list();
            if (protocolListContext != null) {
                List<ObjCParser.Protocol_nameContext> protocolNameContexts = protocolListContext.protocol_name();
                if (protocolNameContexts != null) {
                    for (ObjCParser.Protocol_nameContext protocolNameContext : protocolNameContexts) {
                        Ref protocolRef = support.ref(protocolNameContext);
                        protocolRef.defKey = new DefKey(null, protocolNameContext.getText());
                        support.emit(protocolRef);
                    }
                }
            }
        }
    }

    private void processDeclarationList(ObjCParser.Interface_declaration_listContext interfaceDeclarationListContext) {
        if (interfaceDeclarationListContext != null) {
            List<ObjCParser.Class_method_declarationContext> classMethodDeclarationContexts =
                    interfaceDeclarationListContext.class_method_declaration();
            if (classMethodDeclarationContexts != null) {
                for (ObjCParser.Class_method_declarationContext classMethodDeclarationContext : classMethodDeclarationContexts) {
                    processMethodDeclaration(currentClassName, classMethodDeclarationContext.method_declaration());
                }
            }
            List<ObjCParser.Instance_method_declarationContext> instanceMethodDeclarationContexts =
                    interfaceDeclarationListContext.instance_method_declaration();
            if (instanceMethodDeclarationContexts != null) {
                for (ObjCParser.Instance_method_declarationContext instanceMethodDeclarationContext : instanceMethodDeclarationContexts) {
                    processMethodDeclaration(currentClassName, instanceMethodDeclarationContext.method_declaration());
                }
            }
        }
    }

    private Var getLocalVariable(String variable) {
        for (int i = localVars.size() - 1; i >= 0; i--) {
            Var var = localVars.get(i).get(variable);
            if (var != null) {
                return var;
            }
        }
        return null;
    }

    private String currentDefKey(String ident) {
        StringBuilder ret = new StringBuilder();
        if (currentClassName != null) {
            ret.append(currentClassName).append('/');
        }
        if (currentMethodName != null) {
            ret.append(currentMethodName).append('/');
        }
        ret.append(ident);
        return ret.toString();
    }

    private class Var {
        String type;
        String defKey;

        Var(String name, String type) {
            this.type = type;
            this.defKey = currentDefKey(name);
            if (blockCounter > 0) {
                this.defKey += "$" + blockCounter;
            }
        }
    }
}