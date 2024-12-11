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
package nl.knaw.dans.dvingest.core.bagprocessor;

import io.dropwizard.configuration.ConfigurationException;
import nl.knaw.dans.dvingest.core.DataverseIngestBag;

import java.io.IOException;
import java.util.UUID;

/**
 * Factory for creating BagProcessors.
 */
public interface BagProcessorFactory {

    /**
     * Create a BagProcessor for the given deposit.
     *
     * @param depositId the deposit id
     * @param bag       the bag
     * @return the BagProcessor
     * @throws ConfigurationException if the Yaml files in the bag are not valid
     * @throws IOException            if there was a problem readin the bag files
     */
    BagProcessor createBagProcessor(UUID depositId, DataverseIngestBag bag) throws ConfigurationException, IOException;

}
