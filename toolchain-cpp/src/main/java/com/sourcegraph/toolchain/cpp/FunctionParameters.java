package com.sourcegraph.toolchain.cpp;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Function parameters
 */
class FunctionParameters {

    /**
     * List of function parameters
     */
    Collection<FunctionParameter> params = new LinkedList<>();

    /**
     * @return display representation (int foo, char *bar, ...)
     */
    String getRepresentation() {
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (FunctionParameter param : params) {
            if (!first) {
                ret.append(", ");
            } else {
                first = false;
            }
            ret.append(param.repr);
        }
        return ret.toString();
    }

    /**
     * @return signature _,_,_ where each parameter is marked by underscore
     */
    String getSignature() {
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (FunctionParameter param : params) {
            if (!first) {
                ret.append(',');
            } else {
                first = false;
            }
            ret.append(param.signature);
        }
        return ret.toString();
    }

}
