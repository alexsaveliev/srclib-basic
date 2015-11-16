package com.sourcegraph.toolchain.php.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class CompoundClassFileResolver implements ClassFileResolver {

    private Collection<ClassFileResolver> resolvers;

    public CompoundClassFileResolver() {
        this.resolvers = new ArrayList<>();
    }

    public void addResolver(ClassFileResolver resolver) {
        this.resolvers.add(resolver);

    }
    @Override
    public File resolve(String fullyQualifiedClassName) {
        for (ClassFileResolver resolver : resolvers) {
            File ret = resolver.resolve(fullyQualifiedClassName);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }
}
