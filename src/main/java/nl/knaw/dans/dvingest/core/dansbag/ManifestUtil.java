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

import gov.loc.repository.bagit.creator.CreatePayloadManifestsVistor;
import gov.loc.repository.bagit.creator.CreateTagManifestsVistor;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.util.PathUtils;
import gov.loc.repository.bagit.writer.ManifestWriter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gov.loc.repository.bagit.hash.StandardSupportedAlgorithms.SHA1;

@Slf4j
public class ManifestUtil  {

    public static void ensureSha1ManifestPresent(Bag bag) throws NoSuchAlgorithmException, IOException {
        log.debug("Ensure SHA-1 manifest is present in bag {}", bag.getRootDir());
        var manifests = bag.getPayLoadManifests();
        var algorithms = manifests.stream().map(Manifest::getAlgorithm);

        if (algorithms.anyMatch(SHA1::equals)) {
            log.debug("SHA-1 manifest already present in bag {}", bag.getRootDir());
            return;
        }

        var payloadFilesMap = Hasher.createManifestToMessageDigestMap(List.of(SHA1));
        var payloadVisitor = new CreatePayloadManifestsVistor(payloadFilesMap, true);
        Files.walkFileTree(PathUtils.getDataDir(bag), payloadVisitor);
        manifests.addAll(payloadFilesMap.keySet());
        ManifestWriter.writePayloadManifests(manifests, PathUtils.getBagitDir(bag), bag.getRootDir(), bag.getFileEncoding());

        updateTagManifests(bag);
        log.debug("SHA-1 manifest added to bag {}", bag.getRootDir());
    }

    private static void updateTagManifests(Bag bag) throws NoSuchAlgorithmException, IOException {
        var algorithms = bag.getTagManifests().stream()
            .map(Manifest::getAlgorithm)
            .collect(Collectors.toList());
        var tagFilesMap = Hasher.createManifestToMessageDigestMap(algorithms);
        var bagRootDir = bag.getRootDir();
        var tagVisitor = new CreateTagManifestsVistor(tagFilesMap, true) {

            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) throws IOException {
                /*
                 * Fix for EASY-1306: a tag manifest must not contain an entry for itself, as this is practically
                 * impossible to calculate. It could in theory contain entries for other tag manifests. However,
                 * the CreateTagManifestsVisitor, once it finds an entry for a tag file in ONE of the tag manifests,
                 * will add an entry in ALL tag manifests.
                 *
                 * Therefore, we adopt the strategy NOT to calculate any checksums for the tag manifests themselves.
                 *
                 * Update: this is actually required in V1.0: https://tools.ietf.org/html/rfc8493#section-2.2.1
                 */
                var isTagManifest = bagRootDir.relativize(path).getNameCount() == 1 &&
                    path.getFileName().toString().startsWith("tagmanifest-");

                if (isTagManifest) {
                    return FileVisitResult.CONTINUE;
                }
                else {
                    return super.visitFile(path, attrs);
                }
            }
        };

        Files.walkFileTree(bagRootDir, tagVisitor);
        bag.getTagManifests().clear();
        bag.getTagManifests().addAll(tagFilesMap.keySet());
        ManifestWriter.writeTagManifests(bag.getTagManifests(), PathUtils.getBagitDir(bag), bagRootDir, bag.getFileEncoding());
    }

    static public Map<Path, String> getFilePathToSha1(Bag bag) {
        var result = new HashMap<Path, String>();
        var manifest = bag.getPayLoadManifests().stream()
            .filter(item -> item.getAlgorithm().equals(StandardSupportedAlgorithms.SHA1))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Deposit bag does not have SHA-1 payload manifest"));

        for (var entry : manifest.getFileToChecksumMap().entrySet()) {
            result.put(bag.getRootDir().relativize(entry.getKey()), entry.getValue());
        }

        return result;
    }
}
