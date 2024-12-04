package nl.knaw.dans.dvingest.core.dansbag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lightweight bag-info.txt parser, which can be used without reading the whole bag.
 */
public class LightweightBagInfo {
    private final Map<String, String> keyValues;

    public LightweightBagInfo(Path file) throws IOException {
        try (var linesStream = Files.lines(file)) {
            keyValues = linesStream
                .map(line -> line.split(":", 2))
                .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
        }
    }

    public String get(String key) {
        return keyValues.get(key);
    }
}
