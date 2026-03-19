import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

import { PromptArtifactError } from "./errors.js";
import type { PromptSource } from "./types.js";

function resolveSafePath(rootDir: string, relativePath: string): string {
  if (!relativePath) {
    throw new PromptArtifactError("Prompt path must not be empty");
  }

  const normalized = path.posix.normalize(relativePath);
  if (normalized.startsWith("../") || normalized === ".." || path.posix.isAbsolute(normalized)) {
    throw new PromptArtifactError(`Prompt path escapes source root: ${relativePath}`);
  }

  const resolvedRoot = path.resolve(rootDir);
  const resolvedPath = path.resolve(resolvedRoot, normalized);
  const relative = path.relative(resolvedRoot, resolvedPath);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    throw new PromptArtifactError(`Prompt path escapes source root: ${relativePath}`);
  }

  return resolvedPath;
}

export class DirectoryPromptSource implements PromptSource {
  constructor(private readonly rootDir: string) {}

  async readText(relativePath: string): Promise<string> {
    const filePath = resolveSafePath(this.rootDir, relativePath);
    try {
      return await readFile(filePath, "utf8");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      throw new PromptArtifactError(`Could not read prompt bundle file ${relativePath}: ${message}`);
    }
  }
}

export class PackageResourcePromptSource extends DirectoryPromptSource {
  constructor(rootDir = fileURLToPath(new URL("../resources", import.meta.url))) {
    super(rootDir);
  }
}
