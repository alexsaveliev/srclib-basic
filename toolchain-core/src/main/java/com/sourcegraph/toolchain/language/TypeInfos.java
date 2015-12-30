package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TypeInfos<K, V> {

    private Map<String, TypeInfo<K, V>> infos;

    public TypeInfos() {
        infos = new HashMap<>();
    }

    public TypeInfo<K, V> getRoot() {
        return get(StringUtils.EMPTY);
    }

    public void setData(String name, K data) {
        get(name).setData(data);
    }

    public TypeInfo<K, V> get(String name) {
        TypeInfo<K, V> ret = infos.get(name);
        if (ret == null) {
            ret = new TypeInfo<>();
            infos.put(name, ret);
        }
        return ret;
    }

    public V getProperty(String typeName, String category, String propertyName) {
        return get(typeName).getProperty(category, propertyName);
    }

    public void setProperty(String typeName, String category, String propertyName, V value) {
        get(typeName).setProperty(category, propertyName, value);
    }

}