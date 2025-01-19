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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class FilesEditorAddFilesTest extends FilesEditorTestFixture {

    @Test
    public void addRestrictedFiles_adds_one_batch() throws Exception {
        // Given
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(100).build();
        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
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
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

}
