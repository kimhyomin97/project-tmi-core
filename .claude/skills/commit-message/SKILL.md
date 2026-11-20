---
name: commit-message
description: 커밋 메시지·PR 제목 작성 시 사용. 포맷 `type(scope): subject` 통일 — type 은 feat/fix/docs/chore 4종, scope 는 모듈명, subject 는 한글 명사구 50자 이내(종결어미 금지). 본문은 선택, 쓸 때도 1-3 bullet 짧게.
---

# 커밋 메시지 규약

모든 커밋·PR 제목은 **`type(scope): subject`** 포맷 통일. Git log·GitHub UI 가독성, 릴리즈 노트 자동화 전제.

## 포맷

```
<type>(<scope>): <subject>

<body (선택)>

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
```

## Type (4종)

| type | 용도 |
|---|---|
| `feat` | 사용자 가시 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 (`docs/`, `README`, 주석) |
| `chore` | 그 외 전부 — 설정·빌드·의존성·테스트·리팩터·`.claude/`·`init.sql` |

4종 외 분화 필요 시점 (예: `refactor` 가 빈번해져 섞여 보임) 에 재검토.

## Scope

- 변경된 모듈명, **영문 소문자**
- 하위 도메인은 점 구분: `toss.holdings`, `infra.similarity`
- **가능하면 포함**. 생략은 **전역 변경만** (루트 설정, `.gitignore`, 다중 모듈 횡단)

주로 쓰는 scope 예: `sentence` · `answer` · `toss.holdings` · `toss.auth` · `infra.similarity` · `common` · `skills` · `ai` · `release`

## Subject

- **한글 명사구** (동사 종결어미 금지: ~합니다 · ~했다 · ~한다 X)
- **50자 이내** (억지로 맞출 필요 없음, 짧으면 더 좋음)
- **끝 마침표 금지**
- 백틱 ` ` ` 으로 파일명·심볼 감싸기 OK

## Body (선택)

- 3+ 파일 또는 비자명한 선택이 있을 때 권장
- `- ` bullet, 1-3 줄, 각 짧게
- "무엇" 보다 **"왜"** 중심 (코드가 "무엇" 은 이미 보여줌)
- 생략 가능

## Footer

- `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>` 는 모든 커밋 끝에 유지 (시스템 지시)
- 이슈 트래커 미사용 → 현재 `Refs #` 없음

## PR 제목

- **동일 규약 적용** (`type(scope): subject`)
- Squash merge 시 PR 제목이 그대로 커밋 메시지가 되므로 릴리즈 노트 품질에 직결
- PR 본문은 자유 포맷 (요약·테스트 계획 등)

## 좋은 예

```
feat(toss.holdings): 보유자산 스냅샷 수집 서비스

- 시간당 1회 cron 수집, KST 기준
- 외부 API 실패 시 skip (스케줄러 종료 방지)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
```

```
fix(answer): 502 응답 메시지 마침표 불일치
```

```
docs(toss): 보유자산 스냅샷 설계 문서 추가
```

```
chore(skills): 커밋 메시지 스킬 추가
```

## 안티패턴

| 나쁜 예 | 문제 |
|---|---|
| `커밋` / `WIP` / `수정` | type·scope·내용 전부 결여 |
| `feat: 기능을 추가합니다.` | 종결어미 + 끝 마침표 + scope 생략(단일 모듈인데) |
| `feat(toss.holdings): 보유자산 스냅샷 수집 서비스를 추가하여 시간당 한 번씩 API를 호출해 DB에 저장` | 50자 초과 — 본문으로 분리 |
| `update stuff` | 영문 vague, 변경 내용 불명 |
| `feat(toss, infra): ...` | scope 복수 — 커밋을 분리 |

## 체크리스트

- [ ] type 4종 중 하나
- [ ] scope — 단일 모듈이면 포함, 전역이면 생략
- [ ] subject 한글 명사구, 종결어미·마침표 없음
- [ ] 50자 이내 (넘으면 본문 활용)
- [ ] 본문은 "왜" 중심, 짧게
- [ ] Co-Authored-By 라인
- [ ] amend·force push 금지 (CLAUDE.md)

## 참고

- 릴리즈 노트 자동화의 전제 — `feat` → Added, `fix` → Fixed 분류 기반
- CLAUDE.md "작업 방식" 항목과 연계
