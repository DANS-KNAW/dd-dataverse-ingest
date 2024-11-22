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

import io.dropwizard.hibernate.UnitOfWork;
import nl.knaw.dans.ingest.core.BlockedTarget;
import nl.knaw.dans.ingest.core.exception.TargetBlockedException;
import nl.knaw.dans.ingest.core.exception.TargetNotFoundException;
import nl.knaw.dans.ingest.db.BlockedTargetDAO;

public class BlockedTargetServiceImpl implements BlockedTargetService {
    private final BlockedTargetDAO blockedTargetDAO;

    public BlockedTargetServiceImpl(BlockedTargetDAO blockedTargetDAO) {
        this.blockedTargetDAO = blockedTargetDAO;
    }

    @Override
    @UnitOfWork
    public void unblockTarget(String target) throws TargetNotFoundException {
        var records = blockedTargetDAO.getTarget(target);

        if (records.size() == 0) {
            throw new TargetNotFoundException("Target not found");
        }

        for (var record : records) {
            blockedTargetDAO.delete(record);
        }
    }

    @Override
    @UnitOfWork
    public void blockTarget(String target) throws TargetBlockedException {
        var existing = blockedTargetDAO.getTarget(target);

        if (existing.size() > 0) {
            throw new TargetBlockedException("Target is already blocked");
        }

        blockedTargetDAO.save(new BlockedTarget(null, target, null, null));
    }

    @Override
    @UnitOfWork
    public void blockTarget(String depositId, String target, String state, String message) {
        blockedTargetDAO.save(new BlockedTarget(depositId, target, state, message));
    }

    @Override
    @UnitOfWork
    public boolean isBlocked(String target) {
        var targets = blockedTargetDAO.getTarget(target);
        return targets.size() > 0;
    }
}