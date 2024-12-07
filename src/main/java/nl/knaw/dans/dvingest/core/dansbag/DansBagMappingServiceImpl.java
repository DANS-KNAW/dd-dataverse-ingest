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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.deposit.BagDirResolver;
import nl.knaw.dans.dvingest.core.dansbag.deposit.BagDirResolverImpl;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositFileLister;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositFileListerImpl;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositReader;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DepositReaderImpl;
import nl.knaw.dans.dvingest.core.dansbag.domain.DansBagDeposit;
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;
import nl.knaw.dans.dvingest.core.dansbag.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.dvingest.core.dansbag.service.BagDataManager;
import nl.knaw.dans.dvingest.core.dansbag.service.BagDataManagerImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelper;
import nl.knaw.dans.dvingest.core.dansbag.service.ManifestHelperImpl;
import nl.knaw.dans.dvingest.core.dansbag.service.XmlReader;
import nl.knaw.dans.dvingest.core.dansbag.service.XmlReaderImpl;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import nl.knaw.dans.lib.util.ZipUtil;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class DansBagMappingServiceImpl implements DansBagMappingService {
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");
    private static final SimpleDateFormat yyyymmddFormat = new SimpleDateFormat("YYYY-MM-dd");

    private final DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper;
    private final DataverseService dataverseService;
    private final DepositReader depositReader;
    private final SupportedLicenses supportedLicenses;
    private final Pattern fileExclusionPattern;
    private final List<String> embargoExclusions;

    public DansBagMappingServiceImpl(DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper, DataverseService dataverseService, SupportedLicenses supportedLicenses,
        Pattern fileExclusionPattern, List<String> embargoExclusions) {
        this.depositToDvDatasetMetadataMapper = depositToDvDatasetMetadataMapper;
        this.dataverseService = dataverseService;
        BagReader bagReader = new BagReader();
        ManifestHelper manifestHelper = new ManifestHelperImpl();
        DepositFileLister depositFileLister = new DepositFileListerImpl();
        BagDataManager bagDataManager = new BagDataManagerImpl(bagReader);
        XmlReader xmlReader = new XmlReaderImpl();
        BagDirResolver bagDirResolver = new BagDirResolverImpl();

        depositReader = new DepositReaderImpl(xmlReader, bagDirResolver, bagDataManager, depositFileLister, manifestHelper);
        this.supportedLicenses = supportedLicenses;
        this.fileExclusionPattern = fileExclusionPattern;
        this.embargoExclusions = embargoExclusions;
    }

    @Override
    public String getUpdatesDataset(Path depositDir) throws IOException, DataverseException {
        var dansDepositProperties = new DansDepositProperties(depositDir.resolve("deposit.properties"));
        try (var stream = Files.list(depositDir)) {
            var bag = stream.filter(Files::isDirectory)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No bag found in deposit"));
            var bagInfo = new LightweightBagInfo(bag.resolve("bag-info.txt"));
            var isVersionOf = bagInfo.get("Is-Version-Of");
            if (isVersionOf != null) {
                log.debug("Found Is-Version-Of in bag-info.txt, so this is an update-deposit: {}", isVersionOf);
                List<String> results;
                if (dansDepositProperties.getSwordToken() != null) {
                    log.debug("Found sword token in deposit.properties, looking for target dataset by sword token");
                    results = dataverseService.findDoiByMetadataField("dansSwordToken", dansDepositProperties.getSwordToken());
                }
                else if (depositToDvDatasetMetadataMapper.isMigration()) {
                    log.debug("This is a migration deposit, looking for target dataset by dansBagIt. Note that this will only work for two versions of the same dataset");
                    results = dataverseService.findDoiByMetadataField("dansBagId", isVersionOf);

                }
                else {
                    throw new IllegalArgumentException("Update deposit should have either a sword token or be a migration deposit");
                }
                if (results.size() == 1) {
                    return results.get(0);
                }
                else {
                    throw new IllegalArgumentException("Update deposit should update exactly one dataset, found " + results.size());
                }
            }
            else {
                log.debug("No Is-Version-Of found in bag-info.txt, so this is a deposit of a new dataset");
                return null;
            }
        }
    }

    @Override
    public Dataset getDatasetMetadataFromDansDeposit(DansBagDeposit dansDeposit) {
        var dataset = depositToDvDatasetMetadataMapper.toDataverseDataset(
            dansDeposit.getDdm(),
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

        // TODO: when processing an update-deposit, retrieve terms of access from the previous version
        if (!dansDeposit.allowAccessRequests() && StringUtils.isBlank(version.getTermsOfAccess())) {
            version.setTermsOfAccess("N/a");
        }
        version.setLicense(supportedLicenses.getLicenseFromDansDeposit(dansDeposit));
        return dataset;
    }

    @Override
    public EditFiles getEditFilesFromDansDeposit(DansBagDeposit dansDeposit) {
        return new EditFilesComposer(dansDeposit, fileExclusionPattern, embargoExclusions).composeEditFiles();
    }

    @Override
    public EditPermissions getEditPermissionsFromDansDeposit(DansBagDeposit dansDeposit) {
        var userId = dansDeposit.getDepositorUserId();
        var editPermissions = new EditPermissions();
        var roleAssignment = new RoleAssignment();
        roleAssignment.setAssignee("@" + userId);
        roleAssignment.setRole("contributorplus"); // TODO: make this configurable
        editPermissions.setAddRoleAssignments(List.of(roleAssignment));
        return editPermissions;
    }

    @Override
    public String packageOriginalMetadata(DansBagDeposit dansDeposit) throws IOException {
        // Zip the contents of the metadata directory of the bag
        var metadataDir = dansDeposit.getBagDir().resolve("metadata");
        var zipFile = dansDeposit.getBagDir().resolve("data/original-metadata.zip");
        ZipUtil.zipDirectory(metadataDir, zipFile, false);
        return zipFile.toString();
    }

    Optional<String> getDateOfDeposit(DansBagDeposit dansDeposit) {
        if (dansDeposit.isUpdate()) {
            return Optional.empty(); // See for implementation CIT025B in DatasetUpdater
        }
        else {
            return Optional.of(yyyymmddPattern.print(DateTime.now())); // CIT025B
        }
    }

    Optional<AuthenticatedUser> getDatasetContact(DansBagDeposit dansDeposit) {
        return Optional.ofNullable(dansDeposit.getDepositorUserId())
            .filter(StringUtils::isNotBlank)
            .map(userId -> dataverseService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Unable to fetch user with id " + userId)));
    }

    @Override
    public DansBagDeposit readDansDeposit(Path depositDir) throws InvalidDepositException {
        return depositReader.readDeposit(depositDir);
    }
}
