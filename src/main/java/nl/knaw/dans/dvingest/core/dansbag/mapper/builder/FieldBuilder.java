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
package nl.knaw.dans.dvingest.core.dansbag.mapper.builder;

import nl.knaw.dans.lib.dataverse.CompoundFieldBuilder;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class FieldBuilder {
    private final Map<String, CompoundFieldBuilder> compoundFields;
    private final Map<String, PrimitiveFieldBuilder> primitiveFields;

    public FieldBuilder() {
        this.compoundFields = new HashMap<>();
        this.primitiveFields = new HashMap<>();
    }

    public Map<String, PrimitiveFieldBuilder> getPrimitiveFields() {
        return primitiveFields;
    }

    public Map<String, CompoundFieldBuilder> getCompoundFields() {
        return compoundFields;
    }

    CompoundFieldBuilder getCompoundBuilder(String name, boolean multiple) {
        var result = Optional.ofNullable(compoundFields.get(name))
            .map(CompoundFieldBuilder::nextValue)
            .orElse(new CompoundFieldBuilder(name, multiple));

        compoundFields.put(name, result);
        return result;
    }

    PrimitiveFieldBuilder getPrimitiveBuilder(String name, boolean multiple, boolean controlled) {
        var result = Optional.ofNullable(primitiveFields.get(name))
            .orElse(new PrimitiveFieldBuilder(name, multiple, controlled));

        primitiveFields.put(name, result);
        return result;
    }

    void setPrimitiveField(String name, String value) {
        getPrimitiveBuilder(name, false, false)
            .addValue(value);
    }

    void setPrimitiveFields(String name, List<String> values) {
        getPrimitiveBuilder(name, true, false)
            .addValues(values);
    }

    void setControlledField(String name, String value) {
        getPrimitiveBuilder(name, false, true)
            .addValue(value);
    }

    void setControlledFields(String name, List<String> values) {
        getPrimitiveBuilder(name, true, true)
            .addValues(values);
    }

    public void addSingleString(String name, Stream<String> data) {
        data
            .filter(Objects::nonNull)
            .filter(StringUtils::isNotBlank)
            .findFirst().ifPresent(value -> setPrimitiveField(name, value));

    }

    public void addMultipleControlledFields(String name, Stream<String> data) {
        var values = data
            .filter(Objects::nonNull)
            .filter(StringUtils::isNotBlank)
            .sorted()
            .collect(Collectors.toList());

        setControlledFields(name, values);
    }

    public void addSingleControlledField(String name, String data) {
        setControlledField(name, data);
    }

    public void addMultiplePrimitiveString(String name, Stream<String> data) {
        var values = data.collect(Collectors.toList()).stream()
            .map(s -> s == null ? null : s.trim()).collect(Collectors.toList());
        setPrimitiveFields(name, values);
    }

    public void addMultiple(String name, Stream<Node> data, CompoundFieldGenerator<Node> generator) {
        data.forEach(value -> {
            var builder = getCompoundBuilder(name, true);
            generator.build(builder, value);
        });
    }

    public void addMultipleString(String name, Stream<String> data, CompoundFieldGenerator<String> generator) {
        data.forEach(value -> {
            var builder = getCompoundBuilder(name, true);
            generator.build(builder, value);
        });
    }
}
