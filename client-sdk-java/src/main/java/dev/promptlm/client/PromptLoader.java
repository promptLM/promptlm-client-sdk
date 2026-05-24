package dev.promptlm.client;

public interface PromptLoader {

    Prompt loadPrompt(String promptId);
}