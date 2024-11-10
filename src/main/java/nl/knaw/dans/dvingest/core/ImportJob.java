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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.api.ImportCommandDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto.StatusEnum;
import nl.knaw.dans.lib.dataverse.DataverseClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ImportJob implements Runnable {
    @NonNull
    @Getter
    private final ImportCommandDto importCommand;
    @NonNull
    private final Path outputDir;
    @NonNull
    private final DataverseClient dataverseClient;
    @NonNull
    private final CompletionHandler completionHandler;

    @Getter
    private final ImportJobStatusDto status = new ImportJobStatusDto();

    public static interface CompletionHandler {
        void handle(ImportJob job);
    }

    @Override
    public void run() {
        try {
            log.debug("Starting import job: {}", importCommand);
            status.setStatus(StatusEnum.RUNNING);
            List<Deposit> deposits = new ArrayList<>();

            // Build deposit list, todo: ordered
            if (importCommand.getSingleObject()) {
                deposits.add(new Deposit(Path.of(importCommand.getPath())));
            }
            else {
                try (var depositPaths = Files.list(Path.of(importCommand.getPath()))) {
                    depositPaths.forEach(p -> deposits.add(new Deposit(p)));
                }
            }

            initOutputDir();

            // Process deposits
            for (Deposit deposit : deposits) {
                log.info("START Processing deposit: {}", deposit.getId());
                new IngestTask(deposit, dataverseClient, outputDir).run();
                log.info("END Processing deposit: {}", deposit.getId());
                // TODO: record number of processed/rejected/failed deposits in ImportJob status
            }

            status.setStatus(StatusEnum.DONE);
        }
        catch (Exception e) {
            log.error("Failed to process import job", e);
            status.setStatus(StatusEnum.FAILED);
        } finally {
            completionHandler.handle(this);
        }
    }

    private void initOutputDir() {
        log.debug("Initializing output directory: {}", outputDir);
        createDirectoryIfNotExists(outputDir);
        createDirectoryIfNotExists(outputDir.resolve("processed"));
        createDirectoryIfNotExists(outputDir.resolve("failed"));
        createDirectoryIfNotExists(outputDir.resolve("rejected"));
        if (!importCommand.getSingleObject()) {
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
}
