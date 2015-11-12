package com.sourcegraph.toolchain.application;

import com.beust.jcommander.Parameter;
import com.sourcegraph.toolchain.core.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class DepresolveCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepresolveCommand.class);

    @Parameter(names = {"--debug-unit-file"}, description = "The path to a source unit input file, which will be read as though it came from stdin. Used to mimic stdin when you can't actually pipe to stdin (e.g., in IntelliJ run configurations).")
    String debugUnitFile;

    /**
     * Main method
     */
    public void Execute() {
        JSONUtil.writeJSON(Collections.emptyList());
    }

}
