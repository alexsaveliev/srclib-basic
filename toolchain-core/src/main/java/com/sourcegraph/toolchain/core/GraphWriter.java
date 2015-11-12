package com.sourcegraph.toolchain.core;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.Ref;

/**
 * This interface is responsible for collecting and writing references and definitions produced by grapher
 */
public interface GraphWriter {

    /**
     * Writes reference
     * @param ref reference to write
     */
    void writeRef(Ref ref);

    /**
     * Writes definition
     * @param def definition to write
     */
    void writeDef(Def def);

    /**
     * Flush underlying streams
     */
    void flush();
}
