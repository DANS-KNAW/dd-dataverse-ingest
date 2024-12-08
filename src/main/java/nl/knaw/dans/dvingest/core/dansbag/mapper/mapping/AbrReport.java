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
import org.w3c.dom.Node;

import java.util.Map;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SCHEME_ABR_RAPPORT_TYPE;
import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.SCHEME_URI_ABR_RAPPORT_TYPE;

@Slf4j
public class AbrReport extends Base {

    public static boolean isAbrReportType(Node node) {
        return "reportNumber".equals(node.getLocalName())
            && hasSchemeAndUriAttribute(node, SCHEME_ABR_RAPPORT_TYPE, SCHEME_URI_ABR_RAPPORT_TYPE);
    }

    public static String toAbrRapportType(Node node, Map<String, String> abrReportCodeToTerm) {
        return getValueUri(node, abrReportCodeToTerm);
    }

    public static String toAbrRapportNumber(Node node) {
        return node.getTextContent();
    }
}
