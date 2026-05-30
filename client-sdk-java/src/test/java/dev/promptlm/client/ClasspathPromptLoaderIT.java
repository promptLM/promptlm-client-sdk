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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;

class ClasspathPromptLoaderIT {

    @Test
    void loadsPromptFromPromptBundleJarUsingPackagedSdkJar() throws Exception {
        String projectVersion = System.getProperty("projectVersion");
        assertNotNull(projectVersion);

        Path testClassesDir = Path.of(
            ClasspathPromptLoaderIT.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        );
        Path targetDir = testClassesDir.getParent();
        assertNotNull(targetDir);

        Path sdkJar = findSdkJar(targetDir, projectVersion);
        assertTrue(Files.exists(sdkJar));

        Path promptBundleJar = Files.createTempFile("prompt-bundle-", ".jar");
        createPromptBundleJar(promptBundleJar);

        Set<URL> urls = new LinkedHashSet<>();
        urls.add(sdkJar.toUri().toURL());
        urls.add(promptBundleJar.toUri().toURL());

        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            for (String entry : cp.split(File.pathSeparator)) {
                if (entry.endsWith(".jar")) {
                    urls.add(Path.of(entry).toUri().toURL());
                }
            }
        }

        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", sdkJar.toString());
        try (URLClassLoader isolated = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader())) {
            Class<?> loaderClass = Class.forName("dev.promptlm.client.ClasspathPromptLoader", true, isolated);
            URL loaderCodeSource = loaderClass.getProtectionDomain().getCodeSource().getLocation();
            assertTrue(loaderCodeSource.toString().endsWith(".jar"));

            Object loader = loaderClass.getConstructor().newInstance();
            Object prompt = loaderClass.getMethod("loadPrompt", String.class).invoke(loader, "translate");

            String name = (String) prompt.getClass().getMethod("getName").invoke(prompt);
            String promptText = (String) prompt.getClass().getMethod("getPrompt").invoke(prompt);

            assertEquals("Text Translator", name);
            assertEquals("Translate the following text.\n", promptText);
        } finally {
            if (originalClasspath != null) {
                System.setProperty("java.class.path", originalClasspath);
            }
        }
    }

    private static Path findSdkJar(Path targetDir, String projectVersion) throws IOException {
        Path jar = targetDir.resolve("promptlm-client-" + projectVersion + ".jar");
        if (Files.exists(jar)) {
            return jar;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "promptlm-client-*.jar")) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate) && !candidate.getFileName().toString().endsWith("-sources.jar") && !candidate.getFileName().toString().endsWith("-javadoc.jar")) {
                    return candidate;
                }
            }
        }

        return jar;
    }

    private static void createPromptBundleJar(Path jarPath) throws IOException {
        byte[] indexJson = (
                "[" +
                        "{\"id\":\"translate\",\"version\":\"1.0.0\",\"name\":\"Text Translator\",\"path\":\"prompts/text/translate/promptlm.yml\"}" +
                        "]"
        ).getBytes(StandardCharsets.UTF_8);

        byte[] translateYaml = (
                "id: translate\n" +
                        "name: Text Translator\n" +
                        "version: 1.0.0\n" +
                        "prompt: |\n" +
                        "  Translate the following text.\n"
        ).getBytes(StandardCharsets.UTF_8);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            putEntry(jos, "prompts/prompt-index.json", indexJson);
            putEntry(jos, "prompts/text/translate/promptlm.yml", translateYaml);
        }
    }

    private static void putEntry(JarOutputStream jos, String name, byte[] content) throws IOException {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(content);
        jos.closeEntry();
    }
}
