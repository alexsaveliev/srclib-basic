package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TypeInfos<K, V> {

    private Map<String, TypeInfo<K, V>> infos;

    public TypeInfos() {
        infos = new HashMap<>();
        put(StringUtils.EMPTY, new TypeInfo<>());
    }

    public TypeInfo<K, V> getRoot() {
        return infos.get(StringUtils.EMPTY);
    }

    public TypeInfo<K, V> get(String name) {
        return infos.get(name);
    }

    public void put(String name, TypeInfo<K, V> info) {
        infos.put(name, info);
    }

}