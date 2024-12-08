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
package nl.knaw.dans.dvingest.core.dansbag;

import nl.knaw.dans.dvingest.core.dansbag.deposit.FileInfo;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EditFilesComposerForUpdateTest extends EditFilesComposerFixture {
    private final DataverseService dataverseServiceMock = mock(DataverseService.class);

    @Test
    public void file_with_same_path_and_different_checksum_is_replaced() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "newchecksum"));

        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getReplaceFiles()).hasSize(1);
        assertThat(editFiles.getReplaceFiles().get(0)).isEqualTo("file1.txt");

        assertEmptyFieldsExcept(editFiles, "replaceFiles");
    }

    @Test
    public void file_with_same_path_and_same_checksum_is_NOT_replaced() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "oldchecksum"));

        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getReplaceFiles()).isEmpty();

        assertEmptyFieldsExcept(editFiles);
    }

    @Test
    public void file_with_different_path_and_same_checksum_is_moved() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("path/to/file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("path/three/file2.txt", "oldchecksum"));

        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getMoveFiles()).hasSize(1);
        assertThat(editFiles.getMoveFiles().get(0).getFrom()).isEqualTo("path/to/file1.txt");
        assertThat(editFiles.getMoveFiles().get(0).getTo()).isEqualTo("path/three/file2.txt");

        assertEmptyFieldsExcept(editFiles, "moveFiles");
    }

    @Test
    public void unrestricted_file_with_different_path_and_different_checksum_is_added() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("path/to/file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("path/to/file1.txt", "oldchecksum")); // Confirming that the file is to remain in the dataset
        add(map, file("path/three/file2.txt", "newchecksum"));
        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getAddUnrestrictedFiles()).hasSize(1);
        assertThat(editFiles.getAddUnrestrictedFiles()).contains("path/three/file2.txt");

        assertEmptyFieldsExcept(editFiles, "addUnrestrictedFiles");
    }

    @Test
    public void restricted_file_with_different_path_and_different_checksum_is_added() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("path/to/file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("path/to/file1.txt", "oldchecksum")); // Confirming that the file is to remain in the dataset
        add(map, file("path/three/file2.txt", "newchecksum", true));
        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getAddRestrictedFiles()).hasSize(1);
        assertThat(editFiles.getAddRestrictedFiles()).contains("path/three/file2.txt");

        assertEmptyFieldsExcept(editFiles, "addRestrictedFiles");
    }

    @Test
    public void ambiguous_move_is_implemented_add_delete_and_add() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("path/to/file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("path/three/file1.txt", "oldchecksum"));
        add(map, file("path/three/file2.txt", "oldchecksum"));

        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getDeleteFiles()).hasSize(1);
        assertThat(editFiles.getDeleteFiles().get(0)).isEqualTo("path/to/file1.txt");
        assertThat(editFiles.getAddUnrestrictedFiles()).hasSize(2);
        assertThat(editFiles.getAddUnrestrictedFiles()).contains("path/three/file1.txt", "path/three/file2.txt");

        assertEmptyFieldsExcept(editFiles, "deleteFiles", "addUnrestrictedFiles");
    }

    @Test
    public void file_not_replaced_nor_in_current_deposit_is_deleted() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("path/to/file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();

        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertThat(editFiles.getDeleteFiles()).hasSize(1);
        assertThat(editFiles.getDeleteFiles().get(0)).isEqualTo("path/to/file1.txt");

        assertEmptyFieldsExcept(editFiles, "deleteFiles");
    }

    @Test
    public void file_with_same_path_and_checksum_is_not_touched() throws Exception {
        // Given
        when(dataverseServiceMock.getFiles(anyString())).thenReturn(List.of(fileMeta("path/to/file1.txt", "oldchecksum")));
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("path/to/file1.txt", "oldchecksum"));

        editFilesComposer = new EditFilesComposerForUpdate(map, inThePast, "doi:some", null, List.of(), dataverseServiceMock);

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        assertEmptyFieldsExcept(editFiles);
    }

}
