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
package nl.knaw.dans.dvingest.core.bagprocessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.UpdateState;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class StateUpdater {
    private final UUID depositId;
    private final UpdateState updateState;
    private final DataverseService dataverseService;

    private String pid;

    public void updateState(String pid) throws DataverseException, IOException {
        this.pid = pid;
        if (updateState == null) {
            log.debug("No update state found. Skipping update state processing.");
            return;
        }
        if ("publish-major".equals(updateState.getAction())) {
            publishVersion(UpdateType.major);
        }
        else if ("publish-minor".equals(updateState.getAction())) {
            publishVersion(UpdateType.minor);
        }
        else if ("submit-for-review".equals(updateState.getAction())) {
            // TODO: Implement submit for review
            throw new UnsupportedOperationException("Submit for review not yet implemented");
        }
        else {
            throw new IllegalArgumentException("Unknown update state action: " + updateState.getAction());
        }
    }

    private void publishVersion(UpdateType updateType) throws DataverseException, IOException {
        log.debug("Start publishing version for deposit {}", depositId);
        dataverseService.publishDataset(pid, updateType);
        dataverseService.waitForState(pid, "RELEASED");
        log.debug("End publishing version for deposit {}", depositId);
    }

}
