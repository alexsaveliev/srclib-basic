package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptLexer;
import com.sourcegraph.toolchain.js.antlr4.JavaScriptParser;
import com.sourcegraph.toolchain.language.*;
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
    private Set<String> visited = new HashSet<>();
    private Set<String> files;

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(sourceFile,
                    JavaScriptLexer.class,
                    JavaScriptParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((JavaScriptParser) configuration.parser).program();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new JavaScriptParseTreeListener(this), tree);
        } catch (Exception e) {
            throw new ParseException(e);
        }

    }

    @Override
    protected FileCollector getFileCollector(File rootDir, String repoUri) {
        return new ExtensionBasedFileCollector().extension(".js");
    }

    @Override
    public String getName() {
        return "js";
    }

    @Override
    public DefKey resolve(DefKey source) {
        return null;
    }
}