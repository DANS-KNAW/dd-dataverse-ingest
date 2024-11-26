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

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Getter
@Slf4j
@ToString
public class DataverseIngestDeposit implements Comparable<DataverseIngestDeposit>, Deposit {
    private final OffsetDateTime creationTimestamp;

    private final UUID id;

    private Path location;

    private final Properties depositProperties;
    private final String updatesDataset;
    private final YamlService yamlService;

    public DataverseIngestDeposit(@NonNull Path location, @NonNull YamlService yamlService) {
        this.location = location;
        this.yamlService = yamlService;
        this.depositProperties = new Properties();
        try {
            depositProperties.load(Files.newBufferedReader(location.resolve("deposit.properties")));
            var creationTimestamp = depositProperties.getProperty("creation.timestamp");
            if (creationTimestamp == null) {
                throw new IllegalStateException("Deposit " + location + " does not contain a creation timestamp");
            }
            this.creationTimestamp = OffsetDateTime.parse(creationTimestamp);
            this.id = UUID.fromString(location.getFileName().toString());
            this.updatesDataset = depositProperties.getProperty("updates.dataset");
        }
        catch (IOException e) {
            throw new IllegalStateException("Error loading deposit properties from " + location.resolve("deposit.properties"), e);
        }
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

    @Override
    public void onSuccess() {

    }

    @Override
    public void onFailed() {

    }

    @Override
    public void moveTo(Path targetDir) throws IOException {
        log.debug("Moving deposit {} to {}", location, targetDir);
        Files.move(location, targetDir.resolve(location.getFileName()));
        location = targetDir.resolve(location.getFileName());
    }

    @Override
    public int compareTo(@NotNull DataverseIngestDeposit dataverseIngestDeposit) {
        return getCreationTimestamp().compareTo(dataverseIngestDeposit.getCreationTimestamp());
    }
}
