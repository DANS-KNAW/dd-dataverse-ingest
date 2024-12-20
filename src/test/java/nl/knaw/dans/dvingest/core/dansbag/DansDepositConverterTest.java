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

import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.ALTERNATIVE_TITLE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUDIENCE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_AFFILIATION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_IDENTIFIER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_IDENTIFIER_SCHEME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.COLLECTION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATASET_CONTACT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATASET_CONTACT_EMAIL;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATASET_CONTACT_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DATA_SOURCES;
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
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SUBJECT;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.TITLE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link DansDepositConverter}. It uses the valid examples from the dd-dans-sword2-examples project.
 */
public class DansDepositConverterTest extends DansConversionFixture {

    private final YamlService yamlService = new YamlServiceImpl();

    @Test
    public void run_converts_dans_sword_all_mappings_example_to_dataverse_ingest_deposit() throws Exception {
        // Given
        var depositDir = createValidDeposit("all-mappings", "00000000-0000-0000-0000-000000000001");
        var authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("John");
        authenticatedUser.setLastName("Doe");
        authenticatedUser.setEmail("jdoe@foo.com");
        authenticatedUser.setDisplayName("John Doe");
        Mockito.when(dataverseServiceMock.getUserById(Mockito.anyString())).thenReturn(Optional.of(authenticatedUser));
        var deposit = dansBagDepositReader.readDeposit(depositDir);

        // When
        new DansDepositConverter(deposit, null, null, mappingService, yamlService).run();

        // Then
        assertThat(deposit.getBagDir().resolve("dataset.yml")).exists();
        var datasetYml = yamlService.readYaml(deposit.getBagDir().resolve("dataset.yml"), Dataset.class);

        // Citation block
        var citationBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("citation").getFields();
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, TITLE, "A bag containing examples for each mapping rule");
        assertPrimitiveMultiValueFieldContainsValues(citationBlockFields, ALTERNATIVE_TITLE, "DCTERMS title 1");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, DATASET_CONTACT, Map.of(
            DATASET_CONTACT_NAME, "John Doe",
            DATASET_CONTACT_EMAIL, "jdoe@foo.com"
        ));
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
            Map.of(KEYWORD_VALUE, "Ithkuil"));
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

    }

}
