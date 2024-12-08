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
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DansDepositConverterTest extends DansConversionFixture {

    private final YamlService yamlService = new YamlServiceImpl();

    @Test
    public void run_converts_dans_sword_all_mappings_example_to_dataverse_ingest_deposit() throws Exception {
        // Given
        FileUtils.copyDirectoryToDirectory(Paths.get("src/test/resources/unit-test/d0919038-9866-49e8-986a-bcef54ae7566").toFile(), testDir.toFile());
        var depositDir = testDir.resolve("d0919038-9866-49e8-986a-bcef54ae7566");
        var deposit = dansBagDepositReader.readDeposit(depositDir);
        var authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setFirstName("John");
        authenticatedUser.setLastName("Doe");
        authenticatedUser.setEmail("jdoe@foo.com");
        Mockito.when(dataverseServiceMock.getUserById(Mockito.anyString())).thenReturn(Optional.of(authenticatedUser));

        // When
        new DansDepositConverter(deposit, null, mappingService, yamlService).run();

        // Then
        assertThat(deposit.getBagDir().resolve("dataset.yml")).exists();
    }

}
