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
package nl.knaw.dans.ingest.core.service.mapper.mapping;

import nl.knaw.dans.ingest.core.service.mapper.builder.CompoundFieldGenerator;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.lang3.StringUtils;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT_AFFILIATION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT_EMAIL;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT_NAME;

public class Contact {

    public static CompoundFieldGenerator<AuthenticatedUser> toContactValue = (builder, value) -> {
        builder.addSubfield(DATASET_CONTACT_NAME, value.getDisplayName());
        builder.addSubfield(DATASET_CONTACT_EMAIL, value.getEmail());

        if (StringUtils.isNotBlank(value.getAffiliation())) {
            builder.addSubfield(DATASET_CONTACT_AFFILIATION, value.getAffiliation());
        }
    };
}
