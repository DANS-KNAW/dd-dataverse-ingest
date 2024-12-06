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

import nl.knaw.dans.dvingest.core.TestDirFixture;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.lib.dataverse.model.file.DataFile;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class FilesEditorTest extends TestDirFixture {
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);
    private final UtilityServices utilityServicesMock = Mockito.mock(UtilityServices.class);

    private Path dataDir;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.reset(dataverseServiceMock);
        Mockito.reset(utilityServicesMock);
        dataDir = testDir.resolve("data");
        Files.createDirectories(dataDir);
    }

    private FileMeta file(String path, int id) {
        var dvPath = new DataversePath(path);
        var fileMeta = new FileMeta();
        var dataFile = new DataFile();
        dataFile.setId(id);
        fileMeta.setDataFile(dataFile);
        fileMeta.setLabel(dvPath.getLabel());
        fileMeta.setDirectoryLabel(dvPath.getDirectoryLabel());
        return fileMeta;
    }

    @Test
    public void deleteFiles_deletes_files_from_dataset() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        var editFiles = new EditFiles();
        editFiles.setDeleteFiles(List.of("file1", "file3"));
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFiles, dataverseServiceMock, utilityServicesMock);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).deleteFile(1);
        Mockito.verify(dataverseServiceMock).deleteFile(3);
        assertThat(filesEditor.getFilesInDatasetCache().get("file1")).isNull();
    }

    @Test
    public void deleteFiles_throws_exception_when_file_not_found() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        var editFiles = new EditFiles();
        editFiles.setDeleteFiles(List.of("file1", "file4"));
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFiles, dataverseServiceMock, utilityServicesMock);

        // When
        assertThatThrownBy(() -> filesEditor.editFiles("pid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File to delete not found in dataset: file4");
    }

}
