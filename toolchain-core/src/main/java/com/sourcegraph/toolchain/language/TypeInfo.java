package com.sourcegraph.toolchain.language;

import java.util.HashMap;
import java.util.Map;

public class TypeInfo<E> {

    private Map<String, Map<String, E>> props = new HashMap<>();

    public void addProperty(String category, String name, E data) {
        Map<String, E> categoryProps = props.get(category);
        if (categoryProps == null) {
            categoryProps = new HashMap<>();
            props.put(category, categoryProps);
        }
        categoryProps.put(name, data);
    }

    public E getProperty(String category, String name) {
        Map<String, E> categoryProps = props.get(category);
        if (categoryProps == null) {
            return null;
        }
        return categoryProps.get(name);
    }
}