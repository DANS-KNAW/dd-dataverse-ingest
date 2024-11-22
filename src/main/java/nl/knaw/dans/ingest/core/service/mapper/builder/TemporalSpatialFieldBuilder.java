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
package nl.knaw.dans.ingest.core.service.mapper.builder;

import org.w3c.dom.Node;

import java.util.stream.Stream;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SPATIAL_BOX;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SPATIAL_COVERAGE_CONTROLLED;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SPATIAL_COVERAGE_UNCONTROLLED;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SPATIAL_POINT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.TEMPORAL_COVERAGE;

public class TemporalSpatialFieldBuilder extends FieldBuilder {

    public void addTemporalCoverage(Stream<String> nodes) {
        addMultiplePrimitiveString(TEMPORAL_COVERAGE, nodes);
    }

    public void addSpatialPoint(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(SPATIAL_POINT, stream, generator);
    }

    public void addSpatialBox(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(SPATIAL_BOX, stream, generator);
    }

    public void addSpatialCoverageControlled(Stream<String> stream) {
        addMultipleControlledFields(SPATIAL_COVERAGE_CONTROLLED, stream);
    }

    public void addSpatialCoverageUncontrolled(Stream<String> nodes) {
        addMultiplePrimitiveString(SPATIAL_COVERAGE_UNCONTROLLED, nodes);
    }

}
