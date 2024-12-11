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
package nl.knaw.dans.dvingest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.client.ValidateDansBagService;
import nl.knaw.dans.dvingest.core.DataverseIngestDeposit;
import nl.knaw.dans.dvingest.core.Deposit;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositSupport;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositSupportFactory;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.YamlService;

@Slf4j
@AllArgsConstructor
public class DansDepositSupportFactoryImpl implements DansDepositSupportFactory {
    private final ValidateDansBagService validateDansBagService;
    private final DansBagMappingService dansBagMappingService;
    private final DataverseService dataverseService;
    private final YamlService yamlService;

    // todo: property isMigration

    @Override
    public Deposit addDansDepositSupportIfEnabled(DataverseIngestDeposit deposit) {
        return new DansDepositSupport(deposit, validateDansBagService, dansBagMappingService, dataverseService, yamlService);
    }
}
