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
package nl.knaw.dans.dvingest.core.dansbag;

import nl.knaw.dans.dvingest.core.bagprocessor.DataversePath;
import nl.knaw.dans.dvingest.core.dansbag.deposit.FileInfo;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.lib.dataverse.model.file.Checksum;
import nl.knaw.dans.lib.dataverse.model.file.DataFile;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EditFilesComposerFixture {
    protected static final Instant inThePast = Instant.parse("2010-01-01T00:00:00Z");
    protected final Instant inTheFuture = Instant.now().plus(1, ChronoUnit.DAYS);
    protected EditFilesComposer editFilesComposer;

    /*
     * Helper methods to set things up.
     */
    protected FileInfo file(String path, String checksum, boolean restricted, String description, List<String> categories) {
        var fileMeta = new FileMeta();
        var dataversePath = new DataversePath(path);
        fileMeta.setLabel(dataversePath.getLabel());
        fileMeta.setDirectoryLabel(dataversePath.getDirectoryLabel());
        fileMeta.setRestrict(restricted);
        if (description != null) {
            fileMeta.setDescription(description);
        }
        if (categories != null) {
            fileMeta.setCategories(categories);
        }
        return new FileInfo(Path.of(path), checksum, false, fileMeta);
    }

    private FileInfo sanitizedFile(String path, String sanitizedPath, String checksum, boolean restricted, String description, List<String> categories) {
        var fileMeta = new FileMeta();
        var dataversePath = new DataversePath(sanitizedPath);
        fileMeta.setLabel(dataversePath.getLabel());
        fileMeta.setDirectoryLabel(dataversePath.getDirectoryLabel());
        fileMeta.setRestrict(restricted);
        if (description != null) {
            fileMeta.setDescription(description);
        }
        if (categories != null) {
            fileMeta.setCategories(categories);
        }
        return new FileInfo(Path.of(path), checksum, true, fileMeta);
    }

    protected FileInfo sanitizedFile(String path, String sanitizedPath, String checksum) {
        return sanitizedFile(path, sanitizedPath, checksum, false, null, null);
    }

    protected FileInfo file(String path, String checksum) {
        return file(path, checksum, false, null, null);
    }

    protected FileInfo file(String path, String checksum, boolean restricted) {
        return file(path, checksum, restricted, null, null);
    }

    protected FileInfo file(String path, String checksum, boolean restricted, String description) {
        return file(path, checksum, restricted, description, null);
    }

    protected void add(Map<Path, FileInfo> map, FileInfo fileInfo) {
        map.put(fileInfo.getPath(), fileInfo);
    }

    protected void assertEmptyFieldsExcept(EditFiles editFiles, String... except) {
        List<String> exceptList = List.of(except);
        if (!exceptList.contains("addRestrictedFiles")) {
            assertThat(editFiles.getAddRestrictedFiles()).as("expected addRestrictedFiles to remain empty").isEmpty();
        }
        if (!exceptList.contains("addUnrestrictedFiles")) {
            assertThat(editFiles.getAddUnrestrictedFiles()).as("expected addUnrestrictedFiles to remain empty").isEmpty();
        }
        if (!exceptList.contains("autoRenameFiles")) {
            assertThat(editFiles.getAutoRenameFiles()).as("expected autoRenameFiles to remain empty").isEmpty();
        }
        if (!exceptList.contains("updateFileMetas")) {
            assertThat(editFiles.getUpdateFileMetas()).as("expected updateFileMetas to remain empty").isEmpty();
        }
        if (!exceptList.contains("addEmbargoes")) {
            assertThat(editFiles.getAddEmbargoes()).as("expected addEmbargoes to remain empty").isEmpty();
        }
        if (!exceptList.contains("deleteFiles")) {
            assertThat(editFiles.getDeleteFiles()).as("expected deleteFiles to remain empty").isEmpty();
        }
        if (!exceptList.contains("moveFiles")) {
            assertThat(editFiles.getMoveFiles()).as("expected moveFiles to remain empty").isEmpty();
        }
    }

    protected FileMeta fileMeta(String path, String checksum) {
        return fileMeta(path, checksum, false);
    }

    protected FileMeta fileMeta(String path, String checksum, boolean restricted) {
        var fileMeta = new FileMeta();
        var dataversePath = new DataversePath(path);
        fileMeta.setLabel(dataversePath.getLabel());
        fileMeta.setDirectoryLabel(dataversePath.getDirectoryLabel());
        fileMeta.setRestrict(restricted);
        var dataFile = new DataFile();
        var cs = new Checksum();
        cs.setType("SHA-1");
        cs.setValue(checksum);
        dataFile.setChecksum(cs);
        dataFile.setFilename(dataversePath.getLabel());
        // No directoryLabel?
        fileMeta.setDataFile(dataFile);
        return fileMeta;
    }
}
