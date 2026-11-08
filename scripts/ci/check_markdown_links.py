"""Check relative Markdown links in active repository documentation."""
import pathlib
import re
import sys
from urllib.parse import unquote

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
errors = []
pattern = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
for document in sorted(root.rglob("*.md")):
    relative = document.relative_to(root)
    if "archive" in relative.parts or "fork-base" in relative.parts:
        continue
    text = document.read_text(encoding="utf-8", errors="replace")
    for target in pattern.findall(text):
        target = target.strip().split()[0].strip("<>")
        if not target or target.startswith(("http://", "https://", "mailto:", "#")):
            continue
        target = unquote(target.split("#", 1)[0].split("?", 1)[0])
        path = (document.parent / target).resolve()
        try:
            path.relative_to(root)
        except ValueError:
            errors.append(f"{relative}: link escapes repository: {target}")
            continue
        if not path.exists():
            errors.append(f"{relative}: missing link target: {target}")

for error in errors:
    print("ERROR:", error)
print(f"check_markdown_links: {len(errors)} problem(s)")
sys.exit(1 if errors else 0)
