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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.ingest.core.service.mapper.builder.CompoundFieldGenerator;
import org.w3c.dom.Node;

import java.util.regex.Pattern;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATE_OF_COLLECTION_END;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATE_OF_COLLECTION_START;

@Slf4j
public class DatesOfCollection extends Base {

    private static final Pattern DATES_OF_COLLECTION_PATTERN = Pattern.compile("^(.*)/(.*)$");

    public static CompoundFieldGenerator<Node> toDateOfCollectionValue = (builder, node) -> {
        var matches = DATES_OF_COLLECTION_PATTERN.matcher(node.getTextContent().trim());

        if (matches.matches()) {
            builder.addSubfield(DATE_OF_COLLECTION_START, matches.group(1));
            builder.addSubfield(DATE_OF_COLLECTION_END, matches.group(2));
        }
    };

    public static boolean isValidDatesOfCollectionPattern(Node node) {
        var text = node.getTextContent().trim();
        var matcher = DATES_OF_COLLECTION_PATTERN.matcher(text.trim());

        return matcher.matches();
    }
}
