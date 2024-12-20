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

import nl.knaw.dans.dvingest.core.dansbag.testhelpers.DansBagMappingServiceBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DansBagMappingServiceTest extends DansConversionFixture {

    @Test
    public void getUpdatesDataset_finds_null_for_non_update_deposit() throws Exception {
        // Given
        var testDepositDir = testDir.resolve("00000000-0000-0000-0000-000000000001");
        FileUtils.copyDirectory(Paths.get("src/test/resources/unit-test/update-deposits/00000000-0000-0000-0000-000000000001").toFile(), testDepositDir.toFile());
        var mappingService = DansBagMappingServiceBuilder.builder().dataverseService(dataverseServiceMock).build();

        // When / Then
        assertThat(mappingService.getUpdatesDataset(testDepositDir)).isNull();
    }

    @Test
    public void getUpdatesDataset_finds_dataset_for_update_deposit() throws Exception {
        // Given
        var doi = "doi:10.5072/dans-2xg-4yf";
        when(dataverseServiceMock.findDoiByMetadataField("dansSwordToken", "sword:00000000-0000-0000-0000-000000000001")).thenReturn(List.of(doi));
        var testDepositDir = testDir.resolve("00000000-0000-0000-0000-000000000002");
        FileUtils.copyDirectory(Paths.get("src/test/resources/unit-test/update-deposits/00000000-0000-0000-0000-000000000002").toFile(), testDepositDir.toFile());
        var mappingService = DansBagMappingServiceBuilder.builder().dataverseService(dataverseServiceMock).build();

        // When / Then
        assertThat(mappingService.getUpdatesDataset(testDepositDir)).isEqualTo(doi);
    }
}
