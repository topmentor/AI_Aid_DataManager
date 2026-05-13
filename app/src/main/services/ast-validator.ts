import { spawn } from "node:child_process";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";

const ALLOWED_IMPORTS = [
  "pandas", "numpy", "json", "csv",
  "pathlib", "datetime", "math", "statistics", "collections",
  "functools", "itertools", "re", "typing", "data_helpers",
  "sqlite3", "os.path",
];

// Python AST validation script — writes code to a temp file to avoid stdin encoding issues
function buildValidatorScript(allowedImports: string[], codeFile: string): string {
  const allowedJson = JSON.stringify(allowedImports);
  const codeFileEscaped = codeFile.replace(/\\/g, "\\\\");
  return `# -*- coding: utf-8 -*-
import ast, sys, json

try:
    with open(${JSON.stringify(codeFileEscaped)}, encoding="utf-8") as f:
        code = f.read()
except Exception as e:
    print(json.dumps({"ok": False, "errors": [f"Read error: {e}"]}))
    sys.exit(0)

try:
    tree = ast.parse(code)
except SyntaxError as e:
    print(json.dumps({"ok": False, "errors": [f"SyntaxError: {e}"]}))
    sys.exit(0)
except Exception as e:
    print(json.dumps({"ok": False, "errors": [f"ParseError: {e}"]}))
    sys.exit(0)

_allowed = set(${allowedJson})
_errors = []

class _Checker(ast.NodeVisitor):
    def visit_Import(self, node):
        for alias in node.names:
            base = alias.name.split(".")[0]
            if base not in _allowed and alias.name not in _allowed:
                _errors.append(f"Blocked import: {alias.name}")
        self.generic_visit(node)
    def visit_ImportFrom(self, node):
        module = node.module or ""
        base = module.split(".")[0]
        if base not in _allowed and module not in _allowed:
            _errors.append(f"Blocked import from: {module}")
        self.generic_visit(node)
    def visit_Call(self, node):
        if isinstance(node.func, ast.Name):
            if node.func.id in ("eval", "exec", "__import__", "compile"):
                _errors.append(f"Blocked call: {node.func.id}()")
        self.generic_visit(node)

try:
    _Checker().visit(tree)
except Exception as e:
    _errors.append(f"Checker error: {e}")

print(json.dumps({"ok": len(_errors) == 0, "errors": _errors}))
`;
}

export interface ValidationResult {
  ok: boolean;
  errors: string[];
}

export async function validatePython(
  code: string,
  pythonBin = "python"
): Promise<ValidationResult> {
  const ts = Date.now();
  const tmpCode   = path.join(os.tmpdir(), `aidclaude_code_${ts}.py`);
  const tmpScript = path.join(os.tmpdir(), `aidclaude_validator_${ts}.py`);

  try {
    await fs.writeFile(tmpCode, code, "utf-8");
    const script = buildValidatorScript(ALLOWED_IMPORTS, tmpCode);
    await fs.writeFile(tmpScript, script, "utf-8");

    return await new Promise((resolve, reject) => {
      const child = spawn(pythonBin, [tmpScript], {
        stdio: ["ignore", "pipe", "pipe"],
        env: { ...process.env, PYTHONUTF8: "1", PYTHONIOENCODING: "utf-8" },
      });

      let stdout = "";
      let stderr = "";

      child.stdout.on("data", (b: Buffer) => (stdout += b.toString("utf-8")));
      child.stderr.on("data", (b: Buffer) => (stderr += b.toString("utf-8")));

      child.on("exit", () => {
        try {
          resolve(JSON.parse(stdout) as ValidationResult);
        } catch {
          resolve({
            ok: false,
            errors: [`Validator error: ${(stderr || stdout).slice(0, 500)}`],
          });
        }
      });

      child.on("error", (err) => {
        reject(new Error(`Failed to spawn Python (${pythonBin}): ${err.message}`));
      });
    });
  } finally {
    await Promise.all([
      fs.unlink(tmpCode).catch(() => undefined),
      fs.unlink(tmpScript).catch(() => undefined),
    ]);
  }
}
