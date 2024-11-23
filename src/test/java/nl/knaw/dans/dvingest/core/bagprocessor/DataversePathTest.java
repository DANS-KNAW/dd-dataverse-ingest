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
package nl.knaw.dans.dvingest.core.bagprocessor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DataversePathTest {

    @Test
    public void ctor_creates_path_without_directory() {
        var path = new DataversePath("file");
        assertThat(path.getDirectoryLabel()).isBlank();
        assertThat(path.getLabel()).isEqualTo("file");
        assertThat(path.toString()).isEqualTo("file");
    }

    @Test
    public void ctor_creates_path_with_directory() {
        var path = new DataversePath("dir/file");
        assertThat(path.getDirectoryLabel()).isEqualTo("dir");
        assertThat(path.getLabel()).isEqualTo("file");
        assertThat(path.toString()).isEqualTo("dir/file");
    }

    @Test
    public void ctor_creates_path_with_directory_and_subdirectory() {
        var path = new DataversePath("dir/subdir/file");
        assertThat(path.getDirectoryLabel()).isEqualTo("dir/subdir");
        assertThat(path.getLabel()).isEqualTo("file");
        assertThat(path.toString()).isEqualTo("dir/subdir/file");
    }

    @Test
    public void ctor_creates_path_from_components() {
        var path = new DataversePath("dir", "file");
        assertThat(path.getDirectoryLabel()).isEqualTo("dir");
        assertThat(path.getLabel()).isEqualTo("file");
        assertThat(path.toString()).isEqualTo("dir/file");
    }

    @Test
    public void ctor_throws_IllegalArgumentException_when_path_is_null() {
        assertThatThrownBy(() -> new DataversePath(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("path is marked non-null but is null");
    }

    @Test
    public void ctor_throws_IllegalArgumentException_when_path_is_empty() {
        assertThatThrownBy(() -> new DataversePath(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("path is empty");
    }

    @Test
    public void ctor_throws_IllegalArgumentException_when_label_contains_slash() {
        assertThatThrownBy(() -> new DataversePath("directory", "dir/subdir/file"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("label contains slash");
    }

    @Test
    public void getDirectoryLabel_returns_directory_label() {
        var path = new DataversePath("dir/subdir/file");
        assertThat(path.getDirectoryLabel()).isEqualTo("dir/subdir");
    }

    @Test
    public void getLabel_returns_label() {
        var path = new DataversePath("dir/subdir/file");
        assertThat(path.getLabel()).isEqualTo("file");
    }

}
