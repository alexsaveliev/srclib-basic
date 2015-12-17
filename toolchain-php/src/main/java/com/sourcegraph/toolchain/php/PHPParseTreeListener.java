package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
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

    /**
     * Pattern being used to resolve include file names
     */
    private static final Pattern INCLUDE_EXPRESSION = Pattern.compile("\\s*\\(?\\s*['\"]([^'\"]+)['\"]\\s*\\)?");

    /**
     * Pattern being used to resolve constant names
     */
    private static final Pattern CONSTANT_NAME_EXPRESSION = Pattern.compile("['\"]([a-zA-Z_\\x7f-\\xff][a-zA-Z0-9_\\x7f-\\xff]*)['\"]");

    /**
     * Separates FQN: NS1 SEP NS2 SEP ... NSn in path
     */
    private static final String NAMESPACE_SEPARATOR = ":";

    /**
     * Separates class name from property/method/constant in path
     */
    private static final char CLASS_NAME_SEPARATOR = '/';

    /**
     * Global namespace identifer
     */
    private static final String GLOBAL_NAMESPACE = StringUtils.EMPTY;

    /**
     * Indicates that we think there might be a ref to method with a given name
     */
    private static final String MAYBE_METHOD = "(?M)";

    /**
     * Indicates that we think there might be a ref to property with a given name
     */
    private static final String MAYBE_PROPERTY = "(?P)";

    /**
     * Indicates that we think there might be a ref to constant with a given name
     */
    private static final String MAYBE_CONSTANT = "(?C)";

    private static final String THIS_KEYWORD = "$this";

    /**
     * Caller
     */
    private LanguageImpl support;

    /**
     * Current class being processed
     */
    private ClassInfo currentClassInfo;

    /**
     * Counts blocks (identified by opening/closing brackets). Using it to generate unique suffixes in path,
     * for example to distinguish two local variables
     */
    private Stack<Integer> blockCounter = new Stack<>();

    /**
     * Keeps stack of blocks which may be: namespace, class, function
     */
    private Stack<String> blockStack = new Stack<>();

    /**
     * Current function arguments (name => type)
     */
    private Map<String, String> functionArguments = new HashMap<>();

    /**
     * Keeps stack of namespaces, updated upon "namespace N {}" or "namespace N;"
     */
    private Stack<String> namespace = new Stack<>();

    /**
     * Maps aliases to namespaces. Updated by "use X [as Y]" statements
     */
    private Map<String, String> namespaceAliases = new HashMap<>();

    public PHPParseTreeListener(LanguageImpl support) {
        this.support = support;
        // initializing variables with an empty map
        support.vars.push(new HashMap<>());
        // initializing block counter
        blockCounter.push(0);
        // current namespace is the global one
        namespace.push(GLOBAL_NAMESPACE);
    }

    /**
     * Initializes and emits definition
     * @param ctx
     * @param kind
     * @return
     */
    protected Def def(ParserRuleContext ctx, String kind) {
        Def def = support.def(ctx, kind);
        initDef(def);
        return def;
    }

    /**
     * Initializes and emits definition
     * @param token
     * @param kind
     * @return
     */
    protected Def def(Token token, String kind) {
        Def def = support.def(token, kind);
        initDef(def);
        return def;
    }

    /**
     * Initializes definition by adding local and test attributes
     * @param def
     */
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

    /**
     * Handles { ... } statements. Updates block counters
     */
    @Override
    public void enterBlockStatement(PHPParser.BlockStatementContext ctx) {
        blockCounter.push(blockCounter.pop() + 1);
        blockCounter.push(0);
    }

    /**
     * Handles { ... } statements. Updates block counters
     */
    @Override
    public void exitBlockStatement(PHPParser.BlockStatementContext ctx) {
        blockCounter.pop();
    }

    /**
     * Handles include../require.. statements, trying to process include files
     */
    @Override
    public void enterPreprocessorExpression(PHPParser.PreprocessorExpressionContext ctx) {
        String file = extractIncludeName(ctx.expression().getText());
        if (file != null) {
            this.support.process(new File(file));
        }
    }

    /**
     * Handles function foo() { ... } statements.
     */
    @Override
    public void enterFunctionDeclaration(PHPParser.FunctionDeclarationContext ctx) {

        // Emitting function definition
        Def fnDef = def(ctx.identifier(), DefKind.FUNCTION);
        String fqn = fqn(fnDef.name);
        fnDef.defKey = new DefKey(null, fqn);
        fnDef.format("function", "(" + ctx.formalParameterList().getText() + ")", DefData.SEPARATOR_EMPTY);
        fnDef.defData.setName(globalLevelLabel(fnDef.name));
        fnDef.defData.setKind("function");
        support.emit(fnDef);

        support.functions.add(fqn);
        // Updating local variables, function resets them
        support.vars.push(new HashMap<>());
        // Updating block stack
        blockStack.push(fnDef.name);
        // Processing function arguments
        processFunctionParameters(ctx.formalParameterList().formalParameter());
    }

    /**
     * Handles function foo() { ... } statements.
     */
    @Override
    public void exitFunctionDeclaration(PHPParser.FunctionDeclarationContext ctx) {
        // clearing arguments and local vars, updating block stack
        support.vars.pop();
        blockStack.pop();

        functionArguments.clear();
    }

    /**
     * Handles foo(...) statements.
     */
    @Override
    public void enterFunctionCall(PHPParser.FunctionCallContext ctx) {

        // Ref to function
        PHPParser.FunctionCallNameContext fnCallNameCtx = ctx.functionCallName();
        PHPParser.QualifiedNamespaceNameContext qNameCtx = fnCallNameCtx.qualifiedNamespaceName();
        if (qNameCtx != null) {
            Ref fnRef = support.ref(qNameCtx);
            fnRef.defKey = new DefKey(null, resolveFunctionFqn(qNameCtx.getText()));
            support.emit(fnRef);
        }
        // Special processing of define("A", "B") - emits A constant definition if possible
        if ("define".equals(fnCallNameCtx.getText())) {
            // constant definition
            PHPParser.ActualArgumentContext constant = ctx.actualArguments().arguments().actualArgument(0);
            if (constant != null) {
                String constantName = extractConstantName(constant.getText());
                if (constantName != null) {
                    Def constantDef = def(constant, DefKind.CONSTANT);
                    constantDef.name = constantName;
                    constantDef.defKey = new DefKey(null, fqn(constantName));
                    constantDef.format("define", "const", DefData.SEPARATOR_SPACE);
                    constantDef.defData.setKind("constant");
                    support.emit(constantDef);
                }
            }
            return;
        }

        if (fnCallNameCtx.classConstant() != null) {
            processClassConstantRef(fnCallNameCtx.classConstant(), true);
        }
    }

    /**
     * Handles global foo; statements.
     */
    @Override
    public void enterGlobalStatement(PHPParser.GlobalStatementContext ctx) {
        if (this.support.vars.size() < 2) {
            // We do not expect global $foo in the global scope
            return;
        }
        List<PHPParser.GlobalVarContext> vars = ctx.globalVar();
        if (vars == null) {
            // Just in case
            return;
        }
        for (PHPParser.GlobalVarContext var : vars) {
            TerminalNode varNameNode = var.VarName();
            if (varNameNode != null) {
                String varName = varNameNode.getText();
                if (!this.support.vars.firstElement().containsKey(varName)) {
                    // make sure there is global variable defined
                    continue;
                }
                Ref globalVarRef = support.ref(varNameNode.getSymbol());
                globalVarRef.defKey = new DefKey(null, GLOBAL_NAMESPACE + varName);
                support.emit(globalVarRef);
                // Pushing variable info into current map, trying to preserve type if known
                VarInfo globalInfo = this.support.vars.get(0).get(varName);
                VarInfo localInfo;
                if (globalInfo == null) {
                    localInfo = new VarInfo(null, false);
                } else {
                    localInfo = new VarInfo(globalInfo.type, false);
                }
                this.support.vars.peek().put(varName, localInfo);
            }
        }
    }

    /**
     * Handles class Foo, interface Foo, and trait Foo statements.
     */
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
            interfaceDef.format("interface", "interface", DefData.SEPARATOR_SPACE);
            interfaceDef.defData.setKind("interface");
            interfaceDef.defData.setName(globalLevelLabel(interfaceDef.name));
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
            String kind;
            String keyword;

            if (classEntryTypeContext.Trait() != null) {
                kind = DefKind.TRAIT;
                keyword = "trait";
            } else {
                kind = DefKind.CLASS;
                keyword = "class";
            }

            Def classOrTraitDef = def(ctx.identifier(), kind);
            classOrTraitDef.defKey = new DefKey(null, fqn(classOrTraitDef.name));
            classOrTraitDef.format(keyword, keyword, DefData.SEPARATOR_SPACE);
            classOrTraitDef.defData.setName(globalLevelLabel(classOrTraitDef.name));
            classOrTraitDef.defData.setKind(keyword);
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

    /**
     * Handles const foo = bar; statements.
     */
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
            constantDef.format("const", "const", DefData.SEPARATOR_SPACE);
            constantDef.defData.setName(globalLevelLabel(constantDef.name));
            constantDef.defData.setKind("constant");
            support.emit(constantDef);
        }
    }

    /**
     * Handles function FUNCTION(), const CONSTANT, use TRAIT, and [var] $PROPERTY statements inside class
     */
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

    /**
     * Handles labels
     */
    @Override
    public void enterNonEmptyStatement(PHPParser.NonEmptyStatementContext ctx) {
        PHPParser.IdentifierContext label = ctx.identifier();
        if (label != null) {
            Def labelDef = def(label, DefKind.LABEL);
            labelDef.defKey = new DefKey(null, labelDef.name + getBlockNameSuffix() + getFileSuffix());
            labelDef.format("label", "label", DefData.SEPARATOR_SPACE);
            labelDef.defData.setKind("label");
            support.emit(labelDef);
        }
    }

    /**
     * Probably the most important statement in the world
     */
    @Override
    public void enterGotoStatement(PHPParser.GotoStatementContext ctx) {
        PHPParser.IdentifierContext label = ctx.identifier();
        if (label != null) {
            Ref labelRef = support.ref(label);
            labelRef.defKey = new DefKey(null, label.getText() + getBlockNameSuffix() + getFileSuffix());
            support.emit(labelRef);
        }
    }

    /**
     * Handles references to CONSTANT
     */
    @Override
    public void enterConstant(PHPParser.ConstantContext ctx) {

        PHPParser.QualifiedNamespaceNameContext qNameContext = ctx.qualifiedNamespaceName();
        if (qNameContext != null) {
            Ref constRef = support.ref(qNameContext);
            constRef.defKey = new DefKey(null, resolveFqn(qNameContext.getText()));
            support.emit(constRef);
            return;
        }

        PHPParser.LiteralConstantContext literalConstantContext = ctx.literalConstant();
        if (literalConstantContext != null && literalConstantContext.stringConstant() != null) {
            Ref constRef = support.ref(literalConstantContext);
            constRef.defKey = new DefKey(null, resolveFqn(literalConstantContext.getText()));
            support.emit(constRef);
            return;
        }

        PHPParser.ClassConstantContext classConstantContext = ctx.classConstant();
        if (classConstantContext != null) {
            processClassConstantRef(classConstantContext, false);
        }
    }

    /**
     * Handles new Foo() statements
     */
    @Override
    public void enterNewexpr(PHPParser.NewexprContext ctx) {
        ParserRuleContext typeCtx = ctx.typeRef();
        String localClassName = typeCtx.getText();
        String className = resolveFqn(localClassName);
        resolveClass(className);
        String method = "__construct";
        String defining = this.support.getDefiningClass(className, method);
        if (defining == null) {
            // backward compatibility
            // class A { function A {}}
            method = localClassName;
            defining = this.support.getDefiningClass(className, method);
        }
        if (defining != null) {
            Ref constructorRef = support.ref(typeCtx);
            constructorRef.defKey = new DefKey(null, defining + CLASS_NAME_SEPARATOR + method + "()");
            support.emit(constructorRef);
        }

        // looking for parent assignment expression to track var => type map
        ParserRuleContext parent = ctx.getParent();
        if (parent == null || !(parent instanceof PHPParser.NewExpressionContext)) {
            return;
        }
        parent = parent.getParent();
        if (parent == null || !(parent instanceof PHPParser.AssignmentExpressionContext)) {
            return;
        }
        PHPParser.AssignmentExpressionContext assignment = (PHPParser.AssignmentExpressionContext) parent;
        PHPParser.ChainContext chain = assignment.chain(0);
        if (chain.memberAccess() != null && !chain.memberAccess().isEmpty()) {
            // we don't supporting $foo->bar = new baz; yet
            return;
        }
        PHPParser.ChainBaseContext chainBase = chain.chainBase();
        if (chainBase.qualifiedStaticTypeRef() != null) {
            // we don't supporting foo::$bar = new baz; yet
            return;
        }
        List<PHPParser.KeyedVariableContext> vars = chainBase.keyedVariable();
        if (vars.size() > 1) {
            // we don't supporting $foo::$bar = new baz; yet
            return;
        }
        String varName = vars.get(0).getText();
        VarInfo info = support.vars.peek().get(varName);
        if (info != null) {
            // updating type info
            info.type = className;
        }

    }

    /**
     * Handles namespace foo; statements.
     */
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

    /**
     * Handles use foo [as bar]; statements.
     */
    @Override
    public void enterUseDeclaration(PHPParser.UseDeclarationContext ctx) {
        List<PHPParser.UseDeclarationContentContext> declarations = ctx.useDeclarationContentList().
                useDeclarationContent();
        if (declarations == null) {
            return;
        }
        for (PHPParser.UseDeclarationContentContext declaration : declarations) {
            String ns = makeNamespaceName(declaration.namespaceNameList());
            String alias;
            if (declaration.As() != null) {
                // use My\Full\Classname as Another
                alias = declaration.identifier().getText();
            } else {
                // use My\Full\Classname
                alias = StringUtils.substringAfterLast(ns, NAMESPACE_SEPARATOR);
            }
            namespaceAliases.put(alias, ns);
            if (ctx.Const() != null) {
                Ref useConstRef = support.ref(declaration.namespaceNameList());
                useConstRef.defKey = new DefKey(null, ns);
                support.emit(useConstRef);
            } else if (ctx.Function() != null) {
                Ref useFunctionRef = support.ref(declaration.namespaceNameList());
                useFunctionRef.defKey = new DefKey(null, ns);
                support.emit(useFunctionRef);
            }
        }
    }

    /**
     * Handles foo::$bar, $foo, and $foo::$bar
     */
    @Override
    public void enterChainBase(PHPParser.ChainBaseContext ctx) {
        ParserRuleContext classNameCtx = ctx.qualifiedStaticTypeRef();
        if (classNameCtx != null) {
            // Foo::$bar
            String typeName = resolveFqn(classNameCtx.getText());
            resolveClass(typeName);

            ParserRuleContext varCtx = ctx.keyedVariable(0);

            String propertyClass = support.getPropertyClass(typeName, varCtx.getText());
            if (propertyClass == null) {
                if (support.classes.containsKey(typeName)) {
                    Ref classRef = support.ref(classNameCtx);
                    classRef.defKey = new DefKey(null, typeName);
                    support.emit(classRef);
                }
                // maybe we'll be able to guess def later
                Ref staticClassPropertyRef = support.ref(varCtx);
                staticClassPropertyRef.candidate = true;
                staticClassPropertyRef.defKey = new DefKey(null, MAYBE_PROPERTY + varCtx.getText());
                support.emit(staticClassPropertyRef);
            } else {
                Ref classRef = support.ref(classNameCtx);
                classRef.defKey = new DefKey(null, typeName);
                support.emit(classRef);
                Ref staticClassPropertyRef = support.ref(varCtx);
                staticClassPropertyRef.defKey = new DefKey(null, propertyClass + CLASS_NAME_SEPARATOR + varCtx.getText());
                support.emit(staticClassPropertyRef);
            }
            return;
        }
        List<PHPParser.KeyedVariableContext> vars = ctx.keyedVariable();
        if (vars.size() == 1) {
            // $foo
            processVariable(vars.get(0));
            return;
        }
        // $foo::$bar
        String objectVarName = vars.get(0).getText();
        VarInfo info;
        Map<String, VarInfo> localVars = support.vars.peek();
        String path = null;
        if (THIS_KEYWORD.equals(objectVarName)) {
            if (currentClassInfo == null) {
                return;
            }
            info = new VarInfo(currentClassInfo.className, true);
            path = currentClassInfo.className + CLASS_NAME_SEPARATOR + THIS_KEYWORD;
        } else {
            if (functionArguments.containsKey(objectVarName)) {
                info = new VarInfo(functionArguments.get(objectVarName), true);
            } else {
                info = localVars.get(objectVarName);
            }
        }
        if (info != null) {
            Ref objectVarRef = support.ref(vars.get(0));
            if (path == null) {
                path = info.local ? fqn(getBlockNamePrefix()) + objectVarName : NAMESPACE_SEPARATOR + objectVarName;
            }
            objectVarRef.defKey = new DefKey(null, path);
            support.emit(objectVarRef);
        }

        String propertyVarName = vars.get(1).getText();
        if (info != null) {
            String type = this.support.getPropertyClass(info.type, propertyVarName);
            if (type != null) {
                // we were able to resolve type
                Ref propertyVarRef = support.ref(vars.get(1));
                path = type + CLASS_NAME_SEPARATOR + propertyVarName;
                propertyVarRef.defKey = new DefKey(null, path);
                support.emit(propertyVarRef);
                return;
            }
        }

        // maybe we'll resolve it later
        Ref propertyVarRef = support.ref(vars.get(1));
        path = MAYBE_PROPERTY + propertyVarName;
        propertyVarRef.defKey = new DefKey(null, path);
        propertyVarRef.candidate = true;
        support.emit(propertyVarRef);
    }

    /**
     * Handles $foo->bar and $foo->bar() statements.
     */
    @Override
    public void enterMemberAccess(PHPParser.MemberAccessContext ctx) {
        PHPParser.ChainContext parent = (PHPParser.ChainContext) ctx.getParent();
        PHPParser.ChainBaseContext chainBaseContext = parent.chainBase();

        String varName = null;
        String varType = null;

        if (chainBaseContext != null) {
            List<PHPParser.KeyedVariableContext> var = chainBaseContext.keyedVariable();
            if (var != null && !var.isEmpty()) {
                TerminalNode varNameNode = var.get(0).VarName();
                if (varNameNode != null) {
                    varName = varNameNode.getText();
                    if (THIS_KEYWORD.equals(varName)) {
                        if (currentClassInfo != null) {
                            varType = currentClassInfo.className;
                        }
                    } else {
                        varType = functionArguments.get(varName);
                    }
                }
            }
        }

        if (varType == null) {
            VarInfo info = this.support.vars.peek().get(varName);
            if (info != null) {
                varType = info.type;
            }
        }
        PHPParser.KeyedFieldNameContext keyedFieldNameContext = ctx.keyedFieldName();
        String propertyName = keyedFieldNameContext.getText();
        boolean isMethodCall = ctx.actualArguments() != null;
        if (!isMethodCall && keyedFieldNameContext.keyedSimpleFieldName() != null) {
            propertyName = "$" + propertyName;}

        Ref ref = support.ref(keyedFieldNameContext);
        if (varType == null) {
            ref.candidate = true;
            String prefix;
            if (isMethodCall) {
                prefix = MAYBE_METHOD;
            } else {
                prefix = MAYBE_PROPERTY;
            }
            ref.defKey = new DefKey(null, prefix + propertyName);
        } else {
            String propertyClass = support.getPropertyClass(varType, propertyName);
            if (propertyClass == null) {
                propertyClass = varType;
            }
            String path = propertyClass + CLASS_NAME_SEPARATOR + propertyName;
            if (isMethodCall) {
                path += "()";
            }
            ref.defKey = new DefKey(null, path);
        }
        support.emit(ref);
    }

    /**
     * Handles references to $foo
     */
    private void processVariable(PHPParser.KeyedVariableContext ctx) {
        TerminalNode varNameNode = ctx.VarName();
        if (varNameNode == null) {
            return;
        }
        String varName = varNameNode.getText();
        VarInfo info;
        Map<String, VarInfo> localVars = support.vars.peek();
        String path = null;
        if (THIS_KEYWORD.equals(varName)) {
            if (currentClassInfo == null) {
                return;
            }
            info = new VarInfo(currentClassInfo.className, true);
            path = currentClassInfo.className + CLASS_NAME_SEPARATOR + THIS_KEYWORD;
        } else {
            if (functionArguments.containsKey(varName)) {
                info = new VarInfo(functionArguments.get(varName), true);
            } else {
                info = localVars.get(varName);
            }
        }
        if (info == null) {
            // new variable
            Def varDef = def(varNameNode.getSymbol(), DefKind.VARIABLE);
            if (!blockStack.empty()) {
                varDef.local = true;
                varDef.exported = false;
                info = new VarInfo(null, true);
            } else {
                varDef.local = false;
                varDef.exported = true;
                info = new VarInfo(null, false);
            }
            varDef.defKey = new DefKey(null, fqn(getBlockNamePrefix()) + varName);
            varDef.format(StringUtils.EMPTY, "mixed", DefData.SEPARATOR_SPACE);
            varDef.defData.setKind("variable");
            support.emit(varDef);
            localVars.put(varName, info);
        } else {
            Ref varRef = support.ref(varNameNode.getSymbol());
            if (path == null) {
                path = info.local ? fqn(getBlockNamePrefix()) + varName : NAMESPACE_SEPARATOR + varName;
            }
            varRef.defKey = new DefKey(null, path);
            support.emit(varRef);
        }
    }

    /**
     * Updates current namespace stack
     * @param ctx
     */
    private void processNamespaceChange(PHPParser.NamespaceNameListContext ctx) {
        namespace.push(makeNamespaceName(ctx));
    }

    /**
     * Constructs namespace
     * @param ctx namespace list
     * @return namespace string
     */
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
                ns.append(NAMESPACE_SEPARATOR);
                ns.append(identifier.getText());
            }
            return ns.toString();
        }
    }

    /**
     * Processes class properties
     * @param variables
     */
    private void processClassProperties(List<PHPParser.VariableInitializerContext> variables) {
        String blockName = blockStack.pop();

        for (PHPParser.VariableInitializerContext variable : variables) {
            Def propertyDef = def(variable.VarName().getSymbol(), DefKind.VARIABLE);
            propertyDef.local = false;
            propertyDef.exported = true;
            propertyDef.defKey = new DefKey(null, fqn(getBlockNamePrefix() + propertyDef.name));
            propertyDef.format(StringUtils.EMPTY, "mixed", DefData.SEPARATOR_SPACE);
            propertyDef.defData.setName(classLevelLabel(propertyDef.name));
            propertyDef.defData.setKind("property");
            currentClassInfo.properties.add(propertyDef.name);
            support.emit(propertyDef);
            support.resolutions.put(MAYBE_PROPERTY + propertyDef.name, propertyDef);
        }
        blockStack.push(blockName);
    }

    /**
     * Processes class constants
     * @param constants
     */
    private void processClassConstant(List<PHPParser.IdentifierInititalizerContext> constants) {
        String blockName = blockStack.pop();
        for (PHPParser.IdentifierInititalizerContext constant : constants) {
            Def classConstantDef = def(constant.identifier(), DefKind.CONSTANT);
            classConstantDef.defKey = new DefKey(null, fqn(getBlockNamePrefix() + classConstantDef.name));
            classConstantDef.format("const", "const", DefData.SEPARATOR_SPACE);
            classConstantDef.defData.setKind("constant");
            classConstantDef.defData.setName(classLevelLabel(classConstantDef.name));
            support.emit(classConstantDef);
            support.resolutions.put(MAYBE_CONSTANT + classConstantDef.name, classConstantDef);
            currentClassInfo.constants.add(classConstantDef.name);
        }
        blockStack.push(blockName);
    }

    /**
     * Processes class method
     */
    private void processClassMethod(PHPParser.ClassStatementContext ctx) {

        // does current class defines method? implements it? both?

        blockStack.pop();
        String className = blockStack.peek();
        ParserRuleContext methodCtx = ctx.identifier();
        String methodName = methodCtx.getText();

        String definingClass = this.support.getDefiningClass(fqn(className), methodName);
        if (definingClass == null) {
            currentClassInfo.definesMethods.add(methodName);
            Def classMethodDef = def(methodCtx, DefKind.METHOD);
            // adding () to distinguish properties from methods
            classMethodDef.defKey = new DefKey(null, fqn(className + CLASS_NAME_SEPARATOR + methodName + "()"));
            support.emit(classMethodDef);
            classMethodDef.format("function", "(" + ctx.formalParameterList().getText() + ")", DefData.SEPARATOR_EMPTY);
            classMethodDef.defData.setName(classLevelLabel(classMethodDef.name));
            classMethodDef.defData.setKind("method");
            support.resolutions.put(MAYBE_METHOD + methodName, classMethodDef);
        } else {
            Ref classMethodRef = support.ref(methodCtx);
            // adding () to distinguish properties from methods
            classMethodRef.defKey = new DefKey(null, resolveFqn(definingClass) + CLASS_NAME_SEPARATOR + methodName + "()");
            support.emit(classMethodRef);
        }

        blockStack.push(methodName);

        List<PHPParser.FormalParameterContext> fnParams = ctx.formalParameterList().formalParameter();
        processFunctionParameters(fnParams);
    }

    /**
     * Processes "use trait"
     * @param ctx
     */
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

    /**
     * Handles function parameters
     * @param parameters
     */
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
            fnArgDef.format(StringUtils.EMPTY, typeName == null ? "mixed" : typeName, DefData.SEPARATOR_SPACE);
            fnArgDef.defData.setKind("argument");
            support.emit(fnArgDef);
            functionArguments.put(fnArgDef.name, typeName);
        }
    }

    /**
     * Handles foo::bar statements
     * @param ctx
     * @param isCall
     */
    private void processClassConstantRef(PHPParser.ClassConstantContext ctx, boolean isCall) {
        if (ctx.identifier() == null) {
            return;
        }

        String parts[] = ctx.getText().split("::");
        String rootClassName = null;
        if ("self".equals(parts[0])) {
            if (currentClassInfo == null) {
                return;
            }
            rootClassName = currentClassInfo.className;
            Ref selfRef = support.ref(ctx.qualifiedStaticTypeRef());
            selfRef.defKey = new DefKey(null, rootClassName);
            support.emit(selfRef);
        } else if ("parent".equals(parts[0])) {
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
                rootClassName = fqn;
                Ref classRef = support.ref(ctx.qualifiedStaticTypeRef());
                classRef.defKey = new DefKey(null, fqn);
                support.emit(classRef);
            } else {
                VarInfo info = support.vars.peek().get(parts[0]);
                if (info != null) {
                    rootClassName = info.type;
                }
            }
        }

        if (rootClassName == null) {
            if (isCall) {
                // method, putting a candidate because we were unable to identify class name
                Ref maybeClassMethodRef = support.ref(ctx.identifier());
                maybeClassMethodRef.candidate = true;
                maybeClassMethodRef.defKey = new DefKey(null, MAYBE_METHOD + parts[1] + "()");
                support.emit(maybeClassMethodRef);
            } else {
                // constant, putting a candidate because we were unable to identify class name
                Ref maybeClassConstantRef = support.ref(ctx.identifier());
                maybeClassConstantRef.candidate = true;
                maybeClassConstantRef.defKey = new DefKey(null, MAYBE_CONSTANT + parts[1]);
                support.emit(maybeClassConstantRef);
            }
            return;
        }

        if (isCall) {
            String methodClass = this.support.getDefiningClass(rootClassName, parts[1]);
            if (methodClass != null) {
                Ref classMethodRef = support.ref(ctx.identifier());
                classMethodRef.defKey = new DefKey(null, methodClass + CLASS_NAME_SEPARATOR + parts[1] + "()");
                support.emit(classMethodRef);
            }
        } else {
            String constantClass = this.support.getConstantClass(rootClassName, parts[1]);
            if (constantClass != null) {
                Ref classConstRef = support.ref(ctx.identifier());
                classConstRef.defKey = new DefKey(null, constantClass + CLASS_NAME_SEPARATOR + parts[1]);
                support.emit(classConstRef);
            }
        }
    }

    /**
     * @return suffix constructed from current block stack, for example ":class:function"
     */
    private String getBlockNameSuffix() {
        StringBuilder ret = new StringBuilder();
        for (String blockName : blockStack) {
            ret.append(':').append(blockName);
        }
        return ret.toString();
    }

    /**
     * @return prefix constructed from current block stack, for example "class/"
     */
    private String getBlockNamePrefix() {
        StringBuilder ret = new StringBuilder();
        for (String blockName : blockStack) {
            ret.append(blockName).append(CLASS_NAME_SEPARATOR);
        }
        return ret.toString();
    }

    /**
     * @return current file name converted to printable suffix (replaces / with |)
     */
    private String getFileSuffix() {
        return ":" + support.getCurrentFile().replace('/', '|');
    }

    /**
     * Extracts include file name
     * @param expressionText
     * @return
     */
    private static String extractIncludeName(String expressionText) {
        Matcher m = INCLUDE_EXPRESSION.matcher(expressionText);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * @param expressionText
     * @return extracted constant name
     */
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

    /**
     * @param name local name
     * @return fully qualified name made by combining current namespace with local name
     */
    private String fqn(String name) {
        return namespace.peek() + NAMESPACE_SEPARATOR + name;
    }

    /**
     * @param name absolute, relative or local name
     * @return fully qualified name
     */
    private String resolveFqn(String name) {
        name = name.replace("\\", NAMESPACE_SEPARATOR);
        if (name.startsWith(NAMESPACE_SEPARATOR)) {
            return name;
        }
        String prefix = StringUtils.substringBefore(name, NAMESPACE_SEPARATOR);
        String namespace = namespaceAliases.get(prefix);
        if (namespace != null) {
            return namespace + name.substring(prefix.length());
        }
        return fqn(name);
    }

    /**
     * @param name absolute, relative or local name
     * @return fully qualified function name (there are special rules for functions differ from classes and constants)
     */
    private String resolveFunctionFqn(String name) {
        name = name.replace("\\", NAMESPACE_SEPARATOR);
        String fqn;
        boolean absolute = false;
        if (!name.startsWith(NAMESPACE_SEPARATOR)) {
            fqn = resolveFqn(name);
        } else {
            fqn = name;
            absolute = true;
        }
        // looking in the current namespace
        if (support.functions.contains(fqn)) {
            return fqn;
        }
        // must be global one
        if (!absolute) {
            name = NAMESPACE_SEPARATOR + name;
        }
        return name;
    }

    /**
     * When encountered a statement that might be class name, trying to "touch" it by trying to
     * process file which might define given class (support of PHP autoload)
     * @param fullyQualifiedClassName fully qualified class name
     */
    private void resolveClass(String fullyQualifiedClassName) {
        support.resolveClass(fullyQualifiedClassName.replace(NAMESPACE_SEPARATOR, "\\"));
    }

    /**
     * Formats name defined on global level (possibly with namespace). Used to format functions, global constants
     * @param name name to format
     * @return name optionally prefixed by namespace (if namespace is not empty)
     */
    private String globalLevelLabel(String name) {
        String ns = namespace.peek();
        if (ns.isEmpty()) {
            return name;
        }
        return ns.replace(NAMESPACE_SEPARATOR, "\\") + '\\' + name;
    }

    /**
     * Formats name defined on clas level. Used to format class members, properties, and constants
     * @param name name to format
     * @return label in form [\NS\]CLASSNAME:NAME
     */
    private String classLevelLabel(String name) {
        String ns = namespace.peek();
        if (!ns.isEmpty()) {
            ns = ns.replace(NAMESPACE_SEPARATOR, "\\") + '\\';
        }
        String block = getBlockNamePrefix();
        // removing trailing CLASS_NAME_SEPARATOR
        block = block.substring(0, block.length() - 1);
        return ns + block + "::" + name;
    }

}