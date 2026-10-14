# AI 컨텍스트·문서 구조 설계

> 상태: **적용 완료** — 결정 E1~E4 는 [../decisions/0001-context-structure.md](../decisions/0001-context-structure.md). 기록: [../plans/completed/context-structure.md](../plans/completed/context-structure.md)
> 조사일 2026-10-13. 근거는 각 행의 출처 번호 → §9. 모든 출처는 원문을 직접 열어 확인했다. 예외는 §9 에 "미확인" 으로 표시했다.
> 목적: 이 저장소에서 Claude Code 같은 코딩 에이전트가 **필요한 것만, 필요할 때** 읽도록 문서와 설정을 배치한다.

---

## 1. 한 줄 요약

**루트 지침 파일은 짧은 지도(목차)로 두고, 오래 가는 지식은 `docs/` 에 주제별로, 경로별 규칙과 반복 절차는 `.claude/rules`·`.claude/skills` 로, 반드시 지킬 것은 hook·권한으로, 쓰고 버릴 것은 저장소 밖(세션 scratchpad)에 둔다.**

---

## 2. 조사 결론 — 세 회사·도구가 공통으로 권하는 것

| # | 원칙 | Anthropic | OpenAI | 그 밖 (Google·GitHub·Cursor·Kiro) |
|---|---|---|---|---|
| P1 | 루트 지침 파일은 **짧게** — 매 세션 필요한 것만 | CLAUDE.md 파일당 200줄 미만. "이 줄을 지우면 실수하나?" 로 거른다 [A1][A2] | AGENTS.md 약 100줄, "백과사전이 아니라 지도" [O1][O2] | Copilot "2페이지 이내", Cursor 규칙당 500줄 이내 [X2][X3] |
| P2 | 상세 지식은 **별도 문서**, 루트 파일은 위치만 알려 준다 | 상세 API 문서는 "link to docs instead" [A2] | `docs/` 가 system of record, 점진적 공개 [O2] | Cursor `@file` 참조, 복사 금지 [X3] |
| P3 | **경로·상황별 규칙**은 조건부로 로드 | `.claude/rules/*.md` + `paths:` glob [A1] | 하위 디렉터리 AGENTS.md, 가까운 것이 우선 [O3] | Copilot `applyTo`, Cursor `globs`, Kiro `fileMatch` [X2][X3][X5] |
| P4 | **여러 단계 절차**는 규칙이 아니라 skill | "Procedures belong in skills", commands 는 skills 로 통합 [A3][A4] | 저장소 skills `.agents/skills` [O4] | Antigravity "제약은 rule, 절차는 skill" [X1] |
| P5 | **반드시 지킬 것**은 문서가 아니라 기계로 강제 | "Never do this" 는 hook·permission [A3] | 커스텀 린트·구조 테스트, 에러 메시지에 해결법 [O2] | Cursor "스타일 가이드 대신 linter" [X3] |
| P6 | **긴 작업 계획**은 저장소에 커밋되는 살아 있는 문서 | feature list JSON + progress 파일 + git [A5] | `docs/exec-plans/active·completed/`, ExecPlan 형식 [O2][O5] | Spec Kit `specs/NNN/`, Kiro `.kiro/specs/` (커밋) [X4][X5] |
| P7 | **쓰고 버릴 파일**은 저장소 밖이 기본 | 세션 scratchpad `/tmp/claude-*/…/scratchpad`, 계획은 `~/.claude/plans/` [A6] | 공식 지침 없음 (작은 계획은 "ephemeral") [O2] | Gemini 계획 `~/.gemini/tmp/…`, Cursor 계획 홈 디렉터리 [X1][X3] |
| P8 | 개인용 설정은 **gitignore** | `CLAUDE.local.md`, `.claude/settings.local.json` [A1][A6] | `~/.codex/AGENTS.md` (저장소 밖) [O3] | VS Code 도 `CLAUDE.local.md` 비커밋 [X2] |
| P9 | 문서는 **낡는다** — 최신성 관리 장치가 필요 | `/doctor prompt-audit`, 세션 끝에 CLAUDE.md 개선 [A1][A7] | doc-gardening 에이전트, 문서 CI 검사 [O2] | Jules·agents.md "최신 상태로 유지" [X1][X6] |
| P10 | 여러 도구를 쓰면 **AGENTS.md 하나를 원본**으로 | CLAUDE.md 첫 줄 `@AGENTS.md` + Claude 전용 내용 [A1] | AGENTS.md 표준 (Linux Foundation AAIF) [O1][X6] | Copilot·Cursor·Gemini·Kiro 모두 AGENTS.md 읽음 [X1~X5] |

### 꼭 알아야 할 함정 3개

1. **`@import` 는 컨텍스트를 줄이지 않는다.** CLAUDE.md 의 `@docs/x.md` 는 세션 시작 때 **전부** 로드된다 [A1]. 지도 역할을 하려면 경로를 백틱으로 감싸 쓰고(`` `docs/ai/x.md` ``), 필요할 때 읽게 한다. 실제로 컨텍스트를 줄이는 것은 `.claude/rules`(paths), skill, 하위 디렉터리 CLAUDE.md 다.
2. **`CLAUDE.local.md` 를 만들면 AGENTS.md 를 안 읽는다.** Claude Code 기본 모드는 CLAUDE.md 계열이 하나라도 있으면 AGENTS.md 를 건너뛴다 [A1]. AGENTS.md 를 원본으로 쓸 거면 CLAUDE.md 에서 `@AGENTS.md` 로 명시 import 한다.
3. **코드에서 알 수 있는 것은 쓰지 않는다.** 파일별 설명, 디렉터리 나열, 의존성 목록은 빼라는 것이 현행 권장이다 [A1][A2]. 2025년 글은 디렉터리 구조를 넣으라고 했지만 바뀌었다 (§9 변경 메모).

---

## 3. 현재 구조 진단

| # | 현재 | 문제 | 근거 원칙 |
|---|---|---|---|
| C1 | `CLAUDE.md` 75줄. 빌드 명령 + 패키지 규칙 + 결함 규칙 + 예외·로깅 + 작업 방식 + 학습 노트 절차가 한 파일 | 길이는 기준 안이지만, **테스트·영속성·외부 호출 규칙**은 그 파일을 만질 때만 필요하다. 학습 노트 작성은 절차라 skill 대상 | P1·P3·P4 |
| C2 | `docs/ai/` 에 **오래 가는 지식**(current-state, 제안서)과 **한 번 쓴 작업 산출물**(TASK.md, checklist.json, reviews/ 21개, pr-body.md, wip-snapshot.patch)이 섞여 있다 | 에이전트가 `docs/ai` 를 탐색할 때 낡은 작업 기록을 현행 규칙으로 읽을 위험 | P2·P9 |
| C3 | `.claude/hooks/check-done.sh` 가 Stop hook 으로 **계속 켜져 있다** | 이미 끝난 TASK.md 용 게이트. 이 문서 세트가 끝났는데도 매 종료마다 검사 | P5 (대상이 틀림) |
| C4 | 쓰고 버릴 파일 위치가 정해져 있지 않다. `.gitignore` 에는 옛 `/docs/private` 만 있다 | 에이전트가 저장소 루트·`docs/` 에 임시 파일을 만들 수 있다 (이 세션에서 zip 을 루트에 만들었다가 지운 사례) | P7 |
| C5 | 장기 작업 상태 파일이 `feature_list.example.json` 제안뿐, 실제 `feature_list.json`·progress 가 없다 | 기능 개발을 시작하면 바로 필요 | P6 |
| C6 | 결정된 사항을 남길 곳이 없다. `decisions-needed.md` 는 "결정 전" 목록 | 결정 후 근거가 흩어진다 | P2·P9 |
| C7 | 루트 `README.md` 가 폐기된 헥사고날 구조를 설명 | 사람과 에이전트 모두 잘못 읽는다 (CLAUDE.md 가 경고로 막는 중) | P9 |

---

## 4. 제안 구조

```
CLAUDE.md                     # 지도. 명령·함정·금지·문서 위치 (목표 100줄 이하)          [커밋]
AGENTS.md                     # (결정 E1) 다른 도구도 쓸 때만. 공통 내용의 원본            [커밋]
.gitignore                    # + .scratch/  CLAUDE.local.md  .claude/settings.local.json  .claude/worktrees/
.claude/
├── settings.json             # 팀 권한(allow/deny) + hooks                                [커밋]
├── settings.local.json       # 개인 권한                                                  [무시]
├── rules/                    # 경로별 규칙 — 해당 파일을 만질 때만 로드                  [커밋]
│   ├── testing.md            #   paths: src/test/**
│   ├── persistence.md        #   paths: **/repository/**, **/domain/**, **/*Entity.java
│   ├── external-client.md    #   paths: **/infra/**, **/client/**
│   └── docs.md               #   paths: docs/**  (docs/public 수정 전 확인, 학습 노트 위치)
├── skills/                   # 여러 단계 절차 — 부를 때만 로드                          [커밋]
│   ├── feature-item/SKILL.md #   feature_list 항목 1개 수행 (계획→실패 테스트→구현→verify→기록)
│   ├── new-error-type/SKILL.md   # ETmiErrorType + 중첩 예외 + messages 3곳
│   └── study-note/SKILL.md   #   학습 노트 작성 (지금 CLAUDE.md 의 절을 옮김)
├── agents/
│   └── spring-reviewer.md    # 독립 리뷰어 (이번 리뷰 루프의 "까다로운 시니어" 역할)    [커밋]
└── hooks/
    ├── session-base.sh       # SessionStart: base 커밋 기록 (ai-workflow §4.6)           [커밋]
    └── check-feature.sh      # Stop: 이번 세션 항목의 verify 기록 확인 (ai-workflow §4.6) [커밋]
docs/
├── public/                   # 팀 공식 설계·규칙 (변경 없음, 수정은 사람 확인)            [커밋]
├── ai/                       # 에이전트용 지식 — system of record                         [커밋]
│   ├── README.md             #   색인: 문서별 한 줄 설명 + 상태(현행/제안/보관)
│   ├── context/              #   오래 가는 사실
│   │   ├── current-state.md  #     (이동) 코드 현황·결함·ArchUnit 위반
│   │   └── research.md       #     (이동) 외부 근거
│   ├── proposals/            #   결정 전 제안서
│   │   ├── architecture-proposal.md · conventions-proposal.md · ai-workflow.md · context-structure-design.md (이 문서)
│   │   ├── examples/         #     구조 제안 예시 코드 (빌드 대상 아님)
│   │   └── decisions-needed.md
│   ├── decisions/            #   결정된 것 (ADR, 짧게). NNNN-제목.md
│   ├── plans/
│   │   ├── active/           #   진행 중 계획 (ExecPlan 형식, §5.3)
│   │   └── completed/        #   끝난 계획 (회고 포함)
│   ├── feature_list.json     #   루프 상태 (JSON — passes 만 에이전트가 수정)
│   ├── progress.md           #   세션별 진행 메모 (자유 서술, 최신이 위)
│   ├── blockers.md           #   환경 문제와 우회
│   ├── reviews/              #   독립 리뷰 기록 (<id>-round-<N>.md, 필요할 때 생성)
│   └── archive/
│       └── 2026-10-ai-dev-prep/   # TASK.md, checklist.json, reviews/, pr-body.md, wip-snapshot.patch, examples/
└── study/                    # 사용자 학습 노트 (변경 없음)                              [커밋]
.scratch/                     # 쓰고 버리되 세션 밖까지 남겨야 하는 것 (§5.4)              [무시]
```

### 4.1 파일마다 무엇을 넣고 무엇을 빼나

| 위치 | 넣는 것 | 넣지 않는 것 |
|---|---|---|
| `CLAUDE.md` | 빌드·테스트 명령, 환경 함정(DB 필요, gradlew 권한, Lombok 없음), **이 저장소에서 실제로 난 결함 규칙**(@RequestBody, self-invocation), 금지 사항, 문서 위치 지도 | 패키지 나열, 파일별 설명, 상세 예외 코드 예시, 절차 |
| `.claude/rules/*.md` | 그 경로에만 해당하는 규칙 (예: 테스트는 Lombok 없이, `@MockitoBean`; 리포지토리 쿼리 메서드는 트랜잭션 없음) | 전역 규칙 (CLAUDE.md 로) |
| `.claude/skills/*/SKILL.md` | 순서가 있는 절차, 체크리스트 (500줄 미만, 상세는 같은 폴더의 참조 파일) | 항상 지켜야 하는 제약 |
| `.claude/settings.json` | 금지를 기계로: `docs/public/**` Edit `ask`, `src/main/**/generated` Read `deny`(생기면), 위험 명령 deny | 설명 문장 |
| `docs/ai/context/` | 코드에서 바로 안 보이는 사실: 결함 목록, 측정 결과, 외부 근거 | 결정 전 의견 |
| `docs/ai/decisions/` | 결정 1건 = 파일 1개: 맥락·결정·결과 (20~40줄) | 선택지 비교 전부 (제안서에 남김) |

---

## 5. 운영 규칙

### 5.1 CLAUDE.md 목차 (제안)

| 절 | 줄 수 목표 | 비고 |
|---|---|---|
| 프로젝트 한 줄 + 스택 | 3 | |
| 빌드·테스트 명령 | 10 | 현행 유지 |
| 환경 함정 | 8 | DB, gradlew, `--tests` OR, `.gitignore out/` |
| 반드시 지킬 것 (실제 결함 규칙) | 15 | 현행에서 경로 무관한 것만 |
| 금지·확인 필요 | 6 | 헥사고날 금지, docs/public·의존성·API 변경 전 확인 |
| 문서 지도 | 12 | 백틱 경로로만 (import 금지): context / proposals / decisions / plans / study |
| 작업 방식 | 8 | 항목 1개 = PR 1개, 실패 테스트 먼저, 성공 주장 시 명령·결과, 임시 파일 위치 |
| 합계 | **약 60~70** | 테스트·영속성·외부 호출 상세는 `.claude/rules` 로 이동 |

### 5.2 지식이 흘러가는 방향

```
대화·작업 중 발견 ──▶ progress.md (세션 메모)
                         │ 두 번 이상 쓰이면
                         ▼
        경로 한정? ──예──▶ .claude/rules/<주제>.md
            │아니오
            ▼
        절차인가? ──예──▶ .claude/skills/<이름>/
            │아니오
            ▼
        매 세션 필요? ──예──▶ CLAUDE.md (한 줄)
            │아니오
            ▼
        docs/ai/context/ 또는 decisions/
```

같은 실수를 두 번 하면 규칙으로 승격한다 [O4]. 규칙으로도 안 지켜지면 hook·권한·ArchUnit 으로 승격한다 [A3][O2] — 이 저장소의 R1~R7 ArchUnit 이 이미 그 단계다.

### 5.3 계획 문서 (ExecPlan 형식, 간소화)

`docs/ai/plans/active/<feature-id>.md` — 기능 1개 또는 리팩터링 1개당 1파일. 끝나면 `completed/` 로 옮긴다.

| 절 | 내용 |
|---|---|
| 목적 | 무엇이 동작하게 되는가 (사용자 관점 한두 줄) |
| 맥락 | 관련 파일·클래스, 참고 문서 경로 |
| 작업 순서 | 단계별, 각 단계의 검증 명령 |
| 진행 | 체크박스 + 날짜 (이 절에서만 체크박스) |
| 발견·결정 로그 | 예상과 달랐던 것, 고른 이유 |
| 회고 | 끝난 뒤 3줄 |

작은 변경(diff 를 한 문장으로 설명할 수 있는 것)은 계획 파일 없이 진행한다 [A2][O2].

### 5.4 쓰고 버리는 파일 — 3단계

| 단계 | 위치 | 언제 | 커밋 |
|---|---|---|---|
| 1 (기본) | **세션 scratchpad** (저장소 밖, Claude Code 가 제공) | 중간 결과, 보조 스크립트, 초안 | 아니오 — 세션과 함께 사라짐 [A6] |
| 2 | **`.scratch/`** (저장소 안, gitignore) | 로컬에서 여러 세션에 걸쳐 남겨야 하는 임시 파일 (실험 출력, 비교용 덤프) | 아니오 |
| 3 | `docs/ai/plans/`, `progress.md` | 다음 세션·다른 사람이 이어받아야 하는 것 | 예 |

- 이름은 `.scratch/` 를 추천한다. 점으로 시작해 IDE·검색에서 덜 보이고, "임시" 라는 뜻이 분명하다. `private/` 는 "비공개 지식" 으로, `thinking/` 은 "사고 기록" 으로 읽혀 커밋 대상처럼 보일 수 있다.
- **클라우드 세션 주의**: 컨테이너가 회수되면 1·2단계는 모두 사라진다. 남길 것은 3단계로 옮기거나 파일로 받는다.
- 에이전트 규칙(CLAUDE.md 한 줄): "임시 파일은 scratchpad, 세션 밖까지 필요하면 `.scratch/`. 저장소 루트·`docs/` 에 임시 파일을 만들지 않는다."

### 5.5 최신성 유지

| 장치 | 내용 | 시점 |
|---|---|---|
| 문서 링크 검사 | CLAUDE.md·`docs/ai/README.md` 에 적힌 경로가 실제로 있는지 확인하는 짧은 스크립트를 CI 또는 Stop hook 에 연결 | 적용 3단계 |
| 세션 종료 정리 | 항목 완료 시 progress.md 갱신 + "CLAUDE.md·rules 에 반영할 것" 1줄 검토 [A7] | feature-item skill 의 마지막 단계 |
| 주기 점검 | 기능 5개마다 `/doctor prompt-audit` 으로 낡은·충돌 지시 확인 [A1] | 수동 |
| 상태 표시 | `docs/ai/README.md` 에 문서마다 상태(현행/제안/보관) 표시 | 이동 시 |

---

## 6. 기존 파일 이동표

| 현재 | 이동 후 | 이유 |
|---|---|---|
| `docs/ai/context/current-state.md` | `docs/ai/context/current-state.md` | 오래 가는 사실 |
| `docs/ai/context/research.md` | `docs/ai/context/research.md` | 〃 |
| `docs/ai/proposals/architecture-proposal.md` 외 제안서 3개 (이름 유지) | `docs/ai/proposals/` | 결정 전. 본문에 파일 이름으로 언급된 곳이 많아 이름은 바꾸지 않는다 |
| `docs/ai/proposals/decisions-needed.md` | `docs/ai/proposals/decisions-needed.md` | 〃 |
| `docs/ai/feature_list.example.json` | `docs/ai/feature_list.json` (D12 결정 후 복사) | 실사용 |
| `docs/ai/archive/2026-10-ai-dev-prep/TASK.md`, `checklist.json`, `pr-body.md`, `wip-snapshot.patch`, `reviews/` | `docs/ai/archive/2026-10-ai-dev-prep/` | 끝난 작업 기록 |
| `docs/ai/proposals/examples/` | `docs/ai/proposals/examples/` | 구조 제안의 부속. D1 결정 후 보관 |
| `.claude/hooks/check-done.sh` + settings 의 Stop hook | 제거 → `check-feature.sh` 로 교체 | 끝난 작업용 게이트 (C3) |
| `CLAUDE.md` "학습 노트" 절 | `.claude/skills/study-note/SKILL.md` | 절차 |
| `CLAUDE.md` 테스트·영속성·외부 호출 상세 | `.claude/rules/*.md` | 경로 한정 |
| `.gitignore` `/docs/private` | `.scratch/` 등으로 교체 | §5.4 |

- 이동 시 문서 안 상대 링크를 함께 고친다. `git mv` 로 이력을 유지한다.
- 이동은 문서만 바꾸므로 `src/main` 영향 없음.

---

## 7. 결정 필요 사항

| # | 질문 | 추천 | 이유 |
|---|---|---|---|
| E1 | AGENTS.md 를 둘까? | **지금은 두지 않는다.** Codex·Copilot·Cursor 를 함께 쓰게 되면 그때 AGENTS.md 를 원본으로 만들고 CLAUDE.md 첫 줄에 `@AGENTS.md` | 도구가 Claude Code 하나면 파일만 늘어난다. 나중에 옮기는 비용이 작다 [A1][X6] |
| E2 | 임시 디렉터리 이름 | **`.scratch/`** (gitignore) | §5.4 |
| E3 | `docs/ai` 재배치 (§6) 를 지금 할까? | **예, 기능 개발 전에 한 PR 로** | 기능 PR 마다 문서가 섞이면 이동이 어려워진다. 링크 수정이 한 번에 끝난다 |
| E4 | `.claude/rules` 분리 범위 | **testing·persistence·external-client 3개부터** | 이 저장소의 실제 결함(F1·F4·테스트 함정)이 이 세 영역에 있다. docs.md 는 필요해지면 |

이 문서의 결정은 `decisions-needed.md` 의 D12(AI 루프 운영)와 함께 정하면 된다.

---

## 8. 적용 순서 (각 단계 = PR 1개)

| 단계 | 내용 | 검증 |
|---|---|---|
| 1 | `docs/ai` 재배치 + `docs/ai/README.md` 색인 + 링크 수정 + `.gitignore` 정리 + `check-done.sh` 제거 | `grep` 으로 깨진 상대 링크 0건, `bash ./gradlew build` 통과 |
| 2 | `CLAUDE.md` 축약(§5.1) + `.claude/rules` 3개 + `study-note`·`new-error-type` skill | 새 세션에서 `/memory` 로 로드 목록 확인, 테스트 파일을 열었을 때만 testing.md 가 로드되는지 확인 |
| 3 | `feature_list.json` + `progress.md` + `feature-item` skill + `session-base.sh`·`check-feature.sh` hook + 문서 링크 검사 | ai-workflow §4.6 의 hook 시나리오 재실행 |
| 4 | `spring-reviewer` 서브에이전트 + 첫 기능 `TEST-1` 을 이 구조로 진행 | TEST-1 verify 명령 통과, plans/completed 에 회고 |

적용 결과 (2026-10-14): 1~4단계 모두 반영. 옛 hook 제거·새 hook 등록(`.claude/settings.json`)과 체크리스트·리뷰 보관 이동까지 끝났다 — [../plans/completed/context-structure.md](../plans/completed/context-structure.md).

---

## 9. 출처

모두 2026-10-13 에 원문을 직접 열어 확인했다. 단 [O2] 는 openai.com 이 직접 접근을 막아 리더 서비스(r.jina.ai)로 원문을 받았다.

| 번호 | 출처 |
|---|---|
| A1 | Claude Code — Memory (CLAUDE.md 계층, 200줄, `@import`, `.claude/rules`, AGENTS.md 지원, 자동 메모리) https://code.claude.com/docs/en/memory |
| A2 | Claude Code — Best practices (포함/제외 표, plan 생략 기준, SPEC.md·PLAN.md) https://code.claude.com/docs/en/best-practices |
| A3 | Steering Claude Code: skills, hooks, rules, subagents (2026-06-18) https://claude.com/blog/steering-claude-code-skills-hooks-rules-subagents-and-more |
| A4 | Claude Code — Skills ("Custom commands have been merged into skills") https://code.claude.com/docs/en/skills · Agent Skills best practices https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices |
| A5 | Effective harnesses for long-running agents (2025-11-26) https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents · Effective context engineering for AI agents (2025-09-29) https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents |
| A6 | Claude Code — `.claude` directory (세션 scratchpad) https://code.claude.com/docs/en/claude-directory · Settings (`plansDirectory`, `cleanupPeriodDays`) https://code.claude.com/docs/en/settings-reference · Worktrees (`.claude/worktrees/` gitignore) https://code.claude.com/docs/en/worktrees |
| A7 | How Anthropic teams use Claude Code (세션 끝 CLAUDE.md 개선) https://claude.com/blog/how-anthropic-teams-use-claude-code |
| O1 | AGENTS.md 표준 https://agents.md/ |
| O2 | Harness engineering: leveraging Codex in an agent-first world (2026-02-11) https://openai.com/index/harness-engineering/ |
| O3 | Codex — AGENTS.md (탐색 순서, override, 32 KiB) https://developers.openai.com/codex/guides/agents-md |
| O4 | Codex — Best practices ("short, accurate AGENTS.md", 같은 실수 두 번이면 갱신) https://developers.openai.com/codex/learn/best-practices |
| O5 | Using PLANS.md for multi-hour problem solving (2025-10-07) https://cookbook.openai.com/articles/codex_exec_plans |
| X1 | Gemini CLI GEMINI.md https://geminicli.com/docs/cli/gemini-md/ · Plan mode https://geminicli.com/docs/cli/plan-mode/ · Antigravity rules https://antigravity.google/docs/rules · Jules https://jules.google/docs |
| X2 | GitHub Copilot repository instructions https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions · VS Code custom instructions https://code.visualstudio.com/docs/copilot/customization/custom-instructions |
| X3 | Cursor rules https://cursor.com/docs/context/rules · Plan mode https://cursor.com/docs/agent/plan-mode |
| X4 | GitHub Spec Kit https://github.github.io/spec-kit/quickstart.html |
| X5 | Kiro specs https://kiro.dev/docs/specs/ · steering https://kiro.dev/docs/steering/ |
| X6 | Linux Foundation — Agentic AI Foundation 설립 (2025-12-09, AGENTS.md 이관) https://www.linuxfoundation.org/press/linux-foundation-announces-the-formation-of-the-agentic-ai-foundation |

**변경 메모** (오래된 글과 현행 문서가 다른 곳):
- Anthropic 의 2025-04 블로그 "Claude Code best practices" 는 현재 docs 로 리다이렉트된다. 구판의 "checklists and scratchpads" 절은 현행판에 없고(구판 원문 미확인), SPEC.md·PLAN.md·검증 우선 방식으로 바뀌었다.
- `.claude/commands/` 는 skills 로 통합됐다. 새로 만들 때는 skill 을 쓴다 [A4].
- 2025-11 글은 CLAUDE.md 에 디렉터리 구조를 넣으라고 했지만, 현행 문서와 `/doctor` 는 코드에서 알 수 있는 구조 설명을 빼라고 한다 [A1][A2].
