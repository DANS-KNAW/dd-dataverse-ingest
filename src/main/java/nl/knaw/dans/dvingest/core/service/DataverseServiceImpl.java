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
import nl.knaw.dans.lib.dataverse.DataverseHttpResponse;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetCreationResult;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetPublicationResult;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    public DataverseHttpResponse<DatasetCreationResult> createDataset(Dataset datasetMetadata) throws DataverseException, IOException {
        return dataverseClient.dataverse("root").createDataset(datasetMetadata, metadataKeys);
    }

    @Override
    public DataverseHttpResponse<FileList> addFile(String persistentId, Path file, FileMeta fileMeta) throws DataverseException, IOException {
        return dataverseClient.dataset(persistentId).addFile(file, fileMeta);
    }

    @Override
    public DataverseHttpResponse<DatasetPublicationResult> publishDataset(String persistentId) throws DataverseException, IOException {
        return dataverseClient.dataset(persistentId).publish();
    }

    @Override
    public DataverseHttpResponse<DatasetVersion> updateMetadata(String targetDatasetPid, DatasetVersion datasetMetadata) throws DataverseException, IOException {
        return dataverseClient.dataset(targetDatasetPid).updateMetadata(datasetMetadata, metadataKeys);
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

    private String getDatasetState(String datasetId) throws IOException, DataverseException {
        var version = dataverseClient.dataset(datasetId).getLatestVersion();
        return version.getData().getLatestVersion().getVersionState();

    }
}
