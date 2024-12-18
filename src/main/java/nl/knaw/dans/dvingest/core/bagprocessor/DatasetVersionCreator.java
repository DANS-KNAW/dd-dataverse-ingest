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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.Expect;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;

import java.io.IOException;
import java.util.UUID;

/**
 * Creates a new dataset version in Dataverse. If the target dataset does not exist, a new dataset is created.
 */
@Slf4j
@AllArgsConstructor
public class DatasetVersionCreator {
    @NonNull
    private final UUID depositId;
    @NonNull
    private final DataverseService dataverseService;

    private final Init init;

    private final Dataset dataset;

    public String createDatasetVersion(String targetPid) throws IOException, DataverseException {
        if (init != null && init.getExpect() != null) {
            checkExpectations(init.getExpect(), targetPid);
        }

        var pid = targetPid;
        if (targetPid == null) {
            if (dataset == null) {
                throw new IllegalArgumentException("Must have dataset metadata to create a new dataset.");
            }
            if (init != null && init.getCreate() != null && init.getCreate().getImportPid() != null) {
                importDataset(init.getCreate().getImportPid());
                pid = init.getCreate().getImportPid();
            }
            else {
                pid = createDataset();
            }
        }
        // Even if we just created the dataset, we still need to update the metadata, because Dataverse ignores some things
        // in the create request.
        if (dataset != null) {
            updateDataset(pid);
        }
        return pid;
    }

    private void checkExpectations(Expect expect, String targetPid) throws DataverseException, IOException {
        if (targetPid == null) {
            return;
        }

        if (expect != null && expect.getState() != null) {
            switch (expect.getState()) {
                case draft:
                case released:
                    var state = dataverseService.getDatasetState(targetPid);
                    if (expect.getState().name().equals(state.toLowerCase())) {
                        log.debug("Expected state {} found for dataset {}", expect.getState(), targetPid);
                    }
                    else {
                        throw new IllegalStateException("Expected state " + expect.getState() + " but found " + state + " for dataset " + targetPid);
                    }
                    break;
                case absent:
                    try {
                        dataverseService.getDatasetState(targetPid);
                        throw new IllegalStateException("Expected state absent but found for dataset " + targetPid);
                    }
                    catch (DataverseException e) {
                        if (e.getMessage().contains("404")) {
                            log.debug("Expected state absent found for dataset {}", targetPid);
                        }
                        else {
                            throw e;
                        }
                    }
            }
        }
    }

    private void importDataset(String pid) throws IOException, DataverseException {
        log.debug("Start importing dataset for deposit {}", depositId);
        dataverseService.importDataset(pid, dataset);
        log.debug("End importing dataset for deposit {}", depositId);
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
