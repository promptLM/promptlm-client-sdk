package dev.promptlm.client;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Prompt {
    private final String id;
    private final String version;
    private final String name;
    private final String prompt;
    private final Path path;

    @JsonCreator
    public Prompt(@JsonProperty("id") String id, @JsonProperty("version") String version, @JsonProperty("name") String name, @JsonProperty("prompt") String prompt, @JsonProperty("path") Path path) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.prompt = prompt;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getPrompt() {
        return prompt;
    }

    public Path getPath() {
        return path;
    }
}
