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

import nl.knaw.dans.dvingest.config.YamlServiceConfig;
import nl.knaw.dans.dvingest.core.dansbag.testhelpers.DansBagMappingServiceBuilder;
import nl.knaw.dans.dvingest.core.dansbag.testhelpers.DansDepositCreator;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.dvingest.core.yaml.EditFilesRoot;
import nl.knaw.dans.dvingest.core.yaml.EditPermissionsRoot;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ABR_ARTIFACT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ABR_COMPLEX;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ABR_PERIOD;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ABR_RAPPORT_NUMMER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ABR_RAPPORT_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ABR_VERWERVINGSWIJZE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ALTERNATIVE_TITLE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ARCHIS_NUMBER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ARCHIS_NUMBER_ID;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ARCHIS_NUMBER_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ARCHIS_ZAAK_ID;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUDIENCE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_AFFILIATION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_IDENTIFIER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_IDENTIFIER_SCHEME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.BAG_ID;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.COLLECTION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DANS_OTHER_ID;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATASET_CONTACT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATASET_CONTACT_EMAIL;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATASET_CONTACT_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATAVERSE_PID;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATAVERSE_PID_VERSION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATA_SOURCES;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATA_SUPPLIER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATE_OF_COLLECTION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATE_OF_COLLECTION_END;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATE_OF_COLLECTION_START;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DESCRIPTION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DESCRIPTION_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DISTRIBUTION_DATE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DISTRIBUTOR;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DISTRIBUTOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.GRANT_NUMBER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.GRANT_NUMBER_AGENCY;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.GRANT_NUMBER_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD_VOCABULARY;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD_VOCABULARY_URI;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.LANGUAGE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.LANGUAGE_OF_METADATA;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.NBN;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.OTHER_ID;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.OTHER_ID_AGENCY;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.OTHER_ID_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.PERSONAL_DATA_PRESENT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.PRODUCTION_DATE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.RELATION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.RELATION_TEXT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.RELATION_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.RELATION_URI;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.RIGHTS_HOLDER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SERIES;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SERIES_INFORMATION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_BOX;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_BOX_EAST;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_BOX_NORTH;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_BOX_SCHEME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_BOX_SOUTH;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_BOX_WEST;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_COVERAGE_CONTROLLED;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_COVERAGE_UNCONTROLLED;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_POINT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_POINT_SCHEME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_POINT_X;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SPATIAL_POINT_Y;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SUBJECT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SWORD_TOKEN;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.TEMPORAL_COVERAGE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.TITLE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link DansDepositConverter}. It uses the valid examples from the dd-dans-sword2-examples project.
 */
public class DansDepositConverterTest extends DansConversionFixture {

    private final YamlService yamlService = new YamlServiceImpl(new YamlServiceConfig());

    @Test
    public void run_converts_dans_sword_all_mappings_example_to_dataverse_ingest_deposit() throws Exception {
        /*
         * Given
         */
        var swordToken = "sword:64c59184-7667-4ea0-b4fd-09f421ecb3cf";
        var depositDir = testDir.resolve("00000000-0000-0000-0000-000000000001");
        DansDepositCreator.creator()
            .copyBagFrom(Paths.get("target/test/example-bags/valid/all-mappings"))
            .depositDir(depositDir)
            .withProperty("dataverse.sword-token", swordToken)
            .withProperty("deposit.origin", "SWORD")
            .withProperty("depositor.userId", "jdoe")
            .create();
        var authenticatedUser = authenticatedUser("John", "Doe", "jdoe@foo.com", "John Doe");
        Mockito.when(dataverseServiceMock.getUserById(Mockito.anyString())).thenReturn(Optional.of(authenticatedUser));
        Mockito.when(dataverseServiceMock.getSupportedLicenses()).thenReturn(licenses("http://opensource.org/licenses/MIT"));
        var deposit = dansBagDepositReader.readDeposit(depositDir);
        var mappingService = DansBagMappingServiceBuilder.builder()
            .dataSuppliers(Map.of("jdoe", "Supplier Joe"))
            .dataverseService(dataverseServiceMock).build();

        /*
         * When
         */
        new DansDepositConverter(deposit, null, null, mappingService, yamlService).run();

        /*
         * Then
         */
        assertThat(deposit.getBagDir().resolve("dataset.yml")).exists();
        var datasetYml = yamlService.readYaml(deposit.getBagDir().resolve("dataset.yml"), Dataset.class);

        // Citation block
        var citationBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("citation").getFields();
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, TITLE, "A bag containing examples for each mapping rule");
        assertPrimitiveMultiValueFieldContainsValues(citationBlockFields, ALTERNATIVE_TITLE, "DCTERMS title 1");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, OTHER_ID,
            Map.of(OTHER_ID_AGENCY, "", OTHER_ID_VALUE, "DCTERMS_ID001"),
            Map.of(OTHER_ID_AGENCY, "", OTHER_ID_VALUE, "DC_ID002"),
            Map.of(OTHER_ID_AGENCY, "", OTHER_ID_VALUE, "DCTERMS_ID003"),
            Map.of(OTHER_ID_AGENCY, "TESTPREFIX", OTHER_ID_VALUE, "1234"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, AUTHOR,
            Map.of(AUTHOR_NAME, "I Lastname",
                AUTHOR_AFFILIATION, "Example Org",
                AUTHOR_IDENTIFIER_SCHEME, "ORCID",
                AUTHOR_IDENTIFIER, "0000-0001-9183-9538"),
            Map.of(AUTHOR_NAME, "Creator Organization"),
            Map.of(AUTHOR_NAME, "Unformatted Creator"),
            Map.of(AUTHOR_NAME, "Another Unformatted Creator"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DATASET_CONTACT,
            Map.of(DATASET_CONTACT_NAME, "John Doe",
                DATASET_CONTACT_EMAIL, "jdoe@foo.com"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DESCRIPTION,
            Map.of(DESCRIPTION_VALUE, "<p>This bags contains one or more examples of each mapping rule.</p>"),
            Map.of(DESCRIPTION_VALUE, "<p>A second description</p>"),
            Map.of(DESCRIPTION_VALUE, "<p>DC title 2</p>"),
            Map.of(DESCRIPTION_VALUE, "<p>DCTERMS alt title 1</p>"),
            Map.of(DESCRIPTION_VALUE, "<p>DCTERMS alt title 2</p>"),
            Map.of(DESCRIPTION_VALUE, "Date: some date"),
            Map.of(DESCRIPTION_VALUE, "Date: some other date"),
            Map.of(DESCRIPTION_VALUE, "Date Accepted: some acceptance date"),
            Map.of(DESCRIPTION_VALUE, "Date Copyrighted: some copyright date"),
            Map.of(DESCRIPTION_VALUE, "Date Submitted: some submission date"),
            Map.of(DESCRIPTION_VALUE, "Modified: some modified date"),
            Map.of(DESCRIPTION_VALUE, "Issued: some issuing date"),
            Map.of(DESCRIPTION_VALUE, "Valid: some validation date"),
            Map.of(DESCRIPTION_VALUE, "Coverage: some coverage description"),
            Map.of(DESCRIPTION_VALUE, "Coverage: some other coverage description"),
            Map.of(DESCRIPTION_VALUE, "<p>Even more descriptions</p>"),
            Map.of(DESCRIPTION_VALUE, "<p>And yet more</p>")
        );
        assertControlledMultiValueFieldContainsValues(citationBlockFields, SUBJECT,
            "Chemistry",
            "Computer and Information Science");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, KEYWORD,
            Map.of(KEYWORD_VALUE, "keyword1"),
            Map.of(KEYWORD_VALUE, "keyword2"),
            Map.of(KEYWORD_VALUE, "non-military uniform button",
                KEYWORD_VOCABULARY, "PAN thesaurus ideaaltypes",
                KEYWORD_VOCABULARY_URI, "https://data.cultureelerfgoed.nl/term/id/pan/PAN"),
            Map.of(KEYWORD_VALUE, "buttons (fasteners)",
                KEYWORD_VOCABULARY, "Art and Architecture Thesaurus",
                KEYWORD_VOCABULARY_URI, "http://vocab.getty.edu/aat/"),
            Map.of(KEYWORD_VALUE, "Old School Latin"),
            Map.of(KEYWORD_VALUE, "Ithkuil"),
            Map.of(KEYWORD_VALUE, "Ancient Greek"));
        assertControlledMultiValueFieldContainsValues(citationBlockFields, LANGUAGE,
            "Basque",
            "Kalaallisut, Greenlandic",
            "Western Frisian");
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, PRODUCTION_DATE, "2015-09-09");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, CONTRIBUTOR,
            Map.of(CONTRIBUTOR_NAME, "CON van Tributor (Contributing Org)", CONTRIBUTOR_TYPE, "Project Member"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - DataCollector", CONTRIBUTOR_TYPE, "Data Collector"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - DataCurator", CONTRIBUTOR_TYPE, "Data Curator"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - DataManager", CONTRIBUTOR_TYPE, "Data Manager"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - Editor", CONTRIBUTOR_TYPE, "Editor"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - HostingInstitution", CONTRIBUTOR_TYPE, "Hosting Institution"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - ProjectLeader", CONTRIBUTOR_TYPE, "Project Leader"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - ProjectManager", CONTRIBUTOR_TYPE, "Project Manager"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - ProjectMember", CONTRIBUTOR_TYPE, "Project Member"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - RelatedPerson", CONTRIBUTOR_TYPE, "Related Person"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - ResearchGroup", CONTRIBUTOR_TYPE, "Research Group"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - Researcher", CONTRIBUTOR_TYPE, "Researcher"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - Sponsor", CONTRIBUTOR_TYPE, "Sponsor"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - Supervisor", CONTRIBUTOR_TYPE, "Supervisor"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - WorkPackageLeader", CONTRIBUTOR_TYPE, "Work Package Leader"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - Producer", CONTRIBUTOR_TYPE, "Other"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - Other", CONTRIBUTOR_TYPE, "Other"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - RegistrationAuthority", CONTRIBUTOR_TYPE, "Other"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - RegistrationAgency", CONTRIBUTOR_TYPE, "Other"),
            Map.of(CONTRIBUTOR_NAME, "Contributing Org - ContactPerson", CONTRIBUTOR_TYPE, "Other")
        );
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, GRANT_NUMBER,
            Map.of(GRANT_NUMBER_AGENCY, "NWO",
                GRANT_NUMBER_VALUE, "54321"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DISTRIBUTOR,
            Map.of(DISTRIBUTOR_NAME, "D. I. Stributor"),
            Map.of(DISTRIBUTOR_NAME, "P. Ublisher"));
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, DISTRIBUTION_DATE, "2015-09-09");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DATE_OF_COLLECTION,
            Map.of(DATE_OF_COLLECTION_START, "2015-06-01",
                DATE_OF_COLLECTION_END, "2016-12-31"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, SERIES,
            Map.of(SERIES_INFORMATION, "<p>Information about a series: first</p>"),
            Map.of(SERIES_INFORMATION, "<p>Information about a series: second</p>"));
        assertPrimitiveMultiValueFieldContainsValues(citationBlockFields, DATA_SOURCES,
            "Source 2",
            "Sous an ayisyen",
            "Source 3");

        // Rights Metadata block
        var rightsMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansRights").getFields();
        assertPrimitiveMultiValueFieldContainsValues(rightsMetadataBlockFields, RIGHTS_HOLDER, "I Lastname");
        assertControlledSingleValueFieldContainsValue(rightsMetadataBlockFields, PERSONAL_DATA_PRESENT, "No");
        assertControlledMultiValueFieldContainsValues(rightsMetadataBlockFields, LANGUAGE_OF_METADATA,
            "English",
            "Georgian",
            "Haitian, Haitian Creole");

        // Relation Metadata block
        var relationMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansRelationMetadata").getFields();
        assertPrimitiveMultiValueFieldContainsValues(relationMetadataBlockFields, AUDIENCE,
            "https://www.narcis.nl/classification/D13400", // Inorganic Chemistry
            "https://www.narcis.nl/classification/D16300", // Theoretical computer science
            "https://www.narcis.nl/classification/D16100", // Computer systems, architectures, networks
            "https://www.narcis.nl/classification/D16200", // Software, algorithms, control systems
            "https://www.narcis.nl/classification/D16400", // Information systems, databases
            "https://www.narcis.nl/classification/D16500", // User interfaces, multimedia
            "https://www.narcis.nl/classification/E16000"); // Nanotechnology
        assertPrimitiveMultiValueFieldContainsValues(relationMetadataBlockFields, COLLECTION,
            "https://vocabularies.dans.knaw.nl/collections/ssh/ce21b6fb-4283-4194-9369-b3ff4c3d76e7"); // Erfgoed van de Oorlog
        assertCompoundMultiValueFieldContainsValues(relationMetadataBlockFields, RELATION,
            // RELATION_TYPE is capitalized in Dataverse's controlled vocabulary, but Dataverse picks it up anyway.
            Map.of(RELATION_TEXT, "Test relation",
                RELATION_TYPE, "relation",
                RELATION_URI, "https://example.com/relation"),
            Map.of(RELATION_TEXT, "Test conforms to",
                RELATION_TYPE, "conforms to",
                RELATION_URI, "https://example.com/conformsTo"),
            Map.of(RELATION_TEXT, "Test has format",
                RELATION_TYPE, "has format",
                RELATION_URI, "https://example.com/hasFormat"),
            Map.of(RELATION_TEXT, "Test has part",
                RELATION_TYPE, "has part",
                RELATION_URI, "https://example.com/hasPart"),
            Map.of(RELATION_TEXT, "Test references",
                RELATION_TYPE, "references",
                RELATION_URI, "https://example.com/references"),
            Map.of(RELATION_TEXT, "Test replaces",
                RELATION_TYPE, "replaces",
                RELATION_URI, "https://example.com/replaces"),
            Map.of(RELATION_TEXT, "Test requires",
                RELATION_TYPE, "requires",
                RELATION_URI, "https://example.com/requires"),
            Map.of(RELATION_TEXT, "Test has version",
                RELATION_TYPE, "has version",
                RELATION_URI, "https://example.com/hasVersion"),
            Map.of(RELATION_TEXT, "Test is format of",
                RELATION_TYPE, "is format of",
                RELATION_URI, "https://example.com/isFormatOf"),
            Map.of(RELATION_TEXT, "Test is part of",
                RELATION_TYPE, "is part of",
                RELATION_URI, "https://example.com/isPartOf"),
            Map.of(RELATION_TEXT, "Test is referenced by",
                RELATION_TYPE, "is referenced by",
                RELATION_URI, "https://example.com/isReferencedBy"),
            Map.of(RELATION_TEXT, "Test is required by",
                RELATION_TYPE, "is required by",
                RELATION_URI, "https://example.com/isRequiredBy"),
            Map.of(RELATION_TEXT, "Test is version of",
                RELATION_TYPE, "is version of",
                RELATION_URI, "https://example.com/isVersionOf")
        );

        // Archaeology Specific Metadata block
        var archaeologyMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansArchaeologyMetadata").getFields();
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ARCHIS_ZAAK_ID, "12345");
        assertCompoundMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ARCHIS_NUMBER,
            // In the UI the identifier from the CV will be displayed. Strangely, for the relation field the value is displayed, although capitalized.
            Map.of(ARCHIS_NUMBER_TYPE, "onderzoek",
                ARCHIS_NUMBER_ID, "12345"),
            Map.of(ARCHIS_NUMBER_TYPE, "vondstmelding",
                ARCHIS_NUMBER_ID, "67890"),
            Map.of(ARCHIS_NUMBER_TYPE, "monument",
                ARCHIS_NUMBER_ID, "12345"),
            Map.of(ARCHIS_NUMBER_TYPE, "waarneming",
                ARCHIS_NUMBER_ID, "67890"));
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ABR_RAPPORT_TYPE,
            "https://data.cultureelerfgoed.nl/term/id/abr/d6b2e162-3f49-4027-8f03-28194db2905e", // BAAC-rapport
            "https://data.cultureelerfgoed.nl/term/id/abr/a881708a-4545-4ca3-ac5e-bc096e98e9f7"); // RCE Rapportage Archeologische Monumentenzorg
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ABR_RAPPORT_NUMMER,
            "BAAC 123-A",
            "RCE Rapportage Archeologische Monumentenzorg");
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ABR_VERWERVINGSWIJZE,
            "https://data.cultureelerfgoed.nl/term/id/abr/967bfdf8-c44d-4c69-8318-34ed1ab1e784",
            "https://data.cultureelerfgoed.nl/term/id/abr/2f851932-e4a1-4a11-be5e-aa988fb39278");
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ABR_COMPLEX,
            "https://data.cultureelerfgoed.nl/term/id/abr/9a758542-8d0d-4afa-b664-104b938fe13e",
            "https://data.cultureelerfgoed.nl/term/id/abr/60bc20cd-0010-48ea-8d6f-42d333bfb39d",
            "https://data.cultureelerfgoed.nl/term/id/abr/2ac4deb4-a12a-4aa1-baa1-496b0e649941");
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ABR_ARTIFACT,
            "https://data.cultureelerfgoed.nl/term/id/abr/88d17503-7f04-4310-843d-e9e6005a163b",
            "https://data.cultureelerfgoed.nl/term/id/abr/5bd97bc0-697c-4128-b7b2-d2324bc4a2e1",
            "https://data.cultureelerfgoed.nl/term/id/abr/5c166519-c182-43c3-941c-551b3bed7136",
            "https://data.cultureelerfgoed.nl/term/id/abr/84bccabd-a463-4fda-8a7c-60390699436b");
        assertPrimitiveMultiValueFieldContainsValues(archaeologyMetadataBlockFields, ABR_PERIOD,
            "https://data.cultureelerfgoed.nl/term/id/abr/5b253754-ddd0-4ae0-a5bb-555176bca858",
            "https://data.cultureelerfgoed.nl/term/id/abr/6264b6bd-899e-4c34-a88e-03a36e1d4008");

        // Temporal and Spatial Metadata block
        var temporalSpatialMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansTemporalSpatial").getFields();
        assertPrimitiveMultiValueFieldContainsValues(temporalSpatialMetadataBlockFields, TEMPORAL_COVERAGE,
            "Het Romeinse Rijk",
            "De Oudheid");
        assertCompoundMultiValueFieldContainsValues(temporalSpatialMetadataBlockFields, SPATIAL_POINT,
            Map.of(SPATIAL_POINT_X, "126466",
                SPATIAL_POINT_Y, "529006",
                SPATIAL_POINT_SCHEME, "RD (in m.)"),
            Map.of(SPATIAL_POINT_X, "4.288788",
                SPATIAL_POINT_Y, "52.078663",
                SPATIAL_POINT_SCHEME, "longitude/latitude (degrees)"));
        assertCompoundMultiValueFieldContainsValues(temporalSpatialMetadataBlockFields, SPATIAL_BOX,
            Map.of(SPATIAL_BOX_NORTH, "628000",
                SPATIAL_BOX_EAST, "140000",
                SPATIAL_BOX_SOUTH, "335000",
                SPATIAL_BOX_WEST, "102000",
                SPATIAL_BOX_SCHEME, "RD (in m.)"),
            Map.of(SPATIAL_BOX_NORTH, "53.23074335194507",
                SPATIAL_BOX_EAST, "6.563118076315912",
                SPATIAL_BOX_SOUTH, "51.46343658020442",
                SPATIAL_BOX_WEST, "3.5621054065986075",
                SPATIAL_BOX_SCHEME, "longitude/latitude (degrees)"));
        assertControlledMultiValueFieldContainsValues(temporalSpatialMetadataBlockFields, SPATIAL_COVERAGE_CONTROLLED,
            "Japan", "South Africa");
        assertPrimitiveMultiValueFieldContainsValues(temporalSpatialMetadataBlockFields, SPATIAL_COVERAGE_UNCONTROLLED,
            "Roman Empire");

        // Vault Metadata block
        var dataVaultMetadata = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansDataVaultMetadata").getFields();
        // Assigned by pre-publication workflow, which hasn't run yet at this point
        assertFieldIsAbsent(dataVaultMetadata, DATAVERSE_PID);
        assertFieldIsAbsent(dataVaultMetadata, DATAVERSE_PID_VERSION);
        assertFieldIsAbsent(dataVaultMetadata, BAG_ID);
        assertFieldIsAbsent(dataVaultMetadata, NBN);

        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, DANS_OTHER_ID, "TESTPREFIX:1234"); // From bag-info.txt Has-Organizational-Identifier
        // TODO: add Has-Organizational-Identifier-Version to input bag

        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, SWORD_TOKEN, swordToken);
        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, DATA_SUPPLIER, "Supplier Joe");

        // Read edit-files.yml
        assertThat(deposit.getBagDir().resolve("edit-files.yml")).exists();
        var editFilesYml = yamlService.readYaml(deposit.getBagDir().resolve("edit-files.yml"), EditFilesRoot.class);

        assertThat(editFilesYml.getEditFiles().getAddUnrestrictedFiles()).containsExactlyInAnyOrder(
            "file1.txt",
            "original-metadata.zip"
        );
        assertThat(editFilesYml.getEditFiles().getAddRestrictedFiles()).containsExactlyInAnyOrder(
            "subdir/file2.txt"
        );
        assertThat(editFilesYml.getEditFiles().getUpdateFileMetas()).extracting("label").containsExactly("file1.txt");
        assertThat(editFilesYml.getEditFiles().getUpdateFileMetas()).extracting("description").containsExactly("A file with a simple description");
        assertThat(editFilesYml.getEditFiles().getReplaceFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getDeleteFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getMoveFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddEmbargoes()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddRestrictedIndividually()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddUnrestrictedIndividually()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAutoRenameFiles()).isEmpty();

        // Read edit-permissions.yml
        assertThat(deposit.getBagDir().resolve("edit-permissions.yml")).exists();
        var editPermissionsYml = yamlService.readYaml(deposit.getBagDir().resolve("edit-permissions.yml"), EditPermissionsRoot.class);

        var expectedRoleAssignment = new RoleAssignment();
        expectedRoleAssignment.setAssignee("@jdoe");
        expectedRoleAssignment.setRole("swordupdater");

        assertThat(editPermissionsYml.getEditPermissions().getAddRoleAssignments()).containsExactlyInAnyOrder(expectedRoleAssignment);
        assertThat(editPermissionsYml.getEditPermissions().getDeleteRoleAssignments()).isEmpty();
    }

    @Test
    public void run_converts_dans_sword_audiences_example_to_dataverse_ingest_deposit() throws Exception {
        /*
         * Given
         */
        var swordToken = "sword:64c59184-7667-4ea0-b4fd-09f421ecb3cf";
        var depositDir = testDir.resolve("00000000-0000-0000-0000-000000000002");
        DansDepositCreator.creator()
            .copyBagFrom(Paths.get("target/test/example-bags/valid/audiences"))
            .depositDir(depositDir)
            .withProperty("dataverse.sword-token", swordToken)
            .withProperty("deposit.origin", "SWORD")
            .withProperty("depositor.userId", "jdoe")
            .create();
        var authenticatedUser = authenticatedUser("John", "Doe", "jdoe@foo.com", "John Doe");
        Mockito.when(dataverseServiceMock.getUserById(Mockito.anyString())).thenReturn(Optional.of(authenticatedUser));
        Mockito.when(dataverseServiceMock.getSupportedLicenses()).thenReturn(licenses("http://opensource.org/licenses/MIT"));
        var deposit = dansBagDepositReader.readDeposit(depositDir);
        var mappingService = DansBagMappingServiceBuilder.builder()
            .dataSuppliers(Map.of("jdoe", "Supplier Joe"))
            .dataverseService(dataverseServiceMock).build();

        /*
         * When
         */
        new DansDepositConverter(deposit, null, null, mappingService, yamlService).run();

        /*
         * Then
         */
        assertThat(deposit.getBagDir().resolve("dataset.yml")).exists();
        var datasetYml = yamlService.readYaml(deposit.getBagDir().resolve("dataset.yml"), Dataset.class);

        // Citation block
        var citationBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("citation").getFields();
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, TITLE, "A bag containing multiple audiences");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, AUTHOR,
            Map.of(AUTHOR_NAME, "I Lastname",
                AUTHOR_AFFILIATION, "Example Org"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DATASET_CONTACT, Map.of(
            DATASET_CONTACT_NAME, "John Doe",
            DATASET_CONTACT_EMAIL, "jdoe@foo.com"
        ));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DESCRIPTION,
            Map.of(DESCRIPTION_VALUE,
                "<p>This bag contains multiple ddm:audience entries, each audience is represented by a code for which the classification can be found at https://www.narcis.nl/content/pdf/classification_en.pdf</p>")
        );
        assertControlledMultiValueFieldContainsValues(citationBlockFields, SUBJECT,
            "Computer and Information Science");
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, PRODUCTION_DATE, "2015-09-09");
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, DISTRIBUTION_DATE, "2015-09-09");

        // Rights Metadata block
        var rightsMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansRights").getFields();
        assertPrimitiveMultiValueFieldContainsValues(rightsMetadataBlockFields, RIGHTS_HOLDER, "I Lastname");
        assertControlledSingleValueFieldContainsValue(rightsMetadataBlockFields, PERSONAL_DATA_PRESENT, "No");
        assertControlledMultiValueFieldContainsValues(rightsMetadataBlockFields, LANGUAGE_OF_METADATA,
            "English");

        // Relation Metadata block
        var relationMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansRelationMetadata").getFields();
        assertPrimitiveMultiValueFieldContainsValues(relationMetadataBlockFields, AUDIENCE,
            "https://www.narcis.nl/classification/D16300", // Theoretical computer science
            "https://www.narcis.nl/classification/D16100", // Computer systems, architectures, networks
            "https://www.narcis.nl/classification/D16200", // Software, algorithms, control systems
            "https://www.narcis.nl/classification/D16400", // Information systems, databases
            "https://www.narcis.nl/classification/D16500", // User interfaces, multimedia
            "https://www.narcis.nl/classification/E16000"); // Nanotechnology

        // Vault Metadata block
        var dataVaultMetadata = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansDataVaultMetadata").getFields();
        // Assigned by pre-publication workflow, which hasn't run yet at this point
        assertFieldIsAbsent(dataVaultMetadata, DATAVERSE_PID);
        assertFieldIsAbsent(dataVaultMetadata, DATAVERSE_PID_VERSION);
        assertFieldIsAbsent(dataVaultMetadata, BAG_ID);
        assertFieldIsAbsent(dataVaultMetadata, NBN);

        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, SWORD_TOKEN, swordToken);
        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, DATA_SUPPLIER, "Supplier Joe");

        // Read edit-files.yml
        assertThat(deposit.getBagDir().resolve("edit-files.yml")).exists();
        var editFilesYml = yamlService.readYaml(deposit.getBagDir().resolve("edit-files.yml"), EditFilesRoot.class);

        assertThat(editFilesYml.getEditFiles().getAddUnrestrictedFiles()).containsExactlyInAnyOrder(
            "a/deeper/path/With some file.txt",
            "random images/image01.png",
            "random images/image02.jpeg",
            "random images/image03.jpeg",
            "original-metadata.zip"
        );
        assertThat(editFilesYml.getEditFiles().getAddRestrictedFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getUpdateFileMetas()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getReplaceFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getDeleteFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getMoveFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddEmbargoes()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddRestrictedIndividually()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddUnrestrictedIndividually()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAutoRenameFiles()).isEmpty();

        // Read edit-permissions.yml
        assertThat(deposit.getBagDir().resolve("edit-permissions.yml")).exists();
        var editPermissionsYml = yamlService.readYaml(deposit.getBagDir().resolve("edit-permissions.yml"), EditPermissionsRoot.class);

        var expectedRoleAssignment = new RoleAssignment();
        expectedRoleAssignment.setAssignee("@jdoe");
        expectedRoleAssignment.setRole("swordupdater");

        assertThat(editPermissionsYml.getEditPermissions().getAddRoleAssignments()).containsExactlyInAnyOrder(expectedRoleAssignment);
        assertThat(editPermissionsYml.getEditPermissions().getDeleteRoleAssignments()).isEmpty();
    }

    @Test
    public void run_converts_dans_sword_embargoed_example_to_dataverse_ingest_deposit() throws Exception {
        /*
         * Given
         */
        var swordToken = "sword:64c59184-7667-4ea0-b4fd-09f421ecb3cf";
        var depositDir = testDir.resolve("00000000-0000-0000-0000-000000000003");
        DansDepositCreator.creator()
            .copyBagFrom(Paths.get("target/test/example-bags/valid/embargoed"))
            .depositDir(depositDir)
            .withProperty("dataverse.sword-token", swordToken)
            .withProperty("deposit.origin", "SWORD")
            .withProperty("depositor.userId", "jdoe")
            .create();
        var authenticatedUser = authenticatedUser("John", "Doe", "jdoe@foo.com", "John Doe");
        Mockito.when(dataverseServiceMock.getUserById(Mockito.anyString())).thenReturn(Optional.of(authenticatedUser));
        Mockito.when(dataverseServiceMock.getSupportedLicenses()).thenReturn(licenses("http://opensource.org/licenses/MIT"));
        var deposit = dansBagDepositReader.readDeposit(depositDir);
        var mappingService = DansBagMappingServiceBuilder.builder()
            .dataSuppliers(Map.of("jdoe", "Supplier Joe"))
            .embargoExclusions(List.of("original-metadata.zip"))
            .dataverseService(dataverseServiceMock).build();

        /*
         * When
         */
        new DansDepositConverter(deposit, null, null, mappingService, yamlService).run();

        /*
         * Then
         */
        assertThat(deposit.getBagDir().resolve("dataset.yml")).exists();
        var datasetYml = yamlService.readYaml(deposit.getBagDir().resolve("dataset.yml"), Dataset.class);

        // Citation block
        var citationBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("citation").getFields();
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, TITLE, "A dataset that is under embargo");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, AUTHOR,
            Map.of(AUTHOR_NAME, "I Lastname",
                AUTHOR_AFFILIATION, "Example Org"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DATASET_CONTACT, Map.of(
            DATASET_CONTACT_NAME, "John Doe",
            DATASET_CONTACT_EMAIL, "jdoe@foo.com"
        ));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DESCRIPTION,
            Map.of(DESCRIPTION_VALUE,
                "<p>This dataset makes use of the ddm:available field to put all files under embargo until a specified date.</p>")
        );
        assertControlledMultiValueFieldContainsValues(citationBlockFields, SUBJECT,
            "Medicine, Health and Life Sciences");
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, PRODUCTION_DATE, "2015-09-09");
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, DISTRIBUTION_DATE, "2026-01-01");

        // Rights Metadata block
        var rightsMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansRights").getFields();
        assertPrimitiveMultiValueFieldContainsValues(rightsMetadataBlockFields, RIGHTS_HOLDER, "I Lastname");
        assertControlledSingleValueFieldContainsValue(rightsMetadataBlockFields, PERSONAL_DATA_PRESENT, "No");
        assertControlledMultiValueFieldContainsValues(rightsMetadataBlockFields, LANGUAGE_OF_METADATA,
            "English");

        // Relation Metadata block
        var relationMetadataBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansRelationMetadata").getFields();
        assertPrimitiveMultiValueFieldContainsValues(relationMetadataBlockFields, AUDIENCE,
            "https://www.narcis.nl/classification/D23320" // Anesthesiology
        );

        // Vault Metadata block
        var dataVaultMetadata = datasetYml.getDatasetVersion().getMetadataBlocks().get("dansDataVaultMetadata").getFields();
        // Assigned by pre-publication workflow, which hasn't run yet at this point
        assertFieldIsAbsent(dataVaultMetadata, DATAVERSE_PID);
        assertFieldIsAbsent(dataVaultMetadata, DATAVERSE_PID_VERSION);
        assertFieldIsAbsent(dataVaultMetadata, BAG_ID);
        assertFieldIsAbsent(dataVaultMetadata, NBN);

        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, SWORD_TOKEN, swordToken);
        assertPrimitiveSinglevalueFieldContainsValue(dataVaultMetadata, DATA_SUPPLIER, "Supplier Joe");

        // Read edit-files.yml
        assertThat(deposit.getBagDir().resolve("edit-files.yml")).exists();
        var editFilesYml = yamlService.readYaml(deposit.getBagDir().resolve("edit-files.yml"), EditFilesRoot.class);

        assertThat(editFilesYml.getEditFiles().getAddUnrestrictedFiles()).containsExactlyInAnyOrder(
            "a/deeper/path/With some file.txt",
            "random images/image01.png",
            "random images/image02.jpeg",
            "random images/image03.jpeg",
            "original-metadata.zip"
        );

        assertThat(editFilesYml.getEditFiles().getAddEmbargoes()).hasSize(1);
        assertThat(editFilesYml.getEditFiles().getAddEmbargoes().get(0).getFilePaths()).containsExactlyInAnyOrder(
            "a/deeper/path/With some file.txt",
            "random images/image01.png",
            "random images/image02.jpeg",
            "random images/image03.jpeg");
        assertThat(editFilesYml.getEditFiles().getAddEmbargoes().get(0)).extracting("dateAvailable").isEqualTo("2026-01-01");

        assertThat(editFilesYml.getEditFiles().getAddRestrictedFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getUpdateFileMetas()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getReplaceFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getDeleteFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getMoveFiles()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddRestrictedIndividually()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAddUnrestrictedIndividually()).isEmpty();
        assertThat(editFilesYml.getEditFiles().getAutoRenameFiles()).isEmpty();

        // Read edit-permissions.yml
        assertThat(deposit.getBagDir().resolve("edit-permissions.yml")).exists();
        var editPermissionsYml = yamlService.readYaml(deposit.getBagDir().resolve("edit-permissions.yml"), EditPermissionsRoot.class);

        var expectedRoleAssignment = new RoleAssignment();
        expectedRoleAssignment.setAssignee("@jdoe");
        expectedRoleAssignment.setRole("swordupdater");

        assertThat(editPermissionsYml.getEditPermissions().getAddRoleAssignments()).containsExactlyInAnyOrder(expectedRoleAssignment);
        assertThat(editPermissionsYml.getEditPermissions().getDeleteRoleAssignments()).isEmpty();
    }
}