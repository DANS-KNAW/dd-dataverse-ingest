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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.client.ValidateDansBagService;
import nl.knaw.dans.dvingest.core.DataverseIngestBag;
import nl.knaw.dans.dvingest.core.DataverseIngestDeposit;
import nl.knaw.dans.dvingest.core.Deposit;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.ingest.core.domain.DepositState;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.exception.RejectedDepositException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
public class DansDepositSupport implements Deposit {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ValidateDansBagService validateDansBagService;
    private final DansBagMappingService dansBagMappingService;
    private final YamlService yamlService;
    private final DataverseIngestDeposit ingestDataverseIngestDeposit;
    private final boolean isDansDeposit;

    private nl.knaw.dans.ingest.core.domain.Deposit dansDeposit;

    public DansDepositSupport(ValidateDansBagService validateDansBagService, DataverseIngestDeposit dataverseIngestDeposit, DansBagMappingService dansBagMappingService, YamlService yamlService) {
        this.validateDansBagService = validateDansBagService;
        this.ingestDataverseIngestDeposit = dataverseIngestDeposit;
        this.dansBagMappingService = dansBagMappingService;
        this.yamlService = yamlService;
        try {
            this.isDansDeposit = dataverseIngestDeposit.getBags().get(0).looksLikeDansBag();
        }
        catch (IOException e) {
            throw new RuntimeException("Error reading bags", e);
        }
    }

    @Override
    public boolean convertDansDepositIfNeeded() {
        if (isDansDeposit && dansDeposit == null) {
            log.info("Converting deposit to Dataverse ingest metadata");
            try {
                dansDeposit = dansBagMappingService.readDansDeposit(ingestDataverseIngestDeposit.getLocation());
                new DansDepositConverter(dansDeposit, dansBagMappingService, yamlService).run();
                log.info("Conversion successful");
                return true;
            }
            catch (IOException | InvalidDepositException e) {
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
    public void onSuccess(String pid) {
        if (dansDeposit == null) {
            return;
        }
        try {
            var bag = ingestDataverseIngestDeposit.getBags().get(0);
            var action = bag.getUpdateState().getAction();
            if (action.startsWith("publish")) {
                dansBagMappingService.updateDepositStatus(dansDeposit, DepositState.PUBLISHED, pid);
            }
            else if (action.equals("submit-for-review")) {
                dansBagMappingService.updateDepositStatus(dansDeposit, DepositState.SUBMITTED, pid);
            }
            else {
                throw new RuntimeException("Unknown update action: " + action);
            }

        }
        catch (IOException e) {
            throw new RuntimeException("Error reading bag", e);
        }
        catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFailed(String pid) {
        dansBagMappingService.updateDepositStatus(dansDeposit, DepositState.FAILED, pid);
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
                // TODO: get the bag from the deposit, but we cannot use dansDeposit yet, because conversion can only happen after validation

                var depositLocation = ingestDataverseIngestDeposit.getLocation();
                var result = validateDansBagService.validate(bag.);

                var isCompliant = result.getIsCompliant();
                if (isCompliant == null) {
                    throw new RuntimeException("Validation result is null");
                }
                if (!result.getIsCompliant()) {
                    throw new RejectedDepositException(dansDeposit, objectMapper.writeValueAsString(result));
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
