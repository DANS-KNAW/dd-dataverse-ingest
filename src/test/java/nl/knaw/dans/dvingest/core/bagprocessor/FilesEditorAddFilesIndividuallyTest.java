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
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class FilesEditorAddFilesIndividuallyTest extends FilesEditorTestFixture {
    @Test
    public void addRestrictedIndividually_adds_files_to_dataset() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedIndividually:
                  - file1
                  - file2
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).addFile(anyString(), eq(dataDir.resolve("file1")), Mockito.any(FileMeta.class));
        Mockito.verify(dataverseServiceMock).addFile(anyString(), eq(dataDir.resolve("file2")), Mockito.any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedIndividually()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFilesIndividually_adds_files_to_dataset() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        Files.createFile(dataDir.resolve("file3"));
        Files.createFile(dataDir.resolve("file4"));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedIndividually:
                  - file3
                  - file4
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).addFile(anyString(), eq(dataDir.resolve("file3")), Mockito.any(FileMeta.class));
        Mockito.verify(dataverseServiceMock).addFile(anyString(), eq(dataDir.resolve("file4")), Mockito.any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedIndividually()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void addRestrictedFilesIndividually_throws_exception_when_file_not_found() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedIndividually:
                  - file1
                  - file5
            """, EditFilesRoot.class);
        // Create file1 but not file5
        Files.createFile(dataDir.resolve("file1"));
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        assertThatThrownBy(() -> filesEditor.editFiles("pid"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("File to add not found in bag: file5");
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedIndividually()).isEqualTo("""
            numberCompleted: 1
            completed: false
            """);
    }

    @Test
    public void addUnrestrictedFilesIndividually_throws_exception_when_file_not_found() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedIndividually:
                  - file3
                  - file6
            """, EditFilesRoot.class);
        // create file3 but not file6
        Files.createFile(dataDir.resolve("file3"));
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        assertThatThrownBy(() -> filesEditor.editFiles("pid"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("File to add not found in bag: file6");
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedIndividually()).isEqualTo("""
            numberCompleted: 1
            completed: false
            """);
    }

    @Test
    public void addRestrictedFilesIndividually_is_skipped_when_already_completed() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedIndividually:
                  - file1
                  - file2
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddRestrictedIndividually().setCompleted(true);
        editFilesLog.getAddRestrictedIndividually().setNumberCompleted(2);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.addRestrictedFilesIndividually();

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).addFile(Mockito.anyString(), Mockito.any(Path.class), Mockito.any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedIndividually()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFilesIndividually_is_skipped_when_already_completed() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedIndividually:
                  - file3
                  - file4
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddUnrestrictedIndividually().setCompleted(true);
        editFilesLog.getAddUnrestrictedIndividually().setNumberCompleted(2);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.addUnrestrictedFilesIndividually();

        // Then
        Mockito.verify(dataverseServiceMock, Mockito.never()).addFile(Mockito.anyString(), Mockito.any(Path.class), Mockito.any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedIndividually()).isEqualTo("""
            numberCompleted: 2
            completed: true
            """);
    }

    @Test
    public void addRestrictedFilesIndividually_will_continue_after_number_already_completed() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedIndividually:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddRestrictedIndividually().setCompleted(false);
        editFilesLog.getAddRestrictedIndividually().setNumberCompleted(2);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).addFile(anyString(), eq(dataDir.resolve("file3")), Mockito.any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedIndividually()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    // The same for unrestricted
    @Test
    public void addUnrestrictedFilesIndividually_will_continue_after_number_already_completed() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedIndividually:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddUnrestrictedIndividually().setCompleted(false);
        editFilesLog.getAddUnrestrictedIndividually().setNumberCompleted(2);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).addFile(anyString(), eq(dataDir.resolve("file3")), Mockito.any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedIndividually()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addRestrictedFilesIndividually_will_auto_rename_file() throws Exception {
        // Given
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), fileMetaCaptor.capture()))
            .thenAnswer(invocation -> new FileList(List.of(fileMetaCaptor.getValue())));
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedIndividually:
                  - file1
                  - file2
                  - file3
                autoRenameFiles:
                  - from: file3
                    to: file3-renamed
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);
        // When
        filesEditor.editFiles("pid");

        // Then
        Mockito.verify(dataverseServiceMock).addFile(eq("pid"), eq(dataDir.resolve("file3")), fileMetaCaptor.capture());
        assertThat(fileMetaCaptor.getValue().getLabel()).isEqualTo("file3-renamed");
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedIndividually()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

}
