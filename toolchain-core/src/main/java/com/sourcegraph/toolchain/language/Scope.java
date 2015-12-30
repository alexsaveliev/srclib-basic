package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Current scope, holds variables info
 */
public class Scope<E> {

    private String name;
    private String prefix;
    private boolean root;

    private Map<String, E> items = new HashMap<>();

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
    public E get(String name) {
        return items.get(name);
    }

    /**
     * Registers new variable
     * @param name variable name
     * @param value associated data
     */
    public void put(String name, E value) {
        items.put(name, value);
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

    public boolean isRoot() {
        return root;
    }

    static <E> Scope<E> root() {
        Scope<E> ret = new Scope<E>(StringUtils.EMPTY, StringUtils.EMPTY);
        ret.root = true;
        return ret;
    }
    @Override
    public String toString() {
        return getName();
    }


}
