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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the properties of a DANS bag deposit.
 */
@Slf4j
public class DansDepositProperties {
    private final PropertiesConfiguration properties;
    @Getter
    private final String depositId;

    public DansDepositProperties(Path depositPropertiesFile) throws IOException {
        try {
            properties = new PropertiesConfiguration();
            properties.read(Files.newBufferedReader(depositPropertiesFile));
            depositId = properties.getString("depositId");
        }
        catch (ConfigurationException e) {
            log.error("Error reading deposit properties file", e);
            throw new RuntimeException("Error reading deposit properties file", e);
        }
    }

    public DansDepositProperties(PropertiesConfiguration properties) {
        this.properties = properties;
        this.depositId = properties.getString("depositId");
    }

    public String getSwordToken() {
        return properties.getString("dataverse.sword-token");
    }

    public boolean leaveDraft() {
        return properties.getBoolean("dans-deposit.leave-draft", false);
    }

}
