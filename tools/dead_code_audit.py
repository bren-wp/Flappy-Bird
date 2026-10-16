#!/usr/bin/env python3
"""Conservative dead-code audit for the real SkyPeep Android sources.

Unreferenced Kotlin imports / common private declaration forms and unused Java private
methods are certain enough to block CI. Public or reflection-owned APIs are
reported but not deleted automatically. Run with Python 3; no dependencies.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app" / "src" / "com" / "brendigo" / "skypeep"
TOOLS = ROOT / "tools"
report = ["SkyPeep dead-code audit (static conservative pass)",
          "Sources: Kotlin Android implementation and Java asset tooling",
          "External APIs, Android lifecycle overrides and dynamic asset keys are retained."]
problems = []
checked = 0
for file in sorted(list(SOURCE.glob("*.kt")) + list(TOOLS.glob("*.java"))):
    checked += 1
    content = file.read_text(encoding="utf-8")
    code = re.sub(r"(?m)^\s*import\s+[^\n]+\n", "", content)
    code = re.sub(r"(?s)/\*.*?\*/", "", code)
    code = re.sub(r"(?m)//[^\n]*", "", code)
    if file.suffix == ".kt":
        for imp in re.findall(r"(?m)^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$", content):
            symbol = imp[1] or imp[0].split(".")[-1]
            if not re.search(r"\b" + re.escape(symbol) + r"\b", code):
                problems.append(f"{file.relative_to(ROOT)}: unused import {imp[0]}")
        declarations = re.findall(
            r"(?m)^\s*private\s+(?:(?:const|lateinit|suspend|data|sealed|enum|inline|tailrec|inner|open|abstract)\s+)*(?:fun|val|var|class|object|interface|typealias)\s+(\w+)\b", code)
        for symbol in declarations:
            if len(re.findall(r"\b" + re.escape(symbol) + r"\b", code)) == 1:
                problems.append(f"{file.relative_to(ROOT)}: unused private {symbol}")
    else:
        for symbol in re.findall(
            r"\bprivate\s+static\s+(?:void|int|double|boolean|Color|BufferedImage)\s+(\w+)\s*\(", code):
            if len(re.findall(r"\b" + re.escape(symbol) + r"\b", code)) == 1:
                problems.append(f"{file.relative_to(ROOT)}: unused Java helper {symbol}")

report.append(f"Inspected {checked} Kotlin/Java production source files")
report.append(f"High-confidence unused symbols: {len(problems)}")
report.extend(" - " + x for x in problems)
report.append("Dynamic reachability, native GPU allocation and packaged assets are checked by Gradle / emulator CI separately.")
destination = ROOT / "smoke-results" / "dead-code-audit.txt"
destination.parent.mkdir(exist_ok=True)
destination.write_text("\n".join(report) + "\n", encoding="utf-8")
print("\n".join(report))
sys.exit(1 if problems else 0)
