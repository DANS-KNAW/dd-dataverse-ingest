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
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDeposit;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDepositReader;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDepositReaderImpl;
import nl.knaw.dans.dvingest.core.dansbag.deposit.FileInfo;
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;
import nl.knaw.dans.dvingest.core.dansbag.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.Amd;
import nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.FileElement;
import nl.knaw.dans.dvingest.core.dansbag.xml.XPathEvaluator;
import nl.knaw.dans.dvingest.core.dansbag.xml.XmlReader;
import nl.knaw.dans.dvingest.core.dansbag.xml.XmlReaderImpl;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.Create;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.dvingest.core.yaml.Expect;
import nl.knaw.dans.dvingest.core.yaml.Expect.State;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.dvingest.core.yaml.PublishAction;
import nl.knaw.dans.dvingest.core.yaml.ReleaseMigratedAction;
import nl.knaw.dans.dvingest.core.yaml.UpdateAction;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import nl.knaw.dans.lib.dataverse.model.file.Checksum;
import nl.knaw.dans.lib.dataverse.model.file.DataFile;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import nl.knaw.dans.lib.util.ZipUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DansBagMappingServiceImpl implements DansBagMappingService {
    private static final String ORIGINAL_METADATA_ZIP = "original-metadata.zip";
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");

    private final DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper;
    private final DataverseService dataverseService;
    private final DansBagDepositReader dansBagDepositReader;
    private final SupportedLicenses supportedLicenses;
    private final Pattern fileExclusionPattern;
    private final List<String> embargoExclusions;
    private final String depositorRoleAutoIngest;
    private final String depositorRoleMigration;

    public DansBagMappingServiceImpl(DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper, DataverseService dataverseService, SupportedLicenses supportedLicenses,
        Pattern fileExclusionPattern, List<String> embargoExclusions, String depositorRoleAutoIngest, String depositorRoleMigration) {
        this.depositToDvDatasetMetadataMapper = depositToDvDatasetMetadataMapper;
        this.dataverseService = dataverseService;
        this.depositorRoleAutoIngest = depositorRoleAutoIngest;
        this.depositorRoleMigration = depositorRoleMigration;
        BagReader bagReader = new BagReader();
        XmlReader xmlReader = new XmlReaderImpl();

        dansBagDepositReader = new DansBagDepositReaderImpl(xmlReader, bagReader);
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

            if (isVersionOf == null) {
                log.debug("No Is-Version-Of found in bag-info.txt, so this is a deposit of a new dataset");
                return null;
            }

            log.debug("Found Is-Version-Of in bag-info.txt, so this is an update-deposit: {}", isVersionOf);
            List<String> results;

            if (dansDepositProperties.getSwordToken() != null) {
                log.debug("Found sword token in deposit.properties, looking for target dataset by sword token");
                results = dataverseService.findDoiByMetadataField("dansSwordToken", dansDepositProperties.getSwordToken());
            }
            else if (depositToDvDatasetMetadataMapper.isMigration()) {
                log.debug("This is a migration deposit, looking for target dataset by dansBagId");
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
    }

    @Override
    public Init getInitFromDansDeposit(DansBagDeposit dansDeposit, boolean isUpdate) {
        if (!isUpdate && depositToDvDatasetMetadataMapper.isMigration()) {
            if (StringUtils.isBlank(dansDeposit.getDoi())) {
                throw new IllegalArgumentException("Migration deposit must have a DOI");
            }
            var create = new Create();
            var doi = dansDeposit.getDoi();
            if (!doi.startsWith("doi:")) {
                doi = "doi:" + doi;
            }
            create.setImportPid(doi);
            var init = new Init();
            init.setCreate(create);
            return init;
        }
        else if (isUpdate) {
            var expect = new Expect();
            expect.setState(State.released);
            var init = new Init();
            init.setExpect(expect);
            return init;
        }
        return null;
    }

    @Override
    public Dataset getDatasetMetadataFromDansDeposit(DansBagDeposit dansDeposit, DatasetVersion currentMetadata) {
        // TODO: rename to DatasetComposer en push the terms stuff into it as well.
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

        // Allow access requests only if it was not forbidden in previous versions
        var fileAccessRequest = dansDeposit.allowAccessRequests() && (currentMetadata == null || currentMetadata.getFileAccessRequest());
        version.setFileAccessRequest(fileAccessRequest);
        if (StringUtils.isBlank(version.getTermsOfAccess()) && currentMetadata != null) {
            version.setTermsOfAccess(currentMetadata.getTermsOfAccess());
        }
        if (!fileAccessRequest && StringUtils.isBlank(version.getTermsOfAccess())) {
            // Dataverse requires terms of access to be set if file access requests are disabled, so set it to "N/a" if not explicitly provided
            version.setTermsOfAccess("N/a");
        }
        version.setLicense(supportedLicenses.getLicenseFromDansDeposit(dansDeposit));
        return dataset;
    }

    @Override
    public EditFiles getEditFilesFromDansDeposit(DansBagDeposit dansDeposit, String updatesDataset) {
        var files = getFileInfo(dansDeposit);
        if (!depositToDvDatasetMetadataMapper.isMigration()) {
            try {
                files.put(Path.of(ORIGINAL_METADATA_ZIP), createOriginalMetadataFileInfo(dansDeposit));
            }
            catch (IOException e) {
                throw new RuntimeException("Error creating original metadata zip", e);
            }
        }
        var dateAvailable = getDateAvailable(dansDeposit);
        if (updatesDataset == null) {
            return new EditFilesComposer(files, dateAvailable, fileExclusionPattern, embargoExclusions).composeEditFiles();
        }
        else {
            return new EditFilesComposerForUpdate(files, dateAvailable, updatesDataset, fileExclusionPattern, embargoExclusions, dataverseService).composeEditFiles();
        }
    }

    private FileInfo createOriginalMetadataFileInfo(DansBagDeposit dansDeposit) throws IOException {
        var metadataDir = dansDeposit.getBagDir().resolve("metadata");
        var zipFile = dansDeposit.getBagDir().resolve("data/" + ORIGINAL_METADATA_ZIP);
        ZipUtil.zipDirectory(metadataDir, zipFile, false);
        var checksum = DigestUtils.sha1Hex(new FileInputStream(zipFile.toFile()));
        var fileMeta = new FileMeta();
        fileMeta.setLabel(ORIGINAL_METADATA_ZIP);
        var dataFile = new DataFile();
        dataFile.setFilename(ORIGINAL_METADATA_ZIP);
        var dfChecksum = new Checksum();
        dfChecksum.setType("SHA-1");
        dfChecksum.setValue(checksum);
        dataFile.setChecksum(dfChecksum);
        fileMeta.setDataFile(dataFile);
        fileMeta.setRestrict(false);
        return new FileInfo(zipFile, checksum, false, fileMeta);
    }

    @Override
    public EditPermissions getEditPermissionsFromDansDeposit(DansBagDeposit dansDeposit, boolean isUpdate) {
        if (isUpdate) {
            log.debug("Not changing permissions for update deposit");
            return new EditPermissions();
        }
        var userId = dansDeposit.getDepositorUserId();
        var editPermissions = new EditPermissions();
        var roleAssignment = new RoleAssignment();
        roleAssignment.setAssignee("@" + userId);
        String role = depositToDvDatasetMetadataMapper.isMigration() ? depositorRoleMigration : depositorRoleAutoIngest;
        log.debug("Setting role for {} to {}", userId, role);
        roleAssignment.setRole(role);
        editPermissions.setAddRoleAssignments(List.of(roleAssignment));
        return editPermissions;
    }

    @Override
    public UpdateAction getUpdateActionFromDansDeposit(DansBagDeposit dansDeposit) {
        if (depositToDvDatasetMetadataMapper.isMigration()) {
            var amd = dansDeposit.getAmd();

            if (amd == null) {
                throw new RuntimeException(String.format("no AMD found for %s", dansDeposit.getDoi()));
            }

            var date = Amd.toPublicationDate(amd);

            if (date.isEmpty()) {
                throw new IllegalArgumentException(String.format("no publication date found in AMD for %s", dansDeposit.getDoi()));
            }

            return new ReleaseMigratedAction(date.get());
        }
        else {
            return new PublishAction(UpdateType.major);
        }
    }

    // todo: move to mapping package
    private Map<Path, FileInfo> getFileInfo(DansBagDeposit dansDeposit) {
        var files = FileElement.pathToFileInfo(dansDeposit, false); // TODO: handle migration case

        return files.entrySet().stream()
            .map(entry -> {
                // relativize the path
                var bagPath = entry.getKey();
                var fileInfo = entry.getValue();
                var newKey = Path.of("data").relativize(bagPath);

                return Map.entry(newKey, fileInfo);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // todo: move to mapping package
    private Instant getDateAvailable(DansBagDeposit dansBagDeposit) {
        return XPathEvaluator.strings(dansBagDeposit.getDdm(), "/ddm:DDM/ddm:profile/ddm:available")
            .map(DansBagMappingServiceImpl::parseDate)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Deposit without a ddm:available element"));
    }

    // todo: move to util class
    private static Instant parseDate(String value) {
        try {
            log.debug("Trying to parse {} as LocalDate", value);
            return LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        catch (DateTimeParseException e) {
            try {
                log.debug("Trying to parse {} as ZonedDateTime", value);
                return ZonedDateTime.parse(value).toInstant();
            }
            catch (DateTimeParseException ee) {
                log.debug("Trying to parse {} as LocalDateTime", value);
                var id = ZoneId.systemDefault().getRules().getOffset(Instant.now());
                return LocalDateTime.parse(value).toInstant(id);
            }
        }
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
        return dansBagDepositReader.readDeposit(depositDir);
    }
}
