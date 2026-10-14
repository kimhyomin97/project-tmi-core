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
- [ ] 단계 2
- [ ] 단계 3
- [ ] 단계 4

## 사람이 할 일 (에이전트 권한 밖)

1. **옛 Stop hook 제거** — `.claude/hooks/check-done.sh` 는 끝난 TASK.md 용이다. 지우고 `.claude/settings.json` 의 `hooks.Stop` 을 비운다.
   ```bash
   git rm .claude/hooks/check-done.sh
   printf '{}\n' > .claude/settings.json
   ```
2. 1번 후 `docs/ai/checklist.json` 과 `docs/ai/reviews/` 를 `docs/ai/archive/2026-10-ai-dev-prep/` 로 옮긴다 (지금은 옛 hook 이 이 경로를 읽어서 남겨 뒀다). 에이전트에게 "체크리스트·리뷰 보관으로 옮겨" 라고 하면 된다.

## 발견·결정 로그

- 2026-10-14 파일 이름은 유지하고 디렉터리만 옮겼다. 본문에 "current-state.md §3" 처럼 이름으로 언급된 곳이 많다.
- 2026-10-14 링크 검사는 마크다운 링크만 본다. 본문 속 경로까지 검사하면 "앞으로 만들 파일" 언급이 모두 오류로 잡힌다.
- 2026-10-14 `docs/public` 의 깨진 링크 3건(`./06_배포·운영.md` → 실제 파일은 `07_배포·운영.md`)은 팀 문서라 고치지 않고 검사 기본 범위에서 뺐다. `--all` 로 확인 가능.
- 2026-10-14 `.claude/settings.json`·hook 수정은 권한 검사에서 거부됐다. 옛 hook 이 `docs/ai/checklist.json`·`docs/ai/reviews/` 를 읽으므로 둘은 원래 위치에 남겼다.
