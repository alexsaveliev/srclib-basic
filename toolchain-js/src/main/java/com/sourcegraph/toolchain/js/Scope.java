package com.sourcegraph.toolchain.js;

import java.util.HashMap;

/**
 * Created by iisaev on 17.12.15.
 */
public class Scope {

    private HashMap<String, SemaElement> idents;
    private String name;

    /**
     * @param name hierarchical name of the current scope
     */
    public Scope(String name) {
        idents = new HashMap<String, SemaElement>();
        this.name = name;
    }

    /**
     * searches for the certain identifier in the current scope
     * @param id name of the entity to be looked for
     * @return previously created semantic element, if the current scope contains one, null - otherwise
     */
    public SemaElement find(String id) {
        return idents.get(id);
    }

    /**
     * puts semantic element to the current scope
     * @param e element to be put
     */
    public void add(SemaElement e) {
        idents.put(e.getName(), e);
    }

    /**
     * sgetter for the name of the scope
     * @return hierarchical name of the scope
     */
    public String getName() {
        return name;
    }
}
