package com.sourcegraph.toolchain.swift;

import com.sourcegraph.toolchain.core.PathUtil;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import com.sourcegraph.toolchain.swift.antlr4.SwiftLexer;
import com.sourcegraph.toolchain.swift.antlr4.SwiftParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LanguageImpl extends LanguageBase {

    TypeInfos<Scope, String> infos = new TypeInfos<>();

    boolean firstPass = true;

    private Map<File, ParseTree> trees = new HashMap<>();

    public LanguageImpl() {
        super();
        infos.getRoot().setData(new Scope(StringUtils.EMPTY, StringUtils.EMPTY));
    }

    @Override
    public void graph() {
        // first pass to extract defs
        super.graph();

        // second pass to extract refs
        firstPass = false;
        for (Map.Entry<File, ParseTree> entry : trees.entrySet()) {
            processingPath.push(PathUtil.relativizeCwd(entry.getKey().toPath()));
            LOGGER.info("Extracting refs from {}", getCurrentFile());
            try {
                ParseTreeWalker walker = new ParseTreeWalker();
                walker.walk(new SwiftParseTreeListener(this), entry.getValue());
            } catch (Exception e) {
                LOGGER.error("Failed to process {} - unexpected error", getCurrentFile(), e);
            } finally {
                processingPath.pop();
            }
        }
    }

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(
                    this,
                    sourceFile,
                    SwiftLexer.class,
                    SwiftParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((SwiftParser) configuration.parser).top_level();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new SwiftParseTreeListener(this), tree);
            trees.put(sourceFile, tree);
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

    @Override
    protected CharStream getCharStream(File sourceFile) throws IOException {
        Reader r = new PreprocessorCleaner(
                new InputStreamReader(
                        new FileInputStream(sourceFile), Charsets.UTF_8));
        return new ANTLRInputStream(r);
    }

    private static class PreprocessorCleaner extends FilterReader {

        enum STATE {
            START,
            REGULAR,
            EOL,
            DIRECTIVE
        }

        private STATE state = STATE.START;

        PreprocessorCleaner(Reader source) {
            super(source);
        }

        @Override
        public int read() throws IOException {
            int c = super.read();
            return convert(c);
        }

        @Override
        public int read(char cbuf[], int off, int len) throws IOException {
            int l = super.read(cbuf, off, len);
            for (int i = 0; i < l; i++) {
                int pos = off + i;
                cbuf[pos] = (char) convert(cbuf[pos]);
            }
            return l;
        }

        private int convert(int c) {
            if (c == -1) {
                return c;
            }
            if (c == '#') {
                if (state == STATE.START || state == STATE.EOL) {
                    // preprocessor directive starts
                    state = STATE.DIRECTIVE;
                }
            } else if (c == '\r' || c == '\n') {
                state = STATE.EOL;
            } else {
                if (state == STATE.START || state == STATE.EOL) {
                    state = STATE.REGULAR;
                }
            }
            return state == STATE.DIRECTIVE ? '/' : c;
        }
    }

}