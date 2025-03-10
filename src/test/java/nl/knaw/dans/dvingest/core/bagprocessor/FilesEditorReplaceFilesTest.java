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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class FilesEditorReplaceFilesTest extends FilesEditorTestFixture {
    @Test
    public void replaceFiles_replaces_files_in_dataset() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        when(dataverseServiceMock.replaceFile(eq("pid"), eq(file("file1", 1)), any())).thenReturn(file("file1", 4));
        when(dataverseServiceMock.replaceFile(eq("pid"), eq(file("file3", 3)), any())).thenReturn(file("file3", 5));
        when(utilityServicesMock.wrapIfZipFile(dataDir.resolve("file1"))).thenReturn(Optional.of(Path.of("/tmp/wrapped-file1.zip")));
        when(utilityServicesMock.wrapIfZipFile(dataDir.resolve("file3"))).thenReturn(Optional.empty());
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                replaceFiles:
                  - file1
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).replaceFile("pid", file("file1", 1), Path.of("/tmp/wrapped-file1.zip"));
        Mockito.verify(dataverseServiceMock).replaceFile("pid", file("file3", 3), dataDir.resolve("file3"));
        YamlBeanAssert.assertThat(editFilesLog.getReplaceFiles()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void replaceFiles_throws_exception_when_file_not_found() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        when(dataverseServiceMock.replaceFile(eq("pid"), eq(file("file1", 1)), any())).thenReturn(file("file1", 4));
        when(dataverseServiceMock.replaceFile(eq("pid"), eq(file("file3", 3)), any())).thenReturn(file("file3", 5));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                replaceFiles:
                  - file1
                  - file4
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        assertThatThrownBy(() -> filesEditor.editFiles("pid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File to replace not found in dataset: file4");
        YamlBeanAssert.assertThat(editFilesLog.getReplaceFiles()).isEqualTo("""
            numberCompleted: 1
            completed: false
            """);
    }

    @Test
    public void replaceFiles_is_skipped_when_already_completed() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                replaceFiles:
                  - file1
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getReplaceFiles().setCompleted(true);
        editFilesLog.getReplaceFiles().setNumberCompleted(2);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).replaceFile(any(), any(), any());
        YamlBeanAssert.assertThat(editFilesLog.getReplaceFiles()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void replaceFiles_is_skipped_when_list_is_empty() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                replaceFiles: []
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).replaceFile(any(), any(), any());
        YamlBeanAssert.assertThat(editFilesLog.getReplaceFiles()).isEqualTo("""
            numberCompleted: 0
            completed: true
            """);
    }

    @Test
    public void replaceFiles_will_continue_after_number_already_completed() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file2", 2),
                file("file3", 3)));
        when(dataverseServiceMock.replaceFile(eq("pid"), eq(file("file3", 3)), any())).thenReturn(file("file3", 5));
        when(dataverseServiceMock.replaceFile(eq("pid"), eq(file("file1", 1)), any())).thenReturn(file("file1", 4));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                replaceFiles:
                  - file1 # already replaced
                  # file2 will not be replaced
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getReplaceFiles().setCompleted(false);
        editFilesLog.getReplaceFiles().setNumberCompleted(1);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).replaceFile("pid", file("file3", 3), dataDir.resolve("file3"));
        YamlBeanAssert.assertThat(editFilesLog.getReplaceFiles()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }
}
