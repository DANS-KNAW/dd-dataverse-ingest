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

import io.dropwizard.core.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.knaw.dans.lib.util.DataverseClientFactory;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class DdIngestFlowConfiguration extends Configuration {

    @NotNull
    private IngestFlowConfig ingestFlow;

    @NotNull
    private DataverseClientFactory dataverse;

    @NotNull
    private DataverseExtra dataverseExtra;

    @NotNull
    private ValidateDansBagConfig validateDansBag;

    public IngestFlowConfig getIngestFlow() {
        applyDefaults(ingestFlow.getAutoIngest());
        applyDefaults(ingestFlow.getImportConfig());
        applyDefaults(ingestFlow.getMigration());
        return ingestFlow;
    }

    private void applyDefaults(IngestAreaConfig ingestAreaConfig) {
        if (StringUtils.isBlank(ingestAreaConfig.getDepositorRole())) {
            ingestAreaConfig.setDepositorRole(ingestFlow.getDepositorRole());
        }
        var defaultAuthorization = ingestFlow.getAuthorization();
        if (ingestAreaConfig.getAuthorization() == null) {
            ingestAreaConfig.setAuthorization(defaultAuthorization);
        }
        else {
            var authorization = ingestAreaConfig.getAuthorization();
            if (StringUtils.isBlank(authorization.getDatasetPublisher())) {
                authorization.setDatasetPublisher(defaultAuthorization.getDatasetPublisher());
            }
            if (StringUtils.isBlank(authorization.getDatasetUpdater())) {
                authorization.setDatasetUpdater(defaultAuthorization.getDatasetUpdater());
            }
        }
        if (StringUtils.isBlank(ingestAreaConfig.getApiKey())) {
            ingestAreaConfig.setApiKey(dataverse.getApiKey());
        }
    }
}
