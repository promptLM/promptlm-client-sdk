from dataclasses import dataclass


@dataclass(frozen=True)
class Prompt:
    id: str
    version: str
    name: str
    path: str
    prompt: str
