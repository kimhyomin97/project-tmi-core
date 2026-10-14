#!/bin/bash
# .claude/hooks/session-base.sh — SessionStart hook (settings.json matcher: "startup" — resume·compact 에서는 다시 쓰지 않음)
# 사람의 변경(plan_approved 등)을 먼저 받고, 세션 시작 커밋 + session_id 를 기록한다.
[ "$CLAUDE_CODE_REMOTE" = "true" ] || exit 0
cd "$CLAUDE_PROJECT_DIR" || exit 0
sid=$(jq -r '.session_id // "unknown"')
git pull --ff-only --quiet 2>/dev/null || echo "session-base: git pull 실패 — 로컬 기준으로 진행" >&2
gd=$(git rev-parse --git-dir)                                  # worktree 에서는 .git 이 파일이므로
printf '%s %s\n' "$(git rev-parse HEAD)" "$sid" > "$gd/claude-session-base"
printf '\nsession-base: %s\n' "$(cat "$gd/claude-session-base")" >> docs/ai/progress.md
