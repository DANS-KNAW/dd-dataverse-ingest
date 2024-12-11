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
package nl.knaw.dans.dvingest.core.dansbag;

import nl.knaw.dans.dvingest.core.DataverseIngestDeposit;
import nl.knaw.dans.dvingest.core.Deposit;

/**
 * Factory for creating DansDepositSupport objects.
 */
public interface DansDepositSupportFactory {

    /**
     * Create a DansDepositSupport object for the given deposit. The object implements the {@link Deposit} interface, implementing the appropriate methods and forwarding the others to call the
     * original deposit. If DANS deposit support is disabled, the deposit is returned as is.
     *
     * @param deposit the deposit
     * @return the DansDepositSupport object
     */
    Deposit addDansDepositSupportIfEnabled(DataverseIngestDeposit deposit);

}
