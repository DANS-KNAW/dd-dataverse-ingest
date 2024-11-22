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
package nl.knaw.dans.ingest.resources;

import nl.knaw.dans.ingest.api.ResponseMessage;
import nl.knaw.dans.ingest.api.StartImport;
import nl.knaw.dans.ingest.core.ImportArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/migrations")
@Produces(MediaType.APPLICATION_JSON)
public class MigrationsResource {
    private static final Logger log = LoggerFactory.getLogger(MigrationsResource.class);

    private final ImportArea migrationArea;

    public MigrationsResource(ImportArea migrationArea) {
        this.migrationArea = migrationArea;
    }

    @POST
    @Path("/:start")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startImport(StartImport start) {
        log.info("Received command = {}", start);
        String taskName;
        try {
            var securePath = migrationArea.getSecurePath(start.getInputPath());
            taskName = migrationArea.startImport(securePath, start.isBatch(), start.isContinue());
        }
        catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Response.accepted(
                new ResponseMessage(Response.Status.ACCEPTED.getStatusCode(),
                    String.format("migration request was received (batch = %s, continue = %s", taskName, start.isContinue())))
            .build();
    }

}
