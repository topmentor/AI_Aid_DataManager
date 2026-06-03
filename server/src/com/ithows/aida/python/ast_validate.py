# -*- coding: utf-8 -*-
# AIDA analyze.py 정적 검증기. 인자: 검증할 .py 경로.
# 출력: JSON {"ok": bool, "errors": [..]}, 위반 시 종료코드 2.
import ast, sys, json

ALLOWED = {
    "pandas", "numpy", "json", "csv",
    "pathlib", "datetime", "math", "statistics", "collections",
    "functools", "itertools", "re", "typing", "data_helpers",
    "sqlite3", "os.path", "matplotlib",
}

if len(sys.argv) < 2:
    print(json.dumps({"ok": False, "errors": ["No file argument"]}))
    sys.exit(2)

path = sys.argv[1]
try:
    with open(path, encoding="utf-8") as f:
        code = f.read()
except Exception as e:
    print(json.dumps({"ok": False, "errors": ["Read error: %s" % e]}))
    sys.exit(2)

try:
    tree = ast.parse(code)
except Exception as e:
    print(json.dumps({"ok": False, "errors": ["ParseError: %s" % e]}))
    sys.exit(2)

errors = []

class Checker(ast.NodeVisitor):
    def visit_Import(self, node):
        for alias in node.names:
            base = alias.name.split(".")[0]
            if base not in ALLOWED and alias.name not in ALLOWED:
                errors.append("Blocked import: %s" % alias.name)
        self.generic_visit(node)

    def visit_ImportFrom(self, node):
        module = node.module or ""
        base = module.split(".")[0]
        if base not in ALLOWED and module not in ALLOWED:
            errors.append("Blocked import from: %s" % module)
        self.generic_visit(node)

    def visit_Call(self, node):
        if isinstance(node.func, ast.Name) and node.func.id in ("eval", "exec", "__import__", "compile"):
            errors.append("Blocked call: %s()" % node.func.id)
        self.generic_visit(node)

try:
    Checker().visit(tree)
except Exception as e:
    errors.append("Checker error: %s" % e)

print(json.dumps({"ok": len(errors) == 0, "errors": errors}))
sys.exit(0 if len(errors) == 0 else 2)
