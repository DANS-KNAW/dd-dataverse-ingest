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
package nl.knaw.dans.dvingest.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Data
public class IngestConfig {

    @Valid
    @JsonProperty("import")
    private IngestAreaConfig importConfig;

    // If null, use the standard temp directory
    private Path tempDir;

    private int maxNumberOfFilesPerUpload = 1000;

    private Map<String, String> metadataKeys = new HashMap<>();

    @Valid
    @NotNull
    private WaitForReleasedStateConfig waitForReleasedState;
}
