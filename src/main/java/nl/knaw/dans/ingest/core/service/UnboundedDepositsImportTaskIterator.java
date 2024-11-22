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

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class UnboundedDepositsImportTaskIterator extends AbstractDepositsImportTaskIterator {
    private static final Logger log = LoggerFactory.getLogger(UnboundedDepositsImportTaskIterator.class);
    private final Set<Path> initialPathsRead = new HashSet<>();
    private boolean initialized = false;
    private boolean keepRunning = true;

    public UnboundedDepositsImportTaskIterator(Path inboxDir, Path outBox, int pollingInterval, DepositIngestTaskFactory taskFactory, EventWriter eventWriter) {
        super(inboxDir, outBox, taskFactory, eventWriter);
        var observer = new FileAlterationObserver(inboxDir.toFile(), f -> f.isDirectory() && f.getParentFile().equals(inboxDir.toFile()));
        observer.addListener(new EventHandler());
        var monitor = new FileAlterationMonitor(pollingInterval);
        monitor.addObserver(observer);

        try {
            log.debug("Starting FileAlterationMonitor for directory {}", inboxDir);
            monitor.start();
        }
        catch (Exception e) {
            throw new IllegalStateException(String.format("Could not start monitoring %s", inboxDir), e);
        }
    }

    @Override
    public boolean hasNext() {
        // Assuming that eventually a new item will arrive, unless we are explicitly stopping watching the inbox
        return keepRunning;
    }

    public void stop() {
        keepRunning = false;
    }

    // onStart is called before any other callback methods are called, so we can safely assume
    // that while the onStart method is still running, the onDirectoryCreate method will not be called.
    // The reason this logic is still inside this adaptor is that there might just be a new deposit
    // between reading the directory and starting the monitor, so we switch it around. First we start
    // the monitor, then on the first run the existing paths are processed.
    private class EventHandler extends FileAlterationListenerAdaptor {

        // this is called every time the monitor runs (currently every half second), so only check existing files on the first run
        @Override
        public void onStart(FileAlterationObserver observer) {
            log.trace("onStart called");

            if (initialized) {
                log.trace("onStart EventHandler is already initialized, returning");
                return;
            }

            initialized = true;

            // find all deposits
            var initialTasks = createDepositIngestTasks(getAllDepositPathsFromInbox());

            for (var task : initialTasks) {
                var path = task.getDepositPath();
                log.debug("onStart initial deposit found: {}", path);
                initialPathsRead.add(path);
                addTask(task);
            }

            log.trace("onStart finished");
        }

        @Override
        public void onDirectoryCreate(File file) {
            log.trace("onDirectoryCreate: {}", file);
            var path = file.toPath();

            // This should only happen if a new deposit was made after the monitor was initialized but before the first
            // call to onStart was made. In that case, we ignore it as the onStart method already processed it.
            if (initialPathsRead.contains(path)) {
                log.warn("onDirectoryCreate called with path that was also read during startup: {}", path);
            }
            else {
                addTaskForDeposit(file.toPath());
            }
        }

        @Override
        public void onStop(FileAlterationObserver observer) {
            log.trace("onStop");

            // after the first initial scan of the inbox, all paths will have been processed, and we don't need to check if they are duplicates
            if (initialPathsRead.size() > 0) {
                initialPathsRead.clear();
            }
        }
    }
}
