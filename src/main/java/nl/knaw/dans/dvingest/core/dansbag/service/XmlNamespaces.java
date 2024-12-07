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
package nl.knaw.dans.dvingest.core.dansbag.service;

public interface XmlNamespaces {
    String NAMESPACE_XML = "http://www.w3.org/XML/1998/namespace";
    String NAMESPACE_DC = "http://purl.org/dc/elements/1.1/";
    String NAMESPACE_DCX_DAI = "http://easy.dans.knaw.nl/schemas/dcx/dai/";
    String NAMESPACE_DDM = "http://schemas.dans.knaw.nl/dataset/ddm-v2/";
    String NAMESPACE_DCTERMS = "http://purl.org/dc/terms/";
    String NAMESPACE_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    String NAMESPACE_ID_TYPE = "http://easy.dans.knaw.nl/schemas/vocab/identifier-type/";
    String NAMESPACE_DCX_GML = "http://easy.dans.knaw.nl/schemas/dcx/gml/";
    String NAMESPACE_FILES_XML = "http://easy.dans.knaw.nl/schemas/bag/metadata/files/";
    String NAMESPACE_AFM_XML = "http://easy.dans.knaw.nl/schemas/bag/metadata/afm/";
    String NAMESPACE_OPEN_GIS = "http://www.opengis.net/gml";
    String NAMESPACE_EASY_WORKFLOW = "http://easy.dans.knaw.nl/easy/workflow/";
    String NAMESPACE_DAMD = "http://easy.dans.knaw.nl/easy/dataset-administrative-metadata/";
    String NAMESPACE_AGREEMENTS = "http://easy.dans.knaw.nl/schemas/bag/metadata/agreements/";
}
