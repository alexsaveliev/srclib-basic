package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.objc.antlr4.CPP14Lexer;
import com.sourcegraph.toolchain.objc.antlr4.CPP14Parser;
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

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(this,
                    sourceFile,
                    CPP14Lexer.class,
                    CPP14Parser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((CPP14Parser) configuration.parser).translationunit();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new CPPParseTreeListener(this), tree);
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
        return "cpp";
    }

    @Override
    public DefKey resolve(DefKey source) {
        // TODO (alexsaveliev)
        return null;
    }
}