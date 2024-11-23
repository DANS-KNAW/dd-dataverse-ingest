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
package nl.knaw.dans.dvingest.core.service;

import lombok.AllArgsConstructor;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Optional;

@AllArgsConstructor
public class DansBagMappingServiceImpl implements DansBagMappingService {
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");

    private final DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper;
    private final DataverseService datasetService;

    @Override
    public Dataset getDatasetMetadataFromDansDeposit(Deposit dansDeposit) {
        return depositToDvDatasetMetadataMapper.toDataverseDataset(dansDeposit.getDdm(),
            dansDeposit.getOtherDoiId(),
            getDateOfDeposit(dansDeposit).orElse(null),
            getDatasetContact(dansDeposit).orElse(null), // But null is never actually used, because an exception is thrown if contact is not found
            dansDeposit.getVaultMetadata(),
            dansDeposit.getDepositorUserId(),
            dansDeposit.restrictedFilesPresent(),
            dansDeposit.getHasOrganizationalIdentifier(),
            dansDeposit.getHasOrganizationalIdentifierVersion());
    }

    Optional<String> getDateOfDeposit(Deposit dansDeposit) {
        if (dansDeposit.isUpdate()) {
            return Optional.empty(); // See for implementation CIT025B in DatasetUpdater
        }
        else {
            return Optional.of(yyyymmddPattern.print(DateTime.now())); // CIT025B
        }
    }

    Optional<AuthenticatedUser> getDatasetContact(Deposit dansDeposit) {
        return Optional.ofNullable(dansDeposit.getDepositorUserId())
            .filter(StringUtils::isNotBlank)
            .map(userId -> datasetService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Unable to fetch user with id " + userId)));
    }
}
