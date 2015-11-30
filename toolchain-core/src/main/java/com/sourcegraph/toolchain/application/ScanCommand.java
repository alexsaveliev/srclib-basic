package com.sourcegraph.toolchain.application;

import com.beust.jcommander.Parameter;
import com.sourcegraph.toolchain.core.JSONUtil;
import com.sourcegraph.toolchain.core.PathUtil;
import com.sourcegraph.toolchain.core.objects.SourceUnit;
import com.sourcegraph.toolchain.language.LanguageRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ScanCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanCommand.class);

    @Parameter(names = {"--repo"}, description = "The URI of the repository that contains the directory tree being scanned")
    String repoURI;

    @Parameter(names = {"--subdir"}, description = "The path of the current directory (in which the scanner is run), relative to the root directory of the repository being scanned (this is typically the root, \".\", as it is most useful to scan the entire repository)")
    String subdir;

    /**
     * Main method
     */
    public void Execute() {

        try {
            if (repoURI == null) {
                repoURI = StringUtils.EMPTY;
            }
            if (subdir == null) {
                subdir = ".";
            }
            Collection<SourceUnit> units = LanguageRegistry.getInstance().getSourceUnits(PathUtil.CWD.toFile(), repoURI);
            normalize(units, repoURI);
            JSONUtil.writeJSON(units);
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred while collecting source units", e);
            System.exit(1);
        }
    }

    /**
     * Normalizes source units produces by scan command (sorts, relativizes file paths etc)
     * @param units source units to normalize
     */
    @SuppressWarnings("unchecked")
    private static void normalize(Collection<SourceUnit> units, String repoUri) {

        for (SourceUnit unit : units) {
            unit.Dir = PathUtil.relativizeCwd(unit.Dir);
            unit.Repo = repoUri;
            List<String> files = new ArrayList<>();
            for (String file : unit.Files) {
                Path p = Paths.get(file).toAbsolutePath();
                if (p.startsWith(PathUtil.CWD)) {
                    files.add(PathUtil.relativizeCwd(p));
                } else {
                    LOGGER.warn("Excluding {} from source files because it located outside of current directory", p);
                }
            }
            files.sort(String::compareTo);
            unit.Files = files;
        }
    }
}
