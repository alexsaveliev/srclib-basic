package com.sourcegraph.toolchain.js;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

import java.util.Stack;

/**
 * Created by iisaev on 17.12.15.
 */
public class Context {

    /**
     * hierarchy of the scopes in the current point of analysis
     */
    private Stack<Scope> scopes;

    private Method curMethod;

    /**
     * holds the previously resolved semantic element for member access resolution
     * (consider as a stub)
     */
    private SemaElement resolved;

    /**
     * holds the semantic info for current prototype declaration
     * (while analysing prototype declaration)
     */
    private Prototype curProtoDecl;

    /**
     * holds the semantic info for current prototype, when prototype is accessed through
     * reserved 'prototype' property
     * (consider as a stub)
     */
    private Prototype proto;

    /**
     * counter for creating unique names for anonymous scopes
     */
    private int anonc;

    /**
     * holds the parser generated token for
     * property assingment handling in semantic analysis
     */
    private Token propertyToken;

    /**
     * setter for property assignment token
     */
    public void setPropertyToken(Token t) {
        propertyToken = t;
    }

    /**
     * getter for property assignment token
     */
    public Token getPropertyToken() {
        return propertyToken;
    }


    /**
     * generate new unique name for anonymouse scope
     * @param base the name for enclosing scope
     * @return new unique name for anonymous scope
     */
    public String makeAnonScope(String base) {
        String res =  base + "@" + anonc+ "@";
        anonc++;
        return res;
    }

    /**
     * setter for prototype info
     * (when prototype is accessed through reserved 'prototype' property)
     */
    public void setPrototype(Prototype p) {
        proto = p;
    }

    /**
     * getter for prototype info
     * (when prototype is accessed through reserved 'prototype' property)
     */
    public Prototype getPrototype() {
        return proto;
    }

    /**
     * setter for prototype info in prototype constructor
     */
    public void setCurProtoDecl(Prototype p) {
        curProtoDecl = p;
    }

    /**
     * getter for prototype info in prototype constructor
     */
    public Prototype getCurProtoDecl() {
        return curProtoDecl;
    }

    public Context() {
        scopes = new Stack<Scope>();
        pushScope(StringUtils.EMPTY); //global scope
    }

    /**
     * get current scope name
     * @return current scope name
     */
    public String getName() {
        return scopes.peek().getName();
    }

    /**
     * adds a new scope to scope hierarchy
     * @param name name of the new scope
     */
    public void pushScope(String name) {
        Scope s = new Scope(name);
        scopes.push(s);
    }

    /**
     * remove a top scope from scope hierarchy
     */
    public void popScope() {
        scopes.pop();
    }

    /**
     * puts an element to current scope
     */
    public void addToCurrentScope(SemaElement e) {
        Scope s = scopes.peek();
        s.add(e);
    }

    public void setCurMethod(Method m) {
        curMethod = m;
    }

    public Method getCurMethod() {
        return curMethod;
    }

    /**
     * setter for resolved field
     * (member access resolution)
     */
    public void setResolved(SemaElement e) {
        resolved = e;
    }

    /**
     * getter for resolved field
     * (member access resolution)
     */
    public SemaElement getResolved() {
        return resolved;
    }

    /**
     * iteratively searches for the semantic element in scope hierarchy
     * @param id name of the element to be looked for
     * @return semantic element if found, null - otherwise
     */
    public SemaElement find(String id) {
        for (Scope s : scopes) {
            SemaElement e = s.find(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }
}
