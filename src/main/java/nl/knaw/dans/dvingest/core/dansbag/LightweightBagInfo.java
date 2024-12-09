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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lightweight bag-info.txt parser, which can be used without reading the whole bag.
 */
public class LightweightBagInfo {
    private final Map<String, String> keyValues;

    public LightweightBagInfo(Path file) throws IOException {
        try (var linesStream = Files.lines(file)) {
            keyValues = linesStream
                .map(line -> line.split(":", 2))
                .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
        }
    }

    public String get(String key) {
        return keyValues.get(key);
    }
}
