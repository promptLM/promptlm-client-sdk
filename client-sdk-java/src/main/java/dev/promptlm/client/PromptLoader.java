package dev.promptlm.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public interface PromptLoader {

    Prompt loadPrompt(String promptId);
}