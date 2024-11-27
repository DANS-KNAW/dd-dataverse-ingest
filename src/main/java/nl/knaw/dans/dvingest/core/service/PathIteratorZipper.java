/*
 * Copyright (C) 2024 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.dvingest.core.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

// TODO: move to dans-java-utils
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathIteratorZipper {
    @NonNull
    private final Path rootDir;
    @NonNull
    private final Iterator<Path> sourceIterator;
    @NonNull
    private final Path targetZipFile;
    @Builder.Default
    private final boolean overwrite = true;
    @Builder.Default
    private final boolean compress = false;
    @Builder.Default
    private final int maxNumberOfFiles = Integer.MAX_VALUE;
    @Builder.Default
    private final long maxNumberOfBytes = 1073741824; // 1 GB

    public Path zip() throws IOException {
        if (overwrite && Files.exists(targetZipFile)) {
            Files.delete(targetZipFile);
        }
        else {
            if (Files.exists(targetZipFile)) {
                throw new IOException("Target zip file already exists: " + targetZipFile);
            }
        }

        try (OutputStream outputStream = Files.newOutputStream(targetZipFile)) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {
                int fileCount = 0;
                long byteCount = 0;
                while (sourceIterator.hasNext() && fileCount < maxNumberOfFiles && byteCount < maxNumberOfBytes) {
                    Path path = sourceIterator.next();
                    if (Files.isRegularFile(path)) {
                        try {
                            addFileToZipStream(zipArchiveOutputStream, path);
                            fileCount++;
                            byteCount += Files.size(path);
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return targetZipFile;
        }
    }

    private void addFileToZipStream(ZipArchiveOutputStream zipArchiveOutputStream, Path fileToZip) throws IOException {
        if (!fileToZip.startsWith(rootDir)) {
            throw new IllegalArgumentException("File to zip is not a descendant of root directory: " + fileToZip);
        }
        String entryName = rootDir.relativize(fileToZip).toString();
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(fileToZip, entryName);
        zipArchiveEntry.setMethod(compress ? ZipArchiveEntry.STORED : ZipArchiveEntry.DEFLATED);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        try (InputStream fileInputStream = Files.newInputStream(fileToZip)) {
            IOUtils.copy(fileInputStream, zipArchiveOutputStream);
            zipArchiveOutputStream.closeArchiveEntry();
        }
    }
}