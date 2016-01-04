package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds class name - class info pairs
 * @param <K> type of class info general description
 * @param <V> type of class info attributes
 */
public class TypeInfos<K, V> {

    private Map<String, TypeInfo<K, V>> infos;

    public TypeInfos() {
        infos = new HashMap<>();
    }

    /**
     * @return root type info (global scope). Always returns not-null object
     */
    public TypeInfo<K, V> getRoot() {
        return safeGet(StringUtils.EMPTY);
    }

    /**
     * Sets general description for object with the given name
     * @param name object's name
     * @param data data (description) to set
     */
    public void setData(String name, K data) {
        safeGet(name).setData(data);
    }

    /**
     * @param name object's name
     * @return object info
     */
    public TypeInfo<K, V> get(String name) {
        return infos.get(name);
    }

    /**
     *
     * @param typeName type name
     * @param category category name
     * @param propertyName property name
     * @return data associated with the given name in the given category of type name's properties
     */
    public V getProperty(String typeName, String category, String propertyName) {
        return safeGet(typeName).getProperty(category, propertyName);
    }

    /**
     * Sets data associated with the given name in the given category of type name's properties
     * @param typeName type name
     * @param category category name
     * @param propertyName property name
     * @param value data to set
     */
    public void setProperty(String typeName, String category, String propertyName, V value) {
        safeGet(typeName).setProperty(category, propertyName, value);
    }

    private TypeInfo<K, V> safeGet(String name) {
        TypeInfo<K, V> ret = infos.get(name);
        if (ret == null) {
            ret = new TypeInfo<>();
            infos.put(name, ret);
        }
        return ret;
    }


}