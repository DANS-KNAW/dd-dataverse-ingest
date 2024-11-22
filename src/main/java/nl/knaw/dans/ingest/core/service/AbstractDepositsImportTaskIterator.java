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
package nl.knaw.dans.ingest.core.service;

import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractDepositsImportTaskIterator implements Iterator<DepositIngestTask> {
    private static final Logger log = LoggerFactory.getLogger(AbstractDepositsImportTaskIterator.class);
    private final LinkedBlockingDeque<DepositIngestTask> deque = new LinkedBlockingDeque<>();
    private final Path inboxDir;
    private final Path outBox;
    private final DepositIngestTaskFactory taskFactory;
    private final EventWriter eventWriter;

    public AbstractDepositsImportTaskIterator(
        Path inboxDir, Path outBox, DepositIngestTaskFactory taskFactory, EventWriter eventWriter) {
        this.inboxDir = inboxDir;
        this.outBox = outBox;
        this.taskFactory = taskFactory;
        this.eventWriter = eventWriter;
    }

    protected List<Path> getAllDepositPathsFromInbox() {
        try (Stream<Path> paths = Files.list(inboxDir).filter(Files::isDirectory)) {
            var pathList = paths.collect(Collectors.toList());
            log.debug("Found {} deposits", pathList.size());
            return pathList;
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read deposits from inbox", e);
        }
    }

    protected List<DepositIngestTask> createDepositIngestTasks(List<Path> depositPaths) {
        var tasks = new LinkedList<DepositIngestTask>();
        for (var p : depositPaths) {
            try {
                tasks.add(taskFactory.createIngestTask(p, outBox, eventWriter));
            }
            catch (InvalidDepositException | IOException e) {
                throw new IllegalArgumentException("Could not create task for deposit", e);
            }
        }
        return tasks.stream().sorted().collect(Collectors.toList());
    }

    protected void addTaskForDeposit(Path dir) {
        try {
            addTask(taskFactory.createIngestTask(dir, outBox, eventWriter));
        }
        catch (IOException | InvalidDepositException e) {
            log.error("Error while creating task", e);
        }
    }

    protected void addTask(DepositIngestTask task) {
        if (log.isDebugEnabled()) {
//            log.debug("Adding task for {}", task.getDeposit().getDir().getFileName());
            // TODO add debug statement
        }
        deque.add(task);
    }

    @Override
    public boolean hasNext() {
        return deque.peekFirst() != null;
    }

    @Override
    public DepositIngestTask next() {
        try {
            return deque.take();
        }
        catch (InterruptedException e) {
            log.warn("Deque threw error", e);
        }
        return null;
    }
}
