import { PromptArtifactError, PromptNotFoundError } from "./errors.js";
import type { Prompt, PromptIndexEntry, PromptLoader, PromptSource } from "./types.js";

const INDEX_PATH = "prompts/prompt-index.json";

function requireString(value: unknown, field: string, source: string): string {
  if (typeof value !== "string" || value.length === 0) {
    throw new PromptArtifactError(`Missing or invalid ${field} in ${source}`);
  }
  return value;
}

export class JsonPromptLoader implements PromptLoader {
  private indexPromise?: Promise<Map<string, PromptIndexEntry>>;

  constructor(private readonly source: PromptSource) {}

  async loadPrompt(id: string): Promise<Prompt> {
    const index = await this.loadIndex();
    const entry = index.get(id);
    if (!entry) {
      throw new PromptNotFoundError(id);
    }

    const rawPrompt = await this.source.readText(entry.path);
    let parsed: unknown;
    try {
      parsed = JSON.parse(rawPrompt);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      throw new PromptArtifactError(`Invalid JSON in ${entry.path}: ${message}`);
    }

    if (!parsed || typeof parsed !== "object") {
      throw new PromptArtifactError(`Prompt file ${entry.path} must contain a JSON object`);
    }

    const promptRecord = parsed as Record<string, unknown>;
    return {
      id: requireString(promptRecord.id, "id", entry.path),
      version: requireString(promptRecord.version, "version", entry.path),
      name: requireString(promptRecord.name, "name", entry.path),
      path: entry.path,
      prompt: requireString(promptRecord.prompt, "prompt", entry.path)
    };
  }

  private loadIndex(): Promise<Map<string, PromptIndexEntry>> {
    if (!this.indexPromise) {
      this.indexPromise = this.readIndex();
    }
    return this.indexPromise;
  }

  private async readIndex(): Promise<Map<string, PromptIndexEntry>> {
    const rawIndex = await this.source.readText(INDEX_PATH);
    let parsed: unknown;
    try {
      parsed = JSON.parse(rawIndex);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      throw new PromptArtifactError(`Invalid JSON in ${INDEX_PATH}: ${message}`);
    }

    if (!Array.isArray(parsed)) {
      throw new PromptArtifactError(`${INDEX_PATH} must contain a JSON array`);
    }

    const index = new Map<string, PromptIndexEntry>();
    for (const candidate of parsed) {
      if (!candidate || typeof candidate !== "object") {
        throw new PromptArtifactError(`${INDEX_PATH} contains an invalid entry`);
      }

      const entryRecord = candidate as Record<string, unknown>;
      const entry: PromptIndexEntry = {
        id: requireString(entryRecord.id, "id", INDEX_PATH),
        version: requireString(entryRecord.version, "version", INDEX_PATH),
        name: requireString(entryRecord.name, "name", INDEX_PATH),
        path: requireString(entryRecord.path, "path", INDEX_PATH)
      };

      if (index.has(entry.id)) {
        throw new PromptArtifactError(`Duplicate prompt id in ${INDEX_PATH}: ${entry.id}`);
      }

      index.set(entry.id, entry);
    }

    return index;
  }
}
