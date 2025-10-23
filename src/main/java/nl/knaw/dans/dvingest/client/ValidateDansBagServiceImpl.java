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
package nl.knaw.dans.dvingest.client;

import io.dropwizard.core.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.config.ValidateDansBagConfig;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.lib.util.PingHealthCheck;
import nl.knaw.dans.validatedansbag.client.api.ValidateCommandDto;
import nl.knaw.dans.validatedansbag.client.api.ValidateCommandDto.PackageTypeEnum;
import nl.knaw.dans.validatedansbag.client.api.ValidateOkDto;
import nl.knaw.dans.validatedansbag.client.resources.DefaultApi;
import nl.knaw.dans.validatedansbag.invoker.ApiClient;

import java.nio.file.Path;

@Slf4j
public class ValidateDansBagServiceImpl implements ValidateDansBagService {
    private final DefaultApi api;

    public ValidateDansBagServiceImpl(ValidateDansBagConfig config, Environment environment) {
        api = new ClientProxyBuilder<ApiClient, DefaultApi>()
            .apiClient(new ApiClient())
            .defaultApiCtor(DefaultApi::new)
            .httpClient(config.getHttpClient())
            .basePath(config.getUrl()).build();

        if (environment.healthChecks().getHealthCheck(config.getHealthCheck().getName()) == null) {
            log.info("Registering health check with name {}...", config.getHealthCheck().getName());
            environment.healthChecks()
                .register(config.getHealthCheck().getName(), new PingHealthCheck(config.getHealthCheck().getName(), api.getApiClient().getHttpClient(), config.getHealthCheck().getPingUrl()));
        }
        else {
            log.info("Health check with name {} already exists. Not registering again...", config.getHealthCheck().getName());
        }
    }

    @Override
    public ValidateOkDto validate(Path bag) {
        var validateCommand = new ValidateCommandDto()
            .bagLocation(bag.toString())
            .packageType(PackageTypeEnum.DEPOSIT);

        try {
            return api.validateLocalDirPost(validateCommand);
        }
        catch (Exception e) {
            throw new RuntimeException("Error validating bag", e);
        }
    }
}
