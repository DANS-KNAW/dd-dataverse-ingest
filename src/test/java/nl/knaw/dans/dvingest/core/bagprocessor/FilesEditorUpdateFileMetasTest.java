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
import nl.knaw.dans.lib.dataverse.model.file.FileMetaUpdate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class FilesEditorUpdateFileMetasTest extends FilesEditorTestFixture {

    private static String getPath(FileMetaUpdate u) {
        return new DataversePath(u.getDirectoryLabel(), u.getLabel()).toString();
    }

    @Test
    public void updateFileMetas_updates_file_metas_in_dataset() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                updateFileMetas:
                  - label: file1
                    description: "new description"
                    categories: ["cat1", "cat2"]
                  - label: file3
                    description: "another description"
                    categories: ["cat3"]
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        ArgumentCaptor<List<FileMetaUpdate>> fileMetaUpdatesCaptor = ArgumentCaptor.captor();
        Mockito.verify(dataverseServiceMock).updateFileMetadatas(eq("pid"), fileMetaUpdatesCaptor.capture());
        var fileMetaUpdates = fileMetaUpdatesCaptor.getValue();
        assertThat(fileMetaUpdates.stream().map(u -> new ImmutablePair<>(getPath(u), u.getCategories()))).containsExactlyInAnyOrder(
            new ImmutablePair<>("file1", List.of("cat1", "cat2")),
            new ImmutablePair<>("file3", List.of("cat3"))
        );

        var cache = filesEditor.getFilesInDatasetCache();

        assertThat(cache.getFilesInDataset()).hasSize(3);

        // Updated
        assertThat(cache.get("file1").getDescription()).isEqualTo("new description");
        assertThat(cache.get("file1").getCategories()).containsExactly("cat1", "cat2");
        assertThat(cache.get("file3").getDescription()).isEqualTo("another description");
        assertThat(cache.get("file3").getCategories()).containsExactly("cat3");

        // Not modified
        assertThat(cache.get("file2").getDescription()).isNull();
        assertThat(cache.get("file2").getCategories()).isEmpty();

        YamlBeanAssert.assertThat(editFilesLog.getUpdateFileMetas()).isEqualTo("""
            completed: true
            """);
    }

    @Test
    public void updateFileMetas_throws_exception_when_file_not_found() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("file1", 1),
                file("file2", 2),
                file("file3", 3)));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                updateFileMetas:
                  - label: file1
                    description: "new description"
                    categories: ["cat1", "cat2"]
                  - label: file4
                    description: "another description"
                    categories: ["cat3"]
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        assertThatThrownBy(() -> filesEditor.editFiles("pid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Files not found in dataset: [file4]");
    }

    @Test
    public void updateFileMetas_is_skipped_when_already_completed() throws Exception {
        // Given
        var editFilesRoot = yamlService.readYamlFromString("""
                editFiles:
                    updateFileMetas:
                      - label: file1
                        description: "new description"
                        categories: ["cat1", "cat2"]
                      - label: file3
                        description: "another description"
                        categories: ["cat3"]
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getUpdateFileMetas().setCompleted(true);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        YamlBeanAssert.assertThat(editFilesLog.getUpdateFileMetas()).isEqualTo("""
            completed: true
            """);
    }
}
