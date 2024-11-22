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
package nl.knaw.dans.ingest.core.service.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlockSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DepositToDvDatasetMetadataMapperFactory {
    @lombok.NonNull
    private final Map<String, String> iso1ToDataverseLanguage;
    @lombok.NonNull
    private final Map<String, String> iso2ToDataverseLanguage;

    @lombok.NonNull
    private final Map<String, String> abrReportCodeToTerm;
    @lombok.NonNull
    private final Map<String, String> abrAcquisitionMethodCodeToTerm;
    @lombok.NonNull
    private final Map<String, String> abrComplexCodeToTerm;
    @lombok.NonNull
    private final Map<String, String> abrArtifactCodeToTerm;
    @lombok.NonNull
    private final Map<String, String> abrPeriodCodeToTerm;

    @lombok.NonNull
    private final List<String> spatialCoverageCountryTerms;

    @lombok.NonNull
    private final Map<String, String> dataSuppliers;

    @lombok.NonNull
    private final List<String> skipFields;

    private final DataverseClient dataverseClient;

    public DepositToDvDatasetMetadataMapper createMapper(boolean deduplicate, boolean isMigration) {
        return new DepositToDvDatasetMetadataMapper(
            deduplicate,
            getActiveMetadataBlocks(),
            iso1ToDataverseLanguage,
            iso2ToDataverseLanguage,
            abrReportCodeToTerm,
            abrAcquisitionMethodCodeToTerm,
            abrComplexCodeToTerm,
            abrArtifactCodeToTerm,
            abrPeriodCodeToTerm,
            spatialCoverageCountryTerms,
            dataSuppliers,
            skipFields,
            isMigration
        );
    }

    Set<String> getActiveMetadataBlocks() {
        try {
            var result = dataverseClient.dataverse("root").listMetadataBlocks();
            return result.getData().stream().map(MetadataBlockSummary::getName).collect(Collectors.toSet());
        }
        catch (IOException | DataverseException e) {
            log.error("Unable to fetch active metadata blocks", e);
            throw new RuntimeException(e);
        }
    }
}
