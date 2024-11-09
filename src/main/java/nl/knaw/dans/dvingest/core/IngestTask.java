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

import java.io.IOException;
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
            log.debug(result.getEnvelopeAsString());

            // Upload files

            // Publish dataset

            // Wait for publish to complete

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
}
