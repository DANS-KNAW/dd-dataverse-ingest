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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class IngestTask implements Runnable {
    private final Deposit deposit;
    private final DataverseClient dataverseClient;
    private final Path outputDir;

    @Override
    public void run() {
        try {
            var result = dataverseClient.dataverse("root").createDataset(deposit.getDatasetMetadata());
            var pid = result.getData().getPersistentId();
            log.debug(result.getEnvelopeAsString());

            // Upload files
            var iterator = new PathIterator(FileUtils.iterateFiles(deposit.getFilesDir().toFile(), null, true));
            int uploadBatchSize = 1000; // TODO: make configurable
            while (iterator.hasNext()) {
                var zipFile = PathIteratorZipper.builder()
                    .rootDir(deposit.getFilesDir())
                    .sourceIterator(iterator)
                    .targetZipFile(Files.createTempFile("dvingest", ".zip"))
                    .maxNumberOfFiles(uploadBatchSize)
                    .build()
                    .zip();
                dataverseClient.dataset(pid).addFile(zipFile, new FileMeta());
                log.debug("Uploaded {} files (cumulative)", iterator.getIteratedCount());
            }

            // Publish dataset
            dataverseClient.dataset(pid).publish();

            // Wait for publish to complete
            waitForState(pid, "RELEASED");

            deposit.moveTo(outputDir.resolve("processed"));
        }
        catch (Exception e) {
            try {
                log.error("Failed to ingest deposit", e);
                deposit.moveTo(outputDir.resolve("failed"));
            }
            catch (IOException ioException) {
                log.error("Failed to move deposit to failed directory", ioException);
            }
        }
    }

    private void waitForState(String datasetId, String expectedState) {
        var numberOfTimesTried = 0;
        var state = "";

        try {
            state = getDatasetState(datasetId);

            log.debug("Initial state for dataset {} is {}", datasetId, state);

            // TODO: make configurable again
            while (!expectedState.equals(state) && numberOfTimesTried < 10) {
                Thread.sleep(3000);

                state = getDatasetState(datasetId);
                numberOfTimesTried += 1;
                log.trace("Current state for dataset {} is {}, numberOfTimesTried = {}", datasetId, state, numberOfTimesTried);
            }

            if (!expectedState.equals(state)) {
                throw new IllegalStateException(String.format(
                    "Dataset did not become %s within the wait period; current state is %s", expectedState, state
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
