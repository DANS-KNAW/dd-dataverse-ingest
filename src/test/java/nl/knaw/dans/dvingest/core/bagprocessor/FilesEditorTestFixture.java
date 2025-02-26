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
package nl.knaw.dans.dvingest.core.bagprocessor;

import nl.knaw.dans.dvingest.core.TestDirFixture;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.lib.dataverse.model.file.DataFile;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

public class FilesEditorTestFixture extends TestDirFixture {
    protected static final YamlService yamlService = new YamlServiceImpl();
    protected final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);
    protected final UtilityServices utilityServicesMock = Mockito.mock(UtilityServices.class);

    protected Path dataDir;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.reset(dataverseServiceMock);
        Mockito.reset(utilityServicesMock);
        dataDir = testDir.resolve("data");
        Files.createDirectories(dataDir);
    }

    protected FileMeta file(String path, int id) {
        var dvPath = new DataversePath(path);
        var fileMeta = new FileMeta();
        var dataFile = new DataFile();
        dataFile.setId(id);
        fileMeta.setDataFile(dataFile);
        fileMeta.setLabel(dvPath.getLabel());
        fileMeta.setDirectoryLabel(dvPath.getDirectoryLabel());
        return fileMeta;
    }
}
