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

## 2026-10-14 TEST-1 (DB 없이 도는 테스트 3종)

- 추가: `GradeTest`(경계값 7개), `SimilarityClientTest`(`@Disabled("FIX-F1 …")`), `SentenceControllerTest`(`@Disabled("FIX-F2 …")`, 서버 기본 로케일 ko 고정 + `Accept-Language: en`).
- `@Disabled` 를 지우고 현재 코드에서 1회 실행 (acceptance):
  - `SimilarityClientTest` → 실패 `IllegalStateException: Could not resolve parameter [0] in … SimilarityClient.score(…)` = F1 재현
  - `SentenceControllerTest` → 실패 `$.error.message expected:<Sentence not found: level=LEVEL_5, category=TECH> but was:<TMI.SENTENCE.NOT_FOUND>` = F2 재현
- 리뷰(spring-reviewer) 지적 반영: 기본 로케일이 en 인 환경에서는 FIX-F2 의 `fallback-to-system-locale=false` 누락을 못 잡았다 → 기본 로케일을 ko 로 고정. 확인(`@Disabled` 임시 제거, 환경변수로 설정 주입, `--rerun`): 미수정 실패 / basename 만 실패(한글 메시지) / basename + fallback=false 통과.
- verify 실행: `bash ./gradlew test --tests '*GradeTest'` + `skipped="0"` grep rc=0, `--tests '*SimilarityClientTest'` rc=0, `--tests '*SentenceControllerTest'` rc=0, `@Disabled` 사유 grep rc=0. ArchUnit `--tests 'com.tmi.core.architecture.*'` rc=0.
- 범위 메모: 커밋 8e12ee0 에 `.claude/agents/spring-reviewer.md` 가 함께 들어갔다. 사용자가 구조 적용 4단계 진행("단계별로 알아서 쭉 진행")을 지시했고 이 파일은 그 4단계 산출물이다. 이미 push 돼 커밋을 나누지 않았다.
verify: TEST-1 OK
- 다음 세션이 알아야 할 것:
  - 다음 항목은 `FIX-F1` (`@Disabled` 를 지우는 것까지가 verify).
  - FIX-F2 는 basename 과 `fallback-to-system-locale=false` 두 줄이 모두 있어야 `SentenceControllerTest` 가 통과한다.
