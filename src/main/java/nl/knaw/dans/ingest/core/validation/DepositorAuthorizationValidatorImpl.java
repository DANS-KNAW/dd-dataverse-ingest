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
package nl.knaw.dans.ingest.core.validation;

import nl.knaw.dans.ingest.core.dataverse.DatasetService;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.exception.DepositorValidatorException;
import nl.knaw.dans.ingest.core.exception.InvalidDepositorRoleException;
import nl.knaw.dans.lib.dataverse.DataverseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DepositorAuthorizationValidatorImpl implements DepositorAuthorizationValidator {
    private static final Logger log = LoggerFactory.getLogger(DepositorAuthorizationValidatorImpl.class);

    private final DatasetService datasetService;
    private final String datasetPublisherRole;
    private final String datasetUpdaterRole;

    public DepositorAuthorizationValidatorImpl(DatasetService datasetService, String datasetPublisherRole, String datasetUpdaterRole) {
        this.datasetService = datasetService;
        this.datasetPublisherRole = datasetPublisherRole;
        this.datasetUpdaterRole = datasetUpdaterRole;
    }

    @Override
    public boolean isDatasetUpdateAllowed(Deposit deposit) throws DepositorValidatorException {
        if (deposit.isUpdate()) {
            try {
                var doi = deposit.getDataverseDoi();
                var roles = datasetService.getDatasetRoleAssignments(deposit.getDepositorUserId(), doi);
                log.debug("Roles for user {} on deposit with doi {}: {}; expecting role {} to be present", deposit.getDepositorUserId(), doi, roles, datasetUpdaterRole);
                return roles.contains(datasetUpdaterRole);
            }
            catch (DataverseException | IOException e) {
                throw new DepositorValidatorException(e);
            }
        }
        throw new DepositorValidatorException("Deposit is not an update");
    }

    public boolean isDatasetPublicationAllowed(Deposit deposit) throws DepositorValidatorException {
        try {
            var roles = datasetService.getDataverseRoleAssignments(deposit.getDepositorUserId());
            if (!roles.contains(datasetPublisherRole)) {
                log.debug("Roles for user {}: {}; role {} is not present; publication not allowed", deposit.getDepositorUserId(), roles, datasetPublisherRole);
                return false;
            }
        }
        catch (DataverseException | IOException e) {
            throw new DepositorValidatorException(e);
        }
        log.debug("Roles for user {}: role {} is present; publication allowed", deposit.getDepositorUserId(), datasetPublisherRole);
        return true;
    }
}
