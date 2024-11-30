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

import lombok.AllArgsConstructor;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import org.apache.commons.collections4.Predicate;

import java.io.File;
import java.nio.file.Path;

@AllArgsConstructor
public class FileUploadInclusionPredicate implements Predicate<File> {
    private final EditFiles editFiles;
    private final Path dataDir;
    private final boolean restrictedFiles;

    @Override
    public boolean evaluate(File file) {
        if (restrictedFiles) {
            return editFiles != null && isRestricted(file) && notReplaced(file) && notIgnored(file);
        }
        else {
            return editFiles == null || notRestricted(file) && notReplaced(file) && notIgnored(file);
        }
    }

    private boolean notReplaced(File file) {
        return !editFiles.getReplaceFiles().contains(dataDir.relativize(file.toPath()).toString());
    }

    private boolean isRestricted(File file) {
        return editFiles.getAddRestrictedFiles().contains(dataDir.relativize(file.toPath()).toString());
    }

    private boolean notRestricted(File file) {
        return !editFiles.getAddRestrictedFiles().contains(dataDir.relativize(file.toPath()).toString());
    }

    private boolean notIgnored(File file) {
        return !editFiles.getIgnoreFiles().contains(dataDir.relativize(file.toPath()).toString());
    }
}