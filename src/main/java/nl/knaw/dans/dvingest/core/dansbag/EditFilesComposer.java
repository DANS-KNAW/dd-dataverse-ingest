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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.bagprocessor.DataversePath;
import nl.knaw.dans.dvingest.core.dansbag.deposit.FileInfo;
import nl.knaw.dans.dvingest.core.yaml.AddEmbargo;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.FromTo;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Composes the EditFiles object from the information found in the deposit. This object handles the case of a deposit for a new dataset. Several methods are abstract, so that they can be overridden by
 * a subclass that handles the case of an update to an existing dataset.
 */
@Slf4j
@AllArgsConstructor
public class EditFilesComposer {
    protected static final SimpleDateFormat yyyymmddFormat = new SimpleDateFormat("yyyy-MM-dd");

    @NonNull
    protected final Map<Path, FileInfo> files;
    @NonNull
    protected final Instant dateAvailable;

    protected final Pattern fileExclusionPattern;
    @NonNull
    protected final List<String> embargoExclusions;

    public EditFiles composeEditFiles() {
        var pathFileInfoMap = files;
        var renamedFiles = getAutoRenameMap(pathFileInfoMap);
        var ignoredFiles = getFilesToIgnore(pathFileInfoMap);

        var editFiles = new EditFiles();
        editFiles.setIgnoreFiles(ignoredFiles);
        pathFileInfoMap = removeIgnoredFiles(pathFileInfoMap, ignoredFiles);

        editFiles.setAutoRenameFiles(getAutoRenamedFiles(renamedFiles));
        editFiles.setAddRestrictedFiles(getRestrictedFilesToAdd(pathFileInfoMap));
        editFiles.setUpdateFileMetas(getUpdatedFileMetas(pathFileInfoMap));

        var filePathsToEmbargo = getEmbargoedFiles(pathFileInfoMap, dateAvailable);
        if (!filePathsToEmbargo.isEmpty()) {
            var addEmbargo = new AddEmbargo();
            addEmbargo.setDateAvailable(yyyymmddFormat.format(Date.from(dateAvailable)));
            addEmbargo.setFilePaths(filePathsToEmbargo.stream().map(Path::toString).toList());
            editFiles.setAddEmbargoes(List.of(addEmbargo));
        }
        return editFiles;
    }

    /**
     * Get the files that should not be processed by the ingest service.
     *
     * @param files the file infos found inf files.xml
     * @return a list of file paths that should be ignored
     */
    protected List<String> getFilesToIgnore(Map<Path, FileInfo> files) {
        if (fileExclusionPattern == null) {
            return List.of();
        }
        return files.keySet().stream()
            .map(Path::toString)
            .filter(string -> fileExclusionPattern.matcher(string).matches()).toList();
    }

    /**
     * Get the files that should be added as restricted files.
     *
     * @param files the file infos found in files.xml
     * @return a list of file paths that should be added as restricted files
     */
    protected List<String> getRestrictedFilesToAdd(Map<Path, FileInfo> files) {
        return files.entrySet().stream()
            .filter(entry -> entry.getValue().getMetadata().getRestricted())
            .map(entry -> entry.getKey().toString())
            .toList();
    }

    /**
     * Get the files that have metadata that should be updated.
     *
     * @param files the file infos found in files.xml
     * @return a list of FileMetas that should be updated
     */
    protected List<FileMeta> getUpdatedFileMetas(Map<Path, FileInfo> files) {
        return files.values().stream()
            .map(FileInfo::getMetadata)
            .filter(this::hasAttributes)
            .toList();
    }

    private List<Path> getEmbargoedFiles(Map<Path, FileInfo> files, Instant dateAvailable) {
        var now = Instant.now();
        if (dateAvailable.isAfter(now)) {
            return files.keySet().stream()
                .filter(f -> !embargoExclusions.contains(f.toString())).toList();
        }
        else {
            log.debug("Date available in the past, no embargo: {}", dateAvailable);
            return List.of();
        }
    }

    private boolean hasAttributes(FileMeta fileMeta) {
        return (fileMeta.getCategories() != null && !fileMeta.getCategories().isEmpty()) ||
            (fileMeta.getDescription() != null && !fileMeta.getDescription().isBlank());
    }

    private Map<Path, FileInfo> removeIgnoredFiles(Map<Path, FileInfo> files, List<String> ignoredFiles) {
        return files.entrySet().stream()
            .filter(entry -> !ignoredFiles.contains(entry.getKey().toString()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<FromTo> getAutoRenamedFiles(Map<String, String> renamedFiles) {
        return renamedFiles.entrySet().stream()
            .map(entry -> new FromTo(entry.getKey(), entry.getValue()))
            .toList();
    }

    protected Map<String, String> getAutoRenameMap(Map<Path, FileInfo> files) {
        return files.entrySet().stream()
            .filter(entry -> entry.getValue().isSanitized())
            .collect(Collectors.toMap(entry -> entry.getKey().toString(),
                entry -> new DataversePath(entry.getValue().getMetadata().getDirectoryLabel(), entry.getValue().getMetadata().getLabel()).toString()));
    }
}
