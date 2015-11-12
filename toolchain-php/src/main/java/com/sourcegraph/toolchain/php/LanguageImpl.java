package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.php.antlr4.PHPLexer;
import com.sourcegraph.toolchain.php.antlr4.PHPParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class LanguageImpl extends LanguageBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageImpl.class);

    /**
     * keeps global and function-level variables. At each level holds map
     * variable name => is_local
     */
    Stack<Map<String, Boolean>> vars = new Stack<>();

    Map<String, ClassInfo> classes = new HashMap<>();
    Set<String> functions = new HashSet<>();

    /**
     * Map ident => definition. We using it to resolve reference candidates.
     */
    Map<String, Def> resolutions = new HashMap<>();

    private Set<String> visited = new HashSet<>();
    private Set<String> files;

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

    public String getConstantClass(String rootClassName, String constant) {
        ClassInfo info = classes.get(rootClassName);
        if (info == null) {
            return null;
        }
        if (info.definesConstants.contains(constant)) {
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
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(sourceFile,
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
}