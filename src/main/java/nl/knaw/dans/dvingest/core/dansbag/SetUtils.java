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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: move to dans-java-utils or find opensource alternative
public class SetUtils {

    public static <T> Set<T> diff(Collection<T> a, Collection<T> b) {
        return a.stream().filter(k -> !b.contains(k)).collect(Collectors.toSet());
    }

    public static <T> Set<T> intersection(Collection<T> a, Collection<T> b) {
        return a.stream().filter(b::contains).collect(Collectors.toSet());
    }

    public static <T> Set<T> union(Collection<T> a, Collection<T> b) {
        return Stream.of(a.stream(), b.stream()).flatMap(i -> i).collect(Collectors.toSet());
        // a.stream().filter(b::contains).collect(Collectors.toSet());
    }
}
