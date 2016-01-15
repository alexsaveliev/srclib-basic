package com.sourcegraph.toolchain.cpp;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Function lookup result. May contain exact match and zero or more matching candidates
 */
class FunctionLookupResult {

    /**
     * Exact match
     */
    Function exact;

    /**
     * Candidates
     */
    Collection<Function> candidates = new LinkedList<>();

    boolean isEmpty() {
        return exact == null && candidates.isEmpty();
    }
}
