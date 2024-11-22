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
package nl.knaw.dans.ingest.config;

import java.nio.file.Path;

public class IngestAreaConfig {
    private Path inbox;
    private Path outbox;
    private String depositorRole;
    private DatasetAuthorizationConfig authorization;
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Path getInbox() {
        return inbox;
    }

    public void setInbox(Path inbox) {
        this.inbox = inbox;
    }

    public Path getOutbox() {
        return outbox;
    }

    public void setOutbox(Path outbox) {
        this.outbox = outbox;
    }

    public String getDepositorRole() {
        return depositorRole;
    }

    public void setDepositorRole(String depositorRole) {
        this.depositorRole = depositorRole;
    }

    public DatasetAuthorizationConfig getAuthorization() {
        return authorization;
    }

    public void setAuthorization(DatasetAuthorizationConfig authorization) {
        this.authorization = authorization;
    }


}
