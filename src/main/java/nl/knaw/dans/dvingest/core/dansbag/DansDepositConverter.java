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
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.UpdateState;
import nl.knaw.dans.ingest.core.domain.Deposit;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@AllArgsConstructor
public class DansDepositConverter {
    private final Deposit dansDeposit;
    private final DansBagMappingService mappingService;
    private final YamlService yamlService;

    public void run() throws IOException {
        // TODO: Create original-metadata.zip

        var dataset = mappingService.getDatasetMetadataFromDansDeposit(dansDeposit);
        yamlService.writeYaml(dataset, dansDeposit.getBagDir().resolve("dataset.yml"));

        var editFiles = mappingService.getEditFilesFromDansDeposit(dansDeposit);
        yamlService.writeYaml(new EditFilesRoot(editFiles), dansDeposit.getBagDir().resolve("edit-files.yml"));

        var updateState = new UpdateState();
        updateState.setAction("publish-major");
        yamlService.writeYaml(updateState, dansDeposit.getBagDir().resolve("update-state.yml"));
    }

}
