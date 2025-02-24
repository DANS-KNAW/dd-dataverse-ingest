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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class FilesEditorMoveFilesTest extends FilesEditorTestFixture {

    @Test
    public void moveFiles_moves_files_to_different_path_in_dataset() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles("pid", true)).thenReturn(
            List.of(file("some/file1", 1),
                file("some_other/file2", 2),
                file("file3", 3)));
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                moveFiles:
                  - from: some/file1
                    to: some_other/file1
                  - from: file3
                    to: some_other/file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServicesMock, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        ArgumentCaptor<List<FileMetaUpdate>> fileMetaUpdatesCaptor = ArgumentCaptor.captor();
        Mockito.verify(dataverseServiceMock).updateFileMetadatas(eq("pid"), fileMetaUpdatesCaptor.capture());
        var fileMetaUpdates = fileMetaUpdatesCaptor.getValue();
        assertThat(fileMetaUpdates).hasSize(2);
        var newPaths = fileMetaUpdates.stream().map(fmu -> new DataversePath(fmu.getDirectoryLabel(), fmu.getLabel()).toString()).toList();
        assertThat(newPaths).containsExactlyInAnyOrder("some_other/file1", "some_other/file3");
        YamlBeanAssert.assertThat(editFilesLog.getMoveFiles()).isEqualTo("""
            completed: true
            """);
    }

}
