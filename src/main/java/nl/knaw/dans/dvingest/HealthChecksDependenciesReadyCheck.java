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
package nl.knaw.dans.dvingest;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.config.DependenciesReadyCheckConfig;
import nl.knaw.dans.dvingest.core.DependenciesReadyCheck;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Implementation of {@link DependenciesReadyCheck} that waits until all health checks are healthy.
 */
@Slf4j
public class HealthChecksDependenciesReadyCheck implements DependenciesReadyCheck {
    private final List<HealthCheck> healthChecks = new ArrayList<>();
    private final long pollInterval;

    public HealthChecksDependenciesReadyCheck(Environment environment, DependenciesReadyCheckConfig config) {
        for (var name : config.getHealthChecks()) {
            var healthCheck = environment.healthChecks().getHealthCheck(name);
            if (healthCheck == null) {
                throw new IllegalArgumentException("Health check with name " + name + " not found");
            }
            healthChecks.add(healthCheck);
        }
        pollInterval = config.getPollInterval().toMilliseconds();
    }

    @Override
    public void waitUntilReady() {
        while (!allHealthy()) {
            log.warn("Not all health checks are healthy yet, waiting for {} ms", pollInterval);
            try {
                sleep(pollInterval);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean allHealthy() {
        for (var healthCheck : healthChecks) {
            if (healthCheck.execute().isHealthy()) {
                return false;
            }
        }
        return true;
    }
}
