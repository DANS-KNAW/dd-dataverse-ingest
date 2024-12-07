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
package nl.knaw.dans.dvingest.core.dansbag.io;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.nio.file.Path;

public interface BagDataManager {

    /**
     * Reads the bag on the path that contains bag-info.txt and bagit.txt, among other files
     *
     * @param bagDir the path that contains bag-info.txt and bagit.txt
     * @return The bag object
     * @throws UnparsableVersionException
     * @throws InvalidBagitFileFormatException
     * @throws IOException
     * @throws MaliciousPathException
     * @throws UnsupportedAlgorithmException
     */
    Bag readBag(Path bagDir) throws UnparsableVersionException, InvalidBagitFileFormatException, IOException, MaliciousPathException, UnsupportedAlgorithmException;

    /**
     * Reads the deposit.properties file found inside the folder provided. The path should NOT reference deposit.properties directly
     *
     * @param depositDir The directory that contains a deposit.properties file
     * @return
     * @throws ConfigurationException
     */
    Configuration readDepositProperties(Path depositDir) throws ConfigurationException;

}
