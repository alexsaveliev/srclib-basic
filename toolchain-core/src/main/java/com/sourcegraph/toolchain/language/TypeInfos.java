package com.sourcegraph.toolchain.language;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TypeInfos<E> {

    private Map<String, TypeInfo<E>> infos;

    public TypeInfos() {
        infos = new HashMap<>();
        put(StringUtils.EMPTY, new TypeInfo<>());
    }
    public TypeInfo<E> get(String name) {
        return infos.get(name);
    }

    public void put(String name, TypeInfo<E> info) {
        infos.put(name, info);
    }

}