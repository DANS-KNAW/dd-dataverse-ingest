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

import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.dvingest.core.yaml.Create;
import nl.knaw.dans.dvingest.core.yaml.Expect.State;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.dvingest.core.yaml.InitRoot;
import nl.knaw.dans.dvingest.core.yaml.actionlog.CompletableItem;
import nl.knaw.dans.dvingest.core.yaml.actionlog.InitLog;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatasetVersionCreatorTest {
    private final YamlService yamlService = new YamlServiceImpl();
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(dataverseServiceMock);
    }

    @Test
    public void createDatasetVersion_creates_a_new_dataset_if_targetPid_is_null() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var dataset = new Dataset();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        Mockito.when(dataverseServiceMock.createDataset(dataset)).thenReturn("pid");
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, dataset, initLog, datasetLog);

        // When
        datasetVersionCreator.createDatasetVersion(null);

        // Then
        Mockito.verify(dataverseServiceMock).createDataset(dataset);
        Mockito.verify(dataverseServiceMock).updateMetadata("pid", dataset.getDatasetVersion());
        assertThat(initLog.getCreate().isCompleted()).isTrue();
        assertThat(datasetLog.isCompleted()).isTrue();
    }

    @Test
    public void createDatasetVersion_updates_the_dataset_if_targetPid_is_not_null() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var dataset = new Dataset();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        Mockito.when(dataverseServiceMock.createDataset(dataset)).thenReturn("pid-new");
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, dataset, initLog, datasetLog);

        // When
        datasetVersionCreator.createDatasetVersion("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).createDataset(Mockito.any());
        Mockito.verify(dataverseServiceMock).updateMetadata("pid", dataset.getDatasetVersion());
        assertThat(initLog.getCreate().isCompleted()).isTrue();
        assertThat(datasetLog.isCompleted()).isTrue();
    }

    @Test
    public void createDatasetVersion_throws_IllegalArgumentException_if_dataset_is_null() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        Mockito.when(dataverseServiceMock.createDataset(Mockito.any())).thenReturn("pid-new");
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, null, initLog, datasetLog);

        // When
        // Then
        assertThatThrownBy(() -> datasetVersionCreator.createDatasetVersion(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Must have dataset metadata to create a new dataset.");
        assertThat(initLog.getExpect().getState().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isTrue();
        assertThat(initLog.getCreate().isCompleted()).isFalse();
        assertThat(datasetLog.isCompleted()).isFalse();
    }

    @Test
    public void createDatasetVersion_is_noop_if_dataset_is_null_and_targetPid_is_not_null() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, null, initLog, datasetLog);

        // When
        datasetVersionCreator.createDatasetVersion("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).createDataset(Mockito.any());
        Mockito.verify(dataverseServiceMock, Mockito.never()).updateMetadata(Mockito.anyString(), Mockito.any());
        assertThat(initLog.getCreate().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getState().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isTrue();
        assertThat(datasetLog.isCompleted()).isTrue();

    }

    @Test
    public void ctor_throws_NullPointerException_if_dataverseService_is_null() {
        // Given
        var depositId = UUID.randomUUID();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        // When
        // Then
        assertThatThrownBy(() -> new DatasetVersionCreator(depositId, null, null, new Dataset(), initLog, datasetLog))
            .isInstanceOf(NullPointerException.class);
        assertThat(initLog.getCreate().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getState().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isFalse();
        assertThat(datasetLog.isCompleted()).isFalse();
    }

    @Test
    public void ctor_throws_NullPointerException_if_depositId_is_null() {
        // Given
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        // When
        // Then
        assertThatThrownBy(() -> new DatasetVersionCreator(null, dataverseServiceMock, null, new Dataset(), new InitLog(), new CompletableItem()))
            .isInstanceOf(NullPointerException.class);
        assertThat(initLog.getCreate().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getState().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isFalse();
        assertThat(datasetLog.isCompleted()).isFalse();
    }

    @Test
    public void createDatasetVersion_throws_IllegalStateException_if_state_is_draft_while_released_expected() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var initRoot = yamlService.readYamlFromString("""
            init:
              expect:
                 state: released
            """, InitRoot.class);

        var dataset = new Dataset();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        Mockito.when(dataverseServiceMock.getDatasetState("pid")).thenReturn("draft");
        var datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, initRoot.getInit(), dataset, initLog, datasetLog);

        // When
        // Then
        assertThatThrownBy(() -> datasetVersionCreator.createDatasetVersion("pid"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expected state released but found draft for dataset pid");
        assertThat(initLog.getCreate().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getState().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isFalse();
        assertThat(datasetLog.isCompleted()).isFalse();
    }

    @Test
    public void createDatasetVersion_throws_IllegalStateException_if_state_is_released_while_draft_expected() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var initRoot = yamlService.readYamlFromString("""
            init:
              expect:
                 state: draft
            """, InitRoot.class);
        var dataset = new Dataset();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        Mockito.when(dataverseServiceMock.getDatasetState("pid")).thenReturn("released");
        var datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, initRoot.getInit(), dataset, initLog, datasetLog);

        // When
        // Then
        assertThatThrownBy(() -> datasetVersionCreator.createDatasetVersion("pid"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expected state draft but found released for dataset pid");
        assertThat(initLog.getCreate().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getState().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isFalse();
        assertThat(datasetLog.isCompleted()).isFalse();
    }

    @Test
    public void createDatasetVersion_throws_IllegalStateException_if_dataset_found_while_expected_absent() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var initRoot = yamlService.readYamlFromString("""
            init:
              expect:
                 state: absent
            """, InitRoot.class);

        var dataset = new Dataset();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        Mockito.when(dataverseServiceMock.getDatasetState("pid")).thenReturn("draft");
        var datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, initRoot.getInit(), dataset, initLog, datasetLog);

        // When
        // Then
        assertThatThrownBy(() -> datasetVersionCreator.createDatasetVersion("pid"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expected state absent but found for dataset pid");
        assertThat(initLog.getCreate().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getState().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isFalse();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isFalse();
        assertThat(datasetLog.isCompleted()).isFalse();
    }

    @Test
    public void createDatasetVersion_imports_dataset_if_importPid_is_specified() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        var initRoot = yamlService.readYamlFromString("""
            init:
              create:
                importPid: pid-import
            """, InitRoot.class);
        var dataset = new Dataset();
        var initLog = new InitLog();
        var datasetLog = new CompletableItem();
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, initRoot.getInit(), dataset, initLog, datasetLog);

        // When
        datasetVersionCreator.createDatasetVersion(null);

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).createDataset(dataset);
        Mockito.verify(dataverseServiceMock).importDataset("pid-import", dataset);
        Mockito.verify(dataverseServiceMock).updateMetadata("pid-import", dataset.getDatasetVersion());
        assertThat(initLog.getCreate().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getState().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getDataverseRoleAssignment().isCompleted()).isTrue();
        assertThat(initLog.getExpect().getDatasetRoleAssignment().isCompleted()).isTrue();
        assertThat(datasetLog.isCompleted()).isTrue();
    }

//    @Test


}
