package com.sourcegraph.toolchain.core.objects;


import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * SourceUnit represents a source unit expected by srclib.
 */
public class SourceUnit {

    /**
     * Source unit name
     */
    public String Name;

    /**
     * Source unit type
     */
    public String Type;

    /**
     * List of files that produce source units
     */
    public Collection<String> Files = new LinkedList<>();

    /**
     * Source unit directory
     */
    public String Dir;

    /**
     * Source unit raw data
     */
    public Map<String, Object> Data = new HashMap<>();

    public SourceUnit() {

    }

    @Override
    public int hashCode() {
        return Name == null ? 0 : Name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof SourceUnit)) {
            return false;
        }
        return StringUtils.equals(Name, ((SourceUnit) o).Name);
    }
}
