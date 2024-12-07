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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.bagprocessor.FilesInDatasetCache;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.FromTo;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.FileInfo;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Overrides the methods in EditFilesComposer to handle the case of an update to an existing dataset.
 */
@Slf4j
public class EditFilesComposerForUpdate extends EditFilesComposer {
    private final String updatesDatasetPid;
    private final DataverseService dataverseService;
    // TODO: this should be a read-only variant the cache
    private FilesInDatasetCache filesInDatasetCache;

    public EditFilesComposerForUpdate(Deposit dansDeposit, String updatesDatasetPid, Pattern fileExclusionPattern, List<String> embargoExclusions, DataverseService dataverseService) {
        super(dansDeposit, fileExclusionPattern, embargoExclusions);
        this.updatesDatasetPid = updatesDatasetPid;
        this.dataverseService = dataverseService;
    }

    protected void init(Map<String, String> renamedFiles) {
        filesInDatasetCache = new FilesInDatasetCache(dataverseService, renamedFiles);
        try {
            filesInDatasetCache.downloadFromDataset(updatesDatasetPid);
        }
        catch (IOException | DataverseException e) {
            log.error("Could not download files from dataset with pid {}", updatesDatasetPid, e);
        }
    }

    @Override
    protected List<String> getFilesToIgnore(Map<Path, FileInfo> files) {
        super.getFilesToIgnore(files);

        return List.of();
    }

    @Override
    protected List<FromTo> getFileMovements(Map<Path, FileInfo> files) {
        // Convert from Path to String
        var filesInDataset = filesInDatasetCache.getFilesInDataset().entrySet().stream()
            .collect(Collectors.toMap(e -> Path.of(e.getKey()), Map.Entry::getValue));
        var oldToNewPath = getOldToNewPathOfFilesToMove(filesInDataset, files);
        return oldToNewPath.entrySet().stream()
            .map(e -> new FromTo(e.getKey().toString(), e.getValue().toString()))
            .collect(Collectors.toList());
    }

    /**
     * Creates a mapping for moving files to a new location. To determine this, the file needs to be unique in the old and the new version, because its checksum is used to locate it. Files that occur
     * multiple times in either the old or the new version cannot be moved in this way. They will appear to have been deleted in the old version and added in the new. This has the same net result,
     * except that the "Changes" overview in Dataverse does not record that the file was effectively moved.
     *
     * @param pathToFileMetaInLatestVersion map from path to file metadata in the old version
     * @param pathToFileInfo                map from path to file info in the new version (i.e. the deposit).
     * @return map from old path to new path for files that can be moved.
     */
    private Map<Path, Path> getOldToNewPathOfFilesToMove(Map<Path, FileMeta> pathToFileMetaInLatestVersion, Map<Path, FileInfo> pathToFileInfo) {

        var depositChecksums = pathToFileInfo.entrySet().stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().getChecksum()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var latestFileChecksums = pathToFileMetaInLatestVersion.entrySet().stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().getDataFile().getChecksum().getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var checksumsToPathNonDuplicatedFilesInDeposit = getChecksumsToPathOfNonDuplicateFiles(depositChecksums);
        var checksumsToPathNonDuplicatedFilesInLatestVersion = getChecksumsToPathOfNonDuplicateFiles(latestFileChecksums);

        var intersects = checksumsToPathNonDuplicatedFilesInDeposit.keySet().stream()
            .filter(checksumsToPathNonDuplicatedFilesInLatestVersion::containsKey)
            .collect(Collectors.toSet());

        return intersects.stream()
            .map(c -> Map.entry(checksumsToPathNonDuplicatedFilesInLatestVersion.get(c), checksumsToPathNonDuplicatedFilesInDeposit.get(c)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    private Map<String, Path> getChecksumsToPathOfNonDuplicateFiles(Map<Path, String> pathToChecksum) {
        // inverse map first
        var inversed = pathToChecksum.entrySet().stream()
            .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        // filter out items with 0 or more than 1 item
        return inversed.entrySet().stream()
            .filter(item -> item.getValue().size() == 1)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }

    @Override
    protected List<String> getRestrictedFilesToAdd(Map<Path, FileInfo> files) {
        return super.getRestrictedFilesToAdd(files);
    }

    @Override
    protected List<FileMeta> getUpdatedFileMetas(Map<Path, FileInfo> files) {
        return super.getUpdatedFileMetas(files);
    }

    @Override
    protected List<String> getDeleteFiles(Map<Path, FileInfo> files) {
        return super.getDeleteFiles(files);
    }
}
