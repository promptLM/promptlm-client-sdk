package dev.promptlm.client;

import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.jfr.DataAmount;

public class Prompt {
    private final String promptId;
    private final String version;
    private final String name;
    private String prompt;
    private final Path path;

    @JsonCreator
    public Prompt(@JsonProperty("id") String promptId, @JsonProperty("version") String version, @JsonProperty("name") String name, @JsonProperty("prompt") String prompt, @JsonProperty("path") Path path) {
        this.promptId = promptId;
        this.version = version;
        this.name = name;
        this.prompt = prompt;
        this.path = path;
    }

    public String getPromptId() {
        return promptId;
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

    public String getId() {
        return  promptId;
    }

    public Path getPath() {
        return path;
    }

    public void setPrompt(String s) {
        this.prompt = s;
    }
}
