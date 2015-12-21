package com.sourcegraph.toolchain.js;

import com.sourcegraph.toolchain.core.objects.Def;

import java.util.Stack;

/**
 * Created by iisaev on 17.12.15.
 */
public class Context {

    private Stack<Scope> scopes;

    private boolean protoDecl;

    private Method curMethod;

    private SemaElement resolved;

    private Prototype curProtoDecl;

    private Prototype proto;

    private int anonc;

    public String makeAnonScope(String base) {
        String res =  base + "@" + anonc+ "@";
        anonc++;
        return res;
    }

    public void setPrototype(Prototype p) {
        proto = p;
    }

    public void unsetPrototype() {
        proto = null;
    }

    public Prototype getPrototype() {
        return proto;
    }

    public void setCurProtoDecl(Prototype p) {
        curProtoDecl = p;
    }

    public Prototype getCurProtoDecl() {
        return curProtoDecl;
    }

    public Context() {
        scopes = new Stack<Scope>();
        protoDecl = false;
        pushScope(""); //global scope
    }

    public String getName() {
        return scopes.peek().getName();
    }

    public void pushScope(String name) {
        Scope s = new Scope(name);
        scopes.push(s);
    }

    public void popScope() {
        scopes.pop();
    }

    public void addToCurrentScope(SemaElement e) {
        Scope s = scopes.peek();
        s.add(e);
    }

    public void setProtoDecl() {
        protoDecl = true;
    }

    public void unsetProtoDecl() {
        protoDecl = false;
    }

    public boolean isProtoDecl() {
        return protoDecl;
    }

    public void setCurMethod(Method m) {
        curMethod = m;
    }

    public Method getCurMethod() {
        return curMethod;
    }

    public void setResolved(SemaElement e) {
        resolved = e;
    }

    public SemaElement getResolved() {
        return resolved;
    }

    SemaElement find(String id) {
        for (Scope s : scopes) {
            SemaElement e = s.find(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }
}
