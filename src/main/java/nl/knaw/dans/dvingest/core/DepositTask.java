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
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositSupport;
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

    @Getter
    private Status status = Status.TODO;

    public DepositTask(DataverseIngestDeposit dataverseIngestDeposit, Path outputDir, boolean onlyConvertDansDeposit, DataverseService dataverseService, UtilityServices utilityServices,
        DansBagMappingService dansBagMappingService,
        YamlService yamlService) {
        this.deposit = dansBagMappingService == null ? dataverseIngestDeposit : new DansDepositSupport(dataverseIngestDeposit, dansBagMappingService, yamlService);
        this.dataverseService = dataverseService;
        this.onlyConvertDansDeposit = onlyConvertDansDeposit;
        this.utilityServices = utilityServices;
        this.outputDir = outputDir;
    }

    @Override
    public void run() {
        String pid = deposit.getUpdatesDataset();
        try {
            if (deposit.convertDansDepositIfNeeded() && onlyConvertDansDeposit) {
                log.info("Only converting DANS deposit, LEAVING CONVERTED DEPOSIT IN PLACE");
                return;
            }

            for (DataverseIngestBag bag : deposit.getBags()) {
                log.info("START processing deposit / bag: {} / {}", deposit.getId(), bag);
                pid = BagProcessor.builder()
                    .depositId(deposit.getId())
                    .bag(bag)
                    .dataverseService(dataverseService)
                    .utilityServices(utilityServices)
                    .build()
                    .run(pid);
                log.info("END processing deposit / bag: {} / {}", deposit.getId(), bag);
            }
            deposit.onSuccess(pid);
            deposit.moveTo(outputDir.resolve("processed"));
        }
        catch (Exception e) {
            try {
                log.error("Failed to ingest deposit", e);
                deposit.onFailed(pid);
                deposit.moveTo(outputDir.resolve("failed"));
                status = Status.FAILED;
            }
            catch (IOException ioException) {
                log.error("Failed to move deposit to failed directory", ioException);
            }
        }
    }
}