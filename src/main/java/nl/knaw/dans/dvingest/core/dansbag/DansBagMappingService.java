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

import nl.knaw.dans.dvingest.core.yaml.EditFiles;
import nl.knaw.dans.dvingest.core.yaml.EditPermissions;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.domain.DepositState;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;

import java.io.IOException;
import java.nio.file.Path;

public interface DansBagMappingService {
    Deposit readDansDeposit(Path depositDir) throws InvalidDepositException;

    Dataset getDatasetMetadataFromDansDeposit(Deposit dansDeposit);

    EditFiles getEditFilesFromDansDeposit(Deposit dansDeposit);

    EditPermissions getEditPermissionsFromDansDeposit(Deposit dansDeposit);

    void updateDepositStatus(Deposit deposit, DepositState state, String pid);

    String packageOriginalMetadata(Deposit dansDeposit) throws IOException;
}
