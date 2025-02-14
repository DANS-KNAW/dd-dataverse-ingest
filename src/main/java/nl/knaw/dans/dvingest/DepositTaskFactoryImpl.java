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
package nl.knaw.dans.dvingest;

import lombok.AllArgsConstructor;
import nl.knaw.dans.dvingest.core.DataverseIngestDeposit;
import nl.knaw.dans.dvingest.core.DependenciesReadyCheck;
import nl.knaw.dans.dvingest.core.DepositTask;
import nl.knaw.dans.dvingest.core.DepositTaskFactory;
import nl.knaw.dans.dvingest.core.bagprocessor.BagProcessorFactory;
import nl.knaw.dans.dvingest.core.dansbag.DansDepositSupportFactory;

import java.nio.file.Path;

@AllArgsConstructor
public class DepositTaskFactoryImpl implements DepositTaskFactory {
    private final BagProcessorFactory bagProcessorFactory;
    private final DansDepositSupportFactory dansDepositSupportFactory;
    private final DependenciesReadyCheck dependenciesReadyCheck;

    @Override
    public Runnable createDepositTask(DataverseIngestDeposit deposit, Path outputDir, boolean onlyConvertDansDeposit) {
        return new DepositTask(deposit, outputDir, onlyConvertDansDeposit, bagProcessorFactory, dansDepositSupportFactory, dependenciesReadyCheck);
    }
}
