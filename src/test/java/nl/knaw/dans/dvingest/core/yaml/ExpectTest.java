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
package nl.knaw.dans.dvingest.core.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import nl.knaw.dans.dvingest.core.yaml.Expect.State;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExpectTest {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    public void existing_state_is_parsed_correctly() throws Exception {
        var yaml = "state: draft";
        var expect = MAPPER.readValue(yaml, Expect.class);

        assertThat(expect.getState()).isEqualTo(State.draft);
    }

    @Test
    public void invalid_state_throw_exception() {
        var yaml = "state: ambiguous";
        assertThatThrownBy(() -> MAPPER.readValue(yaml, Expect.class))
            .isInstanceOf(InvalidFormatException.class)
            .hasMessageContaining("not one of the values accepted for Enum class: [draft, released, absent]");
    }

}
