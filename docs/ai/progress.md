# 진행 기록 (progress)

세션마다 아래에 블록을 **추가**한다 (지우거나 고치지 않는다). 형식은 `docs/ai/proposals/ai-workflow.md` §4.4.

- `session-base: <sha> <session_id>` 줄은 SessionStart hook 이 쓴다. 직접 쓰지 않는다.
- 검증 기록은 줄 전체가 정확히 `verify: <ID> OK` 여야 한다 (Stop hook 이 문자 그대로 비교).
- 블록 끝에 "다음 세션이 알아야 할 것" 3줄 이내.

---

## 2026-10-14 구조 적용 (context-structure)

- 이 파일과 `feature_list.json` 을 만들었다. 루프 hook(`session-base.sh`·`check-feature.sh`)은 스크립트만 있고 `.claude/settings.json` 에 아직 등록되지 않았다 — 등록 전에는 `session-base` 줄이 자동으로 생기지 않는다.
- 다음 세션이 알아야 할 것:
  - 첫 항목은 `TEST-1` (DB 없이 도는 테스트 3종).
  - 전체 `build` 는 PostgreSQL 이 필요하다. 작업 트리에 미커밋 Toss WIP 가 있으면 빌드 전에 `git stash push -- src/main .gitattributes`.
  - `.claude/` 설정 변경은 사람이 한다 (`docs/ai/plans/active/context-structure.md` "사람이 할 일").
