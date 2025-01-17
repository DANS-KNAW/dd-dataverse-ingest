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
package nl.knaw.dans.dvingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.assertj.core.api.AbstractAssert;

// TODO: move to future dans-test-utils lib
public class YamlBeanAssert<T> extends AbstractAssert<YamlBeanAssert<T>, T> {
    private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public YamlBeanAssert(T actual) {
        super(actual, YamlBeanAssert.class);
    }

    public static <T> YamlBeanAssert<T> assertThat(T actual) {
        return new YamlBeanAssert<T>(actual);
    }

    public YamlBeanAssert<T> isEqualTo(String yaml) {
        try {
            var object = mapper.readValue(yaml, actual.getClass());
            if (!actual.equals(object)) {
                failWithMessage("Expected:\n<%s>\nbut was:\n<%s>", yaml, mapper.writeValueAsString(actual));
            }
        }
        catch (JsonProcessingException e) {
            failWithMessage("Failed to parse YAML: " + e.getMessage());
        }
        return this;
    }
}
