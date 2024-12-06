package nl.knaw.dans.dvingest.core.bagprocessor;

import nl.knaw.dans.dvingest.core.service.DataverseService;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FilesInDatasetCacheTest {
    private final DataverseService dataverseServiceMock = Mockito.mock(DataverseService.class);

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(dataverseServiceMock);
    }

    @Test
    public void get_returns_fileMeta_by_filepath() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of());
        var fileMeta = new FileMeta();
        fileMeta.setLabel("label");
        fileMeta.setDirectoryLabel("directoryLabel");

        // When
        filesInDatasetCache.put(fileMeta);

        // Then
        assertThat(filesInDatasetCache.get("directoryLabel/label")).isEqualTo(fileMeta);
    }

    @Test
    public void put_auto_renames_filepath() {
        // Given
        var filesInDatasetCache = new FilesInDatasetCache(dataverseServiceMock, Map.of("directoryLabel/label", "newDirectoryLabel/newLabel"));
        var fileMeta = new FileMeta();
        fileMeta.setLabel("label");
        fileMeta.setDirectoryLabel("directoryLabel");

        // When
        filesInDatasetCache.put(fileMeta);
        var returnedFileMeta = filesInDatasetCache.get("newDirectoryLabel/newLabel");

        // Then
        assertThat(returnedFileMeta).isEqualTo(fileMeta);
    }

}
