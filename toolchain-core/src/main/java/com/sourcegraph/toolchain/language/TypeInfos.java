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
        return safeGet(StringUtils.EMPTY);
    }

    public void setData(String name, K data) {
        safeGet(name).setData(data);
    }

    public TypeInfo<K, V> get(String name) {
        return infos.get(name);
    }

    public V getProperty(String typeName, String category, String propertyName) {
        return safeGet(typeName).getProperty(category, propertyName);
    }

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