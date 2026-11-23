#!/usr/bin/env python3
"""Tells a subagent to hand over and stop once its context grows past a limit.

Every tool call of an agent sends its whole context to the model again, so an agent that keeps working past a
few hundred thousand tokens costs far more per step than a fresh agent with a short handover. This is a
PostToolUse hook of Claude Code (.claude/settings.json). It reads the hook input on stdin; for a subagent (the
input has agent_id) it finds the subagent's transcript next to the main transcript
(<session folder>/subagents/agent-<agent_id>.jsonl), reads the context size of its last model call (input,
cache write and cache read tokens) and, at or above AGENT_CONTEXT_LIMIT tokens (200000), prints a PostToolUse
additionalContext that tells the agent to finish the current step, commit if its brief allows, and end with a
report that has a handover section (CLAUDE.md, Giao việc cho agent con). The main session is never guarded.

Usage: scripts/agent-context-guard.py < hook-input.json
Always exits 0 and prints nothing when it cannot tell (no agent_id, no transcript, no usage yet), so a broken
transcript never blocks a tool call.
"""
import json
import os
import sys

DEFAULT_LIMIT = 200_000
TAIL_CHUNK = 256 * 1024


def last_context_tokens(path):
    """Context size of the last model call in a transcript, read from the end of the file."""
    try:
        with open(path, "rb") as f:
            f.seek(0, os.SEEK_END)
            end = f.tell()
            pos = end
            carry = b""
            while pos > 0:
                start = max(0, pos - TAIL_CHUNK)
                f.seek(start)
                block = f.read(pos - start) + carry
                lines = block.split(b"\n")
                # The first piece may be a cut line unless the block starts the file.
                carry = lines.pop(0) if start > 0 else b""
                for raw in reversed(lines):
                    tokens = usage_tokens(raw)
                    if tokens is not None:
                        return tokens
                pos = start
            return usage_tokens(carry)
    except OSError:
        return None


def usage_tokens(raw):
    if b'"usage"' not in raw:
        return None
    try:
        entry = json.loads(raw)
    except ValueError:
        return None
    message = entry.get("message")
    if entry.get("type") != "assistant" or not isinstance(message, dict):
        return None
    usage = message.get("usage")
    if not isinstance(usage, dict):
        return None
    return sum(int(usage.get(key) or 0)
               for key in ("input_tokens", "cache_creation_input_tokens", "cache_read_input_tokens"))


def main():
    try:
        hook = json.load(sys.stdin)
    except ValueError:
        return 0
    agent_id = hook.get("agent_id")
    transcript = hook.get("transcript_path")
    session = hook.get("session_id")
    if not agent_id or not transcript or not session:
        return 0
    try:
        limit = int(os.environ.get("AGENT_CONTEXT_LIMIT", DEFAULT_LIMIT))
    except ValueError:
        limit = DEFAULT_LIMIT
    path = os.path.join(os.path.dirname(transcript), session, "subagents", f"agent-{agent_id}.jsonl")
    tokens = last_context_tokens(path)
    if tokens is None or tokens < limit:
        return 0
    message = (
        f"[Hook agent-context-guard của dự án] Ngữ cảnh của agent này đã khoảng {tokens // 1000}k token, từ giới hạn {limit // 1000}k trở lên "
        "(CLAUDE.md, mục Giao việc cho agent con). Làm xong bước đang dở, không bắt đầu việc mới. "
        "Lời giao cho phép commit thì commit phần đã xong, thông điệp ghi rõ còn dở. "
        "Rồi kết thúc ngay bằng báo cáo theo mẫu của lời giao, thêm mục Bàn giao: đã xong gì, còn lại gì, "
        "file đang dở, lệnh kiểm tra nên chạy tiếp, điều cần biết mà lời giao chưa nói."
    )
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": message}},
                     ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
