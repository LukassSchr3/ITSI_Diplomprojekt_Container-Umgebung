"""Parse a JaCoCo XML coverage report and write a Markdown summary to an output file.

Usage:
    python3 parse_jacoco_coverage.py <xml_path> <title> <output_md_path>
"""

import re
import sys
import xml.etree.ElementTree as ET

xml_path, title, output_path = sys.argv[1], sys.argv[2], sys.argv[3]

with open(xml_path, "r") as f:
    content = f.read()

content = re.sub(r"<!DOCTYPE[^>]*>", "", content)
root = ET.fromstring(content)

counters = {}
for counter in root.findall("counter"):
    ctype = counter.get("type")
    missed = int(counter.get("missed", 0))
    covered = int(counter.get("covered", 0))
    total = missed + covered
    pct = round(covered / total * 100, 1) if total > 0 else 0.0
    counters[ctype] = (covered, total, pct)

lines = [f"## 📊 {title} – Coverage Report\n"]
lines.append("| Metric | Covered | Total | Coverage |")
lines.append("|--------|--------:|------:|---------:|")
for ctype in ["LINE", "BRANCH", "METHOD", "INSTRUCTION"]:
    if ctype in counters:
        covered, total, pct = counters[ctype]
        lines.append(f"| {ctype.capitalize()} | {covered} | {total} | {pct}% |")

with open(output_path, "w") as f:
    f.write("\n".join(lines) + "\n")
