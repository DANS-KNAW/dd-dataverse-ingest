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
import java.nio.file.Path;

public interface BagDirResolver {

    /**
     * Returns the first subdirectory of the provided path if it looks like a bag. It will not check if the bag is valid and only do a very superficial check. It will throw an exception if
     * <ul>
     *  <li>The path is not a directory</li>
     *  <li>There is no deposit.properties file inside the path</li>
     *  <li>There are no subdirectories in the path</li>
     *  <li>There are more than 1 subdirectories in the path</li>
     *  <li>The subdirectory does not contain a bagit.txt file</li>
     * </ul>
     *
     * @param depositDir The deposit directory
     * @return The path to the first subdirectory
     * @throws InvalidDepositException If any of the checks above fail
     * @throws IOException             Propagated from underlying systems to indicate an IO error
     */
    Path getBagDir(Path depositDir) throws InvalidDepositException, IOException;
}
