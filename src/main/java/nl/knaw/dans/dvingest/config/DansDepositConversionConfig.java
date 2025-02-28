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

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Data
public class DansDepositConversionConfig {
    private String fileExclusionPattern;

    private String filesForSeparateUploadPattern;

    private List<String> embargoExclusions = List.of();

    private Map<String, String> dataSuppliers = Map.of();

    private boolean deduplicate;

    @NotNull
    private Path mappingDefsDir;

    @NotNull
    private AssignDepositorRoleConfig assignDepositorRole;

    private List<String> skipFields = List.of();

    @NotNull
    @Valid
    private ValidateDansBagConfig validateDansBag;

    @NotNull
    @Valid
    private DepositorAuthorizationConfigs depositorAuthorization;
}
