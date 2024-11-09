package nl.knaw.dans.dvingest.core;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;

public abstract class TestDirFixture {
    protected Path testDir = Path.of("target/test/" + getClass().getSimpleName());

    @BeforeEach
    public void setUp() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
        FileUtils.forceMkdir(testDir.toFile());
    }
}
