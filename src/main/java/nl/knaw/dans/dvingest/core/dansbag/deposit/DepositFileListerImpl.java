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
package nl.knaw.dans.dvingest.core.dansbag.deposit;

import nl.knaw.dans.dvingest.core.dansbag.domain.DansBagDeposit;
import nl.knaw.dans.dvingest.core.dansbag.domain.DepositFile;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelperImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.XPathEvaluator;
import org.w3c.dom.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nl.knaw.dans.dvingest.core.dansbag.service.XPathConstants.FILES_FILE;

public class DepositFileListerImpl implements DepositFileLister {
    @Override
    public List<DepositFile> getDepositFiles(DansBagDeposit dansBagDeposit) throws IOException {
        var bag = dansBagDeposit.getBag();
        var filePathToSha1 = ManifestHelperImpl.getFilePathToSha1(bag);

        return XPathEvaluator.nodes(dansBagDeposit.getFilesXml(), FILES_FILE)
            .map(node -> {
                var filePath = Optional.ofNullable(node.getAttributes().getNamedItem("filepath"))
                    .map(Node::getTextContent)
                    .map(Path::of)
                    .orElseThrow(() -> new IllegalArgumentException("File element without filepath attribute"));

                var sha1 = filePathToSha1.get(filePath);

                return new DepositFile(filePath, sha1, node);
            })
            .collect(Collectors.toList());
    }
}