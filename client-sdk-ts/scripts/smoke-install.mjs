import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { execFileSync } from "node:child_process";

const packageRoot = process.cwd();
const tempRoot = await mkdtemp(path.join(tmpdir(), "promptlm-ts-smoke-"));
let tarball;

try {
  const packed = JSON.parse(
    execFileSync("npm", ["pack", "--json"], {
      cwd: packageRoot,
      encoding: "utf8"
    })
  );
  tarball = path.join(packageRoot, packed[0].filename);

  execFileSync("npm", ["init", "-y"], { cwd: tempRoot, stdio: "ignore" });
  execFileSync("npm", ["install", tarball], { cwd: tempRoot, stdio: "ignore" });

  const scriptPath = path.join(tempRoot, "check.mjs");
  await writeFile(
    scriptPath,
    [
      "import { JsonPromptLoader, PackageResourcePromptSource } from '@promptlm/client-sdk-ts';",
      "const loader = new JsonPromptLoader(new PackageResourcePromptSource());",
      "const prompt = await loader.loadPrompt('translate');",
      "if (prompt.prompt !== 'Translate the following text.\\n') {",
      "  throw new Error(`Unexpected prompt payload: ${prompt.prompt}`);",
      "}"
    ].join("\n"),
    "utf8"
  );

  execFileSync("node", [scriptPath], { cwd: tempRoot, stdio: "inherit" });
} finally {
  if (tarball) {
    await rm(tarball, { force: true });
  }
  await rm(tempRoot, { recursive: true, force: true });
}
