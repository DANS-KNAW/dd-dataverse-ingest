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
package nl.knaw.dans.ingest.health;

import com.codahale.metrics.health.HealthCheck;
import nl.knaw.dans.ingest.core.service.DansBagValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DansBagValidatorHealthCheck extends HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(DansBagValidatorHealthCheck.class);

    private final DansBagValidator dansBagValidatorInstance;

    public DansBagValidatorHealthCheck(DansBagValidator dansBagValidatorInstance) {
        this.dansBagValidatorInstance = dansBagValidatorInstance;
    }

    @Override
    protected Result check() {
        log.debug("Checking that Dans Bag Validator is available");

        try {
            dansBagValidatorInstance.checkConnection();
            return Result.healthy();
        }
        catch (Exception e) {
            return Result.unhealthy("Dans Bag Validator is not available: %s", e.getMessage());
        }

    }
}
