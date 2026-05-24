package dev.promptlm.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URL;
import java.nio.file.Files;


public class ClasspathPromptLoader implements PromptLoader {
    /**
     * Set of cached prompts
     */
//    private static final Map<String, Prompt> promptIndex = new HashMap<>();

    public static final String INDEX_JSON = "prompts/prompt-index.json";

    private static class Loader {
        private static final Map<String, Prompt> promptIndex = new HashMap<>();
        private static final Set<String> loadedSources = new HashSet<>();

        static {
            try {
                URL indexUrl = ClasspathPromptLoader.class.getClassLoader().getResource(INDEX_JSON);
                if (indexUrl != null) {
                    loadIndex(sourceIdFromUrl(indexUrl), indexUrl.openStream());
                }

                String cp = System.getProperty("java.class.path");
                Arrays.stream(cp.split(File.pathSeparator)).sorted().forEach(path -> {
                    Path cpPath = Path.of(path);
                    if (Files.isDirectory(cpPath)) {
                        Path indexPath = cpPath.resolve(INDEX_JSON);
                        if (Files.exists(indexPath)) {
                            try {
                                loadIndex("dir:" + indexPath.toAbsolutePath(), Files.newInputStream(indexPath));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return;
                    }

                    if (path.endsWith(".jar")) {
                        try (JarFile jarFile = new JarFile(path)) {
                            JarEntry entry = jarFile.getJarEntry(INDEX_JSON);
                            if (entry != null) {
                                loadIndex("jar:" + path, jarFile.getInputStream(entry));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static boolean containsKey(String promptId) {
            return promptIndex.containsKey(promptId);
        }

        public static void put(String id, Prompt prompt) {
            promptIndex.put(id, prompt);
        }

        public static Prompt get(String promptId) {
            return promptIndex.get(promptId);
        }
    }

    private static String sourceIdFromUrl(URL url) {
        if ("jar".equals(url.getProtocol())) {
            String path = url.getPath();
            int bang = path.indexOf('!');
            if (bang > 0) {
                String jarPath = path.substring(0, bang);
                if (jarPath.startsWith("file:")) {
                    jarPath = jarPath.substring("file:".length());
                }
                return "jar:" + jarPath;
            }
        }

        if ("file".equals(url.getProtocol())) {
            return "dir:" + Path.of(url.getPath()).toAbsolutePath();
        }

        return "url:" + url;
    }

    private static void loadIndex(String sourceId, InputStream stream) throws IOException {
        if (stream == null || !Loader.loadedSources.add(sourceId)) {
            return;
        }

        try (InputStream is = stream) {
            initPromptIndex(is);
        }
    }

    private static void initPromptIndex(InputStream is) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Prompt> prompts = objectMapper.readValue(is, new TypeReference<>() {
            });
            prompts.forEach(prompt -> {
                if (Loader.containsKey(prompt.getId())) {
                    throw new IllegalStateException("Duplicate prompt id: " + prompt.getId());
                }
                Loader.put(prompt.getId(), prompt);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Prompt loadPrompt(String promptId) {
        Prompt indexPrompt = Loader.get(promptId);

        if (indexPrompt == null) {
            throw new IllegalArgumentException("Unknown Prompt ID: " + promptId);
        }

        Path path = indexPrompt.getPath();
        String resourcePath = path.toString().startsWith("/") ? path.toString() : "/" + path.toString();

        try (InputStream is = ClasspathPromptLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null)
                throw new RuntimeException("Prompt file not found at path: " + resourcePath + " for prompt: " + indexPrompt);
            
            // Parse YAML file to get the prompt content
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Prompt yamlPrompt = yamlMapper.readValue(is, Prompt.class);
            
            return new Prompt(
                    indexPrompt.getId(),
                    indexPrompt.getVersion(),
                    indexPrompt.getName(),
                    yamlPrompt.getPrompt(),
                    indexPrompt.getPath()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
