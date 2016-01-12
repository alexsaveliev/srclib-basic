package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;

/**
 * Created by iisaev on 18.12.15.
 */
public abstract class SemaElement {
    /**
     * name of the element
     */
    private String name;
    /**
     * def that corresponds to this element
     */
    private Def def;

    /**
     * getter for the element name
     */
    public String getName() {
        return name;
    }

    public SemaElement(String name) {
        this.name = name;
    }

    /**
     * setter for the def
     */
    public void setDef(Def def) {
        this.def = def;
    }

    /**
     * getter for the def
     */
    public Def getDef() {
        return this.def;
    }

    public abstract boolean isPrototype();

    public abstract boolean isMethod();

    public abstract boolean isVariable();
}
