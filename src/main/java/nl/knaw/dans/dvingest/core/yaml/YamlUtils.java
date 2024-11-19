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
//package nl.knaw.dans.dvingest.core.yaml;
//
//import io.dropwizard.configuration.ConfigurationException;
//import io.dropwizard.configuration.YamlConfigurationFactory;
//import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
//
//import javax.validation.Validation;
//import java.io.IOException;
//import java.nio.file.Path;
//
//public class YamlUtils {
//    private final YamlConfigurationFactory<Dataset> datasetYamlConfigurationFactory;
//    private final YamlConfigurationFactory<EditInstructions> editInstructionsYamlConfigurationFactory;
//
//
//
//    public YamlUtils() {
//        try (var factory = Validation.buildDefaultValidatorFactory()) {
//            datasetYamlConfigurationFactory = new YamlConfigurationFactory<>(Dataset.class, factory.getValidator(), null, "dw");
//            editInstructionsYamlConfigurationFactory = new YamlConfigurationFactory<>(EditInstructions.class, factory.getValidator(), null, "dw");
//        }
//    }
//
//    public <T> T readYaml(Path yamlFile, Class<T> target) throws IOException, ConfigurationException {
//        if (target == Dataset.class) {
//            return datasetYamlConfigurationFactory.build(yamlFile.toFile());
//        }
//        else if (target == EditInstructions.class) {
//            return editInstructionsYamlConfigurationFactory.build(yamlFile.toFile());
//        }
//        else {
//            throw new IllegalArgumentException("Unsupported target class: " + target.getName());
//        }
//    }
//}
