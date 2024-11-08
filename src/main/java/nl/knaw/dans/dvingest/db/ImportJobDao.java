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
package nl.knaw.dans.dvingest.db;

import io.dropwizard.hibernate.AbstractDAO;
import nl.knaw.dans.dvingest.core.ImportJob;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.OptimisticLockException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Optional;

public class ImportJobDao extends AbstractDAO<ImportJob> {
    public ImportJobDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public ImportJob save(ImportJob importJob) {
        try {
            if (importJob.getId() == null || get(importJob.getId()) == null) {
                persist(importJob);
            }
            else {
                currentSession().update(importJob);
            }
            return importJob;
        }
        catch (ConstraintViolationException e) {
            throw new IllegalArgumentException(e.getSQLException().getMessage());
        }
        catch (OptimisticLockException e) {
            throw new IllegalStateException("Failed to update ImportJob due to concurrent modification", e);
        }
    }

    public Optional<ImportJob> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Optional<ImportJob> findById(long id) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ImportJob> query = builder.createQuery(ImportJob.class);
        Root<ImportJob> root = query.from(ImportJob.class);
        query.select(root).where(builder.equal(root.get("id"), id));

        return Optional.ofNullable(currentSession().createQuery(query).uniqueResult());
    }

    public Optional<ImportJob> getNextJob() {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ImportJob> query = builder.createQuery(ImportJob.class);
        Root<ImportJob> root = query.from(ImportJob.class);
        query.select(root).where(builder.equal(root.get("status"), "PENDING")).orderBy(builder.desc(root.get("creationTime")));

        return Optional.ofNullable(currentSession().createQuery(query).setMaxResults(1).uniqueResult());
    }

}
