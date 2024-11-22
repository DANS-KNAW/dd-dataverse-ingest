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
package nl.knaw.dans.ingest.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import nl.knaw.dans.lib.util.ExecutorServiceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Data
public class IngestFlowConfig {

    private List<String> skipFields;

    @NotNull
    @Valid
    @JsonProperty("import")
    private IngestAreaConfig importConfig;

    @NotNull
    @Valid
    private IngestAreaConfig migration;

    @NotNull
    @Valid
    private IngestAreaConfig autoIngest;

    @NotNull
    private Path zipWrappingTempDir;

    @NotNull
    private Path mappingDefsDir;

    @NotNull
    private String fileExclusionPattern;

    @NotNull
    private String depositorRole;

    @Valid
    @NotNull
    private DatasetAuthorizationConfig authorization;

    @Valid
    private boolean deduplicate;

    @NotNull
    @Valid
    private ExecutorServiceFactory taskQueue;

    @NotNull
    private Map<String, String> iso1ToDataverseLanguage;

    @NotNull
    private Map<String, String> iso2ToDataverseLanguage;

    @NotNull
    private Map<String, String> abrReportCodeToTerm;

    @NotNull
    private Map<String, String> abrAcquisitionMethodCodeToTerm;

    @NotNull
    private Map<String, String> abrComplexTypeCodeToTerm;

    @NotNull
    private Map<String, String> abrArtifactCodeToTerm;

    @NotNull
    private Map<String, String> abrPeriodCodeToTerm;

    @NotNull
    private List<String> spatialCoverageCountryTerms;

    @NotNull
    private Map<String, String> dataSuppliers;

    @NotNull
    private String vaultMetadataKey;

    private boolean deleteDraftOnFailure;
}
