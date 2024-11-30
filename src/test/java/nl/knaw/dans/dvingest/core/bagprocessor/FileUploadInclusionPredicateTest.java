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

import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUploadInclusionPredicateTest {
    private final File dataDir = new File("dataDir");
    private EditFiles editFiles = new EditFiles();

    @BeforeEach
    public void setUp() {
        editFiles = new EditFiles();
    }

    @Test
    public void ignored_files_are_skipped_for_unrestricted_file_upload() throws Exception {
        // Given
        editFiles.setIgnoreFiles(List.of("file1"));
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), false);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void ignored_files_are_skipped_for_restricted_file_upload() throws Exception {
        // Given
        editFiles.setIgnoreFiles(List.of("file1"));
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), true);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void restricted_files_are_included_for_restricted_file_upload() throws Exception {
        // Given
        editFiles.setAddRestrictedFiles(List.of("file1"));
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), true);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void restricted_files_are_skipped_for_unrestricted_file_upload() throws Exception {
        // Given
        editFiles.setAddRestrictedFiles(List.of("file1"));
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), false);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void replaced_files_are_skipped_for_unrestricted_file_upload() throws Exception {
        // Given
        editFiles.setReplaceFiles(List.of("file1"));
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), false);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void replaced_files_are_skipped_for_restricted_file_upload() throws Exception {
        // Given
        editFiles.setReplaceFiles(List.of("file1"));
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), true);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void unrestricted_files_are_included_for_unrestricted_file_upload() throws Exception {
        // Given
        // Default is unrestricted upload
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(editFiles, dataDir.toPath(), false);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void all_files_are_included_if_editFiles_is_null_for_unrestricted_file_upload() throws Exception {
        // Given
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(null, dataDir.toPath(), false);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void all_files_are_excluded_if_editFiles_is_null_for_restricted_file_upload() throws Exception {
        // Given
        var fileUploadInclusionPredicate = new FileUploadInclusionPredicate(null, dataDir.toPath(), true);

        // When
        var result = fileUploadInclusionPredicate.evaluate(new File("dataDir/file1"));

        // Then
        assertThat(result).isFalse();
    }
}
