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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.api.ImportCommandDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto.StatusEnum;
import nl.knaw.dans.dvingest.core.service.DansBagMappingService;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.service.YamlService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

@Slf4j
@Builder
public class ImportJob implements Runnable {
    @NonNull
    @Getter
    private final ImportCommandDto importCommand;
    @NonNull
    private final Path outputDir;
    @NonNull
    private final DataverseService dataverseService;
    @NonNull
    private final UtilityServices utilityServices;
    @NonNull
    private final YamlService yamlService;
    @NonNull
    private final DansBagMappingService dansBagMappingService;

    @Getter
    private final ImportJobStatusDto status = new ImportJobStatusDto();

    private ImportJob(@NonNull ImportCommandDto importCommand,
        @NonNull Path outputDir,
        @NonNull DataverseService dataverseService,
        @NonNull UtilityServices utilityServices,
        @NonNull YamlService yamlService,
        @NonNull DansBagMappingService dansBagMappingService) {
        this.importCommand = importCommand;
        this.outputDir = outputDir;
        this.dataverseService = dataverseService;
        this.utilityServices = utilityServices;
        this.yamlService = yamlService;
        this.dansBagMappingService = dansBagMappingService;
    }

    @Override
    public void run() {
        try {
            log.debug("Starting import job: {}", importCommand);
            status.setStatus(StatusEnum.RUNNING);
            var deposits = new TreeSet<Deposit>();

            // Build deposit list, todo: ordered
            if (importCommand.getSingleObject()) {
                deposits.add(new Deposit(Path.of(importCommand.getPath()), yamlService));
            }
            else {
                try (var depositPaths = Files.list(Path.of(importCommand.getPath()))) {
                    depositPaths.filter(Files::isDirectory).forEach(p -> deposits.add(new Deposit(p, yamlService)));
                }
            }

            initOutputDir();

            // Process deposits
            for (Deposit deposit : deposits) {
                log.info("START Processing deposit: {}", deposit.getId());
                var task = new DepositTask(deposit, dataverseService, utilityServices, outputDir, dansBagMappingService, yamlService);
                task.run();
                log.info("END Processing deposit: {}", deposit.getId());
                // TODO: record number of processed/rejected/failed deposits in ImportJob status
            }

            status.setStatus(StatusEnum.DONE);
        }
        catch (Exception e) {
            log.error("Failed to process import job", e);
            status.setStatus(StatusEnum.FAILED);
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
