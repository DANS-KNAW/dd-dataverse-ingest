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
package nl.knaw.dans.dvingest.core.yaml;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Data
@NoArgsConstructor
public class ReleaseMigratedAction implements UpdateAction {
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private String releaseDate;

    public ReleaseMigratedAction(String releaseDate) {
        setReleaseDate(releaseDate);
    }

    public void setReleaseDate(String releaseDate) {
        if (!DATE_PATTERN.matcher(releaseDate).matches()) {
            throw new IllegalArgumentException("Release date must be in the format YYYY-MM-DD");
        }
        this.releaseDate = releaseDate;
    }
}