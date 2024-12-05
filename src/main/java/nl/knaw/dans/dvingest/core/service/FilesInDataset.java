package nl.knaw.dans.dvingest.core.service;

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.io.IOException;

/**
 * Interface for a service that keeps track of the files in a dataset. The key to each file is its path in the dataset, i.e. the combination of label and directoryLabel.
 */
public interface FilesInDataset {

    /**
     * Gets the FileMeta object for the file with the same path as the given FileMeta object.
     *
     * @param fileMeta the FileMeta object with the path of the file to get
     * @return the FileMeta object for the file with the same path as the given FileMeta object, or null if no such file exists
     */
    FileMeta get(FileMeta fileMeta) throws IOException, DataverseException;

    /**
     * Adds or overwrites the FileMeta object for the file with the same path as the given FileMeta object.
     *
     * @param fileMeta the FileMeta object to put
     */
    void put(FileMeta fileMeta) throws IOException, DataverseException;

    /**
     * Removes the FileMeta object for the file with the same path as the given FileMeta object.
     *
     * @param fileMeta the FileMeta object to remove
     */
    void remove(FileMeta fileMeta) throws IOException, DataverseException;
}
