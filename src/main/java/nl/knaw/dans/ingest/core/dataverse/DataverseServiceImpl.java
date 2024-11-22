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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.ingest.core.exception.InvalidDatasetStateException;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.Embargo;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import nl.knaw.dans.lib.dataverse.model.license.License;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class DataverseServiceImpl implements DatasetService {
    public static final String AUTHENTICATED_USERS = ":authenticated-users";
    private final DataverseClient dataverseClient;
    private final int publishAwaitUnlockMillisecondsBetweenRetries;
    private final int publishAwaitUnlockMaxNumberOfRetries;

    private final SimpleDateFormat dateAvailableFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DataverseServiceImpl(DataverseClient dataverseClient, int publishAwaitUnlockMillisecondsBetweenRetries, int publishAwaitUnlockMaxNumberOfRetries) {
        this.dataverseClient = dataverseClient;
        this.publishAwaitUnlockMillisecondsBetweenRetries = publishAwaitUnlockMillisecondsBetweenRetries;
        this.publishAwaitUnlockMaxNumberOfRetries = publishAwaitUnlockMaxNumberOfRetries;
    }

    @Override
    public Optional<String> getDatasetUrnNbn(String datasetId) throws IOException, DataverseException {
        var dataset = dataverseClient.dataset(datasetId);
        dataset.awaitUnlock(publishAwaitUnlockMaxNumberOfRetries, publishAwaitUnlockMillisecondsBetweenRetries);

        var version = dataset.getVersion();
        var data = version.getData();
        var metadata = data.getMetadataBlocks().get("dansDataVaultMetadata");

        return metadata.getFields().stream()
            .filter(f -> f.getTypeName().equals("dansNbn"))
            .map(f -> (PrimitiveSingleValueField) f)
            .map(PrimitiveSingleValueField::getValue)
            .findFirst();
    }

    @Override
    public String getDatasetState(String datasetId) throws IOException, DataverseException {
        var version = dataverseClient.dataset(datasetId).getLatestVersion();
        return version.getData().getLatestVersion().getVersionState();

    }

    @Override
    public void setEmbargo(String datasetId, Instant dateAvailable, Collection<Integer> fileIds) throws IOException, DataverseException {
        var api = dataverseClient.dataset(datasetId);
        var embargo = new Embargo(dateAvailableFormat.format(Date.from(dateAvailable)), "",
            ArrayUtils.toPrimitive(fileIds.toArray(Integer[]::new)));

        api.setEmbargo(embargo);
        api.awaitUnlock(publishAwaitUnlockMaxNumberOfRetries, publishAwaitUnlockMillisecondsBetweenRetries);
    }

    @Override
    public void waitForState(String datasetId, String expectedState) throws InvalidDatasetStateException {
        var numberOfTimesTried = 0;
        var state = "";

        try {
            state = getDatasetState(datasetId);

            log.trace("Initial state for dataset {} is {}", datasetId, state, numberOfTimesTried);
            while (!expectedState.equals(state) && numberOfTimesTried < publishAwaitUnlockMaxNumberOfRetries) {
                Thread.sleep(publishAwaitUnlockMillisecondsBetweenRetries);

                state = getDatasetState(datasetId);
                numberOfTimesTried += 1;
                log.trace("Current state for dataset {} is {}, numberOfTimesTried = {}", datasetId, state, numberOfTimesTried);
            }

            if (!expectedState.equals(state)) {
                throw new InvalidDatasetStateException(String.format(
                    "Dataset did not become %s within the wait period; current state is %s", expectedState, state
                ));
            }
        }
        catch (InterruptedException e) {
            throw new InvalidDatasetStateException("Dataset state check was interrupted; last know state is " + state);
        }
        catch (IOException | DataverseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void releaseMigrated(String datasetId, String date) throws IOException, DataverseException {
        var dataset = dataverseClient.dataset(datasetId);
        var datePublishJsonLd = String.format("{\"http://schema.org/datePublished\": \"%s\"}", date);

        dataset.releaseMigrated(datePublishJsonLd, true);
        dataset.awaitUnlock(publishAwaitUnlockMaxNumberOfRetries, publishAwaitUnlockMillisecondsBetweenRetries);
    }

    @Override
    public void publishDataset(String datasetId) throws IOException, DataverseException {
        var dataset = dataverseClient.dataset(datasetId);
        var result = dataset.publish(UpdateType.major, true);
        if (log.isDebugEnabled()) {
            log.debug("Publish response message: {}", result.getEnvelopeAsJson().toPrettyString());
        }
        dataset.awaitUnlock(publishAwaitUnlockMaxNumberOfRetries, publishAwaitUnlockMillisecondsBetweenRetries);
        log.debug("Dataset {} published", datasetId);
    }

    @Override
    public void submitForReview(String persitentId) throws IOException, DataverseException {
        var dataset = dataverseClient.dataset(persitentId);
        var result = dataset.submitForReview();
        if (log.isDebugEnabled()) {
            log.debug("Response message: {}", result.getEnvelopeAsJson().toPrettyString());
        }
        // DO NOT wait for unlock, because submit for review IS a lock state
    }

    @Override
    public Optional<AuthenticatedUser> getUserById(String userId) {
        try {
            return Optional.of(dataverseClient.admin().listSingleUser(userId).getData());
        }
        catch (IOException | DataverseException e) {
            log.error("Error retrieving user with id {} from dataverse", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<String> getDatasetRoleAssignments(String userId, String datasetId) throws IOException, DataverseException {
        return dataverseClient.dataset(datasetId).listRoleAssignments()
            .getData()
            .stream()
            .filter(r -> r.getAssignee().replaceFirst("@", "").equals(userId) ||
                r.getAssignee().equals(AUTHENTICATED_USERS))
            // TODO: also check if assignee is an explicit group that contains userId as member
            .map(RoleAssignmentReadOnly::get_roleAlias)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getDataverseRoleAssignments(String userId) throws IOException, DataverseException {
        return dataverseClient.dataverse("root")
            .listRoleAssignments()
            .getData()
            .stream()
            .filter(r -> r.getAssignee().replaceFirst("@", "").equals(userId) ||
                // TODO: also check if assignee is an explicit group that contains userId as member
                r.getAssignee().equals(AUTHENTICATED_USERS)
            )
            .map(RoleAssignmentReadOnly::get_roleAlias)
            .collect(Collectors.toList());
    }

    @Override
    public DataverseClient _getClient() {
        return dataverseClient;
    }

    @Override
    public List<DatasetResultItem> searchDatasets(String key, String value) throws IOException, DataverseException {
        var query = String.format("%s:\"%s\"", key, value);

        log.trace("Searching datasets with query '{}'", query);
        var results = dataverseClient.search().find(query);
        var items = results.getData().getItems();

        return items.stream()
            .filter(r -> r instanceof DatasetResultItem)
            .map(r -> (DatasetResultItem) r)
            .collect(Collectors.toList());
    }

    @Override
    public List<URI> getLicenses() throws IOException, DataverseException {
        return dataverseClient.license().getLicenses().getData().stream()
            .map(License::getUri)
            .map(l -> {
                try {
                    return new URI(l);
                }
                catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isDatasetInReview(String persistentId) throws IOException, DataverseException {
        var r = dataverseClient.dataset(persistentId).getLocks();
        return r.getData().stream().anyMatch(l -> l.getLockType().equals("InReview"));
    }
}
