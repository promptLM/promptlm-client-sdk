/*
 * Copyright 2025 promptLM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.promptlm.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;

/**
 * Immutable value object representing a single prompt loaded from a
 * promptLM prompt bundle. Carries the identifier, version, display
 * name, the prompt body, and the relative classpath path of the
 * source file.
 */
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

    /** @return the stable identifier of this prompt. */
    public String getId() {
        return id;
    }

    /** @return the version string of this prompt. */
    public String getVersion() {
        return version;
    }

    /** @return the human-readable display name of this prompt. */
    public String getName() {
        return name;
    }

    /** @return the prompt body text. */
    public String getPrompt() {
        return prompt;
    }

    /** @return the relative classpath path of the prompt source file. */
    public Path getPath() {
        return path;
    }
}
