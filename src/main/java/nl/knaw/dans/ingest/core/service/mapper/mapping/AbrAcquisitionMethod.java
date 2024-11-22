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

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;

import java.util.Map;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_ABR_VERWERVINGSWIJZE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_URI_ABR_VERWERVINGSWIJZE;

@Slf4j
public class AbrAcquisitionMethod extends Base {
    public static boolean isVerwervingswijze(Node node) {
        return hasSchemeAndUriAttribute(node, SCHEME_ABR_VERWERVINGSWIJZE, SCHEME_URI_ABR_VERWERVINGSWIJZE);
    }

    public static String toVerwervingswijze(Node node, Map<String, String> abrAcquisitionMethodCodeToTerm) {
        return getValueUri(node, abrAcquisitionMethodCodeToTerm);
    }
}
