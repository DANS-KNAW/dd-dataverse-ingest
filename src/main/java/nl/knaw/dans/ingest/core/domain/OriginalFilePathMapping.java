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
package nl.knaw.dans.ingest.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OriginalFilePathMapping {
    // the first entry is the logical (original) name, as provided by the deposit
    // the second entry is the physical path, renamed to solve filesystem limitations
    private final Map<Path, Path> mappings = new HashMap<>();

    public OriginalFilePathMapping(Collection<Mapping> mappings) {
        for (var mapping: mappings) {
            this.mappings.put(mapping.getOriginalPath(), mapping.getPhysicalPath());
        }
    }

    public boolean hasMapping(Path path) {
        return this.mappings.containsKey(path);
    }

    public Path getPhysicalPath(Path path) {
        return this.mappings.getOrDefault(path, path);
    }

    @Data
    @AllArgsConstructor
    public static class Mapping {
        private Path physicalPath;
        private Path originalPath;
    }
}
