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

public class Author extends Base {

    public static CompoundFieldGenerator<Node> toAuthorValueObject = (builder, node) -> {

        var author = getChildNode(node, "dcx-dai:author");
        var organization = getChildNode(node, "dcx-dai:organization");

        var localName = node.getLocalName();

        if (localName.equals("creatorDetails") && author.isPresent()) {
            DcxDaiAuthor.toAuthorValueObject.build(builder, author.get());
        }
        else if (localName.equals("creatorDetails") && organization.isPresent()) {
            DcxDaiOrganization.toAuthorValueObject.build(builder, organization.get());
        }
        else if (localName.equals("creator")) {
            Creator.toAuthorValueObject.build(builder, node);
        }
    };

}
