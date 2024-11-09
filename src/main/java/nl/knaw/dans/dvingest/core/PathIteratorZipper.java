package nl.knaw.dans.dvingest.core;

import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

// TODO: move to dans-java-utils
@Builder
public class PathIteratorZipper {
    @NonNull
    private final Path rootDir;
    @NonNull
    private final Iterator<Path> sourceIterator;
    @NonNull
    private final Path targetZipFile;
    @Builder.Default
    private final boolean overwrite = true;
    @Builder.Default
    private final boolean compress = false;
    @Builder.Default
    private final int maxNumberOfFiles = Integer.MAX_VALUE;

    public void zip() throws IOException {
        if (overwrite && Files.exists(targetZipFile)) {
            Files.delete(targetZipFile);
        } else {
            if (Files.exists(targetZipFile)) {
                throw new IOException("Target zip file already exists: " + targetZipFile);
            }
        }

        try (OutputStream outputStream = Files.newOutputStream(targetZipFile)) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {
                int count = 0;
                while (sourceIterator.hasNext() && count < maxNumberOfFiles) {
                    Path path = sourceIterator.next();
                    if (Files.isRegularFile(path)) {
                        try {
                            addFileToZipStream(zipArchiveOutputStream, path);
                            count++;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    private void addFileToZipStream(ZipArchiveOutputStream zipArchiveOutputStream, Path fileToZip) throws IOException {
        if (!fileToZip.startsWith(rootDir)) {
            throw new IllegalArgumentException("File to zip is not a descendant of root directory: " + fileToZip);
        }
        String entryName = rootDir.relativize(fileToZip).toString();
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(fileToZip, entryName);
        zipArchiveEntry.setMethod(compress ? ZipArchiveEntry.STORED : ZipArchiveEntry.DEFLATED);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        try (InputStream fileInputStream = Files.newInputStream(fileToZip)) {
            IOUtils.copy(fileInputStream, zipArchiveOutputStream);
            zipArchiveOutputStream.closeArchiveEntry();
        }
    }
}