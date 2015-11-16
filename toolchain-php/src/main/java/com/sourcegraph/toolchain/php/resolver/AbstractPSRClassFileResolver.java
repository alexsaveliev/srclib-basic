package com.sourcegraph.toolchain.php.resolver;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractPSRClassFileResolver implements ClassFileResolver {

    private Map<String, Collection<File>> prefixes = new HashMap<>();

    public void addNamespace(String prefix, String baseDirectory) {

        prefix = "\\" + StringUtils.strip(prefix, "\\") + '\\';

        Collection<File> directories = prefixes.get(prefix);
        if (directories == null) {
            directories = new ArrayList<>();
            prefixes.put(prefix, directories);
        }

        directories.add(new File(baseDirectory));
    }

    @Override
    public File resolve(String fullyQualifiedClassName) {

        String prefix = fullyQualifiedClassName;

        int pos;
        while ((pos = prefix.lastIndexOf('\\')) != -1) {

            prefix = fullyQualifiedClassName.substring(0, pos + 1);
            String relativeClass = fullyQualifiedClassName.substring(pos + 1);

            File file = resolvePrefix(prefix, relativeClass);
            if (file != null) {
                return file;
            }
            prefix = StringUtils.stripEnd(prefix, "\\");
        }
        // fallback
        return resolvePrefix(StringUtils.EMPTY, fullyQualifiedClassName);
    }

    private File resolvePrefix(String prefix, String relativeClass) {
        Collection<File> directories = prefixes.get(prefix);
        if (directories != null) {
            for (File directory : directories) {
                File file = this.resolve(directory, prefix, relativeClass);
                if (file != null) {
                    return file;
                }
            }
        }
        return null;
    }

    protected abstract File resolve(File directory, String prefix, String className);
}
