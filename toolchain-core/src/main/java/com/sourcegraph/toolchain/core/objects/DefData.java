package com.sourcegraph.toolchain.core.objects;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * Definition data, used to format definitions and to store extra information if needed
 */
public class DefData extends HashMap<String, Object> {

    public static final String SEPARATOR_SPACE = " ";
    public static final String SEPARATOR_EMPTY = StringUtils.EMPTY;

    private static final String NAME        = "Name";
    private static final String KEYWORD     = "Keyword";
    private static final String TYPE        = "Type";
    private static final String KIND        = "Kind";
    private static final String SEPARATOR   = "Separator";

    public DefData() {
        super();
    }

    public String getName() {
        return (String) get(NAME);
    }

    public void setName(String name) {
        put(NAME, name);
    }

    /*
     * Type is the type of the def s, if s is not itself a type. If s is
     * itself a type, then Type returns its underlying type.
     *
     * Outputs:
     *
     *   TYPE OF s          RESULT
     *   ------------   -----------------------------------------------------------------
     *   named type     the named type's name
     *   primitive      the primitive's name
     *   function       `(arg1, arg2, ..., argN)` with language-specific type annotations
     *   package        empty
     *   anon. type     the leading keyword (or similar) of the anonymous type definition
     *
     * These rules are not strictly defined or enforced. Language toolchains
     * should freely bend the rules (after noting important exceptions here) to
     * produce sensible output.
    */
    public String getType() {
        return (String) get(TYPE);
    }

    public void setType(String type) {
        put(TYPE, type);
    }

    /**
     * NameAndTypeSeparator is the string that should be inserted between the
     * def's name and type. This is typically empty for functions (so that
     * they are formatted with the left paren immediately following the name,
     * like `F(a)`) and a single space for other defs (e.g., `MyVar string`).
     */
    public String getNameAndTypeSeparator() {
        return (String) get(SEPARATOR);
    }

    public void setNameAndTypeSeparator(String separator) {
        put(SEPARATOR, separator);
    }

    /**
     * DefKeyword is the language keyword used to define the def (e.g.,
     * 'class', 'type', 'func').
     */
    public String getKeyword() {
        return (String) get(KEYWORD);
    }

    public void setKeyword(String keyword) {
        put(KEYWORD, keyword);
    }

    /**
     * 	Kind is the language-specific kind of this def (e.g., 'package', 'field', 'CommonJS module').
     */
    public String getKind(String kind) {
        return (String) get(KIND);
    }

    public void setKind(String kind) {
        put(KIND, kind);
    }


}
