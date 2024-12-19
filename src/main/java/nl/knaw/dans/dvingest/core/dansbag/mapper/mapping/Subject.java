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

import java.util.regex.Pattern;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD_VALUE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD_VOCABULARY;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.KEYWORD_VOCABULARY_URI;

@Slf4j
public class Subject extends Base {
    final public static String SCHEME_PAN = "PAN thesaurus ideaaltypes";
    final public static String SCHEME_URI_PAN = "https://data.cultureelerfgoed.nl/term/id/pan/PAN";

    final public static String SCHEME_AAT = "Art and Architecture Thesaurus";
    final public static String SCHEME_URI_AAT = "http://vocab.getty.edu/aat/";

    private final static Pattern matchPrefix = Pattern.compile("^\\s*[a-zA-Z]+\\s+Match:\\s*");
    public static CompoundFieldGenerator<Node> toKeywordValue = (builder, value) -> {
        builder.addSubfield(KEYWORD_VALUE, value.getTextContent().trim());
    };
    public static CompoundFieldGenerator<Node> toPanKeywordValue = (builder, value) -> {
        builder.addSubfield(KEYWORD_VALUE, removeMatchPrefix(value.getTextContent().trim()));
        builder.addSubfield(KEYWORD_VOCABULARY, SCHEME_PAN);
        builder.addSubfield(KEYWORD_VOCABULARY_URI, SCHEME_URI_PAN);
    };
    public static CompoundFieldGenerator<Node> toAatKeywordValue = (builder, value) -> {
        builder.addSubfield(KEYWORD_VALUE, removeMatchPrefix(value.getTextContent().trim()));
        builder.addSubfield(KEYWORD_VOCABULARY, SCHEME_AAT);
        builder.addSubfield(KEYWORD_VOCABULARY_URI, SCHEME_URI_AAT);
    };

    static String removeMatchPrefix(String input) {
        return matchPrefix.matcher(input).replaceAll("");
    }

    public static boolean hasNoCvAttributes(Node node) {
        var ss = getAttribute(node, "subjectScheme")
            .map(Node::getTextContent)
            .orElse("")
            .isEmpty();

        var su = getAttribute(node, "schemeURI")
            .map(Node::getTextContent)
            .orElse("")
            .isEmpty();

        return ss && su;
    }

    public static boolean isPanTerm(Node node) {
        return node.getLocalName().equals("subject")
            && hasAttributeValue(node, "subjectScheme", SCHEME_PAN)
            && hasAttributeValue(node, "schemeURI", SCHEME_URI_PAN);
    }

    public static boolean isAatTerm(Node node) {
        return node.getLocalName().equals("subject")
            && hasAttributeValue(node, "subjectScheme", SCHEME_AAT)
            && hasAttributeValue(node, "schemeURI", SCHEME_URI_AAT);
    }
}
