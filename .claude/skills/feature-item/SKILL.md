---
name: feature-item
description: docs/ai/feature_list.json 의 작업 항목 하나를 처음부터 끝까지 수행할 때 사용 (항목 선택 → 계획 → 실패 테스트 → 구현 → verify → 리뷰 → 기록). 사용자가 "다음 작업 진행", "TEST-1 해줘", "feature_list 항목 진행" 같이 요청할 때.
---

# feature_list 항목 1개 수행

상세 규칙의 원본: `docs/ai/proposals/ai-workflow.md` §4. 이 skill 은 그 순서를 따른다. 충돌하면 원본이 이긴다.

## 0. 시작

1. `git log --oneline -20`, `docs/ai/progress.md` 마지막 블록, `docs/ai/feature_list.json` 을 읽는다.
2. `bash scripts/init.sh` — 실패하면 새 항목을 시작하지 않고 원인부터 고친다.
3. 항목 선택 (사용자가 id 를 지정했으면 그 항목). 제외: `passes: true`, `blocked` 가 null 아님, `awaiting_human: true`, `depends_on` 미충족, `requires` 미충족(예: Docker 없음).

## 1. 계획

- `human_gate: "plan"` 이고 `plan_approved` 가 비어 있으면: `docs/ai/plans/active/<id>.md` 에 계획만 쓰고 멈춘다 (사람 승인 대기).
- 그 밖: 작은 항목은 계획 파일 없이 진행해도 된다. 여러 파일·여러 단계면 `docs/ai/plans/active/<id>.md` 를 만든다 (목적 · 맥락 · 작업 순서 · 진행 · 발견·결정 로그 · 회고).

## 2. 구현

1. 브랜치: `main` 에서 `ai/<id>` (계획만 쓰는 세션은 브랜치 없음).
2. **실패하는 테스트를 먼저** 커밋한다. 아직 고치지 않을 결함은 `@Disabled("<ID> <사유>")`.
3. 구현 커밋. 범위: 모듈 1개, `src/main` 변경 5파일 이하. 넘으면 멈추고 항목 분할을 제안한다.
4. 항목의 `verify` 명령을 **전부, 적힌 그대로** 실행한다.

## 3. 리뷰

- 독립 리뷰어(서브에이전트 `spring-reviewer`)에게 diff 와 항목의 `acceptance` 를 주고 리뷰를 받는다. 지적을 반영한 뒤 verify 를 다시 실행한다.

## 4. 기록과 종료

1. `docs/ai/progress.md` 에 블록 추가: 한 일, 실행한 verify 명령과 결과 요약, 그리고 줄 전체가 정확히 `verify: <id> OK`.
2. `feature_list.json` 에서 **`passes`·`blocked`·`awaiting_human` 만** 바꾼다. 사람 판정 항목(`verify: ["human"]`)은 `awaiting_human: true` 까지만.
3. 실패로 끝나면 `blocked: "<원인 한 줄>"` + `docs/ai/blockers.md` 에 상세.
4. 계획 파일이 있으면 회고 3줄을 붙여 `plans/completed/` 로 옮긴다.
5. 핵심 개념이 쓰였으면 skill `study-note`.
6. 커밋 → push → PR (본문에 verify 명령과 결과).
