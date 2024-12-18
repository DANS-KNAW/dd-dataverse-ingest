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
import nl.knaw.dans.dvingest.core.yaml.AddEmbargo;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class EditFilesComposerTest extends EditFilesComposerFixture {

    @Test
    public void adding_two_unrestricted_files_adds_them_to_addUnrestrictedFiles() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1"));
        add(map, file("file2.txt", "checksum2"));
        editFilesComposer = new EditFilesComposer(map, inThePast, null, null,  List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(2);
        assertThat(unrestrictedFiles).contains("file1.txt", "file2.txt");

        // Then
        assertEmptyFieldsExcept(editFiles, "addUnrestrictedFiles");
    }

    @Test
    public void adding_two_restricted_files_adds_them_to_addRestrictedFiles() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1", true));
        add(map, file("file2.txt", "checksum2", true));
        add(map, file("file3.txt", "checksum3", false));
        editFilesComposer = new EditFilesComposer(map, inThePast, null, null, List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        var addRestrictedFiles = editFiles.getAddRestrictedFiles();
        assertThat(addRestrictedFiles).hasSize(2);
        assertThat(addRestrictedFiles).contains("file1.txt", "file2.txt");

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(1);
        assertThat(unrestrictedFiles).contains("file3.txt");

        assertEmptyFieldsExcept(editFiles, "addRestrictedFiles", "addUnrestrictedFiles");
    }

    @Test
    public void setting_description_adds_it_to_updateFileMetas() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1", false, "description1"));
        add(map, file("file2.txt", "checksum2", false, "description2"));
        add(map, file("file3.txt", "checksum3", false));
        editFilesComposer = new EditFilesComposer(map, inThePast, null, null, List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        var updateFileMetas = editFiles.getUpdateFileMetas();
        assertThat(updateFileMetas).hasSize(2);
        assertThat(updateFileMetas).extracting(FileMeta::getDescription).contains("description1", "description2");

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(3);
        assertThat(unrestrictedFiles).contains("file1.txt", "file2.txt", "file3.txt");

        assertEmptyFieldsExcept(editFiles, "updateFileMetas", "addUnrestrictedFiles");
    }

    @Test
    public void setting_categories_adds_them_to_updateFileMetas() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1", false, null, List.of("category1")));
        add(map, file("file2.txt", "checksum2", false, null, List.of("category2")));
        add(map, file("file3.txt", "checksum3", false));
        editFilesComposer = new EditFilesComposer(map, inThePast, null, null, List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        var updateFileMetas = editFiles.getUpdateFileMetas();
        assertThat(updateFileMetas).hasSize(2);
        assertThat(updateFileMetas).extracting(FileMeta::getCategories).contains(List.of("category1"), List.of("category2"));

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(3);
        assertThat(unrestrictedFiles).contains("file1.txt", "file2.txt", "file3.txt");

        assertEmptyFieldsExcept(editFiles, "updateFileMetas", "addUnrestrictedFiles");
    }

    @Test
    public void setting_dateAvailable_to_future_date_embargoes_all_files() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1"));
        add(map, file("file2.txt", "checksum2"));
        editFilesComposer = new EditFilesComposer(map, inTheFuture, null, null, List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        var addEmbargoes = editFiles.getAddEmbargoes();
        assertThat(addEmbargoes).hasSize(1); // There is only one embargo, covering all files
        assertThat(addEmbargoes).extracting(AddEmbargo::getFilePaths).containsExactly(List.of("file1.txt", "file2.txt"));

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(2);
        assertThat(unrestrictedFiles).contains("file1.txt", "file2.txt");

        assertEmptyFieldsExcept(editFiles, "addEmbargoes", "addUnrestrictedFiles");
    }

    @Test
    public void sanitized_files_are_add_to_autoRenamedFiles() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, sanitizedFile("file1.txt", "file1_sanitized.txt", "checksum1"));
        add(map, file("file2.txt", "checksum2"));
        editFilesComposer = new EditFilesComposer(map, inThePast, null, null, List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        var autoRenameFiles = editFiles.getAutoRenameFiles();
        assertThat(autoRenameFiles).hasSize(1);
        assertThat(autoRenameFiles.get(0).getFrom()).isEqualTo("file1.txt");
        assertThat(autoRenameFiles.get(0).getTo()).isEqualTo("file1_sanitized.txt");

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(2);
        assertThat(unrestrictedFiles).contains("file1.txt", "file2.txt");

        assertEmptyFieldsExcept(editFiles, "autoRenameFiles", "addUnrestrictedFiles");
    }

    @Test
    public void fileExclusionPattern_ignores_files_matching_pattern() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1"));
        add(map, file("file2.txt", "checksum2"));
        editFilesComposer = new EditFilesComposer(map, inThePast, Pattern.compile("file1.*"), null, List.of());

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        var addUnrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(addUnrestrictedFiles).hasSize(1);
        assertThat(addUnrestrictedFiles).contains("file2.txt");

        // Then
        assertEmptyFieldsExcept(editFiles, "addUnrestrictedFiles");
    }

    @Test
    public void embargoExclusions_ignores_files_matching_filepaths() {
        // Given
        Map<Path, FileInfo> map = new HashMap<>();
        add(map, file("file1.txt", "checksum1"));
        add(map, file("file2.txt", "checksum2"));
        add(map, file("subdir/file3.txt", "checksum3"));
        editFilesComposer = new EditFilesComposer(map, inTheFuture, null, null, List.of("file2.txt", "subdir/file3.txt"));

        // When
        var editFiles = editFilesComposer.composeEditFiles();

        // Then
        var addEmbargoes = editFiles.getAddEmbargoes();
        assertThat(addEmbargoes).hasSize(1); // There is only one embargo, covering all files
        assertThat(addEmbargoes).extracting(AddEmbargo::getFilePaths).containsExactly(List.of("file1.txt"));

        var unrestrictedFiles = editFiles.getAddUnrestrictedFiles();
        assertThat(unrestrictedFiles).hasSize(3);
        assertThat(unrestrictedFiles).contains("file1.txt", "file2.txt", "subdir/file3.txt");

        assertEmptyFieldsExcept(editFiles, "addEmbargoes", "addUnrestrictedFiles");
    }

}
