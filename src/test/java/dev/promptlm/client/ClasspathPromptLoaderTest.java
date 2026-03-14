package dev.promptlm.client;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathPromptLoaderTest {

    @Test
    void throwsWhenPromptIdIsUnknown() {
        ClasspathPromptLoader loader = new ClasspathPromptLoader();
        String unknownPromptId = "unknown-" + UUID.randomUUID();

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> loader.loadPrompt(unknownPromptId));

        assertTrue(exception.getMessage().contains("Unknown Prompt ID"));
    }

    @Test
    void loadsPromptFromClasspathIndexAndYaml() {
        ClasspathPromptLoader loader = new ClasspathPromptLoader();

        Prompt prompt = loader.loadPrompt("translate");

        assertEquals("Text Translator", prompt.getName());
        assertEquals("1.0.0", prompt.getVersion());
        assertEquals("Translate the following text.\n", prompt.getPrompt());
    }
}
