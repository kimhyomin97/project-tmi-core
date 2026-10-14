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
- [x] 2026-10-14 단계 4: 서브에이전트 정의 `.claude/agents/spring-reviewer.md`, 첫 항목 `TEST-1` 을 이 구조(feature-item 순서)로 진행 — 실패 재현 확인, 독립 리뷰(주요 3건 반영: 로케일 고정, progress 기록, 범위 메모), verify 4개 rc=0, `passes: true`. 학습 노트 `docs/study/spring/message-source-locale.md`

## 마무리 (2026-10-14)

- [x] 옛 Stop hook(`check-done.sh`) 제거, `.claude/settings.json` 에 `session-base.sh`(SessionStart, matcher `startup`)·`check-feature.sh`(Stop) 등록. 처음에는 직접 수정이 권한 검사에서 거부됐고, 사용자 요청 후 설정 변경 절차(update-config)로 반영했다
- [x] `docs/ai/checklist.json`·`docs/ai/reviews/` → `docs/ai/archive/2026-10-ai-dev-prep/`, 링크 수정 (검사 0건)
- [ ] 다음 새 세션에서 확인: `/memory` 로 rules 경로별 로드, `docs/ai/progress.md` 에 `session-base` 줄이 자동으로 생기는지

## 회고

- 구조는 하루 안에 적용됐고, 첫 항목(TEST-1)에서 독립 리뷰가 실제 결함(로케일에 따른 거짓 통과)을 잡아 루프의 가치를 확인했다.
- 에이전트가 자기 설정(`.claude/settings.json`, hook)을 바꾸는 일은 권한 검사에 걸린다. 처음부터 "사람이 할 일" 로 분리한 것이 맞았다.
- 파일 이동 시 자동 링크 수정 스크립트가 디렉터리 링크를 잘못 매핑한 적이 있다. 이동 후 `check-doc-links.py` 를 반드시 돌린다.

## 발견·결정 로그

- 2026-10-14 파일 이름은 유지하고 디렉터리만 옮겼다. 본문에 "current-state.md §3" 처럼 이름으로 언급된 곳이 많다.
- 2026-10-14 링크 검사는 마크다운 링크만 본다. 본문 속 경로까지 검사하면 "앞으로 만들 파일" 언급이 모두 오류로 잡힌다.
- 2026-10-14 `docs/public` 의 깨진 링크 3건(`./06_배포·운영.md` → 실제 파일은 `07_배포·운영.md`)은 팀 문서라 고치지 않고 검사 기본 범위에서 뺐다. `--all` 로 확인 가능.
- 2026-10-14 `.claude/rules`·`skills`·`agents`·hook **새 스크립트 파일** 추가는 허용됐고, `settings.json` 수정과 기존 hook 삭제만 거부됐다.
- 2026-10-14 리뷰가 잡은 것: 기본 로케일이 en 인 환경에서는 F2 의 반쪽 수정을 테스트가 못 잡는다. 리뷰어가 실제로 로케일을 바꿔 실행해 확인했다 — 독립 리뷰의 효과.
- 2026-10-14 `.claude/settings.json`·hook 수정은 권한 검사에서 거부됐다. 옛 hook 이 `docs/ai/archive/2026-10-ai-dev-prep/checklist.json`·`docs/ai/archive/2026-10-ai-dev-prep/reviews/` 를 읽으므로 둘은 원래 위치에 남겼다.
