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
package nl.knaw.dans.ingest.core.deposit;

import gov.loc.repository.bagit.domain.Metadata;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import lombok.AllArgsConstructor;
import nl.knaw.dans.ingest.core.domain.DepositLocation;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.exception.MissingTargetException;
import nl.knaw.dans.ingest.core.io.BagDataManager;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@AllArgsConstructor
public class DepositLocationReaderImpl implements DepositLocationReader {
    private final BagDataManager bagDataManager;

    @Override
    public DepositLocation readDepositLocation(Path depositDir) throws InvalidDepositException, IOException {
        try {
            var properties = bagDataManager.readDepositProperties(depositDir);
            var depositId = getDepositId(depositDir);
            var target = getTarget(properties);
            var created = getCreated(properties);

            return new DepositLocation(depositDir, target, depositId.toString(), created);
        }
        catch (ConfigurationException e) {
            throw new InvalidDepositException("Deposit.properties file could not be read", e);
        }
        catch (MissingTargetException e) {
            throw new InvalidDepositException(e.getMessage(), e);
        }
    }

    String getTarget(Configuration properties) throws MissingTargetException {
        // the logic for the target should be
        // 1. if there is a dataverse.sword-token, use that
        // 2. otherwise, use identifier.doi
        var target = properties.getString("dataverse.sword-token");

        if (StringUtils.isBlank(target)) {
            target = properties.getString("identifier.doi");
        }

        if (StringUtils.isBlank(target)) {
            throw new MissingTargetException("No viable target found in deposit");
        }

        return target;
    }

    UUID getDepositId(Path path) throws InvalidDepositException {
        try {
            return UUID.fromString(path.getFileName().toString());
        }
        catch (IllegalArgumentException e) {
            throw new InvalidDepositException(String.format(
                "Deposit dir must be an UUID; found '%s'",
                path.getFileName()
            ), e);
        }
    }

    OffsetDateTime getCreated(Configuration properties) throws InvalidDepositException {
        try {
            var created = properties.getString("creation.timestamp");
            if (StringUtils.isBlank(created)) {
                throw new InvalidDepositException("No creation timestamp found in deposit");
            }

            return OffsetDateTime.parse(created);
        }
        catch (DateTimeParseException e) {
            throw new InvalidDepositException("Error while parsing date", e);
        }
    }
}
