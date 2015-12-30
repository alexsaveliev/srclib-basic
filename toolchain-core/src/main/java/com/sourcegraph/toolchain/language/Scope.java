package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Current scope, holds variables info
 */
public class Scope {

    static final Scope ROOT = new Scope(StringUtils.EMPTY);

    private String name;
    private String prefix;

    private Map<String, String> items = new HashMap<>();

    public Scope(String name) {
        this(name, StringUtils.EMPTY);
    }

    public Scope(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    /**
     * @param name variable name
     * @return variable type
     */
    public String get(String name) {
        return items.get(name);
    }

    /**
     * Registers new variable
     * @param name variable name
     * @param type variable type
     */
    public void put(String name, String type) {
        items.put(name, type);
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPath() {
        return prefix + name;
    }

    public String getPathTo(String id, char separator) {
        String ret = getPath();
        if (!ret.isEmpty()) {
            ret += separator;
        }
        return ret + id;
    }

    @Override
    public String toString() {
        return getName();
    }


}
