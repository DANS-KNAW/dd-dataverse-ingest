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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.Version;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.Embargo;
import nl.knaw.dans.lib.dataverse.model.dataset.FieldList;
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.dataset.License;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlockSummary;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class DataverseServiceImpl implements DataverseService {
    @NonNull
    private final DataverseClient dataverseClient;

    @Builder.Default
    private int maxNumberOfRetries = 10;

    @Builder.Default
    private long millisecondsBetweenChecks = 3000;

    @Builder.Default
    private Map<String, String> metadataKeys = new HashMap<>();

    public String createDataset(Dataset datasetMetadata) throws DataverseException, IOException {
        var result = dataverseClient.dataverse("root").createDataset(datasetMetadata, metadataKeys);
        log.debug(result.getEnvelopeAsString());
        return result.getData().getPersistentId();
    }

    @Override
    public FileList addFile(String persistentId, Path file, FileMeta fileMeta) throws DataverseException, IOException {
        var result = dataverseClient.dataset(persistentId).addFile(file, fileMeta);
        log.debug(result.getEnvelopeAsString());
        return result.getData();
    }

    @Override
    public void publishDataset(String persistentId, UpdateType updateType) throws DataverseException, IOException {
        var result = dataverseClient.dataset(persistentId).publish(updateType, true);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void updateMetadata(String targetDatasetPid, DatasetVersion datasetMetadata) throws DataverseException, IOException {
        var result = dataverseClient.dataset(targetDatasetPid).updateMetadata(datasetMetadata, metadataKeys);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void updateFileMetadata(int id, FileMeta newMeta) throws DataverseException, IOException {
        var result = dataverseClient.file(id).updateMetadata(newMeta);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public List<FileMeta> getFiles(String pid) throws IOException, DataverseException {
        var result = dataverseClient.dataset(pid).getFiles(Version.LATEST.toString());
        return result.getData();
    }

    @Override
    public void replaceFile(String targetDatasetPid, FileMeta fileToReplace, Path replacement) throws DataverseException, IOException {
        log.debug("Replacing file: {}", fileToReplace);
        var result = dataverseClient.file(fileToReplace.getDataFile().getId()).replaceFile(replacement, fileToReplace);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void deleteFile(int id) throws DataverseException, IOException {
        var result = dataverseClient.sword().deleteFile(id);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void deleteDatasetMetadata(String pid, List<MetadataField> fieldList) throws DataverseException, IOException {
        var result = dataverseClient.dataset(pid).deleteMetadata(new FieldList(fieldList), metadataKeys);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void editMetadata(String pid, List<MetadataField> addFieldValues, boolean b) throws DataverseException, IOException {
        var result = dataverseClient.dataset(pid).editMetadata(new FieldList(addFieldValues), b);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void addRoleAssignment(String pid, RoleAssignment roleAssignment) throws DataverseException, IOException {
        var result = dataverseClient.dataset(pid).assignRole(roleAssignment);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public void deleteRoleAssignment(String pid, RoleAssignment roleAssignment) throws DataverseException, IOException {
        var listResult = dataverseClient.dataset(pid).listRoleAssignments();
        var list = listResult.getData();
        for (RoleAssignmentReadOnly ra : list) {
            if (ra.getAssignee().equals(roleAssignment.getAssignee()) && ra.get_roleAlias().equals(roleAssignment.getRole())) {
                log.debug("Deleting role assignment: {}", ra);
                var deleteResult = dataverseClient.dataset(pid).deleteRoleAssignment(ra.getId());
                log.debug(deleteResult.getEnvelopeAsString());
            }
        }
    }

    @Override
    public Optional<AuthenticatedUser> getUserById(String userId) {
        try {
            return Optional.of(dataverseClient.admin().listSingleUser(userId).getData());
        }
        catch (IOException | DataverseException e) {
            log.error("Error retrieving user with id {} from dataverse", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<License> getSupportedLicenses() throws IOException, DataverseException {
        return dataverseClient.license().getLicenses().getData().stream()
            // We need to map from one license class to another, because they have different fields
            .map(license -> new License(license.getName(), URI.create(license.getUri()), URI.create(license.getIconUrl()))).toList();
    }

    @Override
    public Set<String> getActiveMetadataBlockNames() throws IOException, DataverseException {
        return dataverseClient.dataverse("root")
            .listMetadataBlocks()
            .getData()
            .stream()
            .map(MetadataBlockSummary::getName)
            .collect(Collectors.toSet());
    }

    @Override
    public void addEmbargo(String pid, Embargo embargo) throws IOException, DataverseException {
        var result = dataverseClient.dataset(pid).setEmbargo(embargo);
        log.debug(result.getEnvelopeAsString());
    }

    @Override
    public List<String> findDoiByMetadataField(String key, String value) throws IOException, DataverseException {
        var query = String.format("%s:\"%s\"", key, value);

        log.trace("Searching datasets with query '{}'", query);
        var results = dataverseClient.search().find(query);
        var items = results.getData().getItems();

        return items.stream()
            .filter(r -> r instanceof DatasetResultItem)
            .map(r -> (DatasetResultItem) r)
            .map(DatasetResultItem::getGlobalId)
            .collect(Collectors.toList());
    }

    @Override
    public String getDatasetUrnNbn(String pid) throws IOException, DataverseException {
        var dataset = dataverseClient.dataset(pid);
        var version = dataset.getVersion();
        var data = version.getData();
        var metadata = data.getMetadataBlocks().get("dansDataVaultMetadata");

        return metadata.getFields().stream()
            .filter(f -> f.getTypeName().equals("dansNbn"))
            .map(f -> (PrimitiveSingleValueField) f)
            .map(PrimitiveSingleValueField::getValue)
            .findFirst().orElseThrow(() -> new IllegalStateException("No URN:NBN found in dataset"));
    }

    @Override
    public DatasetVersion getDatasetMetadata(String pid) throws IOException, DataverseException {
        return dataverseClient.dataset(pid).getVersion().getData();
    }

    @Override
    public String getDatasetState(String pid) throws IOException, DataverseException {
        return dataverseClient.dataset(pid).getVersion(Version.LATEST.toString(), true).getData().getVersionState();
    }

    @Override
    public void importDataset(String pid, Dataset dataset) throws IOException, DataverseException {
        log.debug("Start importing dataset for deposit {}", pid);
        var result = dataverseClient.dataverse("root").importDataset(dataset, pid, false);
        log.debug(result.getEnvelopeAsString());
        log.debug("End importing dataset for deposit {}", pid);
    }

    // TODO: move this to dans-dataverse-client-lib; it is similar to awaitLockState.
    public void waitForState(String datasetId, String expectedState) {
        var numberOfTimesTried = 0;
        var state = "";

        try {
            state = getDatasetState(datasetId);
            log.debug("Initial state for dataset {} is {}", datasetId, state);
            while (!expectedState.equals(state) && numberOfTimesTried < maxNumberOfRetries) {
                log.debug("Sleeping for {} milliseconds before checking again", millisecondsBetweenChecks);
                Thread.sleep(millisecondsBetweenChecks);

                state = getDatasetState(datasetId);
                numberOfTimesTried += 1;
                log.debug("Current state for dataset {} is {}, tried {} of {} times", datasetId, state, numberOfTimesTried, maxNumberOfRetries);
            }

            if (!expectedState.equals(state)) {
                throw new IllegalStateException(String.format(
                    "Dataset did not become %s within the wait period (%d seconds); current state is %s",
                    expectedState, (maxNumberOfRetries * millisecondsBetweenChecks), state
                ));
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Dataset state check was interrupted; last know state is " + state);
        }
        catch (IOException | DataverseException e) {
            throw new RuntimeException(e);
        }
    }
}
