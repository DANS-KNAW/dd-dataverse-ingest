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

import nl.knaw.dans.lib.dataverse.model.dataset.ControlledMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledSingleValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PrimitiveFieldBuilder {
    private final String typeName;
    private final boolean multiple;
    private final boolean controlled;
    private final List<String> values = new LinkedList<>();

    public PrimitiveFieldBuilder(String typeName, boolean multiple, boolean controlled) {
        this.typeName = typeName;
        this.multiple = multiple;
        this.controlled = controlled;
    }

    public PrimitiveFieldBuilder addValues(List<String> values) {
        var filteredValues = values.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
        this.values.addAll(filteredValues);
        return this;
    }

    public PrimitiveFieldBuilder addValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            this.values.add(value);
        }
        return this;
    }

    public MetadataField build(boolean deduplicate) {
        if (this.values.size() == 0) {
            return null;
        }

        var values = deduplicate ? this.values.stream().distinct().collect(Collectors.toList()) : this.values;

        if (this.multiple) {
            if (this.controlled) {
                return new ControlledMultiValueField(this.typeName, values);
            }
            else {
                return new PrimitiveMultiValueField(this.typeName, values);
            }
        }
        else {
            if (this.controlled) {
                return new ControlledSingleValueField(this.typeName, values.get(0));
            }
            else {
                return new PrimitiveSingleValueField(this.typeName, values.get(0));
            }
        }
    }
}
