#!/usr/bin/env python3
"""
PreCompact hook — backup transcript before every compact.
Reads JSON payload from stdin, copies the transcript JSONL file to
.claude/backups/ with a timestamp + compact_type suffix.

Exits 0 always so it never blocks the compact operation.
"""
import json
import shutil
import sys
from datetime import datetime
from pathlib import Path


def main() -> None:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        # Malformed payload — don't block compact
        sys.exit(0)

    transcript_path = payload.get("transcript_path", "")
    compact_type    = payload.get("compact_type", "unknown")   # "auto" | "manual"

    src = Path(transcript_path)
    if not src.exists():
        print(f"[backup-transcript] source not found: {src}", file=sys.stderr)
        sys.exit(0)

    # Resolve .claude/backups/ relative to the project root (CWD when hook runs)
    backup_dir = Path(".claude") / "backups"
    backup_dir.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    dest = backup_dir / f"transcript_{timestamp}_{compact_type}.jsonl"

    shutil.copy2(src, dest)
    size_kb = dest.stat().st_size // 1024
    print(f"[backup-transcript] {compact_type} → {dest.name} ({size_kb} KB)", file=sys.stderr)


if __name__ == "__main__":
    main()
