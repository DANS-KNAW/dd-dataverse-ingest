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
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.dvingest.core.yaml.tasklog.EditPermissionsLog;
import nl.knaw.dans.lib.dataverse.DataverseException;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class PermissionsEditor {
    private final UUID depositId;
    private final EditPermissions editPermissions;
    private final EditPermissionsLog editPermissionsLog;
    private final DataverseService dataverseService;

    private String pid;

    public void editPermissions(String pid) throws IOException, DataverseException {
        if (editPermissions == null) {
            log.debug("No permissions to edit for deposit {}", depositId);
            return;
        }

        this.pid = pid;
        log.debug("Start updating permissions for deposit {}", depositId);
        deleteRoleAssignments();
        addRoleAssignments();
        log.debug("End updating permissions for deposit {}", depositId);
    }

    private void addRoleAssignments() throws IOException, DataverseException {
        if (editPermissionsLog.getAddRoleAssignments().isCompleted()) {
            log.debug("Adding of role assignments already completed. Skipping addition.");
            return;
        }
        if (editPermissions.getAddRoleAssignments().isEmpty()) {
            log.debug("No role assignments to add. Skipping addition.");
        }
        else {
            log.debug("Start adding {} role assignments for deposit {}", depositId, editPermissions.getAddRoleAssignments().size());
            int numberCompleted = editPermissionsLog.getAddRoleAssignments().getNumberCompleted();
            if (numberCompleted > 0) {
                log.debug("Resuming adding role assignments from index {}", numberCompleted);
                for (int i = numberCompleted; i < editPermissions.getAddRoleAssignments().size(); i++) {
                    var roleAssignment = editPermissions.getAddRoleAssignments().get(i);
                    log.debug("Adding role assignment: {}", roleAssignment);
                    dataverseService.addRoleAssignment(pid, roleAssignment);
                    editPermissionsLog.getAddRoleAssignments().setNumberCompleted(i + 1);
                }
            }
            log.debug("End adding role assignments for deposit {}", depositId);
        }
        editPermissionsLog.getAddRoleAssignments().setCompleted(true);
    }

    private void deleteRoleAssignments() throws IOException, DataverseException {
        if (editPermissionsLog.getDeleteRoleAssignments().isCompleted()) {
            log.debug("Deletion of role assignments already completed. Skipping deletion.");
            return;
        }
        if (editPermissions.getDeleteRoleAssignments().isEmpty()) {
            log.debug("No role assignments to delete. Skipping deletion.");
        }
        else {
            log.debug("Start deleting {} role assignments for deposit {}", depositId, editPermissions.getDeleteRoleAssignments().size());
            for (var roleAssignment : editPermissions.getDeleteRoleAssignments()) {
                log.debug("Deleting role assignment: {}", roleAssignment);
                dataverseService.deleteRoleAssignment(pid, roleAssignment);
            }
            log.debug("End deleting role assignments for deposit {}", depositId);
        }
        editPermissionsLog.getDeleteRoleAssignments().setCompleted(true);
    }
}
