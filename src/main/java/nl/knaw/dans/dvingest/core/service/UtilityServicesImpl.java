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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import nl.knaw.dans.lib.util.PathIteratorZipper;
import nl.knaw.dans.lib.util.PathIteratorZipper.PathIteratorZipperBuilder;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Builder
public class UtilityServicesImpl implements UtilityServices {
    private final Set<String> needToBeZipWrapped = Set.of(
        "application/zip",
        "application/zipped-shapefile",
        "application/fits-gzipped"
    );
    private final Path tempDir;
    private final int maxNumberOfFilesPerUpload;
    private final long maxUploadSize;
    private final Tika tika = new Tika();

    @Override
    public Path createTempZipFile() throws IOException {
        if (tempDir == null) {
            return Files.createTempFile("dvingest", ".zip");
        }
        else {
            return Files.createTempFile(tempDir, "dvingest", ".zip");
        }
    }

    @Override
    public PathIteratorZipperBuilder createPathIteratorZipperBuilder() {
        return createPathIteratorZipperBuilder(Map.of());
    }

    @Override
    public PathIteratorZipperBuilder createPathIteratorZipperBuilder(Map<String, String> renameMap) {
        return PathIteratorZipper.builder()
            .renameMap(renameMap)
            .maxNumberOfFiles(maxNumberOfFilesPerUpload)
            .maxNumberOfBytes(maxUploadSize);
    }

    @Override
    public Optional<Path> wrapIfZipFile(Path path) throws IOException {
        if (needsToBeWrapped(path)) {
            var filename = Optional.ofNullable(path.getFileName())
                .map(Path::toString)
                .orElse("");

            var randomName = String.format("zip-wrapped-%s-%s.zip",
                filename, UUID.randomUUID());

            var tempFile = tempDir.resolve(randomName);

            try (var zip = new ZipFile(tempFile.toFile())) {
                zip.addFile(path.toFile(), zipWithoutCompressing());
            }

            return Optional.of(tempFile);
        }
        else {
            return Optional.empty();
        }
   }

    private ZipParameters zipWithoutCompressing() {
        var params = new ZipParameters();
        params.setCompressionMethod(CompressionMethod.STORE);
        return params;
    }

    private boolean needsToBeWrapped(Path path) throws IOException {
        var endsWithZip = Optional.ofNullable(path.getFileName())
            .map(Path::toString)
            .map(x -> x.endsWith(".zip"))
            .orElse(false);

        log.debug("Checking if path {} needs to be wrapped: endsWithZip={}", path, endsWithZip);

        return endsWithZip || needToBeZipWrapped.contains(getMimeType(path));
    }

    private String getMimeType(Path path) throws IOException {
        String result = tika.detect(path);
        log.debug("MimeType of path {} is {}", path, result);
        return result;
    }

}
