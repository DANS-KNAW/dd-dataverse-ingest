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
package nl.knaw.dans.dvingest.core.dansbag.deposit;

import nl.knaw.dans.dvingest.core.dansbag.domain.Deposit;
import nl.knaw.dans.dvingest.core.dansbag.domain.DepositLocation;
import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;

import java.io.IOException;
import java.nio.file.Path;

// an adapter around DepositReader and DepositWriter

/**
 * An adapter class around several functionalities regarding deposit reading, writing and moving.
 */
public interface DepositManager {
    void saveBagInfo(Deposit deposit) throws IOException;

    /**
     * Reads a Deposit object given the data found in a DepositLocation's <code>dir</code> property.
     *
     * @param location The DepositLocation that refers to a deposit directory
     * @return A fully read Deposit
     * @throws InvalidDepositException
     */
    Deposit readDeposit(DepositLocation location) throws InvalidDepositException;

    /**
     * Reads a DepositLocation from the provided path. This is different from a regular Deposit in that it only contains the minimal amount of data required to sort and group tasks. This data is:
     * <pre>
     *     - A path
     *     - The created date
     *     - The target, either a sword token or a DOI
     *     - The ID of the deposit
     * </pre>
     *
     * @param path
     * @return A DepositLocation object
     * @throws InvalidDepositException
     * @throws IOException
     */
    DepositLocation readDepositLocation(Path path) throws InvalidDepositException, IOException;

    /**
     * Update the deposit.properties file with the data found in this Deposit, and move it to the target path. The path should NOT include the deposit ID.
     *
     * @param deposit The Deposit that should be written.
     * @param outbox  The path that this deposit should move to after saving the properties. This path should NOT include the deposit ID.
     * @throws IOException
     * @throws InvalidDepositException
     */
    void updateAndMoveDeposit(Deposit deposit, Path outbox) throws IOException, InvalidDepositException;

    /**
     * Move the first path to the target path. The target path should NOT include the deposit ID. This method should only be used if an exception occurred while reading the deposit, making it
     * impossible to read/write to the deposit.properties file.
     *
     * @param depositDir The deposit dir that was attempted to be read
     * @param outbox     The path to which the deposit dir should be moved
     * @throws IOException
     */
    void moveDeposit(Path depositDir, Path outbox) throws IOException;
}
