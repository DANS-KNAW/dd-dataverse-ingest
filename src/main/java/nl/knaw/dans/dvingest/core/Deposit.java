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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Deposit implements Comparable<Deposit> {
    private final OffsetDateTime creationTimestamp;

    private final UUID id;

    private Path location;

    private final Properties depositProperties;
    private final String updatesDataset;

    public Deposit(@NonNull Path location) {
        this.location = location;
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

    public List<DepositBag> getBags() throws IOException {
        try (var files = Files.list(location)) {
            return files
                .filter(Files::isDirectory)
                .map(DepositBag::new)
                .sorted()
                .toList();
        }
    }

    public void moveTo(Path targetDir) throws IOException {
        log.debug("Moving deposit {} to {}", location, targetDir);
        Files.move(location, targetDir.resolve(location.getFileName()));
        location = targetDir.resolve(location.getFileName());
    }

    @Override
    public int compareTo(@NotNull Deposit deposit) {
        return getCreationTimestamp().compareTo(deposit.getCreationTimestamp());
    }
}
