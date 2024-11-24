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
import nl.knaw.dans.dvingest.core.MappingLoader;
import nl.knaw.dans.dvingest.core.TestDirFixture;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.ingest.core.deposit.BagDirResolver;
import nl.knaw.dans.ingest.core.deposit.BagDirResolverImpl;
import nl.knaw.dans.ingest.core.deposit.DepositFileLister;
import nl.knaw.dans.ingest.core.deposit.DepositFileListerImpl;
import nl.knaw.dans.ingest.core.deposit.DepositReader;
import nl.knaw.dans.ingest.core.deposit.DepositReaderImpl;
import nl.knaw.dans.ingest.core.io.BagDataManager;
import nl.knaw.dans.ingest.core.io.BagDataManagerImpl;
import nl.knaw.dans.ingest.core.io.FileService;
import nl.knaw.dans.ingest.core.io.FileServiceImpl;
import nl.knaw.dans.ingest.core.service.ManifestHelper;
import nl.knaw.dans.ingest.core.service.ManifestHelperImpl;
import nl.knaw.dans.ingest.core.service.XmlReader;
import nl.knaw.dans.ingest.core.service.XmlReaderImpl;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.lib.dataverse.model.dataset.License;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class DansDepositConverterTest extends TestDirFixture {
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);

    private DepositReader depositReader;
    private DansBagMappingService mappingService;
    private final YamlService yamlService = new YamlServiceImpl();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        BagReader bagReader = new BagReader();
        ManifestHelper manifestHelper = new ManifestHelperImpl();
        DepositFileLister depositFileLister = new DepositFileListerImpl();
        BagDataManager bagDataManager = new BagDataManagerImpl(bagReader);
        XmlReader xmlReader = new XmlReaderImpl();
        FileService fileService = new FileServiceImpl();
        BagDirResolver bagDirResolver = new BagDirResolverImpl(fileService);

        depositReader = new DepositReaderImpl(xmlReader, bagDirResolver, fileService, bagDataManager, depositFileLister, manifestHelper);
        var defaultConfigDir = Paths.get("src/main/assembly/dist/cfg");
        var mapper = new DepositToDvDatasetMetadataMapper(
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
            List.of(),
            false);
        var supportedLicenses = new SupportedLicenses(licenses("http://opensource.org/licenses/MIT"));
        mappingService = new DansBagMappingServiceImpl(mapper, dataverseServiceMock, supportedLicenses, Pattern.compile("a^")); // never match

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

    @Test
    public void run_converts_dans_sword_all_mappings_example_to_dataverse_ingest_deposit() throws Exception {
        // Given
        FileUtils.copyDirectoryToDirectory(Paths.get("src/test/resources/unit-test/d0919038-9866-49e8-986a-bcef54ae7566").toFile(), testDir.toFile());
        var depositDir = testDir.resolve("d0919038-9866-49e8-986a-bcef54ae7566");
        var deposit = depositReader.readDeposit(depositDir);
        var authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("John");
        authenticatedUser.setLastName("Doe");
        authenticatedUser.setEmail("jdoe@foo.com");
        Mockito.when(dataverseServiceMock.getUserById(Mockito.anyString())).thenReturn(Optional.of(authenticatedUser));

        // When
        new DansDepositConverter(deposit, mappingService, yamlService).run();

        // Then
        assertThat(deposit.getBagDir().resolve("dataset.yml")).exists();
    }

}
