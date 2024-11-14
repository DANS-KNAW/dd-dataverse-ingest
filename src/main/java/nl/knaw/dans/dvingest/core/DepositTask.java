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
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                pid = processBag(bag, pid);
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
        log.info("Processing bag: {}", bag);
        if (targetPid == null) {
            targetPid = createNewDataset(bag);
        }
        else {
            updateMetadata(bag, targetPid);
        }
        deleteFiles(bag, targetPid);
        addFiles(bag, targetPid);
        publishVersion(targetPid);
        return targetPid;
    }

    private String createNewDataset(DepositBag bag) throws IOException, DataverseException {
        var result = dataverseService.createDataset(bag.getDatasetMetadata());
        var pid = result.getData().getPersistentId();
        log.debug(result.getEnvelopeAsString());
        return pid;
    }

    private void updateMetadata(DepositBag bag, String pid) throws IOException, DataverseException {
        dataverseService.updateMetadata(pid, bag.getDatasetMetadata().getDatasetVersion());
    }

    private void deleteFiles(DepositBag bag, String pid) throws IOException, DataverseException {
        //        var files = bag.getFiles();
        //        for (var file : files) {
        //            dataverseService.deleteFile(pid, file.getDataFile().getId());
        //        }
    }

    private void addFiles(DepositBag bag, String pid) throws IOException, DataverseException {
        var iterator = new PathIterator(FileUtils.iterateFiles(bag.getDataDir().toFile(), null, true));
        while (iterator.hasNext()) {
            uploadFileBatch(iterator, bag.getDataDir(), pid);
        }
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
        dataverseService.publishDataset(pid);
        dataverseService.waitForState(pid, "RELEASED");
    }
}