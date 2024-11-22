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

import nl.knaw.dans.ingest.core.service.XmlReader;
import nl.knaw.dans.ingest.core.service.mapper.builder.CompoundFieldGenerator;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.Optional;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ARCHIS_NUMBER_ID;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ARCHIS_NUMBER_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER_AGENCY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID_AGENCY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_CITATION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_ID_NUMBER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_ID_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_URL;
import static nl.knaw.dans.ingest.core.service.XmlReader.NAMESPACE_XSI;

public class Identifier extends Base {

    public static final CompoundFieldGenerator<Node> toRelatedPublicationValue = (builder, node) -> {
        var text = node.getTextContent().trim();

        builder.addSubfield(PUBLICATION_CITATION, "");
        builder.addSubfield(PUBLICATION_ID_NUMBER, text);
        builder.addControlledSubfield(PUBLICATION_ID_TYPE, getIdType(node));
        builder.addSubfield(PUBLICATION_URL, "");
    };

    public static final CompoundFieldGenerator<Node> toNwoGrantNumber = (builder, node) -> {
        var text = node.getTextContent().trim();

        builder.addSubfield(GRANT_NUMBER_AGENCY, "NWO");
        builder.addSubfield(GRANT_NUMBER_VALUE, text);
    };

    public static final CompoundFieldGenerator<Node> toOtherIdValue = (builder, node) -> {
        var text = node.getTextContent().trim();

        if (hasXsiType(node, "EASY2")) {
            builder.addSubfield(OTHER_ID_AGENCY, "DANS-KNAW");
            builder.addSubfield(OTHER_ID_VALUE, text);
        }
        else if (!hasAttribute(node, XmlReader.NAMESPACE_XSI, "type")) {
            builder.addSubfield(OTHER_ID_AGENCY, "");
            builder.addSubfield(OTHER_ID_VALUE, text);
        }
    };

    private final static Map<String, String> archisNumberTypeToCvItem = Map.of(
        "ARCHIS-ONDERZOEK", "onderzoek",
        "ARCHIS-VONDSTMELDING", "vondstmelding",
        "ARCHIS-MONUMENT", "monument",
        "ARCHIS-WAARNEMING", "waarneming"
    );

    public static final CompoundFieldGenerator<Node> toArchisNumberValue = (builder, node) -> {
        var numberType = getAttribute(node, NAMESPACE_XSI, "type")
            .map(Node::getTextContent)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("Archis number without type"));

        var numberId = Optional.ofNullable(node.getTextContent())
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("Archis number without ID"));

        var type = archisNumberTypeToCvItem.get(numberType.substring(numberType.indexOf(':') + 1));
        builder.addControlledSubfield(ARCHIS_NUMBER_TYPE, type);
        builder.addSubfield(ARCHIS_NUMBER_ID, numberId);
    };

    private static String getIdType(Node node) {
        return Optional.ofNullable(node.getAttributes().getNamedItemNS(NAMESPACE_XSI, "type"))
            .map(item -> {
                var text = item.getTextContent();
                return text.substring(text.indexOf(':') + 1);
            })
            .map(String::toLowerCase)
            .orElse("");
    }

    public static boolean hasXsiTypeDoi(Node node) {
        return hasXsiType(node, "DOI");
    }

    public static boolean isArchisZaakId(Node node) {
        return hasXsiType(node, "ARCHIS-ZAAK-IDENTIFICATIE");
    }

    public static boolean isRelatedPublication(Node node) {
        return hasXsiType(node, "ISBN") || hasXsiType(node, "ISSN");
    }

    public static String toArchisZaakId(Node node) {
        return node.getTextContent();
    }

    public static boolean hasNoXsiType(Node node) {
        return !hasAttribute(node, NAMESPACE_XSI, "type");
    }

    public static boolean hasXsiTypeEasy2(Node node) {
        return hasXsiType(node, "EASY2");
    }

    public static boolean isArchisNumber(Node node) {
        return hasXsiType(node, "ARCHIS-ONDERZOEK") ||
            hasXsiType(node, "ARCHIS-VONDSTMELDING") ||
            hasXsiType(node, "ARCHIS-MONUMENT") ||
            hasXsiType(node, "ARCHIS-WAARNEMING");
    }

    public static boolean isNwoGrantNumber(Node node) {
        return hasXsiType(node, "NWO-PROJECTNR");
    }
}
