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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Iterator;

public class TargetedTaskSourceImpl implements TargetedTaskSource<DepositIngestTask> {
    private static final Logger log = LoggerFactory.getLogger(TargetedTaskSourceImpl.class);

    private final String name;
    private final Path inDir;
    private final Path outDir;
    private final EventWriter eventWriter;
    private final DepositIngestTaskFactory taskFactory;

    public TargetedTaskSourceImpl(String name, Path inDir, Path outDir, TaskEventService taskEventService, DepositIngestTaskFactory taskFactory) {
        this.name = name;
        if (!inDir.isAbsolute())
            throw new IllegalArgumentException("inDir must be an absolute path");
        this.inDir = inDir;
        if (!outDir.isAbsolute())
            throw new IllegalArgumentException("outDir must be an absolute path");
        this.outDir = outDir;
        this.eventWriter = new EventWriter(taskEventService, name);
        this.taskFactory = taskFactory;
    }

    @Override
    public Iterator<DepositIngestTask> iterator() {
        return createIterator(inDir, outDir, taskFactory, eventWriter);
    }

    protected AbstractDepositsImportTaskIterator createIterator(Path inDir, Path outDir, DepositIngestTaskFactory taskFactory, EventWriter eventWriter) {
        return new BoundedDepositImportTaskIterator(inDir, outDir, taskFactory, eventWriter);
    }
}
