package com.sourcegraph.toolchain.js;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by iisaev on 18.12.15.
 */
public class Method extends SemaElement {

    /**
     * list of method parameters
     */
    private List<Variable> params;

    /**
     * add parameter
     * @param p parameter to be added
     */
    public void addParam(Variable p) {
        params.add(p);
    }

    /**
     * get i-th parameter of the methos
     * @param i number of the parameter
     */
    public Variable getParam(int i) {
        return params.get(i);
    }

    public Method(String name) {
        super(name);
        params = new ArrayList<>();
    }

    public boolean isPrototype() {
        return false;
    }

    public boolean isMethod() {
        return true;
    }

    public boolean isVariable() {
        return false;
    }

}
