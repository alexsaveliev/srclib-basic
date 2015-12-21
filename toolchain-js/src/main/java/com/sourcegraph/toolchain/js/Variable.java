package com.sourcegraph.toolchain.js;

/**
 * Created by iisaev on 18.12.15.
 */
public class Variable extends SemaElement {

    public Variable(String name) {
        super(name);
    }

    public boolean isPrototype() {
        return false;
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isVariable() {
        return true;
    }
}
