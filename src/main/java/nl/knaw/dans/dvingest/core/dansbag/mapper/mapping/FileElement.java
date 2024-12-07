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
package nl.knaw.dans.dvingest.core.dansbag.mapper.mapping;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvingest.core.dansbag.domain.Deposit;
import nl.knaw.dans.dvingest.core.dansbag.domain.FileInfo;
import nl.knaw.dans.dvingest.core.dansbag.service.XPathEvaluator;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class FileElement extends Base {
    private final static Pattern filenameForbidden = Pattern.compile("[:*?\"<>|;#]");
    private final static Pattern directoryLabelForbidden = Pattern.compile("[^_\\-.\\\\/ 0-9a-zA-Z]");
    private final static Map<String, Boolean> accessibilityToRestrict = Map.of(
        "KNOWN", true,
        "NONE", true,
        "RESTRICTED_REQUEST", true,
        "ANONYMOUS", false
    );

    @Value
    public static class FileMetaResult {
        boolean sanitized;
        FileMeta fileMeta;
    }

    public static FileMetaResult toFileMeta(Node node, boolean defaultRestrict, boolean isMigration) {
        var filepathAttribute = getAttribute(node, "filepath")
            .map(Node::getTextContent)
            .orElseThrow(() -> new RuntimeException("File node without a filepath attribute"));

        if (!filepathAttribute.startsWith("data/")) {
            throw new RuntimeException(String.format("file outside data folder: %s", filepathAttribute));
        }

        var pathInDataset = Path.of(filepathAttribute.substring("data/".length()));
        // FIL001
        var filename = pathInDataset.getFileName().toString();
        var sanitizedFilename = replaceForbiddenCharactersInFilename(filename);
        var dirPath = Optional.ofNullable(pathInDataset.getParent()).map(Path::toString)
            // Work-around for DD-1308
            // PLEASE, DO NOT TRIM!!! No kidding, providing a space will remove the directoryLabel if it exists.
            .orElse(" ");
        // FIL002
        var sanitizedDirLabel = replaceForbiddenCharactersInPath(dirPath);

        // FIL005
        var restricted = getChildNode(node, "files:accessibleToRights")
            .map(Node::getTextContent)
            .map(accessibilityToRestrict::get)
            .orElse(defaultRestrict);

        boolean isSanitized = !StringUtils.equals(filename, sanitizedFilename) || !StringUtils.equals(dirPath, sanitizedDirLabel);
        var originalFilePath = isSanitized
            ? pathInDataset.toString()
            : null;

        var kv = getKeyValuePairs(node, filename, originalFilePath, isMigration);

        var description = getDescription(kv);

        var fm = new FileMeta();
        fm.setLabel(sanitizedFilename);
        fm.setDirectoryLabel(sanitizedDirLabel);
        fm.setDescription(description);
        fm.setRestricted(restricted);

        return new FileMetaResult(isSanitized, fm);
    }

    private static String getDescription(Map<String, List<String>> kv) {
        if (!kv.isEmpty()) {
            if (kv.keySet().size() == 1 && kv.containsKey("description")) {
                // FIL004
                return kv.get("description").stream().findFirst().orElse(null);
            }
            else {
                return formatKeyValuePairs(kv);
            }
        }

        return null;
    }

    private static String formatKeyValuePairs(Map<String, List<String>> kv) {
        return kv.entrySet().stream().map(entry -> {
                var values = StringUtils.join(entry.getValue(), ",");
                return String.format("%s: \"%s\"", entry.getKey(), values);
            })
            .collect(Collectors.joining("; "));
    }

    private static Map<String, List<String>> getKeyValuePairs(Node node, String filename, String originalFilePath, boolean isMigration) {
        var fixedKeys = List.of(
            "hardware",
            "original_OS",
            "software",
            "notes",
            "case_quantity",
            "file_category",
            "description",
            "othmat_codebook",
            "data_collector",
            "collection_date",
            "time_period",
            "geog_cover",
            "geog_unit",
            "local_georef",
            "mapprojection",
            "analytic_units");

        var result = new HashMap<String, List<String>>() {

            void addValue(String key, String value) {
                computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }

            ;
        };
        if (isMigration) {

            // FIL002A
            getChildNodes(node, "afm:keyvaluepair")
                .forEach(n -> {
                    var key = getChildNode(n, "afm:key")
                        .map(Node::getTextContent)
                        .orElse(null);

                    var value = getChildNode(n, "afm:value")
                        .map(Node::getTextContent)
                        .orElse(null);

                    if (key != null && value != null) {
                        result.addValue(key, value);
                    }
                });

            // FIL002B
            for (var key : fixedKeys) {
                var child = getChildNodes(node, "dcterms:" + key)
                    .map(Node::getTextContent)
                    .collect(Collectors.toList());

                log.trace("matches for key '{}': {}", key, result);

                if (child.size() > 0) {
                    result.put(key, child);
                }
            }
        }

        // FIL003
        if (originalFilePath != null) {
            result.addValue("original_filepath", originalFilePath);
        }

        if (isMigration) {
            // "archival_name" of EASY-I and "original_file" of EASY-II are mapped to titles
            // see easy-fedora-to-bag.FileItem[Spec]
            getChildNodes(node, "dcterms:title")
                .map(Node::getTextContent)
                .filter(n -> !StringUtils.equalsIgnoreCase(filename, n))
                .forEach(n -> result.addValue("title", n));
        }
        else {
            // FIL004 in case of migration part of FIL002B
            getChildNodes(node, "dcterms:description")
                .map(Node::getTextContent)
                .filter(n -> !StringUtils.equalsIgnoreCase(filename, n))
                .forEach(n -> result.addValue("description", n));
        }
        return result;
    }

    private static String replaceForbiddenCharactersInPath(String dirPath) {
        if (dirPath == null) {
            return null;
        }
        return directoryLabelForbidden.matcher(dirPath).replaceAll("_");
    }

    private static String replaceForbiddenCharactersInFilename(String filename) {
        if (filename == null) {
            return null;
        }
        return filenameForbidden.matcher(filename).replaceAll("_");
    }

    public static Map<Path, FileInfo> pathToFileInfo(Deposit deposit, boolean isMigration) {
        // FIL006
        var defaultRestrict = XPathEvaluator.nodes(deposit.getDdm(), "/ddm:DDM/ddm:profile/ddm:accessRights")
            .map(AccessRights::toDefaultRestrict)
            .findFirst()
            .orElse(true);

        var result = new HashMap<Path, FileInfo>();
        var bagDir = deposit.getBagDir();

        deposit.getFiles().forEach(depositFile -> {
            var fileMetaResult = toFileMeta(depositFile.getXmlNode(), defaultRestrict, isMigration);
            result.put(depositFile.getPath(), new FileInfo(
                bagDir.resolve(depositFile.getPath()),
                depositFile.getChecksum(),
                fileMetaResult.isSanitized(),
                fileMetaResult.getFileMeta()
            ));
        });

        return result;
    }
}
