package com.sourcegraph.toolchain.php.resolver;

import java.io.File;

public class PSR0ClassFileResolver extends AbstractPSRClassFileResolver {

    @Override
    protected File resolve(File directory, String prefix, String className) {
        File file = new File(new File(directory, prefix.replace('\\', File.separatorChar)),
                className.replace('_', File.separatorChar) + ".php");
        return file.isFile() ? file: null;
    }
}
