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

import gov.loc.repository.bagit.reader.BagReader;
import io.dropwizard.core.setup.Environment;
import nl.knaw.dans.ingest.config.DdIngestFlowConfiguration;
import nl.knaw.dans.ingest.config.IngestAreaConfig;
import nl.knaw.dans.ingest.core.dataverse.DataverseServiceImpl;
import nl.knaw.dans.ingest.core.deposit.BagDirResolverImpl;
import nl.knaw.dans.ingest.core.deposit.DepositFileListerImpl;
import nl.knaw.dans.ingest.core.deposit.DepositLocationReaderImpl;
import nl.knaw.dans.ingest.core.deposit.DepositManager;
import nl.knaw.dans.ingest.core.deposit.DepositManagerImpl;
import nl.knaw.dans.ingest.core.deposit.DepositReaderImpl;
import nl.knaw.dans.ingest.core.deposit.DepositWriterImpl;
import nl.knaw.dans.ingest.core.io.BagDataManagerImpl;
import nl.knaw.dans.ingest.core.io.FileServiceImpl;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapperFactory;
import nl.knaw.dans.ingest.core.validation.DepositorAuthorizationValidatorImpl;

import java.io.IOException;
import java.net.URISyntaxException;

public class DepositIngestTaskFactoryBuilder {

    private final DdIngestFlowConfiguration configuration;
    private final DansBagValidator dansBagValidator;
    private final DepositManager depositManager;
    private final ZipFileHandler zipFileHandler;
    private final BlockedTargetService blockedTargetService;

    public DepositIngestTaskFactoryBuilder(DdIngestFlowConfiguration configuration, DansBagValidator dansBagValidator, BlockedTargetService blockedTargetService) {

        final var xmlReader = new XmlReaderImpl();
        final var fileService = new FileServiceImpl();
        final var depositFileLister = new DepositFileListerImpl();

        // the parts responsible for reading and writing deposits to disk
        final var bagReader = new BagReader();
        final var bagDataManager = new BagDataManagerImpl(bagReader);
        final var bagDirResolver = new BagDirResolverImpl(fileService);
        final var depositReader = new DepositReaderImpl(xmlReader, bagDirResolver, fileService, bagDataManager, depositFileLister, new ManifestHelperImpl());
        final var depositLocationReader = new DepositLocationReaderImpl(bagDataManager);
        final var depositWriter = new DepositWriterImpl(bagDataManager);

        this.configuration = configuration;
        this.dansBagValidator = dansBagValidator;
        this.depositManager =  new DepositManagerImpl(depositReader, depositLocationReader, depositWriter);
        this.zipFileHandler = new ZipFileHandler(configuration.getIngestFlow().getZipWrappingTempDir());
        this.blockedTargetService = blockedTargetService;
    }

    public DepositIngestTaskFactory createTaskFactory(Environment environment, String name, IngestAreaConfig ingestAreaConfig, boolean isMigration) throws IOException, URISyntaxException {
        final var dataverseClientFactory = configuration.getDataverse();
        if (ingestAreaConfig.getApiKey() != null) {
            dataverseClientFactory.setApiKey(ingestAreaConfig.getApiKey());
        }
        final var dataverseClient = dataverseClientFactory.build(environment, name);
        final var ingestFlowConfig = configuration.getIngestFlow();
        final var mapperFactory = new DepositToDvDatasetMetadataMapperFactory(
            ingestFlowConfig.getIso1ToDataverseLanguage(),
            ingestFlowConfig.getIso2ToDataverseLanguage(),
            ingestFlowConfig.getAbrReportCodeToTerm(),
            ingestFlowConfig.getAbrAcquisitionMethodCodeToTerm(),
            ingestFlowConfig.getAbrComplexTypeCodeToTerm(),
            ingestFlowConfig.getAbrArtifactCodeToTerm(),
            ingestFlowConfig.getAbrPeriodCodeToTerm(),
            ingestFlowConfig.getSpatialCoverageCountryTerms(),
            ingestFlowConfig.getDataSuppliers(),
            configuration.getIngestFlow().getSkipFields(), dataverseClient
        );
        final var datasetService = new DataverseServiceImpl(
            dataverseClient,
            configuration.getDataverseExtra().getPublishAwaitUnlockWaitTimeMs(),
            configuration.getDataverseExtra().getPublishAwaitUnlockMaxRetries()
        );
        final var authorization = ingestAreaConfig.getAuthorization();
        final var datasetPublisher = authorization.getDatasetPublisher();
        final var datasetUpdater = authorization.getDatasetUpdater();
        return new DepositIngestTaskFactory(
            isMigration,
            ingestAreaConfig.getDepositorRole(),
            dansBagValidator,
            ingestFlowConfig,
            depositManager,
            mapperFactory,
            zipFileHandler,
            datasetService,
            blockedTargetService,
            new DepositorAuthorizationValidatorImpl(datasetService, datasetPublisher, datasetUpdater),
            configuration.getIngestFlow().getVaultMetadataKey()
        );
    }
}
