# ai-workflow 리뷰 — Round 4

> 대상 커밋: `01898ba`. 대상 파일: `docs/ai/ai-workflow.md`, `docs/ai/feature_list.example.json`. 대조한 문서: `decisions-needed.md` D5·D12, `CLAUDE.md`, ArchUnit 테스트(`ConventionArchitectureTest`·`ModuleArchitectureTest`), hooks 공식 문서, 이전 리뷰 `ai-workflow-round-1~3.md`.
> 검증 방법 (모든 실행은 전용 worktree `scratchpad/wt-ai-workflow` = `01898ba` 에서 했고, 끝난 뒤 worktree 를 제거했다):
> - **verify 전수 실행**: feature_list 의 15개 항목 verify 를 jq 로 뽑아 그대로 실행하는 러너(`scratchpad/r4/runverify.sh`)로 HEAD 에서 돌렸다.
> - **수정 시뮬레이션**: (a) TEST-1 완료 상태를 만들었다. `GradeTest`, `@Disabled("FIX-F1 …")` 를 붙인 `SimilarityClientTest`, `@Disabled("FIX-F2 …")` 를 붙인 `SentenceControllerTest` 를 conventions §6.3 코드로 추가했다. 그 위에서 각 항목을 **단독으로** 고쳤다. (b) FIX-F1 만: `@RequestBody` 를 추가하고 F1·R7 의 `@Disabled` 를 지웠다. (c) FIX-R5 만: `SentenceService.getRandom` 이 `SentenceResponse` 를 반환하게 하고 R5 의 `@Disabled` 를 지웠다. (d) FIX-F1 + ARCH-A1: `RestClientConfig` 를 `infra.similarity.SimilarityClientConfig` 로 옮기고 R1·R3 의 `@Disabled` 를 지웠다. (e) R6 만: `loadSentence` 의 `@Transactional` 을 지우고 R6 의 `@Disabled` 를 지웠다. R5 는 꺼진 채로 두었다. 각 상태에서 해당 항목과 이웃 항목의 verify 를 실행했다. 음성 대조로, R7 의 `@Disabled` 만 지우고 코드는 고치지 않은 상태도 실행했다.
> - **hook**: §4.6 의 `session-base.sh`(L320-329)·`check-feature.sh`(L333-375)를 문서에서 그대로 뽑아 `bash -n` 으로 검사했다. 그런 다음 임시 git 저장소에서 표의 20개 시나리오를 재현했다. 추가 프로브 3개도 돌렸다. 하나는 bare origin 을 두고 사람이 먼저 승인하는 경우, 하나는 세션 중에 pull 하는 경우, 하나는 병렬 브랜치 2개를 머지하는 경우다.
> - `curl https://code.claude.com/docs/en/hooks` (HTTP 200) 에서 SessionStart matcher 값, `$CLAUDE_CODE_REMOTE`, 8회 상한의 원문을 확인했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | 메서드 단위 필터, 항목별 독립 통과, hook 표 20행을 모두 실행으로 재현했다. 남은 것은 표 N2 행 문구 같은 사소한 것뿐이다(m1) |
| 구체성 | 4 | verify·hook·init.sh 는 그대로 실행된다. 다만 새로 넣은 브랜치 규칙이 계획(plan) 흐름·H2 변경 요청과 맞물리는 방식이 정의되지 않았다(M1). 2~3문장이면 보완된다 |
| 일관성 | 4 | Round 3 의 수치·필드 불일치는 정리됐다. 남은 것은 decisions-needed D12 의 "14개 시나리오"(표는 20행), CLAUDE.md 의 "클래스마다" 문구 같은 잔여 불일치다(m1) |
| 판단 가능성 | 4 | 선행 조건·게이트·포기하는 것(base 재작성, 8회 상한)이 명시돼 있어 채택을 판단할 수 있다. M1 은 운영 규칙 보완이고 채택 판단을 바꾸지는 않는다 |

## 치명 (승인 차단)

없음.

## 주요

- [M1] §4.4 L278 (브랜치·PR) × L273-275 (선택 규칙 1·2·3) × §2 그림 L67-72·L80 × §2.2 H1·H2 L97-98 — **"원격에 `ai/<id>` 가 있으면 그 항목은 건너뛴다" 는 규칙이 계획 흐름과 H2 변경 요청 흐름을 막는다.** 규칙에 계획 세션 예외가 없다. 그러면 계획만 쓰는 세션(규칙 2)도 `ai/<id>` 브랜치에 계획을 올리게 된다. 이 경우:
  - 사람이 H1 에서 `plan_approved` 를 기록해도, 원격에 `ai/SIG-1` 이 남아 있으면 규칙 3(구현)으로 갈 수 없다. GitHub 는 머지 후 브랜치를 기본으로 남긴다. 또 머지하지 않으면 main 에는 계획 파일이 없다. 그 상태에서 구현 세션은 계획을 읽을 수 없다. 규칙 2 의 "계획 파일 없음" 판정도 main 기준으로는 다시 참이 된다.
  - 반려(규칙 1)는 "계획 파일 첫 줄" 을 고쳐야 한다. 그런데 브랜치가 원격에 있으면 항목 전체를 건너뛰므로 규칙 1 에 도달하지 못한다. Round 3 M2 를 고친 `Status: revised` 장치가 이 경로에서는 동작하지 않는다.
  - 그림의 `H2 -->|변경 요청| I` 는 §4.4 어디에서도 선택되지 않는다. PR 이 열려 있어 브랜치가 있으면 그 항목은 건너뛴다. 그런데 "변경 요청이 달린 PR 의 브랜치에서 재개한다" 는 규칙이 없다. 사람 판정 항목의 "변경 요청이면 `awaiting_human: false`"(L98·L287)도 어느 브랜치에 기록하는지 정해져 있지 않다.
  - 영향 범위: plan gate 항목은 SIG-1·FIX-F3·FIX-F4·DOC-1 과 보류 중인 B2·A2·DOC-2 다. 규칙 2 가 구현보다 우선하므로, 첫 세션이 SIG-1 계획을 쓸 가능성이 높다. 그래서 루프를 처음 돌리면 바로 이 경로에 들어간다.
  — 요구 수정 (택1 또는 조합):
  (1) 계획 파일은 `ai/<id>-plan` 브랜치에 올리고, H1 에서 사람이 승인하면서 main 에 머지한다. 반려는 그 PR 에 남기고 브랜치는 유지한다. 건너뛰기 조건은 `ai/<id>`(구현 브랜치)에만 적용한다.
  (2) H2 머지 시 브랜치를 삭제한다고 §2.2 에 적는다(또는 저장소 설정 "Automatically delete head branches").
  (3) 선택 규칙 0번으로 "열린 PR 에 변경 요청이 있는 항목 → 그 `ai/<id>` 브랜치를 checkout 해서 재개" 를 추가한다(`gh pr list --head ai/<id> --json reviewDecision`).

## 경미

- [m1] 잔여 불일치. (a) decisions-needed.md L134 D12 는 "14개 시나리오 실행 확인" 이라고 하지만 §4.6 표는 20행이다. (b) §4.6 표 N2 행(L387)은 여전히 "영향 없음 — base 는 `.git/claude-session-base` 에서 읽음" 이다. 실제 경로는 `$(git rev-parse --git-dir)/…` 이다. L317 은 "base 파일을 직접 다시 쓰는 것은 막지 못한다" 고 하므로 표와 본문이 다르다. Round 3 M1 이 요구한 문구 수정이 표에는 반영되지 않았다. (c) CLAUDE.md L28 은 "클래스마다 따로 실행" 만 적고, 클래스 단위 skip 검사의 교착(§4.2 L199)과 메서드 단위 필터는 언급하지 않는다. CLAUDE.md 가 매 세션 로드되는 규칙이므로 한 줄을 추가할 만하다. (d) L315 의 matcher 값 목록에 `fork` 가 빠졌다. hooks 문서는 `startup, resume, clear, compact, fork` 다. `startup` 으로 한정했으므로 동작에는 영향이 없다.
- [m2] §2.2 H0 L96 "D5 가 (a) 가 아니면 `SIG-2` 를 `items` 에서 뺀다" 에는 후속 조치가 없다. `FIX-F3.depends_on` 에 `"SIG-2"`(json L130-133)가 남는다. 없는 id 에 대한 의존은 공통 제외 조건 "depends_on 미충족" 으로 영구히 걸린다. 그래서 FIX-F3 → DOC-1 이 계속 "대기" 로 남는다. 추천안(a)에서는 생기지 않는 문제다. 수정: "… 빼고 `FIX-F3.depends_on` 에서도 지운다" 를 덧붙인다.
- [m3] 병렬 브랜치에서 `claude-progress.md` 머지 충돌 [실행 P3]. main 에서 갈라진 `ai/FIX-F2`·`ai/FIX-R5` 가 각자 progress 끝에 줄을 추가했다. 첫 머지는 성공하고, 두 번째 머지는 `CONFLICT … docs/ai/claude-progress.md` 다. `feature_list.json` 은 자동 병합된다. 브랜치 모델상 독립 항목은 PR 이 머지되기를 기다리지 않고 병렬로 진행되므로, H2 에서 거의 매번 이 충돌을 해결해야 한다. 수정: `.gitattributes` 에 `docs/ai/claude-progress.md merge=union` 을 추가하거나, progress 를 항목별 파일로 나눈다.
- [m4] hook 잔여 (Round 3 M3·m3 일부). (a) 사람 전용 필드가 바뀌었을 때의 차단 메시지(L362)가 여전히 "되돌릴 것" 이다 [실행 P2: 세션 중 pull 하면 `exit=2 … 되돌릴 것`]. 세션 중 pull 금지(L278)로 위험은 줄었지만, 에이전트가 사람의 승인을 되돌리도록 유도할 수 있다. 문구를 "세션 중 pull 했다면 `git reset` 대신 다음 세션에서 재개" 처럼 원인별로 나눈다. (b) `depends_on` 이 충족되지 않은 항목을 passes=true 로 바꾸는 것(X8)은 여전히 통과한다. (c) `session_id` 가 없으면 `"unknown"` 으로 대체되므로, 같은 sha 에서 시작한 두 세션을 구분하지 못한다. 셋 다 협조적 에이전트를 전제로 하면 위험이 낮다.
- [m5] 두 hook 모두 첫 줄이 `[ "$CLAUDE_CODE_REMOTE" = "true" ] || exit 0` 이다(L323·L337). hooks 문서에 따르면 이 변수는 "not set in the local CLI" 다. 그래서 **로컬에서 루프를 돌리면 게이트가 아무것도 하지 않는다.** 그런데 §4.6 본문은 이 점을 말하지 않는다. "클라우드 세션 전용" 이라고 쓰거나 조건을 없앤다. 덧붙여, 클라우드 세션을 새 컨테이너에서 resume 하면 `.git` 의 base 파일이 없을 수 있다(estimated, 미검증). `startup` matcher 에서는 base 가 다시 기록되지 않으므로 Stop 이 해소할 수 없는 조건으로 계속 막는다. 이를 피하려면 matcher 를 `startup|resume` 으로 넓히고, resume 일 때는 "파일이 없을 때만 기록" 한다.
- [m6] ARCH-A2 verify(json L311)는 `--tests '*ModuleArchitectureTest.r2_*'` 로 메서드 이름 접두사에 묶여 있다. 그런데 acceptance(L314)는 R2 를 A′ 규칙 (a)(b) 로 "교체" 하라고만 하고 메서드 이름 규칙은 정하지 않는다. 계획 단계에서 `r2a_…` 같은 이름을 고르면 `No tests found` 로 verify 가 영구히 실패한다. 에이전트는 verify 를 고칠 수 없다. acceptance 에 "메서드 이름은 `r2_` 로 시작" 을 적는다.
- [m7] TEST-1 verify L19 의 `grep -rln '"FIX-F1 '` 는 주석·문자열에서도 참이 된다(Round 3 m8 미반영). 다음 항목의 verify 가 해제를 확인하므로 실질 영향은 작다.
- [m8] SIG-1 은 `requires: []` 이고, verify 는 태그·태스크가 있는지만 본다. 그래서 Docker 가 없는 클라우드 세션에서는 `TestcontainersConfig` 가 실제로 PostgreSQL 을 띄우는지 확인하지 않은 채 passes=true 가 된다. `integrationTest` 가 동작하는지는 FIX-F4 에서 처음 확인된다. gate=plan·H2 가 있으므로 경미로 둔다. acceptance 에 "Docker 가 있는 환경에서 `integrationTest` 를 1회 실행해 결과를 기록" 을 추가하면 된다.

## 이전 라운드 지적 해소 여부

| Round 3 | 상태 | 확인 |
|---|---|---|
| **C1 클래스 단위 skip 검사 교착** | **해소** | [실행] HEAD 에서는 r5·r6·r7·r1·r3·r2 의 메서드 필터가 모두 `tests="1" skipped="1"` → rc=1 이다. 단독 수정 상태에서는 다음과 같다. FIX-F1 만 고치면 FIX-F1 의 4개 명령이 모두 rc=0 이다(R5·R6 은 꺼진 채 `ConventionArchitectureTest.r7_*` → `tests="1" skipped="0"`). FIX-R5 만 고치면 FIX-R5 rc=0 이다(R6·R7 은 꺼진 채). FIX-F1+ARCH-A1 이면 ARCH-A1 의 4개 명령이 rc=0 이다(R2 는 꺼진 채). R6 만 고치면 `r6_*` 가 `skipped="0"` 이다(R5 는 꺼진 채). 음성 대조로 R7 의 `@Disabled` 만 지우고 코드를 고치지 않으면 `BUILD FAILED` 다. 항목 간 교착은 없다. 이웃 항목(FIX-F2·R5·F4·B3)은 각 상태에서 계속 rc=1 이므로, 한 항목을 고쳐 다른 항목이 거짓으로 통과하는 일도 없다 |
| M1 base 재작성·compact·같은 sha | **해소** | matcher `startup` 과 settings.json 예시(L308-315), base 에 session_id 포함, `git rev-parse --git-dir`. [실행 #19] 같은 base sha 에서 다른 세션의 기록만 있으면 → `exit=2`. base 직접 재작성은 L317 에 "남는 위험 — H2 대조" 로 명시돼 있다(표 N2 문구는 m1) |
| M2 반려 항목이 루프를 붙잡음 | **해소 (조건부)** | `Status: revised` 첫 줄 + 사람이 재반려 시 삭제(L97·L273). 규칙 자체에는 교착이 없다. 단 브랜치 규칙과 함께 쓰면 도달하지 못한다 (M1) |
| M3 브랜치·동기화 모델 | **부분 해소** | SessionStart 에서 `git pull --ff-only` 를 base 기록 전에 실행한다(L326). [실행 P1] origin 에 사람이 남긴 `plan_approved` 를 받은 뒤 base 를 잡아 `exit=0`. `ai/<id>` 규칙과 세션 중 pull 금지가 생겼다. 그러나 계획·변경 요청 경로가 정의되지 않았다 (M1). 차단 메시지 구분은 미반영 (m4) |
| M4 ARCH-B2 DB 의존 | **해소** | json L241-245 `depends_on` 에 SIG-1, verify 가 `SPRING_DATASOURCE_URL=…localhost:1/none` [실행: HEAD 에서 `BUILD FAILED`] |
| m1 SIG-1 verify 우회 | 해소 | `@Tag("integration")` grep + `tasks --all \| grep '^integrationTest'` (json L98-99, HEAD 에서 rc=1) |
| m2 feature_list 삭제 | 해소 | [실행 #15] `exit=2 feature_list.json 이 삭제됨` |
| m3 awaiting_human·기타 필드·의존 미충족 | 대부분 해소 | [실행 #16·#17] `exit=2`. 의존 미충족 passes 는 여전히 미검사 (m4) |
| m4 pending 안 사람 판정 항목 | 해소 | [실행 #18] `exit=2 DOC-2 는 사람 판정 항목` |
| m5 worktree `.git` 파일 | 해소 | `$(git rev-parse --git-dir)` (L327·L344), "해소 불가 조건에서 무한 반복" 을 L317 에 명시 |
| m6 변경 요청 시 awaiting_human 복귀 | 해소 | L98·L287 (기록 위치는 M1) |
| m7 SIG-2 후 spotless | 해소 | init.sh L244-248 이 `SIG-2.passes` 로 분기 |
| m8 TEST-1 grep | 미반영 | m7 |

## 검증한 주장 (문제 없음 확인)

- **verify 원칙 "지금 실패"** [실행]: 기계로 판정하는 13개 항목(TEST-1·FIX-F1·F1T·F2·SIG-1·SIG-2·F3·R5·F4·ARCH-A1·B2·B3·A2)이 HEAD 에서 모두 항목 rc=1 이다. FIX-F1·F2 의 `! grep '"FIX-F1 '` 류는 HEAD 에서 rc=0 이지만 다른 명령이 실패한다. TEST-1 완료 상태에서는 TEST-1 이 rc=0 이 되고, 그 뒤 FIX-F1·F2 의 사유 grep 이 rc=1 이 된다. 순서가 의도대로 동작한다.
- **§4.2 L199 "필터된 XML 에는 그 메서드만 남는다"** [실행]: `--tests '*ConventionArchitectureTest.r7_*'` 의 결과 XML 은 `tests="1"` 이다. `@DisplayName` 이 있어도 메서드 이름 필터가 동작한다.
- **§4.6 표 20행** [실행]: 모두 표와 같다. 차단되는 경우: 기록 없음, `stop_hook_active`, 다른 id, compact JSON, 이전 세션, progress 위조, 깨진 JSON, DOC-1 verify 변경, 사람 판정 passes, 항목 삭제, pending passes, `"true"` 문자열, 파일 삭제, 일반 항목에 awaiting_human, title 변경, pending DOC-2, 같은 sha 의 다른 세션. 통과하는 경우: true→false, 기록 후 커밋, 사람 판정 항목에 awaiting_human. 두 스크립트 모두 `bash -n` 을 통과한다.
- **§4.4 선택 규칙의 나머지**: 공통 제외 조건(passes·blocked·awaiting_human·requires·depends_on)과 규칙 1→2→3→4 의 순서 자체에는 교착이 없다. 계획 파일이 있고 승인 전이면 규칙 4 "대기" 로 간다. 재반려 시 사람이 `Status: revised` 를 지우면 규칙 1 로 돌아간다. Docker 가 없으면 FIX-F4 → DOC-1 은 "대기" 이고 실패로 세지 않는다(§4.5). 단 브랜치 규칙과의 결합은 M1, SIG-2 를 뺄 때의 처리는 m2.
- **hooks 문서 인용** [curl]: 다음 원문이 §4.6 L304·L315 와 일치한다. "8-consecutive-continuation cap … resets each time Claude calls a tool", SessionStart matcher `startup, resume, clear, compact, fork`(fork 누락은 m1), SessionStart 입력의 `session_id`.
- **Round 3 C1 근거의 반영**: §4.2 함정 표 L199 에 교착 사례와 대응이 들어갔다. json 의 FIX-F1·R5·F4·ARCH-A1·B3·A2 가 모두 메서드 단위 필터를 쓰고, §4.2 의 스키마 예시(L178-183)도 json 과 같다.
