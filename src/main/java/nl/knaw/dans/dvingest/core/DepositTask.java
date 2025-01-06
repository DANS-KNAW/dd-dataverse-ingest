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
import nl.knaw.dans.dvingest.core.bagprocessor.BagProcessorFactory;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositSupportFactory;
import nl.knaw.dans.dvingest.core.dansbag.exception.RejectedDepositException;

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
    private final BagProcessorFactory bagProcessorFactory;

    @Getter
    private Status status = Status.TODO;

    public DepositTask(DataverseIngestDeposit dataverseIngestDeposit, Path outputDir, boolean onlyConvertDansDeposit, BagProcessorFactory bagProcessorFactory,
        DansDepositSupportFactory dansDepositSupportFactory) {
        this.deposit = dansDepositSupportFactory.addDansDepositSupportIfEnabled(dataverseIngestDeposit);
        this.outputDir = outputDir;
        this.onlyConvertDansDeposit = onlyConvertDansDeposit;
        this.bagProcessorFactory = bagProcessorFactory;
    }

    @Override
    public void run() {
        String pid = null;
        try {
            deposit.validate();
            if (deposit.convertDansDepositIfNeeded() && onlyConvertDansDeposit) {
                log.info("Only converting DANS deposit, LEAVING CONVERTED DEPOSIT IN PLACE");
                return;
            }
            deposit.checkAuthorized();
            pid = deposit.getUpdatesDataset();

            for (DataverseIngestBag bag : deposit.getBags()) {
                log.info("START processing deposit / bag: {} / {}", deposit.getId(), bag);
                pid = bagProcessorFactory.createBagProcessor(deposit.getId(), bag).run(pid);
                log.info("END processing deposit / bag: {} / {}", deposit.getId(), bag);
            }
            deposit.onSuccess(pid, "Deposit processed successfully");
            deposit.moveTo(outputDir.resolve("processed"));
        }
        // TODO: move RejectedDepositException to a package that is not specific to DANS
        catch (RejectedDepositException e) {
            try {
                log.error("Deposit rejected", e);
                deposit.onRejected(pid, e.getMessage());
                deposit.moveTo(outputDir.resolve("rejected"));
                status = Status.REJECTED;
            }
            catch (Exception e2) {
                log.error("Failed to move deposit to rejected directory", e2);
            }
        }
        catch (Exception e) {
            try {
                log.error("Failed to ingest deposit", e);
                deposit.onFailed(pid, e.getMessage());
                deposit.moveTo(outputDir.resolve("failed"));
                status = Status.FAILED;
            }
            catch (IOException ioException) {
                log.error("Failed to move deposit to failed directory", ioException);
            }
        }
    }
}