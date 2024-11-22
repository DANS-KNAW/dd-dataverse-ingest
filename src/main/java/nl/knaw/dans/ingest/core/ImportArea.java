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
package nl.knaw.dans.ingest.core;

import nl.knaw.dans.ingest.core.service.DepositIngestTask;
import nl.knaw.dans.ingest.core.service.DepositIngestTaskFactory;
import nl.knaw.dans.ingest.core.service.EnqueuingService;
import nl.knaw.dans.ingest.core.service.SingleDepositTargetedTaskSourceImpl;
import nl.knaw.dans.ingest.core.service.TargetedTaskSource;
import nl.knaw.dans.ingest.core.service.TargetedTaskSourceImpl;
import nl.knaw.dans.ingest.core.service.TaskEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class ImportArea extends AbstractIngestArea {
    private static final Logger log = LoggerFactory.getLogger(ImportArea.class);

    public ImportArea(Path inboxDir, Path outboxDir, DepositIngestTaskFactory taskFactory, TaskEventService taskEventService, EnqueuingService enqueuingService) {
        super(inboxDir, outboxDir, taskFactory, taskEventService, enqueuingService);
    }

    public String startImport(Path inputPath, boolean isBatch, boolean continuePrevious) {
        log.trace("startBatch({}, {})", inputPath, continuePrevious);
        Path relativeInputDir;
        if (inputPath.isAbsolute()) {
            relativeInputDir = inboxDir.relativize(inputPath);
            if (relativeInputDir.startsWith(Paths.get(".."))) {
                throw new IllegalArgumentException(
                    String.format("Input directory must be subdirectory of %s. Provide correct absolute path or a path relative to this directory.", inboxDir));
            }
        }
        else {
            relativeInputDir = inputPath;
        }

        Path batchInDir;
        Path batchOutDir;

        if (isBatch) {
            batchInDir = inboxDir.resolve(relativeInputDir);
            batchOutDir = outboxDir.resolve(relativeInputDir);
            validateBatchDirectory(batchInDir);
        }
        else {
            validateDepositDirectory(inboxDir.resolve(relativeInputDir));
            batchInDir = inboxDir.resolve(relativeInputDir.getParent());
            batchOutDir = outboxDir.resolve(relativeInputDir.getParent());
        }
        log.debug("relativeInputDir = {}, batchInDir = {}, batchOutDir = {}", relativeInputDir, batchInDir, batchOutDir);

        validateInDir(batchInDir);
        initOutbox(batchOutDir, continuePrevious || !isBatch);
        String taskName = relativeInputDir.toString();
        TargetedTaskSource<DepositIngestTask> taskSource;
        if (isBatch) {
            taskSource = new TargetedTaskSourceImpl(taskName, batchInDir, batchOutDir, taskEventService, taskFactory);
        }
        else {
            taskSource = new SingleDepositTargetedTaskSourceImpl(taskName, inboxDir.resolve(relativeInputDir), batchOutDir, taskEventService, taskFactory);
        }
        enqueuingService.executeEnqueue(taskSource);
        return relativeInputDir.toString();
    }

    private void validateBatchDirectory(Path input) {
        if (Files.isDirectory(input)) {
            try (Stream<Path> subPaths = Files.list(input)) {
                List<Path> paths = subPaths.toList();
                for (Path f : paths) {
                    validateDepositDirectory(f);
                }
            }
            catch (IOException e) {
                throw new IllegalArgumentException(String.format("Cannot read %s", input));
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Invalid batch: %s. At least one non-deposit: %s", input, e.getMessage()));
            }
        }
        else {
            throw new IllegalArgumentException(String.format("File %s is not a directory. Cannot be a batch.", input));
        }
    }

    private void validateDepositDirectory(Path input) {
        if (!Files.isRegularFile(input.resolve("deposit.properties"))) {
            throw new IllegalArgumentException(String.format("Directory %s does not contain file deposit.properties. Not a valid deposit directory", input));
        }
    }

    public Path getSecurePath(Path path) throws RuntimeException {
        Path normalizedPath = path.normalize().toAbsolutePath();
        if (!normalizedPath.startsWith(this.inboxDir)) {
            throw new IllegalArgumentException(String.format("InsecurePath %s", normalizedPath));
        }
        return normalizedPath;
    }
}
