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
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class DataverseServiceImplTest {
    @Test
    public void addFile_adds_file_and_returns_fileList() throws Exception {
        var fileMeta = new FileMeta();
        var fileList = new FileList();
        var filePath = Path.of("somefile.txt");
        var persistentId = "doi:10.5072/FK2/ABCDEF";

        var dataverseClientMock = mock(DataverseClient.class);
        var httpResponseMock = mock(DataverseHttpResponse.class);
        var datasetApiMock = mock(DatasetApi.class);
        var dataverseService = createDataverseService(dataverseClientMock);
        when(httpResponseMock.getData()).thenReturn(fileList);
        when(httpResponseMock.getEnvelopeAsString()).thenReturn("dummy envelope");
        when(datasetApiMock.addFile(filePath, fileMeta)).thenReturn(httpResponseMock);
        when(dataverseClientMock.dataset(persistentId)).thenReturn(datasetApiMock);
        doNothing().when(datasetApiMock).awaitUnlock(anyList(), eq(10), eq(1000));
        when(datasetApiMock.addFile(filePath, fileMeta)).thenReturn(httpResponseMock);

        FileList result = dataverseService.addFile(persistentId, filePath, fileMeta);

        assertThat(result).isSameAs(fileList);
        verify(dataverseClientMock).dataset(persistentId);
        verify(datasetApiMock).awaitUnlock(List.of("Ingest"), 10, 1000);
        verify(datasetApiMock).addFile(filePath, fileMeta);
        verify(httpResponseMock).getData();
        verify(httpResponseMock).getEnvelopeAsString();
        verifyNoMoreInteractions(dataverseClientMock, datasetApiMock, httpResponseMock);
    }
    @Test
    public void addFile_throws_when_awaitUnlock_fails() throws Exception {
        var fileMeta = new FileMeta();
        var filePath = Path.of("somefile.txt");
        var persistentId = "doi:10.5072/FK2/ABCDEF";

        var dataverseClientMock = mock(DataverseClient.class);
        var datasetApiMock = mock(DatasetApi.class);
        var dataverseService = createDataverseService(dataverseClientMock);
        when(dataverseClientMock.dataset(persistentId)).thenReturn(datasetApiMock);
        doThrow(new IOException("unlock failed"))
            .when(datasetApiMock).awaitUnlock(anyList(), eq(10), eq(1000));


        assertThatThrownBy(() -> dataverseService.addFile(persistentId, filePath, fileMeta))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unlock failed");

        verify(dataverseClientMock).dataset(persistentId);
        verify(datasetApiMock).awaitUnlock(List.of("Ingest"), 10, 1000);
        verifyNoMoreInteractions(dataverseClientMock, datasetApiMock);
    }

    private DataverseServiceImpl createDataverseService(DataverseClient dataverseClient) {
        return DataverseServiceImpl.builder()
            .dataverseClient(dataverseClient)
            .build();
    }
}