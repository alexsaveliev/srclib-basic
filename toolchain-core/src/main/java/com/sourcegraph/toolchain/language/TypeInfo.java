package com.sourcegraph.toolchain.language;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TypeInfo<K, V> {

    private K data;

    private Map<String, Map<String, V>> props = new HashMap<>();

    public void setProperty(String category, String name, V data) {
        Map<String, V> categoryProps = props.get(category);
        if (categoryProps == null) {
            categoryProps = new HashMap<>();
            props.put(category, categoryProps);
        }
        categoryProps.put(name, data);
    }

    public V getProperty(String category, String name) {
        Map<String, V> categoryProps = props.get(category);
        if (categoryProps == null) {
            return null;
        }
        return categoryProps.get(name);
    }

    public Collection<String> getPropertyNames(String category) {
        Map<String, V> categoryProps = props.get(category);
        if (categoryProps == null) {
            return Collections.emptyList();
        }
        return categoryProps.keySet();
    }

    public K getData() {
        return data;
    }

    public TypeInfo<K, V> setData(K data) {
        this.data = data;
        return this;
    }
}