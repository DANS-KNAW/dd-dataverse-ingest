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

import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.FileInfo;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Overrides the methods in EditFilesComposer to handle the case of an update to an existing dataset.
 */
public class EditFilesComposerForUpdate extends EditFilesComposer {
    private final DataverseService dataverseService;

    public EditFilesComposerForUpdate(Deposit dansDeposit, Pattern fileExclusionPattern, List<String> embargoExclusions, DataverseService dataverseService) {
        super(dansDeposit, fileExclusionPattern, embargoExclusions);
        this.dataverseService = dataverseService;
    }

    @Override
    protected List<String> getFilesToIgnore(Map<Path, FileInfo> files) {
        super.getFilesToIgnore(files);






    }

    @Override
    protected List<String> getRestrictedFilesToAdd(Map<Path, FileInfo> files) {
        return super.getRestrictedFilesToAdd(files);
    }

    @Override
    protected List<FileMeta> getUpdatedFileMetas(Map<Path, FileInfo> files) {
        return super.getUpdatedFileMetas(files);
    }

    @Override
    protected List<String> getDeleteFiles(Map<Path, FileInfo> files) {
        return super.getDeleteFiles(files);
    }
}
