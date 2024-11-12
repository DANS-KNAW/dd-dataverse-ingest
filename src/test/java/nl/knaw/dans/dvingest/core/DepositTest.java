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

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

public class DepositTest extends TestDirFixture {

    @Test
    public void ctor_should_throw_IllegalStateException_when_deposit_properties_file_not_found() throws Exception {
        // Given
        var depositDir = testDir.resolve(UUID.randomUUID().toString());
        Files.createDirectories(depositDir);

        // When
        // Then
        assertThatIllegalStateException().isThrownBy(() -> new Deposit(depositDir))
            .withMessage("Error loading deposit properties from " + depositDir.resolve("deposit.properties"));

    }

    @Test
    public void getId_should_return_uuid_from_dir_name() throws Exception {
        // Given
        var uuid = UUID.randomUUID();
        var depositDir = testDir.resolve(uuid.toString());
        Files.createDirectories(depositDir);
        Files.createFile(depositDir.resolve("deposit.properties"));

        // When
        var deposit = new Deposit(depositDir);

        // Then
        assertThat(deposit.getId()).isEqualTo(uuid);
    }

    @Test
    public void deposits_should_be_ordered_by_sequence_number() throws Exception {
        // Given
        var id1 = UUID.randomUUID().toString();
        var id2 = UUID.randomUUID().toString();
        var id3 = UUID.randomUUID().toString();

        var dir1 = testDir.resolve(id1);
        Files.createDirectories(dir1);
        var props1 = new Properties();
        props1.setProperty("sequence-number", "1");
        props1.store(Files.newBufferedWriter(dir1.resolve("deposit.properties")), "");
        var dir2 = testDir.resolve(id2);
        Files.createDirectories(dir2);
        var props2 = new Properties();
        props2.setProperty("sequence-number", "2");
        props2.store(Files.newBufferedWriter(dir2.resolve("deposit.properties")), "");
        var dir3 = testDir.resolve(id3);
        Files.createDirectories(dir3);
        var props3 = new Properties();
        props3.setProperty("sequence-number", "3");
        props3.store(Files.newBufferedWriter(dir3.resolve("deposit.properties")), "");

        var deposit1 = new Deposit(dir1);
        var deposit2 = new Deposit(dir2);
        var deposit3 = new Deposit(dir3);

        // When
        var deposits = new TreeSet<>();
        deposits.add(deposit2);
        deposits.add(deposit1);
        deposits.add(deposit3);

        // Then
        assertThat(deposits).containsExactly(deposit1, deposit2, deposit3);
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
        props1.setProperty("creation-timestamp", "2023-01-01T10:00:00Z");
        props1.store(Files.newBufferedWriter(dir1.resolve("deposit.properties")), "");

        var dir2 = testDir.resolve(id2);
        Files.createDirectories(dir2);
        var props2 = new Properties();
        props2.setProperty("creation-timestamp", "2023-01-02T10:00:00Z");
        props2.store(Files.newBufferedWriter(dir2.resolve("deposit.properties")), "");

        var dir3 = testDir.resolve(id3);
        Files.createDirectories(dir3);
        var props3 = new Properties();
        props3.setProperty("creation-timestamp", "2023-01-03T10:00:00Z");
        props3.store(Files.newBufferedWriter(dir3.resolve("deposit.properties")), "");

        var deposit1 = new Deposit(dir1);
        var deposit2 = new Deposit(dir2);
        var deposit3 = new Deposit(dir3);

        // When
        var deposits = new TreeSet<>();
        deposits.add(deposit2);
        deposits.add(deposit1);
        deposits.add(deposit3);

        // Then
        assertThat(deposits).containsExactly(deposit1, deposit2, deposit3);
    }

    @Test
    public void ordering_should_fail_if_both_sequence_number_and_creation_timestamp_are_missing_in_one_of_the_deposits() throws Exception {
        // Given
        var id1 = UUID.randomUUID().toString();
        var id2 = UUID.randomUUID().toString();
        var id3 = UUID.randomUUID().toString();

        var dir1 = testDir.resolve(id1);
        Files.createDirectories(dir1);
        var props1 = new Properties();
        props1.store(Files.newBufferedWriter(dir1.resolve("deposit.properties")), "");

        var dir2 = testDir.resolve(id2);
        Files.createDirectories(dir2);
        var props2 = new Properties();
        props2.setProperty("sequence-number", "2");
        props2.store(Files.newBufferedWriter(dir2.resolve("deposit.properties")), "");

        var deposit1 = new Deposit(dir1);
        var deposit2 = new Deposit(dir2);

        // Then
        assertThatIllegalStateException().isThrownBy(() -> {
                // When
                var deposits = new TreeSet<>();
                deposits.add(deposit2);
                deposits.add(deposit1);
            })
            .withMessage("Deposit " + deposit1.getId() + " should contain either a sequence number or a creation timestamp");
    }

}
