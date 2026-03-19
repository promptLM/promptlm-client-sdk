from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


PACKAGE_ROOT = Path(__file__).resolve().parents[1]


def run(*args: str, cwd: Path) -> None:
    subprocess.run(args, cwd=cwd, check=True)


def main() -> None:
    temp_root = Path(tempfile.mkdtemp(prefix="promptlm-py-smoke-"))
    try:
        dist_dir = temp_root / "dist"
        venv_dir = temp_root / "venv"
        wheelhouse = temp_root / "wheelhouse"

        run(sys.executable, "-m", "pip", "wheel", ".", "-w", str(wheelhouse), "--no-deps", cwd=PACKAGE_ROOT)
        wheel = next(wheelhouse.glob("promptlm_client_python-*.whl"))

        run(sys.executable, "-m", "venv", str(venv_dir), cwd=PACKAGE_ROOT)
        venv_python = venv_dir / ("Scripts/python.exe" if sys.platform == "win32" else "bin/python")
        run(str(venv_python), "-m", "pip", "install", str(wheel), cwd=PACKAGE_ROOT)
        run(
            str(venv_python),
            "-c",
            (
                "from promptlm_client import JsonPromptLoader, PackageResourcePromptSource; "
                "prompt = JsonPromptLoader(PackageResourcePromptSource()).load_prompt('translate'); "
                "assert prompt.prompt == 'Translate the following text.\\n'"
            ),
            cwd=PACKAGE_ROOT,
        )
    finally:
        shutil.rmtree(temp_root, ignore_errors=True)


if __name__ == "__main__":
    main()
