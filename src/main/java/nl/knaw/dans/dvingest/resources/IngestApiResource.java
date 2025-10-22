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
package nl.knaw.dans.dvingest.resources;

import lombok.AllArgsConstructor;
import nl.knaw.dans.dvingest.api.ImportCommandDto;
import nl.knaw.dans.dvingest.core.IngestArea;

import javax.ws.rs.core.Response;

@AllArgsConstructor
public class IngestApiResource implements IngestApi {
    private final IngestArea ingestArea;
    private final IngestArea migrationArea;

    @Override
    public Response ingestCancelPost(String path, Boolean migration) {
        if (migration) {
            return Response.accepted(migrationArea.cancel(path)).build();
        }
        else {
            return Response.accepted(ingestArea.cancel(path)).build();
        }
    }

    @Override
    public Response ingestGet(String path, Boolean migration) {
        if (migration) {
            return Response.ok(migrationArea.getStatus(path)).build();
        }
        else {
            return Response.ok(ingestArea.getStatus(path)).build();
        }
    }

    @Override
    public Response ingestPost(ImportCommandDto importCommandDto) {
        if (importCommandDto.getMigration()) {
            return Response.ok(migrationArea.submit(importCommandDto)).build();
        }
        else {
            return Response.ok(ingestArea.submit(importCommandDto)).build();
        }
    }

}
