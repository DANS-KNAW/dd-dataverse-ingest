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
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.RELATION_TEXT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.RELATION_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.RELATION_URI;

public class Relation extends Base {

    private static final Map<String, String> labelToType = new HashMap<>();
    public static CompoundFieldGenerator<Node> toRelationObject = (builder, node) -> {
        var href = getAttribute(node, "href")
            .map(Node::getTextContent)
            .orElse("");

        var nodeName = node.getLocalName();

        builder.addControlledSubfield(RELATION_TYPE, labelToType.getOrDefault(nodeName, nodeName));
        builder.addSubfield(RELATION_URI, href);
        builder.addSubfield(RELATION_TEXT, node.getTextContent());
    };

    static {
        labelToType.put("relation", "relation");
        labelToType.put("conformsTo", "conforms to");
        labelToType.put("hasFormat", "has format");
        labelToType.put("hasPart", "has part");
        labelToType.put("references", "references");
        labelToType.put("replaces", "replaces");
        labelToType.put("requires", "requires");
        labelToType.put("hasVersion", "has version");
        labelToType.put("isFormatOf", "is format of");
        labelToType.put("isPartOf", "is part of");
        labelToType.put("isReferencedBy", "is referenced by");
        labelToType.put("isReplacedBy", "is replaced by");
        labelToType.put("isRequiredBy", "is required by");
        labelToType.put("isVersionOf", "is version of");
    }

    public static boolean isRelation(Node node) {
        return labelToType.containsKey(node.getLocalName());
    }

}
