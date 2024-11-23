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

import nl.knaw.dans.dvingest.core.service.YamlService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

public class DepositTest extends TestDirFixture {
    private final YamlService yamlServiceMock = Mockito.mock(YamlService.class);

    @Test
    public void ctor_should_throw_IllegalStateException_when_deposit_properties_file_not_found() throws Exception {
        // Given
        var depositDir = testDir.resolve(UUID.randomUUID().toString());
        Files.createDirectories(depositDir);

        // When
        // Then
        assertThatIllegalStateException().isThrownBy(() -> new Deposit(depositDir, yamlServiceMock))
            .withMessage("Error loading deposit properties from " + depositDir.resolve("deposit.properties"));

    }

    @Test
    public void getId_should_return_uuid_from_dir_name() throws Exception {
        // Given
        var uuid = UUID.randomUUID();
        var depositDir = testDir.resolve(uuid.toString());
        Files.createDirectories(depositDir);
        var props = new Properties();
        props.setProperty("creation.timestamp", "2023-01-01T10:00:00Z");
        props.store(Files.newBufferedWriter(depositDir.resolve("deposit.properties")), "");

        // When
        var deposit = new Deposit(depositDir, yamlServiceMock);

        // Then
        assertThat(deposit.getId()).isEqualTo(uuid);
    }


    @Test
    public void deposits_should_be_ordered_by_creation_timestamp() throws Exception {
        // Given
        var id1 = UUID.randomUUID().toString();
        var id2 = UUID.randomUUID().toString();
        var id3 = UUID.randomUUID().toString();

        var dir1 = testDir.resolve(id1);
        Files.createDirectories(dir1);
        var props1 = new Properties();
        props1.setProperty("creation.timestamp", "2023-01-01T10:00:00Z");
        props1.store(Files.newBufferedWriter(dir1.resolve("deposit.properties")), "");

        var dir2 = testDir.resolve(id2);
        Files.createDirectories(dir2);
        var props2 = new Properties();
        props2.setProperty("creation.timestamp", "2023-01-02T10:00:00Z");
        props2.store(Files.newBufferedWriter(dir2.resolve("deposit.properties")), "");

        var dir3 = testDir.resolve(id3);
        Files.createDirectories(dir3);
        var props3 = new Properties();
        props3.setProperty("creation.timestamp", "2023-01-03T10:00:00Z");
        props3.store(Files.newBufferedWriter(dir3.resolve("deposit.properties")), "");

        var deposit1 = new Deposit(dir1, yamlServiceMock);
        var deposit2 = new Deposit(dir2, yamlServiceMock);
        var deposit3 = new Deposit(dir3, yamlServiceMock);

        // When
        var deposits = new TreeSet<>();
        deposits.add(deposit2);
        deposits.add(deposit1);
        deposits.add(deposit3);

        // Then
        assertThat(deposits).containsExactly(deposit1, deposit2, deposit3);
    }
}
