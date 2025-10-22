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

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.dvingest.core.dansbag.ManifestUtil;
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;
import nl.knaw.dans.dvingest.core.dansbag.xml.XPathEvaluator;
import nl.knaw.dans.dvingest.core.dansbag.xml.XmlReader;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nl.knaw.dans.dvingest.core.dansbag.xml.XPathConstants.FILES_FILE;

public class DansBagDepositReaderImpl implements DansBagDepositReader {
    private static final String DEPOSIT_PROPERTIES_FILENAME = "deposit.properties";

    private final XmlReader xmlReader;
    private final BagReader bagReader;

    public DansBagDepositReaderImpl(XmlReader xmlReader, BagReader bagReader) {
        this.xmlReader = xmlReader;
        this.bagReader = bagReader;
    }

    @Override
    public DansBagDeposit readDeposit(Path depositDir) throws InvalidDepositException {
        try {
            var bagDir = getBagDir(depositDir);

            var depositProperties = readDepositProperties(depositDir);
            var bag = bagReader.read(bagDir);
            ManifestUtil.ensureSha1ManifestPresent(bag);

            var deposit = mapToDeposit(bag, depositProperties);

            deposit.setBag(bag);
            deposit.setDdm(readRequiredXmlFile(deposit.getDdmPath()));
            deposit.setFilesXml(readRequiredXmlFile(deposit.getFilesXmlPath()));
            deposit.setAmd(readOptionalXmlFile(deposit.getAmdPath()));

            deposit.setFiles(getDepositFiles(deposit));

            return deposit;
        }
        catch (Exception cex) {
            throw new InvalidDepositException(cex.getMessage(), cex);
        }
    }

    private Path getBagDir(Path depositDir) throws InvalidDepositException, IOException {
        if (!Files.isDirectory(depositDir)) {
            throw new InvalidDepositException(String.format("%s is not a directory", depositDir));
        }

        try (var substream = Files.list(depositDir).filter(Files::isDirectory)) {
            var directories = substream.toList();

            // only 1 directory allowed, not 0 or more than 1
            if (directories.size() != 1) {
                throw new InvalidDepositException(String.format(
                    "%s has more or fewer than one subdirectory", depositDir
                ));
            }

            // check for the presence of deposit.properties and bagit.txt
            if (!Files.exists(depositDir.resolve("deposit.properties"))) {
                throw new InvalidDepositException(String.format(
                    "%s does not contain a deposit.properties file", depositDir
                ));
            }

            var bagDir = directories.get(0);

            if (!Files.exists(bagDir.resolve("bagit.txt"))) {
                throw new InvalidDepositException(String.format(
                    "%s does not contain a bag", depositDir
                ));
            }

            return bagDir;
        }
    }

    private Configuration readDepositProperties(Path depositDir) throws ConfigurationException {
        var propertiesFile = depositDir.resolve(DEPOSIT_PROPERTIES_FILENAME);
        var params = new Parameters();
        var paramConfig = params.properties()
            .setFileName(propertiesFile.toString());

        var builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>
            (PropertiesConfiguration.class, null, true)
            .configure(paramConfig);

        return builder.getConfiguration();
    }

    private List<DepositFile> getDepositFiles(DansBagDeposit dansBagDeposit) throws IOException {
        var bag = dansBagDeposit.getBag();
        var filePathToSha1 = ManifestUtil.getFilePathToSha1(bag);

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

    private Document readRequiredXmlFile(Path path) throws ParserConfigurationException, IOException, SAXException {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Required file not found: " + path);
        }
        return xmlReader.readXmlFile(path);
    }

    private Document readOptionalXmlFile(Path path) throws ParserConfigurationException, IOException, SAXException {
        if (Files.exists(path)) {
            return xmlReader.readXmlFile(path);
        }

        return null;
    }

    private DansBagDeposit mapToDeposit(Bag bag, Configuration depositProperties) {
        var deposit = new DansBagDeposit();
        deposit.setBagDir(bag.getRootDir());
        deposit.setDir(bag.getRootDir().getParent());
        deposit.setDoi(depositProperties.getString("identifier.doi", ""));
        deposit.setUrn(depositProperties.getString("identifier.urn"));
        deposit.setCreated(Optional.ofNullable(depositProperties.getString("creation.timestamp")).map(OffsetDateTime::parse).orElse(null));
        deposit.setDepositorUserId(depositProperties.getString("depositor.userId"));

        deposit.setDataverseIdProtocol(depositProperties.getString("dataverse.id-protocol", ""));
        deposit.setDataverseIdAuthority(depositProperties.getString("dataverse.id-authority", ""));
        deposit.setDataverseId(depositProperties.getString("dataverse.id-identifier", ""));
        deposit.setDataverseBagId(depositProperties.getString("dataverse.bag-id", ""));
        deposit.setDataverseNbn(depositProperties.getString("dataverse.nbn", ""));
        deposit.setDataverseOtherId(depositProperties.getString("dataverse.other-id", ""));
        deposit.setDataverseOtherIdVersion(depositProperties.getString("dataverse.other-id-version", ""));
        deposit.setDataverseSwordToken(depositProperties.getString("dataverse.sword-token", ""));
        deposit.setHasOrganizationalIdentifier(getFirstValue(bag.getMetadata().get("Has-Organizational-Identifier")));

        var isVersionOf = bag.getMetadata().get("Is-Version-Of");

        if (isVersionOf != null) {
            isVersionOf.stream()
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .ifPresent(item -> {
                    deposit.setUpdate(true);
                    deposit.setIsVersionOf(item);
                });
        }

        return deposit;
    }

    private String getFirstValue(List<String> value) {
        if (value == null) {
            return null;
        }

        return value.stream()
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
    }

}
