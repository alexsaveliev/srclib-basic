package com.sourcegraph.toolchain.core.objects;

import com.google.gson.*;
import com.sourcegraph.toolchain.core.PathUtil;

import java.lang.reflect.Type;

/**
 * Definition object
 */
public class Def {

    /**
     * DefKey is the natural unique key for a def. It is stable
     * (subsequent runs of a grapher will emit the same defs with the same
     * DefKeys).
     */
    public DefKey defKey;

    /**
     * Kind is the kind of thing this definition is. This is
     * language-specific. Possible values include "type", "func",
     * "var", etc.
     */
    public String kind;

    /**
     * Name of the definition. This need not be unique.
     */
    public String name;

    /**
     * Source file
     */
    public String file;

    /**
     * Definition start
     */
    public int defStart;

    /**
     * Definition end
     */
    public int defEnd;

    /** Exported is whether this def is part of a source unit's
     * public API. For example, in Java a "public" field isExported.
     */
    public boolean exported;

    /**
     * Local is whether this def is local to a function or some
     * other inner scope. Local defs do *not* have module,
     * package, or file scope. For example, in Java a function's
     * args are Local, but fields with "private" scope are not
     * Local.
     */
    public boolean local;

    /**
     * Test is whether this def is defined in test code (as opposed to main
     * code). For example, definitions in Go *_test.go files have Test = true.
     */
    public boolean test;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Def def = (Def) o;

        // alexsaveliev: using only defKey to compare definitions because
        // there may be cases when two _files_ may want to define the same object.
        // for example, two C files may have main() function
        return !(defKey != null ? !defKey.equals(def.defKey) : def.defKey != null);

    }

    @Override
    public int hashCode() {
        // alexsaveliev: using only defKey to compare definitions because
        // there may be cases when two _files_ may want to define the same object.
        // for example, two C files may have main() function
        return defKey != null ? defKey.hashCode() : 0;
    }

    /**
     * JSON serialization rules for definition objects
     */
    public static class JSONSerializer implements JsonSerializer<Def> {

        @Override
        public JsonElement serialize(Def sym, Type arg1, JsonSerializationContext arg2) {
            JsonObject object = new JsonObject();

            if (sym.file != null) {
                object.add("File", new JsonPrimitive(PathUtil.relativizeCwd(sym.file)));
            }

            object.add("Name", new JsonPrimitive(sym.name));

            object.add("DefStart", new JsonPrimitive(sym.defStart));
            object.add("DefEnd", new JsonPrimitive(sym.defEnd));

            object.add("Kind", new JsonPrimitive(sym.kind));

            object.add("Exported", new JsonPrimitive(sym.exported));
            object.add("Local", new JsonPrimitive(sym.local));
            object.add("Test", new JsonPrimitive(sym.test));

            object.add("Path", new JsonPrimitive(sym.defKey.formatPath()));
            object.add("TreePath", new JsonPrimitive(sym.defKey.formatTreePath()));

            return object;
        }

    }
}
