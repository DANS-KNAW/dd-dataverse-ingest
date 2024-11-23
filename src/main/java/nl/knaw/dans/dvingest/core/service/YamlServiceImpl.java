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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.YamlConfigurationFactory;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.EditMetadataRoot;
import nl.knaw.dans.dvingest.core.yaml.EditPermissionsRoot;
import nl.knaw.dans.dvingest.core.yaml.UpdateState;
import nl.knaw.dans.lib.dataverse.MetadataFieldDeserializer;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;

import javax.validation.Validation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class YamlServiceImpl implements YamlService {
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<Class<?>, YamlConfigurationFactory<?>> yamlConfigurationFactories = new HashMap<>();

    public YamlServiceImpl() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(MetadataField.class, new MetadataFieldDeserializer());
            mapper.setSerializationInclusion(Include.NON_NULL);
            mapper.registerModule(module);
            yamlConfigurationFactories.put(Dataset.class, new YamlConfigurationFactory<>(Dataset.class, factory.getValidator(), mapper, "dw"));
            yamlConfigurationFactories.put(EditFilesRoot.class, new YamlConfigurationFactory<>(EditFilesRoot.class, factory.getValidator(), mapper, "dw"));
            yamlConfigurationFactories.put(EditMetadataRoot.class, new YamlConfigurationFactory<>(EditMetadataRoot.class, factory.getValidator(), mapper, "dw"));
            yamlConfigurationFactories.put(EditPermissionsRoot.class, new YamlConfigurationFactory<>(EditPermissionsRoot.class, factory.getValidator(), mapper, "dw"));
            yamlConfigurationFactories.put(UpdateState.class, new YamlConfigurationFactory<>(UpdateState.class, factory.getValidator(), mapper, "dw"));
        }
        catch (Throwable e) {
            // This ctor is called from a static context, so we log the error to make sure it is not lost
            log.error("Failed to create YamlUtils", e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readYaml(Path yamlFile, Class<T> target) throws IOException, ConfigurationException {
        YamlConfigurationFactory<T> factory = (YamlConfigurationFactory<T>) yamlConfigurationFactories.get(target);
        if (factory == null) {
            throw new IllegalArgumentException("No factory found for class: " + target.getName());
        }
        return factory.build(yamlFile.toFile());
    }

    @Override
    public void writeYaml(Object object, Path yamlFile) throws IOException {
        mapper.writeValue(yamlFile.toFile(), object);
    }
}
