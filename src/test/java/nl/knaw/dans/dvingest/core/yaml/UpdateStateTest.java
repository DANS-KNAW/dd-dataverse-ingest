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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UpdateStateTest {
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new DataverseIngestModule());
    }

    @Test
    public void publish_action_is_deserialized_correctly() throws Exception {
        var yaml = """
            updateState:
                publish: major
            """;

        var updateState = mapper.readValue(yaml, UpdateStateRoot.class);
        assertThat(updateState.getUpdateState()).isInstanceOf(PublishAction.class);
        assertThat(((PublishAction) updateState.getUpdateState()).getUpdateType()).isEqualTo(UpdateType.major);
    }

    @Test
    public void publish_action_is_serialized_correctly() throws Exception {
        var updateState = new UpdateStateRoot(new PublishAction(UpdateType.major));
        var yaml = mapper.writeValueAsString(updateState);
        assertThat(yaml.trim()).isEqualTo("""
            ---
            updateState:
              publish: "major"
            """.trim());
    }

    @Test
    public void release_migrated_action_is_deserialized_correctly() throws Exception {
        var yaml = """
            updateState:
                releaseMigrated: 2021-09-01
            """;

        var updateState = mapper.readValue(yaml, UpdateStateRoot.class);
        assertThat(updateState.getUpdateState()).isInstanceOf(ReleaseMigratedAction.class);
        assertThat(((ReleaseMigratedAction) updateState.getUpdateState()).getReleaseDate()).isEqualTo("2021-09-01");
    }

    @Test
    public void release_date_must_follow_yyyy_mmd_dd_pattern() {
        var yaml = """
            updateState:
                releaseMigrated: 2021-09-01T00:00:00Z
            """;

        assertThatThrownBy(() -> mapper.readValue(yaml, UpdateStateRoot.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Release date must be in the format YYYY-MM-DD");
    }

    @Test
    public void release_migrated_action_is_serialized_correctly() throws Exception {
        var updateState = new UpdateStateRoot(new ReleaseMigratedAction("2021-09-01"));
        var yaml = mapper.writeValueAsString(updateState);
        assertThat(yaml.trim()).isEqualTo("""
            ---
            updateState:
              releaseMigrated: "2021-09-01"
            """.trim());
    }

    @Test
    public void both_publish_and_release_migrated_actions_are_not_allowed() {
        var yaml = """
            updateState:
                publish: major
                releaseMigrated: 2021-09-01
            """;

        assertThatThrownBy(() -> mapper.readValue(yaml, UpdateStateRoot.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Exactly one of publish or releaseMigrated must be set");
    }

    @Test
    public void neither_publish_nor_release_migrated_actions_are_not_allowed() {
        var yaml = """
            updateState: {}
            """;

        assertThatThrownBy(() -> mapper.readValue(yaml, UpdateStateRoot.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Exactly one of publish or releaseMigrated must be set");
    }

}
