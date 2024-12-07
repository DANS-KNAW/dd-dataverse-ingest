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
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.SPATIAL_POINT_SCHEME;
import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.SPATIAL_POINT_X;
import static nl.knaw.dans.dvingest.core.dansbag.service.DepositDatasetFieldNames.SPATIAL_POINT_Y;

@Slf4j
public class SpatialPoint extends Spatial {

    public static CompoundFieldGenerator<Node> toEasyTsmSpatialPointValueObject = (builder, node) -> {
        var isRd = isRd(node);
        var point = getChildNode(node, "gml:Point/gml:pos")
            .map(n -> getPoint(n, isRd))
            .orElseThrow(() -> new RuntimeException(String.format("No Point/pos node found in node %s", node.getNodeName())));

        builder.addControlledSubfield(SPATIAL_POINT_SCHEME, isRd ? RD_SCHEME : LONLAT_SCHEME);
        builder.addSubfield(SPATIAL_POINT_X, point.getX());
        builder.addSubfield(SPATIAL_POINT_Y, point.getY());
    };

    public static boolean isPoint(Node node) {
        var hasChild = hasChildNode(node, "gml:Point/gml:pos");
        var srsName = Base.getAttribute(node, "srsName")
            .map(Node::getTextContent)
            .filter(StringUtils::isNotBlank)
            .orElse(null);

        if (!hasChild) {
            return false;
        }

        // TS002: if @srsName=http://www.opengis.net/def/crs/EPSG/0/28992
        // TS003: if @srsName not present OR @srsName=http://www.opengis.net/def/crs/EPSG/0/4326
        return srsName == null || LONLAT_SRS_NAME.equals(srsName) || RD_SRS_NAME.equals(srsName);
    }
}
