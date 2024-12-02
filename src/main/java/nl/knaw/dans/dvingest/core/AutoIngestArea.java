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
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.client.ValidateDansBagService;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;
import nl.knaw.dans.dvingest.core.service.YamlService;
import nl.knaw.dans.lib.util.inbox.Inbox;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
public class AutoIngestArea extends IngestArea implements Managed {
    private final Inbox autoIngestInbox;

    @Builder(builderMethodName = "autoIngestAreaBuilder")
    public AutoIngestArea(@NonNull ExecutorService executorService, @NonNull ValidateDansBagService validateDansBagService, @NonNull DataverseService dataverseService,
        @NonNull UtilityServices utilityServices,
        DansBagMappingService dansBagMappingService /* may be null if disabled !*/, @NonNull YamlService yamlService,
        Path inbox, Path outbox) {
        super(executorService, validateDansBagService, dataverseService, utilityServices, dansBagMappingService, yamlService, inbox, outbox);
        var inboxTaskFactory = new DepositInboxTaskFactory(outbox, false, dataverseService, utilityServices, validateDansBagService, dansBagMappingService, yamlService);
        autoIngestInbox = Inbox.builder()
            .inbox(inbox)
            .taskFactory(inboxTaskFactory)
            .build();
    }

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
