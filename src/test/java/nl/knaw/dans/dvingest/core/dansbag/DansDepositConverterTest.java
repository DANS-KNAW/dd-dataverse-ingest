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


        // TODO: CHECK ALL THE OTHER FIELDS

    }

}
