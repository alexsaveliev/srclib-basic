package com.sourcegraph.toolchain.language;

import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.PathUtil;
import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.core.objects.SourceUnit;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for ANTLR-based language support.
 */
public abstract class LanguageBase implements com.sourcegraph.toolchain.language.Language {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LanguageBase.class);

    /**
     * Contains processing path to current file (for example B was scheduled for processing from C scheduled from A)
     */
    protected Stack<String> processingPath = new Stack<>();

    /**
     * Writer object to use
     */
    protected GraphWriter writer;

    /**
     * Source unit to graph
     */
    protected SourceUnit unit;

    /**
     * List of files that were already visited during current session
     */
    protected Set<File> visited = new HashSet<>();

    /**
     * List of files to process converted to set for fast lookup purposes
     */
    protected Set<File> files;

    @Override
    public void setSourceUnit(SourceUnit unit) {
        this.unit = unit;
    }

    @Override
    public void setGraphWriter(GraphWriter writer) {
        this.writer = writer;
    }

    /**
     * Base implementation expects single source unit per language
     */
    @Override
    public Collection<SourceUnit> getSourceUnits(File rootDir, String repoUri) throws IOException {
        return Collections.singleton(getSourceUnit(rootDir, repoUri));
    }

    @Override
    public void graph() {
        // Converting unit file's to set for faster lookup
        this.files = new HashSet<>();
        for (String file : unit.Files) {
            File sourceFile = new File(file);
            if (sourceFile.isFile()) {
                this.files.add(sourceFile);
            } else {
                LOGGER.warn("File {} does not exist or not a file, skipping", sourceFile);
            }
        }
        // Processing files sequentially
        this.files.forEach(this::process);
    }

    /**
     * Processes single file. May be called by parser tree listener to add more file for processing.
     * For example when parser encountered "#include "foo" it may ask to process foo before current file
     * @param sourceFile source file to process
     */
    public void process(File sourceFile) {
        // We do not accept not-existing or not-a-files
        if (!sourceFile.isFile()) {
            LOGGER.debug("Requested processing of not-existing {}", sourceFile);
            return;
        }
        // Already visited
        if (visited.contains(sourceFile)) {
            return;
        }
        // Not in the list
        // TODO (alexsaveliev)
        if (!files.contains(sourceFile)) {
            LOGGER.debug("Requested processing of outer file {}", sourceFile);
            return;
        }
        LOGGER.info("Processing {}", sourceFile);
        visited.add(sourceFile);

        processingPath.push(PathUtil.relativizeCwd(sourceFile.toPath()));

        try {
            parse(sourceFile);
        } catch (Exception e) {
            LOGGER.error("Failed to process {} - unexpected error", sourceFile, e);
        } finally {
            processingPath.pop();
        }
    }

    /**
     * @param ctx parser rule context
     * @param kind def's kind (i.e. "const")
     * @return Def object initialized with the proper info (span, text, file)
     */
    public Def def(ParserRuleContext ctx, String kind) {
        Def def = new Def();
        def.defStart = ctx.getStart().getStartIndex();
        def.defEnd = ctx.getStop().getStopIndex() + 1;
        def.name = ctx.getText();
        def.file = getCurrentFile();
        def.kind = kind;
        return def;
    }

    /**
     * @param token lexer's token
     * @param kind def's kind (i.e. "const")
     * @return Def object initialized with the proper info (span, text, file)
     */
    public Def def(Token token, String kind) {
        Def def = new Def();
        def.defStart = token.getStartIndex();
        def.defEnd = token.getStopIndex() + 1;
        def.name = token.getText();
        def.file = getCurrentFile();
        def.kind = kind;
        return def;
    }

    /**
     * @param ctx parser rule context
     * @return new Ref object initialized with proper info (span, file)
     */
    public Ref ref(ParserRuleContext ctx) {
        Ref ref = new Ref();
        ref.start = ctx.getStart().getStartIndex();
        ref.end = ctx.getStop().getStopIndex() + 1;
        ref.file = getCurrentFile();
        return ref;
    }

    /**
     * @param token lexer token
     * @return new Ref object initialized with proper info (span, file)
     */
    public Ref ref(Token token) {
        Ref ref = new Ref();
        ref.start = token.getStartIndex();
        ref.end = token.getStopIndex() + 1;
        ref.file = getCurrentFile();
        return ref;
    }

    /**
     * Emits def object (and automatically emits ref)
     * @param def definition to emit
     */
    public void emit(Def def) {
        writer.writeDef(def);
        // auto-adding self-references
        Ref ref = new Ref();
        ref.defKey = def.defKey;
        ref.def = true;
        ref.start = def.defStart;
        ref.end = def.defEnd;
        ref.file = def.file;
        emit(ref);
    }

    /**
     * Emits ref object
     * @param ref reference to emit
     */
    public void emit(Ref ref) {
        writer.writeRef(ref);
    }

    /**
     * @return path (relative to CWD) to file being processed
     */
    public String getCurrentFile() {
        return processingPath.peek();
    }

    /**
     * Parses given source file and emits defs and refs.
     * Implementation expects to do something like
     * {code}
     * GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(sourceFile,
     * (LANGUAGE)Lexer.class,
     * (LANGUAGE)Parser.class,
     * new DefaultErrorListener(sourceFile));
     * ParseTree tree = (((LANGUAGE)Parser) configuration.parser).(ROOTELEMENT)();
     * ParseTreeWalker walker = new ParseTreeWalker();
     * walker.walk(new (LANGUAGE)ParseTreeListener(this), tree);
     * {/code}
     * We expect that implementation will ask ANTLR to tokenize and parse source file and then will
     * traverse AST built by ANTLR emitting defs and refs
     * @param sourceFile source file to process
     * @throws ParseException
     */
    protected abstract void parse(File sourceFile) throws ParseException;

    /**
     * @param rootDir root directory
     * @param repoUri repository URI
     * @return file collector. In a basic case we should construct ExtensionBasedFileCollector and feed it with proper
     * extensions and includes/exludes
     */
    protected abstract FileCollector getFileCollector(File rootDir, String repoUri);

    /**
     * Makes single source unit from a given source dir, collecting all source files that match current language
     * @param rootDir root directory
     * @param repoUri repository URI
     * @return source unit
     * @throws IOException
     */
    protected SourceUnit getSourceUnit(File rootDir, String repoUri) throws IOException {
        SourceUnit unit = new SourceUnit();
        unit.Name = getName();
        unit.Dir = PathUtil.relativizeCwd(rootDir.toPath());
        Collection<File> files = getFileCollector(rootDir, repoUri).collect(rootDir);
        unit.Files = files.stream().map(File::toString).collect(Collectors.toList());
        return unit;
    }

    /**
     * Helper method to construct ANTLR lexer and parser.
     * @param support language support object, used to instantiate streams
     * @param sourceFile source of characters to feed to lexer
     * @param lexerClass lexer's implementation class
     * @param parserClass parser's implementation class
     * @param errorListener error listener to use
     * @return structure that holds constructed lexer and parser objects.
     * Lexer is bound to character stream made from source file; parser is bound to lexer
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected static GrammarConfiguration createGrammarConfiguration(LanguageBase support,
                                                                     File sourceFile,
                                                                     Class<? extends Lexer> lexerClass,
                                                                     Class<? extends Parser> parserClass,
                                                                     ANTLRErrorListener errorListener)
            throws IOException,
            NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        CharStream stream = support.getCharStream(sourceFile);
        Constructor<? extends Lexer> lexerConstructor = lexerClass.getConstructor(CharStream.class);

        Lexer lexer = lexerConstructor.newInstance(stream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        TokenStream tokens = new CommonTokenStream(lexer);
        Constructor<? extends Parser> parserConstructor = parserClass.getConstructor(TokenStream.class);
        Parser parser = parserConstructor.newInstance(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        GrammarConfiguration configuration = new GrammarConfiguration();
        configuration.lexer = lexer;
        configuration.parser = parser;
        return configuration;
    }

    /**
     * @param sourceFile input file
     * @return character stream to read data from
     * @throws IOException
     */
    protected CharStream getCharStream(File sourceFile) throws IOException {
        return new ANTLRFileStream(sourceFile.getPath());
    }

    /**
     * Holder for parser and lexer objects
     */
    protected static class GrammarConfiguration {
        public Lexer lexer;
        public Parser parser;
    }
}
