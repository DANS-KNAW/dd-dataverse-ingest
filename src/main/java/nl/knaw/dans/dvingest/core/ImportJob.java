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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nl.knaw.dans.validation.Uuid;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.nio.file.Path;

/**
 * Description of an import job. A job is a request to import a deposit or a batch of deposits.
 */
@Entity
@Table(name = "import_job")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {
    @GeneratedValue
    @Id
    private Long id;

    @Column(name = "creation_time")
    private Long creationTime;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "location")
    private String location;

    // TODO: isBatch to indicate if the location contains a single object or a batch of objects, for now always a single object
}
