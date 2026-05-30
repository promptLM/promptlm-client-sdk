# client-sdk-java

Java client SDK for promptLM. Loads prompt bundles packaged on the
JVM classpath via a small, dependency-light API.

JDK 17+ is required.

## Install

### Maven

```xml
<dependency>
    <groupId>dev.promptlm</groupId>
    <artifactId>promptlm-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("dev.promptlm:promptlm-client:0.1.0")
}
```

## Usage

Drop a `prompts/prompt-index.json` (plus the prompt YAML files it
references) anywhere on your classpath — typically under
`src/main/resources` — and resolve a prompt by its id:

```java
import dev.promptlm.client.ClasspathPromptLoader;
import dev.promptlm.client.Prompt;
import dev.promptlm.client.PromptLoader;

public class Example {
    public static void main(String[] args) {
        PromptLoader loader = new ClasspathPromptLoader();
        Prompt prompt = loader.loadPrompt("greeting.v1");
        System.out.println(prompt.getName());
        System.out.println(prompt.getPrompt());
    }
}
```

The loader scans every classpath entry once on first use, builds an
in-memory index, and rejects duplicate prompt ids across bundles.

## Release

Release mechanics are documented in
[`../docs/releasing-java.md`](../docs/releasing-java.md).
