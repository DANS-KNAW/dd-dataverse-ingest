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
package nl.knaw.dans.ingest.core.deposit;

import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.exception.InvalidDepositException;
import nl.knaw.dans.ingest.core.io.BagDataManager;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class DepositWriterImpl implements DepositWriter {
    private final BagDataManager bagDataManager;

    public DepositWriterImpl(BagDataManager bagDataManager) {
        this.bagDataManager = bagDataManager;
    }

    @Override
    public void saveDeposit(Deposit deposit) throws InvalidDepositException {
        var config = new HashMap<String, Object>();
        config.put("state.label", deposit.getState().toString());
        config.put("state.description", deposit.getStateDescription());
        config.put("identifier.doi", deposit.getDoi());
        config.put("identifier.urn", deposit.getUrn());

        try {
            bagDataManager.saveDepositProperties(deposit.getDir(), config);
        }
        catch (ConfigurationException cex) {
            throw new InvalidDepositException("Unable to save deposit properties", cex);
        }
    }

    @Override
    public void moveDeposit(Deposit deposit, Path outbox) throws IOException {
        moveDeposit(deposit.getDir(), outbox);
    }

    @Override
    public void moveDeposit(Path source, Path outbox) throws IOException {
        Files.move(source, outbox.resolve(source.getFileName()));
    }

    @Override
    public void saveBagInfo(Deposit deposit) throws IOException {
        bagDataManager.writeBagMetadata(deposit.getBag());
    }
}
