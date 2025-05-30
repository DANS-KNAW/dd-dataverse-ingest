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
import nl.knaw.dans.dvingest.core.dansbag.deposit.FileInfo;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.FromTo;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nl.knaw.dans.dvingest.core.dansbag.SetUtils.diff;
import static nl.knaw.dans.dvingest.core.dansbag.SetUtils.union;

@Slf4j
public class EditFilesComposerForUpdate extends EditFilesComposer {
    private final String updatesDatasetPid;
    private final DataverseService dataverseService;

    public EditFilesComposerForUpdate(Map<Path, FileInfo> files, Instant dateAvailable, String updatesDatasetPid, Pattern fileExclusionPattern, Pattern filesForSeparateUploadPattern,
        List<String> embargoExclusions,
        DataverseService dataverseService) {
        super(files, dateAvailable, fileExclusionPattern, filesForSeparateUploadPattern, embargoExclusions);
        this.updatesDatasetPid = updatesDatasetPid;
        this.dataverseService = dataverseService;
    }

    @Override
    public EditFiles composeEditFiles() {
        var pathFileInfoMap = files;
        var renamedFiles = getAutoRenameMap(pathFileInfoMap);
        // TODO: this should be a read-only variant of the cache
        FilesInDatasetCache filesInDatasetCache = new FilesInDatasetCache(dataverseService, renamedFiles);
        try {
            // Do not get the files from the draft version, if it exists, because this will mess up any retry
            filesInDatasetCache.downloadFromDataset(updatesDatasetPid, false);
        }
        catch (IOException | DataverseException e) {
            log.error("Could not download files from dataset with pid {}", updatesDatasetPid, e);
        }
        var editFiles = new EditFiles();
        editFiles.setAutoRenameFiles(getAutoRenamedFiles(renamedFiles));

        // move old paths to new paths
        // Convert from Path to String
        var filesInDataset = filesInDatasetCache.getFilesInDataset().entrySet().stream()
            .collect(Collectors.toMap(e -> Path.of(e.getKey()), Map.Entry::getValue));
        var oldToNewPathMovedFiles = getOldToNewPathOfFilesToMove(filesInDataset, pathFileInfoMap);
        var fileMovementFromTos = oldToNewPathMovedFiles.entrySet().stream()
            .map(e -> new FromTo(e.getKey().toString(), e.getValue().toString()))
            .collect(Collectors.toList());
        log.debug("fileMovements = {}", fileMovementFromTos);
        editFiles.setMoveFiles(fileMovementFromTos);

        /*
         * File replacement can only happen on files with paths that are not also involved in a rename/move action. Otherwise, we end up with:
         *
         * - trying to update the file metadata by a database ID that is not the "HEAD" of a file version history (which Dataverse doesn't allow anyway, it
         * fails with "You cannot edit metadata on a dataFile that has been replaced"). This happens when a file A is renamed to B, but a different file A
         * is also added in the same update. Schematically, (1) means unique file 1, A is a file name:
         * V1    -> V2
         *
         * (1)A -> (1)B (move)
         * ..   -> (2)A  (add)
         *
         * - trying to add a file with a name that already exists. This happens when a file A is renamed to B, while B is also part of the latest version (V1)
         *
         * V1 -> V2
         * (1)A -> (1)B (move)
         * (2)B -> .. (delete)
         *
         */
        var fileReplacementCandidates = filesInDataset.entrySet().stream()
            .filter(pathToFileInfoEntry -> !oldToNewPathMovedFiles.containsKey(pathToFileInfoEntry.getKey()))
            .filter(pathToFileInfoEntry -> !oldToNewPathMovedFiles.containsValue(pathToFileInfoEntry.getKey())) // TODO in the OG version, this was a Set; check what performance is like
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var filesToReplace = getFilesToReplace(pathFileInfoMap, fileReplacementCandidates);
        log.debug("filesToReplace = {}", filesToReplace);
        editFiles.setReplaceFiles(filesToReplace.stream().map(Path::toString).collect(Collectors.toList()));

        /*
         * To find the files to delete we start from the paths in the deposit payload. In principle, these paths are remaining, so should NOT be deleted.
         * However, if a file is moved/renamed to a path that was also present in the latest version, then the old file at that path must first be deleted
         * (and must therefore NOT be included in candidateRemainingFiles). Otherwise, we'll end up trying to use an existing (directoryLabel, label) pair.
         */
        var newPathsOfMovedFiles = new HashSet<>(oldToNewPathMovedFiles.values());
        var candidateRemainingFiles = diff(pathFileInfoMap.keySet(), newPathsOfMovedFiles);

        /*
         * The paths to delete, now, are the paths in the latest version minus the remaining files. We further subtract the old paths of the moved files.
         * This may be a bit confusing, but the goal is to make sure that the underlying FILE remains present (after all, it is to be renamed/moved). The
         * path itself WILL be "removed" from the latest version by the move. (It MAY be filled again by a file addition in the same update, though.)
         */
        var oldPathsOfMovedFiles = new HashSet<>(oldToNewPathMovedFiles.keySet());
        var pathsToDelete = diff(diff(filesInDataset.keySet(), candidateRemainingFiles), oldPathsOfMovedFiles);
        log.debug("pathsToDelete = {}", pathsToDelete);
        editFiles.setDeleteFiles(pathsToDelete.stream().map(Path::toString).collect(Collectors.toList()));

        /*
         * After the movements have been performed, which paths are occupied? We start from the paths of the latest version (pathToFileMetaInLatestVersion.keySet)
         *
         * The old paths of the moved files (oldToNewPathMovedFiles.keySet) are no longer occupied, so they must be subtracted. This is important in the case where
         * a deposit renames/moves a file (leaving the checksum unchanged) but provides a new file for the vacated path.
         *
         * The paths of the deleted files (pathsToDelete) are no longer occupied, so must be subtracted. (It is not strictly necessary for the calculation
         * of pathsToAdd, but done anyway to keep the logic consistent.)
         *
         * The new paths of the moved files (oldToNewPathMovedFiles.values.toSet) *are* now occupied, so the must be added. This is important to
         * avoid those files from being marked as "new" files, i.e. files to be added.
         *
         * All paths in the deposit that are not occupied, are new files to be added.
         */
        var diffed = diff(diff(filesInDataset.keySet(), newPathsOfMovedFiles), pathsToDelete);
        var occupiedPaths = union(diffed, oldToNewPathMovedFiles.values());

        log.debug("occupiedPaths = {}", occupiedPaths);
        var pathsToAdd = diff(pathFileInfoMap.keySet(), occupiedPaths);
        editFiles.setAddRestrictedIndividually(pathsToAdd.stream()
            .filter(this::forSeparateUpload)
            .filter(p -> pathFileInfoMap.get(p).getMetadata().getRestricted()).toList().stream().map(Path::toString).toList());
        editFiles.setAddRestrictedFiles(pathsToAdd.stream()
            .filter(p -> !forSeparateUpload(p))
            .filter(p -> pathFileInfoMap.get(p).getMetadata().getRestricted()).toList().stream().map(Path::toString).toList());

        editFiles.setAddUnrestrictedIndividually(pathsToAdd.stream()
            .filter(this::forSeparateUpload)
            .filter(p -> !pathFileInfoMap.get(p).getMetadata().getRestricted()).toList().stream().map(Path::toString).toList());
        editFiles.setAddUnrestrictedFiles(pathsToAdd.stream()
            .filter(p -> !forSeparateUpload(p))
            .filter(p -> !pathFileInfoMap.get(p).getMetadata().getRestricted()).toList().stream().map(Path::toString).toList());

        editFiles.setUpdateFileMetas(getAttributesChanges(pathFileInfoMap, filesInDatasetCache));

        addEmbargo(editFiles, SetUtils.union(pathsToAdd, filesToReplace));
        return editFiles;
    }

    private Set<Path> getFilesToReplace(Map<Path, FileInfo> pathToFileInfo, Map<Path, FileMeta> fileReplacementCandidates) {

        var intersection = SetUtils.intersection(pathToFileInfo.keySet(), fileReplacementCandidates.keySet());

        log.debug("Intersection paths for replacing = {}", intersection);

        return intersection.stream()
            .filter(p -> !pathToFileInfo.get(p).getChecksum().equals(fileReplacementCandidates.get(p).getDataFile().getChecksum().getValue()))
            .collect(Collectors.toSet());

    }

    /**
     * Creating a mapping for moving files to a new location. To determine this, the file needs to be unique in the old and the new version, because its checksum is used to locate it. Files that occur
     * multiple times in either the old or the new version cannot be moved in this way. They will appear to have been deleted in the old version and added in the new. This has the same net result,
     * except that the "Changes" overview in Dataverse does not record that the file was effectively moved.
     *
     * @param pathToFileMetaInLatestVersion map from path to file metadata in the old version
     * @param pathToFileInfo                map from path to file info in the new version (i.e. the deposit).
     * @return a map from old path to new path
     */
    Map<Path, Path> getOldToNewPathOfFilesToMove(Map<Path, FileMeta> pathToFileMetaInLatestVersion, Map<Path, FileInfo> pathToFileInfo) {

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
            .filter(entry -> !entry.getKey().equals(entry.getValue())) // filter out files that are not moved (this was not present in the old code)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    private Map<String, Path> getChecksumsToPathOfNonDuplicateFiles(Map<Path, String> pathToChecksum) {
        // inverse map first
        var inverse = pathToChecksum.entrySet().stream()
            .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        // filter out items with 0 or more than 1 item
        return inverse.entrySet().stream()
            .filter(item -> item.getValue().size() == 1)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }

    private List<FileMeta> getAttributesChanges(Map<Path, FileInfo> files, FilesInDatasetCache filesInDatasetCache) {
        List<FileMeta> result = new ArrayList<>();
        for (var entry : files.entrySet()) {
            var path = entry.getKey();
            var fileInfo = entry.getValue();
            var fileMeta = fileInfo.getMetadata();
            var fileInDataset = filesInDatasetCache.get(path.toString());
            if (fileInDataset == null) {
                log.debug("File {} is new", path);
                continue;
            }
            if (fileMeta.getRestricted() != fileInDataset.getRestricted()) {
                log.debug("File {} has a restriction change", path);
                result.add(fileMeta);
            }
            // Categories are never null, but can be empty
            else if (!new HashSet<>(fileMeta.getCategories()).equals(new HashSet<>(fileInDataset.getCategories()))) {
                log.debug("File {} has a category change", path);
                result.add(fileMeta);
            }
            else if (!StringUtils.equals(fileMeta.getDescription(), fileInDataset.getDescription())) {
                log.debug("File {} has a description change", path);
                result.add(fileMeta);
            }
        }
        return result;
    }
}
