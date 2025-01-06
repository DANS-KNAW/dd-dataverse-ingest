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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDeposit;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.dataverse.DataverseException;

import java.io.IOException;

@Slf4j
@AllArgsConstructor
public class DepositorAuthorizationValidator {
    private final DataverseService dataverseService;
    private final String datasetPublisherRole;
    private final String datasetUpdaterRole;


    public boolean isDatasetUpdateAllowed(DansBagDeposit deposit) {
        if (deposit.isUpdate()) {
            try {
                var doi = deposit.getDataverseDoi();
                var roles = dataverseService.getDatasetRolesFor(deposit.getDepositorUserId(), doi);
                log.debug("Roles for user {} on deposit with doi {}: {}; expecting role {} to be present", deposit.getDepositorUserId(), doi, roles, datasetUpdaterRole);
                return roles.contains(datasetUpdaterRole);
            }
            catch (DataverseException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Deposit is not an update");
    }

    public boolean isDatasetPublicationAllowed(DansBagDeposit deposit) {
        try {
            var roles = dataverseService.getDataverseRolesFor(deposit.getDepositorUserId());
            if (!roles.contains(datasetPublisherRole)) {
                log.debug("Roles for user {}: {}; role {} is not present; publication not allowed", deposit.getDepositorUserId(), roles, datasetPublisherRole);
                return false;
            }
        }
        catch (DataverseException | IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("Roles for user {}: role {} is present; publication allowed", deposit.getDepositorUserId(), datasetPublisherRole);
        return true;
    }
}
