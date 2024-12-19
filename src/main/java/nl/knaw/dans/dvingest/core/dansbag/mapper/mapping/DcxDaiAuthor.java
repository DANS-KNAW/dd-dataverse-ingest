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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.mapper.builder.CompoundFieldGenerator;
import nl.knaw.dans.dvingest.core.dansbag.xml.XPathEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Optional;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_AFFILIATION;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_IDENTIFIER;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_IDENTIFIER_SCHEME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.Contributor.contributorRoleToContributorType;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.IdUriHelper.reduceUriToId;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.IdUriHelper.reduceUriToOrcidId;

@Slf4j
public final class DcxDaiAuthor extends Base {
    @Data
    @Builder
    public static class DatasetAuthor {
        private String titles;
        private String initials;
        private String insertions;
        private String surname;
        private String dai;
        private String isni;
        private String orcid;
        private String role;
        private String organization;
    }

    public static CompoundFieldGenerator<Node> toAuthorValueObject = (builder, node) -> {
        var author = parseAuthor(node);
        var name = formatName(author);

        if (StringUtils.isNotBlank(name)) {
            builder.addSubfield(AUTHOR_NAME, name.trim());
        }

        if (author.getOrcid() != null) {
            builder.addControlledSubfield(AUTHOR_IDENTIFIER_SCHEME, "ORCID");
            builder.addSubfield(AUTHOR_IDENTIFIER, reduceUriToId(author.getOrcid()));
        }

        else if (author.getIsni() != null) {
            builder.addControlledSubfield(AUTHOR_IDENTIFIER_SCHEME, "ISNI");
            builder.addSubfield(AUTHOR_IDENTIFIER, reduceUriToId(author.getIsni()));
        }

        else if (author.getDai() != null) {
            builder.addControlledSubfield(AUTHOR_IDENTIFIER_SCHEME, "DAI");
            builder.addSubfield(AUTHOR_IDENTIFIER, author.getDai());
        }

        if (author.getOrganization() != null) {
            builder.addSubfield(AUTHOR_AFFILIATION, author.getOrganization());
        }
    };
    public static CompoundFieldGenerator<Node> toContributorValueObject = (builder, node) -> {
        var author = parseAuthor(node);
        var name = formatName(author);

        if (StringUtils.isNotBlank(name)) {
            var completeName = author.getOrganization() != null
                ? String.format("%s (%s)", name, author.getOrganization())
                : name;

            builder.addSubfield(CONTRIBUTOR_NAME, completeName);
        }
        else if (StringUtils.isNotBlank(author.getOrganization())) {
            builder.addSubfield(CONTRIBUTOR_NAME, author.getOrganization());
        }
        if (StringUtils.isNotBlank(author.getRole())) {
            var value = contributorRoleToContributorType.getOrDefault(author.getRole(), "Other");
            builder.addControlledSubfield(CONTRIBUTOR_TYPE, value);
        }
    };

    public static boolean isValidContributor(Node node) {
        var author = parseAuthor(node);
        var name = formatName(author);

        return (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(author.getOrganization())) || StringUtils.isNotBlank(author.getRole());
    }

    static String formatName(DatasetAuthor author) {
        return String.join(" ", List.of(
                Optional.ofNullable(author.getInitials()).orElse(""),
                Optional.ofNullable(author.getInsertions()).orElse(""),
                Optional.ofNullable(author.getSurname()).orElse("")
            ))
            .trim().replaceAll("\\s+", " ");
    }

    static String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    static DatasetAuthor parseAuthor(Node node) {
        return DatasetAuthor.builder()
            .titles(getFirstValue(node, "dcx-dai:titles"))
            .initials(getFirstValue(node, "dcx-dai:initials"))
            .insertions(getFirstValue(node, "dcx-dai:insertions"))
            .surname(getFirstValue(node, "dcx-dai:surname"))
            .dai(getFirstValue(node, "dcx-dai:DAI"))
            .isni(reduceUriToId(getFirstValue(node, "dcx-dai:ISNI")))
            .orcid(reduceUriToOrcidId(getFirstValue(node, "dcx-dai:ORCID")))
            .role(getFirstValue(node, "dcx-dai:role"))
            .organization(getFirstValue(node, "dcx-dai:organization/dcx-dai:name"))
            .build();
    }

    public static boolean isRightsHolder(Node node) {
        var organization = parseAuthor(node);
        return organization.getRole() != null && "RightsHolder".equals(organization.getRole().trim());
    }

    public static String toRightsHolder(Node node) {
        var author = parseAuthor(node);
        return formatRightsHolder(author);
    }

    static String formatRightsHolder(DatasetAuthor author) {
        if (author.getSurname() == null || author.getSurname().isBlank()) {
            return Optional.ofNullable(author.getOrganization()).orElse("");
        }

        return String.join(" ", List.of(
                Optional.ofNullable(author.getTitles()).orElse(""),
                Optional.ofNullable(author.getInitials()).orElse(""),
                Optional.ofNullable(author.getInsertions()).orElse(""),
                Optional.ofNullable(author.getSurname()).orElse(""),
                Optional.ofNullable(author.getOrganization())
                    .map(s -> String.format("(%s)", s)).orElse("")
            ))
            .trim().replaceAll("\\s+", " ");
    }
}
