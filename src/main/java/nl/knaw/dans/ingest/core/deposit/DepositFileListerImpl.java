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
package nl.knaw.dans.ingest.core.deposit;

import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.DepositFile;
import nl.knaw.dans.ingest.core.domain.OriginalFilePathMapping;
import nl.knaw.dans.ingest.core.service.ManifestHelperImpl;
import nl.knaw.dans.ingest.core.service.XPathEvaluator;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static nl.knaw.dans.ingest.core.service.XPathConstants.FILES_FILE;

public class DepositFileListerImpl implements DepositFileLister {
    @Override
    public List<DepositFile> getDepositFiles(Deposit deposit) throws IOException {
        var bag = deposit.getBag();
        var bagDir = bag.getRootDir();
        var filePathToSha1 = ManifestHelperImpl.getFilePathToSha1(bag);
        var originalFilePathMappings = getOriginalFilePathMapping(bagDir);

        return XPathEvaluator.nodes(deposit.getFilesXml(), FILES_FILE)
            .map(node -> {
                var filePath = Optional.ofNullable(node.getAttributes().getNamedItem("filepath"))
                    .map(Node::getTextContent)
                    .map(Path::of)
                    .orElseThrow(() -> new IllegalArgumentException("File element without filepath attribute"));

                var physicalFile = originalFilePathMappings.getPhysicalPath(filePath);
                var sha1 = filePathToSha1.get(physicalFile);

                return new DepositFile(filePath, physicalFile, sha1, node);
            })
            .collect(Collectors.toList());
    }

    private OriginalFilePathMapping getOriginalFilePathMapping(Path bagDir) throws IOException {
        var originalFilepathsFile = bagDir.resolve("original-filepaths.txt");

        if (Files.exists(originalFilepathsFile)) {
            var lines = Files.readAllLines(originalFilepathsFile);
            var mappings = lines.stream().map(line -> {
                    var parts = line.split("\\s+", 2);

                    if (parts.length == 2) {
                        return new OriginalFilePathMapping.Mapping(
                            Path.of(parts[0].trim()),
                            Path.of(parts[1].trim())
                        );
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            return new OriginalFilePathMapping(mappings);
        }
        else {
            return new OriginalFilePathMapping(Set.of());
        }
    }
}