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
package nl.knaw.dans.dvingest.core;

import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Deposit {
    String UPDATES_DATASET_KEY = "updates-dataset";

    /**
     * Convert the deposit to a DANS deposit if necessary. It is necessary if a DANS bag is required; in this case the deposit fails if it is not a DANS deposit. If a DANS bag is not required, but the
     * bag in the deposit still looks like a DANS bag, conversion is also deemed necessary.
     *
     * @return true if the conversion was necessary and successful, false if not necessary.
     * @throws RuntimeException if the conversion was necessary but failed.
     */
    boolean convertDansDepositIfNeeded();

    /*
     * Gets the PID of the dataset that is updated by this deposit, if any.
     */
    String getUpdatesDataset();

    /**
     * Get the bags in the deposit.
     *
     * @return the bags in the deposit
     * @throws IOException if the bags cannot be read
     */
    List<DataverseIngestBag> getBags() throws IOException;

    /**
     * Get the ID of the deposit.
     *
     * @return the ID of the deposit
     */
    UUID getId();

    /**
     * Get the location of the deposit.
     *
     * @return the location of the deposit
     */
    Path getLocation();

    /**
     * Executed after the deposit was successfully processed.
     *
     * @param pid     the PID of the dataset that was created or updated
     * @param message a message to be logged
     */
    void onSuccess(@NonNull String pid, String message);

    /**
     * Executed after the deposit failed to be processed. If the deposit contains multiple bags, possibly some of them were processed successfully.
     *
     * @param pid     the PID of the dataset that was created or updated
     * @param message a message to be logged
     */
    void onFailed(String pid, String message);

    /**
     * Executed after the deposit was rejected. If the deposit contains multiple bags, possibly some of them were processed successfully.
     *
     * @param pid     the PID of the dataset that was created or updated
     * @param message a message to be logged
     */
    void onRejected(String pid, String message);

    /**
     * Move the deposit to a new location.
     *
     * @param toPath the new location
     * @throws IOException if the deposit cannot be moved
     */
    void moveTo(Path toPath) throws IOException;

    /**
     * Verify that the deposit conforms to the requirements of its type.
     * TODO: implement validation for DataverseIngestDeposit
     */
    void validate();

    /**
     * Check if the user is authorized to perform the actions in the deposit.
     */
    void checkAuthorized();
}
