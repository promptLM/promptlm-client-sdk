import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from promptlm_client import (
    DirectoryPromptSource,
    JsonPromptLoader,
    PackageResourcePromptSource,
    PromptArtifactError,
    PromptNotFoundError,
)


FIXTURE_ROOT = Path(__file__).resolve().parents[2] / "fixtures" / "prompt-bundle"


def create_bundle(files: dict[str, str]) -> Path:
    root = Path(tempfile.mkdtemp(prefix="promptlm-py-"))
    for relative_path, content in files.items():
        target = root / relative_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
    return root


class JsonPromptLoaderTest(unittest.TestCase):
    def test_loads_prompt_from_shared_fixture_bundle(self) -> None:
        loader = JsonPromptLoader(DirectoryPromptSource(FIXTURE_ROOT))

        prompt = loader.load_prompt("translate")

        self.assertEqual(prompt.id, "translate")
        self.assertEqual(prompt.version, "1.0.0")
        self.assertEqual(prompt.name, "Text Translator")
        self.assertEqual(prompt.path, "prompts/text/translate/promptlm.json")
        self.assertEqual(prompt.prompt, "Translate the following text.\n")

    def test_loads_prompt_from_packaged_resources(self) -> None:
        loader = JsonPromptLoader(PackageResourcePromptSource())

        prompt = loader.load_prompt("translate")

        self.assertEqual(prompt.prompt, "Translate the following text.\n")

    def test_rejects_unknown_prompt_ids(self) -> None:
        loader = JsonPromptLoader(DirectoryPromptSource(FIXTURE_ROOT))

        with self.assertRaises(PromptNotFoundError):
            loader.load_prompt("missing")

    def test_rejects_duplicate_prompt_ids(self) -> None:
        root = create_bundle(
            {
                "prompts/prompt-index.json": json.dumps(
                    [
                        {
                            "id": "translate",
                            "version": "1.0.0",
                            "name": "A",
                            "path": "prompts/a/promptlm.json",
                        },
                        {
                            "id": "translate",
                            "version": "1.0.0",
                            "name": "B",
                            "path": "prompts/b/promptlm.json",
                        },
                    ]
                )
            }
        )

        loader = JsonPromptLoader(DirectoryPromptSource(root))
        with self.assertRaises(PromptArtifactError):
            loader.load_prompt("translate")

    def test_rejects_missing_prompt_files(self) -> None:
        root = create_bundle(
            {
                "prompts/prompt-index.json": json.dumps(
                    [
                        {
                            "id": "translate",
                            "version": "1.0.0",
                            "name": "Text Translator",
                            "path": "prompts/text/translate/promptlm.json",
                        }
                    ]
                )
            }
        )

        loader = JsonPromptLoader(DirectoryPromptSource(root))
        with self.assertRaises(PromptArtifactError):
            loader.load_prompt("translate")

    def test_rejects_malformed_prompt_json(self) -> None:
        root = create_bundle(
            {
                "prompts/prompt-index.json": json.dumps(
                    [
                        {
                            "id": "translate",
                            "version": "1.0.0",
                            "name": "Text Translator",
                            "path": "prompts/text/translate/promptlm.json",
                        }
                    ]
                ),
                "prompts/text/translate/promptlm.json": "{not valid json",
            }
        )

        loader = JsonPromptLoader(DirectoryPromptSource(root))
        with self.assertRaises(PromptArtifactError):
            loader.load_prompt("translate")

    def test_rejects_prompt_files_without_prompt(self) -> None:
        root = create_bundle(
            {
                "prompts/prompt-index.json": json.dumps(
                    [
                        {
                            "id": "translate",
                            "version": "1.0.0",
                            "name": "Text Translator",
                            "path": "prompts/text/translate/promptlm.json",
                        }
                    ]
                ),
                "prompts/text/translate/promptlm.json": json.dumps(
                    {
                        "id": "translate",
                        "version": "1.0.0",
                        "name": "Text Translator",
                    }
                ),
            }
        )

        loader = JsonPromptLoader(DirectoryPromptSource(root))
        with self.assertRaises(PromptArtifactError):
            loader.load_prompt("translate")

    def test_ignores_extra_fields(self) -> None:
        root = create_bundle(
            {
                "prompts/prompt-index.json": json.dumps(
                    [
                        {
                            "id": "translate",
                            "version": "1.0.0",
                            "name": "Text Translator",
                            "path": "prompts/text/translate/promptlm.json",
                        }
                    ]
                ),
                "prompts/text/translate/promptlm.json": json.dumps(
                    {
                        "id": "translate",
                        "version": "1.0.0",
                        "name": "Text Translator",
                        "prompt": "Translate the following text.\n",
                        "description": "ignored",
                    }
                ),
            }
        )

        loader = JsonPromptLoader(DirectoryPromptSource(root))
        prompt = loader.load_prompt("translate")

        self.assertEqual(prompt.prompt, "Translate the following text.\n")


if __name__ == "__main__":
    unittest.main()
