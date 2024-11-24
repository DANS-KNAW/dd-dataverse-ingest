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

import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.ingest.core.deposit.BagDirResolver;
import nl.knaw.dans.ingest.core.deposit.BagDirResolverImpl;
import nl.knaw.dans.ingest.core.deposit.DepositFileLister;
import nl.knaw.dans.ingest.core.deposit.DepositFileListerImpl;
import nl.knaw.dans.ingest.core.deposit.DepositReader;
import nl.knaw.dans.ingest.core.deposit.DepositReaderImpl;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.FileInfo;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.io.BagDataManager;
import nl.knaw.dans.ingest.core.io.BagDataManagerImpl;
import nl.knaw.dans.ingest.core.io.FileService;
import nl.knaw.dans.ingest.core.io.FileServiceImpl;
import nl.knaw.dans.ingest.core.service.ManifestHelper;
import nl.knaw.dans.ingest.core.service.ManifestHelperImpl;
import nl.knaw.dans.ingest.core.service.XmlReader;
import nl.knaw.dans.ingest.core.service.XmlReaderImpl;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.ingest.core.service.mapper.mapping.FileElement;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DansBagMappingServiceImpl implements DansBagMappingService {
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");

    private final DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper;
    private final DataverseService datasetService;
    private final DepositReader depositReader;
    private final SupportedLicenses supportedLicenses;
    private final Pattern fileExclusionPattern;

    public DansBagMappingServiceImpl(DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper, DataverseService dataverseService, SupportedLicenses supportedLicenses,
        Pattern fileExclusionPattern) {
        this.depositToDvDatasetMetadataMapper = depositToDvDatasetMetadataMapper;
        this.datasetService = dataverseService;
        BagReader bagReader = new BagReader();
        ManifestHelper manifestHelper = new ManifestHelperImpl();
        DepositFileLister depositFileLister = new DepositFileListerImpl();
        BagDataManager bagDataManager = new BagDataManagerImpl(bagReader);
        XmlReader xmlReader = new XmlReaderImpl();
        FileService fileService = new FileServiceImpl();
        BagDirResolver bagDirResolver = new BagDirResolverImpl(fileService);

        depositReader = new DepositReaderImpl(xmlReader, bagDirResolver, fileService, bagDataManager, depositFileLister, manifestHelper);
        this.supportedLicenses = supportedLicenses;
        this.fileExclusionPattern = fileExclusionPattern;
    }

    @Override
    public Dataset getDatasetMetadataFromDansDeposit(Deposit dansDeposit) {
        var dataset = depositToDvDatasetMetadataMapper.toDataverseDataset(dansDeposit.getDdm(),
            dansDeposit.getOtherDoiId(),
            getDateOfDeposit(dansDeposit).orElse(null),
            getDatasetContact(dansDeposit).orElse(null), // But null is never actually used, because an exception is thrown if contact is not found
            dansDeposit.getVaultMetadata(),
            dansDeposit.getDepositorUserId(),
            dansDeposit.restrictedFilesPresent(),
            dansDeposit.getHasOrganizationalIdentifier(),
            dansDeposit.getHasOrganizationalIdentifierVersion());
        var version = dataset.getDatasetVersion();
        version.setFileAccessRequest(dansDeposit.allowAccessRequests());

        // TODO: when processing an update-deposit, retriever terms of access from the previous version
        if (!dansDeposit.allowAccessRequests() && StringUtils.isBlank(version.getTermsOfAccess())) {
            version.setTermsOfAccess("N/a");
        }
        version.setLicense(supportedLicenses.getLicenseFromDansDeposit(dansDeposit));
        return dataset;
    }

    @Override
    public EditFiles getEditFilesFromDansDeposit(Deposit dansDeposit) {
        var editFiles = new EditFiles();

        var pathFileInfoMap = getFileInfo(dansDeposit);

        editFiles.setAddRestrictedFiles(pathFileInfoMap.entrySet().stream()
            .filter(entry -> entry.getValue().getMetadata().getRestricted())
            .map(Map.Entry::getKey)
            .map(Path::toString).toList());

        return editFiles;
    }

    Map<Path, FileInfo> getFileInfo(Deposit dansDeposit) {
        var files = FileElement.pathToFileInfo(dansDeposit, false); // TODO: handle migration case

        return files.entrySet().stream()
            .map(entry -> {
                // relativize the path
                var bagPath = entry.getKey();
                var fileInfo = entry.getValue();
                var newKey = Path.of("data").relativize(bagPath);

                return Map.entry(newKey, fileInfo);
            })
            .filter(entry -> {
                // remove entries that match the file exclusion pattern
                var path = entry.getKey().toString();

                return (fileExclusionPattern == null || !fileExclusionPattern.matcher(path).matches());
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Optional<String> getDateOfDeposit(Deposit dansDeposit) {
        if (dansDeposit.isUpdate()) {
            return Optional.empty(); // See for implementation CIT025B in DatasetUpdater
        }
        else {
            return Optional.of(yyyymmddPattern.print(DateTime.now())); // CIT025B
        }
    }

    Optional<AuthenticatedUser> getDatasetContact(Deposit dansDeposit) {
        return Optional.ofNullable(dansDeposit.getDepositorUserId())
            .filter(StringUtils::isNotBlank)
            .map(userId -> datasetService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Unable to fetch user with id " + userId)));
    }

    @Override
    public Deposit readDansDeposit(Path depositDir) throws InvalidDepositException {
        return depositReader.readDeposit(depositDir);
    }
}
