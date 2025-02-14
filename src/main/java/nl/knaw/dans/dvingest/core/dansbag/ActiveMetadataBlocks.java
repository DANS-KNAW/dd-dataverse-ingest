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

import nl.knaw.dans.dvingest.core.service.DataverseService;

import java.util.Set;

public class ActiveMetadataBlocks {
    private final DataverseService dataverseService;
    private Set<String> activeMetadataBlockNames;

    public ActiveMetadataBlocks(DataverseService dataverseService) {
        this.activeMetadataBlockNames = null;
        this.dataverseService = dataverseService;
    }

    // FOR TESTING
    public ActiveMetadataBlocks(Set<String> activeMetadataBlockNames) {
        this.activeMetadataBlockNames = activeMetadataBlockNames;
        this.dataverseService = null;
    }

    public boolean contains(String blockName) {
        return getActiveMetadataBlockNames().contains(blockName);
    }

    public Set<String> getActiveMetadataBlockNames() {
        if (activeMetadataBlockNames == null) {
            try {
                activeMetadataBlockNames = dataverseService.getActiveMetadataBlockNames();
            }
            catch (Exception e) {
                throw new IllegalStateException("Could not fetch active metadata blocks from Dataverse", e);
            }
        }
        return activeMetadataBlockNames;
    }

}
