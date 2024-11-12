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
package nl.knaw.dans.dvingest.core.datasetversiontask;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.Deposit;
import nl.knaw.dans.dvingest.core.service.PathIterator;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the first version of a new dataset in Dataverse.
 */
@Slf4j
public class CreateNewDataset extends AbstractDatasetVersionTask {
    public CreateNewDataset(Deposit deposit, DataverseService dataverseService, UtilityServices utilityServices, Path outputDir) {
        super(deposit, dataverseService, utilityServices, outputDir);
    }

    @Override
    public String performAndReturnPid() throws Exception {
        var result = dataverseService.createDataset(deposit.getDatasetMetadata());
        var pid = result.getData().getPersistentId();
        log.debug(result.getEnvelopeAsString());
        var iterator = new PathIterator(FileUtils.iterateFiles(deposit.getFilesDir().toFile(), null, true));
        while (iterator.hasNext()) {
            var tempZipFile = utilityServices.createTempZipFile();
            try {
                var zipFile = utilityServices.createPathIteratorZipperBuilder()
                    .rootDir(deposit.getFilesDir())
                    .sourceIterator(iterator)
                    .targetZipFile(tempZipFile)
                    .build()
                    .zip();
                dataverseService.addFile(pid, zipFile, new FileMeta());
                log.debug("Uploaded {} files (cumulative)", iterator.getIteratedCount());
            }
            finally {
                Files.deleteIfExists(tempZipFile);
            }

        }
        return pid;
    }
}
