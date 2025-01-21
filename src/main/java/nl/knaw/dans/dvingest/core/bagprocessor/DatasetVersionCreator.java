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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.exception.RejectedDepositException;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.Expect;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.dvingest.core.yaml.tasklog.CompletableItem;
import nl.knaw.dans.dvingest.core.yaml.tasklog.InitLog;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
public class DatasetVersionCreator {
    @NonNull
    private final UUID depositId;
    @NonNull
    private final DataverseService dataverseService;
    private final Init init;
    private final Dataset dataset;
    private final InitLog initLog;
    private final CompletableItem datasetLog;

    public String createDatasetVersion(String targetPid) throws IOException, DataverseException {
        if (init != null && init.getExpect() != null) {
            checkExpectations(init.getExpect(), targetPid);
        }
        else {
            initLog.getExpect().completeAll();
        }

        String pid = createDatasetIfNeeded(targetPid);
        if (dataset != null) {
            updateDataset(pid);
        }
        datasetLog.setCompleted(true);
        return pid;
    }

    private void checkExpectations(@NonNull Expect expect, String targetPid) throws DataverseException, IOException {
        log.debug("Start checking expectations for deposit {}", depositId);
        if (expect.getState() != null && targetPid != null && !initLog.getExpect().getState().isCompleted()) {
            checkDatasetState(expect, targetPid);
        }
        else {
            initLog.getExpect().getState().setCompleted(true);
        }

        if (expect.getDataverseRoleAssignment() != null && !initLog.getExpect().getDataverseRoleAssignment().isCompleted()) {
            checkDataverseRoleAssignment(expect);
        }
        else {
            initLog.getExpect().getDataverseRoleAssignment().setCompleted(true);
        }

        if (expect.getDatasetRoleAssignment() != null && targetPid != null && !initLog.getExpect().getDatasetRoleAssignment().isCompleted()) {
            checkDatasetRoleAssignment(expect, targetPid);
        }
        else {
            initLog.getExpect().getDatasetRoleAssignment().setCompleted(true);
        }
        log.debug("End checking expectations for deposit {}", depositId);
    }

    private void checkDatasetState(@NonNull Expect expect, String targetPid) throws DataverseException, IOException {
        var actualState = dataverseService.getDatasetState(targetPid);
        if (expect.getState().name().equalsIgnoreCase(actualState)) {
            log.debug("Expected state {} found for dataset {}", expect.getState(), targetPid);
            initLog.getExpect().getState().setCompleted(true);
        }
        else {
            throw new IllegalStateException("Expected state " + expect.getState() + " but found " + actualState + " for dataset " + targetPid);
        }
    }

    private void checkDataverseRoleAssignment(@NonNull Expect expect) throws DataverseException, IOException {
        var actualRoleAssignments = dataverseService.getRoleAssignmentsOnDataverse("root");
        if (contains(actualRoleAssignments, expect.getDataverseRoleAssignment(), true)) {
            log.debug("Expected role assignment found for dataverse root");
            initLog.getExpect().getDataverseRoleAssignment().setCompleted(true);
        }
        else {
            throw new RejectedDepositException(depositId, String.format("User '%s' does not have the expected role '%s' on dataverse root", expect.getDataverseRoleAssignment().getAssignee(),
                expect.getDataverseRoleAssignment().getRole()));
        }
    }

    private void checkDatasetRoleAssignment(@NonNull Expect expect, String targetPid) throws DataverseException, IOException {
        var actualRoleAssignments = dataverseService.getRoleAssignmentsOnDataset(targetPid);
        if (contains(actualRoleAssignments, expect.getDatasetRoleAssignment(), false)) {
            log.debug("Expected role assignment found for dataset {}", targetPid);
            initLog.getExpect().getDatasetRoleAssignment().setCompleted(true);
        }
        else {
            throw new RejectedDepositException(depositId, String.format("User '%s' does not have the expected role '%s' on dataset %s", expect.getDatasetRoleAssignment().getAssignee(),
                expect.getDatasetRoleAssignment().getRole(), targetPid));
        }
    }

    private boolean contains(List<RoleAssignmentReadOnly> actualRoleAssignments, RoleAssignment expectedRoleAssignment, boolean authenticatedUserSuffices) {
        return actualRoleAssignments.stream()
            .anyMatch(ra -> assigneeCorrect(ra, expectedRoleAssignment, authenticatedUserSuffices) && roleCorrect(ra, expectedRoleAssignment));
    }

    private boolean assigneeCorrect(RoleAssignmentReadOnly ra, RoleAssignment expectedRoleAssignment, boolean authenticatedUserSuffices) {
        return ra.getAssignee().equals(expectedRoleAssignment.getAssignee()) || ra.getAssignee().equals(":authenticated-users") && authenticatedUserSuffices;
    }

    private boolean roleCorrect(RoleAssignmentReadOnly ra, RoleAssignment expectedRoleAssignment) {
        return ra.get_roleAlias().equals(expectedRoleAssignment.getRole());
    }

    private String createDatasetIfNeeded(String targetPid) throws IOException, DataverseException {
        if (initLog.getCreate().isCompleted()) {
            log.debug("Create task already completed for deposit {}", depositId);
            return initLog.getTargetPid();
        }

        String pid;
        if (targetPid == null) {
            log.debug("Start creating dataset for deposit {}", depositId);
            if (dataset == null) {
                throw new IllegalArgumentException("Must have dataset metadata to create a new dataset.");
            }
            pid = createOrImportDataset();
        }
        else {
            log.debug("Target PID provided, dataset does not need to be created for deposit {}", depositId);
            pid = targetPid;

        }
        initLog.getCreate().setCompleted(true);
        initLog.setTargetPid(pid);
        return pid;
    }

    private String createOrImportDataset() throws IOException, DataverseException {
        String importPid = getImportPid();
        return (importPid != null) ? importDataset(importPid) : createDataset();
    }

    private String getImportPid() {
        return init != null && init.getCreate() != null ? init.getCreate().getImportPid() : null;
    }

    private @NonNull String importDataset(String pid) throws IOException, DataverseException {
        log.debug("Start importing dataset for deposit {}", depositId);
        dataverseService.importDataset(pid, dataset);
        log.debug("End importing dataset for deposit {}", depositId);
        return pid;
    }

    private @NonNull String createDataset() throws IOException, DataverseException {
        log.debug("Start creating dataset for deposit {}", depositId);
        var pid = dataverseService.createDataset(dataset);
        log.debug("End creating dataset for deposit {}", depositId);
        return pid;
    }

    private void updateDataset(String pid) throws IOException, DataverseException {
        log.debug("Start updating dataset for deposit {}", depositId);
        dataverseService.updateMetadata(pid, dataset.getDatasetVersion());
        log.debug("End updating dataset for deposit {}", depositId);
    }
}