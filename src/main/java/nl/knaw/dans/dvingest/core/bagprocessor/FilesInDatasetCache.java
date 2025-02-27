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

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Keeps track of the FileMeta objects of files in a dataset. The cache is initialized by downloading the files from the dataset.
 * </p>
 */
@Slf4j
public class FilesInDatasetCache {
    private final DataverseService dataverseService;
    /*
     * Key: filepath after auto-rename / Value: FileMeta object
     */
    @Getter
    private final Map<String, FileMeta> filesInDataset = new java.util.HashMap<>();
    @Getter
    private final Map<String, String> autoRenamedFiles;
    private boolean initialized = false;

    public FilesInDatasetCache(@NonNull DataverseService dataverseService, @NonNull Map<String, String> autoRenamedFiles) {
        this.dataverseService = dataverseService;
        this.autoRenamedFiles = Collections.unmodifiableMap(autoRenamedFiles);
    }

    /**
     * Returns the cached FileMeta object for the given filepath. The filepath will be auto-renamed if it is in the renamedFiles map, so the local path from the bag is used.
     *
     * @param filepath before auto-rename
     * @return the FileMeta object for the file in the dataset
     */
    public FileMeta get(@NonNull String filepath) {
        return filesInDataset.get(autoRenamePath(filepath));
    }

    private String autoRenamePath(@NonNull String filepath) {
        return autoRenamedFiles.getOrDefault(filepath, filepath);
    }

    /**
     * Adds or updates the FileMeta object for the given filepath. No auto-rename is done. It is assumed that the FileMeta object was returned by the Dataverse API or that the filepath was already
     * auto-renamed by the client.
     *
     * @param fileMeta the FileMeta object for the file in the dataset
     */
    public void put(@NonNull FileMeta fileMeta) {
        filesInDataset.put(getPath(fileMeta), fileMeta);
    }

    /**
     * A move operation is in fact a file metadata update operation in which the directory label and label are updated. This method allows to update cached FileMeta object before the move with the new
     * filepath. The filepath will be auto-renamed if it is in the renamedFiles map, so the local path from the bag is used.
     *
     * @param fromPath the current path, before auto-rename, so paths as found in the bag can be used
     * @param toPath   the new path, before auto-rename, so paths as found in the bag can be used
     * @return the updated cached FileMeta object
     */
    public FileMeta modifyCachedFileMetaForFileMove(@NonNull String fromPath, @NonNull String toPath) {
        var fromDataversePath = new DataversePath(autoRenamePath(fromPath));
        var toDataversePath = new DataversePath(autoRenamePath(toPath));
        var cachedFileMeta = filesInDataset.get(fromDataversePath.toString());
        if (cachedFileMeta == null) {
            throw new IllegalArgumentException("File to move not found in dataset: " + fromDataversePath);
        }
        cachedFileMeta.setDirectoryLabel(toDataversePath.getDirectoryLabel());
        cachedFileMeta.setLabel(toDataversePath.getLabel());
        // Ensure that the file meta is findable under the new path.
        filesInDataset.remove(fromDataversePath.toString());
        filesInDataset.put(toDataversePath.toString(), cachedFileMeta);
        return cachedFileMeta;
    }

    /**
     * Updates the cached FileMeta object with the new description, categories and restrict value. The filepath will be auto-renamed if it is in the renamedFiles map, so the local path from the bag is
     * used. If restrict is not to changed, it must be set to null, otherwise the API will return an error.
     *
     * @param fileMeta the new FileMeta object with the updated values (without the datafile)
     * @return the updated cached FileMeta object
     */
    public FileMeta modifyFileMetaForUpdate(@NonNull FileMeta fileMeta) {
        var dataversePath = new DataversePath(fileMeta.getDirectoryLabel(), fileMeta.getLabel());
        var cachedFileMeta = filesInDataset.get(dataversePath.toString());
        if (cachedFileMeta == null) {
            throw new IllegalArgumentException("File to update not found in dataset: " + dataversePath);
        }
        cachedFileMeta.setDescription(fileMeta.getDescription());
        cachedFileMeta.setCategories(fileMeta.getCategories());
        cachedFileMeta.setRestricted(fileMeta.getRestrict());
        return cachedFileMeta;
    }

    /**
     * Removes the FileMeta object for the given filepath. The filepath will be auto-renamed if it is in the renamedFiles map, so the local path from the bag is used.
     *
     * @param filepath before auto-rename
     */
    public void remove(@NonNull String filepath) {
        filesInDataset.remove(autoRenamePath(filepath));
    }

    /**
     * Download the file metadata from the dataset with the given persistent identifier, initializing the cache. This method can only be called once. Subsequent calls will throw an exception.
     *
     * @param pid                 the persistent identifier of the dataset
     * @param includeDraftVersion whether to download from a draft version of the dataset, if that is the latest version
     * @throws IOException           if an I/O error occurs
     * @throws DataverseException    if the Dataverse API returns an error
     * @throws IllegalStateException if the cache is already initialized
     */
    public void downloadFromDataset(@NonNull String pid, boolean includeDraftVersion) throws IOException, DataverseException {
        if (initialized) {
            throw new IllegalStateException("Cache already initialized");
        }

        var files = dataverseService.getFiles(pid, includeDraftVersion);
        for (var file : files) {
            filesInDataset.put(getPath(file), file);
        }
        initialized = true;
    }

    /**
     * Returns the number of files in the dataset.
     *
     * @return the number of files in the dataset
     */
    public int getNumberOfFilesInDataset() {
        return filesInDataset.size();
    }

    public void putAll(List<FileMeta> fileMetas) {
        for (var fileMeta : fileMetas) {
            put(fileMeta);
        }
    }

    public void removeAll(List<String> paths) {
        for (var path : paths) {
            remove(path);
        }
    }

    private String getPath(@NonNull FileMeta file) {
        var dataversePath = new DataversePath(file.getDirectoryLabel(), file.getLabel());
        return dataversePath.toString();
    }

}
