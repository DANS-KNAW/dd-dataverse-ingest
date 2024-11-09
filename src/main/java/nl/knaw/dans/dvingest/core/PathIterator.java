package nl.knaw.dans.dvingest.core;

import lombok.AllArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

// TODO: move to dans-java-utils
@AllArgsConstructor
public class PathIterator implements Iterator<Path> {
    private final Iterator<File> fileIterator;

    @Override
    public boolean hasNext() {
        return fileIterator.hasNext();
    }

    @Override
    public Path next() {
        return fileIterator.next().toPath();
    }
}