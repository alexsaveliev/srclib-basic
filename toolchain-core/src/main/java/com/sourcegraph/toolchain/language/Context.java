package com.sourcegraph.toolchain.language;

import java.util.Stack;

/**
 * Parse context. Keeps stack of scopes
 */
public class Context {

    private Stack<Scope> scopes;

    public Context() {
        scopes = new Stack<>();
        scopes.push(Scope.ROOT);
    }

    /**
     * Adds new scope into the stack
     * @param scope scope to add
     */
    public void enterScope(Scope scope) {
        scopes.push(scope);
    }

    /**
     * Removes scope from the stack
     * @return removed scope
     */
    public Scope exitScope() {
        return scopes.pop();
    }

    /**
     * @return current scope
     */
    public Scope currentScope() {
        return scopes.peek();
    }

    /**
     * @param separator separator character
     * @return path to current scope separated by given character excluding the root scope.
     * Example: Class.SubClass.Method.
     */
    public String getPath(char separator) {
        StringBuilder ret = new StringBuilder();
        for (Scope scope : scopes) {
            if (scope == Scope.ROOT) {
                continue;
            }
            ret.append(scope.getName()).append(separator);
        }
        return ret.toString();
    }
}
