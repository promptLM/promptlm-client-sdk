package dev.promptlm.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathPromptLoaderTest {

    private static Map<String, Prompt> promptIndex() {
        try {
            Class<?> loaderClass = Class.forName("dev.promptlm.client.ClasspathPromptLoader$Loader");
            Field field = loaderClass.getDeclaredField("promptIndex");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Prompt> map = (Map<String, Prompt>) field.get(null);
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void initPromptIndexFromJson(String json) {
        try {
            Method method = ClasspathPromptLoader.class.getDeclaredMethod("initPromptIndex", InputStream.class);
            method.setAccessible(true);
            method.invoke(null, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    @Test
    void returnsNewPromptInstanceOnEachLoad() {
        ClasspathPromptLoader loader = new ClasspathPromptLoader();

        Prompt prompt1 = loader.loadPrompt("translate");
        Prompt prompt2 = loader.loadPrompt("translate");

        assertNotSame(prompt1, prompt2);
        assertEquals(prompt1.getId(), prompt2.getId());
        assertEquals(prompt1.getVersion(), prompt2.getVersion());
        assertEquals(prompt1.getName(), prompt2.getName());
        assertEquals(prompt1.getPrompt(), prompt2.getPrompt());
        assertEquals(prompt1.getPath(), prompt2.getPath());
    }

    @Test
    void throwsWhenYamlResourceIsMissing() {
        Map<String, Prompt> index = promptIndex();
        Map<String, Prompt> backup = new HashMap<>(index);
        try {
            index.put(
                    "missing-yaml",
                    new Prompt("missing-yaml", "1.0.0", "Missing YAML", null, Path.of("prompts/does-not-exist.yml"))
            );

            ClasspathPromptLoader loader = new ClasspathPromptLoader();
            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> loader.loadPrompt("missing-yaml"));

            assertTrue(exception.getMessage().contains("Prompt file not found"));
            assertTrue(exception.getMessage().contains("/prompts/does-not-exist.yml"));
        } finally {
            index.clear();
            index.putAll(backup);
        }
    }

    @Test
    void throwsWhenYamlIsInvalid() {
        Map<String, Prompt> index = promptIndex();
        Map<String, Prompt> backup = new HashMap<>(index);
        try {
            index.put(
                    "invalid-yaml",
                    new Prompt("invalid-yaml", "1.0.0", "Invalid YAML", null, Path.of("prompts/text/invalid/promptlm.yml"))
            );

            ClasspathPromptLoader loader = new ClasspathPromptLoader();
            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> loader.loadPrompt("invalid-yaml"));

            assertNotNull(exception.getCause());
        } finally {
            index.clear();
            index.putAll(backup);
        }
    }

    @Test
    void throwsOnDuplicatePromptIdInIndex() {
        Map<String, Prompt> index = promptIndex();
        Map<String, Prompt> backup = new HashMap<>(index);
        try {
            index.clear();

            String json = "[" +
                    "{\"id\":\"dup\",\"version\":\"1.0.0\",\"name\":\"A\",\"path\":\"prompts/a.yml\"}," +
                    "{\"id\":\"dup\",\"version\":\"1.0.0\",\"name\":\"B\",\"path\":\"prompts/b.yml\"}" +
                    "]";

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> initPromptIndexFromJson(json));
            assertTrue(exception.getMessage().contains("Duplicate prompt id"));
        } finally {
            index.clear();
            index.putAll(backup);
        }
    }
}
