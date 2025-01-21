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
import nl.knaw.dans.dvingest.core.yaml.tasklog.CompletableItemWithCount;
import nl.knaw.dans.dvingest.core.yaml.tasklog.EditFilesLog;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Embargo;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.util.PathIterator;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j

/**
 * Edits files in a dataset in Dataverse, based on the edit-files.yml file, which has been read and parsed into an EditFiles object.
 */
public class FilesEditor {
    private final UUID depositId;
    private final Path dataDir;
    private final EditFiles editFiles;
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;
    @Getter(AccessLevel.PACKAGE) // for testing
    private final FilesInDatasetCache filesInDatasetCache;
    private final EditFilesLog editFilesLog;

    private String pid;

    public FilesEditor(@NonNull UUID depositId, @NonNull Path dataDir, @NonNull EditFiles editFiles, @NonNull DataverseService dataverseService,
        @NonNull UtilityServices utilityServices, @NonNull EditFilesLog editFilesLog) {
        this.depositId = depositId;
        this.dataDir = dataDir;
        this.editFiles = editFiles;
        this.dataverseService = dataverseService;
        this.utilityServices = utilityServices;
        this.editFilesLog = editFilesLog;
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
                editFilesLog.completeAll();
                return;
            }
        }

        log.debug("Start editing files for deposit {}", depositId);
        this.pid = pid;
        filesInDatasetCache.downloadFromDataset(pid);
        if (editFiles != null) {
            deleteFiles();
            replaceFiles();
            addUnrestrictedFiles();
            addRestrictedFiles();
            addUnrestrictedFilesIndividually();
            addRestrictedFilesIndividually();
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
        if (editFilesLog.getDeleteFiles().isCompleted()) {
            log.debug("Task deleteFiles already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getDeleteFiles().isEmpty()) {
            log.debug("No files to delete for deposit {}", depositId);
        }
        else {
            log.debug("Start deleting {} files for deposit {}", depositId, editFiles.getDeleteFiles().size());
            int numberDeleted = editFilesLog.getDeleteFiles().getNumberCompleted();
            if (numberDeleted > 0) {
                log.debug("Resuming deleting files from number {}", numberDeleted);
            }
            for (int i = numberDeleted; i < editFiles.getDeleteFiles().size(); i++) {
                var filepath = editFiles.getDeleteFiles().get(i);
                log.debug("Deleting file: {}", filepath);
                var fileToDelete = filesInDatasetCache.get(filepath);
                if (fileToDelete == null) {
                    throw new IllegalArgumentException("File to delete not found in dataset: " + filepath);
                }
                dataverseService.deleteFile(fileToDelete.getDataFile().getId());
                filesInDatasetCache.remove(filepath);
                editFilesLog.getDeleteFiles().setNumberCompleted(++numberDeleted);
            }
            log.debug("End deleting files for deposit {}", depositId);
        }
        editFilesLog.getDeleteFiles().setCompleted(true);
    }

    private void replaceFiles() throws IOException {
        if (editFilesLog.getReplaceFiles().isCompleted()) {
            log.debug("Task replaceFiles already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getReplaceFiles().isEmpty()) {
            log.debug("No files to replace for deposit {}", depositId);
        }
        else {
            log.debug("Start replacing {} files for deposit {}", depositId, editFiles.getReplaceFiles().size());
            int numberReplaced = editFilesLog.getReplaceFiles().getNumberCompleted();
            if (numberReplaced > 0) {
                log.debug("Resuming replacing files from number {}", numberReplaced);
            }
            for (int i = numberReplaced; i < editFiles.getReplaceFiles().size(); i++) {
                var filepath = editFiles.getReplaceFiles().get(i);
                log.debug("Replacing file: {}", filepath);
                var fileToReplace = filesInDatasetCache.get(filepath);
                if (fileToReplace == null) {
                    throw new IllegalArgumentException("File to replace not found in dataset: " + filepath);
                }
                utilityServices.wrapIfZipFile(dataDir.resolve(filepath)).ifPresentOrElse(
                    zipFile -> {
                        replaceFileOrThrow(pid, fileToReplace, zipFile);
                        FileUtils.deleteQuietly(zipFile.toFile());
                    },
                    () -> {
                        var fileToUpload = dataDir.resolve(filepath);
                        replaceFileOrThrow(pid, fileToReplace, fileToUpload);
                    }
                );
                editFilesLog.getReplaceFiles().setNumberCompleted(++numberReplaced);
            }
            log.debug("End replacing files for deposit {}", depositId);
        }
        editFilesLog.getReplaceFiles().setCompleted(true);
    }

    private void replaceFileOrThrow(String pid, FileMeta fileMeta, Path fileToUpload) {
        try {
            dataverseService.replaceFile(pid, fileMeta, fileToUpload);
        }
        catch (IOException | DataverseException e) {
            throw new RuntimeException(e);
        }
    }

    public void addRestrictedFilesIndividually() throws IOException, DataverseException {
        if (editFilesLog.getAddRestrictedIndividually().isCompleted()) {
            log.debug("Task addRestrictedIndividually already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getAddRestrictedIndividually().isEmpty()) {
            log.debug("No restricted files to add individually for deposit {}", depositId);
        }
        else {
            addFilesIndividually(editFiles.getAddRestrictedIndividually(), true);
        }
        editFilesLog.getAddRestrictedIndividually().setCompleted(true);
    }

    public void addUnrestrictedFilesIndividually() throws IOException, DataverseException {
        if (editFilesLog.getAddUnrestrictedIndividually().isCompleted()) {
            log.debug("Task addUnrestrictedIndividually already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getAddUnrestrictedIndividually().isEmpty()) {
            log.debug("No unrestricted files to add individually for deposit {}", depositId);
        }
        else {
            addFilesIndividually(editFiles.getAddUnrestrictedIndividually(), false);
        }
        editFilesLog.getAddUnrestrictedIndividually().setCompleted(true);
    }

    public void addFilesIndividually(List<String> files, boolean restricted) throws IOException, DataverseException {
        log.debug("Start adding {} files individually for deposit {}, restrict = {}", editFiles.getAddRestrictedIndividually().size(), depositId, restricted);
        int numberAdded = editFilesLog.getAddRestrictedIndividually().getNumberCompleted();
        if (numberAdded > 0) {
            log.debug("Resuming adding files from number {}", numberAdded);
        }
        for (int i = numberAdded; i < files.size(); i++) {
            var filepath = files.get(i);
            log.debug("Adding file: {}", filepath);
            var fileMeta = new FileMeta();
            fileMeta.setRestricted(restricted);
            var realFilepath = filepath;
            // TODO: a bit confusing that autorenamedFiles is part of the cache, although the file looked up here has not been added to the dataset yet.
            if (filesInDatasetCache.getAutoRenamedFiles().containsKey(filepath)) {
                realFilepath = filesInDatasetCache.getAutoRenamedFiles().get(filepath);
            }
            var dataversePath = new DataversePath(realFilepath);
            fileMeta.setLabel(dataversePath.getLabel());
            fileMeta.setDirectoryLabel(dataversePath.getDirectoryLabel());
            var fileToUpload = dataDir.resolve(filepath);
            if (!Files.exists(fileToUpload)) {
                throw new IllegalArgumentException("File to add not found in bag: " + filepath);
            }
            var addedFileMeta = dataverseService.addFile(pid, fileToUpload, fileMeta);
            for (var fm : addedFileMeta.getFiles()) {
                filesInDatasetCache.put(fm);
            }
            if (restricted) {
                editFilesLog.getAddRestrictedIndividually().setNumberCompleted(++numberAdded);
            }
            else {
                editFilesLog.getAddUnrestrictedIndividually().setNumberCompleted(++numberAdded);
            }

        }
    }

    private void addRestrictedFiles() throws IOException, DataverseException {
        if (editFilesLog.getAddRestrictedFiles().isCompleted()) {
            log.debug("Task addRestrictedFiles already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getAddRestrictedFiles().isEmpty()) {
            log.debug("No restricted files to add for deposit {}", depositId);
        }
        else {
            log.debug("Start adding {} restricted files for deposit {}", editFiles.getAddRestrictedFiles().size(), depositId);
            var iterator = new PathIterator(
                IteratorUtils.skippingIterator(
                    editFiles.getAddRestrictedFiles().stream().map(dataDir::resolve).map(Path::toFile).iterator(),
                    editFilesLog.getAddRestrictedFiles().getNumberCompleted()));
            while (iterator.hasNext()) {
                uploadFileBatch(iterator, true, editFilesLog.getAddRestrictedFiles());
            }
            log.debug("End adding {} restricted files for deposit {}", iterator.getIteratedCount(), depositId);
        }
        editFilesLog.getAddRestrictedFiles().setCompleted(true);
    }

    private void addUnrestrictedFiles() throws IOException, DataverseException {
        if (editFilesLog.getAddUnrestrictedFiles().isCompleted()) {
            log.debug("Task addUnrestrictedFiles already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getAddUnrestrictedFiles().isEmpty()) {
            log.debug("No unrestricted files to add for deposit {}", depositId);
        }
        else {
            log.debug("Start adding {} unrestricted files for deposit {}", editFiles.getAddUnrestrictedFiles().size(), depositId);
            var iterator = new PathIterator(
                IteratorUtils.skippingIterator(
                    editFiles.getAddUnrestrictedFiles().stream().map(dataDir::resolve).map(Path::toFile).iterator(),
                    editFilesLog.getAddUnrestrictedFiles().getNumberCompleted()));
            while (iterator.hasNext()) {
                uploadFileBatch(iterator, false, editFilesLog.getAddUnrestrictedFiles());
            }
            log.debug("End uploading {} unrestricted files for deposit {}", iterator.getIteratedCount(), depositId);
        }
        editFilesLog.getAddUnrestrictedFiles().setCompleted(true);
    }

    private void uploadFileBatch(PathIterator iterator, boolean restrict, CompletableItemWithCount trackLog) throws IOException, DataverseException {
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
            log.debug("Uploaded {} files, ({} cumulative)", addedFileMetaList.getFiles().size(), iterator.getIteratedCount());
            trackLog.setNumberCompleted(trackLog.getNumberCompleted() + addedFileMetaList.getFiles().size());
            for (var fm : addedFileMetaList.getFiles()) {
                filesInDatasetCache.put(fm); // auto-rename is done by PathIteratorZipper
            }
        }
        finally {
            Files.deleteIfExists(tempZipFile);
        }
    }

    private void moveFiles() throws IOException, DataverseException {
        if (editFilesLog.getMoveFiles().isCompleted()) {
            log.debug("Task moveFiles already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getMoveFiles().isEmpty()) {
            log.debug("No files to move for deposit {}", depositId);
        } else {
            log.debug("Start moving {} files for deposit {}", editFiles.getMoveFiles().size(), depositId);
            int numberMoved = editFilesLog.getMoveFiles().getNumberCompleted();
            if (numberMoved > 0) {
                log.debug("Resuming moving files from number {}", numberMoved);
            }
            for (int i = numberMoved; i < editFiles.getMoveFiles().size(); i++) {
                var move = editFiles.getMoveFiles().get(i);
                var fileMeta = filesInDatasetCache.get(move.getFrom());
                fileMeta = filesInDatasetCache.createFileMetaForMovedFile(move.getTo(), fileMeta);
                dataverseService.updateFileMetadata(fileMeta.getDataFile().getId(), fileMeta);
                filesInDatasetCache.remove(move.getFrom());
                filesInDatasetCache.put(fileMeta); // auto-rename is done by getMovedFile
                editFilesLog.getMoveFiles().setNumberCompleted(++numberMoved);
            }
            log.debug("End moving files for deposit {}", depositId);
        }
        editFilesLog.getMoveFiles().setCompleted(true);
    }

    private void updateFileMetas() throws IOException, DataverseException {
        if (editFilesLog.getUpdateFileMetas().isCompleted()) {
            log.debug("Task updateFileMetas already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getUpdateFileMetas().isEmpty()) {
            log.debug("No file metadata to update for deposit {}", depositId);
            editFilesLog.getUpdateFileMetas().setCompleted(true);
        } else {
            log.debug("Start updating {} file metas for deposit {}", editFiles.getUpdateFileMetas().size(), depositId);
            int numberUpdated = editFilesLog.getUpdateFileMetas().getNumberCompleted();
            if (numberUpdated > 0) {
                log.debug("Resuming updating file metadata from number {}", numberUpdated);
            }
            for (int i = numberUpdated; i < editFiles.getUpdateFileMetas().size(); i++) {
                var fileMeta = editFiles.getUpdateFileMetas().get(i);
                var id = filesInDatasetCache.get(getPath(fileMeta)).getDataFile().getId();
                dataverseService.updateFileMetadata(id, fileMeta);
                editFilesLog.getUpdateFileMetas().setNumberCompleted(++numberUpdated);
            }
            log.debug("End updating file metadata for deposit {}", depositId);
        }
        editFilesLog.getUpdateFileMetas().setCompleted(true);
    }

    private String getPath(FileMeta file) {
        var dataversePath = new DataversePath(file.getDirectoryLabel(), file.getLabel());
        return dataversePath.toString();
    }

    private void addEmbargoes() throws IOException, DataverseException {
        if (editFilesLog.getAddEmbargoes().isCompleted()) {
            log.debug("Task addEmbargoes already completed for deposit {}", depositId);
            return;
        }
        if (editFiles.getAddEmbargoes().isEmpty()) {
            log.debug("No embargoes to add for deposit {}", depositId);
        } else {
            log.debug("Start adding {} embargoes for deposit {}", editFiles.getAddEmbargoes().size(), depositId);
            int numberOfEmbargoesAdded = editFilesLog.getAddEmbargoes().getNumberCompleted();
            if (numberOfEmbargoesAdded > 0) {
                log.debug("Resuming adding embargoes from number {}", numberOfEmbargoesAdded);
            }
            for (int i = numberOfEmbargoesAdded; i < editFiles.getAddEmbargoes().size(); i++) {
                var addEmbargo = editFiles.getAddEmbargoes().get(i);
                var embargo = new Embargo();
                embargo.setDateAvailable(addEmbargo.getDateAvailable());
                embargo.setReason(addEmbargo.getReason());
                var fileIds = addEmbargo.getFilePaths()
                    .stream()
                    .map(filesInDatasetCache::get)
                    .mapToInt(file -> file.getDataFile().getId()).toArray();
                embargo.setFileIds(fileIds);
                dataverseService.addEmbargo(pid, embargo);
                editFilesLog.getAddEmbargoes().setNumberCompleted(++numberOfEmbargoesAdded);
            }
            log.debug("End adding embargoes for deposit {}", depositId);
        }
        editFilesLog.getAddEmbargoes().setCompleted(true);
    }
}
