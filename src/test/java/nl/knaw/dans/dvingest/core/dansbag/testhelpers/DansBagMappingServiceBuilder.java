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
package nl.knaw.dans.dvingest.core.dansbag.testhelpers;

import lombok.Data;
import lombok.experimental.Accessors;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingServiceImpl;
import nl.knaw.dans.dvingest.core.dansbag.SupportedLicenses;
import nl.knaw.dans.dvingest.core.dansbag.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.util.MappingLoader;
import org.apache.commons.io.FileUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helper for creating a {@link DansBagMappingService} instance to test, with sensible defaults for most fields. Currently, not all fields can be set. For example, the language mapping files are
 * hardcoded to the default configuration files in the distribution.
 */
@Data
@Accessors(fluent = true, chain = true)
public class DansBagMappingServiceBuilder {
    private boolean isMigration = false;
    private boolean deduplicate = true;
    private Map<String, String> dataSuppliers = new HashMap<>();
    private List<String> skipFields = List.of();
    private DataverseService dataverseService;
    private Pattern fileExclusionPattern = Pattern.compile("a^");
    private Pattern filesForIndividualUploadPattern = Pattern.compile("a^");
    private List<String> embargoExclusions = List.of();
    private String depositorRoleAutoIngest = "swordupdater";
    private String depositorRoleMigration = "contributorplus";

    private DansBagMappingServiceBuilder() {
    }

    public static DansBagMappingServiceBuilder builder() {
        return new DansBagMappingServiceBuilder();
    }

    public DansBagMappingService build() throws Exception {
        var defaultConfigDir = Paths.get("src/main/assembly/dist/cfg");
        var mapper = new DepositToDvDatasetMetadataMapper(
            isMigration,
            deduplicate,
            Set.of("citation", "dansRights", "dansRelationMetadata", "dansArchaeologyMetadata", "dansTemporalSpatial", "dansDataVaultMetadata"),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("iso639-1-to-dv.csv")).keyColumn("ISO639-1").valueColumn("Dataverse-language").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("iso639-2-to-dv.csv")).keyColumn("ISO639-2").valueColumn("Dataverse-language").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-report-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("verwervingswijzen-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-complextype-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-artifact-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-period-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            FileUtils.readLines(defaultConfigDir.resolve("spatial-coverage-country-terms.txt").toFile(), StandardCharsets.UTF_8),
            dataSuppliers,
            skipFields);
        var supportedLicenses = new SupportedLicenses(dataverseService);
        return new DansBagMappingServiceImpl(mapper, dataverseService, supportedLicenses, fileExclusionPattern, filesForIndividualUploadPattern, embargoExclusions, depositorRoleAutoIngest,
            depositorRoleMigration);
    }
}
