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
import nl.knaw.dans.dvingest.config.DansDepositConversionConfig;
import nl.knaw.dans.dvingest.config.DdDataverseIngestConfiguration;
import nl.knaw.dans.dvingest.core.IngestArea;
import nl.knaw.dans.dvingest.core.MappingLoader;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingService;
import nl.knaw.dans.dvingest.core.dansbag.DansBagMappingServiceImpl;
import nl.knaw.dans.dvingest.core.dansbag.SupportedLicenses;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.dvingest.core.service.DataverseServiceImpl;
import nl.knaw.dans.dvingest.core.service.UtilityServicesImpl;
import nl.knaw.dans.dvingest.resources.DefaultApiResource;
import nl.knaw.dans.dvingest.resources.IllegalArgumentExceptionMapper;
import nl.knaw.dans.dvingest.resources.IngestApiResource;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapper;
import nl.knaw.dans.lib.dataverse.DataverseException;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        environment.jersey().register(new DefaultApiResource());
        var dataverseClient = configuration.getDataverse().build();
        var dataverseService = DataverseServiceImpl.builder()
            .dataverseClient(dataverseClient)
            .metadataKeys(configuration.getIngest().getMetadataKeys())
            .millisecondsBetweenChecks(configuration.getIngest().getWaitForReleasedState().getTimeBetweenChecks().toMilliseconds())
            .maxNumberOfRetries(configuration.getIngest().getWaitForReleasedState().getMaxNumberOfRetries())
            .build();
        var utilityServices = UtilityServicesImpl.builder()
            .tempDir(configuration.getIngest().getTempDir())
            .maxNumberOfFilesPerUpload(configuration.getIngest().getMaxNumberOfFilesPerUploadBatch())
            .build();
        var importAreaBuilder = IngestArea.builder()
            .executorService(environment.lifecycle().executorService("import").minThreads(1).maxThreads(1).build())
            .dataverseService(dataverseService)
            .utilityServices(utilityServices)
            .inbox(configuration.getIngest().getImportConfig().getInbox())
            .outbox(configuration.getIngest().getImportConfig().getOutbox());
        if (configuration.getDansDepositConversion() == null) {
            log.info("DANS Deposit conversion is disabled");
        }
        else {
            importAreaBuilder.dansBagMappingService(createDansBagMappingService(configuration.getDansDepositConversion(), dataverseService));
        }
        var importArea = importAreaBuilder.build();
        environment.jersey().register(new IngestApiResource(importArea));
        environment.jersey().register(new IllegalArgumentExceptionMapper());
    }

    private DansBagMappingService createDansBagMappingService(DansDepositConversionConfig dansDepositConversionConfig, DataverseService dataverseService) {
        log.info("Configuring DANS Deposit conversion");
        try {
            var mappingDefsDir = dansDepositConversionConfig.getMappingDefsDir();

            var mapper = new DepositToDvDatasetMetadataMapper(
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
            return new DansBagMappingServiceImpl(mapper, dataverseService, new SupportedLicenses(dataverseService), Pattern.compile("a^"));
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to read configuration files", e);
        }
        catch (DataverseException e) {
            throw new IllegalStateException("Failed to read supported licenses", e);
        }
    }
}
