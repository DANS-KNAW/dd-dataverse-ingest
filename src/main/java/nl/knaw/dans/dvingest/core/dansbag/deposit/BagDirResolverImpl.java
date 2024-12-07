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
package nl.knaw.dans.dvingest.core.dansbag.deposit;

import nl.knaw.dans.dvingest.core.dansbag.exception.InvalidDepositException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BagDirResolverImpl implements BagDirResolver {

    @Override
    public Path getBagDir(Path depositDir) throws InvalidDepositException, IOException {
        if (!Files.isDirectory(depositDir)) {
            throw new InvalidDepositException(String.format("%s is not a directory", depositDir));
        }

        try (var substream = Files.list(depositDir).filter(Files::isDirectory)) {
            var directories = substream.toList();

            // only 1 directory allowed, not 0 or more than 1
            if (directories.size() != 1) {
                throw new InvalidDepositException(String.format(
                    "%s has more or fewer than one subdirectory", depositDir
                ));
            }

            // check for the presence of deposit.properties and bagit.txt
            if (!Files.exists(depositDir.resolve("deposit.properties"))) {
                throw new InvalidDepositException(String.format(
                    "%s does not contain a deposit.properties file", depositDir
                ));
            }

            var bagDir = directories.get(0);

            if (!Files.exists(bagDir.resolve("bagit.txt"))) {
                throw new InvalidDepositException(String.format(
                    "%s does not contain a bag", depositDir
                ));
            }

            return bagDir;
        }
    }
}
