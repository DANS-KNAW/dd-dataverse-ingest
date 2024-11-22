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
package nl.knaw.dans.ingest.core.service;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class XmlReaderImpl implements XmlReader {

    private final XPath xpath;

    public XmlReaderImpl() {
        this.xpath = XPathFactory
            .newInstance()
            .newXPath();

        final var namespaceMap = Map.of(
            "xml", NAMESPACE_XML,
            "dc", NAMESPACE_DC,
            "dcx-dai", NAMESPACE_DCX_DAI,
            "ddm", NAMESPACE_DDM,
            "dcterms", NAMESPACE_DCTERMS,
            "xsi", NAMESPACE_XSI,
            "id-type", NAMESPACE_ID_TYPE,
            "dcx-gml", NAMESPACE_DCX_GML,
            "files", NAMESPACE_FILES_XML,
            "gml", NAMESPACE_OPEN_GIS
        );

        xpath.setNamespaceContext(new NamespaceContext() {

            @Override
            public String getNamespaceURI(String s) {
                return namespaceMap.get(s);
            }

            @Override
            public String getPrefix(String s) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String s) {
                return null;
            }
        });

    }

    @Override
    public Document readXmlFile(Path path) throws ParserConfigurationException, IOException, SAXException {
        var factory = getFactory();

        return factory
            .newDocumentBuilder()
            .parse(path.toFile());
    }

    @Override
    public Document readXmlString(String str) throws ParserConfigurationException, IOException, SAXException {
        var factory = getFactory();

        return factory
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(str)));
    }

    public DocumentBuilderFactory getFactory() throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);
        return factory;
    }
}
