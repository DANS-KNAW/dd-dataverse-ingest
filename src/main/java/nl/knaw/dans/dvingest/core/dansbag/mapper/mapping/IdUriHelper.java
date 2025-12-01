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

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class IdUriHelper {
    private static final Pattern pattern = Pattern
        .compile("https?://.*/([-0-9]+[0-9Xx])$");

    public static String reduceUriToId(String value) {
        if (!StringUtils.isAllEmpty(value)) {
            var m = pattern.matcher(value);
            if (m.find())
                return m.group(1).replaceAll("-", "");
        }
        return value;
    }

    public static String reduceUriToOrcidId(String value) {
        if (!StringUtils.isAllEmpty(value)) {
            var m = pattern.matcher(value);
            if (m.find()) {
                String s = m.group(1).replaceAll("-", "");
                return StringUtils
                    .leftPad(s, 16, '0')
                    .replaceAll("(\\d\\d\\d\\d)", "$1-")
                    .replaceAll("-$", "");
            }
        }
        return value;
    }
}
