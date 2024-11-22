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
package nl.knaw.dans.ingest.core.service;

import nl.knaw.dans.validatedansbag.client.api.ValidateCommandDto;
import nl.knaw.dans.validatedansbag.client.api.ValidateOkDto;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class DansBagValidatorImpl implements DansBagValidator {
    private static final Logger log = LoggerFactory.getLogger(DansBagValidatorImpl.class);
    private final Client httpClient;
    private final URI serviceUri;
    private final URI pingUri;

    public DansBagValidatorImpl(Client httpClient, URI serviceUri, URI pingUri) {
        this.httpClient = httpClient;
        this.serviceUri = serviceUri;
        this.pingUri = pingUri;
    }

    @Override
    public void checkConnection() {
        try (var response = httpClient.target(pingUri)
            .request(MediaType.TEXT_PLAIN)
            .get()) {

            if (response.getStatus() != 200) {
                var content = response.readEntity(String.class);

                if (!"pong".equals(content.trim())) {
                    throw new RuntimeException("Validate DANS bag ping URL did not respond with 'pong'");
                }

                throw new RuntimeException(String.format(
                    "Connection to Validate DANS Bag Service could not be established. Service responded with %s",
                    response.getStatusInfo()));
            }
        }

        log.debug("OK: validator service is reachable.");
    }

    @Override
    public ValidateOkDto validateBag(Path bagDir, ValidateCommandDto.PackageTypeEnum informationPackageType) {
        var command = new ValidateCommandDto()
            .bagLocation(bagDir.toString())
            .packageType(informationPackageType);

        log.debug("Validating bag {} with command {}", bagDir, command);

        // TODO why is this not in the configuration
        var uri = serviceUri.resolve("validate");

        try (var multipart = new FormDataMultiPart()
            .field("command", command, MediaType.APPLICATION_JSON_TYPE)) {

            try (var response = httpClient.target(uri)
                .request()
                .post(Entity.entity(multipart, multipart.getMediaType()))) {

                if (response.getStatus() == 200) {
                    return response.readEntity(ValidateOkDto.class);
                }
                else {
                    throw new RuntimeException(String.format(
                        "DANS Bag Validation failed (%s): %s",
                        response.getStatusInfo(), response.readEntity(String.class)));
                }
            }
        }
        catch (IOException e) {
            log.error("Unable to create multipart form data object", e);
        }

        return null;
    }
}
