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

import nl.knaw.dans.ingest.core.TaskEvent;
import nl.knaw.dans.ingest.core.sequencing.TargetedTask;
import nl.knaw.dans.ingest.core.sequencing.TargetedTaskSequenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnqueuingServiceImpl implements EnqueuingService {
    private static final Logger log = LoggerFactory.getLogger(EnqueuingServiceImpl.class);

    private final ExecutorService enqueuingExecutor;
    private final TargetedTaskSequenceManager targetedTaskSequenceManager;

    public EnqueuingServiceImpl(TargetedTaskSequenceManager targetedTaskSequenceManager, int numberOfClients) {
        this.targetedTaskSequenceManager = targetedTaskSequenceManager;
        enqueuingExecutor = Executors.newFixedThreadPool(numberOfClients);
    }

    @Override
    public <T extends TargetedTask> void executeEnqueue(TargetedTaskSource<T> source) {
        log.trace("executeEnqueue({})", source);
        enqueuingExecutor.execute(() -> {
            log.debug("Start enqueuing tasks");
            try {
                for (T t : source) {
                    enqueue(t);
                }
            }
            catch (Exception e) {
                log.error("Enqueuing could not start because of an error", e);
            }
        });
    }

    private <T extends TargetedTask> void enqueue(T t) {
        log.trace("Enqueuing {}", t);
        try {
            targetedTaskSequenceManager.scheduleTask(t);
            t.writeEvent(TaskEvent.EventType.ENQUEUE, TaskEvent.Result.OK, null);
        }
        catch (Exception e) {
            log.error("Enqueuing of {} failed", t, e);
            t.writeEvent(TaskEvent.EventType.ENQUEUE, TaskEvent.Result.FAILED, e.getMessage());
        }
    }
}
