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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.bagprocessor.DataversePath;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class FilesInDatasetImpl implements FilesInDataset {
    private final Map<String, FileMeta> filesInDataset = new java.util.HashMap<>();
    private final DataverseService dataverseService;
    private final String pid;
    private boolean filesRetrieved = false;

    public FilesInDatasetImpl(String pid, DataverseService dataverseService) {
        this.dataverseService = dataverseService;
        this.pid = pid;
    }

    @Override
    public FileMeta get(FileMeta fileMeta) throws IOException, DataverseException {
        return filesInDataset().get(getPath(fileMeta));
    }

    @Override
    public void put(FileMeta fileMeta) throws IOException, DataverseException {
        filesInDataset().put(getPath(fileMeta), fileMeta);

    }

    @Override
    public void remove(FileMeta fileMeta) throws IOException, DataverseException {
        filesInDataset().remove(getPath(fileMeta));
    }

    private Map<String, FileMeta> filesInDataset() throws IOException, DataverseException {
        if (!filesRetrieved) {
            log.debug("Retrieving files in dataset for pid {}", pid);
            var files = dataverseService.getFiles(pid);
            for (var file : files) {
                filesInDataset.put(getPath(file), file);
            }
            filesRetrieved = true;
            log.debug("Retrieved {} files in dataset for pid {}", files.size(), pid);
        }
        else {
            log.debug("Files in dataset already retrieved for pid {}", pid);
        }
        return filesInDataset;
    }

    private String getPath(FileMeta file) {
        var dataversePath = new DataversePath(file.getDirectoryLabel(), file.getLabel());
        return dataversePath.toString();
    }
}
