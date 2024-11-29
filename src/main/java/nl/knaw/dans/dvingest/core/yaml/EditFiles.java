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
package nl.knaw.dans.dvingest.core.yaml;

import lombok.Data;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.util.List;

@Data
public class EditFiles {
    private List<String> deleteFiles = List.of();
    private List<String> replaceFiles = List.of();
    private List<String> addRestrictedFiles = List.of();
    private List<Move> moveFiles = List.of();
    private List<FileMeta> updateFileMetas = List.of();
    private List<AddEmbargo> addEmbargoes = List.of();
}
