#!/bin/bash
# 클라우드 세션에서만 동작 (로컬 세션에는 영향 없음)
[ "$CLAUDE_CODE_REMOTE" = "true" ] || exit 0

# 무한 루프 방지: 15회 이상 막았으면 종료 허용
COUNTER=/tmp/stop-hook-count
n=$(( $(cat "$COUNTER" 2>/dev/null || echo 0) + 1 )); echo "$n" > "$COUNTER"
[ "$n" -gt 15 ] && exit 0

f="docs/ai/checklist.json"
[ -f "$f" ] || { echo "checklist.json이 아직 없음. docs/ai/TASK.md를 다시 읽고 1단계부터 진행할 것." >&2; exit 2; }

pending=$(jq '[.items[] | select(.done != true)] | length' "$f")
reviews=$(ls docs/ai/reviews/*.md 2>/dev/null | wc -l)

if [ "$pending" -gt 0 ] || [ "$reviews" -lt 10 ]; then
  echo "미완료 항목 ${pending}개, 리뷰 파일 ${reviews}/10개. docs/ai/TASK.md를 다시 확인하고 남은 작업과 리뷰 라운드를 계속할 것." >&2
  exit 2
fi
exit 0