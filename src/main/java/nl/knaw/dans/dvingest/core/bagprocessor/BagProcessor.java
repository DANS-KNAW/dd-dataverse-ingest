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

import io.dropwizard.configuration.ConfigurationException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.DataverseIngestBag;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.yaml.UpdateState;
import nl.knaw.dans.dvingest.core.yaml.UpdateStateRoot;
import nl.knaw.dans.lib.dataverse.DataverseException;

import java.io.IOException;
import java.util.UUID;

/**
 * Processes a bag, creating and/or editing a dataset version in Dataverse.
 */
@Slf4j
public class BagProcessor {
    private final DatasetVersionCreator datasetVersionCreator;
    private final FilesEditor filesEditor;
    private final MetadataEditor metadataEditor;
    private final PermissionsEditor permissionsEditor;
    private final StateUpdater stateUpdater;

    @Builder
    private BagProcessor(UUID depositId, DataverseIngestBag bag, DataverseService dataverseService, UtilityServices utilityServices) throws IOException, ConfigurationException {
        this.datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseService, bag.getInit(), bag.getDatasetMetadata());
        this.filesEditor = new FilesEditor(depositId, bag.getDataDir(), bag.getEditFiles(), dataverseService, utilityServices);
        this.metadataEditor = new MetadataEditor(depositId, bag.getEditMetadata(), dataverseService);
        this.permissionsEditor = new PermissionsEditor(depositId, bag.getEditPermissions(), dataverseService);
        this.stateUpdater = new StateUpdater(depositId, bag.getUpdateState(), dataverseService);
    }

    public String run(String targetPid) throws IOException, DataverseException {
        targetPid = datasetVersionCreator.createDatasetVersion(targetPid);
        filesEditor.editFiles(targetPid);
        metadataEditor.editMetadata(targetPid);
        permissionsEditor.editPermissions(targetPid);
        stateUpdater.updateState(targetPid);
        return targetPid;
    }
}
