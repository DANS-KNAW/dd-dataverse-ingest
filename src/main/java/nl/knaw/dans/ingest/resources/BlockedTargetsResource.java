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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.ingest.api.ResponseMessage;
import nl.knaw.dans.ingest.core.exception.TargetBlockedException;
import nl.knaw.dans.ingest.core.exception.TargetNotFoundException;
import nl.knaw.dans.ingest.core.service.BlockedTargetService;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/blocked-targets")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class BlockedTargetsResource {

    private final BlockedTargetService blockedTargetService;

    public BlockedTargetsResource(BlockedTargetService blockedTargetService) {
        this.blockedTargetService = blockedTargetService;
    }

    @POST
    @Path("/{target}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response blockTarget(@PathParam("target") String target) {
        try {
            blockedTargetService.blockTarget(target);
        }
        catch (TargetBlockedException e) {
            throw new ClientErrorException(
                String.format("Target %s is already blocked", target),
                Status.CONFLICT
            );
        }

        return Response.ok(new ResponseMessage(200, String.format("Target %s is blocked", target))).build();
    }

    @DELETE
    @Path("/{target}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unblockTarget(@PathParam("target") String target) {
        try {
            blockedTargetService.unblockTarget(target);
        }
        catch (TargetNotFoundException e) {
            throw new NotFoundException(String.format("Target %s could not be found", target));
        }

        return Response.ok(new ResponseMessage(200, String.format("Target %s is unblocked", target))).build();
    }
}
