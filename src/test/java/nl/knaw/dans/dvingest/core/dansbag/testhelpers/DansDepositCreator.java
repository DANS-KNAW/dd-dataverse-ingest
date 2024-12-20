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
package nl.knaw.dans.dvingest.core.dansbag.testhelpers;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Data
@Accessors(fluent = true, chain = true)
public class DansDepositCreator {
    private final Properties properties = new Properties();

    private Path copyBagFrom;
    private Path depositDir;

    private DansDepositCreator() {
    }

    public static DansDepositCreator creator() {
        return new DansDepositCreator();
    }

    public DansDepositCreator withProperty(String key, String value) {
        properties.setProperty(key, value);
        return this;
    }

    public void create() throws Exception {
        Files.createDirectory(depositDir);
        properties.setProperty("state.label", "SUBMITTED");
        properties.setProperty("state.description", "Deposit is submitted");
        properties.setProperty("creation.timestamp", DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneId.of("UTC"))
            .format(Instant.now()));
        try (var out = Files.newBufferedWriter(depositDir.resolve("deposit.properties"))) {
            properties.store(out, null);
        }
        FileUtils.copyDirectoryToDirectory(copyBagFrom.toFile(), depositDir.toFile());
    }

}
