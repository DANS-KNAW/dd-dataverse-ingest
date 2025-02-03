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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.api.ImportCommandDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto.StatusEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

@Slf4j
public class ImportJob implements Runnable {
    @NonNull
    @Getter
    private final ImportCommandDto importCommand;
    @NonNull
    private final Path outputDir;
    private final boolean onlyConvertDansDeposit;
    private final DataverseIngestDepositFactory depositFactory;
    private final DepositTaskFactory depositTaskFactory;

    @Getter
    private final ImportJobStatusDto status;


    private boolean cancelled = false;

    public ImportJob(ImportCommandDto importCommand, String path, Path outputDir, boolean onlyConvertDansDeposit, DataverseIngestDepositFactory depositFactory, DepositTaskFactory depositTaskFactory) {
        this.importCommand = importCommand;
        this.outputDir = outputDir;
        this.onlyConvertDansDeposit = onlyConvertDansDeposit;
        this.depositFactory = depositFactory;
        this.depositTaskFactory = depositTaskFactory;
        this.status = new ImportJobStatusDto().status(StatusEnum.PENDING).path(path).singleObject(importCommand.getSingleObject());
    }

    public void cancel() {
        cancelled = true;
    }

    @Override
    public void run() {
        try {
            log.debug("Starting import job: {}", importCommand);
            status.setStatus(StatusEnum.RUNNING);
            var deposits = createDataverseIngestDeposits();
            initOutputDir();
            processDeposits(deposits);
        }
        catch (Exception e) {
            log.error("Failed to process import job", e);
            status.setStatus(StatusEnum.FAILED);
            status.setMessage(e.getMessage());
        }
    }

    private TreeSet<DataverseIngestDeposit> createDataverseIngestDeposits() throws IOException {
            var deposits = new TreeSet<DataverseIngestDeposit>();

            if (importCommand.getSingleObject()) {
                deposits.add(depositFactory.createDataverseIngestDeposit(Path.of(importCommand.getPath())));
            }
            else {
                try (var depositPaths = Files.list(Path.of(importCommand.getPath()))) {
                    depositPaths.filter(Files::isDirectory)
                        .sorted()
                        .map(depositFactory::createDataverseIngestDeposit)
                        .forEach(deposits::add);
                }
            }
        return deposits;
    }

    private void initOutputDir() {
        log.debug("Initializing output directory: {}", outputDir);
        createDirectoryIfNotExists(outputDir);
        createDirectoryIfNotExists(outputDir.resolve("processed"));
        createDirectoryIfNotExists(outputDir.resolve("failed"));
        createDirectoryIfNotExists(outputDir.resolve("rejected"));
        if (!importCommand.getSingleObject() && !importCommand.getContinueBatch()) {
            checkDirectoryEmpty(outputDir.resolve("processed"));
            checkDirectoryEmpty(outputDir.resolve("failed"));
            checkDirectoryEmpty(outputDir.resolve("rejected"));
        }
    }

    private void createDirectoryIfNotExists(Path path) {
        if (!path.toFile().exists()) {
            if (!path.toFile().mkdirs()) {
                throw new IllegalStateException("Failed to create directory: " + path);
            }
        }
    }

    private void checkDirectoryEmpty(Path path) {
        try (var stream = Files.list(path)) {
            if (stream.findAny().isPresent()) {
                throw new IllegalStateException("Directory not empty: " + path);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to check directory: " + path, e);
        }
    }

    private void processDeposits(TreeSet<DataverseIngestDeposit> deposits) {
        for (DataverseIngestDeposit dataverseIngestDeposit : deposits) {
            if (cancelled) {
                log.info("Import job cancelled");
                status.setMessage("Import job cancelled");
                status.setStatus(StatusEnum.DONE);
                return;
            }
            else {
                log.info("[{}] START Processing deposit.", dataverseIngestDeposit.getId());
                var task = depositTaskFactory.createDepositTask(dataverseIngestDeposit, outputDir, onlyConvertDansDeposit);
                task.run();
                log.info("[{}] END Processing deposit.", dataverseIngestDeposit.getId());
                // TODO: record number of processed/rejected/failed deposits in ImportJob status
            }
        }
        status.setMessage("Import job completed");
        status.setStatus(StatusEnum.DONE);
    }
}
