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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.client.ValidateDansBagService;
import nl.knaw.dans.dvingest.core.DataverseIngestBag;
import nl.knaw.dans.dvingest.core.DataverseIngestDeposit;
import nl.knaw.dans.dvingest.core.Deposit;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDeposit;
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;
import nl.knaw.dans.dvingest.core.dansbag.exception.RejectedDepositException;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class DansDepositSupport implements Deposit {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ValidateDansBagService validateDansBagService;
    private final DansBagMappingService dansBagMappingService;
    private final DataverseService dataverseService;
    private final YamlService yamlService;
    private final DataverseIngestDeposit ingestDataverseIngestDeposit;
    private final boolean isDansDeposit;
    private final boolean isMigration;

    private DansBagDeposit dansDeposit;

    public DansDepositSupport(DataverseIngestDeposit dataverseIngestDeposit, boolean isMigration, ValidateDansBagService validateDansBagService, DansBagMappingService dansBagMappingService,
        DataverseService dataverseService, YamlService yamlService) {
        this.ingestDataverseIngestDeposit = dataverseIngestDeposit;
        this.isMigration = isMigration;
        this.validateDansBagService = validateDansBagService;
        this.dansBagMappingService = dansBagMappingService;
        this.dataverseService = dataverseService;
        this.yamlService = yamlService;
        try {
            this.isDansDeposit = dataverseIngestDeposit.getBags().get(0).looksLikeDansBag();
        }
        catch (IOException e) {
            throw new RuntimeException("Error reading bags", e);
        }
        try {
            Files.deleteIfExists(dataverseIngestDeposit.getBags().get(0).getDataDir().resolve("original-metadata.zip"));
        }
        catch (IOException e) {
            throw new RuntimeException("Error deleting original-metadata.zip", e);
        }
    }

    @Override
    public boolean convertDansDepositIfNeeded() {
        if (isDansDeposit && dansDeposit == null) {
            log.info("Converting deposit to Dataverse ingest metadata");
            try {
                var updatesDataset = dansBagMappingService.getUpdatesDataset(ingestDataverseIngestDeposit.getLocation());
                DatasetVersion currentMetadata = null;
                if (updatesDataset != null) {
                    ingestDataverseIngestDeposit.updateProperties(Map.of(UPDATES_DATASET_KEY, updatesDataset));
                    currentMetadata = dataverseService.getDatasetMetadata(updatesDataset);
                }
                dansDeposit = dansBagMappingService.readDansDeposit(ingestDataverseIngestDeposit.getLocation());
                new DansDepositConverter(dansDeposit, updatesDataset, currentMetadata, dansBagMappingService, yamlService).run();
                log.info("Conversion successful");
                return true;
            }
            catch (IOException | InvalidDepositException | DataverseException e) {
                throw new RuntimeException("Error converting deposit to Dataverse ingest metadata", e);
            }
        }
        return false;
    }

    @Override
    public String getUpdatesDataset() {
        return ingestDataverseIngestDeposit.getUpdatesDataset();
    }

    @Override
    public List<DataverseIngestBag> getBags() throws IOException {
        return ingestDataverseIngestDeposit.getBags();
    }

    @Override
    public UUID getId() {
        return ingestDataverseIngestDeposit.getId();
    }

    @Override
    public Path getLocation() {
        return ingestDataverseIngestDeposit.getLocation();
    }

    @Override
    public void onSuccess(@NonNull String pid, String message) {
        try {
            var bag = ingestDataverseIngestDeposit.getBags().get(0);
            var action = bag.getUpdateState().getAction();

            if (action.startsWith("publish")) {
                handlePublishAction(pid);
            }
            else if (action.equals("submit-for-review")) {
                handleSubmitForReviewAction(pid);
            }
            else {
                throw new RuntimeException("Unknown update action: " + action);
            }
        }
        catch (IOException | ConfigurationException e) {
            throw new RuntimeException("Error processing onSuccess", e);
        }
    }

    private void handlePublishAction(String pid) {
        try {
            var nbn = dataverseService.getDatasetUrnNbn(pid);
            var newProps = new HashMap<String, String>();
            newProps.put("state.label", "PUBLISHED");
            newProps.put("state.description", "The dataset is published");
            if (!isMigration) {
                newProps.put("identifier.doi", pid);
                newProps.put("identifier.urn", nbn);
            }
            ingestDataverseIngestDeposit.updateProperties(newProps);
        }
        catch (IOException | DataverseException e) {
            throw new RuntimeException("Error getting URN:NBN", e);
        }
    }

    private void handleSubmitForReviewAction(String pid) {
        var newProps = new HashMap<String, String>();
        newProps.put("state.label", "SUBMITTED");
        newProps.put("state.description", "The dataset is submitted for review");
        if (!isMigration) {
            newProps.put("identifier.doi", pid);
        }
        ingestDataverseIngestDeposit.updateProperties(newProps);
    }

    @Override
    public void onFailed(String pid, String message) {
        ingestDataverseIngestDeposit.onFailed(pid, message);
    }

    @Override
    public void onRejected(String pid, String message) {
        ingestDataverseIngestDeposit.onRejected(pid, message);
    }

    @Override
    public void moveTo(Path processed) throws IOException {
        ingestDataverseIngestDeposit.moveTo(processed);
    }

    @Override
    public void validate() {
        if (isDansDeposit) {
            log.debug("Validating DANS deposit");
            try {
                var result = validateDansBagService.validate(ingestDataverseIngestDeposit.getBags().get(0).getLocation().toAbsolutePath());

                var isCompliant = result.getIsCompliant();
                if (isCompliant == null) {
                    throw new RuntimeException("Validation result is null");
                }
                if (!result.getIsCompliant()) {
                    throw new RejectedDepositException(ingestDataverseIngestDeposit, objectMapper.writeValueAsString(result));
                }
                log.debug("Validation successful. Bag is compliant.");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
