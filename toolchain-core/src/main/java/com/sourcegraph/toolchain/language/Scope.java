package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Parse scope.
 * For example, may denote class, function, or block
 */
public class Scope<E> {

    /**
     * Scope name
     */
    private String name;
    /**
     * Scope prefix, e.g. foo.bar.baz.
     * Together with the name makes path to scope
     */
    private String prefix;
    /**
     * Indicates that current scope is a root one (global variables)
     */
    private boolean root;

    /**
     * Scope items, for example variables, functions, etc
     */
    private Map<String, E> items = new HashMap<>();

    /**
     * Blocks counter, used to distinguish unnamed blocks ({}) inside current scope
     */
    private int counter;

    /**
     * Makes new scope with the given name
     * @param name scope name
     */
    public Scope(String name) {
        this(name, StringUtils.EMPTY);
    }

    /**
     * Makes new scope with the given name and prefix
     * @param name scope name
     * @param prefix scope prefix
     */
    public Scope(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    /**
     * @param name item key
     * @return item associated with the key
     */
    public E get(String name) {
        return items.get(name);
    }

    /**
     * Registers new item
     * @param name item key
     * @param value associated item data
     */
    public void put(String name, E value) {
        items.put(name, value);
    }

    /**
     * @return scope name
     */
    public String getName() {
        return name;
    }

    /**
     * @return scope prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return scope path (prefix + name)
     */
    public String getPath() {
        return prefix + name;
    }

    /**
     *
     * @param id identifier
     * @param separator separator char
     * @return path to current scope + separator + identifier
     */
    public String getPathTo(String id, char separator) {
        String ret = getPath();
        if (!ret.isEmpty()) {
            ret += separator;
        }
        return ret + id;
    }

    /**
     * @return true is current scope is the root one (global vars)
     */
    public boolean isRoot() {
        return root;
    }

    /**
     * @return next ID that may be used to distinguish unnamed blocks ({}) in the current scope
     */
    public String nextId() {
        return String.valueOf(++counter);
    }

    /**
     * @return new root scope
     */
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
