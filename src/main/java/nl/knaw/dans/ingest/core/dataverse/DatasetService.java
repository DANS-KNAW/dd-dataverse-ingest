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
package nl.knaw.dans.ingest.core.dataverse;

import nl.knaw.dans.ingest.core.exception.InvalidDatasetStateException;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DatasetService {

    Optional<String> getDatasetUrnNbn(String datasetId) throws IOException, DataverseException;

    String getDatasetState(String datasetId) throws IOException, DataverseException;

    void setEmbargo(String datasetId, Instant dateAvailable, Collection<Integer> fileIds) throws IOException, DataverseException;

    void waitForState(String datasetId, String state) throws InvalidDatasetStateException;

    void releaseMigrated(String datasetId, String date) throws IOException, DataverseException;

    void publishDataset(String datasetId) throws IOException, DataverseException;

    Optional<AuthenticatedUser> getUserById(String userId);

    List<String> getDatasetRoleAssignments(String userId, String datasetId) throws IOException, DataverseException;

    List<String> getDataverseRoleAssignments(String userId) throws IOException, DataverseException;

    DataverseClient _getClient();

    List<DatasetResultItem> searchDatasets(String key, String value) throws IOException, DataverseException;

    List<URI> getLicenses() throws IOException, DataverseException;

    void submitForReview(String persitentId) throws IOException, DataverseException;

    boolean isDatasetInReview(String persitentId) throws IOException, DataverseException;
}
