# ai-workflow 리뷰 — Round 3

> 대상 커밋: `83f7b92`. 대상 파일: `docs/ai/ai-workflow.md`, `docs/ai/feature_list.example.json`. 대조한 문서: `decisions-needed.md` §0·D1·D3·D5·D6·D12, ArchUnit 테스트 3개, hooks 공식 문서, 이전 리뷰 `ai-workflow-round-1.md`·`-round-2.md`.
> 검증 방법 (모든 실행은 전용 worktree `scratchpad/wt-ai-workflow` = `83f7b92` 에서 했고, 끝난 뒤 worktree 를 제거했다):
> - §4.6 의 `session-base.sh`·`check-feature.sh` 를 문서 L301-345 에서 그대로 뽑아 임시 git 저장소(`scratchpad/hs3`)에서 돌렸다. 문서 표의 14개 시나리오를 재현하고, 새 우회 시도 15개를 추가로 실행했다(X1~X12, S6′·S6″, R1).
> - feature_list 의 verify 13개 항목(사람 판정 3개 제외)을 HEAD 에서 모두 실행했다. 이어서 FIX-F1 을 **실제로 고친 상태**(TEST-1 의 `SimilarityClientTest` 추가 + `@RequestBody` 추가 + R7 `@Disabled` 제거)에서 FIX-F1 의 verify 를 다시 돌렸다. R1·R3·R5 의 `@Disabled` 를 지운 상태에서 클래스 단위 skip 수도 확인했다. Gradle 이 실행할 때마다 test-results 를 비우는지(이전 결과가 남아 판정을 오염시키는지)도 확인했다.
> - §4.3 `init.sh` 를 `scripts/init.sh` 로 뽑아 `bash -n` 으로 문법을 검사했다. 그런 다음 `bash scripts/init.sh` 를 세 가지 상태로 실행했다: feature_list 없음, SIG-1 passes=false, SIG-1 passes=true.
> - `curl https://code.claude.com/docs/en/hooks` (HTTP 200) 에서 SessionStart matcher 값과 Stop 의 8회 상한 원문을 확인했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | hook 표의 14개 시나리오는 모두 그대로 재현된다. 다만 "클래스별 명령 + `skipped="0"`" 은 고친 뒤 통과하는지 실행해 본 적이 없고, 실제로는 통과하지 않는다(C1). "N2 영향 없음 — base 는 `.git` 에서 읽음" 도 `.git` 파일을 다시 쓰면 무너진다(M1) |
| 구체성 | 3 | 스크립트·명령은 그대로 실행된다(init.sh 정상). 하지만 이 feature_list 를 복사해 루프를 돌리면 FIX-F1·FIX-R5·ARCH-A1 이 끝날 수 없다(C1) |
| 일관성 | 4 | Round 2 의 숫자·목록 불일치(m5~m8, m11)는 모두 정리됐고 decisions-needed 와 맞는다. 남은 문제는 ARCH-B2 verify 와 §0 의 "DB 없이" 전제가 어긋나는 것(M4)이다 |
| 판단 가능성 | 3 | 반려·사람 판정·환경 조건까지 상태가 정의됐다. 그러나 반려 항목이 루프를 계속 붙잡는다(M2). 또 PR 머지 전에 다음 세션이 무엇을 기준으로 시작하는지 정해져 있지 않아(M3) 운영 방식을 결정할 수 없다 |

## 치명 (승인 차단)

- [C1] feature_list.example.json L41 (FIX-F1), L157 (FIX-R5), L178 (FIX-F4), L202 (ARCH-A1) + ai-workflow §4.2 L180·L196 — **클래스 단위 `skipped="0"` 검사는 같은 클래스에 있는 다른 항목의 규칙까지 켜져 있어야 통과한다. 그래서 의존 그래프가 교착된다.** `ConventionArchitectureTest` 에는 R5(FIX-R5)·R6(FIX-F4)·R7(FIX-F1)이 함께 있고(`ConventionArchitectureTest.java:54,70,81`), `ModuleArchitectureTest` 에는 R1·R3(ARCH-A1)과 R2(ARCH-B3/A2)가 함께 있다(`ModuleArchitectureTest.java:29,41,55`). 근거 [실행]:
  - FIX-F1 을 끝까지 고친 상태(`SimilarityClientTest` 가 skip 0 으로 통과, R7 해제, `@RequestBody` 추가)에서 FIX-F1 verify 를 돌리면 4번째 명령이 `rc=1` 이다. 결과는 `ConventionArchitectureTest tests="3" skipped="2"` (R5·R6 이 아직 꺼져 있음).
  - R5 를 해제해도 `skipped="1"` (R6) 이 남는다. FIX-R5 는 R6 이 풀려야 통과하는데, R6 을 푸는 FIX-F4 는 `depends_on: ["FIX-R5"]` 다. **순환이다.** FIX-F4 도 R7(FIX-F1)이 필요한데 FIX-F1 에 의존하지 않는다.
  - R1·R3 를 해제해도 `ModuleArchitectureTest skipped="1"` (R2) 가 남는다. R2 를 푸는 ARCH-B3·A2 는 ARCH-A1 에 (B3 는 B2 를 거쳐) 의존한다. **역시 순환이다.**
  - 결과: TEST-1·FIX-F2·SIG-1·SIG-2·FIX-F3 만 끝낼 수 있다. FIX-F1 과 FIX-R5 는 §4.5 의 "2세션 연속 실패" 규칙에 걸려 `blocked` 가 된다. 그 뒤의 F1T·ARCH-A1·F4·DOC-1·B2·B3·A2 는 시작조차 못 한다. decisions-needed §0 이 "결정 없이 바로 해도 된다" 고 한 6개 중 4개가 여기에 해당한다.
  - Round 2 C2(`--tests` OR 결합) 를 고친 방식이 이 문제를 새로 만들었다. 문서는 "수정 전 실패" 만 확인했고 "수정 후 통과" 는 확인하지 않았다.
  — 요구 수정: ArchUnit 은 **메서드 단위 필터**로 바꾼다. 예: `bash ./gradlew test --tests '*ConventionArchitectureTest.r7*' && grep -q 'skipped="0"' build/test-results/test/TEST-*.ConventionArchitectureTest.xml`. [실행] 이 명령은 HEAD 에서 `tests="1" skipped="1"` → rc=1, F1 을 고친 뒤 `tests="1" skipped="0"` → rc=0 이다. FIX-R5 → `r5*`, FIX-F4 → `r6*`, ARCH-A1 → `r1*` 와 `r3*` 를 각각 별도 명령으로, ARCH-B3/A2 → `r2*` 로 한다. §4.2 함정 표에 "같은 클래스에 다른 항목의 규칙이 있으면 클래스 단위 skip 검사는 교착" 을 한 줄 추가하고, 표 아래에 고친 뒤 통과하는 것을 실행해 확인했다는 기록을 남긴다.

## 주요

- [M1] §4.6 L303-306·L322·L338·L357 — **세션 base 를 실제로는 쉽게 다시 쓸 수 있다.** 문서는 "base 는 `.git/claude-session-base` 에서 읽으므로 위조해도 영향 없다"(L357)고 하지만, 다음 경우가 모두 `exit=0` 이다 [실행]:
  - (X1) passes=true 와 `verify` 변경을 커밋한 뒤 `git rev-parse HEAD > .git/claude-session-base` 한 줄을 실행 → 통과. 사람 전용 필드 검사와 기록 검사가 모두 풀린다. progress 파일보다 더 쉽게 다시 쓸 수 있는 위치다.
  - (X3) **위조하지 않아도 된다.** SessionStart 는 `startup`·`resume`·`clear`·`compact`·`fork` 에서 모두 실행된다(hooks 문서의 matcher 표). 그런데 문서에는 matcher 와 settings.json 등록 예시가 없다. 긴 세션에서 자동 compact 가 일어나면 base 가 그 시점의 HEAD 로 바뀐다. compact 전에 커밋한 passes=true 는 더 이상 검사하지 않는다. 반대로 compact 전에 남긴 `verify:` 기록은 새 base 줄보다 앞에 있게 되어 무시된다(정상 종료가 거짓으로 막힘).
  - (X2) 직전 세션이 커밋 없이 끝나 두 세션의 base sha 가 같은 경우 → awk 가 **첫 번째** `session-base` 줄부터 읽는다. 그래서 이전 세션의 `verify: TEST-1 OK` 를 이번 세션 기록으로 친다. Round 2 의 N3 가 이 조건에서는 다시 생긴다.
  — 요구 수정: settings.json 등록 예시를 넣고 SessionStart matcher 를 `"startup"` 으로 한정한다(resume·compact 에서는 다시 쓰지 않음). base 는 stdin 의 `session_id` 를 키로 저장하고(예: `$(git rev-parse --git-dir)/claude-session-base-<session_id>`), Stop hook 도 같은 `session_id` 로 읽는다. progress 표식에도 `session_id` 를 함께 적고, awk 는 그 줄의 **마지막** 출현 이후만 읽는다. 표의 N2 행은 "에이전트가 base 파일을 직접 쓰는 것은 막지 못함 — H2 에서 확인" 으로 고친다.

- [M2] §4.4 L267 (선택 규칙 1) + §2 그림 L66-71 — **반려된 항목이 루프를 계속 붙잡는다.** 규칙 1 은 `plan_approved` 가 `"rejected: …"` 인 항목을 가장 먼저 고르고, "계획을 고치고 세션 종료" 한다. 그런데 에이전트는 `plan_approved` 를 바꿀 수 없다. 바꾸려 하면 hook 이 막는다 [실행 R1: `exit=2 사람 전용 필드 … 바뀜`]. 그러면 사람이 다시 검토할 때까지 매 세션 규칙 1 이 같은 항목을 집어 계획을 또 고치고 끝난다. 그동안 규칙 3 의 `none` 항목(예: FIX-F2, FIX-R5)은 실행할 수 있는데도 진행되지 않는다. Round 2 C3 의 교착은 풀렸지만, 대신 "고친 계획을 다시 고치는" 반복이 생겼다. — 요구 수정: 규칙 1 에 "계획 파일의 마지막 커밋이 반려 기록보다 새로우면(= 이미 수정함) 제외" 를 넣는다. 아니면 에이전트가 쓸 수 있는 `plan_revised: true` 같은 필드를 둔다. 사람이 다시 검토할 때 `rejected` 를 지우는 절차도 §2.2 H1 에 적는다.

- [M3] §1.1 "항목 1개 = PR 1개" + §2.2 H2 + §4.4 1·3단계 — **브랜치와 동기화 모델이 없다.** H2(PR 머지)는 사람이 나중에 따로 한다. 그런데 다음 세션이 main 에서 시작하는지, 직전 항목 브랜치 위에서 시작하는지 정해져 있지 않다. main 에서 시작하면 머지 전 항목은 `passes: false` 로 보이므로 **같은 항목을 다시 고른다.** 직전 브랜치 위에서 시작하면 PR 이 줄줄이 쌓이고, H2 의 "변경 요청 → I" 가 어느 브랜치로 돌아가는지 정의되지 않는다. 사람이 H1 에서 기록하는 `plan_approved` 를 세션이 받으려면 pull 이 필요하다. 하지만 세션을 시작한 뒤 pull 하면 base 와 값이 달라져 hook 이 막는다. 이때 차단 메시지는 "되돌릴 것" 이라서, 에이전트가 사람의 승인을 되돌리게 된다 [실행 X9: 세션 중 `plan_approved` 가 바뀌면 `exit=2`]. — 요구 수정: §4.4 0단계에 "세션 시작 전 main 을 pull(SessionStart 보다 먼저)" 과 "브랜치 이름 규칙(`ai/<id>`)" 을 적는다. "PR 이 열려 있는 항목" 을 나타내는 상태(예: 에이전트가 쓸 수 있는 `pr_open: true`, 또는 `gh pr list --head ai/<id>` 로 판정)를 선택 제외 조건에 넣는다. hook 차단 메시지에서는 "되돌릴 것" 대신 원인을 구분해 보여 준다.

- [M4] feature_list.example.json L245 (ARCH-B2 verify `bash ./gradlew test`) — **ARCH-B2 는 DB 가 없으면 통과할 수 없는데 SIG-1 에 의존하지 않는다.** ARCH-B2 의 depends_on 은 `ARCH-A1`·`FIX-F1T` 뿐이다. 그런데 `./gradlew test` 는 SIG-1 전에는 `CoreApplicationTests` 때문에 실 PostgreSQL 이 필요하다(§0 표 1행). [실행] DB 가 있는 이 환경에서는 rc=0 이다. DB 를 막으면(SIG-1 verify 와 같은 조건) `1 failed` 로 실패한다. §0 이 전제하는 DB 없는 환경에서는 D1 의 추천안 B 의 첫 항목이 2세션 만에 `blocked` 가 된다. — 요구 수정: `depends_on` 에 `SIG-1` 을 넣거나, verify 를 `SPRING_DATASOURCE_URL=…localhost:1/none bash ./gradlew test` 로 고정하고 SIG-1 에 의존하게 한다.

## 경미

- [m1] SIG-1 verify (json L97) — DB 를 막고 `./gradlew test` 가 통과하는지만 본다. 그래서 `CoreApplicationTests` 를 `exclude` 하거나 지우기만 해도 통과한다. `integrationTest` 태스크가 있는지, `@Tag("integration")` 이 붙었는지는 확인하지 않는다. 예: `grep -q '@Tag("integration")' src/test/java/com/tmi/core/CoreApplicationTests.java && bash ./gradlew tasks --all | grep -q integrationTest` 를 추가한다(Docker 없이 실행 가능).
- [m2] hook (X4) — `git rm docs/ai/feature_list.json` 을 하면 L317 `[ -f "$f" ] || exit 0` 에 걸려 통과한다. base 에 파일이 있었다면 차단해야 한다.
- [m3] hook (X5·X7·X8) — 다음 경우가 모두 통과한다. 일반 항목에 `awaiting_human: true` 를 쓰는 것(선택 대상에서 영구히 빠지고 blocked 개수에도 안 들어가는 탈출구), `title`·`module`·`evidence` 를 바꾸는 것(§4.1 은 에이전트가 3개 필드만 바꾼다고 함), depends_on 이 충족되지 않은 항목을 passes=true 로 하는 것. 위험은 낮지만, `awaiting_human: true` 는 `verify == ["human"]` 인 항목에만 허용하는 검사 한 줄로 막을 수 있다.
- [m4] hook L340 (X6) — 사람 판정 항목 검사가 `.items[]` 만 본다. `pending_decision` 안의 `DOC-2`(verify `["human"]`)는 `verify: DOC-2 OK` 한 줄만 있으면 passes=true 로 통과한다. L325 의 `$all` 을 쓰면 된다.
- [m5] session-base.sh L305 (X10) — `.git` 이 파일인 git worktree 에서는 `Not a directory` 오류가 나고 base 를 쓰지 못한다. 그러면 Stop 이 계속 막는다. 8회 상한은 도구를 호출할 때마다 초기화되므로(hooks 원문) 막힘이 끝나지 않을 수 있다. `$(git rev-parse --git-dir)/claude-session-base` 를 쓴다. §4.6 의 "포기하는 것" 문단에도 "상한은 도구 호출로 초기화되므로 해결 불가능한 조건(base 없음)에서는 무한 반복될 수 있다" 를 적는다.
- [m6] §2.2 H2 + §4.4 6단계 — 사람 판정 항목이 H2 에서 "변경 요청" 을 받으면, `awaiting_human: true` 를 누가 `false` 로 되돌리는지 적혀 있지 않다. 그대로 두면 선택 대상에서 영구히 빠진다.
- [m7] init.sh L241-242 — SIG-2 가 끝난 뒤에도 spotless 위반이 "정보용" 으로만 출력되고 FAIL 로 집계되지 않는다. `enforceCheck = true` 는 `check` 에만 걸리고 init.sh 가 돌리는 `test`·`compileTestJava` 에는 걸리지 않으므로, 포맷이 다시 깨져도 init.sh 로는 알 수 없다. SIG-1 처럼 `SIG-2.passes` 로 분기한다.
- [m8] TEST-1 verify L19 — `grep -rln '"FIX-F1 '` 는 `@Disabled` 가 아닌 주석·문자열에서도 참이 된다. acceptance(클래스에 `@Disabled`)와 정확히 맞지 않는다. 다음 항목 FIX-F1 의 verify 가 해제를 확인하므로 실질 영향은 작다.

## 이전 라운드 지적 해소 여부

| Round 2 | 상태 | 확인 |
|---|---|---|
| C1 `stop_hook_active` 로 풀림 | **해소** | [실행 S2] 두 번째 Stop(`stop_hook_active: true`)도 `exit=2`. 사람 판정 항목도 두 번 모두 차단 [S9] |
| C2 `--tests` OR 결합 | **해소했으나 새 치명 생김** | OR 결합은 클래스마다 명령을 나눠 해결됐다. 그러나 클래스 단위 skip 검사 때문에 FIX-F1·R5·F4·ARCH-A1 이 교착된다 (C1) |
| C3 반려 교착 | **부분 해소** | `"rejected: …"` 상태와 규칙 1 이 생겼다. 교착은 풀렸지만 반려 항목이 매 세션 다시 선택되어 루프를 붙잡는다 (M2) |
| M1 base 비교·우회 N1~N11 | **부분 해소** | N1·N3·N4·N7·N10·N11 은 차단된다 [실행]. N2(progress 위조)도 막힌다 [S6′]. 그러나 base 파일을 다시 쓰는 경우, compact 때 SessionStart 가 다시 실행되는 경우, 두 세션의 base sha 가 같은 경우는 새 우회다 (M1) |
| M2 사람 판정 항목 종료 | 대부분 해소 | `awaiting_human` 이 정의되고 선택 대상과 blocked 개수에서 빠진다. 변경 요청 때 다시 여는 절차는 없다 (m6) |
| M3 Docker 전제 | 해소 | §0 표의 Docker 행, `requires: ["docker"]`, init.sh 요약 [실행: `docker: 없음 — requires=docker 항목 건너뜀`], §4.5 의 "환경 미충족은 실패로 세지 않음" |
| M4 기록 형식 | 해소 | §4.4 에 코드 블록으로 고정하고 §2.1 은 참조. §3 #1 "기록 존재만 ✓, 통과 여부는 리뷰어" |
| M5 계획 작성 우선순위 | 해소 | 규칙 2 가 구현보다 앞서고, 공통 제외 조건이 depends_on 을 포함한다. SIG-1 계획이 첫 세션에 작성된다 |
| M6 ARCH-A2 gate | 해소 | json L304 `plan`, acceptance 에 "계획에 ArchUnit 코드 포함", §2.2 표 L108 |
| m1 init.sh | 해소 | 요약 출력, `grep -m1 version`(이 환경에서 `openjdk version "21.0.12.1"`), `bash scripts/init.sh` |
| m2 init.sh DB 차단 | 해소 | L235 `SPRING_DATASOURCE_URL=…localhost:1/none` [실행: SIG-1 passes=true 분기 → `test: FAIL`, rc=1 (SIG-1 미완료라 예상대로)] |
| m3 접두사 충돌 | 해소 | `'"FIX-F1 '`·`'"FIX-F2 '` + 사유 형식 "`<ID>` + 공백" (§4.2 표, TEST-1 acceptance) |
| m4 verify 시점 | 해소 | §4.2 L190, json `_comment` |
| m5 시나리오 수 | 해소 | §4.6 표 14행 = decisions-needed D12 "14개" |
| m6 29/30 파일 | 해소 | §1.1 L46, json L108, D5 모두 30 |
| m7 §0 목록 | 해소 | ai-workflow §0 L23 = decisions-needed §0 의 6개 |
| m8 ARCH-A1 | 해소 | decisions-needed §0 6행·D1 L56 "§0 에서 먼저 진행" |
| m9 SIG-2 전제·의존 | 해소 | §2.2 H0 "D5 가 (a) 가 아니면 SIG-2 를 뺀다", SIG-2 의 depends_on 이 `[]`, §4.2 L219 |
| m10 pending·문자열 | 해소 | [실행 S11·S12] 둘 다 `exit=2` |
| m11 D6 와 DOC-1 | 해소 | D6 L100 "H0 에서 DOC-1 을 둘로 나눈다" |

## 검증한 주장 (문제 없음 확인)

- **§4.6 표 14개 시나리오** [실행]: 모두 표와 같다. 기록 없음 → 2, `stop_hook_active` → 2, 다른 id 기록만 → 2, compact JSON → 2, 이전 세션 기록만 → 2, progress 의 session-base 위조(기록 없음) → 2, 깨진 JSON → 2, DOC-1 verify 변경 → 2, 사람 판정 항목 passes → 2, 항목 삭제 → 2, pending passes → 2, 문자열 `"true"` → 2, true→false → 0, 기록 후 커밋 → 0. `passes: null`·`items: []` 도 차단된다.
- **verify 가 HEAD 에서 실패하는지** [실행]: TEST-1·FIX-F1·F1T·F2·SIG-1·SIG-2·F3·R5·F4·ARCH-A1·B2·B3·A2 의 13개 항목 모두 명령이 최소 하나 rc≠0 이다. 단일 클래스 필터는 클래스가 없으면 `No tests found` 로 실패한다. FIX-F1 의 `! grep '"FIX-F1 '` 는 HEAD 에서 rc=0 이지만 R7 grep 이 rc=1 이라 항목 전체는 실패한다. `'"FIX-F1 '`(공백 포함)이 `"FIX-F1T` 를 잡지 않는 것도 확인했다.
- **test-results 가 남아 판정을 오염시키지 않는다** [실행]: `--tests '*ConventionArchitectureTest'` 실행 뒤 `--tests '*SimilarityClientTest'` 를 실행하면 `build/test-results/test/` 에 SimilarityClientTest XML 만 남는다. 그래서 `TEST-*.<Class>.xml` 의 `skipped` 검사는 그 실행의 결과만 본다.
- **FIX-F1 의 SimilarityClientTest 부분** [실행]: `@RequestBody` 를 추가하고 `@Disabled("FIX-F1 …")` 를 지우면 `skipped="0"` 으로 통과한다. 따라서 C1 은 ArchUnit 명령에만 해당한다.
- **§4.3 init.sh** [실행]: `bash -n` 통과. feature_list 가 없으면 jq 오류를 출력한 뒤 else 분기 → compile OK, docker 없음, spotless 위반(정보), rc=0. SIG-1 passes=false → jq rc=1 → else 분기. SIG-1 passes=true → DB 를 막은 test → FAIL, rc=1. `git log` 는 맨 앞에서 출력된다.
- **hooks 문서 인용** [curl]: "Check this value or process the transcript to avoid blocking on a condition that will never resolve", "8-consecutive-continuation cap … resets each time Claude calls a tool" 가 §4.6 L296 과 같다.
- **§4.4 선택 규칙의 나머지**: 공통 제외 조건(passes·blocked·awaiting_human·requires·depends_on)이 네 규칙 모두에 적용된다. 계획만 있고 승인되지 않은 항목은 규칙 2·3 어디에도 해당하지 않아 규칙 4 의 "대기" 로 간다. 이 경로에는 교착이 없다. Docker 가 없으면 FIX-F4 → DOC-1 은 "대기" 로 남고 실패로 세지 않는다(§4.5).
- **decisions-needed 와의 일관성**: §0 6개 항목과 ai-workflow §0 L23 이 같다. D3(a) = SIG-1, D4 = FIX-R5→F4, D5 = SIG-2→F3·30파일·H0 에서 제외, D6 = DOC-1 분할, D12 = 14개 시나리오·check-done 을 내림. §2.2 gate 표와 json 의 `human_gate` 11+5개(B2·B3·DOC-2×2·A2)가 하나씩 일치한다. depends_on 이 참조하는 id 는 모두 존재한다(순환은 depends_on 이 아니라 verify 쪽에 있다 — C1).
