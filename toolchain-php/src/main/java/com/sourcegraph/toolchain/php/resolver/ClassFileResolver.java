package com.sourcegraph.toolchain.php.resolver;

import java.io.File;

public interface ClassFileResolver {

    File resolve(String fullyQualifiedClassName);
}