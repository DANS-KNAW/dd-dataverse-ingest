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

import java.util.UUID;

public class EventWriter {
    private final TaskEventService taskEventService;
    private final String name;

    public EventWriter(TaskEventService taskEventService, String name) {
        this.taskEventService = taskEventService;
        this.name = name;
    }

    public void write(UUID depositId, TaskEvent.EventType eventType, TaskEvent.Result result, String message) {
        taskEventService.writeEvent(name, depositId, eventType, result, message);
    }
}
