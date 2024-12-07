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
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelper;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelperImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.XPathEvaluator;
import nl.knaw.dans.dvingest.core.dansbag.service.XmlReader;
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

import static nl.knaw.dans.dvingest.core.dansbag.service.XPathConstants.FILES_FILE;

public class DepositReaderImpl implements DepositReader {
    private static final String DEPOSIT_PROPERTIES_FILENAME = "deposit.properties";

    private final XmlReader xmlReader;
    private final BagDirResolver bagDirResolver;
    private final BagReader bagReader;

    private final ManifestHelper manifestHelper;

    public DepositReaderImpl(XmlReader xmlReader, BagDirResolver bagDirResolver, BagReader bagReader, ManifestHelper manifestHelper) {
        this.xmlReader = xmlReader;
        this.bagDirResolver = bagDirResolver;
        this.bagReader = bagReader;
        this.manifestHelper = manifestHelper;
    }

    @Override
    public DansBagDeposit readDeposit(Path depositDir) throws InvalidDepositException {
        try {
            var bagDir = bagDirResolver.getBagDir(depositDir);

            var config = readDepositProperties(depositDir);
            var bag = bagReader.read(bagDir);
            manifestHelper.ensureSha1ManifestPresent(bag);

            var deposit = mapToDeposit(depositDir, bagDir, config, bag);

            deposit.setBag(bag);
            deposit.setDdm(readRequiredXmlFile(deposit.getDdmPath()));
            deposit.setFilesXml(readRequiredXmlFile(deposit.getFilesXmlPath()));
            deposit.setAmd(readOptionalXmlFile(deposit.getAmdPath()));

            deposit.setFiles(getDepositFiles(deposit));

            return deposit;
        }
        catch (Throwable cex) {
            throw new InvalidDepositException(cex.getMessage(), cex);
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

    private DansBagDeposit mapToDeposit(Path path, Path bagDir, Configuration config, Bag bag) {
        var deposit = new DansBagDeposit();
        deposit.setBagDir(bagDir);
        deposit.setDir(path);
        deposit.setDoi(config.getString("identifier.doi", ""));
        deposit.setUrn(config.getString("identifier.urn"));
        deposit.setCreated(Optional.ofNullable(config.getString("creation.timestamp")).map(OffsetDateTime::parse).orElse(null));
        deposit.setDepositorUserId(config.getString("depositor.userId"));

        deposit.setDataverseIdProtocol(config.getString("dataverse.id-protocol", ""));
        deposit.setDataverseIdAuthority(config.getString("dataverse.id-authority", ""));
        deposit.setDataverseId(config.getString("dataverse.id-identifier", ""));
        deposit.setDataverseBagId(config.getString("dataverse.bag-id", ""));
        deposit.setDataverseNbn(config.getString("dataverse.nbn", ""));
        deposit.setDataverseOtherId(config.getString("dataverse.other-id", ""));
        deposit.setDataverseOtherIdVersion(config.getString("dataverse.other-id-version", ""));
        deposit.setDataverseSwordToken(config.getString("dataverse.sword-token", ""));
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
