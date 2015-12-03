package com.sourcegraph.toolchain.ruby;

import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.ruby.antlr4.RubyLexer;
import com.sourcegraph.toolchain.ruby.antlr4.RubyParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LanguageImpl extends LanguageBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageImpl.class);

    GraphWriter writer;

    Map<String, String> globalVars = new HashMap<>();
    // class name -> (variable -> type)
    Map<String, Map<String, String>> instanceVars = new HashMap<>();

    Set<String> functions = new HashSet<>();
    Set<String> types = new HashSet<>();

    private Set<String> visited = new HashSet<>();
    private Set<String> files;

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(sourceFile,
                    RubyLexer.class,
                    RubyParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((RubyParser) configuration.parser).program();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new RubyParseTreeListener(this), tree);
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
    public DefKey resolve(DefKey source) {
        // TODO (alexsaveliev)
        return null;
    }
}