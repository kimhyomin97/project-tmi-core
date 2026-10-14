# 계획: AI 컨텍스트·문서 구조 적용

## 목적

[../../proposals/context-structure-design.md](../../proposals/context-structure-design.md) 의 구조를 4단계로 적용한다. 결정: [../../decisions/0001-context-structure.md](../../decisions/0001-context-structure.md).

## 맥락

- 대상: `docs/ai/`, `CLAUDE.md`, `.claude/`, `.gitignore`, `scripts/`
- `src/main` 은 건드리지 않는다. 작업 트리의 미커밋 Toss WIP 는 빌드할 때만 `git stash push -- src/main .gitattributes` 로 치운다.
- **에이전트가 `.claude/` 아래 설정·hook 을 바꾸는 것은 세션 권한 검사(Self-Modification)에서 막힌다.** 그 부분은 아래 "사람이 할 일" 로 남긴다.

## 작업 순서

| 단계 | 내용 | 검증 |
|---|---|---|
| 1 | `docs/ai` 재배치, 색인(`docs/ai/README.md`), `decisions/`·`plans/`·`archive/`, `.gitignore`, `scripts/check-doc-links.py` | `python3 scripts/check-doc-links.py` 깨진 링크 0, `bash ./gradlew build` |
| 2 | `CLAUDE.md` 축약 + `.claude/rules` 3개 + skill 2개 | 줄 수, frontmatter 형식, 링크 검사 |
| 3 | `feature_list.json`, `progress.md`, `feature-item` skill, hook 스크립트 | hook 시나리오 실행 |
| 4 | `spring-reviewer` 서브에이전트 + 첫 항목 `TEST-1` | TEST-1 verify 명령 |

## 진행

- [x] 2026-10-14 단계 1: 재배치·색인·링크 검사 스크립트. 링크 검사 0건(보관·팀 문서 제외), `bash ./gradlew build` 성공 (테스트 9: 통과 3, `@Disabled` 6)
- [x] 2026-10-14 단계 2: `CLAUDE.md` 75 → 60줄 (경로별 규칙·절차를 분리), `.claude/rules/` testing·persistence·external-client, skill `study-note`·`new-error-type`. 링크 검사 0건, frontmatter YAML 파싱 확인. **로드 동작(경로가 맞을 때만 rules 로드)은 다음 새 세션에서 `/memory` 로 확인해야 한다** — 이번 세션은 시작할 때 이 파일들이 없었다
- [x] 2026-10-14 단계 3: `docs/ai/feature_list.json`(초안 복사), `docs/ai/progress.md`, `scripts/init.sh`, skill `feature-item`, hook 스크립트 `.claude/hooks/session-base.sh`·`check-feature.sh` (progress 경로만 `progress.md` 로 바꿈). 임시 저장소에서 hook 시나리오 5개 실행: 변경 없음 통과 / 기록 없는 passes=true 차단 / 기록 후 통과 / title 변경 차단 / 문자열 "true" 차단. `bash scripts/init.sh` rc=0 (compile OK, docker 없음, spotless 위반 정보). **hook 은 settings.json 등록 전이라 아직 동작하지 않는다**
- [ ] 단계 4

## 사람이 할 일 (에이전트 권한 밖)

1. **옛 Stop hook 제거** — `.claude/hooks/check-done.sh` 는 끝난 TASK.md 용이다. 지우고 `.claude/settings.json` 의 `hooks.Stop` 을 비운다.
   ```bash
   git rm .claude/hooks/check-done.sh
   printf '{}\n' > .claude/settings.json
   ```
   위 두 줄 대신, 새 hook 등록까지 한 번에 하려면 `.claude/settings.json` 을 이 내용으로 바꾼다 (ai-workflow §4.6):
   ```json
   { "hooks": {
     "SessionStart": [ { "matcher": "startup", "hooks": [ { "type": "command", "command": "bash \"$CLAUDE_PROJECT_DIR\"/.claude/hooks/session-base.sh" } ] } ],
     "Stop":         [ { "hooks": [ { "type": "command", "command": "bash \"$CLAUDE_PROJECT_DIR\"/.claude/hooks/check-feature.sh" } ] } ]
   } }
   ```
   두 hook 은 클라우드 세션(`CLAUDE_CODE_REMOTE=true`)에서만 동작한다. 등록하면 다음 세션부터 `docs/ai/progress.md` 에 `session-base` 줄이 생기고, Stop 시 verify 기록을 검사한다.
2. 1번 후 `docs/ai/checklist.json` 과 `docs/ai/reviews/` 를 `docs/ai/archive/2026-10-ai-dev-prep/` 로 옮긴다 (지금은 옛 hook 이 이 경로를 읽어서 남겨 뒀다). 에이전트에게 "체크리스트·리뷰 보관으로 옮겨" 라고 하면 된다.

## 발견·결정 로그

- 2026-10-14 파일 이름은 유지하고 디렉터리만 옮겼다. 본문에 "current-state.md §3" 처럼 이름으로 언급된 곳이 많다.
- 2026-10-14 링크 검사는 마크다운 링크만 본다. 본문 속 경로까지 검사하면 "앞으로 만들 파일" 언급이 모두 오류로 잡힌다.
- 2026-10-14 `docs/public` 의 깨진 링크 3건(`./06_배포·운영.md` → 실제 파일은 `07_배포·운영.md`)은 팀 문서라 고치지 않고 검사 기본 범위에서 뺐다. `--all` 로 확인 가능.
- 2026-10-14 `.claude/settings.json`·hook 수정은 권한 검사에서 거부됐다. 옛 hook 이 `docs/ai/checklist.json`·`docs/ai/reviews/` 를 읽으므로 둘은 원래 위치에 남겼다.
