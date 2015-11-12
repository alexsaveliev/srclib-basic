package com.sourcegraph.toolchain.php;

import java.util.Collection;
import java.util.HashSet;

class ClassInfo {

    String className;

    Collection<String> extendsClasses = new HashSet<>();
    Collection<String> implementsInterfaces = new HashSet<>();
    Collection<String> usesTraits = new HashSet<>();

    Collection<String> definesMethods = new HashSet<>();
    Collection<String> implementsMethods = new HashSet<>();

    Collection<String> definesConstants = new HashSet<>();
}