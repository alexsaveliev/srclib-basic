package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.php.antlr4.PHPParser;
import com.sourcegraph.toolchain.php.antlr4.PHPParserBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PHPParseTreeListener extends PHPParserBaseListener {

    private static final Pattern INCLUDE_EXPRESSION = Pattern.compile("\\s*\\(?\\s*['\"]([^'\"]+)['\"]\\s*\\)?");
    private static final Pattern CONSTANT_NAME_EXPRESSION = Pattern.compile("['\"]([a-zA-Z_\\x7f-\\xff][a-zA-Z0-9_\\x7f-\\xff]*)['\"]");

    private static final String NAMESPACE_SEPARATOR = ":";
    private static final String GLOBAL_NAMESPACE = StringUtils.EMPTY;

    private static final String MAYBE_METHOD = "(?M)";
    private static final String MAYBE_PROPERTY = "(?P)";
    private static final String MAYBE_CONSTANT = "(?C)";

    private LanguageImpl support;

    private ClassInfo currentClassInfo;

    private Stack<Integer> blockCounter = new Stack<>();
    private Stack<String> blockStack = new Stack<>();

    private Map<String, String> functionArguments = new HashMap<>();

    private Stack<String> namespace = new Stack<>();

    private Map<String, String> namespaceAliases = new HashMap<>();

    public PHPParseTreeListener(LanguageImpl support) {
        this.support = support;

        support.vars.push(new HashMap<>());

        blockCounter.push(0);
        namespace.push(GLOBAL_NAMESPACE);
    }

    protected Def def(ParserRuleContext ctx, String kind) {
        Def def = support.def(ctx, kind);
        initDef(def);
        return def;
    }

    protected Def def(Token token, String kind) {
        Def def = support.def(token, kind);
        initDef(def);
        return def;
    }

    protected void initDef(Def def) {
        def.test = false; // not in PHP
        def.exported =
                DefKind.FUNCTION.equals(def.kind) ||
                        DefKind.CLASS.equals(def.kind) ||
                        DefKind.TRAIT.equals(def.kind) ||
                        DefKind.INTERFACE.equals(def.kind) ||
                        DefKind.CONSTANT.equals(def.kind) ||
                        DefKind.METHOD.equals(def.kind);
        def.local = !def.exported;
    }

    @Override
    public void enterBlockStatement(PHPParser.BlockStatementContext ctx) {
        blockCounter.push(blockCounter.pop() + 1);
        blockCounter.push(0);
    }

    @Override
    public void exitBlockStatement(PHPParser.BlockStatementContext ctx) {
        blockCounter.pop();
    }

    /**
     * Processing include../require.. statements, trying to process include files
     */
    @Override
    public void enterPreprocessorExpression(PHPParser.PreprocessorExpressionContext ctx) {
        String file = extractIncludeName(ctx.expression().getText());
        if (file != null) {
            this.support.process(new File(file));
        }
    }

    @Override
    public void enterFunctionDeclaration(PHPParser.FunctionDeclarationContext ctx) {

        Def fnDef = def(ctx.identifier(), DefKind.FUNCTION);
        String fqn = fqn(fnDef.name);
        fnDef.defKey = new DefKey(null, fqn);
        support.emit(fnDef);

        support.functions.add(fqn);
        support.vars.push(new HashMap<>());
        blockStack.push(fnDef.name);

        // TODO (alexsaveliev): what is ctx.typeParameterListInBrackets()?
        processFunctionParameters(ctx.formalParameterList().formalParameter());
    }

    @Override
    public void exitFunctionDeclaration(PHPParser.FunctionDeclarationContext ctx) {
        support.vars.pop();
        blockStack.pop();

        functionArguments.clear();
    }

    @Override
    public void enterFunctionCall(PHPParser.FunctionCallContext ctx) {
        PHPParser.FunctionCallNameContext fnCallNameCtx = ctx.functionCallName();
        PHPParser.QualifiedNamespaceNameContext qNameCtx = fnCallNameCtx.qualifiedNamespaceName();
        if (qNameCtx != null) {
            Ref fnRef = support.ref(qNameCtx);
            fnRef.defKey = new DefKey(null, resolveFunctionFqn(qNameCtx.getText()));
            support.emit(fnRef);
        }
        if ("define".equals(fnCallNameCtx.getText())) {
            // constant definition
            PHPParser.ActualArgumentContext constant = ctx.actualArguments().arguments().actualArgument(0);
            if (constant != null) {
                String constantName = extractConstantName(constant.getText());
                if (constantName != null) {
                    Def constantDef = def(constant, DefKind.CONSTANT);
                    constantDef.name = constantName;
                    constantDef.defKey = new DefKey(null, fqn(constantName));
                    support.emit(constantDef);
                }
            }
            return;
        }

        if (fnCallNameCtx.classConstant() != null) {
            processClassConstantRef(fnCallNameCtx.classConstant(), true);
        }
        // TODO (alexsaveliev): $foo->bar()
    }

    @Override
    public void enterGlobalStatement(PHPParser.GlobalStatementContext ctx) {
        if (this.support.vars.size() < 2) {
            return;
        }
        List<PHPParser.GlobalVarContext> vars = ctx.globalVar();
        if (vars == null) {
            return;
        }
        for (PHPParser.GlobalVarContext var : vars) {
            TerminalNode varNameNode = var.VarName();
            if (varNameNode != null) {
                String varName = varNameNode.getText();
                if (!this.support.vars.firstElement().containsKey(varName)) {
                    continue;
                }
                Ref globalVarRef = support.ref(varNameNode.getSymbol());
                globalVarRef.defKey = new DefKey(null, varName);
                support.emit(globalVarRef);
                this.support.vars.peek().put(varName, false);
            }
        }
    }

    @Override
    public void enterClassDeclaration(PHPParser.ClassDeclarationContext ctx) {
        TerminalNode interfaceNode = ctx.Interface();
        String className = ctx.identifier().getText();
        blockStack.push(className);
        currentClassInfo = new ClassInfo();
        currentClassInfo.className = fqn(className);
        this.support.classes.put(currentClassInfo.className, currentClassInfo);

        if (interfaceNode != null) {
            Def interfaceDef = def(ctx.identifier(), DefKind.INTERFACE);
            interfaceDef.defKey = new DefKey(null, fqn(interfaceDef.name));
            support.emit(interfaceDef);

            PHPParser.InterfaceListContext interfaces = ctx.interfaceList();
            if (interfaces == null) {
                return;
            }
            List<PHPParser.QualifiedStaticTypeRefContext> iNames = interfaces.qualifiedStaticTypeRef();
            if (iNames == null) {
                return;
            }
            for (PHPParser.QualifiedStaticTypeRefContext i : iNames) {
                ParserRuleContext qName = qName(i);
                if (qName == null) {
                    continue;
                }
                String extendsInterfaceName = qName.getText();
                Ref extendsInterfaceRef = support.ref(qName);
                String fqn = resolveFqn(extendsInterfaceName);
                resolveClass(fqn);
                extendsInterfaceRef.defKey = new DefKey(null, fqn);
                support.emit(extendsInterfaceRef);
                currentClassInfo.extendsClasses.add(fqn);
            }

        } else {
            PHPParser.ClassEntryTypeContext classEntryTypeContext = ctx.classEntryType();
            Def classOrTraitDef = def(ctx.identifier(),
                    classEntryTypeContext.Trait() != null ? DefKind.TRAIT : DefKind.CLASS);
            classOrTraitDef.defKey = new DefKey(null, fqn(classOrTraitDef.name));
            support.emit(classOrTraitDef);

            PHPParser.QualifiedStaticTypeRefContext extendsCtx = ctx.qualifiedStaticTypeRef();
            ParserRuleContext qName = qName(extendsCtx);
            if (qName != null) {
                String extendsName = qName.getText();
                Ref extendsRef = support.ref(qName);
                String fqn = resolveFqn(extendsName);
                resolveClass(fqn);
                extendsRef.defKey = new DefKey(null, fqn);
                support.emit(extendsRef);
                currentClassInfo.extendsClasses.add(fqn);
            }

            PHPParser.InterfaceListContext interfaces = ctx.interfaceList();
            if (interfaces == null) {
                return;
            }

            List<PHPParser.QualifiedStaticTypeRefContext> iNames = interfaces.qualifiedStaticTypeRef();
            if (iNames == null) {
                return;
            }
            for (PHPParser.QualifiedStaticTypeRefContext i : iNames) {
                qName = qName(i);
                if (qName == null) {
                    continue;
                }
                String implementsInterfaceName = qName.getText();
                Ref implementsInterfaceRef = support.ref(qName);
                String fqn = resolveFqn(implementsInterfaceName);
                resolveClass(fqn);
                implementsInterfaceRef.defKey = new DefKey(null, fqn);
                support.emit(implementsInterfaceRef);
                currentClassInfo.implementsInterfaces.add(fqn);
            }
        }
    }

    @Override
    public void exitClassDeclaration(PHPParser.ClassDeclarationContext ctx) {
        blockStack.pop();
    }

    @Override
    public void enterGlobalConstantDeclaration(PHPParser.GlobalConstantDeclarationContext ctx) {
        List<PHPParser.IdentifierInititalizerContext> constants = ctx.identifierInititalizer();
        if (constants == null) {
            return;
        }
        for (PHPParser.IdentifierInititalizerContext constant : constants) {
            PHPParser.IdentifierContext ident = constant.identifier();
            Def constantDef = def(ident, DefKind.CONSTANT);
            constantDef.defKey = new DefKey(null, fqn(constantDef.name));
            support.emit(constantDef);
        }
    }

    @Override
    public void enterClassStatement(PHPParser.ClassStatementContext ctx) {

        support.vars.push(new HashMap<>());
        blockStack.push(StringUtils.EMPTY); // dummy, may be redefined by processClassMethod()


        if (ctx.Const() != null) {
            List<PHPParser.IdentifierInititalizerContext> constants = ctx.identifierInititalizer();
            if (constants != null) {
                processClassConstant(constants);
            }
            return;
        }
        List<PHPParser.VariableInitializerContext> properties = ctx.variableInitializer();
        if (properties != null) {
            processClassProperties(properties);
        }

        if (ctx.Function() != null) {
            processClassMethod(ctx);
        }

        if (ctx.Use() != null) {
            processUseTraits(ctx);
        }

    }

    @Override
    public void exitClassStatement(PHPParser.ClassStatementContext ctx) {
        functionArguments.clear();
        blockStack.pop();
    }

    @Override
    public void enterNonEmptyStatement(PHPParser.NonEmptyStatementContext ctx) {
        PHPParser.IdentifierContext label = ctx.identifier();
        if (label != null) {
            Def labelDef = def(label, DefKind.LABEL);
            labelDef.defKey = new DefKey(null, labelDef.name + getBlockNameSuffix() + getFileSuffix());
            support.emit(labelDef);
        }
    }

    @Override
    public void enterGotoStatement(PHPParser.GotoStatementContext ctx) {
        PHPParser.IdentifierContext label = ctx.identifier();
        if (label != null) {
            Ref labelRef = support.ref(label);
            labelRef.defKey = new DefKey(null, label.getText() + getBlockNameSuffix() + getFileSuffix());
            support.emit(labelRef);
        }
    }

    @Override
    public void enterConstant(PHPParser.ConstantContext ctx) {

        PHPParser.ClassConstantContext classConstantContext = ctx.classConstant();
        if (classConstantContext == null) {
            return;
        }
        processClassConstantRef(classConstantContext, false);
    }

    @Override
    public void enterNewexpr(PHPParser.NewexprContext ctx) {
        ParserRuleContext typeCtx = ctx.typeRef();
        String className = resolveFqn(typeCtx.getText());
        resolveClass(className);
        String method = "__construct";
        String defining = this.support.getDefiningClass(className, method);
        if (defining == null) {
            // backward compatibility
            method = className;
            defining = this.support.getDefiningClass(className, method);
        }
        if (defining != null) {
            Ref constructorRef = support.ref(typeCtx);
            constructorRef.defKey = new DefKey(null, defining + '/' + method + "()");
            support.emit(constructorRef);
        }
    }

    @Override
    public void enterNamespaceDeclaration(PHPParser.NamespaceDeclarationContext ctx) {
        processNamespaceChange(ctx.namespaceNameList());
    }

    @Override
    public void exitNamespaceDeclaration(PHPParser.NamespaceDeclarationContext ctx) {
        namespace.pop();
    }

    @Override
    public void enterQualifiedNamespaceName(PHPParser.QualifiedNamespaceNameContext ctx) {

        if (ctx.Namespace() == null) {
            return;
        }

        namespace.pop();

        processNamespaceChange(ctx.namespaceNameList());
    }

    @Override
    public void enterUseDeclaration(PHPParser.UseDeclarationContext ctx) {
        // TODO (alexsaveliev) use const / use function
        if (ctx.Const() != null) {
            return;
        }
        if (ctx.Function() != null) {
            return;
        }
        List<PHPParser.UseDeclarationContentContext> declarations = ctx.useDeclarationContentList().
                useDeclarationContent();
        if (declarations == null) {
            return;
        }
        for (PHPParser.UseDeclarationContentContext declaration : declarations) {
            String ns = makeNamespaceName(declaration.namespaceNameList());
            String alias = StringUtils.substringAfterLast(ns, NAMESPACE_SEPARATOR);
            namespaceAliases.put(alias, ns);
        }
    }

    @Override
    public void enterChainBase(PHPParser.ChainBaseContext ctx) {
        ParserRuleContext classNameCtx = ctx.qualifiedStaticTypeRef();
        if (classNameCtx != null) {
            // Foo:$bar
            String typeName = resolveFqn(classNameCtx.getText());
            resolveClass(typeName);
            if (support.classes.containsKey(typeName)) {
                Ref classRef = support.ref(classNameCtx);
                classRef.defKey = new DefKey(null, typeName);
                support.emit(classRef);
                ParserRuleContext varCtx = ctx.keyedVariable(0);
                Ref staticClassPropertyRef = support.ref(varCtx);
                staticClassPropertyRef.defKey = new DefKey(null, typeName + '/' + varCtx.getText());
                support.emit(staticClassPropertyRef);
            }
            return;
        }
        List<PHPParser.KeyedVariableContext> vars = ctx.keyedVariable();
        if (vars.size() == 1) {
            // $foo
            processVariable(vars.get(0));
        }
        // $foo::$bar
        // TODO (alexsaveliev) can we support $foo::$bar?
    }

    @Override
    public void enterMemberAccess(PHPParser.MemberAccessContext ctx) {
        PHPParser.ChainContext parent = (PHPParser.ChainContext) ctx.getParent();
        List<PHPParser.KeyedVariableContext> var = parent.chainBase().keyedVariable();
        if (var == null || var.isEmpty()) {
            return;
        }
        TerminalNode varNameNode = var.get(0).VarName();
        if (varNameNode == null) {
            return;
        }
        String varName = varNameNode.getText();
        String varType = functionArguments.get(varName);
        if (varType == null) {
            // TODO (alexsaveliev) type hint check in local vars
        }
        PHPParser.KeyedFieldNameContext keyedFieldNameContext = ctx.keyedFieldName();
        String propertyName = keyedFieldNameContext.getText();
        boolean isMethodCall = ctx.actualArguments() != null;
        Ref ref = support.ref(keyedFieldNameContext);
        if (varType == null) {
            ref.candidate = true;
            String prefix;
            if (isMethodCall) {
                prefix = MAYBE_METHOD;
            } else {
                prefix = keyedFieldNameContext.keyedSimpleFieldName() != null ? MAYBE_CONSTANT : MAYBE_PROPERTY;
            }
            ref.defKey = new DefKey(null, prefix + propertyName);
        } else {
            String path = varType + '/' + propertyName;
            if (isMethodCall) {
                path += "()";
            }
            ref.defKey = new DefKey(null, path);
        }
        support.emit(ref);
    }

    private void processVariable(PHPParser.KeyedVariableContext ctx) {
        TerminalNode varNameNode = ctx.VarName();
        if (varNameNode == null) {
            return;
        }
        String varName = varNameNode.getText();
        Boolean local;
        Map<String, Boolean> localVars = support.vars.peek();
        String path = null;
        if ("$this".equals(varName)) {
            local = true;
            path = currentClassInfo.className + "/$this";
        } else {
            if (functionArguments.containsKey(varName)) {
                local = true;
            } else {
                local = localVars.get(varName);
            }
        }
        if (local == null) {
            // new variable
            Def varDef = def(varNameNode.getSymbol(), DefKind.VARIABLE);
            if (!blockStack.empty()) {
                varDef.local = true;
                varDef.exported = false;
                local = true;
            } else {
                varDef.local = false;
                varDef.exported = true;
                local = false;
            }
            varDef.defKey = new DefKey(null, getBlockNamePrefix() + varName);
            support.emit(varDef);
            localVars.put(varName, local);
        } else {
            Ref varRef = support.ref(varNameNode.getSymbol());
            if (path == null) {
                path = local ? getBlockNamePrefix() + varName : varName;
            }
            varRef.defKey = new DefKey(null, path);
            support.emit(varRef);
        }
    }

    private void processNamespaceChange(PHPParser.NamespaceNameListContext ctx) {
        namespace.push(makeNamespaceName(ctx));
    }

    private String makeNamespaceName(PHPParser.NamespaceNameListContext ctx) {
        if (ctx == null) {
            // global namespace
            return GLOBAL_NAMESPACE;
        }
        List<PHPParser.IdentifierContext> identifiers = ctx.identifier();

        if (identifiers == null || identifiers.isEmpty()) {
            // global namespace
            return GLOBAL_NAMESPACE;
        } else {
            StringBuilder ns = new StringBuilder();
            for (PHPParser.IdentifierContext identifier : identifiers) {
                if (ns.length() > 0) {
                    ns.append(NAMESPACE_SEPARATOR);
                }
                ns.append(identifier.getText());
            }
            return ns.toString();
        }
    }


    private void processClassProperties(List<PHPParser.VariableInitializerContext> variables) {
        String blockName = blockStack.pop();

        for (PHPParser.VariableInitializerContext variable : variables) {
            Def propertyDef = def(variable.VarName().getSymbol(), DefKind.VARIABLE);
            propertyDef.local = false;
            propertyDef.exported = true;
            propertyDef.defKey = new DefKey(null, fqn(getBlockNamePrefix() + propertyDef.name));
            support.emit(propertyDef);
            support.resolutions.put(MAYBE_PROPERTY + propertyDef.name, propertyDef);
        }
        blockStack.push(blockName);
    }

    private void processClassConstant(List<PHPParser.IdentifierInititalizerContext> constants) {
        String blockName = blockStack.pop();
        for (PHPParser.IdentifierInititalizerContext constant : constants) {
            Def classConstantDef = def(constant.identifier(), DefKind.CONSTANT);
            classConstantDef.defKey = new DefKey(null, fqn(getBlockNamePrefix() + classConstantDef.name));
            support.emit(classConstantDef);
            support.resolutions.put(MAYBE_CONSTANT + classConstantDef.name, classConstantDef);
            currentClassInfo.definesConstants.add(fqn(classConstantDef.name));
        }
        blockStack.push(blockName);
    }

    private void processClassMethod(PHPParser.ClassStatementContext ctx) {

        // does current class defines method? implements it? both?

        blockStack.pop();
        String className = blockStack.peek();
        ParserRuleContext methodCtx = ctx.identifier();
        String methodName = methodCtx.getText();
        blockStack.push(methodName);

        String definingClass = this.support.getDefiningClass(fqn(className), methodName);
        if (definingClass == null) {
            currentClassInfo.definesMethods.add(methodName);
            Def classMethodDef = def(methodCtx, DefKind.METHOD);
            // adding () to distinguish properties from methods
            classMethodDef.defKey = new DefKey(null, fqn(className + '/' + methodName + "()"));
            support.emit(classMethodDef);
            support.resolutions.put(MAYBE_METHOD + methodName, classMethodDef);
        } else {
            Ref classMethodRef = support.ref(methodCtx);
            // adding () to distinguish properties from methods
            classMethodRef.defKey = new DefKey(null, resolveFqn(definingClass) + '/' + methodName + "()");
            support.emit(classMethodRef);
        }

        // TODO (alexsaveliev): what is ctx.typeParameterListInBrackets()?
        List<PHPParser.FormalParameterContext> fnParams = ctx.formalParameterList().formalParameter();
        processFunctionParameters(fnParams);
    }

    private void processUseTraits(PHPParser.ClassStatementContext ctx) {

        // TODO (alexsaveliev) trait conflicts resolution (insteadof / as)

        List<PHPParser.QualifiedNamespaceNameContext> traits = ctx.qualifiedNamespaceNameList().
                qualifiedNamespaceName();
        for (PHPParser.QualifiedNamespaceNameContext trait : traits) {
            String traitName = trait.getText();
            String fqn = resolveFqn(traitName);
            resolveClass(fqn);
            currentClassInfo.usesTraits.add(fqn);
            Ref traitRef = support.ref(trait);
            traitRef.defKey = new DefKey(null, fqn);
            support.emit(traitRef);
        }
    }

    private void processFunctionParameters(List<PHPParser.FormalParameterContext> parameters) {
        if (parameters == null) {
            return;
        }
        for (PHPParser.FormalParameterContext fnParam : parameters) {
            String typeName = null;
            PHPParser.TypeHintContext typeHint = fnParam.typeHint();
            if (typeHint != null) {
                ParserRuleContext qName = qName(typeHint.qualifiedStaticTypeRef());
                if (qName != null) {
                    Ref typeRef = support.ref(qName);
                    typeName = resolveFqn(qName.getText());
                    resolveClass(typeName);
                    typeRef.defKey = new DefKey(null, typeName);
                    support.emit(typeRef);
                }
            }
            Def fnArgDef = def(fnParam.variableInitializer().VarName().getSymbol(), DefKind.ARGUMENT);
            fnArgDef.defKey = new DefKey(null, fqn(getBlockNamePrefix() + fnArgDef.name));
            support.emit(fnArgDef);
            functionArguments.put(fnArgDef.name, typeName);
        }
    }

    private void processClassConstantRef(PHPParser.ClassConstantContext ctx, boolean isCall) {
        if (ctx.identifier() == null) {
            return;
        }

        String parts[] = ctx.getText().split("::");
        String rootClassName = null;
        if ("self".equals(parts[0])) {
            rootClassName = currentClassInfo.className;
            // TODO (alexsaveliev) ref to self?
        }
        if ("parent".equals(parts[0])) {
            Iterator<String> i = currentClassInfo.extendsClasses.iterator();
            if (i.hasNext()) {
                rootClassName = i.next();
                Ref parentClassRef = support.ref(ctx.Parent_().getSymbol());
                parentClassRef.defKey = new DefKey(null, resolveFqn(rootClassName));
                support.emit(parentClassRef);
            }
        } else {
            String fqn = resolveFqn(parts[0]);
            resolveClass(fqn);
            if (this.support.classes.containsKey(fqn)) {
                // TypeName::CONST, we do not support $...::CONST yet
                rootClassName = fqn;
                Ref classRef = support.ref(ctx.qualifiedStaticTypeRef());
                classRef.defKey = new DefKey(null, fqn);
                support.emit(classRef);
            }
        }

        if (rootClassName == null) {
            return;
        }

        if (isCall) {
            String methodClass = this.support.getDefiningClass(rootClassName, parts[1]);
            if (methodClass != null) {
                Ref classMethodRef = support.ref(ctx.identifier());
                classMethodRef.defKey = new DefKey(null, methodClass + '/' + parts[1] + "()");
                support.emit(classMethodRef);
            }
        } else {
            String constantClass = this.support.getConstantClass(rootClassName, parts[1]);
            if (constantClass != null) {
                Ref classConstRef = support.ref(ctx.identifier());
                classConstRef.defKey = new DefKey(null, constantClass + '/' + parts[1]);
                support.emit(classConstRef);
            }
        }
    }

    private String getBlockSuffix() {
        StringBuilder ret = new StringBuilder();
        for (int i = 1; i < blockCounter.size(); i++) {
            ret.append(':').append(blockCounter.get(i));
        }
        return ret.toString();
    }

    private String getBlockNameSuffix() {
        StringBuilder ret = new StringBuilder();
        for (String blockName : blockStack) {
            ret.append(':').append(blockName);
        }
        return ret.toString();
    }

    private String getBlockNamePrefix() {
        StringBuilder ret = new StringBuilder();
        for (String blockName : blockStack) {
            ret.append(blockName).append('/');
        }
        return ret.toString();
    }

    private String getFileSuffix() {
        return ":" + support.getCurrentFile().replace('/', '|');
    }

    private static String extractIncludeName(String expressionText) {
        Matcher m = INCLUDE_EXPRESSION.matcher(expressionText);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private static String extractConstantName(String expressionText) {
        Matcher m = CONSTANT_NAME_EXPRESSION.matcher(expressionText);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private static ParserRuleContext qName(PHPParser.QualifiedStaticTypeRefContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.qualifiedNamespaceName();
    }

    private String fqn(String name) {
        return namespace.peek() + NAMESPACE_SEPARATOR + name;
    }

    private String resolveFqn(String name) {
        name = name.replace("\\", NAMESPACE_SEPARATOR);
        if (name.startsWith(NAMESPACE_SEPARATOR)) {
            return name.substring(NAMESPACE_SEPARATOR.length());
        }
        String prefix = StringUtils.substringBefore(name, NAMESPACE_SEPARATOR);
        String namespace = namespaceAliases.get(prefix);
        if (namespace != null) {
            return namespace + name.substring(prefix.length());
        }
        return fqn(name);
    }

    private String resolveFunctionFqn(String name) {
        String localName = fqn(name);
        if (support.functions.contains(localName)) {
            return localName;
        }
        // global
        return NAMESPACE_SEPARATOR + name;
    }

    private void resolveClass(String fullyQualifiedClassName) {
        // TODO (alexsaveliev) implement
    }
}