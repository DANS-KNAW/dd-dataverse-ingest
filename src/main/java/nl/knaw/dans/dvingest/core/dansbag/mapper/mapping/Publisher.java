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
package nl.knaw.dans.dvingest.core.dansbag.mapper.mapping;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.mapper.builder.CompoundFieldGenerator;
import org.w3c.dom.Node;

import java.util.Set;

import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.DISTRIBUTOR_ABBREVIATION;
import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.DISTRIBUTOR_AFFILIATION;
import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.DISTRIBUTOR_LOGO_URL;
import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.DISTRIBUTOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.DISTRIBUTOR_URL;

@Slf4j
public class Publisher extends Base {

    public static CompoundFieldGenerator<Node> toDistributorValueObject = (builder, node) -> {
        builder.addSubfield(DISTRIBUTOR_NAME, node.getTextContent());
        builder.addSubfield(DISTRIBUTOR_URL, "");
        builder.addSubfield(DISTRIBUTOR_LOGO_URL, "");
        builder.addSubfield(DISTRIBUTOR_ABBREVIATION, "");
        builder.addSubfield(DISTRIBUTOR_AFFILIATION, "");
    };

    private static final Set<String> dansNames = Set.of("DANS", "DANS-KNAW", "DANS/KNAW");

    public static boolean isDans(Node node) {
        return dansNames.contains(node.getTextContent());
    }

    public static boolean isNotDans(Node node) {
        return !isDans(node);
    }
}
