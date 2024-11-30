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
import nl.knaw.dans.dvingest.core.bagprocessor.DataversePath;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.yaml.AddEmbargo;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.dvingest.core.yaml.FromTo;
import nl.knaw.dans.ingest.core.deposit.BagDirResolver;
import nl.knaw.dans.ingest.core.deposit.BagDirResolverImpl;
import nl.knaw.dans.ingest.core.deposit.DepositFileLister;
import nl.knaw.dans.ingest.core.deposit.DepositFileListerImpl;
import nl.knaw.dans.ingest.core.deposit.DepositReader;
import nl.knaw.dans.ingest.core.deposit.DepositReaderImpl;
import nl.knaw.dans.ingest.core.deposit.DepositWriter;
import nl.knaw.dans.ingest.core.deposit.DepositWriterImpl;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.DepositState;
import nl.knaw.dans.ingest.core.domain.FileInfo;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.io.BagDataManager;
import nl.knaw.dans.ingest.core.io.BagDataManagerImpl;
import nl.knaw.dans.ingest.core.io.FileService;
import nl.knaw.dans.ingest.core.io.FileServiceImpl;
import nl.knaw.dans.ingest.core.service.ManifestHelper;
import nl.knaw.dans.ingest.core.service.ManifestHelperImpl;
import nl.knaw.dans.ingest.core.service.XPathEvaluator;
import nl.knaw.dans.ingest.core.service.XmlReader;
import nl.knaw.dans.ingest.core.service.XmlReaderImpl;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.ingest.core.service.mapper.mapping.FileElement;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DansBagMappingServiceImpl implements DansBagMappingService {
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");
    private static final SimpleDateFormat yyyymmddFormat = new SimpleDateFormat("YYYY-MM-dd");

    private final DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper;
    private final DataverseService datasetService;
    private final DepositReader depositReader;
    private final DepositWriter depositWriter;
    private final SupportedLicenses supportedLicenses;
    private final Pattern fileExclusionPattern;
    private final List<String> embargoExclusions;

    public DansBagMappingServiceImpl(DepositToDvDatasetMetadataMapper depositToDvDatasetMetadataMapper, DataverseService dataverseService, SupportedLicenses supportedLicenses,
        Pattern fileExclusionPattern, List<String> embargoExclusions) {
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
        depositWriter = new DepositWriterImpl(bagDataManager);
        this.supportedLicenses = supportedLicenses;
        this.fileExclusionPattern = fileExclusionPattern;
        this.embargoExclusions = embargoExclusions;
    }

    @Override
    public Dataset getDatasetMetadataFromDansDeposit(Deposit dansDeposit) {
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
    public EditFiles getEditFilesFromDansDeposit(Deposit dansDeposit) {
        var editFiles = new EditFiles();

        var pathFileInfoMap = getFileInfo(dansDeposit);

        // TODO: in update also ignore any files that have not changed (content or metadata)
        var ignoredFiles = getIgnoredFiles(pathFileInfoMap).stream().map(Path::toString).toList();
        editFiles.setIgnoreFiles(ignoredFiles);

        pathFileInfoMap = removeIgnoredFiles(pathFileInfoMap, ignoredFiles);

        editFiles.setRenameAtUploadFiles(getRenameAtUpload(pathFileInfoMap));

        editFiles.setAddRestrictedFiles(pathFileInfoMap.entrySet().stream()
            .filter(entry -> entry.getValue().getMetadata().getRestricted())
            .map(Map.Entry::getKey)
            .map(Path::toString).toList());

        editFiles.setUpdateFileMetas(pathFileInfoMap.values().stream()
            .map(FileInfo::getMetadata)
            .filter(this::hasAttributes)
            .toList());

        var dateAvailable = getDateAvailable(dansDeposit);
        var filePathsToEmbargo = getEmbargoedFiles(pathFileInfoMap, dateAvailable);
        if (!filePathsToEmbargo.isEmpty()) {
            var addEmbargo = new AddEmbargo();
            addEmbargo.setDateAvailable(yyyymmddFormat.format(Date.from(dateAvailable)));
            addEmbargo.setFilePaths(filePathsToEmbargo.stream().map(Path::toString).toList());
            editFiles.setAddEmbargoes(List.of(addEmbargo));
        }
        return editFiles;
    }

    private List<FromTo> getRenameAtUpload(Map<Path, FileInfo> files) {
        ArrayList<FromTo> fromTos = new ArrayList<>();
        for (var entry : files.entrySet()) {
            if (entry.getValue().isSanitized()) {
                var from = entry.getKey().toString();
                var to = new DataversePath(entry.getValue().getMetadata().getDirectoryLabel(), entry.getValue().getMetadata().getLabel()).toString();
                fromTos.add(new FromTo(from, to));
            }
        }
        return fromTos;
    }

    private List<Path> getIgnoredFiles(Map<Path, FileInfo> files) {
        if (fileExclusionPattern == null) {
            return List.of();
        }
        return files.keySet().stream()
            .filter(f -> fileExclusionPattern.matcher(f.toString()).matches()).toList();
    }

    private Map<Path, FileInfo> removeIgnoredFiles(Map<Path, FileInfo> files, List<String> ignoredFiles) {
        return files.entrySet().stream()
            .filter(entry -> !ignoredFiles.contains(entry.getKey().toString()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<Path> getEmbargoedFiles(Map<Path, FileInfo> files, Instant dateAvailable) {
        var now = Instant.now();
        if (dateAvailable.isAfter(now)) {
            return files.keySet().stream()
                .filter(f -> !embargoExclusions.contains(f.toString())).toList();
        }
        else {
            log.debug("Date available in the past, no embargo: {}", dateAvailable);
            return List.of();
        }
    }

    private Instant getDateAvailable(Deposit deposit) {
        return XPathEvaluator.strings(deposit.getDdm(), "/ddm:DDM/ddm:profile/ddm:available")
            .map(DansBagMappingServiceImpl::parseDate)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Deposit without a ddm:available element"));
    }

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

    @Override
    public EditPermissions getEditPermissionsFromDansDeposit(Deposit dansDeposit) {
        var userId = dansDeposit.getDepositorUserId();
        var editPermissions = new EditPermissions();
        var roleAssignment = new RoleAssignment();
        roleAssignment.setAssignee("@" + userId);
        roleAssignment.setRole("contributorplus"); // TODO: make this configurable
        editPermissions.setAddRoleAssignments(List.of(roleAssignment));
        return editPermissions;
    }

    @Override
    public void updateDepositStatus(Deposit deposit, DepositState state) {
        try {
            deposit.setState(state);
            depositWriter.saveDeposit(deposit);
        }
        catch (InvalidDepositException e) {
            throw new RuntimeException("Failed to update deposit status", e);
        }
    }

    private boolean hasAttributes(FileMeta fileMeta) {
        return (fileMeta.getCategories() != null && !fileMeta.getCategories().isEmpty()) ||
            (fileMeta.getDescription() != null && !fileMeta.getDescription().isBlank());
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
