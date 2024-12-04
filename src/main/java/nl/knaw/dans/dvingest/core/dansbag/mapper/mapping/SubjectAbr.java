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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_BASE_URL;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_ABR_ARTIFACT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_ABR_COMPLEX;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_ABR_OLD;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_URI_ABR_ARTIFACT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_URI_ABR_COMPLEX;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_URI_ABR_OLD;

@Slf4j
public class SubjectAbr extends Base {

    public static boolean isAbrComplex(Node node) {
        return hasSchemeAndUriAttribute(node,
            SCHEME_ABR_COMPLEX,
            SCHEME_URI_ABR_COMPLEX
        );
    }

    public static boolean isOldAbr(Node node) {
        return hasSchemeAndUriAttribute(node,
            SCHEME_ABR_OLD,
            SCHEME_URI_ABR_OLD
        );
    }

    public static boolean isAbrArtifact(Node node) {
        return hasSchemeAndUriAttribute(node,
            SCHEME_ABR_ARTIFACT,
            SCHEME_URI_ABR_ARTIFACT
        );
    }

    public static String toAbrComplex(Node node, Map<String, String> abrComplexCodeToTerm) {
        return normalizeToNewAbrTerm(getValueUri(node, abrComplexCodeToTerm));
    }

    public static String toAbrArtifact(Node node, Map<String, String> abrArtifactCodeToTerm) {
        return normalizeToNewAbrTerm(getValueUri(node, abrArtifactCodeToTerm));
    }

    private static String normalizeToNewAbrTerm(String term) {
        if (term == null) {
            return null;
        }
        try {
            var uuid = Paths.get(new URI(term).getPath()).getFileName().toString();
            return String.format("%s/%s", ABR_BASE_URL, uuid);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI", e);
        }
    }
}
