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
import nl.knaw.dans.ingest.core.domain.DatasetOrganization;
import nl.knaw.dans.ingest.core.service.XPathEvaluator;
import nl.knaw.dans.dvingest.core.dansbag.mapper.builder.CompoundFieldGenerator;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import java.util.Set;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.AUTHOR_IDENTIFIER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.AUTHOR_IDENTIFIER_SCHEME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.AUTHOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER_AGENCY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.IdUriHelper.reduceUriToId;

@Slf4j
public final class DcxDaiOrganization {

    public static CompoundFieldGenerator<Node> toAuthorValueObject = (builder, node) -> {
        var organization = parseOrganization(node);

        if (StringUtils.isNotBlank(organization.getName())) {
            builder.addSubfield(AUTHOR_NAME, organization.getName());
        }

        if (organization.getIsni() != null) {
            builder.addControlledSubfield(AUTHOR_IDENTIFIER_SCHEME, "ISNI");
            builder.addSubfield(AUTHOR_IDENTIFIER, organization.getIsni());
        }
        else if (organization.getViaf() != null) {
            builder.addControlledSubfield(AUTHOR_IDENTIFIER_SCHEME, "VIAF");
            builder.addSubfield(AUTHOR_IDENTIFIER, organization.getViaf());
        }
    };

    public static CompoundFieldGenerator<Node> toContributorValueObject = (builder, node) -> {
        var organization = parseOrganization(node);

        if (StringUtils.isNotBlank(organization.getName())) {
            builder.addSubfield(CONTRIBUTOR_NAME, organization.getName());
        }
        if (StringUtils.isNotBlank(organization.getRole())) {
            var value = Contributor.contributorRoleToContributorType.getOrDefault(organization.getRole(), "Other");
            builder.addControlledSubfield(CONTRIBUTOR_TYPE, value);
        }
    };
    public static CompoundFieldGenerator<Node> toGrantNumberValueObject = (builder, node) -> {
        var org = parseOrganization(node);
        builder.addSubfield(GRANT_NUMBER_AGENCY, org.getName().trim());
        builder.addSubfield(GRANT_NUMBER_VALUE, "");
    };

    public static boolean isValidContributor(Node node) {
        var org = parseOrganization(node);
        return StringUtils.isNotBlank(org.getName()) || StringUtils.isNotBlank(org.getRole());
    }

    private static String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    private static DatasetOrganization parseOrganization(Node node) {
        return DatasetOrganization.builder()
            .name(getFirstValue(node, "dcx-dai:name"))
            .role(getFirstValue(node, "dcx-dai:role"))
            .isni(reduceUriToId(getFirstValue(node, "dcx-dai:ISNI")))
            .viaf(reduceUriToId(getFirstValue(node, "dcx-dai:VIAF")))
            .build();
    }

    public static boolean isRightsHolderOrFunder(Node node) {
        var organization = parseOrganization(node);
        return organization.getRole() != null && Set.of("RightsHolder", "Funder").contains(organization.getRole());
    }

    public static boolean isRightsHolder(Node node) {
        var organization = parseOrganization(node);
        return organization.getRole() != null && "RightsHolder".equals(organization.getRole().trim());
    }

    public static String toRightsHolder(Node node) {
        var organization = parseOrganization(node);
        return organization.getName();
    }

    public static boolean isFunder(Node node) {
        var organization = parseOrganization(node);
        return organization.getRole() != null && "Funder".equals(organization.getRole().trim());
    }
}
