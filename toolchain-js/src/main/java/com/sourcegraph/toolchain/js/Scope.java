package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;

import java.util.HashMap;

/**
 * Created by iisaev on 17.12.15.
 */
public class Scope {
    private HashMap<String, SemaElement> idents;
    private String name;

    public Scope(String name) {
        idents = new HashMap<String, SemaElement>();
        this.name = name;
    }

    public SemaElement find(String id) {
        return idents.get(id);
    }

    public void add(SemaElement e) {
        idents.put(e.getName(), e);
    }

    public String getName() {
        return name;
    }
}
