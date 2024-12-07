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
package nl.knaw.dans.dvingest.core.dansbag;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Represents the properties of a DANS bag deposit.
 */
@Slf4j
public class DansDepositProperties {
    private final Properties properties;
    private final String depositId;

    public DansDepositProperties(Path depositPropertiesFile) throws IOException {
        // Load the properties from the file
        properties = new Properties();
        properties.load(depositPropertiesFile.toUri().toURL().openStream());
        depositId = depositPropertiesFile.getParent().getFileName().toString();
    }

    public String getDepositorUserId() {
        return properties.getProperty("depositor.userId");
    }

    public String getSwordToken() {
        return properties.getProperty("dataverse.sword-token");
    }

    public String getBagId() {
        return properties.getProperty("dataverse.bag-id");
    }

    public String getDepositId() {
        return depositId;
    }
}
