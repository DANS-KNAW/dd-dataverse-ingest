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

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.YamlService;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Slf4j
@ToString
public class DataverseIngestDeposit implements Comparable<DataverseIngestDeposit>, Deposit {
    private final OffsetDateTime creationTimestamp;

    private final UUID id;

    private Path location;

    private final PropertiesConfiguration depositProperties;
    private final YamlService yamlService;

    public DataverseIngestDeposit(@NonNull Path location, @NonNull YamlService yamlService) {
        this.location = location;
        this.yamlService = yamlService;
        depositProperties = new PropertiesConfiguration();
        try {
            depositProperties.read(Files.newBufferedReader(location.resolve("deposit.properties")));
            var creationTimestamp = depositProperties.getString(CREATION_TIMESTAMP_KEY);
            if (creationTimestamp == null) {
                throw new IllegalStateException("Deposit " + location + " does not contain a creation timestamp");
            }
            this.creationTimestamp = OffsetDateTime.parse(creationTimestamp);
            this.id = UUID.fromString(location.getFileName().toString());
        }
        catch (IOException | ConfigurationException e) {
            throw new IllegalStateException("Error loading deposit properties from " + location.resolve("deposit.properties"), e);
        }

    }

    @Override
    public String getUpdatesDataset() {
        return depositProperties.getString(UPDATES_DATASET_KEY);
    }

    @Override
    public boolean convertDansDepositIfNeeded() {
        return false;
    }

    @Override
    public List<DataverseIngestBag> getBags() throws IOException {
        try (var files = Files.list(location)) {
            return files
                .filter(Files::isDirectory)
                .map(path -> new DataverseIngestBag(path, yamlService))
                .sorted()
                .toList();
        }
    }

    public void updateProperties(Map<String, String> properties) {
        properties.forEach(depositProperties::setProperty);
        // Save the updated properties
        try (var writer = Files.newBufferedWriter(location.resolve("deposit.properties"))) {
            depositProperties.write(writer);
        }
        catch (ConfigurationException | IOException e) {
            throw new RuntimeException("Error updating deposit properties", e);
        }
    }

    @Override
    public void onSuccess(@NonNull String pid, String message) {
        var map = new HashMap<String, String>();
        map.put(IDENTIFIER_DOI_KEY, pid);
        updateProperties(map);
    }

    @Override
    public void onFailed(String pid, String message) {
        var map = new HashMap<String, String>();
        map.put(STATE_LABEL_KEY, "FAILED");
        map.put(STATE_DESCRIPTION_KEY, message);
        if (pid != null) {
            map.put(IDENTIFIER_DOI_KEY, pid);
        }
        updateProperties(map);
    }

    @Override
    public void onRejected(String pid, String message) {
        var map = new HashMap<String, String>();
        map.put(STATE_LABEL_KEY, "REJECTED");
        map.put(STATE_DESCRIPTION_KEY, message);
        if (pid != null) {
            map.put(IDENTIFIER_DOI_KEY, pid);
        }
        updateProperties(map);
    }

    @Override
    public void validate() {
        log.debug("No validation implemented for DataverseIngestDeposit");
    }

    @Override
    public void moveTo(Path toPath) throws IOException {
        log.debug("Moving deposit {} to {}", location, toPath);
        Files.move(location, toPath.resolve(location.getFileName()));
        location = toPath.resolve(location.getFileName());
    }

    @Override
    public int compareTo(@NotNull DataverseIngestDeposit dataverseIngestDeposit) {
        return getCreationTimestamp().compareTo(dataverseIngestDeposit.getCreationTimestamp());
    }
}
