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
import nl.knaw.dans.dvingest.client.ValidateDansBagService;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.lib.util.inbox.InboxTaskFactory;

import java.nio.file.Path;

@AllArgsConstructor
public class DepositInboxTaskFactory implements InboxTaskFactory {
    private final Path outputDir;
    private final boolean onlyConvertDansDeposit;
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;
    private final ValidateDansBagService validateDansBagService;
    private final DansBagMappingService dansBagMappingService;
    private final YamlService yamlService;

    @Override
    public Runnable createInboxTask(Path path) {
        var deposit = new DataverseIngestDeposit(path, yamlService);
        return new DepositTask(deposit, outputDir, onlyConvertDansDeposit, validateDansBagService, dataverseService, utilityServices, dansBagMappingService, yamlService);
    }
}
