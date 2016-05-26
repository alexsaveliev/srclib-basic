package com.sourcegraph.toolchain.language;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Extension-based file collector. Allows to declare list of supported file extensions and optional list of
 * include/exclude directories.
 * For example,
 * - for Objective C configuration may be : extensions: .h, .m, .mm
 * - for PHP (with Composer) configuration may be : extensions: .php, exclude: vendor
 * Collection rules are the following: Matching file should contain one of registered extension, be located inside
 * include directories (or they should be empty), and not be in the exclude directories.
 */
public class ExtensionBasedFileCollector implements FileCollector {

    private Collection<String> extensions = new HashSet<>();
    private Collection<String> blockerExtensions = new HashSet<>();
    private Collection<String> includes = new HashSet<>();
    private Collection<String> excludes = new HashSet<>();

    /**
     * Registers one or more extension
     * @param extension extension to register
     * @return this
     */
    public ExtensionBasedFileCollector extension(String... extension) {
        Collections.addAll(extensions, extension);
        return this;
    }

    /**
     * Registers one or more extension as the blocker one(s). If collector encounters file with a given extension
     * it stops processing and returns no files. For example, .m is a blocker for C++ code (Objective-C)
     * @param extension extension to register as the blocking(s)
     * @return this
     */
    public ExtensionBasedFileCollector blockerExtension(String... extension) {
        Collections.addAll(blockerExtensions, extension);
        return this;
    }

    /**
     * Adds directories to include section. Files located outside include directories will not be collected
     * (unless includes section is empty)
     * @param directories directories to add
     * @return this
     */
    public ExtensionBasedFileCollector include(String... directories) {
        Collections.addAll(includes, directories);
        return this;
    }

    /**
     * Adds directories to exclude section. Files located inside exclude directories will not be collected
     * @param directories directories to add
     * @return this
     */
    public ExtensionBasedFileCollector exclude(String... directories) {
        Collections.addAll(excludes, directories);
        return this;
    }

    @Override
    public Collection<File> collect(File rootDir) throws IOException {
        Collection<File> files = new ArrayList<>();
        Files.walkFileTree(rootDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.toString();
                for (String extension : extensions) {
                    if (fileName.endsWith(extension)) {
                        files.add(file.toFile());
                        break;
                    }
                }
                for (String extension : blockerExtensions) {
                    if (fileName.endsWith(extension)) {
                        files.clear();
                        return FileVisitResult.TERMINATE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                String rel = rootDir.toPath().relativize(dir).toString();
                if (!includes.isEmpty()) {
                    boolean matches = false;
                    for (String include : includes) {
                        if (rel.startsWith(include)) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                if (!excludes.isEmpty()) {
                    for (String exclude : excludes) {
                        if (rel.startsWith(exclude)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}