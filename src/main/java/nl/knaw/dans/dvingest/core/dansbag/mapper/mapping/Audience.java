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

import com.google.common.collect.Comparators;

import java.util.HashMap;
import java.util.Map;

public class Audience {
    private static final Map<String, String> narcisToSubject = new HashMap<>();

    static {
        narcisToSubject.put("D11", "Mathematical Sciences");
        narcisToSubject.put("D12", "Physics");
        narcisToSubject.put("D13", "Chemistry");
        narcisToSubject.put("D14", "Engineering");
        narcisToSubject.put("D15", "Earth and Environmental Sciences");
        narcisToSubject.put("D16", "Computer and Information Science");
        narcisToSubject.put("D17", "Astronomy and Astrophysics");
        narcisToSubject.put("D18", "Agricultural Sciences");
        narcisToSubject.put("D2", "Medicine, Health and Life Sciences");
        narcisToSubject.put("D3", "Arts and Humanities");
        narcisToSubject.put("D40", "Law");
        narcisToSubject.put("D41", "Law");
        narcisToSubject.put("D42", "Social Sciences");
        narcisToSubject.put("D5", "Social Sciences");
        narcisToSubject.put("D6", "Social Sciences");
        narcisToSubject.put("D7", "Business and Management");
        narcisToSubject.put("E13", "Social Sciences");
        narcisToSubject.put("E14", "Social Sciences");
        narcisToSubject.put("E15", "Earth and Environmental Sciences");
    }

    public static String toCitationBlockSubject(String code) {
        if (!code.matches("^[D|E]\\d{5}$")) {
            throw new RuntimeException("NARCIS classification code incorrectly formatted");
        }

        return narcisToSubject.keySet().stream()
            .filter(code::startsWith)
            .max((a, b) -> Comparators.max(a.length(), b.length()))
            .map(narcisToSubject::get)
            .orElse("Other");
    }

    public static String toNarcisTerm(String code) {
        return "https://www.narcis.nl/classification/" + code;
    }

}
