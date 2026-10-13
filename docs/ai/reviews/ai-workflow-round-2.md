# ai-workflow 리뷰 — Round 2

> 대상 커밋: `3e136c6` (HEAD). 대상: `docs/ai/ai-workflow.md`, `docs/ai/feature_list.example.json`. 대조: `decisions-needed.md` §0·D3·D5·D8·D12, `architecture-proposal.md` §4.3, `conventions-proposal.md` §6.3, ArchUnit 테스트, `.claude/hooks/check-done.sh`.
> 검증 방법 (모두 전용 worktree `scratchpad/wt-ai-workflow` = `3e136c6` 에서 실행, 끝난 뒤 제거):
> - §4.6 `check-feature.sh` 를 문서에서 그대로 추출해 임시 git 저장소(`scratchpad/hooksim2`)에서 이전 5개 + 사람 판정·`stop_hook_active` 2개 + **새 우회 시나리오 11개**를 실행했다.
> - feature_list 의 verify 명령을 HEAD 상태와 "의존 항목 완료" 상태에서 실행했다. TEST-1 상태는 conventions §6.3 의 테스트 3개를 `@Disabled("FIX-F1 …")`/`("FIX-F2 …")` 로 추가해 재현했고, FIX-F1·FIX-F2 는 실제로 수정한 뒤 "수정 전 실패 → 수정 후 통과"를 확인했다.
> - palantir 포맷(`spotlessApply`)을 실제 테스트 파일과 임시 파일에 적용해 `@Disabled` 사유 문자열이 어떤 줄에 놓이는지 확인했다.
> - §4.3 `init.sh` 를 `scripts/init.sh` 로 추출해 `bash -n` 검사와 실행(SIG-1 전 분기)을 하고, jq 분기 판정은 passes=true/false/파일 없음 3가지 경우로 확인했다.
> - `curl https://code.claude.com/docs/en/hooks` (200) 에서 Stop hook·`stop_hook_active`·8회 상한 원문을 확인했다. `docker info` 로 Docker 데몬 상태를 확인했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | 실행 근거가 많고 hook 표의 7개 시나리오는 그대로 재현된다. 하지만 "DOC-1 은 hook 이 차단", "TEST-1 은 --tests 지정으로 대응", §3 #1 "✓" 은 실행해 보면 성립하지 않는다 |
| 구체성 | 3 | 스크립트와 명령이 그대로 실행된다(init.sh 도 동작). 다만 FIX-F3 verify 는 아무것도 고치지 않아도 통과하고, hook 은 두 번째 종료 시도에서 무조건 풀린다 |
| 일관성 | 4 | Round 1 의 decisions-needed 모순(§0·D4·D5·D8)은 모두 정리됐다. 남은 것은 숫자·목록 차이(8 대 7 시나리오, 29 대 30 파일, §0 목록, ARCH-A1 의 D1 의존)와 Docker 전제 누락 정도다 |
| 판단 가능성 | 3 | H0~H3 와 `plan_approved` 로 승인 흐름이 생겼다. 그러나 반려 경로가 교착 상태이고 DOC-1 은 끝낼 방법이 없다. D12 를 "hook 이 막아 준다"는 전제로 결정하면 판단을 잘못하게 된다 |

## 치명 (승인 차단)

- [C1] §4.6 L277 `[ "$(jq -r '.stop_hook_active // false' …)" = "true" ] && exit 0` — **이 게이트는 한 번만 막는다. 에이전트가 종료를 한 번 더 시도하면 무조건 통과된다.** 근거 [실행]: 시나리오 N6 — passes=true 이고 기록이 없으면 첫 Stop 은 `exit=2` 다. 같은 상태에서 `{"stop_hook_active":true}` 로 두 번째 Stop 을 보내면 `exit=0` 이다. 사람 판정 항목(DOC-1)을 true 로 바꾼 경우도 똑같다. hooks 원문은 "Check this value or process the transcript **to avoid blocking on a condition that will never resolve**" 라고 한다. 즉 해결할 수 없는 조건일 때만 풀라는 뜻이다. 그런데 여기서 막는 조건(기록 누락, human 항목의 passes=true)은 에이전트가 passes 를 되돌리거나 verify 를 실행하면 **언제나 해결된다.** 무한 차단은 원문의 내장 상한("8-consecutive-continuation cap … overrides the next block")이 이미 막아 준다. 그 결과 §4.2 L186 "에이전트는 `passes=true` 로 바꿀 수 없음 (§4.6 hook 이 차단)", §3 #1 "✓", §4.7 "검증 없이 passing 표시 → hook 의 id 별 확인" 은 모두 "한 번 경고한다" 수준으로 약해진다. decisions-needed D12 도 이 보장을 전제로 추천하고 있다. — 요구 수정: `stop_hook_active` 로 바로 빠져나가는 줄을 지우고 내장 8회 상한에 맡긴다. 또는 `session_id` 별 카운터 파일(예: `${TMPDIR}/check-feature-$session_id`)로 N회까지는 계속 막는다. human 항목 위반은 상한과 관계없이 항상 exit 2 로 막는다. 표의 "stop_hook_active: true → 통과" 행에는 그것이 무엇을 포기하는지 적는다.

- [C2] feature_list.example.json L122 (FIX-F3 verify) 와 L16 (TEST-1 verify) — **Gradle 의 `--tests` 를 여러 개 주면 OR 로 묶인다. 그래서 하나라도 매칭되면 나머지 테스트가 없어도 통과한다.** §4.2 의 "지금은 실패해야 한다" 원칙이 이 두 항목에서 깨진다. 근거 [실행]:
  - TEST-1·FIX-F2 를 끝낸 상태(SentenceControllerTest 의 `@Disabled` 해제 + basename 수정)에서 FIX-F3 verify `--tests '*AnswerControllerTest' --tests '*SentenceControllerTest' --tests '*GlobalExceptionHandlerLogTest'` 를 **FIX-F3 작업 없이** 실행 → `exit=0`. test-results 에는 SentenceControllerTest 만 있다. AnswerControllerTest·GlobalExceptionHandlerLogTest 는 없어도 된다.
  - TEST-1: `GradeTest.java` 를 지우고 실행 → `exit=0`. GradeTest 까지 `@Disabled("FIX-F1 later")` 로 꺼서 3개 클래스가 모두 skip 이어도 → `exit=0` (`tests="1" skipped="1"`). 클래스가 존재하기만 하면 skip 된 테스트도 필터 매칭으로 친다. 그래서 §4.2 L185 의 대응 방법("`--tests` 로 지정 + FIX id grep")으로는 표 3행의 함정("전부 `@Disabled` 로 추가해도 통과")을 막지 못한다.
  - 참고: 단일 필터를 쓰는 FIX-F1T(`No tests found` 로 실패)와, 앞에 grep 가드가 있는 FIX-F1·F2·R5·F4·ARCH-A1 은 정상이다(아래 "검증한 주장").
  — 요구 수정: 클래스마다 `bash ./gradlew test --tests X` 를 따로 실행하는 명령으로 나눈다. 아니면 실행 뒤 `build/test-results/test/TEST-*<Class>.xml` 이 있는지와 `skipped="0"` 인지를 jq·grep 으로 확인하는 명령을 덧붙인다(예: `for c in AnswerControllerTest GlobalExceptionHandlerLogTest; do grep -q 'skipped="0"' build/test-results/test/TEST-*.$c.xml || exit 1; done`). TEST-1 은 GradeTest 에 이 확인을 붙인다. §4.2 표 3행의 대응 방법도 고친다.

- [C3] §2 L65 (`H1 -->|반려: 계획에 코멘트| S`) 와 §4.4 L242 ("이미 계획이 있으면 그 항목은 건너뛴다") — **반려 경로가 교착 상태다.** 반려된 항목은 `plan_approved: null` 이고 `plans/<id>.md` 가 있다. 그래서 3단계에서는 승인되지 않아 고를 수 없고, 계획 작성 분기에서는 "계획 있음" 이라 건너뛴다. 사람이 계획 파일에 코멘트를 달아도 그 항목을 다시 집는 규칙이 없다. 사람이 계획 파일을 직접 지워야만 다시 진행되는데, 그 방법이 문서 어디에도 없다. SIG-1 이 반려되면 SIG-2 → FIX-F3, FIX-F4 → DOC-1 이 전부 멈춘다. — 요구 수정: 반려를 기록하는 상태를 정의한다. 예: 사람이 `plan_approved: "rejected: <사유>"` 를 쓰거나, 계획 파일 맨 위에 `Status: changes-requested` 줄을 둔다. 그리고 3단계에 "반려된 계획이 있으면 그 계획을 고치고 세션 종료" 를 넣는다. 머메이드 그림과 §2.2 H1 의 "기록 방법" 열도 같이 고친다.

## 주요

- [M1] §4.6 L284-291 — C1 과 별개로, hook 은 **현재 작업 트리의 값만 믿고 base 와 비교하지 않는다.** 그래서 다음 우회가 모두 `exit=0` 이다 [실행]:
  (N1) `feature_list.json` 을 JSON 이 깨지게 만든 경우 → `jq: parse error` 를 출력한 뒤 `after` 가 비어 통과한다.
  (N2) passes 커밋 뒤에 `session-base: <새 HEAD>` 를 한 줄 더 쓴 경우 → `tail -1` 이 새 base 를 집어 통과한다.
  (N3) 이전 세션에 남은 `verify: TEST-1 OK` 기록(append-only 파일)이 있는 경우 → 이번 세션에 verify 를 실행하지 않았어도 통과한다.
  (N4) DOC-1 의 `verify` 를 `["human"]` 에서 다른 값으로 바꾼 뒤 기록 한 줄을 남긴 경우 → 통과한다. §4.5 L260 은 "에이전트는 verify 를 고치지 않는다" 고 하지만 hook 은 이를 확인하지 않는다.
  (N7) 실패하는 항목을 `items` 에서 지운 경우 → 통과한다.
  — 요구 수정: jq 가 실패하면 exit 2 로 막는다. `verify`·`acceptance`·`id` 목록·`human_gate` 를 base 버전과 비교해서 바뀌었으면 exit 2 로 막는다. session-base 는 progress 파일이 아니라 SessionStart hook 이 `$CLAUDE_PROJECT_DIR/.git/` 아래(또는 session_id 파일)에 저장한다. 기록 검색은 그 세션의 session-base 줄 **뒤**로 한정한다.

- [M2] DOC-1 (json L186-202) + §4.4 3·7단계 + §4.5 L262 — **사람 판정 항목은 끝낼 방법이 없다.** DOC-1 은 승인되고 의존 항목이 끝나면 선택 조건을 만족한다. 에이전트가 문서를 고쳐도 passes 는 바꿀 수 없다(바꾸면 hook 이 막는다). 따라서 다음 세션이 DOC-1 을 다시 고르고, "2세션 연속 실패 → blocked" 규칙에 걸려 blocked 가 되며, "blocked 3개 이상이면 루프 중단" 의 개수에 들어간다. — 요구 수정: `verify: ["human"]` 항목은 에이전트가 작업을 마치면 진행 중이라는 상태(예: `blocked: "awaiting-human-verify"` 를 정식 값으로 하되 blocked 개수에서 제외, 또는 별도 필드 `awaiting_human: true`)를 남기도록 하고, 3단계 선택 조건에서 그 항목을 제외한다. H2/H3 에서 사람이 passes 를 바꾸는 절차도 적는다.

- [M3] FIX-F4 verify L160 `bash ./gradlew integrationTest --tests '*OsivConnectionIT'` — **Docker 가 필요한데 그 전제가 루프 문서에 없다.** 근거 [실행]: 이 클라우드 세션(hook 이 대상으로 삼는 `CLAUDE_CODE_REMOTE=true` 환경)에서 `docker info` 를 실행하면 `failed to connect to the docker API at unix:///var/run/docker.sock` 가 나온다(CLI 와 `dockerd` 바이너리는 있지만 데몬이 떠 있지 않다). Docker 가 필요하다는 말은 decisions-needed D3 의 단점 열과 conventions §6.4 에만 있고, ai-workflow §0 신호 표·§4.3 init.sh·§4.7 "실행 방법 탐색" 에는 없다. 이 상태로 루프를 돌리면 FIX-F4 는 환경 때문에 2세션 동안 실패한 뒤 blocked 가 되고, DOC-1 도 함께 막힌다. — 요구 수정: §0 표에 "Docker 데몬" 행을 추가한다. init.sh 에서 `docker info >/dev/null 2>&1 || echo "Docker 없음 — integrationTest 항목(FIX-F4) 착수 불가"` 로 확인한다. §4.4 3단계에 "환경 전제가 없으면 blocked 대신 건너뜀" 을 넣거나, 항목에 `requires: ["docker"]` 필드를 둔다.

- [M4] §3 #1 L123 "✓ (§4.6 hook 이 id 별로 확인)" 과 기록 형식 — hook 이 확인하는 것은 **progress 에 `verify: <id> OK` 라는 글자가 있는지** 뿐이다. "verify 가 통과했다" 는 확인하지 않으므로 이 행은 "기록 존재만 자동, 통과 여부는 리뷰어가 재실행(§2.3)" 으로 적어야 한다. 기록 형식도 문서 안에서 서로 다르다. §2.1 L83 은 "`verify: <id>` 로 시작하는 줄", §4.4 L247 과 hook L291 은 `^verify: <id> OK` 다. 마크다운 목록(`- verify: …`)이나 `verify: TEST-1 통과` 로 쓰면 막힌다. 또 §4.4 4단계의 `session-base: <sha>` 를 .md 관례대로 백틱으로 감싸면(``session-base: `abc…` ``) 정규식이 매칭되지 않아 "session-base 가 없음" 으로 막힌다 [실행 N5]. — 요구 수정: 정확한 줄 형식을 한 곳(§4.4)에 코드 블록으로 고정하고 §2.1 은 그곳을 참조하게 한다. §3 #1 의 자동 열을 고친다.

- [M5] §4.4 L242 — **계획은 "고를 항목이 하나도 없을 때"만 쓴다.** 그래서 H1 대기가 뒤로 밀린다. 배열 순서대로 따라가면 SIG-1 계획은 TEST-1, FIX-F1, F1T, F2, R5, ARCH-A1 의 6세션이 모두 끝난 뒤에야 작성된다. 그 뒤 사람의 H1 을 기다리는 동안 SIG-2·F3·F4·DOC-1 이 줄줄이 멈춘다. SIG-1 은 D3 의 "결정하면 바로 시작" 대상이다. 계획을 쓸 후보가 여러 개일 때 어떤 순서로 고르는지도 정해져 있지 않다. 이 분기에는 depends_on 조건이 없어서, DOC-1 계획이 F3·F4 보다 먼저 쓰이고 낡아질 수도 있다. — 요구 수정: "gate=plan 이고 depends_on 이 충족됐거나 1단계 남은 항목의 계획 작성" 을 일반 항목보다 앞에 두거나 H0 때 한꺼번에 작성하게 한다. 계획 작성 분기에도 순서 규칙(배열 순서)과 depends_on 조건을 적는다.

- [M6] `pending_decision["D1=A'"]` ARCH-A2 (json L245-261), §2.2 L106 — **에이전트가 자기 작업을 판정하는 규칙(R2)을 직접 고쳐 쓰는데 gate 가 `none` 이다.** title 은 "R2 를 '타 기능 repository·domain 접근 금지' 규칙으로 교체", verify 는 `! grep '"R2 위반'` + ModuleArchitectureTest 다. 새 규칙을 에이전트가 작성하므로 통과 기준도 에이전트가 정하게 된다. §4.5 "verify·acceptance 를 고치지 않는다" 의 취지와 맞지 않는다. — 요구 수정: ARCH-A2 를 `human_gate: "plan"` 으로 바꾸고 계획에 새 규칙의 ArchUnit 코드를 포함하게 한다. 또는 규칙 교체를 별도 항목(사람 승인)으로 떼어 내고 ARCH-A2 는 그 규칙을 통과시키는 작업만 맡는다.

## 경미

- [m1] §4.3 init.sh — (a) L212 주석은 "마지막에 요약" 이라고 하지만 요약 출력이 없다. `status` 만 exit 로 돌려준다. (b) L216 `java -version 2>&1 | head -1` 은 이 환경에서 `Picked up JAVA_TOOL_OPTIONS: …` 줄을 출력한다 [실행]. `grep -m1 version` 으로 바꾼다. (c) §4.4 L240 은 `scripts/init.sh` 를 직접 실행하는데, 100644 로 커밋하면 `Permission denied` 가 난다 [실행]. gradlew 와 같은 이유이므로 `bash scripts/init.sh` 로 적는다.
- [m2] init.sh L219 — SIG-1 이후 분기의 `bash ./gradlew test` 는 DB 를 막지 않는다. DB 가 떠 있는 환경(이 환경 포함)에서는 누군가 DB 가 필요한 테스트를 `integration` 태그 없이 추가해도 알아채지 못한다. SIG-1 verify 와 같이 `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:1/none` 를 붙인다.
- [m3] FIX-F1 verify `! grep -rn '"FIX-F1' src/test` 는 접두사가 같은 `"FIX-F1T …` 사유도 잡는다. FIX-F1T 테스트를 `@Disabled("FIX-F1T…")` 로 먼저 추가하는 흐름이 생기면 FIX-F1 이 거짓으로 실패한다. `'"FIX-F1 '`(공백 포함)이나 `-w` 를 쓰고, TEST-1 의 `@Disabled` 사유 형식을 "`<ID>` + 공백" 으로 정한다.
- [m4] verify 는 그 시점에만 맞는 명령이다. TEST-1 의 `grep -rln '"FIX-F1'` 는 FIX-F1 이 끝나면 실패하므로, H2 나 이후 회귀 확인 때 다시 실행하면 거짓 실패가 난다. §4.2 에 "verify 는 해당 PR 시점에만 유효하고 회귀용이 아니다" 를 한 줄 적는다.
- [m5] decisions-needed D12 L133 은 "8개 시나리오 실행 확인" 이라고 하지만 ai-workflow §4.6 표는 7행이다.
- [m6] ai-workflow §1.1 L43 "SIG-2 (29파일)" 과 decisions-needed D5 "30파일(src/main 29 + 테스트 1)" 이 다르다. 실제로 `spotlessApply` 를 하면 30파일이 바뀐다(`CoreApplicationTests.java` 포함) [실행].
- [m7] ai-workflow §0 L21 은 "기다리지 않는" 항목으로 TEST-1·F1·F2 만 들지만, decisions-needed §0 은 F1T·R5 까지 5개를 든다.
- [m8] decisions-needed D1 L55 는 `ARCH-A1` 을 "결정하면 바로 시작" 으로 적었다. 하지만 feature_list 에서는 `items` 에 들어 있고 D1 의존이 없어서, 루프는 D1 전에 ARCH-A1 을 실행한다. 어느 선택지든 버려지지 않는다는 판단이 맞다면 §0 목록으로 옮긴다.
- [m9] SIG-2 `human_gate: "none" (D5 결정 전제)` — 이 전제를 기계적으로 표현하는 장치가 없다. D5 가 (b)·(c) 로 결정되면 사람이 H0 에서 SIG-2 를 `items` 에서 빼야 한다는 것을 §2.2 에 적는다. SIG-2 의 `depends_on: ["SIG-1"]` 은 근거가 적혀 있지 않고(D5 에도 없다), 그 때문에 FIX-F3 가 SIG-1 의 H1 대기에 함께 묶인다.
- [m10] hook 은 `pending_decision` 안 항목의 passes 변경(N10)과 문자열 `"true"`(N11)를 보지 않는다. 위험은 낮지만 M1 의 base 비교에 함께 넣는다.
- [m11] decisions-needed D6 은 "결정하면 바로 시작: DOC-1 의 03 부분" 이라고 하지만 DOC-1 은 FIX-F3·FIX-F4(SIG-1 + Docker)에 의존한다. 03 부분만 먼저 할 수 있는지 정한다.

## 이전 라운드 지적 해소 여부 (해당 시)

| Round 1 | 상태 | 확인 |
|---|---|---|
| C1 hook 결함 (A~E) | **부분 해소** | 표의 7개 시나리오를 재실행한 결과가 표와 모두 같다(A 차단, B 차단, B 정상 기록은 통과, C 차단, D 통과, E 차단, human 차단, stop_hook_active 통과). REMOTE 가드·`cd` 도 추가됐다. 다만 C1·M1 의 새 우회가 있다 |
| C2 verify 가 지금 통과 | **대부분 해소** | FIX-F1·F2·R5·F4·ARCH-A1·B2·B3·A2·SIG-1·SIG-2·F1T 는 지금 실패한다 [실행]. FIX-F3·TEST-1 은 C2 참고 |
| C3 H1 기록 수단 없음 | **부분 해소** | `plan_approved` 필드와 선택 규칙이 추가됐다. 반려 경로는 교착 상태다(C3) |
| M1 OSIV 순서 | 해소 | FIX-R5 → FIX-F4 depends_on, `ConventionArchitectureTest.java:53-54`, current-state L467 이 모두 FIX-R5 로 맞는다 |
| M2 decisions 모순 (a)(b)(c) | 해소 | TEST-1 의 SIG-1 의존 제거, D5 = "FIX-F3 전" + SIG-2→F3, D8 = (b) 별도 PR |
| M3 단위 규칙 예외 | 해소 | §1.1 예외 표, module `"*"`/배열 |
| M4 gate 규칙 대 배정 | 해소 | §2.2 표와 json 의 gate 를 하나씩 대조해 모두 일치. 코드 리뷰는 1라운드로 줄었다 |
| M5 §3 자동 표시 | 대부분 해소 | "해제 후 ✓" 로 나눠 적었다. #1 은 M4 참고 |
| M6 init.sh | 해소 | `set -e` 제거, `git log` 를 맨 앞으로, SIG-1 전에는 compileTestJava 만 실행. 실행 결과 exit 0, spotless 위반은 정보로만 출력 [실행] |
| M7 hooks 근거·check-done 카운터 | 해소 (C1 의 새 문제는 별개) | 인용문이 hooks 원문과 일치한다. check-done.sh 의 `/tmp/stop-hook-count` 를 초기화하지 않는다는 점도 `.claude/hooks/check-done.sh:6-8` 에서 확인 |
| m1~m10 | 해소 | L148 `.md` 이유, L146 "(이 저장소 확장)", L259 "이 문서의 판단", L261 신규/기존 구분, L324 best-practices 인용, L18 "8개 중 6개", architecture §4.3 단계 1 = R1·R3, TEST-1 3종, `pending_decision`, FIX-F3 `OutputCaptureExtension` (단 verify 에서는 C2 때문에 강제되지 않음) |

## 검증한 주장 (문제 없음 확인)

- **verify 가 "지금 실패" 하는지** [실행]: HEAD 에서 `! grep '"R1|R2|R3|R5|R6 위반'` 은 모두 exit 1, `! grep 'com.tmi.core.infra' src/main` 도 exit 1, `grep 'enforceCheck = true' build.gradle` 은 exit 1, `spotlessCheck` 는 실패, `--tests '*SimilarityTimeoutTest'` 는 "No tests found" 로 실패, SIG-1 verify(`SPRING_DATASOURCE_URL=…localhost:1/none`)는 `9 tests completed, 1 failed, 6 skipped` → FAILED(ConnectException). 환경변수가 test JVM 까지 전달돼 `application.properties` 의 URL 을 덮어쓰는 것도 확인했다.
- **FIX-F1 의 실패 → 통과 흐름** [실행]: TEST-1 상태에서 첫 grep 이 `@Disabled("FIX-F1 …")` 를 찾아 실패한다. `@Disabled` 만 지우고 고치지 않으면 gradle 이 실패한다. `@RequestBody` 를 추가하면 grep 2개와 gradle 모두 통과한다(SimilarityClientTest 1/0 skip, ConventionArchitectureTest 3/2 skip). FIX-F2 도 같은 방식으로 수정 전 실패, basename+fallback 추가 후 통과를 확인했다.
- **palantir 와 사유 문자열 grep** [실행]: 현재 6개 `@Disabled` 는 spotlessApply 를 해도 한 줄로 남는다. 사유가 긴 임시 파일에서는 `@Disabled(` 와 `"R9 위반 …"` 이 다른 줄로 나뉜다. `value = "…"` 형식은 세 줄로 나뉜다. 어느 경우든 문자열이 `"R<n> 위반` 으로 시작하므로 §4.2 L183 의 "어노테이션 이름이 아니라 사유 문자열로 grep" 이 맞다. 다만 "나눈다" 는 길이가 120자를 넘을 때만 그렇다.
- §4.3 init.sh: `bash -n` 통과. SIG-1 분기의 jq 판정은 passes=true → 0, false → 1, 파일 없음 → 2(else 분기로 감)로 의도대로 동작한다. SIG-1 전 실행 결과는 compileTestJava 성공, spotless 경고, exit 0 이다.
- hooks 문서 원문: "The stop_hook_active field is true when Claude Code is already continuing as a result of a stop hook", "8-consecutive-continuation cap … resets each time Claude calls a tool" — §4.6 L268 인용과 일치한다.
- `--tests '*ArchitectureTest*'` 가 지금 BUILD SUCCESSFUL(8개 중 6개 skip)이라는 §4.2 L183 의 주장은 실행 결과(6 skipped)와 같다.
- §2.2 gate 표와 json 의 `human_gate` 11+3개가 하나도 빠짐없이 일치한다. depends_on 그래프에 순환이 없고, 참조하는 id 는 모두 존재한다.
- decisions-needed 와의 일관성: §0 의 5개 항목이 모두 gate none 이고 SIG-1 에 의존하지 않는다. D3 (a)와 SIG-1 의 title·verify 가 같다. D4 와 FIX-R5→F4 가 같다. D5 와 SIG-2→FIX-F3 가 같다. D8 (b)와 DOC-1 의 depends_on 이 같다. D12 의 check-done 을 내린다는 내용과 §4.1 L151·§4.6 L308 이 같다.
- conventions §6.3 의 SentenceControllerTest 는 `willThrow` 만 쓰므로 FIX-R5 가 `getRandom` 반환 타입을 바꿔도 테스트를 고칠 필요가 없다(§4.5 의 "테스트를 고치지 않는다" 와 충돌하지 않는다).
