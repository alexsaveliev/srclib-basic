package com.sourcegraph.toolchain.language;

import java.util.HashMap;
import java.util.Map;

/**
 * Current scope, holds variables info
 */
public class Scope {

    static final Scope ROOT = new Scope(null);

    private String name;
    private Map<String, String> items = new HashMap<>();

    public Scope(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return getName();
    }


}
