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

import io.dropwizard.configuration.ConfigurationException;
import lombok.AllArgsConstructor;
import nl.knaw.dans.dvingest.core.DataverseIngestBag;
import nl.knaw.dans.dvingest.core.bagprocessor.BagProcessor;
import nl.knaw.dans.dvingest.core.bagprocessor.BagProcessorFactory;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.UtilityServices;

import java.io.IOException;
import java.util.UUID;

@AllArgsConstructor
public class BagProcessorFactoryImpl implements BagProcessorFactory {
    private final DataverseService dataverseService;
    private final UtilityServices utilityServices;

    @Override
    public BagProcessor createBagProcessor(UUID depositId, DataverseIngestBag bag) throws ConfigurationException, IOException {
        return BagProcessor.builder()
            .depositId(depositId)
            .bag(bag)
            .dataverseService(dataverseService)
            .utilityServices(utilityServices)
            .build();
    }
}
