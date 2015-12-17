package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;

import java.util.HashMap;

/**
 * Created by iisaev on 17.12.15.
 */
public class Scope {
    private HashMap<String, Def> idents;

    public Scope() {
        idents = new HashMap<String, Def>();
    }

    public Def find(String id) {
        return idents.get(id);
    }

    public void add(Def d) {
        idents.put(d.name, d);
    }
}
