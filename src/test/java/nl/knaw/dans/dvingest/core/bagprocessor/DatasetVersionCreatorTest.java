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
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatasetVersionCreatorTest {
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(dataverseServiceMock);
    }

    @Test
    public void createDatasetVersion_creates_a_new_dataset_if_targetPid_is_null() throws Exception{
        // Given
        var depositId = UUID.randomUUID();
        var dataset = new Dataset();
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, dataset);

        // When
        datasetVersionCreator.createDatasetVersion(null);

        // Then
        Mockito.verify(dataverseServiceMock).createDataset(dataset);
        Mockito.verify(dataverseServiceMock, Mockito.never()).updateMetadata(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void createDatasetVersion_updates_the_dataset_if_targetPid_is_not_null() throws Exception{
        // Given
        var depositId = UUID.randomUUID();
        var dataset = new Dataset();
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null,  dataset);

        // When
        datasetVersionCreator.createDatasetVersion("pid");

        // Then
        Mockito.verify(dataverseServiceMock).updateMetadata("pid", dataset.getDatasetVersion());
        Mockito.verify(dataverseServiceMock, Mockito.never()).createDataset(Mockito.any());
    }

    @Test
    public void createDatasetVersion_throws_IllegalArgumentException_if_dataset_is_null() {
        // Given
        var depositId = UUID.randomUUID();
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, null);

        // When
        // Then
        assertThatThrownBy(() -> datasetVersionCreator.createDatasetVersion(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Must have dataset metadata to create a new dataset.");
    }

    @Test
    public void createDatasetVersion_is_noop_if_dataset_is_null_and_targetPid_is_not_null() throws Exception {
        // Given
        var depositId = UUID.randomUUID();
        DatasetVersionCreator datasetVersionCreator = new DatasetVersionCreator(depositId, dataverseServiceMock, null, null);

        // When
        datasetVersionCreator.createDatasetVersion("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).createDataset(Mockito.any());
        Mockito.verify(dataverseServiceMock, Mockito.never()).updateMetadata(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void ctor_throws_NullPointerException_if_dataverseService_is_null() {
        // Given
        var depositId = UUID.randomUUID();
        // When
        // Then
        assertThatThrownBy(() -> new DatasetVersionCreator(depositId, null, null, new Dataset()))
          .isInstanceOf(NullPointerException.class);
    }

    // Throws NullPointerException if dataverseService is null
    @Test
    public void ctor_throws_NullPointerException_if_depositId_is_null() {
        assertThatThrownBy(() -> new DatasetVersionCreator(null, dataverseServiceMock, null, new Dataset()))
          .isInstanceOf(NullPointerException.class);
    }
}
