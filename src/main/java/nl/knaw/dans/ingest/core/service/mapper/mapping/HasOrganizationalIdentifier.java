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
import org.apache.commons.lang3.StringUtils;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID_AGENCY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID_VALUE;

public class HasOrganizationalIdentifier {
    public static CompoundFieldGenerator<String> toOtherIdValue = (builder, value) -> {
        var parts = value.split(":", 2);
        builder.addSubfield(OTHER_ID_AGENCY, parts[0]);
        builder.addSubfield(OTHER_ID_VALUE, parts[1]);
    };

    public static boolean isValidOtherIdValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            var parts = value.split(":", 2);
            return parts.length == 2 && isNonEmptyStringArray(parts);
        }

        return false;
    }

    private static boolean isNonEmptyStringArray(String[] values) {
        for (var value : values) {
            if (StringUtils.isBlank(value)) {
                return false;
            }
        }

        return true;
    }
}
