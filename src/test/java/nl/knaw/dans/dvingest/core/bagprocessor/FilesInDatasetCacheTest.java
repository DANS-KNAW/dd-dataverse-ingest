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
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilesInDatasetCacheTest {
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(dataverseServiceMock);
    }

    @Test
    public void get_returns_fileMeta_by_filepath() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        var fileMeta = new FileMeta();
        fileMeta.setLabel("label");
        fileMeta.setDirectoryLabel("directoryLabel");

        // When
        filesInDatasetCache.put(fileMeta);

        // Then
        assertThat(filesInDatasetCache.get("directoryLabel/label")).isEqualTo(fileMeta);
    }

    @Test
    public void get_returns_null_for_nonexistent_filepath() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());

        // When
        var result = filesInDatasetCache.get("nonexistent/filepath");

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void put_adds_fileMeta_to_cache() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        var fileMeta = new FileMeta();
        fileMeta.setLabel("label");
        fileMeta.setDirectoryLabel("directoryLabel");

        // When
        filesInDatasetCache.put(fileMeta);

        // Then
        assertThat(filesInDatasetCache.get("directoryLabel/label")).isEqualTo(fileMeta);
    }

    @Test
    public void remove_deletes_fileMeta_from_cache() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        var fileMeta = new FileMeta();
        fileMeta.setLabel("label");
        fileMeta.setDirectoryLabel("directoryLabel");
        filesInDatasetCache.put(fileMeta);

        // When
        filesInDatasetCache.remove("directoryLabel/label");

        // Then
        assertThat(filesInDatasetCache.get("directoryLabel/label")).isNull();
    }

    @Test
    public void downloadFromDataset_initializes_cache() throws Exception {
        // Given
        var fileMeta = new FileMeta();
        fileMeta.setLabel("label");
        fileMeta.setDirectoryLabel("directoryLabel");
        Mockito.when(dataverseServiceMock.getFiles("pid", false)).thenReturn(java.util.List.of(fileMeta));
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());

        // When
        filesInDatasetCache.downloadFromDataset("pid", false);

        // Then
        assertThat(filesInDatasetCache.get("directoryLabel/label")).isEqualTo(fileMeta);
    }

    @Test
    public void downloadFromDataset_throws_exception_if_already_initialized() throws Exception {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        filesInDatasetCache.downloadFromDataset("pid", false);

        // When / Then
        assertThatThrownBy(() -> filesInDatasetCache.downloadFromDataset("pid", false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cache already initialized");
    }

    @Test
    public void get_returns_fileMeta_by_old_name_after_rename() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of("oldPath/file.txt", "newPath/file.txt"));
        var fileMeta = new FileMeta();
        fileMeta.setLabel("file.txt");
        fileMeta.setDirectoryLabel("newPath");
        filesInDatasetCache.put(fileMeta);

        // When
        var result = filesInDatasetCache.get("oldPath/file.txt");

        // Then
        assertThat(result).isEqualTo(fileMeta);
    }

}