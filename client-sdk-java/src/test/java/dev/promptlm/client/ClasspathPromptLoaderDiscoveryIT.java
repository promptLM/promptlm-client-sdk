package dev.promptlm.client;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathPromptLoaderDiscoveryIT {

    @Test
    void discoversIndexFromDirectoryOnJavaClassPath() throws Exception {
        String projectVersion = System.getProperty("projectVersion");
        assertNotNull(projectVersion);

        Path targetDir = targetDir();
        Path sdkJar = findSdkJar(targetDir, projectVersion);

        Path promptRoot = Files.createTempDirectory("prompt-dir-");
        writePromptBundleToDirectory(promptRoot, "translate", "Translate the following text.\n");

        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", buildClasspath(originalClasspath, sdkJar, promptRoot.toString()));
        try (URLClassLoader isolated = new URLClassLoader(isolatedUrls(sdkJar, promptRoot.toUri().toURL()), ClassLoader.getPlatformClassLoader())) {
            Object prompt = loadPromptViaIsolatedLoader(isolated, "translate");

            String name = (String) prompt.getClass().getMethod("getName").invoke(prompt);
            String promptText = (String) prompt.getClass().getMethod("getPrompt").invoke(prompt);

            assertEquals("Text Translator", name);
            assertEquals("Translate the following text.\n", promptText);
        } finally {
            restoreClasspath(originalClasspath);
        }
    }

    @Test
    void discoversIndexFromJarOnJavaClassPath() throws Exception {
        String projectVersion = System.getProperty("projectVersion");
        assertNotNull(projectVersion);

        Path targetDir = targetDir();
        Path sdkJar = findSdkJar(targetDir, projectVersion);

        Path promptBundleJar = Files.createTempFile("prompt-bundle-", ".jar");
        createPromptBundleJar(promptBundleJar, "translate", "Translate the following text.\n");

        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", buildClasspath(originalClasspath, sdkJar, promptBundleJar.toString()));
        try (URLClassLoader isolated = new URLClassLoader(isolatedUrls(sdkJar, promptBundleJar.toUri().toURL()), ClassLoader.getPlatformClassLoader())) {
            Object prompt = loadPromptViaIsolatedLoader(isolated, "translate");

            String name = (String) prompt.getClass().getMethod("getName").invoke(prompt);
            String promptText = (String) prompt.getClass().getMethod("getPrompt").invoke(prompt);

            assertEquals("Text Translator", name);
            assertEquals("Translate the following text.\n", promptText);
        } finally {
            restoreClasspath(originalClasspath);
        }
    }

    @Test
    void throwsOnDuplicatePromptIdAcrossSources() throws Exception {
        String projectVersion = System.getProperty("projectVersion");
        assertNotNull(projectVersion);

        Path targetDir = targetDir();
        Path sdkJar = findSdkJar(targetDir, projectVersion);

        Path promptRoot = Files.createTempDirectory("prompt-dir-");
        writePromptBundleToDirectory(promptRoot, "dup", "first\n");

        Path promptBundleJar = Files.createTempFile("prompt-bundle-", ".jar");
        createPromptBundleJar(promptBundleJar, "dup", "second\n");

        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty(
                "java.class.path",
                buildClasspath(originalClasspath, sdkJar, promptRoot.toString(), promptBundleJar.toString())
        );
        try (URLClassLoader isolated = new URLClassLoader(isolatedUrls(sdkJar, promptRoot.toUri().toURL(), promptBundleJar.toUri().toURL()), ClassLoader.getPlatformClassLoader())) {
            InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> {
                loadPromptViaIsolatedLoader(isolated, "dup");
            });

            Throwable cause = error.getCause();
            if (cause instanceof ExceptionInInitializerError initializerError) {
                cause = initializerError.getCause();
            }

            assertNotNull(cause);
            assertTrue(cause.getMessage().contains("Duplicate prompt id"));
        } finally {
            restoreClasspath(originalClasspath);
        }
    }

    private static Object loadPromptViaIsolatedLoader(URLClassLoader isolated, String promptId) throws Exception {
        Class<?> loaderClass = Class.forName("dev.promptlm.client.ClasspathPromptLoader", true, isolated);
        URL codeSource = loaderClass.getProtectionDomain().getCodeSource().getLocation();
        assertTrue(codeSource.toString().endsWith(".jar"));

        Object loader = loaderClass.getConstructor().newInstance();
        return loaderClass.getMethod("loadPrompt", String.class).invoke(loader, promptId);
    }

    private static void writePromptBundleToDirectory(Path root, String promptId, String promptText) throws IOException {
        Path indexPath = root.resolve("prompts/prompt-index.json");
        Files.createDirectories(indexPath.getParent());

        String indexJson = """
                [{"id":"%s","version":"1.0.0","name":"Text Translator","path":"prompts/text/translate/promptlm.yml"}]
                """.formatted(promptId);
        Files.writeString(indexPath, indexJson, StandardCharsets.UTF_8);

        Path yamlPath = root.resolve("prompts/text/translate/promptlm.yml");
        Files.createDirectories(yamlPath.getParent());

        String yaml = "id: " + promptId + "\n" +
                "name: Text Translator\n" +
                "version: 1.0.0\n" +
                "prompt: |\n" +
                "  " + promptText.replace("\n", "\n  ");

        Files.writeString(yamlPath, yaml, StandardCharsets.UTF_8);
    }

    private static void createPromptBundleJar(Path jarPath, String promptId, String promptText) throws IOException {
        byte[] indexJson = (
                """
                [
                {"id":"%s","version":"1.0.0","name":"Text Translator","path":"prompts/text/translate/promptlm.yml"}
                ]""".formatted(promptId)
        ).getBytes(StandardCharsets.UTF_8);

        String indentedPromptText = promptText.replace("\n", "\n  ");
        byte[] translateYaml = (
                """
                id: %s
                name: Text Translator
                version: 1.0.0
                prompt: |
                  %s
                """.formatted(promptId, indentedPromptText)
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

    private static void restoreClasspath(String originalClasspath) {
        if (originalClasspath != null) {
            System.setProperty("java.class.path", originalClasspath);
        }
    }

    private static String buildClasspath(String originalClasspath, Path sdkJar, String... extraEntries) {
        StringBuilder cp = new StringBuilder();

        cp.append(sdkJar);
        for (String extra : extraEntries) {
            cp.append(File.pathSeparator).append(extra);
        }

        if (originalClasspath != null) {
            for (String entry : originalClasspath.split(File.pathSeparator)) {
                if (entry.endsWith(".jar") && !entry.equals(sdkJar.toString())) {
                    cp.append(File.pathSeparator).append(entry);
                }
            }
        }

        return cp.toString();
    }

    private static URL[] isolatedUrls(Path sdkJar, URL... extra) throws IOException {
        Set<URL> urls = new LinkedHashSet<>();
        urls.add(sdkJar.toUri().toURL());
        for (URL url : extra) {
            urls.add(url);
        }

        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            for (String entry : cp.split(File.pathSeparator)) {
                if (entry.endsWith(".jar")) {
                    urls.add(Path.of(entry).toUri().toURL());
                }
            }
        }

        return urls.toArray(new URL[0]);
    }

    private static Path targetDir() throws Exception {
        Path testClassesDir = Path.of(
                ClasspathPromptLoaderDiscoveryIT.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        );
        Path targetDir = testClassesDir.getParent();
        assertNotNull(targetDir);
        return targetDir;
    }

    private static Path findSdkJar(Path targetDir, String projectVersion) throws IOException {
        Path jar = targetDir.resolve("promptlm-client-java-" + projectVersion + ".jar");
        if (Files.exists(jar)) {
            return jar;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "promptlm-client-java-*.jar")) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate)
                        && !candidate.getFileName().toString().endsWith("-sources.jar")
                        && !candidate.getFileName().toString().endsWith("-javadoc.jar")) {
                    return candidate;
                }
            }
        }

        return jar;
    }
}
