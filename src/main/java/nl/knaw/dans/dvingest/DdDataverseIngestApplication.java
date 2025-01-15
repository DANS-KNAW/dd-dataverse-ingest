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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.client.ValidateDansBagServiceImpl;
import nl.knaw.dans.dvingest.config.DansDepositConversionConfig;
import nl.knaw.dans.dvingest.config.DdDataverseIngestConfiguration;
import nl.knaw.dans.dvingest.config.DepositorAuthorizationConfig;
import nl.knaw.dans.dvingest.config.IngestAreaConfig;
import nl.knaw.dans.dvingest.core.AutoIngestArea;
import nl.knaw.dans.dvingest.core.IngestArea;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingServiceImpl;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositSupportFactory;
import nl.knaw.dans.dvingest.core.dansbag.SupportedLicenses;
import nl.knaw.dans.dvingest.core.dansbag.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.DataverseServiceImpl;
import nl.knaw.dans.dvingest.core.service.UtilityServicesImpl;
import nl.knaw.dans.dvingest.core.service.YamlServiceImpl;
import nl.knaw.dans.dvingest.resources.DefaultApiResource;
import nl.knaw.dans.dvingest.resources.IllegalArgumentExceptionMapper;
import nl.knaw.dans.dvingest.resources.IngestApiResource;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.util.DataverseHealthCheck;
import nl.knaw.dans.lib.util.MappingLoader;
import nl.knaw.dans.lib.util.inbox.Inbox;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
public class DdDataverseIngestApplication extends Application<DdDataverseIngestConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DdDataverseIngestApplication().run(args);
    }

    @Override
    public String getName() {
        return "DD Dataverse Ingest";
    }

    @Override
    public void initialize(final Bootstrap<DdDataverseIngestConfiguration> bootstrap) {
    }

    @Override
    public void run(final DdDataverseIngestConfiguration configuration, final Environment environment) {
        /*
         * Create service components
         */
        var dataverseClient = configuration.getDataverse().build(environment, "dataverse");
        var dataverseService = DataverseServiceImpl.builder()
            .dataverseClient(dataverseClient)
            .metadataKeys(configuration.getIngest().getMetadataKeys())
            .timeout(configuration.getIngest().getWaitForReleasedState().getTimeout().toMilliseconds())
            .leadTimePerFile(configuration.getIngest().getWaitForReleasedState().getLeadTimePerFile().toMilliseconds())
            .pollingInterval(configuration.getIngest().getWaitForReleasedState().getPollingInterval().toMilliseconds())
            .build();
        var utilityServices = UtilityServicesImpl.builder()
            .tempDir(configuration.getIngest().getTempDir())
            .maxNumberOfFilesPerUpload(configuration.getIngest().getMaxNumberOfFilesPerUploadBatch())
            .maxUploadSize(configuration.getIngest().getMaxByteSizePerUploadBatch().toBytes())
            .build();
        var yamlService = new YamlServiceImpl();
        var dataverseIngestDepositFactory = new DataverseIngestDepositFactoryImpl(yamlService);
        var bagProcessorFactory = new BagProcessorFactoryImpl(dataverseService, utilityServices);

        /*
         *  Import area
         */
        DansDepositConversionConfig dansDepositConversionConfig = configuration.getDansDepositConversion();
        var importArea = getIngestArea(configuration.getIngest().getImportConfig(), "import", environment, dansDepositConversionConfig, dataverseService, yamlService, bagProcessorFactory,
            dataverseIngestDepositFactory);
        /*
         * Migration area
         */
        var migrationArea = getIngestArea(configuration.getIngest().getMigration(), "migration", environment, dansDepositConversionConfig, dataverseService, yamlService, bagProcessorFactory,
            dataverseIngestDepositFactory);
        /*
         * Auto ingest area
         */
        var autoIngestArea = getAutoIngestArea(configuration.getIngest().getAutoIngest(), environment, dansDepositConversionConfig, dataverseService, yamlService, bagProcessorFactory,
            dataverseIngestDepositFactory);

        /*
         * Register components with Dropwizard
         */
        environment.jersey().register(new DefaultApiResource());
        environment.jersey().register(new IngestApiResource(importArea, migrationArea));
        environment.lifecycle().manage(autoIngestArea);
        environment.jersey().register(new IllegalArgumentExceptionMapper());

        environment.healthChecks().register("dataverse", new DataverseHealthCheck(dataverseClient));
    }

    private AutoIngestArea getAutoIngestArea(IngestAreaConfig ingestAreaConfig, Environment environment, DansDepositConversionConfig dansDepositConversionConfig,
        DataverseServiceImpl dataverseService, YamlServiceImpl yamlService, BagProcessorFactoryImpl bagProcessorFactory, DataverseIngestDepositFactoryImpl dataverseIngestDepositFactory) {
        DansDepositSupportFactory dansDepositSupportFactory = new DansDepositSupportDisabledFactory();
        if (dansDepositConversionConfig != null) {
            var dansBagMappingService = createDansBagMappingService(false, dansDepositConversionConfig, dansDepositConversionConfig.getDepositorAuthorization().getAutoIngest(), dataverseService);
            var validateDansBagService = new ValidateDansBagServiceImpl(dansDepositConversionConfig.getValidateDansBag(), false, environment);
            dansDepositSupportFactory = new DansDepositSupportFactoryImpl(validateDansBagService, dansBagMappingService, dataverseService, yamlService, ingestAreaConfig.getRequireDansBag());
        }
        var depositTaskFactory = new DepositTaskFactoryImpl(bagProcessorFactory, dansDepositSupportFactory);
        var inboxTaskFactory = new InboxTaskFactoryImpl(dataverseIngestDepositFactory, depositTaskFactory, ingestAreaConfig.getOutbox());
        var inbox = Inbox.builder().inbox(ingestAreaConfig.getInbox()).taskFactory(inboxTaskFactory).build();
        return new AutoIngestArea(inbox, ingestAreaConfig.getOutbox());
    }

    private IngestArea getIngestArea(IngestAreaConfig ingestAreaConfig, String name, Environment environment, DansDepositConversionConfig dansDepositConversionConfig,
        DataverseServiceImpl dataverseService, YamlServiceImpl yamlService, BagProcessorFactoryImpl bagProcessorFactory, DataverseIngestDepositFactoryImpl dataverseIngestDepositFactory) {
        boolean isMigration = name.equals("migration");
        DansDepositSupportFactory dansDepositSupportFactory = new DansDepositSupportDisabledFactory();
        if (dansDepositConversionConfig != null) {
            var dansBagMappingService = createDansBagMappingService(isMigration, dansDepositConversionConfig, dansDepositConversionConfig.getDepositorAuthorization().getMigration(), dataverseService);
            var validateDansBag = new ValidateDansBagServiceImpl(dansDepositConversionConfig.getValidateDansBag(), isMigration, environment);
            dansDepositSupportFactory = new DansDepositSupportFactoryImpl(validateDansBag, dansBagMappingService, dataverseService, yamlService,
                ingestAreaConfig.getRequireDansBag());
        }
        var depositTaskFactory = new DepositTaskFactoryImpl(bagProcessorFactory, dansDepositSupportFactory);
        var jobFactory = new ImportJobFactoryImpl(dataverseIngestDepositFactory, depositTaskFactory);
        return new IngestArea(jobFactory, ingestAreaConfig.getInbox(), ingestAreaConfig.getOutbox(),
            environment.lifecycle().executorService(name).minThreads(1).maxThreads(1).build());
    }

    private DansBagMappingService createDansBagMappingService(boolean isMigration, DansDepositConversionConfig dansDepositConversionConfig, DepositorAuthorizationConfig depositorAuthorizationConfig,
        DataverseService dataverseService) {
        log.info("Configuring DANS Deposit conversion");
        try {
            var mapper = createMapper(isMigration, dansDepositConversionConfig, dataverseService);
            return new DansBagMappingServiceImpl(
                mapper,
                dataverseService,
                new SupportedLicenses(dataverseService),
                dansDepositConversionConfig.getFileExclusionPattern() == null ? null :
                    Pattern.compile(dansDepositConversionConfig.getFileExclusionPattern()),
                dansDepositConversionConfig.getFilesForIndividualUploadPattern() == null ? null :
                    Pattern.compile(dansDepositConversionConfig.getFilesForIndividualUploadPattern()),
                dansDepositConversionConfig.getEmbargoExclusions(),
                dansDepositConversionConfig.getAssignDepositorRole().getAutoIngest(),
                dansDepositConversionConfig.getAssignDepositorRole().getMigration(),
                depositorAuthorizationConfig.getPublishDataset(),
                depositorAuthorizationConfig.getEditDataset());
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to read configuration files", e);
        }
        catch (DataverseException e) {
            throw new IllegalStateException("Failed to read supported licenses", e);
        }
    }

    private DepositToDvDatasetMetadataMapper createMapper(boolean isMigration, DansDepositConversionConfig dansDepositConversionConfig, DataverseService dataverseService) {
        var mappingDefsDir = dansDepositConversionConfig.getMappingDefsDir();
        try {
            return new DepositToDvDatasetMetadataMapper(
                isMigration,
                dansDepositConversionConfig.isDeduplicate(),
                dataverseService.getActiveMetadataBlockNames(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("iso639-1-to-dv.csv")).keyColumn("ISO639-1").valueColumn("Dataverse-language").build().load(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("iso639-2-to-dv.csv")).keyColumn("ISO639-2").valueColumn("Dataverse-language").build().load(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("abr-report-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("verwervingswijzen-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("abr-complextype-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("abr-artifact-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
                MappingLoader.builder().csvFile(mappingDefsDir.resolve("abr-period-code-to-term.csv")).keyColumn("code").valueColumn("subject").build().load(),
                FileUtils.readLines(mappingDefsDir.resolve("spatial-coverage-country-terms.txt").toFile(), StandardCharsets.UTF_8),
                dansDepositConversionConfig.getDataSuppliers(),
                dansDepositConversionConfig.getSkipFields());
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to read configuration files", e);
        }
        catch (DataverseException e) {
            throw new IllegalStateException("Failed to read supported licenses", e);
        }
    }
}
