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

import gov.loc.repository.bagit.domain.Bag;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.DepositLocation;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.io.BagDataManager;
import nl.knaw.dans.ingest.core.io.FileService;
import nl.knaw.dans.ingest.core.service.ManifestHelper;
import nl.knaw.dans.ingest.core.service.XmlReader;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class DepositReaderImpl implements DepositReader {
    private final XmlReader xmlReader;
    private final BagDirResolver bagDirResolver;
    private final FileService fileService;
    private final BagDataManager bagDataManager;
    private final DepositFileLister depositFileLister;

    private final ManifestHelper manifestHelper;

    public DepositReaderImpl(XmlReader xmlReader, BagDirResolver bagDirResolver, FileService fileService, BagDataManager bagDataManager, DepositFileLister depositFileLister, ManifestHelper manifestHelper) {
        this.xmlReader = xmlReader;
        this.bagDirResolver = bagDirResolver;
        this.fileService = fileService;
        this.bagDataManager = bagDataManager;
        this.depositFileLister = depositFileLister;
        this.manifestHelper = manifestHelper;
    }

    @Override
    public synchronized Deposit readDeposit(DepositLocation location) throws InvalidDepositException {
        return readDeposit(location.getDir());
    }

    @Override
    public Deposit readDeposit(Path depositDir) throws InvalidDepositException {
        try {
            var bagDir = bagDirResolver.getBagDir(depositDir);

            var config = bagDataManager.readDepositProperties(depositDir);
            var bag = bagDataManager.readBag(bagDir);
            manifestHelper.ensureSha1ManifestPresent(bag);

            var deposit = mapToDeposit(depositDir, bagDir, config, bag);

            deposit.setBag(bag);
            deposit.setDdm(readOptionalXmlFile(deposit.getDdmPath()));
            deposit.setFilesXml(readOptionalXmlFile(deposit.getFilesXmlPath()));
            deposit.setAmd(readOptionalXmlFile(deposit.getAmdPath()));

            deposit.setFiles(depositFileLister.getDepositFiles(deposit));

            return deposit;
        }
        catch (Throwable cex) {
            throw new InvalidDepositException(cex.getMessage(), cex);
        }
    }

    Document readOptionalXmlFile(Path path) throws ParserConfigurationException, IOException, SAXException {
        if (fileService.fileExists(path)) {
            return xmlReader.readXmlFile(path);
        }

        return null;
    }

    Deposit mapToDeposit(Path path, Path bagDir, Configuration config, Bag bag) {
        var deposit = new Deposit();
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
