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
package nl.knaw.dans.dvingest.core.bagprocessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.PathIterator;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class FilesEditor {
    private final UUID depositId;
    private final Path dataDir;
    private final EditFiles editFiles;
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;

    private String pid;
    private final Map<String, FileMeta> filesInDataset = new java.util.HashMap<>();
    private boolean filesRetrieved = false;

    public void editFiles(String pid) throws IOException, DataverseException {
        log.debug("Start editing files for deposit {}", depositId);
        this.pid = pid;
        if (editFiles != null) {
            deleteFiles();
            replaceFiles();
            addRestrictedFiles();
        }
        addUnrestrictedFiles();
        if (editFiles != null) {
            moveFiles();
            updateFileMetas();
        }
        log.debug("End editing files for deposit {}", depositId);
    }

    private void deleteFiles() throws IOException, DataverseException {
        log.debug("Start deleting {} files for deposit {}", depositId, editFiles.getDeleteFiles().size());
        for (var file : editFiles.getDeleteFiles()) {
            log.debug("Deleting file: {}", file);
            var fileToDelete = getFilesInDataset().get(file);
            dataverseService.deleteFile(fileToDelete.getDataFile().getId());
            filesInDataset.remove(file);
        }
        log.debug("End deleting files for deposit {}", depositId);
    }

    private void replaceFiles() throws IOException, DataverseException {
        log.debug("Start replacing {} files for deposit {}", depositId, editFiles.getReplaceFiles().size());
        for (var file : editFiles.getReplaceFiles()) {
            log.debug("Replacing file: {}", file);
            var fileMeta = getFilesInDataset().get(file);
            dataverseService.replaceFile(pid, fileMeta, dataDir.resolve(file));
        }
        log.debug("End replacing files for deposit {}", depositId);
    }

    private void addRestrictedFiles() throws IOException, DataverseException {
        log.debug("Start adding restricted {} files for deposit {}", depositId, editFiles.getAddRestrictedFiles().size());
        var iterator = new PathIterator(getRestrictedFilesToUpload());
        while (iterator.hasNext()) {
            uploadFileBatch(iterator, true);
        }
        log.debug("End adding restricted files for deposit {}", depositId);
    }

    private void addUnrestrictedFiles() throws IOException, DataverseException {
        log.debug("Start uploading files for deposit {}", depositId);
        var iterator = new PathIterator(getUnrestrictedFilesToUpload());
        while (iterator.hasNext()) {
            uploadFileBatch(iterator, false);
        }
        log.debug("End uploading {} unrestricted files for deposit {}", depositId, iterator.getIteratedCount());
    }

    private Iterator<File> getUnrestrictedFilesToUpload() {
        return IteratorUtils.filteredIterator(
            FileUtils.iterateFiles(dataDir.toFile(), null, true),
            path -> editFiles == null ||
                !editFiles.getReplaceFiles().contains(dataDir.relativize(path.toPath()).toString())
                    && !editFiles.getAddRestrictedFiles().contains(dataDir.relativize(path.toPath()).toString()));
    }

    private Iterator<File> getRestrictedFilesToUpload() {
        return IteratorUtils.filteredIterator(
            FileUtils.iterateFiles(dataDir.toFile(), null, true),
            // Skip files that have been replaced in the edit steps
            path -> editFiles == null ||
                !editFiles.getReplaceFiles().contains(dataDir.relativize(path.toPath()).toString())
                    && editFiles.getAddRestrictedFiles().contains(dataDir.relativize(path.toPath()).toString()));
    }

    private void uploadFileBatch(PathIterator iterator, boolean restrict) throws IOException, DataverseException {
        var tempZipFile = utilityServices.createTempZipFile();
        try {
            var zipFile = utilityServices.createPathIteratorZipperBuilder()
                .rootDir(dataDir)
                .sourceIterator(iterator)
                .targetZipFile(tempZipFile)
                .build()
                .zip();
            var fileMeta = new FileMeta();
            fileMeta.setRestricted(restrict);
            var fileLIst = dataverseService.addFile(pid, zipFile, fileMeta);
            log.debug("Uploaded {} files (cumulative)", iterator.getIteratedCount());
            for (var file : fileLIst.getFiles()) {
                filesInDataset.put(getPath(file), file);
            }
        }
        finally {
            Files.deleteIfExists(tempZipFile);
        }
    }

    private void moveFiles() throws IOException, DataverseException {
        log.debug("Start moving files {} for deposit {}", depositId, editFiles.getMoveFiles().size());
        for (var move : editFiles.getMoveFiles()) {
            var fileMeta = getFilesInDataset().get(move.getFrom());
            fileMeta.setDirectoryLabel(getDirectoryLabel(move.getTo()));
            fileMeta.setLabel(getFileName(move.getTo()));
            dataverseService.updateFileMetadata(fileMeta.getDataFile().getId(), fileMeta);
        }
        log.debug("End moving files for deposit {}", depositId);
    }

    private void updateFileMetas() throws IOException, DataverseException {
        log.debug("Start updating {} file metas for deposit {}", depositId, editFiles.getUpdateFileMetas().size());
        for (var fileMeta : editFiles.getUpdateFileMetas()) {
            var id = getFilesInDataset().get(getPath(fileMeta)).getDataFile().getId();
            dataverseService.updateFileMetadata(id, fileMeta);
        }
        log.debug("End updating file metadata for deposit {}", depositId);
    }

    private String getDirectoryLabel(String path) {
        int lastIndex = path.lastIndexOf('/');
        return lastIndex == -1 ? "" : path.substring(0, lastIndex);
    }

    private String getFileName(String path) {
        int lastIndex = path.lastIndexOf('/');
        return path.substring(lastIndex + 1);
    }

    private Map<String, FileMeta> getFilesInDataset() throws IOException, DataverseException {
        if (!filesRetrieved) {
            log.debug("Start getting files in dataset for deposit {}", depositId);
            var files = dataverseService.getFiles(pid);
            for (var file : files) {
                filesInDataset.put(getPath(file), file);
            }
            filesRetrieved = true;
            log.debug("End getting files in dataset for deposit {}", depositId);
        }
        else {
            log.debug("Files in dataset already retrieved for deposit {}", depositId);
        }
        return filesInDataset;
    }

    private String getPath(FileMeta file) {
        if (file.getDirectoryLabel() != null) {
            return file.getDirectoryLabel() + "/" + file.getLabel();
        }
        return file.getLabel();
    }
}
