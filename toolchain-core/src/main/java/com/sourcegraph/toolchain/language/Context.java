package com.sourcegraph.toolchain.language;

import java.util.Stack;

/**
 * Parse context. Keeps stack of scopes
 */
public class Context<E> {

    private Stack<Scope<E>> scopes;

    public Context() {
        scopes = new Stack<>();
        scopes.push(Scope.root());
    }

    /**
     * Adds new scope into the stack
     * @param scope scope to add
     */
    public void enterScope(Scope<E> scope) {
        scopes.push(scope);
    }

    /**
     * Removes scope from the stack
     * @return removed scope
     */
    public Scope<E> exitScope() {
        return scopes.pop();
    }

    /**
     * @return current scope
     */
    public Scope<E> currentScope() {
        return scopes.peek();
    }

    /**
     * @param separator separator character
     * @return path to current scope separated by given character excluding the root scope.
     * Example: Class.SubClass.Method.
     */
    public String getPrefix(char separator) {
        String path = getPath(separator);
        if (!path.isEmpty()) {
            path += separator;
        }
        return path;
    }

    /**
     * @param separator separator character
     * @param maxDepth maximum depth
     * @return path to given depth separated by given character excluding the root scope.
     * Example: Class.SubClass.Method.
     */
    public String getPrefix(char separator, int maxDepth) {
        String path = getPath(separator, maxDepth);
        if (!path.isEmpty()) {
            path += separator;
        }
        return path;
    }

    /**
     * @param separator separator character
     * @return path to current scope separated by given character excluding the root scope.
     * Example: Class.SubClass.Method
     */
    public String getPath(char separator) {
        return getPath(separator, scopes.size());
    }

    /**
     * @param separator separator character
     * @param maxDepth maximum depth
     * @return path to given depth separated by given character excluding the root scope.
     * Example: Class.SubClass.Method
     */
    public String getPath(char separator, int maxDepth) {
        StringBuilder ret = new StringBuilder();
        boolean empty = true;
        int currentDepth = 0;
        for (Scope scope : scopes) {
            if (scope.isRoot()) {
                continue;
            }
            currentDepth++;
            if (currentDepth == maxDepth) {
                break;
            }
            if (!empty) {
                ret.append(separator);
            } else {
                empty = false;
            }
            ret.append(scope.getName());
        }
        return ret.toString();
    }

    /**
     * Tries to resolve variable type
     * @param name variable name
     * @return variable type or null
     */
    public LookupResult<E> lookup(String name) {
        int index = scopes.size() - 1;
        while (index >= 0) {
            Scope<E> s = scopes.get(index);
            E value = s.get(name);
            if (value != null) {
                return new LookupResult<>(value, s, index);
            }
            index--;
        }
        return null;
    }
}
