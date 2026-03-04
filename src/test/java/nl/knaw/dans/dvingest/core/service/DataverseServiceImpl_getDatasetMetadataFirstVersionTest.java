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

import nl.knaw.dans.lib.dataverse.DatasetApi;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseHttpResponse;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DataverseServiceImpl_getDatasetMetadataFirstVersionTest {
    @Test
    void getDatasetMetadataFirstVersion_returns_first_version() throws Exception {
        var persistentId = "doi:10.5072/FK2/ABCDEF";
        var datasetVersion = new DatasetVersion();
        var dataverseClientMock = mock(DataverseClient.class);
        var datasetApiMock = mock(DatasetApi.class);
        var httpResponseMock = mock(DataverseHttpResponse.class);
        var dataverseService = DataverseServiceImpl.builder().dataverseClient(dataverseClientMock).build();

        when(dataverseClientMock.dataset(persistentId)).thenReturn(datasetApiMock);
        when(datasetApiMock.getVersion("1.0", true)).thenReturn(httpResponseMock);
        when(httpResponseMock.getData()).thenReturn(datasetVersion);

        var result = dataverseService.getDatasetMetadataFirstVersion(persistentId);
        assertThat(result).isSameAs(datasetVersion);
        verify(dataverseClientMock).dataset(persistentId);
        verify(datasetApiMock).getVersion("1.0", true);
        verify(httpResponseMock).getData();
        verifyNoMoreInteractions(dataverseClientMock, datasetApiMock, httpResponseMock);
    }

    @Test
    void getDatasetMetadataFirstVersion_throws_on_error() throws Exception {
        var persistentId = "doi:10.5072/FK2/ABCDEF";
        var dataverseClientMock = mock(DataverseClient.class);
        var datasetApiMock = mock(DatasetApi.class);
        var dataverseService = DataverseServiceImpl.builder().dataverseClient(dataverseClientMock).build();

        when(dataverseClientMock.dataset(persistentId)).thenReturn(datasetApiMock);
        when(datasetApiMock.getVersion("1.0", true)).thenThrow(new IOException("not found"));

        var exception = assertThrows(IOException.class, () -> dataverseService.getDatasetMetadataFirstVersion(persistentId));
        assertThat(exception).hasMessageContaining("not found");
        verify(dataverseClientMock).dataset(persistentId);
        verify(datasetApiMock).getVersion("1.0", true);
        verifyNoMoreInteractions(dataverseClientMock, datasetApiMock);
    }
}
