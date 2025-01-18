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
package nl.knaw.dans.dvingest.core.yaml.tasklog;

import lombok.Data;

@Data
public class EditFilesLog {
    private CompletableItemWithCount deleteFiles = new CompletableItemWithCount();
    private CompletableItemWithCount replaceFiles = new CompletableItemWithCount();
    private CompletableItemWithCount addUnrestrictedFiles = new CompletableItemWithCount();
    private CompletableItemWithCount addRestrictedFiles = new CompletableItemWithCount();
    private CompletableItemWithCount moveFiles = new CompletableItemWithCount();
    private CompletableItemWithCount updateFileMetas = new CompletableItemWithCount();
    private CompletableItemWithCount addEmbargoes = new CompletableItemWithCount();
    private CompletableItemWithCount addUnrestrictedIndividually = new CompletableItemWithCount();
    private CompletableItemWithCount addRestrictedIndividually = new CompletableItemWithCount();

    public void completeAll() {
        deleteFiles.setCompleted(true);
        replaceFiles.setCompleted(true);
        addUnrestrictedFiles.setCompleted(true);
        addRestrictedFiles.setCompleted(true);
        moveFiles.setCompleted(true);
        updateFileMetas.setCompleted(true);
        addEmbargoes.setCompleted(true);
        addUnrestrictedIndividually.setCompleted(true);
        addRestrictedIndividually.setCompleted(true);
    }
}
