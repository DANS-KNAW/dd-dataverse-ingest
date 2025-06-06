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

import io.dropwizard.core.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.knaw.dans.lib.util.DataverseClientFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class DdDataverseIngestConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataverseClientFactory dataverse;

    @Valid
    @NotNull
    private IngestConfig ingest;

    @Valid
    // NOT @NotNull, because conversion can be disabled that way
    private DansDepositConversionConfig dansDepositConversion;

    @Valid
    @NotNull
    private DependenciesReadyCheckConfig dependenciesReadyCheck;

    @Valid
    public YamlServiceConfig yamlServiceConfig = new YamlServiceConfig();
}
