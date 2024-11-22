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

import io.dropwizard.hibernate.UnitOfWork;
import nl.knaw.dans.ingest.core.TaskEvent;
import nl.knaw.dans.ingest.db.TaskEventDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/events")
public class EventsResource {

    private final TaskEventDAO taskEventDAO;

    public EventsResource(TaskEventDAO taskEventDAO) {
        this.taskEventDAO = taskEventDAO;
    }

    @GET
    @Produces("text/csv;charset=utf8")
    @UnitOfWork
    public List<TaskEvent> getEvents(@QueryParam("source") String batchName, @QueryParam("depositId") String depositId) {
        return taskEventDAO.getEvents(batchName, depositId);
    }

}
