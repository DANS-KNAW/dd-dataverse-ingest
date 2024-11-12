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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.MetadataFieldDeserializer;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import org.apache.commons.io.FileUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Getter
@Slf4j
public class Deposit implements Comparable<Deposit> {
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MetadataField.class, new MetadataFieldDeserializer());
        MAPPER.registerModule(module);
    }

    private Path location;

    private final Properties depositProperties;

    public Deposit(@NonNull Path location) {
        this.location = location;
        this.depositProperties = new Properties();
        try {
            depositProperties.load(Files.newBufferedReader(location.resolve("deposit.properties")));
        }
        catch (IOException e) {
            throw new IllegalStateException("Error loading deposit properties from " + location.resolve("deposit.properties"), e);
        }
    }

    public UUID getId() {
        return UUID.fromString(location.getFileName().toString());
    }

    @SuppressWarnings("unchecked")
    public Dataset getDatasetMetadata() throws IOException {
        var dataset = MAPPER.readValue(FileUtils.readFileToString(getBagDir().resolve("dataset.yml").toFile(), "UTF-8"), Dataset.class);
        dataset.getDatasetVersion().setFiles(Collections.emptyList()); // files = null or a list of files is not allowed
        return dataset;
    }

    public Path getBagDir() {
        try (var files = Files.list(location).filter(Files::isDirectory)) {
            List<Path> filesList = files.toList();
            if (filesList.size() == 1) {
                return filesList.get(0);
            }
            else {
                throw new IllegalStateException("Deposit " + location + " should contain exactly one directory");
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Error listing files in deposit " + location, e);
        }
    }

    public Path getFilesDir() {
        return getBagDir().resolve("data");
    }

    public int getSeqNumber() {
        return depositProperties.get("seqNumber") == null ? -1 : Integer.parseInt(depositProperties.getProperty("seqNumber"));
    }

    public OffsetDateTime getCreationTimestamp() {
        return OffsetDateTime.parse(depositProperties.getProperty("creation.timestamp"));
    }

    public void moveTo(Path targetDir) throws IOException {
        Files.move(location, targetDir.resolve(location.getFileName()));
        location = targetDir.resolve(location.getFileName());
    }

    @Override
    public int compareTo(@NotNull Deposit deposit) {
        if (getSeqNumber() != -1 && deposit.getSeqNumber() != -1) {
            return Integer.compare(getSeqNumber(), deposit.getSeqNumber());
        }
        else if (getCreationTimestamp() != null && deposit.getCreationTimestamp() != null) {
            return getCreationTimestamp().compareTo(deposit.getCreationTimestamp());
        }
        else {
            throw new IllegalStateException("Deposit " + getId() + " or " + deposit.getId() + " has no sequence number");
        }
    }
}
