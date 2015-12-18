package com.sourcegraph.toolchain.application;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.sourcegraph.toolchain.core.GraphData;
import com.sourcegraph.toolchain.core.JSONUtil;
import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.core.objects.SourceUnit;
import com.sourcegraph.toolchain.language.Language;
import com.sourcegraph.toolchain.language.LanguageRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GraphCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphCommand.class);

    @Parameter(names = {"--debug-unit-file"}, description = "The path to a source unit input file, which will be read as though it came from stdin. Used to mimic stdin when you can't actually pipe to stdin (e.g., in IntelliJ run configurations).")
    String debugUnitFile;

    /**
     * The Source Unit that is read in from STDIN. Defined here, so that it can be
     * accessed within the anonymous classes below.
     */
    public static SourceUnit unit;

    /**
     * Main method
     */
    @SuppressWarnings("unchecked")
    public void Execute() {

        try {
            Reader r;
            if (!StringUtils.isEmpty(debugUnitFile)) {
                LOGGER.debug("Reading source unit JSON data from {}", debugUnitFile);
                r = Files.newBufferedReader(FileSystems.getDefault().getPath(debugUnitFile));
            } else {
                r = new InputStreamReader(System.in);
            }
            unit = new Gson().fromJson(r, SourceUnit.class);
            r.close();
        } catch (IOException e) {
            LOGGER.error("Failed to read source unit data", e);
            System.exit(1);
        }
        LOGGER.info("Building graph for {} unit {}", unit.Type, unit.Name);

        GraphData writer = new GraphData();

        Language language = LanguageRegistry.getInstance().get(unit.Type);
        if (language == null) {
            LOGGER.error("Found no support for {}", unit.Type);
            System.exit(1);
        }

        try {
            LOGGER.debug("Starting graph collection");
            language.setSourceUnit(unit);
            language.setGraphWriter(writer);
            language.graph();
            LOGGER.debug("Graph collection complete");
            writer.flush();
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred while building graph", e);
            System.exit(1);
        }

        JSONUtil.writeJSON(normalize(language, writer));
    }

    private Graph normalize(Language language, GraphData data) {

        Map<DefKey, Collection<DefKey>> resolutions = new HashMap<>();
        Graph g = new Graph();
        g.Defs = data.getDefs();
        Collection<Ref> refs = data.getRefs();
        for (Ref ref : refs) {
            if (ref.candidate) {
                Collection<DefKey> adjusted = resolutions.get(ref.defKey);
                if (adjusted == null) {
                    adjusted = language.resolve(ref.defKey);
                    if (adjusted == null) {
                        continue;
                    }
                    resolutions.put(ref.defKey, adjusted);
                }
                LOGGER.info("{} candidates in {} at {}-{}", adjusted.size(), ref.file, ref.start, ref.end);
                for (DefKey resolvedKey : adjusted) {
                    Ref r = new Ref(ref);
                    LOGGER.info("Candidate {} {}", r.file, resolvedKey.toString());
                    r.defKey = resolvedKey;
                    g.Refs.add(r);
                }
                continue;
            }
            g.Refs.add(ref);
        }
        return g;
    }

    private static class Graph {
        Collection<Def> Defs = new ArrayList<>();
        Collection<Ref> Refs = new ArrayList<>();
    }
}
