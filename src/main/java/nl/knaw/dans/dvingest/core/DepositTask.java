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
package nl.knaw.dans.dvingest.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.PathIterator;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Slf4j
public class DepositTask implements Runnable {
    public enum Status {
        TODO,
        SUCCESS,
        REJECTED,
        FAILED
    }

    protected final Deposit deposit;
    protected final DataverseService dataverseService;
    protected final UtilityServices utilityServices;
    protected final Path outputDir;

    @Getter
    protected Status status = Status.TODO;

    public DepositTask(Deposit deposit, DataverseService dataverseService, UtilityServices utilityServices, Path outputDir) {
        this.deposit = deposit;
        this.dataverseService = dataverseService;
        this.utilityServices = utilityServices;
        this.outputDir = outputDir;
    }

    @Override
    public void run() {
        try {
            String pid = deposit.getUpdatesDataset();
            for (DepositBag bag : deposit.getBags()) {
                log.info("START processing deposit / bag: {} / {}", deposit.getId(), bag);
                pid = processBag(bag, pid);
                log.info("END processing deposit / bag: {} / {}", deposit.getId(), bag);
            }
            deposit.moveTo(outputDir.resolve("processed"));
        }
        catch (Exception e) {
            try {
                log.error("Failed to ingest deposit", e);
                deposit.moveTo(outputDir.resolve("failed"));
                status = Status.FAILED;
            }
            catch (IOException ioException) {
                log.error("Failed to move deposit to failed directory", ioException);
            }
        }
    }

    private String processBag(DepositBag bag, String targetPid) throws IOException, DataverseException {
        if (targetPid == null) {
            targetPid = createNewDataset(bag);
        }
        else {
            updateMetadata(bag, targetPid);
        }
        processEdit(bag, targetPid);
        addFiles(bag, targetPid);
        publishVersion(targetPid);
        return targetPid;
    }

    private void processEdit(DepositBag bag, String pid) throws IOException, DataverseException {
        var edit = bag.getEditInstructions();
        if (edit == null) {
            log.debug("No edit instructions found. Skipping edit processing.");
            return;
        }
        log.debug("Start processing edit instructions for deposit {}", deposit.getId());
        deleteFiles(bag, pid, edit);
        replaceFiles(bag, pid, edit);
    }

    private String createNewDataset(DepositBag bag) throws IOException, DataverseException {
        log.debug("Creating new dataset");
        var result = dataverseService.createDataset(bag.getDatasetMetadata());
        var pid = result.getData().getPersistentId();
        log.debug(result.getEnvelopeAsString());
        return pid;
    }

    private void updateMetadata(DepositBag bag, String pid) throws IOException, DataverseException {
        log.debug("Start updating dataset metadata for deposit {}", deposit.getId());
        dataverseService.updateMetadata(pid, bag.getDatasetMetadata().getDatasetVersion());
        log.debug("End updating dataset metadata for deposit {}", deposit.getId());
    }

    private void deleteFiles(DepositBag bag, String pid, Edit edit) throws IOException, DataverseException {
        log.debug("Start deleting files for deposit {}", deposit.getId());
        for (var file : edit.getDeleteFiles()) {
            log.debug("Deleting file: {}", file);
            dataverseService.deleteFile(pid, file);
        }
        log.debug("End deleting files for deposit {}", deposit.getId());
    }

    private void replaceFiles(DepositBag bag, String pid, Edit edit) throws IOException, DataverseException {
        log.debug("Start replacing files for deposit {}", deposit.getId());
        for (var file : edit.getReplaceFiles()) {
            log.debug("Replacing file: {}", file);
            dataverseService.replaceFile(pid, file, bag.getDataDir().resolve(file));
        }
        log.debug("End replacing files for deposit {}", deposit.getId());
    }

    private void addFiles(DepositBag bag, String pid) throws IOException, DataverseException {
        var edit = bag.getEditInstructions();
        var iterator = new PathIterator(getFilesToUpload(bag, edit));
        while (iterator.hasNext()) {
            uploadFileBatch(iterator, bag.getDataDir(), pid);
        }
        log.debug("End uploading files for deposit {}", deposit.getId());
    }

    private Iterator<File> getFilesToUpload(DepositBag bag, Edit edit) {
        return IteratorUtils.filteredIterator(
            FileUtils.iterateFiles(bag.getDataDir().toFile(), null, true),
            // Skip files that have been replaced in the edit steps
            path -> edit == null || !edit.getReplaceFiles().contains(bag.getDataDir().relativize(path.toPath()).toString()));
    }

    private void uploadFileBatch(PathIterator iterator, Path dataDir, String pid) throws IOException, DataverseException {
        var tempZipFile = utilityServices.createTempZipFile();
        try {
            var zipFile = utilityServices.createPathIteratorZipperBuilder()
                .rootDir(dataDir)
                .sourceIterator(iterator)
                .targetZipFile(tempZipFile)
                .build()
                .zip();
            dataverseService.addFile(pid, zipFile, new FileMeta());
            log.debug("Uploaded {} files (cumulative)", iterator.getIteratedCount());
        }
        finally {
            Files.deleteIfExists(tempZipFile);
        }
    }

    private void publishVersion(String pid) throws DataverseException, IOException {
        log.debug("Start publishing version for deposit {}", deposit.getId());
        dataverseService.publishDataset(pid);
        dataverseService.waitForState(pid, "RELEASED");
        log.debug("End publishing version for deposit {}", deposit.getId());
    }
}