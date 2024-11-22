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
package nl.knaw.dans.ingest.core.sequencing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Manages the process of ingesting deposits in the correct order by ensuring that deposits that target the same dataset are not concurrently scheduled on different threads. If an unfinished deposit
 * for the same dataset is still present, the next deposit for that dataset will be queued on the same thread, ensuring that it cannot overtake the already processing deposit.
 */
public class TargetedTaskSequenceManager {
    private static final Logger log = LoggerFactory.getLogger(TargetedTaskSequenceManager.class);
    private final LinkedHashMap<String, TargetedTaskSequencer> sequencers = new LinkedHashMap<>();
    private final ExecutorService executorService;

    public TargetedTaskSequenceManager(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public synchronized void scheduleTask(TargetedTask targetedTask) {
        log.trace("Scheduling targeted task {}", targetedTask);
        // TODO: Use Is-Version-Of in autoIngest service (DOI is not available there)
        var sequencer = sequencers.get(targetedTask.getTarget());

        if (sequencer == null) {
            log.debug("Creating NEW sequencer for target {}", targetedTask.getTarget());
            sequencer = new TargetedTaskSequencer(this, targetedTask);
            sequencers.put(targetedTask.getTarget(), sequencer);
            executorService.execute(sequencer);
        }
        else {
            log.debug("Using EXISTING sequencer for target {}", targetedTask.getTarget());
            sequencer.enqueue(targetedTask);
        }
    }

    synchronized void removeSequencer(TargetedTaskSequencer sequencer) {
        log.trace("Removing sequencer for target {}", sequencer.getTarget());
        sequencers.remove(sequencer.getTarget());
    }

}
