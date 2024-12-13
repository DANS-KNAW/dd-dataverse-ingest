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
package nl.knaw.dans.dvingest.core.dansbag;

import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDeposit;
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;
import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.dvingest.core.yaml.Init;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Service for mapping a DANS deposit to a standard Dataverse ingest deposit. A DANS deposit has only one bag, which must conform to the DANS BagIt Profile.
 */
public interface DansBagMappingService {
    /**
     * Returns the DOI of the dataset that needs to be updated. If the deposit is to create a new dataset, this method returns null.
     *
     * @param depositDir the deposit directory
     * @return the DOI of the dataset that needs to be updated, or null if the deposit is to create a new dataset
     * @throws IOException        if there was an error reading the deposit or calling Dataverse
     * @throws DataverseException if a call to Dataverse failed
     */
    String getUpdatesDataset(Path depositDir) throws IOException, DataverseException;

    /**
     * Reads the DANS deposit from the given directory into a {@link DansBagDeposit} object.
     *
     * @param depositDir the deposit directory
     * @return the DANS deposit object
     * @throws InvalidDepositException if the deposit is invalid
     */
    DansBagDeposit readDansDeposit(Path depositDir) throws InvalidDepositException;

    /**
     * Determines what preconditions to expect and whether and how to create a new dataset based on the DANS deposit.
     *
     * @param dansDeposit the DANS deposit
     * @param isUpdate
     * @return the preconditions to expect and whether and how to create a new dataset
     */
    Init getInitFromDansDeposit(DansBagDeposit dansDeposit, boolean isUpdate);

    /**
     * Maps the metadata from the DANS deposit to the new dataset level metadata for the dataset. For some parts the new metadata depends on the current metadata of the dataset. That is why the
     * current metadata is also given as input. If the deposit is to create a new dataset, the current metadata is null.
     *
     * @param dansDeposit     the DANS deposit
     * @param currentMetadata the current metadata of the dataset
     * @return the new dataset level metadata
     */
    Dataset getDatasetMetadataFromDansDeposit(DansBagDeposit dansDeposit, DatasetVersion currentMetadata);

    /**
     * Maps file information in the DANS bag to edit actions for the files in the dataset. The edit actions are used to update the files in the dataset.
     *
     * @param dansDeposit    the DANS deposit
     * @param updatesDataset the DOI of the dataset that needs to be updated, or null if the deposit is to create a new dataset
     * @return the edit actions for the files in the dataset
     */
    EditFiles getEditFilesFromDansDeposit(DansBagDeposit dansDeposit, String updatesDataset);

    /**
     * Maps the permissions in the DANS deposit to edit actions for the permissions of the dataset. The edit actions are used to update the permissions of the dataset.
     *
     * @param dansDeposit    the DANS deposit
     * @param updatesDataset the DOI of the dataset that needs to be updated, or null if the deposit is to create a new dataset
     * @return the edit actions for the permissions of the dataset
     */
    EditPermissions getEditPermissionsFromDansDeposit(DansBagDeposit dansDeposit, String updatesDataset);

    /**
     * Packages the original metadata of the DANS bag into a ZIP file and returns the local path to the ZIP file.
     *
     * @param dansDeposit the DANS deposit
     * @return the local path to the ZIP file
     * @throws IOException if there was an error reading the deposit or writing the ZIP file
     */
    String packageOriginalMetadata(DansBagDeposit dansDeposit) throws IOException;
}
