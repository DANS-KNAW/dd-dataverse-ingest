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
package nl.knaw.dans.ingest;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import nl.knaw.dans.ingest.config.DdIngestFlowConfiguration;
import nl.knaw.dans.ingest.config.IngestFlowConfigReader;
import nl.knaw.dans.ingest.core.AutoIngestArea;
import nl.knaw.dans.ingest.core.BlockedTarget;
import nl.knaw.dans.ingest.core.CsvMessageBodyWriter;
import nl.knaw.dans.ingest.core.ImportArea;
import nl.knaw.dans.ingest.core.TaskEvent;
import nl.knaw.dans.ingest.core.sequencing.TargetedTaskSequenceManager;
import nl.knaw.dans.ingest.core.service.BlockedTargetService;
import nl.knaw.dans.ingest.core.service.BlockedTargetServiceImpl;
import nl.knaw.dans.ingest.core.service.DansBagValidator;
import nl.knaw.dans.ingest.core.service.DansBagValidatorImpl;
import nl.knaw.dans.ingest.core.service.DepositIngestTaskFactoryBuilder;
import nl.knaw.dans.ingest.core.service.EnqueuingService;
import nl.knaw.dans.ingest.core.service.EnqueuingServiceImpl;
import nl.knaw.dans.ingest.core.service.TaskEventService;
import nl.knaw.dans.ingest.core.service.TaskEventServiceImpl;
import nl.knaw.dans.ingest.db.BlockedTargetDAO;
import nl.knaw.dans.ingest.db.TaskEventDAO;
import nl.knaw.dans.ingest.health.DansBagValidatorHealthCheck;
import nl.knaw.dans.ingest.resources.BlockedTargetsResource;
import nl.knaw.dans.ingest.resources.EventsResource;
import nl.knaw.dans.ingest.resources.ImportsResource;
import nl.knaw.dans.ingest.resources.MigrationsResource;
import nl.knaw.dans.lib.util.DataverseHealthCheck;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.IOException;
import java.net.URISyntaxException;

public class DdIngestFlowApplication extends Application<DdIngestFlowConfiguration> {

    private final HibernateBundle<DdIngestFlowConfiguration> hibernateBundle = new HibernateBundle<>(TaskEvent.class, BlockedTarget.class) {

        @Override
        public PooledDataSourceFactory getDataSourceFactory(DdIngestFlowConfiguration configuration) {
            return configuration.getTaskEventDatabase();
        }
    };

    public static void main(final String[] args) throws Exception {
        new DdIngestFlowApplication().run(args);
    }

    @Override
    public String getName() {
        return "DD Ingest Flow";
    }

    @Override
    public void initialize(final Bootstrap<DdIngestFlowConfiguration> bootstrap) {
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(new MultiPartBundle());
    }

    @Override
    public void run(final DdIngestFlowConfiguration configuration, final Environment environment) throws IOException, URISyntaxException {
        final var ingestFlowConfig = configuration.getIngestFlow();
        final var taskExecutor = ingestFlowConfig.getTaskQueue().build(environment);
        final var targetedTaskSequenceManager = new TargetedTaskSequenceManager(taskExecutor);

        IngestFlowConfigReader.readIngestFlowConfiguration(ingestFlowConfig);

        final var dansBagValidatorClient = new JerseyClientBuilder(environment)
            .withProvider(MultiPartFeature.class)
            .using(configuration.getValidateDansBag().getHttpClient())
            .build(getName());

        final DansBagValidator dansBagValidator = new DansBagValidatorImpl(
            dansBagValidatorClient,
            configuration.getValidateDansBag().getBaseUrl(),
            configuration.getValidateDansBag().getPingUrl());

        final BlockedTargetDAO blockedTargetDAO = new BlockedTargetDAO(hibernateBundle.getSessionFactory());
        final BlockedTargetService blockedTargetService = new UnitOfWorkAwareProxyFactory(hibernateBundle)
            .create(BlockedTargetServiceImpl.class, BlockedTargetDAO.class, blockedTargetDAO);

        final EnqueuingService enqueuingService = new EnqueuingServiceImpl(targetedTaskSequenceManager, 3 /* Must support importArea, migrationArea and autoIngestArea */);
        final TaskEventDAO taskEventDAO = new TaskEventDAO(hibernateBundle.getSessionFactory());
        final TaskEventService taskEventService = new UnitOfWorkAwareProxyFactory(hibernateBundle).create(TaskEventServiceImpl.class, TaskEventDAO.class, taskEventDAO);
        final var taskFactoryBuilder = new DepositIngestTaskFactoryBuilder(configuration, dansBagValidator, blockedTargetService);

        final var importAreaConfig = ingestFlowConfig.getImportConfig();
        final var migrationAreaConfig = ingestFlowConfig.getMigration();
        final var autoIngestAreaConfig = ingestFlowConfig.getAutoIngest();

        final ImportArea importArea = new ImportArea(
            importAreaConfig.getInbox(),
            importAreaConfig.getOutbox(),
            taskFactoryBuilder.createTaskFactory(environment, "dd-ingest-flow/import", importAreaConfig,  false),
            taskEventService,
            enqueuingService);

        // Can be phased out after migration.
        final ImportArea migrationArea = new ImportArea(
            migrationAreaConfig.getInbox(),
            migrationAreaConfig.getOutbox(),
            taskFactoryBuilder.createTaskFactory(environment, "dd-ingest-flow/migration", migrationAreaConfig,true),
            taskEventService,
            enqueuingService);

        final AutoIngestArea autoIngestArea = new AutoIngestArea(
            autoIngestAreaConfig.getInbox(),
            autoIngestAreaConfig.getOutbox(),
            taskFactoryBuilder.createTaskFactory(environment, "dd-ingest-flow/auto-ingest", autoIngestAreaConfig,  false),
            taskEventService,
            enqueuingService
        );

        // Use the default configuration for the health checks. No API key is required.
        environment.healthChecks().register("Dataverse", new DataverseHealthCheck(configuration.getDataverse().build()));
        environment.healthChecks().register("DansBagValidator", new DansBagValidatorHealthCheck(dansBagValidator));

        environment.lifecycle().manage(autoIngestArea);
        environment.jersey().register(new ImportsResource(importArea));
        environment.jersey().register(new MigrationsResource(migrationArea));
        environment.jersey().register(new EventsResource(taskEventDAO));
        environment.jersey().register(new BlockedTargetsResource(blockedTargetService));
        environment.jersey().register(new CsvMessageBodyWriter());
    }
}
