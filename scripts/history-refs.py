#!/usr/bin/env python3
"""Keeps the rule documents free of history (ADR-0044).

Usage:
  scripts/history-refs.py                       check the rule documents of this repository
  scripts/history-refs.py --links <folder>      check the relative links of every Markdown file in a folder
  scripts/history-refs.py --mentions <folder> ADR-NNNN ...
                                                list the rule documents of a folder that name one of these ADRs

Rule documents are every Markdown file outside the places kept for history, plus the rule line ("Việc AI phải tuân
thủ") of every ADR that is not superseded. Places kept for history: docs/lessons.md, docs/runs/, docs/metrics/,
docs/inbox/, docs/test-sessions/ (a README.md there is a rule document), docs/features/ (tickets, plans, feedback and
requests are history; specs and designs keep their own history section), the rest of each ADR, and generated pages.

The check fails when a rule document names a lesson, ticket or run (`L-` or `T-` and three digits, `RUN-` and eight
digits), or names a superseded ADR. Files and ADRs not cleaned yet are listed in scripts/tests/history-refs-allow.txt
(one path or `ADR-NNNN` per line); the list only shrinks, so an entry that is already clean is an error too.
HISTORY_REFS_ROOT overrides the repository root (tests). Messages are in Vietnamese for the team.
"""

import os
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(os.environ.get("HISTORY_REFS_ROOT", Path(__file__).resolve().parent.parent))
ALLOW = "scripts/tests/history-refs-allow.txt"
HISTORY_REF = re.compile(r"\b(L-\d{3}|T-\d{3}|RUN-\d{8})\b")
ADR_REF = re.compile(r"(?:\bADR-|\badr/)(\d{4})\b")
RULE = re.compile(r"^- \*\*Việc AI phải tuân thủ:\*\*\s*(.*?)(?=^- \*\*|^#|\Z)", re.S | re.M)
HISTORY_DIRS = ("docs/runs/", "docs/metrics/", "docs/inbox/", "docs/test-sessions/")
GENERATED = {"docs/foundation.md", "docs/STATUS.md", "docs/remote-config-keys.md"}
SKIPPED_DIRS = {".git", "build", ".gradle", "node_modules", ".idea"}
FENCE = re.compile(r"^(\s*)(```|~~~).*?^\1\2[^\n]*$", re.S | re.M)
CODE_SPAN = re.compile(r"(`+)(?!`).*?(?<!`)\1(?!`)", re.S)
INLINE_LINK = re.compile(r"\]\(\s*<?([^)\s>]+)>?(?:\s+\"[^\"]*\")?\s*\)")
REFERENCE_LINK = re.compile(r"^\s{0,3}\[(?!\^)[^\]]+\]:\s*<?(\S+?)>?(?:\s|$)", re.M)


def read(path):
    """A file's text; a byte that is not UTF-8 is replaced, so one odd file does not stop the check."""
    return path.read_text(encoding="utf-8", errors="replace")


def markdown_files(root):
    """Every Markdown file of a folder, relative and sorted: the tracked ones when it is a git work tree."""
    result = subprocess.run(["git", "-C", str(root), "ls-files", "-z", "--", "*.md"], capture_output=True)
    if result.returncode == 0 and (root / ".git").exists():
        # An unmerged path has one index entry per stage.
        return sorted({p for p in result.stdout.decode().split("\0") if p and (root / p).is_file()})
    found = []
    for folder, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in SKIPPED_DIRS]
        for name in files:
            if name.endswith(".md"):
                found.append(Path(folder, name).relative_to(root).as_posix())
    return sorted(found)


def is_rule_document(path):
    if path.startswith("docs/adr/") or path in GENERATED or path == "docs/lessons.md":
        return False
    if path.startswith("docs/features/"):
        # Tickets, plans, feedback and requests are history; specs and designs keep their own history section.
        parts = path.split("/")
        if parts[-1] == "spec.md" or {"tickets", "plans", "feedback", "requests", "design"} & set(parts[3:-1]):
            return False
    if path.startswith(HISTORY_DIRS):
        return path.endswith("/README.md")
    return True


def adrs(root):
    """(id, status, rule line) of every ADR of a folder."""
    result = []
    for path in sorted((root / "docs/adr").glob("[0-9][0-9][0-9][0-9]-*.md")):
        text = read(path)
        status = re.search(r"^status:\s*(\S+)", text, re.M)
        rule = RULE.search(text)
        result.append((f"ADR-{path.name[:4]}", status.group(1) if status else "", rule.group(1) if rule else ""))
    return result


def rule_texts(root):
    """(name, text) of every rule document of a folder: a path, or an ADR id for an ADR rule line."""
    texts = [(p, read(root / p)) for p in markdown_files(root) if is_rule_document(p)]
    texts += [(adr_id, rule) for adr_id, status, rule in adrs(root) if status != "superseded" and rule]
    return texts


def problems(text, superseded):
    found = sorted(set(HISTORY_REF.findall(text)))
    found += sorted({f"ADR-{n}" for n in ADR_REF.findall(text)} & superseded)
    return found


def check(root):
    superseded = {adr_id for adr_id, status, _ in adrs(root) if status == "superseded"}
    allow_path = root / ALLOW
    allowed = []
    if allow_path.is_file():
        for line in allow_path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                allowed.append(line)
    names = set()
    errors = []
    for name, text in rule_texts(root):
        names.add(name)
        found = problems(text, superseded)
        if found and name not in allowed:
            errors.append(f"   {name}: {', '.join(found)}")
        elif not found and name in allowed:
            errors.append(f"   {name}: đã sạch, xóa dòng này khỏi {ALLOW}")
    for name in allowed:
        if name not in names:
            errors.append(f"   {name}: không còn là tài liệu quy tắc, xóa dòng này khỏi {ALLOW}")
    if errors:
        print("LỖI: tài liệu quy tắc ghi mã lịch sử hay dẫn ADR đã bị thay (ADR-0044):")
        print("\n".join(errors))
        print("Viết quy tắc ở thì hiện tại, lý do thành một vế câu; mã bài học ghi trong mô tả PR.")
        return 1
    print(f"Tài liệu quy tắc sạch, còn {len(allowed)} mục chưa dọn trong {ALLOW}.")
    return 0


def link_targets(text):
    text = CODE_SPAN.sub("", FENCE.sub("", text))
    return INLINE_LINK.findall(text) + REFERENCE_LINK.findall(text)


def check_links(root):
    broken = []
    for path in markdown_files(root):
        source = root / path
        for target in link_targets(read(source)):
            if re.match(r"^[a-z][a-z0-9+.-]*:", target, re.I) or target.startswith(("#", "/")):
                continue
            file_part = unquote(target.split("#", 1)[0].split("?", 1)[0])
            if not file_part:
                continue
            resolved = Path(os.path.normpath(source.parent / file_part))
            try:
                resolved.relative_to(root)
            except ValueError:
                continue  # outside the tree, like a GitHub link to another branch
            if not resolved.exists():
                broken.append(f"   {path}: {target}")
    if broken:
        print("LỖI: link tương đối trỏ tới file không có:")
        print("\n".join(broken))
        return 1
    print("Không có link tương đối gãy.")
    return 0


def mentions(root, ids):
    wanted = set(ids)
    hits = []
    for name, text in rule_texts(root):
        found = sorted({f"ADR-{n}" for n in ADR_REF.findall(text)} & wanted)
        if found:
            hits.append(f"   {name}: {', '.join(found)}")
    print("\n".join(hits))
    return 0


def main(argv):
    if not argv:
        return check(ROOT)
    if argv[0] == "--links" and len(argv) == 2:
        return check_links(Path(argv[1]).resolve())
    if argv[0] == "--mentions" and len(argv) >= 2:
        return mentions(Path(argv[1]).resolve(), argv[2:])
    print("Cách dùng: scripts/history-refs.py [--links <thư mục> | --mentions <thư mục> ADR-NNNN ...]", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
