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
import nl.knaw.dans.dvingest.core.yaml.EditMetadata;
import nl.knaw.dans.dvingest.core.yaml.tasklog.EditMetadataLog;
import nl.knaw.dans.lib.dataverse.DataverseException;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class MetadataEditor {
    private final UUID depositId;
    private final EditMetadata editMetadata;
    private final EditMetadataLog editMetadataLog;
    private final DataverseService dataverseService;

    private String pid;

    public void editMetadata(String pid) throws IOException, DataverseException {
        this.pid = pid;
        if (editMetadata == null) {
            log.debug("[{}] No metadata to edit. Skipping metadata editing.", depositId);
            editMetadataLog.completeAll();
            return;
        }
        log.debug("[{}] Start editing metadata.", depositId);
        addFieldValues();
        replaceFieldValues();
        deleteFieldValues();
        log.debug("[{}] End editing metadata.", depositId);
    }

    private void deleteFieldValues() throws IOException, DataverseException {
        if (editMetadataLog.getDeleteFieldValues().isCompleted()) {
            log.debug("[{}] Deletion of field values already completed.", depositId);
            return;
        }
        if (editMetadata.getDeleteFieldValues().isEmpty()) {
            log.debug("[{}] No field values to delete.", depositId);
        }
        else {
            log.debug("[{}] Start deleting {} field values.", depositId, editMetadata.getDeleteFieldValues().size());
            for (var fieldValue : editMetadata.getDeleteFieldValues()) {
                log.debug("[{}] Deleting field value: {}", depositId, fieldValue.getTypeName());
                dataverseService.deleteDatasetMetadata(pid, editMetadata.getDeleteFieldValues());
            }
            log.debug("[{}] End deleting field values.", depositId);
        }
        editMetadataLog.getDeleteFieldValues().setCompleted(true);
    }

    private void addFieldValues() throws IOException, DataverseException {
        if (editMetadataLog.getAddFieldValues().isCompleted()) {
            log.debug("[{}] Addition of field values already completed.", depositId);
            return;
        }
        if (editMetadata.getAddFieldValues().isEmpty()) {
            log.debug("[{}] No field values to add.", depositId);
        }
        else {
            log.debug("[{}] Start adding {} field values.", depositId, editMetadata.getAddFieldValues().size());
            for (var fieldValue : editMetadata.getAddFieldValues()) {
                log.debug("[{}] Adding field value: {}", depositId, fieldValue.getTypeName());
                dataverseService.editMetadata(pid, editMetadata.getAddFieldValues(), false);
            }
            log.debug("[{}] End adding field values.", depositId);
        }
        editMetadataLog.getAddFieldValues().setCompleted(true);
    }

    private void replaceFieldValues() throws IOException, DataverseException {
        if (editMetadataLog.getReplaceFieldValues().isCompleted()) {
            log.debug("[{}] Replacement of field values already completed.", depositId);
            return;
        }
        if (editMetadata.getReplaceFieldValues().isEmpty()) {
            log.debug("[{}] No field values to replace.", depositId);
        }
        else {
            log.debug("Start replacing field values for deposit {}", depositId);
            for (var fieldValue : editMetadata.getReplaceFieldValues()) {
                log.debug("Replacing field value: {}", fieldValue.getTypeName());
                dataverseService.editMetadata(pid, editMetadata.getReplaceFieldValues(), true);
            }
            log.debug("End replacing field values for deposit {}", depositId);
        }
        editMetadataLog.getReplaceFieldValues().setCompleted(true);
    }
}
