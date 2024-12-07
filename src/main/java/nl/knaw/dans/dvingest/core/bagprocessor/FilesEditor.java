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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.FromTo;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j

public class FilesEditor {
    private final UUID depositId;
    private final Path dataDir;
    private final EditFiles editFiles;
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;
    @Getter(AccessLevel.PACKAGE) // for testing
    private final FilesInDatasetCache filesInDatasetCache;

    private String pid;

    public FilesEditor(@NonNull UUID depositId, @NonNull Path dataDir, @NonNull EditFiles editFiles, @NonNull DataverseService dataverseService,
        @NonNull UtilityServices utilityServices) {
        this.depositId = depositId;
        this.dataDir = dataDir;
        this.editFiles = editFiles;
        this.dataverseService = dataverseService;
        this.utilityServices = utilityServices;
        this.filesInDatasetCache = new FilesInDatasetCache(dataverseService, getRenameMap(editFiles.getAutoRenameFiles()));
    }

    private static Map<String, String> getRenameMap(List<FromTo> renames) {
        return renames.stream()
            .map(rename -> Map.entry(rename.getFrom(), rename.getTo()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

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
        filesInDatasetCache.downloadFromDataset(pid);
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
        for (var filepath : editFiles.getDeleteFiles()) {
            log.debug("Deleting file: {}", filepath);
            var fileToDelete = filesInDatasetCache.get(filepath);
            if (fileToDelete == null) {
                throw new IllegalArgumentException("File to delete not found in dataset: " + filepath);
            }
            dataverseService.deleteFile(fileToDelete.getDataFile().getId());
            filesInDatasetCache.remove(filepath);
        }
        log.debug("End deleting files for deposit {}", depositId);
    }

    private void replaceFiles() throws IOException, DataverseException {
        log.debug("Start replacing {} files for deposit {}", depositId, editFiles.getReplaceFiles().size());
        for (var filepath : editFiles.getReplaceFiles()) {
            log.debug("Replacing file: {}", filepath);
            var fileMeta = filesInDatasetCache.get(filepath);
            dataverseService.replaceFile(pid, fileMeta, dataDir.resolve(filepath));
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

    private void uploadFileBatch(PathIterator iterator, boolean restrict) throws IOException, DataverseException {
        var tempZipFile = utilityServices.createTempZipFile();
        try {
            var zipFile = utilityServices.createPathIteratorZipperBuilder(filesInDatasetCache.getAutoRenamedFiles())
                .rootDir(dataDir)
                .sourceIterator(iterator)
                .targetZipFile(tempZipFile)
                .build()
                .zip();
            var fileMeta = new FileMeta();
            fileMeta.setRestricted(restrict);
            log.debug("Start uploading zip file at {} for deposit {}", zipFile, depositId);
            var addedFileMetaList = dataverseService.addFile(pid, zipFile, fileMeta);
            log.debug("Uploaded {} files, {} cumulative)", addedFileMetaList.getFiles().size(), iterator.getIteratedCount());
            for (var fm : addedFileMetaList.getFiles()) {
                filesInDatasetCache.put(fm); // auto-rename is done by PathIteratorZipper
            }
        }
        finally {
            Files.deleteIfExists(tempZipFile);
        }
    }

    private void moveFiles() throws IOException, DataverseException {
        log.debug("Start moving files {} for deposit {}", editFiles.getMoveFiles().size(), depositId);
        for (var move : editFiles.getMoveFiles()) {
            var fileMeta = filesInDatasetCache.get(move.getFrom());
            fileMeta = filesInDatasetCache.createFileMetaForMovedFile(move.getTo(), fileMeta);
            dataverseService.updateFileMetadata(fileMeta.getDataFile().getId(), fileMeta);
            filesInDatasetCache.remove(move.getFrom());
            filesInDatasetCache.put(fileMeta); // auto-rename is done by getMovedFile
        }
        log.debug("End moving files for deposit {}", depositId);
    }

    private void updateFileMetas() throws IOException, DataverseException {
        log.debug("Start updating {} file metas for deposit {}", editFiles.getUpdateFileMetas().size(), depositId);
        for (var fileMeta : editFiles.getUpdateFileMetas()) {
            var id = filesInDatasetCache.get(getPath(fileMeta)).getDataFile().getId();
            dataverseService.updateFileMetadata(id, fileMeta);
        }
        log.debug("End updating file metadata for deposit {}", depositId);
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
                .map(filesInDatasetCache::get)
                .mapToInt(file -> file.getDataFile().getId()).toArray();
            embargo.setFileIds(fileIds);
            dataverseService.addEmbargo(pid, embargo);
        }
        log.debug("End adding embargoes for deposit {}", depositId);
    }
}
