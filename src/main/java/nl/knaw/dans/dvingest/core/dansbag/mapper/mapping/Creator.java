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
import org.eclipse.jetty.util.StringUtil;
import org.w3c.dom.Node;

import static nl.knaw.dans.dvingest.core.dansbag.mapper.DepositDatasetFieldNames.AUTHOR_NAME;

@Slf4j
public final class Creator extends Base {
    public static CompoundFieldGenerator<Node> toAuthorValueObject = (builder, node) -> {
        builder.addSubfield(AUTHOR_NAME, StringUtil.isNotBlank(node.getTextContent()) ? node.getTextContent().trim() : "");
    };
}
