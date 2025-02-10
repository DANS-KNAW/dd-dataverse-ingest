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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilesInDatasetCacheNonNullTest {
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);

    @Test
    public void constructor_throws_exception_when_dataverseService_is_null() {
        assertThatThrownBy(() -> new FilesInDatasetCache(null, Map.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void constructor_throws_exception_when_autoRenamedFiles_is_null() {
        assertThatThrownBy(() -> new FilesInDatasetCache(dataverseServiceMock, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void get_throws_exception_when_filepath_is_null() {
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        assertThatThrownBy(() -> filesInDatasetCache.get(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void put_throws_exception_when_fileMeta_is_null() {
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        assertThatThrownBy(() -> filesInDatasetCache.put(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void createFileMetaForMovedFile_throws_exception_when_toPath_is_null() {
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        var fileMeta = new FileMeta();
        assertThatThrownBy(() -> filesInDatasetCache.createFileMetaForMovedFile(null, fileMeta))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void createFileMetaForMovedFile_throws_exception_when_fileMeta_is_null() {
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        assertThatThrownBy(() -> filesInDatasetCache.createFileMetaForMovedFile("path", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void remove_throws_exception_when_filepath_is_null() {
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        assertThatThrownBy(() -> filesInDatasetCache.remove(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void downloadFromDataset_throws_exception_when_pid_is_null() {
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        assertThatThrownBy(() -> filesInDatasetCache.downloadFromDataset(null, false))
            .isInstanceOf(NullPointerException.class);
    }
}