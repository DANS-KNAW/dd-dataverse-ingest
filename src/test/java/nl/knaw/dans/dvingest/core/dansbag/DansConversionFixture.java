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
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.dansbag.deposit.BagDirResolver;
import nl.knaw.dans.dvingest.core.dansbag.deposit.BagDirResolverImpl;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositFileLister;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositFileListerImpl;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositReader;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositReaderImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.BagDataManager;
import nl.knaw.dans.dvingest.core.dansbag.service.BagDataManagerImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelper;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelperImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.XmlReader;
import nl.knaw.dans.dvingest.core.dansbag.service.XmlReaderImpl;
import nl.knaw.dans.dvingest.core.dansbag.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.lib.dataverse.model.dataset.License;
import nl.knaw.dans.lib.util.MappingLoader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class DansConversionFixture extends TestDirFixture {
    protected final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);
    protected DepositReader depositReader;
    protected DansBagMappingService mappingService;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        BagReader bagReader = new BagReader();
        ManifestHelper manifestHelper = new ManifestHelperImpl();
        DepositFileLister depositFileLister = new DepositFileListerImpl();
        BagDataManager bagDataManager = new BagDataManagerImpl(bagReader);
        XmlReader xmlReader = new XmlReaderImpl();
        BagDirResolver bagDirResolver = new BagDirResolverImpl();

        depositReader = new DepositReaderImpl(xmlReader, bagDirResolver, bagDataManager, depositFileLister, manifestHelper);
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
        mappingService = new DansBagMappingServiceImpl(mapper, dataverseServiceMock, supportedLicenses, Pattern.compile("a^"), List.of()); // never match

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
}
