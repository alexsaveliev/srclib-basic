package com.sourcegraph.toolchain.language;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Purpose of file collector is to collect all files from a given root directory that belong to specific language.
 * For example, Objective C language support may collect all .m, .mm, and .h files
 */
public interface FileCollector {

    /**
     * @param rootDir root directory
     * @return all files that belong to specific language
     * @throws IOException
     */
    Collection<File> collect(File rootDir) throws IOException;
}
