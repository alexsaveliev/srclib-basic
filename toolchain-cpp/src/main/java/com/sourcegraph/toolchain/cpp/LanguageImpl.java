package com.sourcegraph.toolchain.cpp;

import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.cpp.antlr4.CPP14Lexer;
import com.sourcegraph.toolchain.cpp.antlr4.CPP14Parser;
import com.sourcegraph.toolchain.language.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        // If found .m or .mm file - it's Objective-C
        return new ExtensionBasedFileCollector().
                extension(
                        ".C", ".cc", ".cpp", ".CPP", ".c++", ".cp", ".cxx",
                        ".h", ".H", ".hh", ".hpp", ".HPP", ".h++", ".hp", ".hxx").
                blockerExtension(".m", ".mm");
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

    /**
     * Handles "#include "foo" directives, tries to resolve file in the current set
     *
     * @param path path to file to be included
     */
    @SuppressWarnings("unused")
    public void include(String path) {
        Path p = Paths.get(path);
        for (File candidate : files) {
            if (candidate.toPath().endsWith(p)) {
                process(candidate);
                break;
            }
        }
    }
}