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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Embargo;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.util.PathIterator;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class FilesEditor {
    @NonNull
    private final UUID depositId;
    @NonNull
    private final Path dataDir;
    private final EditFiles editFiles;

    @NonNull
    private final DataverseService dataverseService;

    @NonNull
    private final UtilityServices utilityServices;

    private String pid;

    @Getter(AccessLevel.PACKAGE) // Getter for unit testing
    private final Map<String, FileMeta> filesInDataset = new java.util.HashMap<>();
    private boolean filesRetrieved = false;

    private Map<String, String> renameMap;

    public void editFiles(String pid) throws IOException, DataverseException {
        /*
         * TODO:
         *  Validations ?
         *  - replaceFiles must exist in bag (+ must be update-deposit)
         *  - addRestrictedFiles must exist in bag
         *  - updateFileMetas must exist in bag if first version deposit
         */
        if (editFiles == null) {
            if (isEmptyDir(dataDir)) {
                log.debug("No files to edit for deposit {}", depositId);
                return;
            }
        }

        log.debug("Start editing files for deposit {}", depositId);
        this.pid = pid;
        this.renameMap = getRenameMap();
        if (editFiles != null) {
            deleteFiles();
            replaceFiles();
            addRestrictedFiles();
        }
        addUnrestrictedFiles();
        if (editFiles != null) {
            moveFiles();
            updateFileMetas();
            addEmbargoes();
        }
        log.debug("End editing files for deposit {}", depositId);
    }

    private boolean isEmptyDir(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.findAny().isEmpty();
        }
    }

    private void deleteFiles() throws IOException, DataverseException {
        log.debug("Start deleting {} files for deposit {}", depositId, editFiles.getDeleteFiles().size());
        for (var file : editFiles.getDeleteFiles()) {
            log.debug("Deleting file: {}", file);
            var fileToDelete = filesInDataset().get(file);
            if (fileToDelete == null) {
                throw new IllegalArgumentException("File to delete not found in dataset: " + file);
            }
            dataverseService.deleteFile(fileToDelete.getDataFile().getId());
            filesInDataset.remove(file);
        }
        log.debug("End deleting files for deposit {}", depositId);
    }

    private void replaceFiles() throws IOException, DataverseException {
        log.debug("Start replacing {} files for deposit {}", depositId, editFiles.getReplaceFiles().size());
        for (var file : editFiles.getReplaceFiles()) {
            log.debug("Replacing file: {}", file);
            var fileMeta = filesInDataset().get(file);
            dataverseService.replaceFile(pid, fileMeta, dataDir.resolve(file));
        }
        log.debug("End replacing files for deposit {}", depositId);
    }

    private void addRestrictedFiles() throws IOException, DataverseException {
        log.debug("Start adding {} restricted files for deposit {}", editFiles.getAddRestrictedFiles().size(), depositId);
        var iterator = new PathIterator(getRestrictedFilesToUpload());
        while (iterator.hasNext()) {
            uploadFileBatch(iterator, true);
        }
        log.debug("End adding {} restricted files for deposit {}", iterator.getIteratedCount(), depositId);
    }

    private void addUnrestrictedFiles() throws IOException, DataverseException {
        log.debug("Start uploading files for deposit {}", depositId);
        var iterator = new PathIterator(getUnrestrictedFilesToUpload());
        while (iterator.hasNext()) {
            uploadFileBatch(iterator, false);
        }
        log.debug("End uploading {} unrestricted files for deposit {}", iterator.getIteratedCount(), depositId);
    }

    private Iterator<File> getUnrestrictedFilesToUpload() {
        return IteratorUtils.filteredIterator(
            FileUtils.iterateFiles(dataDir.toFile(), null, true),
            new FileUploadInclusionPredicate(editFiles, dataDir, false));
    }

    private Iterator<File> getRestrictedFilesToUpload() {
        return IteratorUtils.filteredIterator(
            FileUtils.iterateFiles(dataDir.toFile(), null, true),
            new FileUploadInclusionPredicate(editFiles, dataDir, true));
    }

    private Map<String, String> getRenameMap() {
        return editFiles.getRenameAtUploadFiles().stream()
            .map(rename -> Map.entry(rename.getFrom(), rename.getTo()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void uploadFileBatch(PathIterator iterator, boolean restrict) throws IOException, DataverseException {
        var tempZipFile = utilityServices.createTempZipFile();
        try {
            var zipFile = utilityServices.createPathIteratorZipperBuilder(renameMap)
                .rootDir(dataDir)
                .sourceIterator(iterator)
                .targetZipFile(tempZipFile)
                .build()
                .zip();
            var fileMeta = new FileMeta();
            fileMeta.setRestricted(restrict);
            log.debug("Start uploading zip file at {} for deposit {}", zipFile, depositId);
            var fileList = dataverseService.addFile(pid, zipFile, fileMeta);
            log.debug("Uploaded {} files, {} cumulative)", fileList.getFiles().size(), iterator.getIteratedCount());
            for (var file : fileList.getFiles()) {
                filesInDataset.put(getPath(file), file);
            }
        }
        finally {
            Files.deleteIfExists(tempZipFile);
        }
    }

    private void moveFiles() throws IOException, DataverseException {
        log.debug("Start moving files {} for deposit {}", editFiles.getMoveFiles().size(), depositId);
        for (var move : editFiles.getMoveFiles()) {
            var fileMeta = filesInDataset().get(move.getFrom());
            var dvToPath = new DataversePath(move.getTo());
            fileMeta.setDirectoryLabel(dvToPath.getDirectoryLabel());
            fileMeta.setLabel(dvToPath.getLabel());
            dataverseService.updateFileMetadata(fileMeta.getDataFile().getId(), fileMeta);
        }
        log.debug("End moving files for deposit {}", depositId);
    }

    private void updateFileMetas() throws IOException, DataverseException {
        log.debug("Start updating {} file metas for deposit {}", editFiles.getUpdateFileMetas().size(), depositId);
        for (var fileMeta : editFiles.getUpdateFileMetas()) {
            var id = filesInDataset().get(getPath(fileMeta)).getDataFile().getId();
            dataverseService.updateFileMetadata(id, fileMeta);
        }
        log.debug("End updating file metadata for deposit {}", depositId);
    }

    private Map<String, FileMeta> filesInDataset() throws IOException, DataverseException {
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
        var dataversePath = new DataversePath(file.getDirectoryLabel(), file.getLabel());
        return dataversePath.toString();
    }

    private void addEmbargoes() throws IOException, DataverseException {
        log.debug("Start adding {} embargoes for deposit {}", editFiles.getAddEmbargoes().size(), depositId);
        for (var addEmbargo : editFiles.getAddEmbargoes()) {
            var embargo = new Embargo();
            embargo.setDateAvailable(addEmbargo.getDateAvailable());
            embargo.setReason(addEmbargo.getReason());
            var fileIds = addEmbargo.getFilePaths()
                .stream()
                .map(this::renameFile)
                .map(this::throwIfFileNotInDataset)
                .map(filesInDataset::get)
                .mapToInt(file -> file.getDataFile().getId()).toArray();
            embargo.setFileIds(fileIds);
            dataverseService.addEmbargo(pid, embargo);
        }
        log.debug("End adding embargoes for deposit {}", depositId);
    }

    private String renameFile(String file) {
        return renameMap.getOrDefault(file, file);
    }

    private String throwIfFileNotInDataset(String file) {
        if (!filesInDataset.containsKey(file)) {
            throw new IllegalArgumentException("File not found in dataset: " + file);
        }
        return file;
    }
}
