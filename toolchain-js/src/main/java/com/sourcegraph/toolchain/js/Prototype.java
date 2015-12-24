package com.sourcegraph.toolchain.js;

import java.util.HashMap;

/**
 * Created by iisaev on 18.12.15.
 */
public class Prototype extends SemaElement {

    /**
     * elements of the prototype
     */
    private HashMap<String, SemaElement> fields;

    /**
     * add element to this prototype
     * @param elem element to be added
     */
    public void addField(SemaElement elem) {
        fields.put(elem.getName(), elem);
    }

    /**
     * search for element with certain name
     * @param name name of the element to be looked for
     */
    public SemaElement getField(String name) {
        return fields.get(name);
    }

    public Prototype(String name) {
        super(name);
        fields = new HashMap<>();
        fields.put("prototype", this);
    }

    public boolean isPrototype() {
        return true;
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isVariable() {
        return false;
    }
}
