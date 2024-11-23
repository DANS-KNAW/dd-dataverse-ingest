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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.bagprocessor.BagProcessor;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositConverter;
import nl.knaw.dans.dvingest.core.service.DansBagMappingService;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.service.YamlService;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class DepositTask implements Runnable {
    public enum Status {
        TODO,
        SUCCESS,
        REJECTED,
        FAILED
    }

    private final Deposit deposit;
    private final Path outputDir;
    private final boolean onlyConvertDansDeposit;
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;
    private final DansBagMappingService dansBagMappingService;
    private final YamlService yamlService;

    @Getter
    private Status status = Status.TODO;

    public DepositTask(Deposit deposit, Path outputDir, boolean onlyConvertDansDeposit, DataverseService dataverseService, UtilityServices utilityServices, DansBagMappingService dansBagMappingService,
        YamlService yamlService) {
        this.deposit = deposit;
        this.dataverseService = dataverseService;
        this.onlyConvertDansDeposit = onlyConvertDansDeposit;
        this.utilityServices = utilityServices;
        this.outputDir = outputDir;
        this.dansBagMappingService = dansBagMappingService;
        this.yamlService = yamlService;
    }

    @Override
    public void run() {
        try {
            String pid = deposit.getUpdatesDataset();
            for (DataverseIngestBag bag : deposit.getBags()) {
                log.info("START processing deposit / bag: {} / {}", deposit.getId(), bag);
                if (bag.looksLikeDansBag()) {
                    log.info("Looks like a DANS bag, generating Dataverse ingest metadata");
                    var dansDeposit = dansBagMappingService.readDansDeposit(deposit.getLocation());
                    new DansDepositConverter(dansDeposit, dansBagMappingService, yamlService).run();
                    log.info("Generated Dataverse ingest metadata");
                }
                else {
                    log.info("Does not look like a DANS bag, skipping metadata generation");
                }
                if (onlyConvertDansDeposit) {
                    log.info("Only converting DANS deposit, skipping ingest");
                    continue;
                }
                pid = new BagProcessor(deposit.getId(), bag, dataverseService, utilityServices).run(pid);
                log.info("END processing deposit / bag: {} / {}", deposit.getId(), bag);
            }
            if (onlyConvertDansDeposit) {
                log.info("Only converted DANS deposits, LEAVING CONVERTED DEPOSITS IN PLACE");
            }
            else {
                deposit.moveTo(outputDir.resolve("processed"));
            }
        }
        catch (Exception e) {
            try {
                log.error("Failed to ingest deposit", e);
                deposit.moveTo(outputDir.resolve("failed"));
                status = Status.FAILED;
            }
            catch (IOException ioException) {
                log.error("Failed to move deposit to failed directory", ioException);
            }
        }
    }
}