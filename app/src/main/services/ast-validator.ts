import { spawn } from "node:child_process";

const ALLOWED_IMPORTS = [
  "pandas", "matplotlib", "matplotlib.pyplot", "json", "csv",
  "pathlib", "datetime", "math", "statistics", "collections",
  "functools", "itertools", "re", "typing", "data_helpers",
  "os.path",  // os.path is safe (just path manipulation, no exec)
];

// Python AST validation script — spawned as `python -c <script>`
// Reads code from stdin, prints JSON result to stdout
function buildValidatorScript(allowedImports: string[]): string {
  const allowedJson = JSON.stringify(allowedImports);
  return `
import ast, sys, json
code = sys.stdin.read()
try:
    tree = ast.parse(code)
except SyntaxError as e:
    print(json.dumps({"ok": False, "errors": [f"SyntaxError: {e}"]}))
    sys.exit(0)

allowed = set(${allowedJson})
errors = []

class Checker(ast.NodeVisitor):
    def visit_Import(self, node):
        for alias in node.names:
            base = alias.name.split(".")[0]
            if base not in allowed and alias.name not in allowed:
                errors.append(f"Blocked import: {alias.name}")
        self.generic_visit(node)
    def visit_ImportFrom(self, node):
        module = node.module or ""
        base = module.split(".")[0]
        if base not in allowed and module not in allowed:
            errors.append(f"Blocked import from: {module}")
        self.generic_visit(node)
    def visit_Call(self, node):
        if isinstance(node.func, ast.Name):
            if node.func.id in ("eval", "exec", "__import__", "compile"):
                errors.append(f"Blocked call: {node.func.id}()")
            elif node.func.id == "open":
                # Block open() with absolute paths only
                if node.args and isinstance(node.args[0], ast.Constant):
                    p = str(node.args[0].value)
                    if p.startswith("/") or (len(p) > 2 and p[1] == ":"):
                        errors.append(f"Blocked open() with absolute path: {p}")
        self.generic_visit(node)

Checker().visit(tree)
print(json.dumps({"ok": len(errors) == 0, "errors": errors}))
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
  const script = buildValidatorScript(ALLOWED_IMPORTS);

  return new Promise((resolve, reject) => {
    const child = spawn(pythonBin, ["-c", script], {
      stdio: ["pipe", "pipe", "pipe"],
      shell: process.platform === "win32",
    });

    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (b: Buffer) => (stdout += b.toString()));
    child.stderr.on("data", (b: Buffer) => (stderr += b.toString()));

    child.stdin.write(code, "utf-8");
    child.stdin.end();

    child.on("exit", () => {
      try {
        resolve(JSON.parse(stdout) as ValidationResult);
      } catch {
        resolve({
          ok: false,
          errors: [`Validator error: ${stderr || stdout || "unknown"}`],
        });
      }
    });

    child.on("error", (err) => {
      reject(new Error(`Failed to spawn Python (${pythonBin}): ${err.message}`));
    });
  });
}
