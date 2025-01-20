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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilesEditorAddFilesTest extends FilesEditorTestFixture {
    /**
     * Creates a FileList based on the entries in a ZIP file, mimicking the behavior of the Dataverse API.
     *
     * @param zipFile    the ZIP file
     * @param restricted whether the files are restricted
     * @return the FileList
     * @throws Exception if an I/O error occurs
     */
    private FileList createFileMetaPerZipEntry(Path zipFile, boolean restricted) throws Exception {
        var metas = new ArrayList<FileMeta>();
        try (var zipFiles = new ZipFile(zipFile.toFile())) {
            for (var entry = zipFiles.entries(); entry.hasMoreElements(); ) {
                var zipEntry = entry.nextElement();
                var dataversePath = new DataversePath(zipEntry.getName());
                var fileMeta = new FileMeta();
                fileMeta.setLabel(dataversePath.getLabel());
                fileMeta.setDirectoryLabel(dataversePath.getDirectoryLabel());
                fileMeta.setRestricted(restricted);
                metas.add(fileMeta);
            }
            return new FileList(metas);
        }
    }

    private void assertZipFileContainsFiles(Path zipFile, String... files) throws Exception {
        try (var zipFiles = new ZipFile(zipFile.toFile())) {
            for (var entry = zipFiles.entries(); entry.hasMoreElements(); ) {
                var zipEntry = entry.nextElement();
                assertThat(zipEntry.getName()).isIn(files);
            }
        }
    }

    @Test
    public void addRestrictedFiles_adds_one_batch() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(100).build();
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class))).thenAnswer(
            invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch.zip").toFile());
                return createFileMetaPerZipEntry(path, true);
            });
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
        assertThat(fileMetaCaptor.getValue().getRestricted()).isTrue();
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertZipFileContainsFiles(tempDir.resolve("batch.zip"), "file1", "file2", "file3");
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addRestrictedFiles_adds_two_batches() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, true);
            }).thenAnswer(
                invocation -> {
                    var path = invocation.getArgument(1, Path.class);
                    FileUtils.copyFile(path.toFile(), path.resolveSibling("batch2.zip").toFile());
                    return createFileMetaPerZipEntry(path, true);
                });
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
        assertThat(fileMetaCaptor.getValue().getRestricted()).isTrue();
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertZipFileContainsFiles(tempDir.resolve("batch1.zip"), "file1", "file2");
        assertZipFileContainsFiles(tempDir.resolve("batch2.zip"), "file3");
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFiles_adds_one_batch() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(100).build();
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class))).thenAnswer(
            invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch.zip").toFile());
                return createFileMetaPerZipEntry(path, false);
            });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedFiles:
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
        assertThat(fileMetaCaptor.getValue().getRestricted()).isFalse();
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertZipFileContainsFiles(tempDir.resolve("batch.zip"), "file1", "file2", "file3");
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFiles_adds_two_batches() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, false);
            }).thenAnswer(
                invocation -> {
                    var path = invocation.getArgument(1, Path.class);
                    FileUtils.copyFile(path.toFile(), path.resolveSibling("batch2.zip").toFile());
                    return createFileMetaPerZipEntry(path, false);
                });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedFiles:
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
        assertThat(fileMetaCaptor.getValue().getRestricted()).isFalse();
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertZipFileContainsFiles(tempDir.resolve("batch1.zip"), "file1", "file2");
        assertZipFileContainsFiles(tempDir.resolve("batch2.zip"), "file3");
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFiles_skips_already_completed_files() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, false);
            });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedFiles:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddUnrestrictedFiles().setNumberCompleted(1);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        verify(dataverseServiceMock).addFile(eq("pid"), pathCaptor.capture(), fileMetaCaptor.capture());
        assertThat(fileMetaCaptor.getValue().getRestricted()).isFalse();
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertZipFileContainsFiles(tempDir.resolve("batch1.zip"), "file2", "file3");
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFiles_skips_if_already_completed() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, false);
            });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedFiles:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddUnrestrictedFiles().setCompleted(true);
        editFilesLog.getAddUnrestrictedFiles().setNumberCompleted(3);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        verify(dataverseServiceMock, times(0)).addFile(anyString(), any(Path.class), any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addRestrictedFiles_skips_if_already_completed() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, true);
            });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addRestrictedFiles:
                  - file1
                  - file2
                  - file3
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        editFilesLog.getAddRestrictedFiles().setCompleted(true);
        editFilesLog.getAddRestrictedFiles().setNumberCompleted(3);
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        verify(dataverseServiceMock, times(0)).addFile(anyString(), any(Path.class), any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddRestrictedFiles()).isEqualTo("""
            numberCompleted: 3
            completed: true
            """);
    }

    @Test
    public void addUnrestrictedFiles_is_noop_if_list_is_empty() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));

        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, false);
            });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                addUnrestrictedFiles: [] # NO FILES ACTUALLY ADDED
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        verify(dataverseServiceMock, times(0)).addFile(anyString(), any(Path.class), any(FileMeta.class));
        YamlBeanAssert.assertThat(editFilesLog.getAddUnrestrictedFiles()).isEqualTo("""
            numberCompleted: 0
            completed: true
            """);
    }

    @Test
    public void both_restricted_and_unrestricted_files_added() throws Exception {
        // Given
        var tempDir = Files.createDirectory(testDir.resolve("temp"));
        UtilityServices utilityServices = UtilityServicesImpl.builder()
            .tempDir(testDir.resolve("temp"))
            .maxUploadSize(1000000)
            .maxNumberOfFilesPerUpload(2).build(); // Causes first batch to be limited to 2 files
        Files.createFile(dataDir.resolve("file1"));
        Files.createFile(dataDir.resolve("file2"));
        Files.createFile(dataDir.resolve("file3"));
        Files.createFile(dataDir.resolve("file4"));
        Files.createFile(dataDir.resolve("file5"));
        Files.createFile(dataDir.resolve("file6"));
        when(dataverseServiceMock.getFiles("pid")).thenReturn(
            List.of());
        /*
         * (Ab?)using the "thenAnswer" to save then upload the ZIP file, because it is deleted after upload.
         */
        when(dataverseServiceMock.addFile(anyString(), any(Path.class), any(FileMeta.class)))
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch1.zip").toFile());
                return createFileMetaPerZipEntry(path, true);
            }).thenAnswer(
                invocation -> {
                    var path = invocation.getArgument(1, Path.class);
                    FileUtils.copyFile(path.toFile(), path.resolveSibling("batch2.zip").toFile());
                    return createFileMetaPerZipEntry(path, true);
                })
            .thenAnswer(invocation -> {
                var path = invocation.getArgument(1, Path.class);
                FileUtils.copyFile(path.toFile(), path.resolveSibling("batch3.zip").toFile());
                return createFileMetaPerZipEntry(path, false);
            }).thenAnswer(
                invocation -> {
                    var path = invocation.getArgument(1, Path.class);
                    FileUtils.copyFile(path.toFile(), path.resolveSibling("batch4.zip").toFile());
                    return createFileMetaPerZipEntry(path, false);
                });
        var editFilesRoot = yamlService.readYamlFromString("""
            editFiles:
                # Note that the order of execution is always as follows: restricted files first, then unrestricted files, no matter the order in the YAML
                addRestrictedFiles:
                  - file1
                  - file2
                  - file3
                addUnrestrictedFiles:
                  - file4
                  - file5
                  - file6
            """, EditFilesRoot.class);
        var editFilesLog = new EditFilesLog();
        var filesEditor = new FilesEditor(UUID.randomUUID(), dataDir, editFilesRoot.getEditFiles(), dataverseServiceMock, utilityServices, editFilesLog);

        // When
        filesEditor.editFiles("pid");

        // Then
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<FileMeta> fileMetaCaptor = ArgumentCaptor.forClass(FileMeta.class);
        verify(dataverseServiceMock, times(4)).addFile(eq("pid"), pathCaptor.capture(), fileMetaCaptor.capture());
        assertThat(fileMetaCaptor.getAllValues().get(0).getRestricted()).isTrue();
        assertThat(fileMetaCaptor.getAllValues().get(1).getRestricted()).isTrue();
        assertThat(fileMetaCaptor.getAllValues().get(2).getRestricted()).isFalse();
        assertThat(fileMetaCaptor.getAllValues().get(3).getRestricted()).isFalse();
        assertThat(pathCaptor.getValue().toString())
            .withFailMessage("Uploaded file should be a ZIP file in the temp directory")
            .contains(testDir.resolve("temp").toString())
            .endsWith(".zip");
        assertThat(pathCaptor.getValue())
            .withFailMessage("ZIP file should be deleted after upload")
            .doesNotExist();
        assertZipFileContainsFiles(tempDir.resolve("batch1.zip"), "file1", "file2");
        assertZipFileContainsFiles(tempDir.resolve("batch2.zip"), "file3");
        assertZipFileContainsFiles(tempDir.resolve("batch3.zip"), "file4", "file5");
        assertZipFileContainsFiles(tempDir.resolve("batch4.zip"), "file6");

    }

}
