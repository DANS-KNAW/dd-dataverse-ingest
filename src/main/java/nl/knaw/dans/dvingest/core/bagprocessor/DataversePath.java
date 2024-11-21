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
package nl.knaw.dans.dvingest.core.bagprocessor;

import lombok.NonNull;
import lombok.Value;

/**
 * A filepath in Dataverse is a combination of file label and directory label. This class converts between a regular representation of a path and the Dataverse representation.
 */
@Value
public class DataversePath {
    String directoryLabel;
    String label;

    public DataversePath(@NonNull String directoryLabel, @NonNull String label) {
        if (label.contains("/")) {
            throw new IllegalArgumentException("label contains slash");
        }

        this.directoryLabel = directoryLabel;
        this.label = label;
    }

    public DataversePath(@NonNull String path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path is empty");
        }

        var lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            directoryLabel = "";
            label = path;
        }
        else {
            directoryLabel = path.substring(0, lastSlash);
            label = path.substring(lastSlash + 1);
        }
    }

    public String toString() {
        return directoryLabel.isEmpty() ? label : directoryLabel + "/" + label;
    }
}
