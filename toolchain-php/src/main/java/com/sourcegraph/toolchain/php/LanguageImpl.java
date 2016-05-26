package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.PathUtil;
import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.php.antlr4.PHPLexer;
import com.sourcegraph.toolchain.php.antlr4.PHPParser;
import com.sourcegraph.toolchain.php.composer.ComposerConfiguration;
import com.sourcegraph.toolchain.php.composer.schema.Autoload;
import com.sourcegraph.toolchain.php.composer.schema.ComposerSchemaJson;
import com.sourcegraph.toolchain.php.resolver.CompoundClassFileResolver;
import com.sourcegraph.toolchain.php.resolver.PSR0ClassFileResolver;
import com.sourcegraph.toolchain.php.resolver.PSR4ClassFileResolver;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LanguageImpl extends LanguageBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageImpl.class);

    /**
     * keeps global and function-level variables.
     */
    Stack<Map<String, VarInfo>> vars = new Stack<>();

    Map<String, ClassInfo> classes = new HashMap<>();
    Set<String> functions = new HashSet<>();

    private Set<String> seenClasses = new HashSet<>();

    /**
     * Map ident => definition. We using it to resolve reference candidates.
     */
    Map<String, Def> resolutions = new HashMap<>();

    private CompoundClassFileResolver classFileResolver;

    /**
     * @param rootClassName starting class
     * @param methodName method name we searching for
     * @return class name that defines given method or null
     */
    public String getDefiningClass(String rootClassName, String methodName) {
        ClassInfo info = classes.get(rootClassName);
        if (info == null) {
            return null;
        }
        if (info.definesMethods.contains(methodName)) {
            return rootClassName;
        }
        for (String interfaceName : info.implementsInterfaces) {
            String definingClass = getDefiningClass(interfaceName, methodName);
            if (definingClass != null) {
                return definingClass;
            }
        }

        for (String className : info.extendsClasses) {
            String definingClass = getDefiningClass(className, methodName);
            if (definingClass != null) {
                return definingClass;
            }
        }

        for (String traitName : info.usesTraits) {
            String definingClass = getDefiningClass(traitName, methodName);
            if (definingClass != null) {
                return definingClass;
            }
        }
        return null;
    }

    /**
     * @param rootClassName starting class
     * @param constant constant name
     * @return class name that defines given constant or null
     */
    public String getConstantClass(String rootClassName, String constant) {
        ClassInfo info = classes.get(rootClassName);
        if (info == null) {
            return null;
        }
        if (info.constants.contains(constant)) {
            return rootClassName;
        }
        for (String interfaceName : info.implementsInterfaces) {
            String definingClass = getConstantClass(interfaceName, constant);
            if (definingClass != null) {
                return definingClass;
            }
        }

        for (String className : info.extendsClasses) {
            String definingClass = getConstantClass(className, constant);
            if (definingClass != null) {
                return definingClass;
            }
        }

        for (String traitName : info.usesTraits) {
            String definingClass = getConstantClass(traitName, constant);
            if (definingClass != null) {
                return definingClass;
            }
        }
        return null;
    }

    /**
     * @param rootClassName starting class
     * @param property property name
     * @return class name that defines given property or null
     */
    public String getPropertyClass(String rootClassName, String property) {
        ClassInfo info = classes.get(rootClassName);
        if (info == null) {
            return null;
        }
        if (info.properties.contains(property)) {
            return rootClassName;
        }
        for (String interfaceName : info.implementsInterfaces) {
            String definingClass = getPropertyClass(interfaceName, property);
            if (definingClass != null) {
                return definingClass;
            }
        }

        for (String className : info.extendsClasses) {
            String definingClass = getPropertyClass(className, property);
            if (definingClass != null) {
                return definingClass;
            }
        }

        for (String traitName : info.usesTraits) {
            String definingClass = getPropertyClass(traitName, property);
            if (definingClass != null) {
                return definingClass;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "php";
    }

    @Override
    public DefKey resolve(DefKey source) {
        Def def = resolutions.get(source.getPath());
        if (def != null) {
            return def.defKey;
        }
        return null;
    }

    @Override
    public void graph() {
        // Before graphing, let's load composer configuration if there is any
        this.classFileResolver = new CompoundClassFileResolver();

        File composerJson = new File(PathUtil.CWD.toFile(), "composer.json");
        if (composerJson.isFile()) {
            try {
                ComposerSchemaJson configuration = ComposerConfiguration.getConfiguration(composerJson);
                initAutoLoader(configuration);
            } catch (IOException e) {
                LOGGER.warn("Failed to read composer configuration {}", e.getMessage());
            }
        }
        super.graph();
    }

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(this,
                    sourceFile,
                    PHPLexer.class,
                    PHPParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((PHPParser) configuration.parser).htmlDocument();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new PHPParseTreeListener(this), tree);
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

    @Override
    protected FileCollector getFileCollector(File rootDir, String repoUri) {
        ExtensionBasedFileCollector collector = new ExtensionBasedFileCollector().extension(".php");
        File composerJson = new File(rootDir, "composer.json");
        if (composerJson.isFile()) {
            collector.exclude("vendor");
        }
        return collector;
    }

    /**
     * Invoked by PHP parse tree listener to "touch" class.
     * PHP language support tries to resolve class file using registered class resolver(s)
     * and if file is found, we are trying to parse it before processing current class
     * @param fullyQualifiedClassName FQCN
     */
    protected void resolveClass(String fullyQualifiedClassName) {
        if (!seenClasses.add(fullyQualifiedClassName)) {
            return;
        }
        File file = classFileResolver.resolve(fullyQualifiedClassName);
        if (file != null) {
            process(file);
        }
    }

    /**
     * Initializes autoloader (currently PSR-4 and PSR-0 are supported)
     * @param composerSchemaJson configuration from composer.json
     */
    private void initAutoLoader(ComposerSchemaJson composerSchemaJson) {
        Autoload autoload = composerSchemaJson.getAutoload();
        if (autoload == null) {
            return;
        }

        Map<String, List<String>> psr4 = autoload.getPsr4();
        if (psr4 != null) {
            PSR4ClassFileResolver psr4ClassFileResolver = new PSR4ClassFileResolver();
            for (Map.Entry<String, List<String>> entry : psr4.entrySet()) {
                for (String directory : entry.getValue()) {
                    psr4ClassFileResolver.addNamespace(entry.getKey(), directory);
                }
            }
            classFileResolver.addResolver(psr4ClassFileResolver);
        }

        Map<String, List<String>> psr0 = autoload.getPsr0();
        if (psr0 != null) {
            PSR0ClassFileResolver psr0ClassFileResolver = new PSR0ClassFileResolver();
            for (Map.Entry<String, List<String>> entry : psr0.entrySet()) {
                for (String directory : entry.getValue()) {
                    psr0ClassFileResolver.addNamespace(entry.getKey(), directory);
                }
            }
            classFileResolver.addResolver(psr0ClassFileResolver);
        }
        // TODO: classmap?

    }
}