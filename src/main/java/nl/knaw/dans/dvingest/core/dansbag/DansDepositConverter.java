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
package nl.knaw.dans.dvingest.core.dansbag;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.DataverseIngestBag;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDeposit;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.EditPermissionsRoot;
import nl.knaw.dans.dvingest.core.yaml.InitRoot;
import nl.knaw.dans.dvingest.core.yaml.UpdateStateRoot;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class DansDepositConverter {
    private static final List<String> YAML_FILES = List.of("init.yml", "dataset.yml", "edit-files.yml", "edit-permissions.yml", "update-state.yml");

    private final DansBagDeposit dansDeposit;
    private final String updatesDataset;
    private final DatasetVersion currentMetadata;
    private final DansBagMappingService mappingService;
    private final YamlService yamlService;

    public void run() throws IOException {
        deleteOldYamlFilesIfPresent();
        var init = mappingService.getInitFromDansDeposit(dansDeposit, updatesDataset != null);
        yamlService.writeYaml(new InitRoot(init), dansDeposit.getBagDir().resolve(DataverseIngestBag.INIT_YML));

        var dataset = mappingService.getDatasetMetadataFromDansDeposit(dansDeposit, currentMetadata);
        yamlService.writeYaml(dataset, dansDeposit.getBagDir().resolve(DataverseIngestBag.DATASET_YML));

        var editFiles = mappingService.getEditFilesFromDansDeposit(dansDeposit, updatesDataset);
        yamlService.writeYaml(new EditFilesRoot(editFiles), dansDeposit.getBagDir().resolve(DataverseIngestBag.EDIT_FILES_YML));

        var editPermissions = mappingService.getEditPermissionsFromDansDeposit(dansDeposit, updatesDataset != null);
        yamlService.writeYaml(new EditPermissionsRoot(editPermissions), dansDeposit.getBagDir().resolve(DataverseIngestBag.EDIT_PERMISSIONS_YML));

        var updateAction = mappingService.getUpdateActionFromDansDeposit(dansDeposit);
        if (updateAction.isPresent()) {
            log.debug("Writing update action to YAML");
            yamlService.writeYaml(new UpdateStateRoot(updateAction.get()), dansDeposit.getBagDir().resolve(DataverseIngestBag.UPDATE_STATE_YML));
        }
        else {
            log.debug("No update action found in DANS deposit");
        }
    }

    private void deleteOldYamlFilesIfPresent() {
        log.debug("Starting with clean slate, deleting old YAML files if present");
        for (String file : YAML_FILES) {
            var deleted = FileUtils.deleteQuietly(dansDeposit.getBagDir().resolve(file).toFile());
            if (deleted) {
                log.debug("Deleted old YAML file: {}", file);
            }
            else {
                log.debug("No old YAML file found or could not be deleted: {}", file);
            }
        }
    }
}
