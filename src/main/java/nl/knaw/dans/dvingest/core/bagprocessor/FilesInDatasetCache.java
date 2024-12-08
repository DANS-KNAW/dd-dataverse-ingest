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
     * A move operation is in fact a file metadata update operation in which the directory label and label are updated. This method allows to calculate the file metadata for the moved file in the
     * dataset. The filepath will be auto-renamed if it is in the renamedFiles map, so the local path from the bag is used.
     *
     * @param toPath   new filepath before auto-rename
     * @param fileMeta the FileMeta object for the file in the dataset after the move
     * @return the FileMeta object for the moved file in the dataset
     */
    public FileMeta createFileMetaForMovedFile(@NonNull String toPath, @NonNull FileMeta fileMeta) {
        var newPath = autoRenamePath(toPath);
        var dataversePath = new DataversePath(newPath);
        fileMeta.setDirectoryLabel(dataversePath.getDirectoryLabel());
        fileMeta.setLabel(dataversePath.getLabel());
        return fileMeta;
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
     * @param pid the persistent identifier of the dataset
     * @throws IOException           if an I/O error occurs
     * @throws DataverseException    if the Dataverse API returns an error
     * @throws IllegalStateException if the cache is already initialized
     */
    public void downloadFromDataset(@NonNull String pid) throws IOException, DataverseException {
        if (initialized) {
            throw new IllegalStateException("Cache already initialized");
        }

        var files = dataverseService.getFiles(pid);
        for (var file : files) {
            filesInDataset.put(getPath(file), file);
        }
        initialized = true;
    }

    private String getPath(@NonNull FileMeta file) {
        var dataversePath = new DataversePath(file.getDirectoryLabel(), file.getLabel());
        return dataversePath.toString();
    }

}
