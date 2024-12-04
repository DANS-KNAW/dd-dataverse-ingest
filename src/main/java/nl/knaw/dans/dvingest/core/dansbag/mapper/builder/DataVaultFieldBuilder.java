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

import java.util.stream.Stream;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.BAG_ID;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DANS_OTHER_ID;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DANS_OTHER_ID_VERSION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATA_SUPPLIER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.NBN;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SWORD_TOKEN;

public class DataVaultFieldBuilder extends FieldBuilder {

    public void addBagId(String value) {
        addSingleString(BAG_ID, Stream.ofNullable(value));
    }

    public void addNbn(String value) {
        addSingleString(NBN, Stream.ofNullable(value));
    }

    public void addDansOtherId(String value) {
        addSingleString(DANS_OTHER_ID, Stream.ofNullable(value));
    }

    public void addDansOtherIdVersion(String value) {
        addSingleString(DANS_OTHER_ID_VERSION, Stream.ofNullable(value));
    }

    public void addSwordToken(String value) {
        addSingleString(SWORD_TOKEN, Stream.ofNullable(value));
    }

    public void addDataSupplier(String value) {
        addSingleString(DATA_SUPPLIER, Stream.ofNullable(value));
    }
}
