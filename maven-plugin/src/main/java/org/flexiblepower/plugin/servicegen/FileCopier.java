/*-
 * #%L
 * dEF-Pi service creation maven plugin
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.plugin.servicegen;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * FileCopier
 *
 * @version 0.1
 * @since Aug 2, 2019
 */
public class FileCopier implements FileVisitor<Path> {

    private final Path mainRaml;
    private Path targetDir;
    private final Path source;

    public FileCopier(final Path mainRaml, final Path sourceFile) {
        this.mainRaml = mainRaml;
        this.targetDir = mainRaml.getParent();
        this.source = sourceFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
     */
    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
        if (dir.getFileName().toString().contains(".")) {
            return FileVisitResult.CONTINUE;
        }
        if (!Files.newDirectoryStream(this.targetDir).iterator().hasNext()) {
            Files.delete(this.targetDir);
        }
        this.targetDir = this.targetDir.getParent();
        return FileVisitResult.CONTINUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        if (dir.getFileName().toString().contains(".")
                || dir.getFileName().equals(this.source.getParent().getFileName())) {
            return FileVisitResult.CONTINUE;
        }
        this.targetDir = this.targetDir.resolve(dir.getFileName()).normalize();
        Files.createDirectories(this.targetDir);
        return FileVisitResult.CONTINUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        if (file.equals(this.mainRaml)) {
            return FileVisitResult.CONTINUE;
        }
        if (file.toString().endsWith(".raml")) {
            Files.copy(file, this.targetDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

}
