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

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import nl.knaw.dans.dvingest.config.DdDataverseIngestConfiguration;
import nl.knaw.dans.dvingest.core.DataverseServiceImpl;
import nl.knaw.dans.dvingest.core.IngestArea;
import nl.knaw.dans.dvingest.core.UtilityServicesImpl;
import nl.knaw.dans.dvingest.resources.DefaultApiResource;
import nl.knaw.dans.dvingest.resources.IllegalArgumentExceptionMapper;
import nl.knaw.dans.dvingest.resources.IngestApiResource;

public class DdDataverseIngestApplication extends Application<DdDataverseIngestConfiguration> {
    public static void main(final String[] args) throws Exception {
        new DdDataverseIngestApplication().run(args);
    }

    @Override
    public String getName() {
        return "Dd Dataverse Ingest";
    }

    @Override
    public void initialize(final Bootstrap<DdDataverseIngestConfiguration> bootstrap) {
    }

    @Override
    public void run(final DdDataverseIngestConfiguration configuration, final Environment environment) {
        environment.jersey().register(new DefaultApiResource());
        var dataverseClient = configuration.getDataverse().build();
        var dataverseService = DataverseServiceImpl.builder()
            .dataverseClient(dataverseClient)
            .millisecondsBetweenChecks(configuration.getIngest().getWaitForReleasedState().getTimeBetweenChecks().toMilliseconds())
            .maxNumberOfRetries(configuration.getIngest().getWaitForReleasedState().getMaxNumberOfRetries())
            .build();
        var utilityServices = UtilityServicesImpl.builder()
            .tempDir(configuration.getIngest().getTempDir())
            .maxNumberOfFilesPerUpload(configuration.getIngest().getMaxNumberOfFilesPerUpload())
            .build();
        var importArea = IngestArea.builder()
            .executorService(environment.lifecycle().executorService("import").minThreads(1).maxThreads(1).build())
            .dataverseService(dataverseService)
            .utilityServices(utilityServices)
            .inbox(configuration.getIngest().getImportConfig().getInbox())
            .outbox(configuration.getIngest().getImportConfig().getOutbox()).build();
        environment.jersey().register(new IngestApiResource(importArea));
        environment.jersey().register(new IllegalArgumentExceptionMapper());
    }
}
