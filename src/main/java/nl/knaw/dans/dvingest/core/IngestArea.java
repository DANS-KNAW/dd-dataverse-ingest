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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.api.ImportCommandDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto;
import nl.knaw.dans.dvingest.api.ImportJobStatusDto.StatusEnum;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
public class IngestArea {
    @NonNull
    private final ExecutorService executorService;
    @NonNull
    private final ImportJobFactory importJobFactory;
    @NonNull
    private final Path inbox;
    @NonNull
    protected final Path outbox;

    private final Map<String, ImportJob> importJobs = new ConcurrentHashMap<>();

    public IngestArea(ImportJobFactory importJobFactory, Path inbox, Path outbox, ExecutorService executorService) {
        try {
            this.importJobFactory = importJobFactory;
            this.inbox = inbox.toAbsolutePath().toRealPath();
            this.outbox = outbox.toAbsolutePath().toRealPath();
            this.executorService = executorService;
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to create ingest area", e);
        }
    }

    public void submit(ImportCommandDto importCommand) {
        log.debug("Received import command: {}", importCommand);
        var existingJob = importJobs.get(importCommand.getPath());
        if (existingJob != null && !List.of(StatusEnum.FAILED, StatusEnum.DONE).contains(existingJob.getStatus().getStatus())) {
            throw new IllegalArgumentException("Already submitted and still pending or running " + importCommand.getPath());
        }
        validatePath(importCommand.getPath());
        log.debug("Path validation successful");
        var importJob = createImportJob(importCommand);
        log.debug("Created import job: {}", importJob);
        importJobs.put(importCommand.getPath(), importJob);
        log.debug("Submitted import job");
        executorService.submit(importJob);
    }

    public List<ImportJobStatusDto> getStatus(String path) {
        if (path == null) {
            return importJobs.values().stream().map(ImportJob::getStatus).toList();
        }
        else {
            if (importJobs.get(path) == null) {
                throw new IllegalArgumentException("No job found for path: " + path);
            }
            return List.of(importJobs.get(path).getStatus());
        }
    }

    private ImportJob createImportJob(ImportCommandDto importCommand) {
        Path relativePath;
        if (importCommand.getSingleObject()) {
            relativePath = inbox.relativize(Path.of(importCommand.getPath()).getParent());
        }
        else {
            relativePath = inbox.relativize(Path.of(importCommand.getPath()));
        }
        return importJobFactory.createImportJob(importCommand, outbox.resolve(relativePath), importCommand.getOnlyConvertDansBag());
    }

    private void validatePath(String path) {
        var pathObj = Path.of(path);
        checkPathIsAbsolute(pathObj);
        checkPathInInbox(pathObj);
    }

    private void checkPathIsAbsolute(Path path) {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be absolute: " + path);
        }
    }

    private void checkPathInInbox(Path path) {
        if (!path.startsWith(inbox)) {
            throw new IllegalArgumentException("Path must be in inbox: " + path);
        }
    }
}
