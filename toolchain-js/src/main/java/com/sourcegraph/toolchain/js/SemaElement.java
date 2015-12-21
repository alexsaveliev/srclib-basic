package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;

/**
 * Created by iisaev on 18.12.15.
 */
public abstract class SemaElement {
    private String name;
    private Def def;

    public String getName() {
        return name;
    }

    public SemaElement(String name) {
        this.name = name;
    }

    public void setDef(Def def) {
        this.def = def;
    }

    public Def getDef() {
        return this.def;
    }

    public abstract boolean isPrototype();

    public abstract boolean isMethod();

    public abstract boolean isVariable();
}
