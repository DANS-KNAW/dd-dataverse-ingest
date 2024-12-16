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
package nl.knaw.dans.dvingest.core.dansbag;

import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.dvingest.core.TestDirFixture;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDepositReader;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDepositReaderImpl;
import nl.knaw.dans.dvingest.core.dansbag.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.dvingest.core.dansbag.xml.XmlReader;
import nl.knaw.dans.dvingest.core.dansbag.xml.XmlReaderImpl;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.dataverse.model.dataset.CompoundMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledSingleValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.License;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.util.MappingLoader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class DansConversionFixture extends TestDirFixture {
    protected final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);
    protected DansBagDepositReader dansBagDepositReader;
    protected DansBagMappingService mappingService;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        BagReader bagReader = new BagReader();
        XmlReader xmlReader = new XmlReaderImpl();

        dansBagDepositReader = new DansBagDepositReaderImpl(xmlReader, bagReader);
        var defaultConfigDir = Paths.get("src/main/assembly/dist/cfg");
        var mapper = new DepositToDvDatasetMetadataMapper(
            false,
            false, // Always false ?
            Set.of("citation", "dansRights", "dansRelationMetadata", "dansArchaeologyMetadata", "dansTemporalSpatial", "dansDataVaultMetadata"),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("iso639-1-to-dv.csv")).keyColumn("ISO639-1").valueColumn("Dataverse-language").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("iso639-2-to-dv.csv")).keyColumn("ISO639-2").valueColumn("Dataverse-language").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-report-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("verwervingswijzen-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-complextype-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-artifact-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            MappingLoader.builder().csvFile(defaultConfigDir.resolve("abr-period-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
            FileUtils.readLines(defaultConfigDir.resolve("spatial-coverage-country-terms.txt").toFile(), StandardCharsets.UTF_8),
            Collections.emptyMap(),
            List.of());
        var supportedLicenses = new SupportedLicenses(licenses("http://opensource.org/licenses/MIT"));
        mappingService = new DansBagMappingServiceImpl(mapper, dataverseServiceMock, supportedLicenses, Pattern.compile("a^"), List.of(), "swordupdater", "contributorplus"); // never match

        Mockito.reset(dataverseServiceMock);
    }

    private Map<URI, License> licenses(String... uri) {
        var licenses = new HashMap<URI, License>();
        for (String s : uri) {
            var license = new License();
            license.setUri(URI.create(s));
            licenses.put(license.getUri(), license);
        }
        return licenses;
    }

    protected Path createValidDeposit(String validExample, String depositDir) throws Exception {
        var deposit = testDir.resolve(depositDir);
        Files.createDirectory(deposit);
        // Create deposit.properties
        var props = new Properties();
        props.setProperty("state.label", "SUBMITTED");
        props.setProperty("state.description", "Deposit is submitted");
        props.setProperty("deposit.origin", "SWORD");
        props.setProperty("creation.timestamp", DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneId.of("UTC"))
            .format(Instant.now()));
        props.setProperty("depositor.userId", "jdoe");
        try (var out = Files.newBufferedWriter(deposit.resolve("deposit.properties"))) {
            props.store(out, null);
        }
        FileUtils.copyDirectoryToDirectory(Paths.get("target/test/example-bags/valid").resolve(validExample).toFile(), deposit.toFile());
        return deposit;
    }

    protected void assertPrimitiveSinglevalueFieldContainsValue(List<MetadataField> fields, String typeName, String value) {
        assertThat(fields)
            .filteredOn(f -> typeName.equals(f.getTypeName()))
            .map(f -> (PrimitiveSingleValueField) f).extracting(PrimitiveSingleValueField::getValue)
            .containsExactly(value);
    }

    protected void assertPrimitiveMultiValueFieldContainsValues(List<MetadataField> fields, String typeName, String... values) {
        assertThat(fields)
            .filteredOn(f -> typeName.equals(f.getTypeName()))
            .map(f -> (PrimitiveMultiValueField) f).extracting(PrimitiveMultiValueField::getValue)
            .containsExactly(List.of(values));
    }

    protected void assertControlledSingleValueFieldContainsValue(List<MetadataField> fields, String typeName, String value) {
        assertThat(fields)
            .filteredOn(f -> typeName.equals(f.getTypeName()))
            .map(f -> (ControlledSingleValueField) f).extracting(ControlledSingleValueField::getValue)
            .containsExactly(value);
    }

    protected void assertControlledMultiValueFieldContainsValues(List<MetadataField> fields, String typeName, String... values) {
        assertThat(fields)
            .filteredOn(f -> typeName.equals(f.getTypeName()))
            .map(f -> (ControlledMultiValueField) f).extracting(ControlledMultiValueField::getValue)
            .containsExactly(List.of(values));
    }

    @SafeVarargs
    protected final void assertCompoundMultiValueFieldContainsValues(List<MetadataField> fields, String typeName, Map<String, String>... expectedValues) {
        var filteredFields = fields.stream()
            .filter(f -> typeName.equals(f.getTypeName()))
            .map(f -> (CompoundMultiValueField) f)
            .toList();

        assertThat(filteredFields).as("Field not found: " + typeName).isNotEmpty();
        assertThat(filteredFields).as("Field appearing more than once: " + typeName).hasSize(1);

        var actualValues = filteredFields.get(0).getValue();
        assertThat(actualValues).as("Different number of actual and expected values: " + actualValues.size() + " vs " + expectedValues.length).hasSize(expectedValues.length);

        List<Map<String, String>> actualValuesList = new ArrayList<>();
        for (var actualValue : actualValues) {
            var actualValueMap = actualValue.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            actualValuesList.add(actualValueMap);
        }

        assertThat(actualValuesList).containsExactlyInAnyOrderElementsOf(List.of(expectedValues));
    }
}
