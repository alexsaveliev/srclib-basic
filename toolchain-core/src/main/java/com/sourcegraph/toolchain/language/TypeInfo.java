package com.sourcegraph.toolchain.language;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds information about some object (or class, or whatever).
 * Information is split to general object description and key-value pairs - properties
 * @param <K> general information's type (object description's type)
 * @param <V> property's type
 */
public class TypeInfo<K, V> {

    private K data;

    private Map<String, Map<String, V>> props = new HashMap<>();

    /**
     * Puts pair name-value to given category. Category might be "functions", "variables", etc
     *
     * @param category property's category
     * @param name     property's name
     * @param data     property value
     */
    public void setProperty(String category, String name, V data) {
        Map<String, V> categoryProps = props.get(category);
        if (categoryProps == null) {
            categoryProps = new HashMap<>();
            props.put(category, categoryProps);
        }
        categoryProps.put(name, data);
    }

    /**
     * @param category property's category
     * @param name     property's name
     * @return property's value in a given category for a given property name.
     * for example: type of "foo()" in object's "functions" category
     */
    public V getProperty(String category, String name) {
        Map<String, V> categoryProps = props.get(category);
        if (categoryProps == null) {
            return null;
        }
        return categoryProps.get(name);
    }

    /**
     * @return all categories, e..g "functions", "variables", ...
     */
    public Collection<String> getCategories() {
        return props.keySet();
    }

    /**
     * @param category property's category
     * @return all property names in the given category, e.g. all object "functions" or "variables"
     */
    public Collection<String> getProperties(String category) {
        Map<String, V> categoryProps = props.get(category);
        if (categoryProps == null) {
            return Collections.emptyList();
        }
        return categoryProps.keySet();
    }

    /**
     * @return object's general description
     */
    public K getData() {
        return data;
    }

    /**
     * @param data object's general description to set
     * @return this
     */
    public TypeInfo<K, V> setData(K data) {
        this.data = data;
        return this;
    }
}