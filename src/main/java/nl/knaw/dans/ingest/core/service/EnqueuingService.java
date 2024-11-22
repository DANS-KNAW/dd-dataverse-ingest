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

import nl.knaw.dans.ingest.core.sequencing.TargetedTask;

/**
 * Enqueues a sequence of tasks asynchronously, i.e. it schedules the enqueuing action to be executed by a dedicated background thread. the reason is that the enqueuing can take
 * arbitrarily long. In case of a continuous stream of tasks it may even last until the service is stopped, but even in case of a batch it may take too long for the client to wait for
 * the enqueuing to finish.
 */
public interface EnqueuingService {

    <T extends TargetedTask> void executeEnqueue(TargetedTaskSource<T> source);
}
