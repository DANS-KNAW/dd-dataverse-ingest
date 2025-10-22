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
package nl.knaw.dans.dvingest.core.service;

import io.dropwizard.configuration.ConfigurationParsingException;
import nl.knaw.dans.dvingest.config.YamlServiceConfig;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class YamlServiceImplTest {
    static private final Path yamlFile = Path.of("src/test/resources/test-deposits/072625c6-c2a8-43a6-9f35-f49b2db9435c/1/dataset.yml");

    @Test
    public void tooBigYaml() {
        var yamlServiceConfig = new YamlServiceConfig();
        yamlServiceConfig.getLoaderOptions().setCodePointLimit(20);

        var customYamlService = new YamlServiceImpl(yamlServiceConfig);
        assertThatThrownBy(() ->
            customYamlService.readYaml(yamlFile, Dataset.class)
        ).isInstanceOf(ConfigurationParsingException.class)
            .hasMessageStartingWith(yamlFile + " has an error:")
            .hasMessageContaining("The incoming YAML document exceeds the limit: 20 code points");
    }

    @Test
    public void canReadYaml() {

        var customYamlService = new YamlServiceImpl(new YamlServiceConfig());
        assertDoesNotThrow(() ->
            customYamlService.readYaml(yamlFile, Dataset.class)
        );
    }
}
