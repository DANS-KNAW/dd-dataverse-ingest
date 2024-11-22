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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class IngestFlowConfigReader {
    public static void readIngestFlowConfiguration(IngestFlowConfig config) throws IOException, URISyntaxException {
        config.setIso1ToDataverseLanguage(getMap(config, "iso639-1-to-dv.csv", "ISO639-1", "Dataverse-language"));
        config.setIso2ToDataverseLanguage(getMap(config, "iso639-2-to-dv.csv", "ISO639-2", "Dataverse-language"));
        config.setSpatialCoverageCountryTerms(FileUtils.readLines(config.getMappingDefsDir().resolve("spatial-coverage-country-terms.txt").toFile(), StandardCharsets.UTF_8));
        config.setAbrReportCodeToTerm(getMap(config, "rapporten-code-to-term.csv", "code", "subject"));
        config.setAbrAcquisitionMethodCodeToTerm(getMap(config, "verwervingswijzen-code-to-term.csv", "code", "subject"));
        config.setAbrComplexTypeCodeToTerm(getMap(config, "complextypen-code-to-term.csv", "code", "subject"));
        config.setAbrArtifactCodeToTerm(getMap(config, "artefacten-code-to-term.csv", "code", "subject"));
        config.setAbrPeriodCodeToTerm(getMap(config, "periodes-code-to-term.csv", "code", "subject"));
    }

    private static Map<String, String> loadCsvToMap(Path path, String keyColumn, String valueColumn) throws IOException {
        try (var parser = CSVParser.parse(path, StandardCharsets.UTF_8, CSVFormat.RFC4180.withFirstRecordAsHeader())) {
            var result = new HashMap<String, String>();

            for (var record : parser) {
                result.put(record.get(keyColumn), record.get(valueColumn));
            }

            return result;
        }
    }

    private static Map<String, String> getMap(IngestFlowConfig ingestFlowConfig, String mappingCsv, String keyColumn, String valueColumn) throws IOException {
        return loadCsvToMap(ingestFlowConfig.getMappingDefsDir().resolve(mappingCsv),
            keyColumn,
            valueColumn);
    }

}
