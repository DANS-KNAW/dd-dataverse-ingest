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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.deposit.DansBagDeposit;
import nl.knaw.dans.dvingest.core.dansbag.exception.RejectedDepositException;
import nl.knaw.dans.dvingest.core.dansbag.mapper.mapping.LicenseElem;
import nl.knaw.dans.dvingest.core.dansbag.xml.XPathEvaluator;
import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.License;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static nl.knaw.dans.dvingest.core.dansbag.xml.XPathConstants.DDM_DCMI_METADATA;

// TODO: move to mapping package
@Slf4j
public class SupportedLicenses {
    private final DataverseService dataverseService;
    private Map<URI, License> supportedLicenses;

    public SupportedLicenses(DataverseService dataverseService) {
        supportedLicenses = null;
        this.dataverseService = dataverseService;
    }

    public License getLicenseFromDansDeposit(DansBagDeposit dansDeposit) {
        if (supportedLicenses == null) {
            log.debug("Fetching supported licenses from Dataverse");
            fetchLicenses();
        }
        var optLicenseUri = XPathEvaluator.nodes(dansDeposit.getDdm(), DDM_DCMI_METADATA + "/dcterms:license")
            .filter(LicenseElem::isLicenseUri)
            .findFirst()
            .map(LicenseElem::getLicenseUri);
        if (optLicenseUri.isEmpty()) {
            throw new RejectedDepositException(dansDeposit, "No license found");
        }
        else {
            var licenseUri = optLicenseUri.get();
            if (!supportedLicenses.containsKey(licenseUri)) {
                throw new RejectedDepositException(dansDeposit, "Unsupported license: " + licenseUri);
            }
            return supportedLicenses.get(licenseUri);
        }
    }

    private void fetchLicenses() {
        try {
            supportedLicenses = new HashMap<>();
            for (var license : dataverseService.getSupportedLicenses()) {
                supportedLicenses.put(license.getUri(), license);
            }
        }
        catch (IOException | DataverseException e) {
            supportedLicenses = null;
            throw new IllegalStateException("Could not fetch supported licenses from Dataverse", e);
        }
    }
}
