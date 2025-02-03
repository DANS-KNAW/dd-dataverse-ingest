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
import nl.knaw.dans.dvingest.core.yaml.PublishAction;
import nl.knaw.dans.dvingest.core.yaml.ReleaseMigratedAction;
import nl.knaw.dans.dvingest.core.yaml.UpdateAction;
import nl.knaw.dans.dvingest.core.yaml.tasklog.CompletableItem;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class StateUpdater {
    private final UUID depositId;
    private final UpdateAction updateAction;
    private final CompletableItem updateStateLog;

    private final DataverseService dataverseService;

    private String pid;
    private int numberOfFilesInDataset;

    public void updateState(String pid, int numberOfFilesInDataset) throws DataverseException, IOException {
        if (updateStateLog.isCompleted()) {
            log.debug("[{}] State already updated.", depositId);
            return;
        }

        this.pid = pid;
        this.numberOfFilesInDataset = numberOfFilesInDataset;

        if (updateAction instanceof PublishAction) {
            publishVersion(((PublishAction) updateAction).getUpdateType());
        }
        else if (updateAction instanceof ReleaseMigratedAction) {
            releaseMigrated(((ReleaseMigratedAction) updateAction).getReleaseDate());
        }
        updateStateLog.setCompleted(true);
    }

    private void publishVersion(UpdateType updateType) throws DataverseException, IOException {
        log.debug("[{}] Start publishing version; dataset = {}; updateType = {}", depositId, pid, updateType);
        dataverseService.publishDataset(pid, updateType);
        log.debug("[{}] Waiting for dataset to reach released state; dataset = {}", depositId, pid);
        dataverseService.waitForReleasedState(pid, numberOfFilesInDataset);
        log.debug("[{}] Dataset reached released state; dataset = {}", depositId, pid);
        log.debug("[{}] End publishing version; dataset = {}; updateType = {}", depositId, pid, updateType);
    }

    public void releaseMigrated(String date) throws DataverseException, IOException {
        log.debug("[{}] Start releasing migrated dataset with pid and date: {} {}", depositId, pid, date);
        dataverseService.releaseMigratedDataset(pid, date);
        log.debug("[{}] Waiting for dataset to reach released state; dataset = {}", depositId, pid);
        dataverseService.waitForReleasedState(pid, numberOfFilesInDataset);
        log.debug("[{}] Dataset reached released state; dataset = {}", depositId, pid);
        log.debug("[{}] End releasing migrated dataset with pid and date: {} {}", depositId, pid, date);
    }
}
