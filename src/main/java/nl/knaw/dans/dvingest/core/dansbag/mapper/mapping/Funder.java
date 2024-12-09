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

import lombok.Builder;
import lombok.Data;
import nl.knaw.dans.dvingest.core.dansbag.mapper.builder.CompoundFieldGenerator;
import nl.knaw.dans.dvingest.core.dansbag.xml.XPathEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.GRANT_NUMBER_AGENCY;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.GRANT_NUMBER_VALUE;

public class Funder {

    public static CompoundFieldGenerator<Node> toGrantNumberValueObject = (builder, value) -> {
        var details = parseFunderDetails(value);
        // create a string like 'awardNumber (awardTitle)' but also check for empty values
        var grantNumberValue = Stream.of(
                details.getFundingProgramme(),
                details.getAwardNumber()
            )
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "))
            .trim();

        builder.addSubfield(GRANT_NUMBER_AGENCY, details.getFunderName());
        builder.addSubfield(GRANT_NUMBER_VALUE, grantNumberValue);
    };

    static String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    static FunderDetails parseFunderDetails(Node node) {
        return FunderDetails.builder()
            .funderName(getFirstValue(node, "ddm:funderName"))
            .fundingProgramme(getFirstValue(node, "ddm:fundingProgramme"))
            .awardNumber(getFirstValue(node, "ddm:awardNumber"))
            .awardTitle(getFirstValue(node, "ddm:awardTitle"))
            .build();
    }

    @Data
    @Builder
    public static class FunderDetails {
        private String funderName;
        private String fundingProgramme;
        private String awardNumber;
        private String awardTitle;
    }
}
