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
package nl.knaw.dans.ingest.core.service;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.ingest.config.IngestFlowConfig;
import nl.knaw.dans.ingest.core.dataverse.DatasetService;
import nl.knaw.dans.ingest.core.deposit.DepositManager;
import nl.knaw.dans.ingest.core.domain.DepositLocation;
import nl.knaw.dans.ingest.core.domain.OutboxSubDir;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapperFactory;
import nl.knaw.dans.ingest.core.validation.DepositorAuthorizationValidator;
import nl.knaw.dans.lib.dataverse.DataverseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class DepositIngestTaskFactory {

    private final String depositorRole;
    private final DansBagValidator dansBagValidator;
    private final IngestFlowConfig ingestFlowConfig;
    private final DepositManager depositManager;
    private final boolean isMigration;
    private final DepositToDvDatasetMetadataMapperFactory depositToDvDatasetMetadataMapperFactory;
    private final ZipFileHandler zipFileHandler;
    private final DatasetService datasetService;
    private final BlockedTargetService blockedTargetService;
    private final DepositorAuthorizationValidator depositorAuthorizationValidator;
    private final String vaultMetadataKey;

    private final boolean deleteDraftOnFailure;

    public DepositIngestTaskFactory(
        boolean isMigration,
        String depositorRole,
        DansBagValidator dansBagValidator,
        IngestFlowConfig ingestFlowConfig,
        DepositManager depositManager,
        DepositToDvDatasetMetadataMapperFactory depositToDvDatasetMetadataMapperFactory,
        ZipFileHandler zipFileHandler,
        DatasetService datasetService,
        BlockedTargetService blockedTargetService,
        DepositorAuthorizationValidator depositorAuthorizationValidator,
        String vaultMetadataKey) throws IOException, URISyntaxException {
        this.isMigration = isMigration;
        this.depositorRole = depositorRole;
        this.dansBagValidator = dansBagValidator;
        this.ingestFlowConfig = ingestFlowConfig;
        this.depositManager = depositManager;
        this.depositToDvDatasetMetadataMapperFactory = depositToDvDatasetMetadataMapperFactory;
        this.zipFileHandler = zipFileHandler;
        this.datasetService = datasetService;
        this.blockedTargetService = blockedTargetService;
        this.depositorAuthorizationValidator = depositorAuthorizationValidator;
        this.vaultMetadataKey = vaultMetadataKey;
        this.deleteDraftOnFailure = ingestFlowConfig.isDeleteDraftOnFailure();
    }

    public DepositIngestTask createIngestTask(Path depositDir, Path outboxDir, EventWriter eventWriter) throws InvalidDepositException, IOException {
        try {
            var depositLocation = depositManager.readDepositLocation(depositDir);
            return createDepositIngestTask(depositLocation, outboxDir, eventWriter);
        }
        catch (DataverseException e) {
            log.error("Unexpected dataverse error while preparing data for deposit at path {}, moving deposit", depositDir);
            moveDepositToFailed(depositDir, outboxDir);
            throw new InvalidDepositException(e.getMessage(), e);
        }
        catch (InvalidDepositException | IOException e) {
            // the reading of the deposit failed, so we cannot update its internal state. All we can do is move it
            // to the "failed" directory
            log.error("Unable to load deposit properties, considering deposit at path {} to be broken", depositDir);
            moveDepositToFailed(depositDir, outboxDir);
            throw e;
        }
        catch (Throwable e) {
            // if something bad happens while loading the deposit, we want to wrap it into an InvalidDepositException as well
            log.error("Unexpected error occurred while loading deposit at path {}, moving deposit", depositDir);
            moveDepositToFailed(depositDir, outboxDir);
            throw new InvalidDepositException("Unexpected error occurred: " + e.getMessage(), e);
        }
    }

    void moveDepositToFailed(Path depositDir, Path outboxDir) throws IOException {
        var target = outboxDir
            .resolve(OutboxSubDir.FAILED.getValue());

        log.debug("Moving path {} to {}", depositDir, target);
        depositManager.moveDeposit(depositDir, target);
    }

    private DepositIngestTask createDepositIngestTask(DepositLocation depositLocation, Path outboxDir, EventWriter eventWriter) throws IOException, DataverseException {
        var fileExclusionPattern = Optional.ofNullable(ingestFlowConfig.getFileExclusionPattern())
            .map(Pattern::compile)
            .orElse(null);

        var licenses = datasetService.getLicenses();
        log.debug("Licenses retrieved: {}", licenses);

        log.debug("Creating deposit ingest task, isMigration={}, role={}, outboxDir={}", isMigration, depositorRole, outboxDir);
        if (isMigration) {
            return new DepositMigrationTask(
                depositToDvDatasetMetadataMapperFactory,
                depositLocation,
                depositorRole,
                fileExclusionPattern,
                zipFileHandler,
                licenses,
                dansBagValidator,
                outboxDir,
                eventWriter,
                depositManager,
                datasetService,
                blockedTargetService,
                depositorAuthorizationValidator,
                vaultMetadataKey,
                deleteDraftOnFailure
            );
        }
        else {
            return new DepositIngestTask(
                depositToDvDatasetMetadataMapperFactory,
                depositLocation,
                depositorRole,
                fileExclusionPattern,
                zipFileHandler,
                licenses,
                dansBagValidator,
                outboxDir,
                eventWriter,
                depositManager,
                datasetService,
                blockedTargetService,
                depositorAuthorizationValidator,
                vaultMetadataKey,
                deleteDraftOnFailure
            );
        }

    }
}
