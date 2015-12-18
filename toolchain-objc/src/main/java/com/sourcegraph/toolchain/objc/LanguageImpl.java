package com.sourcegraph.toolchain.objc;

import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.objc.antlr4.ObjCLexer;
import com.sourcegraph.toolchain.objc.antlr4.ObjCParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.util.*;

public class LanguageImpl extends LanguageBase {

    Map<String, String> globalVars = new HashMap<>();
    // class name -> (variable -> type)
    Map<String, Map<String, String>> instanceVars = new HashMap<>();

    Set<String> functions = new HashSet<>();
    Set<String> types = new HashSet<>();

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(sourceFile,
                    ObjCLexer.class,
                    ObjCParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((ObjCParser) configuration.parser).translation_unit();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new ObjCParseTreeListener(this), tree);
        } catch (Exception e) {
            throw new ParseException(e);
        }

    }

    @Override
    protected FileCollector getFileCollector(File rootDir, String repoUri) {
        return new ExtensionBasedFileCollector().extension(".h", ".m", ".mm");
    }

    @Override
    public String getName() {
        return "objc";
    }

    @Override
    public Collection<DefKey> resolve(DefKey source) {
        // TODO (alexsaveliev)
        return null;
    }
}