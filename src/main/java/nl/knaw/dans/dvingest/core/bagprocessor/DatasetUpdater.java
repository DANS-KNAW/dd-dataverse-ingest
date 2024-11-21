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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;

import java.io.IOException;
import java.util.UUID;

/**
 * Updates a dataset in Dataverse, or create a new one if it does not exist yet.
 */
@Slf4j
@AllArgsConstructor
public class DatasetUpdater {
    private final UUID depositId;
    private final DataverseService dataverseService;
    private final Dataset dataset;

    public String createOrUpdateDataset(String targetPid) throws IOException, DataverseException {
        var pid = targetPid;
        if (targetPid == null) {
            if (dataset == null) {
                throw new IllegalArgumentException("Must have dataset metadata to create a new dataset.");
            }
            pid = createDataset();
        }
        else {
            if (dataset != null) {
                updateDataset(pid);
            }
        }
        return pid;
    }

    private String createDataset() throws IOException, DataverseException {
        log.debug("Start creating dataset for deposit {}", depositId);
        var pid = dataverseService.createDataset(dataset);
        log.debug("End creating dataset for deposit {}", depositId);
        return pid;
    }

    private void updateDataset(String pid) throws IOException, DataverseException {
        log.debug("Start updating dataset for deposit {}", depositId);
        dataverseService.updateMetadata(pid, dataset.getDatasetVersion());
        log.debug("End updating dataset for deposit {}", depositId);
    }
}
