package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.cpp.antlr4.CPP14Lexer;
import com.sourcegraph.toolchain.cpp.antlr4.CPP14Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;

public class LanguageImpl extends LanguageBase {

    TypeInfos<Scope, ObjectInfo> infos = new TypeInfos<>();

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(this,
                    sourceFile,
                    CPP14Lexer.class,
                    CPP14Parser.class,
                    new DefaultErrorListener(sourceFile));
            ((CPP14Lexer) configuration.lexer).setSupport(this);
            ParseTree tree = ((CPP14Parser) configuration.parser).translationunit();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new CPPParseTreeListener(this), tree);
        } catch (Exception e) {
            throw new ParseException(e);
        }

    }

    @Override
    protected FileCollector getFileCollector(File rootDir, String repoUri) {
        return new ExtensionBasedFileCollector().extension(
                ".C", ".cc", ".cpp", ".CPP", ".c++", ".cp", ".cxx",
                ".h", ".H", ".hh", ".hpp", ".HPP", ".h++", ".hp", ".hxx");
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

    /**
     * Changed visibility for dev purposes
     */
    @Override
    public CharStream getCharStream(File sourceFile) throws IOException {
        return super.getCharStream(sourceFile);
    }
}