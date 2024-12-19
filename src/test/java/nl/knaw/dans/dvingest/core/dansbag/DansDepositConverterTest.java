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
        var citationBlockFields = datasetYml.getDatasetVersion().getMetadataBlocks().get("citation").getFields();
        // Find the metadata field with property typeName = "title"
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, "title", "A bag containing examples for each mapping rule");
        assertPrimitiveMultiValueFieldContainsValues(citationBlockFields, "alternativeTitle", "DCTERMS title 1");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "datasetContact", Map.of(
            "datasetContactName", "John Doe",
            "datasetContactEmail", "jdoe@foo.com"
        ));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "otherId",
            Map.of("otherIdAgency", "", "otherIdValue", "DCTERMS_ID001"),
            Map.of("otherIdAgency", "", "otherIdValue", "DC_ID002"),
            Map.of("otherIdAgency", "", "otherIdValue", "DCTERMS_ID003"),
            Map.of("otherIdAgency", "TESTPREFIX", "otherIdValue", "1234"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "author",
            Map.of("authorName", "I Lastname",
                "authorAffiliation", "Example Org",
                "authorIdentifierScheme", "ORCID",
                "authorIdentifier", "0000-0001-9183-9538"),
            Map.of("authorName", "Creator Organization"),
            Map.of("authorName", "Unformatted Creator"),
            Map.of("authorName", "Another Unformatted Creator"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "datasetContact",
            Map.of("datasetContactName", "John Doe",
                "datasetContactEmail", "jdoe@foo.com"));
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "dsDescription",
            Map.of("dsDescriptionValue", "<p>This bags contains one or more examples of each mapping rule.</p>"),
            Map.of("dsDescriptionValue", "<p>A second description</p>"),
            Map.of("dsDescriptionValue", "<p>DC title 2</p>"),
            Map.of("dsDescriptionValue", "<p>DCTERMS alt title 1</p>"),
            Map.of("dsDescriptionValue", "<p>DCTERMS alt title 2</p>"),
            Map.of("dsDescriptionValue", "Date: some date"),
            Map.of("dsDescriptionValue", "Date: some other date"),
            Map.of("dsDescriptionValue", "Date Accepted: some acceptance date"),
            Map.of("dsDescriptionValue", "Date Copyrighted: some copyright date"),
            Map.of("dsDescriptionValue", "Date Submitted: some submission date"),
            Map.of("dsDescriptionValue", "Modified: some modified date"),
            Map.of("dsDescriptionValue", "Issued: some issuing date"),
            Map.of("dsDescriptionValue", "Valid: some validation date"),
            Map.of("dsDescriptionValue", "Coverage: some coverage description"),
            Map.of("dsDescriptionValue", "Coverage: some other coverage description"),
            Map.of("dsDescriptionValue", "<p>Even more descriptions</p>"),
            Map.of("dsDescriptionValue", "<p>And yet more</p>")
        );
        assertControlledMultiValueFieldContainsValues(citationBlockFields, "subject",
            "Chemistry",
            "Computer and Information Science");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "keyword",
            Map.of("keywordValue", "keyword1"),
            Map.of("keywordValue", "keyword2"),
            Map.of("keywordValue", "non-military uniform button",
                "keywordVocabulary", "PAN thesaurus ideaaltypes",
                "keywordVocabularyURI", "https://data.cultureelerfgoed.nl/term/id/pan/PAN"),
            Map.of("keywordValue", "buttons (fasteners)",
                "keywordVocabulary", "Art and Architecture Thesaurus",
                "keywordVocabularyURI", "http://vocab.getty.edu/aat/"),
            Map.of("keywordValue", "Old School Latin"),
            Map.of("keywordValue", "Ithkuil"));
        assertControlledMultiValueFieldContainsValues(citationBlockFields, "language",
            "Basque",
            "Kalaallisut, Greenlandic",
            "Western Frisian");
        assertPrimitiveSinglevalueFieldContainsValue(citationBlockFields, "productionDate", "2015-09-09");
        assertCompoundMultiValueFieldContainsValues(citationBlockFields, "contributor",
            Map.of("contributorName", "CON van Tributor (Contributing Org)", "contributorType", "Project Member"),
            Map.of("contributorName", "Contributing Org - DataCollector", "contributorType", "Data Collector"),
            Map.of("contributorName", "Contributing Org - DataCurator", "contributorType", "Data Curator"),
            Map.of("contributorName", "Contributing Org - DataManager", "contributorType", "Data Manager"),
            Map.of("contributorName", "Contributing Org - Editor", "contributorType", "Editor"),
            Map.of("contributorName", "Contributing Org - HostingInstitution", "contributorType", "Hosting Institution"),
            Map.of("contributorName", "Contributing Org - ProjectLeader", "contributorType", "Project Leader"),
            Map.of("contributorName", "Contributing Org - ProjectManager", "contributorType", "Project Manager"),
            Map.of("contributorName", "Contributing Org - ProjectMember", "contributorType", "Project Member"),
            Map.of("contributorName", "Contributing Org - RelatedPerson", "contributorType", "Related Person"),
            Map.of("contributorName", "Contributing Org - ResearchGroup", "contributorType", "Research Group"),
            Map.of("contributorName", "Contributing Org - Researcher", "contributorType", "Researcher"),
            Map.of("contributorName", "Contributing Org - Sponsor", "contributorType", "Sponsor"),
            Map.of("contributorName", "Contributing Org - Supervisor", "contributorType", "Supervisor"),
            Map.of("contributorName", "Contributing Org - WorkPackageLeader", "contributorType", "Work Package Leader"),
            Map.of("contributorName", "Contributing Org - Producer", "contributorType", "Other"),
            Map.of("contributorName", "Contributing Org - Other", "contributorType", "Other"),
            Map.of("contributorName", "Contributing Org - RegistrationAuthority", "contributorType", "Other"),
            Map.of("contributorName", "Contributing Org - RegistrationAgency", "contributorType", "Other"),
            Map.of("contributorName", "Contributing Org - ContactPerson", "contributorType", "Other")
        );

        // TODO: CHECK ALL THE OTHER FIELDS

    }

}
