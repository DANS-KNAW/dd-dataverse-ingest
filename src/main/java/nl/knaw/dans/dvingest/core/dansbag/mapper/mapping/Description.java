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

import nl.knaw.dans.dvingest.core.dansbag.mapper.builder.CompoundFieldGenerator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.DESCRIPTION_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SERIES_INFORMATION;

public class Description extends Base {

    private static final Map<String, String> labelToPrefix = Map.of(
        "date", "Date",
        "valid", "Valid",
        "issued", "Issued",
        "modified", "Modified",
        "dateAccepted", "Date Accepted",
        "dateCopyrighted", "Date Copyrighted",
        "dateSubmitted", "Date Submitted",
        "coverage", "Coverage"
    );
    public static CompoundFieldGenerator<Node> toDescription = (builder, value) -> {
        var text = newlineToHtml(value.getTextContent());
        builder.addSubfield(DESCRIPTION_VALUE, text);
    };
    public static CompoundFieldGenerator<Node> toPrefixedDescription = (builder, value) -> {
        var name = value.getLocalName();
        var prefix = labelToPrefix.getOrDefault(name, name);

        builder.addSubfield(DESCRIPTION_VALUE, String.format("%s: %s", prefix, value.getTextContent()));
    };

    static public CompoundFieldGenerator<Node> toSeries = (builder, value) -> {
        var text = newlineToHtml(value.getTextContent());
        builder.addSubfield(SERIES_INFORMATION, text);
    };


    private static String newlineToHtml(String value) {
        var newline = "\r\n|\n|\r";
        var paragraph = "(\r\n){2,}|\n{2,}|\r{2,}";

        return Arrays.stream(value.trim().split(paragraph))
            .map(String::trim)
            .map(p -> String.format("<p>%s</p>", p))
            .map(p -> Arrays.stream(p.split(newline))
                .map(String::trim)
                .collect(Collectors.joining("<br>")))
            .collect(Collectors.joining(""));
    }

    public static boolean isNotBlank(Node node) {
        return StringUtils.isNotBlank(node.getTextContent());
    }

    public static boolean isNotMapped(Node node) {
        var descriptionType = getAttribute(node, "descriptionType");
        return descriptionType.isEmpty() || !List.of("Other", "SeriesInformation").contains(descriptionType.get().getTextContent());
    }

    public static boolean isSeriesInformation(Node node) {
        return hasAttributeValue(node, "descriptionType", "SeriesInformation") && isNotBlank(node);
    }

    public static boolean hasDescriptionTypeOther(Node node) {
        return hasAttributeValue(node, "descriptionType", "Other") && isNotBlank(node);
    }
}
