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
package nl.knaw.dans.ingest.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.ingest.core.dataverse.DatasetService;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.FileInfo;
import nl.knaw.dans.ingest.core.exception.FailedDepositException;
import nl.knaw.dans.lib.dataverse.DataverseApi;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.singletonMap;

@Slf4j
public class DatasetCreator extends DatasetEditor {

    private final String depositorRole;

    public DatasetCreator(
        boolean isMigration,
        Dataset dataset,
        Deposit deposit,
        ObjectMapper objectMapper,
        List<URI> supportedLicenses,
        Pattern fileExclusionPattern,
        ZipFileHandler zipFileHandler,
        String depositorRole,
        DatasetService datasetService,
        String vaultMetadataKey,
        boolean deleteDraftOnFailure) {
        super(
            isMigration,
            dataset,
            deposit,
            supportedLicenses,
            fileExclusionPattern,
            zipFileHandler, objectMapper, datasetService,
            vaultMetadataKey,
            deleteDraftOnFailure);

        this.depositorRole = depositorRole;
    }

    @Override
    public String performEdit() {
        var api = dataverseClient.dataverse("root");

        log.debug("Creating new dataset");
        String persistentId = null;

        try {
            persistentId = importDataset(api);
            log.debug("New persistent ID: {}", persistentId);
            modifyDataset(persistentId);
            return persistentId;
        }
        catch (Exception e) {
            try {
                if (persistentId != null) {
                    deleteDraftIfExists(persistentId);
                }
            }
            catch (IOException | DataverseException ex) {
                log.error("Error deleting draft dataset", ex);
            }
            throw new FailedDepositException(deposit, "Error creating dataset" + (this.deleteDraftOnFailure ? ". Deleting draft" : "."), e);
        }
    }

    private void modifyDataset(String persistentId) throws IOException, DataverseException {
        var api = dataverseClient.dataset(persistentId);

        // This will set fileAccessRequest and termsOfAccess
        var version = dataset.getDatasetVersion();
        version.setFileAccessRequest(deposit.allowAccessRequests());
        if (!deposit.allowAccessRequests() && StringUtils.isBlank(version.getTermsOfAccess())) {
            version.setTermsOfAccess("N/a");
        }
        var keyMap = new HashMap<String, String>(singletonMap("dansDataVaultMetadata", vaultMetadataKey));
        api.updateMetadata(version, keyMap);
        api.awaitUnlock();

        // license stuff
        var license = toJson(Map.of("http://schema.org/license", getLicense(deposit.getDdm())));
        log.debug("Setting license to {}", license);
        api.updateMetadataFromJsonLd(license, true);
        api.awaitUnlock();

        // add files to dataset
        var pathToFileInfo = getFileInfo();

        FileInfo originalMetadata = null;
        if (!isMigration) {
            originalMetadata = createOriginalMetadataFileInfo();
            pathToFileInfo.put(Paths.get(ORIGINAL_METADATA_ZIP), originalMetadata);
        }

        log.debug("File info: {}", pathToFileInfo);
        var databaseIdToFileInfo = addFiles(persistentId, pathToFileInfo.values());

        if (originalMetadata != null) {
            FileUtils.deleteQuietly(originalMetadata.getPath().toFile());
        }

        log.debug("Database ID -> FileInfo: {}", databaseIdToFileInfo);
        // update individual files metadata
        updateFileMetadata(databaseIdToFileInfo);
        api.awaitUnlock();

        api.assignRole(getRoleAssignment());
        api.awaitUnlock();

        var dateAvailable = getDateAvailable(deposit); //deposit.getDateAvailable().get();
        embargoFiles(persistentId, dateAvailable);
    }

    private RoleAssignment getRoleAssignment() {
        var result = new RoleAssignment();
        result.setRole(depositorRole);
        result.setAssignee(String.format("@%s", deposit.getDepositorUserId()));

        return result;
    }

    private void updateFileMetadata(Map<Integer, FileInfo> databaseIds) throws IOException, DataverseException {
        for (var entry : databaseIds.entrySet()) {
            var id = entry.getKey();
            var result = dataverseClient.file(id).updateMetadata(entry.getValue().getMetadata());
            log.debug("Called updateFileMetadata for id = {}; result = {} {}", id, result.getHttpResponse().getCode(), result.getHttpResponse().getReasonPhrase());
        }
    }

    String importDataset(DataverseApi api) throws IOException, DataverseException {
        var keyMap = new HashMap<String, String>(singletonMap("dansDataVaultMetadata", vaultMetadataKey));

        var response = isMigration
            ? api.importDataset(dataset, String.format("doi:%s", deposit.getDoi()), false, keyMap)
            : api.createDataset(dataset, keyMap);

        return response.getData().getPersistentId();
    }
}
