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
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.tasklog.EditFilesLog;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class FilesEditorDeleteFilesTest extends FilesEditorTestFixture {

    @Test
    public void deleteFiles_deletes_files_from_dataset() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                deleteFiles:
                  - file1
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).deleteFile(1);
        Mockito.verify(dataverseServiceMock).deleteFile(3);
        assertThat(filesEditor.getFilesInDatasetCache().get("file1")).isNull();
        YamlBeanAssert.assertThat(editFilesLog.getDeleteFiles()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void deleteFiles_throws_exception_when_file_not_found() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                deleteFiles:
                  - file1
                  - file4
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        assertThatThrownBy(() -> filesEditor.editFiles("pid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File to delete not found in dataset: file4");
        YamlBeanAssert.assertThat(editFilesLog.getDeleteFiles()).isEqualTo("""
            numberCompleted: 1
            completed: false
            """);
    }

    @Test
    public void deleteFiles_is_skipped_when_already_completed() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                deleteFiles:
                  - file1
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getDeleteFiles().setCompleted(true);
        editFilesLog.getDeleteFiles().setNumberCompleted(2);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).deleteFile(anyInt());
        YamlBeanAssert.assertThat(editFilesLog.getDeleteFiles()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void deleteFiles_is_skipped_when_list_is_empty() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                deleteFiles: []
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).deleteFile(anyInt());
        YamlBeanAssert.assertThat(editFilesLog.getDeleteFiles()).isEqualTo("""
            numberCompleted: 0
            completed: true
            """);
    }

    @Test
    public void deleteFiles_will_continue_after_number_already_completed() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file2", 2),
                file("file3", 3)));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                deleteFiles:
                  - file1 # already deleted
                  # file2 will not be deleted
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getDeleteFiles().setCompleted(false);
        editFilesLog.getDeleteFiles().setNumberCompleted(1);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).deleteFile(3);
        assertThat(filesEditor.getFilesInDatasetCache().get("file1")).isNull(); // already deleted
        assertThat(filesEditor.getFilesInDatasetCache().get("file2")).isNotNull(); // not on delete list
        assertThat(filesEditor.getFilesInDatasetCache().get("file3")).isNull(); // just deleted
        YamlBeanAssert.assertThat(editFilesLog.getDeleteFiles()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }



}
