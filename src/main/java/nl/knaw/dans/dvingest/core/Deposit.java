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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.MetadataFieldDeserializer;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Slf4j
public class Deposit {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @NonNull
    private Path location;

    public UUID getId() {
        return UUID.fromString(location.getFileName().toString());
    }

    @SuppressWarnings("unchecked")
    public Dataset getDatasetMetadata() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MetadataField.class, new MetadataFieldDeserializer());
        mapper.registerModule(module);
        var dataset = mapper.readValue(FileUtils.readFileToString(location.resolve("dataset.yml").toFile(), "UTF-8"), Dataset.class);
        dataset.getDatasetVersion().setFiles(Collections.emptyList()); // files = null or a list of files is not allowed
        return dataset;
    }

    public Path getFilesDir() {
        return location.resolve("files");
    }

    public void moveTo(Path targetDir) throws IOException {
        Files.move(location, targetDir.resolve(location.getFileName()));
        location = targetDir.resolve(location.getFileName());
    }
}
