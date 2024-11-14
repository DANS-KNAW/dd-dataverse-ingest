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
package nl.knaw.dans.dvingest.core.service;

import nl.knaw.dans.dvingest.core.TestDirFixture;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

public class PathIteratorZipperTest extends TestDirFixture {

    @Test
    public void zip_should_zip_single_file() throws Exception {
        // Given
        var inputDir = testDir.resolve("onefile");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);

        // When
        var pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("onefile.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("onefile.zip")).exists();
        try (var zipFile = new ZipFile(testDir.resolve("onefile.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactly("file.txt");
        }
    }

    @Test
    public void zip_should_zip_multiple_files() throws Exception {
        // Given
        var inputDir = testDir.resolve("multiplefiles");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file1.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file2.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);

        // When
        var pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("multiplefiles.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("multiplefiles.zip")).exists();
        try (var zipFile = new ZipFile(testDir.resolve("multiplefiles.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file1.txt", "file2.txt");
        }
    }

    @Test
    public void zip_should_zip_only_files() throws Exception {
        // Given
        var inputDir = testDir.resolve("onlyfiles");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        Files.createDirectory(inputDir.resolve("dir"));

        // When
        var pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("onlyfiles.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("onlyfiles.zip")).exists();
        try (var zipFile = new ZipFile(testDir.resolve("onlyfiles.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactly("file.txt");
        }
    }

    @Test
    public void zip_should_zip_also_files_in_subdirectories() throws Exception {
        // Given
        var inputDir = testDir.resolve("subdirectories");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file1.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file2.txt").toFile(), "Hola, mundo!", StandardCharsets.UTF_8);
        Files.createDirectories(inputDir.resolve("dir1"));
        FileUtils.writeStringToFile(inputDir.resolve("dir1/file3.txt").toFile(), "Hallo, wereld!", StandardCharsets.UTF_8);
        Files.createDirectories(inputDir.resolve("dir2"));
        FileUtils.writeStringToFile(inputDir.resolve("dir2/file4.txt").toFile(), "Bonjour, monde!", StandardCharsets.UTF_8);

        // When
        var pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("subdirectories.zip"))
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("subdirectories.zip")).exists();
        try (var zipFile = new ZipFile(testDir.resolve("subdirectories.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file1.txt", "file2.txt", "dir1/file3.txt", "dir2/file4.txt");
        }
    }

    @Test
    public void zip_should_zip_only_files_up_to_maxNumberOfFiles() throws Exception {
        // Given
        var inputDir = testDir.resolve("maxnumberoffiles");
        Files.createDirectories(inputDir);
        FileUtils.writeStringToFile(inputDir.resolve("file1.txt").toFile(), "Hello, world!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file2.txt").toFile(), "Hola, mundo!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file3.txt").toFile(), "Hallo, wereld!", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(inputDir.resolve("file4.txt").toFile(), "Bonjour, monde!", StandardCharsets.UTF_8);

        // When
        var pathIterator = new PathIterator(FileUtils.iterateFiles(inputDir.toFile(), null, true));
        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("part1.zip"))
            .maxNumberOfFiles(2)
            .build()
            .zip();

        PathIteratorZipper.builder()
            .rootDir(inputDir)
            .sourceIterator(pathIterator)
            .targetZipFile(testDir.resolve("part2.zip"))
            .maxNumberOfFiles(2)
            .build()
            .zip();

        // Then
        assertThat(testDir.resolve("part1.zip")).exists();
        try (var zipFile = new ZipFile(testDir.resolve("part1.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file1.txt", "file2.txt");
        }
        assertThat(testDir.resolve("part2.zip")).exists();
        try (var zipFile = new ZipFile(testDir.resolve("part2.zip").toFile())) {
            assertThat(zipFile.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("file3.txt", "file4.txt");
        }
    }
}