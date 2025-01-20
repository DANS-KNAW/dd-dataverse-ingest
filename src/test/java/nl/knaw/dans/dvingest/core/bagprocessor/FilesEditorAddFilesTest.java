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

import nl.knaw.dans.dvingest.YamlBeanAssert;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.service.UtilityServicesImpl;
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.tasklog.EditFilesLog;
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilesEditorAddFilesTest extends FilesEditorTestFixture {

    @Test
    public void addRestrictedFiles_adds_one_batch() throws Exception {
        // Given
        Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(100).build();
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class))).thenReturn(
            new FileList(List.of(file("file1", 1), file("file2", 2), file("file3", 3))));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedFiles:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        verify(dataverseServiceMock).addFile(eq("pid"), pathCaptor.capture(), fileMetaCaptor.capture());
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertThat(fileMetaCaptor.getValue().getRestricted()).isTrue();
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addRestrictedFiles_adds_two_batches() throws Exception {
        // Given
        Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            // First call
            .thenReturn(new FileList(List.of(file("file1", 1), file("file2", 2))))
            // Second call
            .thenReturn(new FileList(List.of(file("file3", 3))));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedFiles:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        verify(dataverseServiceMock, times(2)).addFile(eq("pid"), pathCaptor.capture(), fileMetaCaptor.capture());
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertThat(fileMetaCaptor.getValue().getRestricted()).isTrue();
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

}
