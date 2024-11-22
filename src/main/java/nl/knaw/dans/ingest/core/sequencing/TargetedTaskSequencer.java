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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

class TargetedTaskSequencer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(TargetedTaskSequencer.class);
    private final Queue<TargetedTask> localQueue = new ConcurrentLinkedDeque<>();
    private final TargetedTaskSequenceManager targetedTaskSequenceManager;
    private final String target;

    public TargetedTaskSequencer(TargetedTaskSequenceManager targetedTaskSequenceManager, TargetedTask task) {
        this.targetedTaskSequenceManager = targetedTaskSequenceManager;
        this.target = task.getTarget();
        enqueue(task);
    }

    public synchronized void enqueue(TargetedTask task) {
        log.debug("Adding task {} to sequencer queue", task);
        if (task.getTarget().equals(target)) {
            localQueue.add(task);
        }
        else {
            throw new IllegalArgumentException("Task target " + task.getTarget() + " is different from target " + target);
        }
    }

    public String getTarget() {
        return target;
    }

    @Override
    public void run() {
        TargetedTask task = localQueue.poll();

        while (task != null) {
            log.debug("Processing task {}", task);
            task.run();
            task = getNextTask();
        }
    }

    private TargetedTask getNextTask() {
        synchronized (targetedTaskSequenceManager) {
            TargetedTask task = localQueue.poll();
            if (task == null) {
                log.debug("No more tasks on sequencer queue. Removing sequencer for target {}", target);
                targetedTaskSequenceManager.removeSequencer(this);
            }
            return task;
        }
    }
}
