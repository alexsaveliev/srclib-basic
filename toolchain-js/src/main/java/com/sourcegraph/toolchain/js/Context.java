package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;

import java.util.Stack;

/**
 * Created by iisaev on 17.12.15.
 */
public class Context {

    private Stack<Scope> scopes;

    public Context() {
        scopes = new Stack<Scope>();
    }

    public void pushScope() {
        Scope s = new Scope();
        scopes.push(s);
    }

    public void popScope() {
        scopes.pop();
    }

    public void addToCurrentScope(Def d) {
        Scope s = scopes.peek();
        s.add(d);
    }

    Def find(String id) {
        for (Scope s : scopes) {
            Def def = s.find(id);
            if (def != null) {
                return def;
            }
        }
        return null;
    }
}
