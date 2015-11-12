package com.sourcegraph.toolchain.core;

import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Implementation of graph writer that collects references and definitions and then writes them as JSON
 */
public class GraphData implements GraphWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphData.class);

    private final Map<Def, Def> defs = new LinkedHashMap<>();
    private final Collection<Ref> refs = new LinkedHashSet<>();

    @Override
    public void writeRef(Ref r) {
        refs.add(r);
    }

    @Override
    public void writeDef(Def s) {
        Def prev = defs.put(s, s);
        if (prev != null) {
            LOGGER.warn("{} already defined in {} at {}:{}, redefinition attempt in {} at {}:{}",
                    prev.defKey.getPath(),
                    prev.file,
                    prev.defStart,
                    prev.defEnd,
                    s.file,
                    s.defStart,
                    s.defEnd);
        }
    }

    @Override
    public void flush() {
    }

    public Collection<Def> getDefs() {
        return defs.keySet();
    }

    public Collection<Ref> getRefs() {
        return refs;
    }
}
