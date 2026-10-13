# ai-workflow 리뷰 — Round 1

> 대상 커밋: `b39d3e2` (claude/ai-dev-prep — ai-workflow.md·feature_list.example.json 은 `99aa1e3` 이후 변경 없음, CLAUDE.md 초안 포함)
> 대상: `docs/ai/ai-workflow.md`, `docs/ai/feature_list.example.json`
> 검증 방법:
> - 외부 출처 3건 + hooks 문서를 `curl` 로 받아(모두 HTTP 200) 본문 텍스트를 추출해 인용문과 대조: anthropic.com/engineering/effective-harnesses-for-long-running-agents, code.claude.com/docs/en/best-practices, /memory, /hooks
> - 전용 worktree(`scratchpad/wt-ai-workflow`, HEAD)에서 `bash ./gradlew test --tests '*ArchitectureTest*' --no-daemon` 실행 → 매칭 클래스·skip 수 확인 (끝난 뒤 worktree 제거)
> - §4.6 Stop hook 스크립트를 임시 git 저장소(scratchpad/hooksim)에 그대로 옮겨 5가지 시나리오로 실행
> - feature_list 항목의 depends_on/verify/acceptance/human_gate 를 ai-workflow §1·§2.2 규칙, decisions-needed, conventions-proposal, architecture-proposal, current-state §6, ArchUnit 테스트 주석과 대조

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | 외부 인용은 대부분 원문과 맞음. 하지만 §3 체크리스트의 "✓ 기계 검증" 표시 다수와 §4.6 의 "공식 문서 미확인"은 사실과 다름 |
| 구체성 | 3 | 스키마·스크립트·루틴이 실제 형식으로 제시돼 있음. 그러나 제안 hook 은 목적대로 동작하지 않고, verify 명령 다수는 아무것도 고치지 않아도 지금 통과함 |
| 일관성 | 2 | decisions-needed(§0, D4, D5, D8), conventions §3.4, architecture §4.3, current-state §6.1, 이 문서 자신의 §1.1·§2.2 규칙과 모순이 여러 건 있음 |
| 판단 가능성 | 3 | H0~H3 표는 읽기 쉬움. 그러나 H1 승인을 기록할 수단이 없어 사람이 "이대로 돌려도 되나"를 판단할 수 없음 |

## 치명 (승인 차단)

- [C1] §4.6 L223-234 `check-feature.sh` — **스크립트가 막겠다고 한 경우를 막지 못하고, 막지 말아야 할 경우를 막는다.** 근거: hooksim 실행 결과 —
  (A) passes=true 를 커밋했고 progress 에 기록이 없을 때 → exit 2 (의도대로).
  (B) progress 에 **다른 항목**의 `verify:` 한 줄만 있을 때 → exit 0. `grep -q "verify:"` 는 파일 어디든 문자열이 한 번만 있으면 참이 되므로, 첫 항목 이후로는 게이트가 영구히 꺼진다. L227 주석의 "그 id 의 verify 출력" 과도 맞지 않는다.
  (C) `jq -c` 등으로 compact JSON(`"passes":true`)을 쓰면 → exit 0. 패턴이 공백 1칸에 묶여 있다.
  (D) passes 를 true→false 로 되돌린 diff 에서 삭제 줄 `-      "passes": true,` 가 매칭됨(count=1). progress 에 기록이 없으면 정상 동작(되돌림)인데도 막는다. diff 의 문맥 줄(앞에 공백) 도 같은 방식으로 매칭된다.
  (E) passes 변경 커밋 뒤에 다른 커밋이 하나라도 있으면 → exit 0. `git diff HEAD~1` 은 "직전 커밋 대비 작업 트리" 만 본다. §4.4 루틴 7→8 단계(passes 커밋 후 progress 기록)를 따로 커밋하면 바로 이 경우다.
  그 밖에 `CLAUDE_CODE_REMOTE` 가드·`cd "$CLAUDE_PROJECT_DIR"` 가 없고, progress 파일이 없으면 grep 오류가 "기록 없음"으로 처리된다.
  — 요구 수정: (1) 기준 커밋을 "세션 시작 커밋"으로 정한다(SessionStart 때 `git rev-parse HEAD` 를 저장하거나, progress 에 남긴 base sha 사용). (2) 바뀐 항목은 텍스트 grep 이 아니라 `jq` 로 계산한다: `jq -r '.items[]|select(.passes)|.id'` 를 base 와 HEAD 에서 각각 뽑아 차집합을 낸다. (3) 각 id 마다 `grep -q "^verify: <id>"` 처럼 **id 에 묶인** 기록을 확인하거나, 더 확실하게는 hook 이 그 항목의 `verify` 명령을 직접 실행한다. (4) 상단에 `check-done.sh` 와 같은 REMOTE 가드와 `cd` 를 둔다.

- [C2] feature_list.example.json — **verify 명령 상당수가 HEAD 그대로(아무것도 고치지 않은 상태)에서 이미 통과한다.** "검증 없이 passing 표시"(§4.7 3행)를 막는다는 이 문서의 핵심 목적이 성립하지 않는다. 근거:
  - [실행] HEAD 에서 `bash ./gradlew test --tests '*ArchitectureTest*'` → `BUILD SUCCESSFUL`. Convention 3 tests/3 skipped, Module 3/3 skipped, Layer 2/0. 따라서 `FIX-F4`(L109), `ARCH-A1`(L125), `ARCH-B3`(L157) 는 verify 만으로는 아무것도 확인하지 못한다. `@Disabled` 를 지웠는지는 acceptance 문장으로만 남고 기계 판정이 없다.
  - `SIG-1`(L11) `bash ./gradlew test` 는 PostgreSQL 이 떠 있는 이 환경(localhost:15432)에서 지금도 통과한다(current-state §7.1 `BUILD SUCCESSFUL`). acceptance 의 "PostgreSQL 미기동 상태에서" 는 verify 가 재현하지 않는다.
  - `TEST-1`(L44) 은 추가한 테스트를 전부 `@Disabled` 로 두어도 통과한다.
  - `DOC-1`(L173) 은 `"verify": []` 라서 언제나 통과한다.
  - FIX-F4 acceptance 의 "외부 호출 중 활성 커넥션 0 (통합 테스트)" 를 돌리는 명령이 없다. §4.3 은 integrationTest 를 init.sh 에서도 뺐다.
  — 요구 수정: verify 를 "고치기 전엔 실패, 고친 뒤엔 통과" 하는 명령으로 바꾼다. 예: `--tests '*ConventionArchitectureTest.r7*'` 와 `! grep -rn '@Disabled("R7' src/test` 를 함께 쓰고, ARCH-A1 은 r1·r3, ARCH-B3 는 r2·r5 에 같은 방식을 적용한다. SIG-1 은 `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:1/x bash ./gradlew test` 처럼 DB 를 막고 실행한다. TEST-1 은 "skipped 가 아닌 테스트 ≥ N" 을 test-results XML 로 확인한다. DOC-1 은 사람 판정 항목이라고 명시하거나(`"verify": ["human"]`) 체크 스크립트를 붙인다. F4 의 커넥션 측정은 별도 항목으로 빼거나 `integrationTest` 를 verify 에 넣는다.

- [C3] §2.2 L81·§4.4 L199·§4.1 L129 — **H1 승인을 기록할 곳이 없어 plan 게이트 항목에서 루프가 멈춘다.** 에이전트가 고칠 수 있는 필드는 `passes`·`blocked` 뿐이고(L129), 스키마에 승인 필드가 없다. §4.4 의 4단계는 "멈추고 요청" 이고, 다음 세션은 3단계에서 같은 항목을 다시 고른 뒤 다시 멈춘다. 승인이 progress 에 있는지 대화에 있는지, 누가 어떤 형식으로 남기는지 정의가 없다. 11개 중 7개(SIG-1, SIG-2, FIX-F3, FIX-F4, ARCH-B2, ARCH-B3, DOC-1)가 plan 이고, 첫 항목 SIG-1 부터 해당한다. — 요구 수정: 사람만 쓰는 `"plan_approved": "<sha 또는 날짜>"` 필드(또는 `docs/ai/plans/<id>.md` 의 `Approved-by:` 줄)를 정의한다. 3단계 선택 조건에 "gate=plan 이면 계획이 없을 때 계획만 쓰고 종료, 승인돼 있으면 구현" 을 넣는다. 승인 대기 중인 항목은 건너뛰고 다음 항목으로 가도록 한다.

## 주요

- [M1] FIX-F4 (json L101-114, §4.2 L166) — **OSIV off 순서가 D4·conventions §3.4 와 반대다.** conventions §3.4 는 "§3.1(서비스 DTO 반환, R5)을 먼저 적용해야 한다", D4 는 "순서가 중요 — DTO 반환 먼저, OSIV off 나중" 이라고 적었다. 그런데 FIX-F4 가 `open-in-view=false` 를 포함하고, R5 해소는 그 뒤의 ARCH-B3(L159)에 들어가 있다. ARCH-B3 는 D1=B 전제이므로, A 를 고르면 R5(`SentenceService.getRandom` 엔티티 반환)를 맡는 항목이 아예 없다. 소유자 표기도 세 곳이 다르다: current-state §6.1 R5 해제 조건은 "conventions §3.1 적용", `ConventionArchitectureTest.java:53` 은 "FIX-F4 이후 ARCH-B3 와 함께", feature_list 는 ARCH-B3. — 요구 수정: 구조 결정과 무관한 `FIX-R5`(SentenceService DTO 반환, R5 해제)를 FIX-F4 앞에 두고 FIX-F4 depends_on 에 넣는다. 또는 OSIV off 를 별도 항목으로 빼서 R5 뒤에 둔다. ARCH-B3 acceptance 에서는 R5 를 지운다.

- [M2] decisions-needed 와의 모순 3건.
  (a) decisions-needed §0 은 FIX-F1·F2 를 "결정 대기 없이 착수" 하라고 하는데, feature_list 에서는 FIX-F1/F2 → TEST-1 → SIG-1(D3 결정 + plan 게이트) 순으로 막혀 있다. 테스트를 먼저 쓴다는 원칙(T2) 때문이라면 F1·F2 의 테스트는 DB 가 필요 없으니(conventions §6.2) TEST-1 의 SIG-1 의존을 끊을 수 있다.
  (b) D5 추천은 "`FIX-F*` 시작 전에 spotlessApply 단독 커밋" 인데, FIX-F* 의 depends_on 에 SIG-2 가 없다. SIG-2 가 H1 대기이면 3단계 선택 규칙상 TEST-1 → FIX-F1 이 먼저 진행된다.
  (c) D8 추천은 "06 수정은 FIX-F4 와 같은 PR" 인데, DOC-1 은 별도 항목이고 §1.1 은 "항목 1개 = PR 1개" 다.
  — 요구 수정: 셋 중 어느 쪽을 따를지 정하고 depends_on 이나 decisions-needed 문구를 맞춘다.

- [M3] §1.1 L33-34 단위 규칙과 예시 항목이 서로 맞지 않는다. `SIG-2` 는 src/main 34파일 전부를 바꾸는데(D5 표기 "29파일", 현재 `find src/main -name '*.java'` 34개) module 은 `"common"` 으로 적혀 있다. "5파일 이하"·"모듈 1개" 의 예외는 `ARCH-*` 만 허용된다(L33). `TEST-1` 은 answer·sentence·infra.similarity 세 모듈에 테스트를 추가하는데 module 이 `"answer"` 다. — 요구 수정: 기계적 포맷 커밋과 테스트만 추가하는 항목을 예외로 명시하고, module 에 `"*"` 또는 배열을 허용한다.

- [M4] §2.2 human_gate 규칙(L87-93)과 실제 배정이 맞지 않는다(4번 중점: 과한 곳·빠진 곳).
  과한 곳: `ARCH-B3`(git mv·의존성·API 변경 없음)와 `FIX-F4`(규칙에 해당 행 없음)가 plan 이다. `SIG-2` 는 D5 를 H0 에서 결정하면 남는 일이 기계적 `spotlessApply` 뿐이라 H1 에서 볼 것이 없다.
  빠진 곳: `FIX-F2` 는 응답 body 의 `message` 를 `TMI.SENTENCE.NOT_FOUND` 에서 `Sentence not found: …` 로 바꾸므로 규칙 1행 "외부 API 응답(body) 변경 → plan" 에 해당하는데 none 이다. 03 명세로 되돌리는 변경이라 none 이 맞다면, 규칙을 "03 명세와 **다른 방향**으로의 변경" 으로 고쳐야 FIX-F3(D2 에 따르면 이것도 버그 수정)과 기준이 일관된다.
  §2.3 의 "모두 4점 + 최소 2라운드" 를 1~5파일짜리 코드 항목 11개 전부에 적용하는 것도 과하다. 이 rubric(근거성·판단 가능성)은 문서용이다. best-practices 원문도 "Tell the reviewer to flag only gaps that affect correctness or the stated requirements" 라며 지적을 끝까지 쫓다 과설계되는 것을 경고한다. — 요구 수정: 규칙 표를 실제 배정에 맞추거나, 배정을 규칙에 맞춘다. 코드 항목 리뷰는 1라운드 + 치명/주요 0 + verify 실패→통과 확인으로 줄인다.

- [M5] §3 체크리스트 L108-117 의 "✓ 기계 검증" 중 상당수가 지금은 사실이 아니다. #3 R2, #4 R5, #5 R6 는 모두 `@Disabled` 상태다(위 [실행]: 6/8 skipped). #1 "✓ (init.sh)" 라고 했지만 init.sh(§4.3)는 항목의 verify 를 실행하지 않는다. #10 "✓ git diff --stat" 는 수작업이다. — 요구 수정: "현재 @Disabled — 해제 항목 ID 이후 ✓" 로 상태를 나눠 적고, #1·#10 은 hook 이나 스크립트로 실제 자동화하거나 "수동" 으로 표기한다.

- [M6] §4.3 init.sh(L174-184)와 §4.4 2단계(L194)가 충돌한다. `set -e` 아래에서 `./gradlew test` 는 SIG-1 이 끝나기 전, DB 가 없는 환경에서는 항상 실패한다. 2단계 규칙은 "실패하면 새 항목을 시작하지 않는다" 이므로 첫 항목 SIG-1 을 시작할 수 없다. 이 환경처럼 DB 가 있으면 반대로 init.sh 가 SIG-1 미완료를 숨긴다. 실패하면 그 자리에서 종료되므로 뒤에 있는 `git log` 도 출력되지 않는다. 원문 글의 init.sh 는 "개발 서버를 띄우고 기본 end-to-end 테스트" 인데, 이 안에는 앱 기동이나 스모크 호출이 없다. F1(호출하면 500)은 바로 그 스모크가 잡는 종류의 결함이다. — 요구 수정: SIG-1 전에는 test 실패를 경고로만 처리하는 예외를 명시하고, `git log` 를 맨 앞으로 옮긴다. 선택 사항으로, SIG-1 이후에는 `bootRun` + `curl /api/v1/sentences/random` 스모크를 넣는다.

- [M7] §4.6 L236-237 — **근거가 낡았고, 따르라는 기존 방식에 결함이 있다.** code.claude.com/docs/en/hooks 는 지금 접근된다(curl 200). research.md §9 에는 code.claude.com 이 접근 실패로 적혀 있지도 않다(docs.anthropic.com 만 있음). 원문은 (1) Stop 의 exit 2 = "Prevents Claude from stopping, continues the conversation" 이라고 확인해 주고, (2) Stop 입력의 `stop_hook_active` 로 무한 차단을 피하라고 하며, (3) "8-consecutive-continuation cap … resets each time Claude calls a tool" 이라는 내장 상한을 둔다. 반면 `check-done.sh:6-8` 의 카운터는 `/tmp/stop-hook-count` 를 **한 번도 초기화하지 않는다**. 세션·프로젝트가 바뀌어도 누적되므로 15번 막고 나면 그 머신에서는 게이트가 영구히 꺼진다. 이 방식을 "따른다" 고 하면 그 버그도 함께 물려받는다. — 요구 수정: 문서 인용을 hooks 문서로 바꾸고, `stop_hook_active` 를 확인하도록 하고, 카운터를 `session_id` 별 파일로 둔다(또는 내장 상한에 맡긴다). feature 루프를 시작할 때 `check-done.sh` 를 settings.json 에서 내릴지 §4.1 L132 에 적는다.

## 경미

- [m1] §4 L123·§4.1 — 원문 진행 파일 이름은 `claude-progress.txt` 다. `.md` 로 바꾼 것 자체는 괜찮지만, 원문의 "JSON 이 Markdown 보다 덮어쓰기에 강하다" 논리를 인용하는 같은 절이므로 "append-only 로그라 md 를 택함" 처럼 이유를 한 줄 적어 둔다.
- [m2] §4.1 L129 — 원문은 "`passes` 만 수정" 이다. `blocked` 수정 권한은 이 저장소에서 확장한 것인데 L123 요약과 구분되지 않는다. "(이 저장소 확장)" 이라고 표기한다.
- [m3] §4.5 L212 — 원문은 "work on only one feature at a time" 일 뿐 "항목마다 새 세션" 을 요구하지 않는다. 이 부분은 이 문서의 판단으로 표기한다.
- [m4] §4.5 L214 "@Disabled 는 blocked 와 함께만" 과 TEST-1 의 "실패 테스트는 @Disabled(\"FIX-F*\")" 가 충돌한다. "기존 테스트를 끄는 것" 과 "새 테스트를 예정된 FIX 와 묶어 추가하는 것" 을 구분해 적는다.
- [m5] §5 L253 "지우면 에이전트가 실수할 것" 의 출처는 memory 가 아니라 best-practices 다("Would removing this cause Claude to make mistakes?"). 인용을 붙인다.
- [m6] §0 L17 "ArchUnit 7개 규칙" — 실제로는 R4b 를 포함해 8개 테스트다(current-state L417 "8개 규칙", [실행] 결과 3+2+3).
- [m7] architecture-proposal §4.3 단계 1 은 "`SimilarityClientConfig` 분리 + `AnswerService → SentenceService` 경유", 완료 신호를 R3·R4 로 적었다. ARCH-A1 은 앞쪽만 담고 R1·R3 를 신호로 쓰며, 단계 2 표는 R1 을 B 단계 신호로 적었다. 테스트 주석(ModuleArchitectureTest:28-29)은 ARCH-A1 과 같다. architecture-proposal 쪽을 고치거나 "AnswerService → SentenceService 경유" 항목을 추가한다.
- [m8] decisions-needed D3 "TEST-1 (§6.3 의 검증된 테스트 **3종**)" 과 TEST-1 의 4개 클래스(AnswerControllerTest 포함) 표기가 다르다. AnswerControllerTest 는 §6.3 에 코드가 없다(문장 1줄뿐).
- [m9] ARCH-B2(json L132-147)는 §1.2 L44 "D1 미결 상태의 ARCH-B* 는 등록하지 않음" 과 달리 예시에 이미 들어가 있다. evidence 에만 "D1=B 결정 후 등록" 이라고 적혀 있어서, 복사해 그대로 쓰면 미결 항목이 함께 등록된다. 별도 배열(`"pending_decision"`)로 분리하는 것을 권한다. acceptance "answer 가 similarity 의 base package 만 import" 도 기계 검증이 없다(R2 는 B3 까지 Disabled).
- [m10] FIX-F3 acceptance "404 는 INFO 로그, 5xx 는 스택트레이스" 를 확인하는 방법(`OutputCaptureExtension` 등)이 명시돼 있지 않다.

## 이전 라운드 지적 해소 여부 (해당 시)

- 해당 없음 (Round 1). §2.3 L100 이 인용한 current-state round-1 C1(OSIV 커넥션 점유)·M2(`RestClient.Builder` 빈 부재)는 해당 리뷰 파일 L18·L24 에 실제로 있다(확인).

## 검증한 주장 (문제 없음 확인)

- Anthropic 글: 게시일 "Published Nov 26, 2025", initializer agent + coding agent, `init.sh`·progress 파일·초기 git 커밋, feature list 의 `passes` 필드만 수정, 원문 문장 "It is unacceptable to remove or edit tests because this could lead to missing or buggy functionality.", "the model is less likely to inappropriately change or overwrite JSON files compared to Markdown files", "work on only one feature at a time", 세션 시작 루틴(pwd → git log·progress → feature list), 실패 유형 4가지(조기 완료 선언 / 버그·미기록 상태 / 조기 passing 표시 / 실행 방법 탐색) — 모두 원문과 일치.
- best-practices: 절 제목 "Give Claude a way to verify its work" 와 "Have Claude show evidence rather than asserting success" 가 원문에 있다. 새 컨텍스트 서브에이전트 리뷰("Add an adversarial review step", "A fresh context improves code review")도 원문에 있어 §2.3 을 뒷받침한다. 이 문장들을 §2.3 근거로 추가 인용할 것을 권한다.
- memory: "Size: target under 200 lines per CLAUDE.md file. Longer files consume more context and reduce adherence." — L252 와 일치. 현재 CLAUDE.md 초안은 65줄로 이 기준을 만족한다.
- `--tests '*ArchitectureTest*'` 필터는 실제 3개 클래스(Convention/Layer/ModuleArchitectureTest)와 모두 매칭된다 [실행]. 다른 테스트 클래스에는 매칭되지 않는다.
- `*SimilarityClientTest`, `*SentenceControllerTest`, `*ControllerTest` 패턴은 conventions §6.3 의 클래스 이름과 맞다. 대상 테스트가 아직 없을 때 Gradle 은 "No tests found" 로 실패하므로, TEST-1 → FIX-* depends_on 순서와도 맞다.
- depends_on 그래프에 순환이 없고, 참조하는 id 는 모두 존재한다.
- ArchUnit `@Disabled` 사유에 적힌 해제 항목 ID(R1·R3→ARCH-A1, R2→ARCH-B2·B3, R6→FIX-F4, R7→FIX-F1)는 feature_list acceptance 와 일치한다(R5 는 [M1] 참고).
- hook 등록 형식(`.claude/settings.json` 의 Stop → command)은 기존 파일과 같다. Stop hook 의 exit 2 가 종료를 막는다는 점은 hooks 문서로 확인했다.
- `bash ./gradlew` 를 쓰는 이유(gradlew 100644)는 blockers.md §2 #1 과 일치한다.
