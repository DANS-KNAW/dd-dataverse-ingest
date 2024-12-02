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

import nl.knaw.dans.dvingest.config.ValidateDansBagConfig;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.validatedansbag.client.api.ValidateCommandDto;
import nl.knaw.dans.validatedansbag.client.api.ValidateCommandDto.PackageTypeEnum;
import nl.knaw.dans.validatedansbag.client.api.ValidateOkDto;
import nl.knaw.dans.validatedansbag.client.resources.DefaultApi;
import nl.knaw.dans.validatedansbag.invoker.ApiClient;

import java.nio.file.Path;

public class ValidateDansBagServiceImpl implements ValidateDansBagService {
    private final DefaultApi api;
    private final boolean isMigration;

    public ValidateDansBagServiceImpl(ValidateDansBagConfig config, boolean isMigration) {
        this.isMigration = isMigration;
        api = new ClientProxyBuilder<ApiClient, DefaultApi>()
            .apiClient(new ApiClient())
            .defaultApiCtor(DefaultApi::new)
            .httpClient(config.getHttpClient())
            .basePath(config.getUrl()).build();
    }

    @Override
    public ValidateOkDto validate(Path bag) {
        var validateCommand = new ValidateCommandDto()
            .bagLocation(bag.toString())
            .packageType(isMigration ? PackageTypeEnum.MIGRATION : PackageTypeEnum.DEPOSIT);

        try {
            return api.validateLocalDirPost(validateCommand);
        }
        catch (Exception e) {
            throw new RuntimeException("Error validating bag", e);
        }
    }
}
