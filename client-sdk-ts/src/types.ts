export type Prompt = {
  id: string;
  version: string;
  name: string;
  path: string;
  prompt: string;
};

export type PromptIndexEntry = {
  id: string;
  version: string;
  name: string;
  path: string;
};

export interface PromptLoader {
  loadPrompt(id: string): Promise<Prompt>;
}

export interface PromptSource {
  readText(relativePath: string): Promise<string>;
}
