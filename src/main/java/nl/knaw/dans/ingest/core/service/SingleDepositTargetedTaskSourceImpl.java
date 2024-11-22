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

import java.nio.file.Path;
import java.util.Iterator;

public class SingleDepositTargetedTaskSourceImpl implements TargetedTaskSource<DepositIngestTask> {
    private final String name;

    private final Path deposit;

    private final Path outbox;

    private final DepositIngestTaskFactory taskFactory;

    private final EventWriter eventWriter;

    public SingleDepositTargetedTaskSourceImpl(String name, Path deposit, Path outbox, TaskEventService taskEventService, DepositIngestTaskFactory taskFactory) {
        this.name = name;
        this.deposit = deposit;
        this.outbox = outbox;
        this.taskFactory = taskFactory;
        this.eventWriter = new EventWriter(taskEventService, deposit.getFileName().toString());
    }

    @Override
    public Iterator<DepositIngestTask> iterator() {
        return new SingleDepositImportTaskIterator(deposit, outbox, taskFactory, eventWriter);
    }
}
