package com.sourcegraph.toolchain.language;

import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.SourceUnit;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/** Programming language support.
 */
public interface Language {

    /**
     *
     * @return language name, for example: php, objective-c. Names are transformed to lowercase
     */
    String getName();

    /**
     * Collects source units in given directory
     * @param rootDir root directory to collect source units in
     * @param repoUri repository URI
     * @return collection of found source units (may be null)
     * @throws IOException
     */
    Collection<SourceUnit> getSourceUnits(File rootDir, String repoUri) throws IOException;

    /**
     * Sets source unit to process. Called before graphing
     * @param unit source unit to process
     */
    void setSourceUnit(SourceUnit unit);

    /**
     * Sets writer to write refs and defs to. Called before graphing
     * @param writer writer to use
     */
    void setGraphWriter(GraphWriter writer);

    /**
     * Graphs current source units, expects data to be written to given writer
     */
    void graph();

    /**
     * Attemps to resolve given definition key. For example, we may construct definition key as @looks-like-def@foo()
     * which language may resolve to class:foo() if possible (e.g. using name => defkey map). Toolchain asks to resolve
     * all references marked as "candidate" when all definitions and refs were emitted and removes all candidate
     * refs that weren't resolved
     * @param source source key to resolve
     * @return resolved key or null if resolution failed.
     */
    DefKey resolve(DefKey source);
}
