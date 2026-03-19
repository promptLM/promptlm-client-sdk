package dev.promptlm.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptTest {

    @Test
    void exposesConstructorValuesAndSupportsPromptMutation() {
        Prompt prompt = new Prompt("id-1", "1.0.0", "greeting", "hello", Path.of("prompts/greeting.yml"));

        assertEquals("id-1", prompt.getPromptId());
        assertEquals("id-1", prompt.getId());
        assertEquals("1.0.0", prompt.getVersion());
        assertEquals("greeting", prompt.getName());
        assertEquals(Path.of("prompts/greeting.yml"), prompt.getPath());
        assertEquals("hello", prompt.getPrompt());

        prompt.setPrompt("updated");
        assertEquals("updated", prompt.getPrompt());
    }
}
