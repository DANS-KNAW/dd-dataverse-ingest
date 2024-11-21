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
package nl.knaw.dans.dvingest.core;

import io.dropwizard.configuration.ConfigurationException;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.PathIterator;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditMetadata;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.dvingest.core.yaml.UpdateState;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class BagProcessor {
    private final UUID depositId;
    private final Path dataDir;
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;
    private final Dataset dataset;
    private final EditFiles editFiles;
    private final EditMetadata editMetadata;
    private final EditPermissions editPermissions;
    private final UpdateState updateState;

    // Only retrieve the file list once, because Dataverse is slow in building it up for large numbers of files.
    private final Map<String, FileMeta> filesInDataset = new HashMap<>();
    private boolean filesRetrieved = false;

    private String pid;

    public BagProcessor(UUID depositId, DepositBag bag, DataverseService dataverseService, UtilityServices utilityServices) throws IOException, ConfigurationException {
        this.depositId = depositId;
        this.dataDir = bag.getDataDir();
        this.dataverseService = dataverseService;
        this.utilityServices = utilityServices;

        this.dataset = bag.getDatasetMetadata();
        this.editFiles = bag.getEditFiles();
        this.editMetadata = bag.getEditMetadata();
        this.editPermissions = bag.getEditPermissions();
        this.updateState = bag.getUpdateState();
    }

    public String run(String targetPid) throws IOException, DataverseException {
        if (targetPid == null) {
            if (dataset == null) {
                throw new IllegalArgumentException("Must have dataset metadata to create a new dataset.");
            }
            pid = createNewDataset();
        }
        else {
            pid = targetPid;
            updateDatasetMetadata();
        }
        editFiles();
        editMetadata();
        editPermissions();
        updateState();
        return pid;
    }

    private String createNewDataset() throws IOException, DataverseException {
        log.debug("Creating new dataset");
        return dataverseService.createDataset(dataset);
    }

    private void updateDatasetMetadata() throws IOException, DataverseException {
        if (dataset == null) {
            log.debug("No dataset metadata found. Skipping dataset metadata update.");
            return;
        }
        log.debug("Start updating dataset metadata for deposit {}", depositId);
        dataverseService.updateMetadata(pid, dataset.getDatasetVersion());
        log.debug("End updating dataset metadata for deposit {}", depositId);
    }

    private void editFiles() throws IOException, DataverseException {
        if (editFiles != null) {
            deleteFiles();
            replaceFiles();
            addRestrictedFiles();
        }
        addUnrestrictedFiles();
        if (editFiles != null) {
            moveFiles();
            updateFileMetas();
//            addEmbargoes();
        }
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

    private void editMetadata() throws IOException, DataverseException {
        if (editMetadata == null) {
            log.debug("No metadata found. Skipping metadata update.");
            return;
        }
        log.debug("Start updating metadata for deposit {}", depositId);
        addFieldValues();
        replaceFieldValues();
        deleteFieldValues();
        log.debug("End updating metadata for deposit {}", depositId);
    }

    private void deleteFieldValues() throws IOException, DataverseException {
        log.debug("Start deleting {} field values for deposit {}", depositId, editMetadata.getDeleteFieldValues().size());
        for (var fieldValue : editMetadata.getDeleteFieldValues()) {
            log.debug("Deleting field value: {}", fieldValue);
            dataverseService.deleteDatasetMetadata(pid, editMetadata.getDeleteFieldValues());
        }
        log.debug("End deleting field values for deposit {}", depositId);
    }

    private void addFieldValues() throws IOException, DataverseException {
        log.debug("Start adding {} field values for deposit {}", depositId, editMetadata.getAddFieldValues().size());
        for (var fieldValue : editMetadata.getAddFieldValues()) {
            log.debug("Adding field value: {}", fieldValue);
            dataverseService.editMetadata(pid, editMetadata.getAddFieldValues(), false);
        }
        log.debug("End adding field values for deposit {}", depositId);
    }

    private void replaceFieldValues() throws IOException, DataverseException {
        log.debug("Start replacing {} field values for deposit {}", depositId, editMetadata.getReplaceFieldValues().size());
        for (var fieldValue : editMetadata.getReplaceFieldValues()) {
            log.debug("Replacing field value: {}", fieldValue);
            dataverseService.editMetadata(pid, editMetadata.getReplaceFieldValues(), true);
        }
        log.debug("End replacing field values for deposit {}", depositId);
    }

    private void editPermissions() throws IOException, DataverseException {
        if (editPermissions == null) {
            log.debug("No permissions found. Skipping permissions update.");
            return;
        }
        log.debug("Start updating permissions for deposit {}", depositId);
        deleteRoleAssignments();
        addRoleAssignments();
        log.debug("End updating permissions for deposit {}", depositId);
    }

    private void addRoleAssignments() throws IOException, DataverseException {
        log.debug("Start adding {} role assignments for deposit {}", depositId, editPermissions.getAddRoleAssignments().size());
        for (var roleAssignment : editPermissions.getAddRoleAssignments()) {
            log.debug("Adding role assignment: {}", roleAssignment);
            dataverseService.addRoleAssignment(pid, roleAssignment);
        }
        log.debug("End adding role assignments for deposit {}", depositId);
    }

    private void deleteRoleAssignments() throws IOException, DataverseException {
        log.debug("Start deleting {} role assignments for deposit {}", depositId, editPermissions.getDeleteRoleAssignments().size());
        for (var roleAssignment : editPermissions.getDeleteRoleAssignments()) {
            log.debug("Deleting role assignment: {}", roleAssignment);
            dataverseService.deleteRoleAssignment(pid, roleAssignment);
        }
        log.debug("End deleting role assignments for deposit {}", depositId);
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

    private void updateFileMetas() throws IOException, DataverseException {
        log.debug("Start updating {} file metas for deposit {}", depositId, editFiles.getUpdateFileMetas().size());
        for (var fileMeta : editFiles.getUpdateFileMetas()) {
            var id = getFilesInDataset().get(getPath(fileMeta)).getDataFile().getId();
            dataverseService.updateFileMetadata(id, fileMeta);
        }
        log.debug("End updating file metadata for deposit {}", depositId);
    }

//    private void addEmbargoes() throws IOException, DataverseException {
//        log.debug("Start adding {} embargoes for deposit {}", depositId, editFiles.getAddEmbargoes().size());
//        for (var embargo : editFiles.getAddEmbargoes()) {
//            var fileMeta = getFilesInDataset().get(embargo.getPath());
//            dataverseService.addEmbargo(fileMeta.getDataFile().getId(), embargo.getEmbargo());
//        }
//        log.debug("End adding embargoes for deposit {}", depositId);
//    }

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

    private String getDirectoryLabel(String path) {
        int lastIndex = path.lastIndexOf('/');
        return lastIndex == -1 ? "" : path.substring(0, lastIndex);
    }

    private String getFileName(String path) {
        int lastIndex = path.lastIndexOf('/');
        return path.substring(lastIndex + 1);
    }

    private void updateState() throws DataverseException, IOException {
        if (updateState == null) {
            log.debug("No update state found. Skipping update state processing.");
            return;
        }
        if ("publish-major".equals(updateState.getAction())) {
            publishVersion(UpdateType.major);
        }
        else if ("publish-minor".equals(updateState.getAction())) {
            publishVersion(UpdateType.minor);
        }
        else if ("submit-for-review".equals(updateState.getAction())) {
            // TODO: Implement submit for review
        }
        else {
            throw new IllegalArgumentException("Unknown update state action: " + updateState.getAction());
        }
    }

    private void publishVersion(UpdateType updateType) throws DataverseException, IOException {
        log.debug("Start publishing version for deposit {}", depositId);
        dataverseService.publishDataset(pid, updateType);
        dataverseService.waitForState(pid, "RELEASED");
        log.debug("End publishing version for deposit {}", depositId);
    }
}
