package com.sourcegraph.toolchain.language;

/**
 * Contains information about object (variable, function etc)
 */
public class ObjectInfo {

    private String type;
    private String prefix;

    public ObjectInfo(String type) {
        this(type, null);
    }

    public ObjectInfo(String type, String prefix) {
        this.type = type;
        this.prefix = prefix;
    }

    public String getType() {
        return type;
    }

    public String getPrefix() {
        return prefix;
    }

}