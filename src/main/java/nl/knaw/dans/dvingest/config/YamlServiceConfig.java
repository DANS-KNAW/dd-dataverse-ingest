package nl.knaw.dans.dvingest.config;

import lombok.Data;
import org.yaml.snakeyaml.LoaderOptions;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class YamlServiceConfig {

    @Valid
    @NotNull
    private LoaderOptions loaderOptions = new LoaderOptions();
}
