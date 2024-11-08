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

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import nl.knaw.dans.dvingest.core.ImportJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ImportJobDaoTest {
    private final DAOTestExtension db = DAOTestExtension.newBuilder()
        .addEntityClass(ImportJob.class)
        .build();
    private final ImportJobDao dao = new ImportJobDao(db.getSessionFactory());

    @Test
    public void create_should_save_ImportJob() {
        var uuid = "f797a6a9-2a85-4f26-af62-b3eaae724497";

        db.inTransaction(() -> {
            var importJob = new ImportJob();
            importJob.setLocation("path");
            importJob.setUuid(uuid);
            importJob.setStatus("status");
            importJob.setCreationTime(123L);
            dao.save(importJob);
        });

        db.inTransaction(() -> {
            var result = dao.findByUuid(uuid);
            assertThat(result).isPresent();
            assertThat(result.get().getLocation()).isEqualTo("path");
            assertThat(result.get().getUuid()).isEqualTo(uuid);
            assertThat(result.get().getStatus()).isEqualTo("status");
            assertThat(result.get().getCreationTime()).isEqualTo(123L);
        });
    }

}
