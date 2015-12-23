package com.sourcegraph.toolchain.swift;

import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.swift.antlr4.SwiftLexer;
import com.sourcegraph.toolchain.swift.antlr4.SwiftParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;

public class LanguageImpl extends LanguageBase {

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(sourceFile,
                    SwiftLexer.class,
                    SwiftParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((SwiftParser) configuration.parser).top_level();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new SwiftParseTreeListener(this), tree);
        } catch (Exception e) {
            throw new ParseException(e);
        }

    }

    @Override
    protected FileCollector getFileCollector(File rootDir, String repoUri) {
        return new ExtensionBasedFileCollector().extension(".swift");
    }

    @Override
    public String getName() {
        return "swift";
    }

    @Override
    public DefKey resolve(DefKey source) {
        return null;
    }
}