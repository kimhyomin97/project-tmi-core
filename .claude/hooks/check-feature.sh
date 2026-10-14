#!/bin/bash
# .claude/hooks/check-feature.sh — Stop hook
# 이번 세션에서 passes=true 가 된 항목마다 progress 에 그 세션의 verify 기록이 있어야 하고,
# 사람 전용 필드는 세션 시작 시점(base)에서 바뀌면 안 된다.
[ "$CLAUDE_CODE_REMOTE" = "true" ] || exit 0
cd "$CLAUDE_PROJECT_DIR" || exit 0
f=docs/ai/feature_list.json; p=docs/ai/progress.md
block() { echo "$1" >&2; exit 2; }
# stop_hook_active 로 빠져나가지 않는다 — 막는 조건은 모두 에이전트가 해소할 수 있고,
# 연속 차단은 Claude Code 내장 상한(연속 8회, 도구 호출 시 초기화)에 맡긴다 (ai-workflow.md §4.6).

gd=$(git rev-parse --git-dir)
read -r base sid < "$gd/claude-session-base" 2>/dev/null
[ -n "$base" ] || block "세션 base 가 없음 — SessionStart hook(.claude/hooks/session-base.sh) 확인."
old=$(git show "$base:$f" 2>/dev/null)
if [ ! -f "$f" ]; then
  [ -z "$old" ] && exit 0                                      # 루프 미사용 저장소
  block "feature_list.json 이 삭제됨 — 복구할 것."
fi
jq -e . "$f" >/dev/null 2>&1 || block "feature_list.json 이 올바른 JSON 이 아님."
[ -n "$old" ] || exit 0                                        # 이번 세션에 처음 등록(사람의 H0 커밋)
all='(.items + ([.pending_decision // {} | .[]] | add // []))'
jq -e "[$all[] | (.passes | type == \"boolean\")] | all" "$f" >/dev/null || block "passes 는 true/false(boolean)만 허용."
jq -e "[$all[] | select(.awaiting_human == true) | .verify == [\"human\"]] | all" "$f" >/dev/null \
  || block "awaiting_human=true 는 verify 가 [\"human\"] 인 항목에만."

# 1) 사람 전용 필드 불변 — 에이전트가 바꿀 수 있는 것은 passes·blocked·awaiting_human 뿐
human="[$all[] | del(.passes, .blocked, .awaiting_human)] | sort_by(.id)"
[ "$(jq -S "$human" <<<"$old")" = "$(jq -S "$human" "$f")" ] \
  || block "passes·blocked·awaiting_human 외의 필드(id 목록 포함)가 세션 중 바뀜 — 되돌릴 것."

# 2) 새로 passes=true 가 된 항목
ids() { jq -r "[$all[] | select(.passes == true) | .id] | .[]" | sort; }
newly=$(comm -13 <(ids <<<"$old") <(ids < "$f"))
# 이번 세션 기록만: progress 의 "session-base: <base> <session_id>" 줄 이후
records=$(awk -v b="session-base: $base $sid" 'found{print} $0==b{found=1}' "$p" 2>/dev/null)
for id in $newly; do
  if jq -e --arg id "$id" "$all[] | select(.id==\$id) | .verify == [\"human\"]" "$f" >/dev/null; then
    block "$id 는 사람 판정 항목 — 에이전트는 awaiting_human=true 까지만 (§4.4)."
  fi
  grep -qx "verify: $id OK" <<<"$records" || block "$id: 이번 세션 progress 에 'verify: $id OK' 줄 없음."
done
exit 0
