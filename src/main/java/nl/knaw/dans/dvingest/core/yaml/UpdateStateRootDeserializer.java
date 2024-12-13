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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;

import java.io.IOException;
import java.time.Instant;

public class UpdateStateRootDeserializer extends StdDeserializer<UpdateStateRoot> {

    protected UpdateStateRootDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public UpdateStateRoot deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        var node = p.getCodec().readTree(p);
        var updateActionInfo = node.get("updateState");
        if (updateActionInfo == null) {
            throw new IllegalArgumentException("No updateState found in the yaml");
        }
        var publishAction = updateActionInfo.get("publish");
        var releaseMigratedAction = updateActionInfo.get("releaseMigrated");

        if ((publishAction == null) == (releaseMigratedAction == null)) {
            throw new IllegalArgumentException("Exactly one of publish or releaseMigrated must be set");
        }

        if (publishAction != null) {
            ValueNode publishActionValue = (ValueNode) publishAction;
            return new UpdateStateRoot(new PublishAction(UpdateType.valueOf(publishActionValue.asText().toLowerCase())));
        }
        else {
            ValueNode releaseMigratedActionValue = (ValueNode) releaseMigratedAction;
            return new UpdateStateRoot(new ReleaseMigratedAction(releaseMigratedActionValue.asText()));
        }
    }
}
