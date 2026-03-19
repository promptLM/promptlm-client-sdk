export class PromptArtifactError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "PromptArtifactError";
  }
}

export class PromptNotFoundError extends Error {
  constructor(promptId: string) {
    super(`Unknown prompt id: ${promptId}`);
    this.name = "PromptNotFoundError";
  }
}
