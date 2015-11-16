package com.sourcegraph.toolchain.php.resolver;

import java.io.File;

public class PSR4ClassFileResolver extends AbstractPSRClassFileResolver {

    @Override
    protected File resolve(File directory, String prefix, String className) {
        File file = new File(directory, className.replace('\\', File.separatorChar) + ".php");
        return file.isFile() ? file: null;
    }
}
