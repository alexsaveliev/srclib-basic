package com.sourcegraph.toolchain.objc;

import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.SourceUnit;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.objc.antlr4.ObjCLexer;
import com.sourcegraph.toolchain.objc.antlr4.ObjCParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LanguageImpl extends LanguageBase {

    Map<String, String> globalVars = new HashMap<>();
    // class name -> (variable -> type)
    Map<String, Map<String, String>> instanceVars = new HashMap<>();

    Set<String> functions = new HashSet<>();
    Set<String> types = new HashSet<>();

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(this,
                    sourceFile,
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
    protected SourceUnit getSourceUnit(File rootDir, String repoUri) throws IOException {
        SourceUnit unit = super.getSourceUnit(rootDir, repoUri);
        boolean hasSourceCode = false;
        // we expect at least one .m or .mm file, otherwise it's probably C++
        for (String file : unit.Files) {
            if (!file.endsWith(".h")) {
                hasSourceCode = true;
                break;
            }
        }
        if (!hasSourceCode) {
            unit.Files.clear();
        }
        return unit;
    }

    @Override
    public String getName() {
        return "objc";
    }

    @Override
    public DefKey resolve(DefKey source) {
        // TODO (alexsaveliev)
        return null;
    }
}