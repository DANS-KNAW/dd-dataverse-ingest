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
import nl.knaw.dans.dvingest.core.yaml.Create;
import nl.knaw.dans.dvingest.core.yaml.Expect;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import org.apache.commons.lang3.StringUtils;

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
        if (init != null) {
            verifyCreate(init.getCreate(), init.getExpect(), targetPid);
            verifyExpect(init.getExpect(), targetPid);
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

    private void verifyCreate(Create create, Expect expect, String targetPid) throws IOException, DataverseException {
        if (create != null && create.getImportPid() != null) {
            if (targetPid != null) {
                throw new IllegalArgumentException("Cannot import a dataset when updating an existing dataset.");
            }
            if (StringUtils.isBlank(create.getImportPid())) {
                throw new IllegalArgumentException("Cannot import a dataset without a PID.");
            }
            if (expect != null && expect.getState() != null && !"absent".equalsIgnoreCase(expect.getState())) {
                throw new IllegalArgumentException("Cannot expect a state other than 'absent' when importing a dataset.");
            }
        }
    }

    private void verifyExpect(Expect expect, String targetPid) throws IOException, DataverseException {
        var expectedState = targetPid == null ? "absent" : "released";
        if (expect != null && expect.getState() != null) {
            expectedState = expect.getState().toLowerCase();
        }
        if (targetPid == null) {
            if (!expectedState.equals("absent")) {
                throw new IllegalArgumentException("Cannot expect a state other than 'absent' when creating a new dataset.");
            }
            // Nothing to check, the dataset is absent by definition if we are creating it; if we are importing it, the action will fail if the PID already exists.
        }
        else {
            if (expectedState.equals("absent")) {
                throw new IllegalArgumentException("Cannot expect state 'absent' when updating an existing dataset.");
            }
            var actualState = dataverseService.getDatasetState(targetPid);
            if (!expectedState.equals(actualState.toLowerCase())) {
                throw new IllegalStateException("Expected state " + expectedState + " but found " + actualState + " for dataset " + targetPid);
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
