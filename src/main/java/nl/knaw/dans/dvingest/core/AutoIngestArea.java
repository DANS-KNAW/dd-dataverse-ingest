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
package nl.knaw.dans.dvingest.core;

import io.dropwizard.lifecycle.Managed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.inbox.Inbox;

import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class AutoIngestArea implements Managed {
    private final Inbox autoIngestInbox;
    private final Path outbox;

    private void initOutputDir() {
        log.debug("Initializing output directory: {}", outbox);
        createDirectoryIfNotExists(outbox);
        createDirectoryIfNotExists(outbox.resolve("processed"));
        createDirectoryIfNotExists(outbox.resolve("failed"));
        createDirectoryIfNotExists(outbox.resolve("rejected"));
    }

    private void createDirectoryIfNotExists(Path path) {
        if (!path.toFile().exists()) {
            if (!path.toFile().mkdirs()) {
                throw new IllegalStateException("Failed to create directory: " + path);
            }
        }
    }

    @Override
    public void start() throws Exception {
        initOutputDir();
        autoIngestInbox.start();
    }

    @Override
    public void stop() throws Exception {
        autoIngestInbox.stop();
    }
}
