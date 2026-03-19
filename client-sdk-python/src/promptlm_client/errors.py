class PromptArtifactError(Exception):
    """Raised when the prompt bundle is invalid or unreadable."""


class PromptNotFoundError(Exception):
    """Raised when a prompt id does not exist in the bundle."""

    def __init__(self, prompt_id: str) -> None:
        super().__init__(f"Unknown prompt id: {prompt_id}")
