from __future__ import annotations

import json
from typing import Any

from .errors import PromptArtifactError, PromptNotFoundError
from .models import Prompt

INDEX_PATH = "prompts/prompt-index.json"


def _require_string(value: Any, field_name: str, source: str) -> str:
    if not isinstance(value, str) or not value:
        raise PromptArtifactError(f"Missing or invalid {field_name} in {source}")
    return value


class JsonPromptLoader:
    def __init__(self, source: Any) -> None:
        self._source = source
        self._index: dict[str, dict[str, str]] | None = None

    def load_prompt(self, prompt_id: str) -> Prompt:
        index = self._load_index()
        entry = index.get(prompt_id)
        if entry is None:
            raise PromptNotFoundError(prompt_id)

        raw_prompt = self._source.read_text(entry["path"])
        try:
            parsed = json.loads(raw_prompt)
        except json.JSONDecodeError as error:
            raise PromptArtifactError(f"Invalid JSON in {entry['path']}: {error}") from error

        if not isinstance(parsed, dict):
            raise PromptArtifactError(f"Prompt file {entry['path']} must contain a JSON object")

        return Prompt(
            id=_require_string(parsed.get("id"), "id", entry["path"]),
            version=_require_string(parsed.get("version"), "version", entry["path"]),
            name=_require_string(parsed.get("name"), "name", entry["path"]),
            path=entry["path"],
            prompt=_require_string(parsed.get("prompt"), "prompt", entry["path"]),
        )

    def _load_index(self) -> dict[str, dict[str, str]]:
        if self._index is None:
            self._index = self._read_index()
        return self._index

    def _read_index(self) -> dict[str, dict[str, str]]:
        raw_index = self._source.read_text(INDEX_PATH)
        try:
            parsed = json.loads(raw_index)
        except json.JSONDecodeError as error:
            raise PromptArtifactError(f"Invalid JSON in {INDEX_PATH}: {error}") from error

        if not isinstance(parsed, list):
            raise PromptArtifactError(f"{INDEX_PATH} must contain a JSON array")

        index: dict[str, dict[str, str]] = {}
        for entry in parsed:
            if not isinstance(entry, dict):
                raise PromptArtifactError(f"{INDEX_PATH} contains an invalid entry")

            candidate = {
                "id": _require_string(entry.get("id"), "id", INDEX_PATH),
                "version": _require_string(entry.get("version"), "version", INDEX_PATH),
                "name": _require_string(entry.get("name"), "name", INDEX_PATH),
                "path": _require_string(entry.get("path"), "path", INDEX_PATH),
            }

            if candidate["id"] in index:
                raise PromptArtifactError(
                    f"Duplicate prompt id in {INDEX_PATH}: {candidate['id']}"
                )

            index[candidate["id"]] = candidate

        return index
