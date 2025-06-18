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

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.Embargo;
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.dataset.License;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.dataverse.model.file.FileMetaUpdate;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DataverseService {

    String createDataset(Dataset datasetMetadata) throws DataverseException, IOException;

    FileList addFile(String persistentId, Path file, FileMeta fileMeta) throws DataverseException, IOException;

    void publishDataset(String persistentId, UpdateType updateType) throws DataverseException, IOException;

    List<FileMeta> getFiles(String pid, boolean includeDraftVersion) throws IOException, DataverseException;

    FileMeta replaceFile(String targetDatasetPid, FileMeta fileToReplace, Path replacement) throws DataverseException, IOException;

    void deleteFiles(String pid, List<Integer> ids) throws DataverseException, IOException;

    String getDatasetUrnNbn(String datasetId) throws IOException, DataverseException;

    void updateMetadata(String targetDatasetPid, DatasetVersion datasetMetadata) throws DataverseException, IOException;

    void updateFileMetadatas(String pid, List<FileMetaUpdate> fileMetaUpdates) throws DataverseException, IOException;

    void deleteDatasetMetadata(String pid, List<MetadataField> fields) throws DataverseException, IOException;

    void editMetadata(String pid, List<MetadataField> addFieldValues, boolean b) throws DataverseException, IOException;

    void addRoleAssignment(String pid, RoleAssignment roleAssignment) throws DataverseException, IOException;

    void deleteRoleAssignment(String pid, RoleAssignment roleAssignment) throws DataverseException, IOException;

    Optional<AuthenticatedUser> getUserById(String userId);

    List<License> getSupportedLicenses() throws IOException, DataverseException;

    Set<String> getActiveMetadataBlockNames() throws IOException, DataverseException;

    void addEmbargo(String pid, Embargo embargo) throws IOException, DataverseException;

    List<String> findDoiByMetadataField(String fieldName, String value) throws IOException, DataverseException;

    DatasetVersion getDatasetMetadata(String pid) throws IOException, DataverseException;

    String getDatasetState(String targetPid) throws IOException, DataverseException;

    void importDataset(String pid, Dataset dataset) throws IOException, DataverseException;

    void releaseMigratedDataset(String pid, String date) throws DataverseException, IOException;

    void waitForReleasedState(String persistentId, int numberOfFilesInDataset) throws DataverseException, IOException;

    List<RoleAssignmentReadOnly> getRoleAssignmentsOnDataverse(String dataverseAlias) throws DataverseException, IOException;

    List<RoleAssignmentReadOnly> getRoleAssignmentsOnDataset(String persistentId) throws DataverseException, IOException;
}
