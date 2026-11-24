---
name: checkpoint
description: 긴 작업을 세션 간 이어받기 위한 플랜 저장·재개 스킬. docs/ai/plans/active/<id>.md 파일로 작업별 상태 관리. 생성·갱신·재개·완료 트리거 제공. Cline memory-bank + Claude Code handoff 패턴 조합, 수동 트리거 중심 (hook 자동화 제외).
---

# 세션 간 이어받기 (checkpoint)

> 긴 작업이 세션을 넘어가도 **다음 세션이 자기완결로 이어받을 수 있게** 플랜 파일 저장. Cline "memory-bank" + Claude Code "handoff" 흐름을 단순화.

## 언제 사용

- Step 여러 개로 나뉘는 설계·구현 (예: 영어 공부 기능 기획, Toss 도메인 신설)
- 1 세션에서 끝나지 않을 가능성이 있는 작업
- 결정·열린 질문이 누적되어 **다음 세션이 다시 읽을 필요** 있는 경우

짧은 1 회성 작업에는 불필요.

## 파일 위치·구조

CLAUDE.md 가 이미 요구한 체계 그대로:

```
docs/ai/
  plans/
    active/
      english-feature.md     ← 작업별 플랜 (여러 개 공존 가능)
      toss-holdings-mvp.md
    done/
      deploy-design.md       ← 완료 이동
  progress.md                ← 세션 메모 (전역, 이 스킬과 분리)
```

`progress.md` 는 세션 종료 시 "오늘 무엇 했는지" 간단 1-2 줄 메모용. 플랜과 역할 분리.

## 명령

| 명령 | 동작 |
|---|---|
| `/checkpoint <이름>` | 신규 플랜 생성 — `active/<이름>.md` |
| `/checkpoint update [이름]` | 현재 위치·결정·열린 질문 갱신. 이름 생략 시 활성 플랜 하나면 그것, 복수면 물음 |
| `/checkpoint done <이름>` | `active/<이름>.md` → `done/<이름>.md` 이동 |
| `/checkpoint list` | 활성 플랜 목록 표시 |
| `"<이름> 이어가자"` (자연어) | 활성 플랜 읽고 재개 |

## 템플릿

```markdown
# <주제>

## 목적
<왜 하는지 1-2줄>

## 범위 / 비범위
- **포함**: ...
- **제외**: ...

## 단계
- [ ] 1. ...
- [ ] 2. ...
- [ ] 3. ...

## 결정된 것
- ...

## 열린 질문
- ...

## 현재 위치
Step N 진행 중 — <구체적 지점>

## 검증 로그
- [V] <무엇이 확인됨> (실행·결과)
- [?] <확인 필요> (사유)
```

- `[V]` = 실행·검증 완료
- `[?]` = 미검증·추정
- 세션 재개 시 `[?]` 를 **먼저** 확인

## 세션 흐름

### 세션 A (작업 시작)

```
사용자: "plan 만들자: 영어 공부 기능 기획"
Claude:
  1. docs/ai/plans/active/english-feature.md 생성 (템플릿)
  2. 사용자와 목적·범위·단계 상의하며 채워넣기
```

작업 진행 중:
```
Claude 가 Step 완료마다 자동으로 "현재 위치" 업데이트 제안
또는 사용자가 "/checkpoint update english-feature"
```

종료 전:
```
사용자: "오늘 여기까지"
Claude:
  1. 현재 위치·결정·열린 질문 반영
  2. 검증 로그에 [V]/[?] 정리
  3. 변경사항 커밋 권장 (commit-message 스킬 연계)
```

### 세션 B (다음 세션, 재개)

```
사용자: "english-feature 이어가자"
Claude:
  1. docs/ai/plans/active/english-feature.md 읽기
  2. git log 로 마지막 변경 이후 커밋 확인 (안전망)
  3. "현재 위치" 부터 재개
  4. [?] 항목이 있으면 사용자에게 먼저 물음
```

### 작업 완료

```
사용자: "english-feature 완료"
Claude:
  /checkpoint done english-feature
    → active/ → done/ 이동
```

## 재개 시 체크 (파일 + git 재구성)

파일만 믿지 않음. Aider 식 **git 재구성**을 안전망으로:

1. `docs/ai/plans/active/<이름>.md` 읽기
2. `git log --oneline -20` — 플랜 파일 마지막 수정 이후 커밋 확인
3. `git status` — 미커밋 변경 있는지 확인
4. 플랜 파일 ↔ 실제 저장소 상태 **불일치 있으면 사용자에게 알림**

## 종료 전 가드

세션 종료 시그널 (사용자가 "오늘 끝", "여기까지" 같은 표현) 받으면:

- 활성 플랜이 있는데 "현재 위치" 가 마지막 작업 이후 업데이트 안 됐으면
- **"plan 갱신 필요 — 지금 반영할까?"** 묻기

사용자가 명시 거절하면 그대로 종료, 그 외엔 간단 갱신 후 종료.

## 다른 스킬과의 관계

| 스킬 | 역할 분담 |
|---|---|
| `feature-item` | **1개 작업 항목**의 TDD 플로우. checkpoint 는 그 상위 — **여러 Step 묶음** 보관 |
| `verify-claim` | `[V]`/`[?]` 태그를 이 스킬이 쓸 때, 증거 블록은 verify-claim 양식 사용 |
| `commit-message` | plan 파일 갱신 커밋 메시지 — `docs(ai): <이름> plan 갱신` 형태 |
| `clean` | 세션 종료 전 plan 파일 과다·불필요 섹션 정리 시 `/clean docs/ai/plans/active/<이름>.md L2` |

## 제외한 기능 (의도적으로)

- **SessionStart hook 자동 주입** — 과잉, 세션 노이즈 증가
- **tool-call 숫자 임계값** (예: 40/60) — 부정확
- **이름 호명 드리프트 감지** — 메타 출력 과다
- **자동 추출 memory** (Cursor Memories 류) — 명시 작성 선호와 어긋남
- **완전 자동 compaction** — 사용자 통제 선호

필요 시점에 추가.

## 체크리스트

- [ ] 긴 작업 시작 시 `/checkpoint <이름>` 으로 파일 생성
- [ ] Step 완료마다 "현재 위치" 업데이트
- [ ] 결정·열린 질문 누적 반영
- [ ] 세션 종료 전 plan 갱신 확인
- [ ] 다음 세션 재개 시 파일 + git log 모두 확인
- [ ] 작업 완료 시 `done/` 이동

## 참고

- CLAUDE.md "다음 세션이 이어받을 것: `docs/ai/plans/active/<id>.md`" 체계
- Cline memory-bank 패턴 (파일 분리 + 수동 트리거)
- Claude Code handoff 패턴 (`[V]`/`[?]` 검증 태그)
- GitHub Copilot SDK `plan.md` (1급 상태)
