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

import org.w3c.dom.Node;

public class SubjectAat extends Base {
    public static final String SCHEME_AAT = "Art and Architecture Thesaurus";
    public static final String SCHEME_URI_AAT = "http://vocab.getty.edu/aat/";

    public static boolean isAatTerm(Node node) {
        return node.getLocalName().equals("subject")
            && hasAttributeValue(node, "subjectScheme", SCHEME_AAT)
            && hasAttributeValue(node, "schemeURI", SCHEME_URI_AAT);
    }

    public static String toAatClassification(Node node) {
        return getValueUri(node);
    }
}
