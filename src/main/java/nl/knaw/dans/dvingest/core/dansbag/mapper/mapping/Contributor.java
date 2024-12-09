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

import java.util.HashMap;
import java.util.Map;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.CONTRIBUTOR_NAME;

@Slf4j
public final class Contributor extends Base {
    public static Map<String, String> contributorRoleToContributorType = new HashMap<>();

    public static CompoundFieldGenerator<Node> toContributorValueObject = (builder, node) -> {
        if ("contributorDetails".equals(node.getLocalName())) {

            getChildNode(node, "dcx-dai:author")
                .filter(n -> !DcxDaiAuthor.isRightsHolder(n))
                .ifPresent(n -> DcxDaiAuthor.toContributorValueObject.build(builder, n));

            getChildNode(node, "dcx-dai:organization")
                .filter(n -> !DcxDaiOrganization.isRightsHolderOrFunder(n))
                .ifPresent(n -> DcxDaiOrganization.toContributorValueObject.build(builder, n));
        }
        else if ("description".equals(node.getLocalName()) || "contributor".equals(node.getLocalName())) {
            builder.addSubfield(CONTRIBUTOR_NAME, node.getTextContent());
        }
    };

    static {
        /*
         * The comments contain the term from the DataCite 4 contributorType vocabulary. This term is mapped to a Dataverse contributorRole,
         * which is based on DataCite 3. If there is no DataCite 3 term, "Other" is used.
         */

        // <xs:enumeration value="ContactPerson" /> NOT MAPPABLE TO DATACITE 3
        contributorRoleToContributorType.put("ContactPerson", "Other");

        // <xs:enumeration value="DataCollector" />
        contributorRoleToContributorType.put("DataCollector", "Data Collector");

        // <xs:enumeration value="DataCurator" />
        contributorRoleToContributorType.put("DataCurator", "Data Curator");

        // <xs:enumeration value="DataManager" />
        contributorRoleToContributorType.put("DataManager", "Data Manager");

        // <xs:enumeration value="Distributor" /> NOT MAPPABLE TO DATACITE 3
        contributorRoleToContributorType.put("Distributor", "Other");

        // <xs:enumeration value="Editor" />
        contributorRoleToContributorType.put("Editor", "Editor");

        // Not in DataCite 4, so should not actually be in the input
        contributorRoleToContributorType.put("Funder", "Funder");

        // <xs:enumeration value="HostingInstitution" />
        contributorRoleToContributorType.put("HostingInstitution", "Hosting Institution");

        // <xs:enumeration value="Other" />
        contributorRoleToContributorType.put("Other", "Other");

        // <xs:enumeration value="Producer" /> NOT MAPPABLE TO DATACITE 3
        contributorRoleToContributorType.put("Producer", "Other");

        // <xs:enumeration value="ProjectLeader" />
        contributorRoleToContributorType.put("ProjectLeader", "Project Leader");

        // <xs:enumeration value="ProjectManager" />
        contributorRoleToContributorType.put("ProjectManager", "Project Manager");

        // <xs:enumeration value="ProjectMember" />
        contributorRoleToContributorType.put("ProjectMember", "Project Member");

        // <xs:enumeration value="RegistrationAgency" /> NOT MAPPABLE TO DATACITE 3
        contributorRoleToContributorType.put("RegistrationAuthority", "Other");

        // <xs:enumeration value="RegistrationAuthority" /> NOT MAPPABLE TO DATACITE 3
        contributorRoleToContributorType.put("RegistrationAgency", "Other");

        // <xs:enumeration value="RelatedPerson" />
        contributorRoleToContributorType.put("RelatedPerson", "Related Person");

        // <xs:enumeration value="ResearchGroup" />
        contributorRoleToContributorType.put("ResearchGroup", "Research Group");

        // <xs:enumeration value="RightsHolder" />
        contributorRoleToContributorType.put("RightsHolder", "Rights Holder");

        // <xs:enumeration value="Researcher" />
        contributorRoleToContributorType.put("Researcher", "Researcher");

        // <xs:enumeration value="Sponsor" />
        contributorRoleToContributorType.put("Sponsor", "Sponsor");

        // <xs:enumeration value="Supervisor" />
        contributorRoleToContributorType.put("Supervisor", "Supervisor");

        // <xs:enumeration value="WorkPackageLeader" />
        contributorRoleToContributorType.put("WorkPackageLeader", "Work Package Leader");
    }

    public static boolean isValidContributor(Node node) {

        var isValidAuthor = getChildNode(node, "dcx-dai:author")
            .filter(n -> !DcxDaiAuthor.isRightsHolder(n))
            .map(DcxDaiAuthor::isValidContributor)
            .orElse(false);

        var isValidOrg = getChildNode(node, "dcx-dai:organization")
            .filter(n -> !DcxDaiOrganization.isRightsHolderOrFunder(n))
            .map(DcxDaiOrganization::isValidContributor)
            .orElse(false);

        return isValidAuthor || isValidOrg;
    }

}
