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
package nl.knaw.dans.dvingest.core;

import io.dropwizard.configuration.ConfigurationException;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.EditMetadata;
import nl.knaw.dans.dvingest.core.yaml.EditMetadataRoot;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.dvingest.core.yaml.EditPermissionsRoot;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.dvingest.core.yaml.InitRoot;
import nl.knaw.dans.dvingest.core.yaml.UpdateState;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class DataverseIngestBag implements Comparable<DataverseIngestBag> {
    private final YamlServiceImpl yamService;

    public static final String INIT_YML = "init.yml";
    public static final String DATASET_YML = "dataset.yml";
    public static final String EDIT_FILES_YML = "edit-files.yml";
    public static final String EDIT_METADATA_YML = "edit-metadata.yml";
    public static final String EDIT_PERMISSIONS_YML = "edit-permissions.yml";
    public static final String UPDATE_STATE_YML = "update-state.yml";

    private final Path bagDir;

    public DataverseIngestBag(Path bagDir, YamlService yamlService) {
        this.bagDir = bagDir;
        this.yamService = (YamlServiceImpl) yamlService;
        // Minimal check to see if it is a bag
        if (!Files.exists(bagDir.resolve("bagit.txt"))) {
            throw new IllegalStateException("Not a bag: " + bagDir);
        }
    }

    public boolean looksLikeDansBag() {
        return Files.exists(bagDir.resolve("metadata/dataset.xml"));
    }

    public Init getInit() throws IOException, ConfigurationException {
        if (!Files.exists(bagDir.resolve(INIT_YML))) {
            return null;
        }
        var initRoot = yamService.readYaml(bagDir.resolve(INIT_YML), InitRoot.class);
        return initRoot.getInit();
    }

    public Dataset getDatasetMetadata() throws IOException, ConfigurationException {
        if (!Files.exists(bagDir.resolve(DATASET_YML))) {
            return null;
        }
        var dataset = yamService.readYaml(bagDir.resolve(DATASET_YML), Dataset.class);
        dataset.getDatasetVersion().setFiles(Collections.emptyList()); // files = null or a list of files is not allowed
        return dataset;
    }

    public EditFiles getEditFiles() throws IOException, ConfigurationException {
        if (!Files.exists(bagDir.resolve(EDIT_FILES_YML))) {
            return null;
        }
        var editFilesRoot = yamService.readYaml(bagDir.resolve(EDIT_FILES_YML), EditFilesRoot.class);
        return editFilesRoot.getEditFiles();
    }

    public EditMetadata getEditMetadata() throws IOException, ConfigurationException {
        if (!Files.exists(bagDir.resolve(EDIT_METADATA_YML))) {
            return null;
        }
        var editMetadataRoot = yamService.readYaml(bagDir.resolve(EDIT_METADATA_YML), EditMetadataRoot.class);
        return editMetadataRoot.getEditMetadata();
    }

    public EditPermissions getEditPermissions() throws IOException, ConfigurationException {
        if (!Files.exists(bagDir.resolve(EDIT_PERMISSIONS_YML))) {
            return null;
        }
        var editPermissionsRoot = yamService.readYaml(bagDir.resolve(EDIT_PERMISSIONS_YML), EditPermissionsRoot.class);
        return editPermissionsRoot.getEditPermissions();
    }

    public UpdateState getUpdateState() throws IOException, ConfigurationException {
        if (!Files.exists(bagDir.resolve(UPDATE_STATE_YML))) {
            return null;
        }
        return yamService.readYaml(bagDir.resolve(UPDATE_STATE_YML), UpdateState.class);
    }

    @Override
    public int compareTo(DataverseIngestBag dataverseIngestBag) {
        return bagDir.getFileName().toString().compareTo(dataverseIngestBag.bagDir.getFileName().toString());
    }

    public Path getDataDir() {
        return bagDir.resolve("data");
    }

    public String toString() {
        return bagDir.getFileName().toString();
    }

    public Path getLocation() {
        return bagDir;
    }
}
