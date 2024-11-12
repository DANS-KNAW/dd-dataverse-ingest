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
package nl.knaw.dans.dvingest.core.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

// TODO: move to dans-java-utils
@RequiredArgsConstructor
public class PathIterator implements Iterator<Path> {
    @NonNull
    private final Iterator<File> fileIterator;

    @Getter
    private long iteratedCount = 0;

    @Override
    public boolean hasNext() {
        return fileIterator.hasNext();
    }

    @Override
    public Path next() {
        iteratedCount++;
        return fileIterator.next().toPath();
    }
}