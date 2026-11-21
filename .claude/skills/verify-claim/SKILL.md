---
name: verify-claim
description: AI 주장(완료·통과·성공·fix·작성·수정 등)에 의도적 회의를 걸어 환각·과신 방지. 코드뿐 아니라 문서·분석·결정·리뷰·커밋 등 범용 적용. "완료" 선언 전 ✅ 검증 블록 필수, 검증 못 하면 완료 선언 금지(검증 보류).
---

# 주장 검증 (verify-claim)

> AI 의 주장을 그대로 믿지 말고, 매번 **증거**를 요구한다. "월드 Z 의 10번째 사람" 원리 — 집단사고·과신 방지의 자동 회의.

**적용 원칙**: 매체(코드/문서/분석/결정)에 관계없이 **주장에는 증거**. 증거 없으면 완료 선언 금지.

## 트리거

다음 중 **어느 쪽**에서 들어와도 자동 발동:

1. **AI 자기 주장 시** — "완료", "통과", "성공", "fix 함", "작성함", "수정함", "확인함" 같은 종결 표현 쓰려 할 때
2. **사용자 완료 선언 시** — "Step N 완료", "작성 완료" 같은 입력 받으면 검증 플로우 개시
3. **명시 호출** — `/verify-claim` 또는 "검증해줘"

## 강도 — Strict

- 검증 못 하면 **완료 선언 자체 금지**
- 실행 불가 환경이면 `⚠️ 검증 보류` 로 **솔직히 표시**
- "아마 됐을 것" / "통과했을 것" 같은 추정 금지

## 통일 ✅ 검증 블록

주장 매체에 관계없이 동일 형태 사용:

```
✅ 검증
- 실행 / 참조: <명령 or 파일 경로>
- 결과 / 근거: <결과 요약 or 핵심 발견>
- 영향 범위: <파일·섹션·커밋>
- 남은 리스크: <한줄 or "없음">
```

## 주장 유형별 검증 수단

| 주장 | 수단 |
|---|---|
| 코드 작성 완료 | `bash ./gradlew compileJava` → 작업 scope test → 전체 build (단계적) |
| 테스트 통과 | 실행 명령 + 결과 로그 (예: `3 passed (12s)`) |
| 문서 작성 완료 | 파일 경로 + 핵심 섹션명 + `python3 scripts/check-doc-links.py` |
| 커밋·푸시 완료 | `git log -1`, `git push` 결과 (예: `c2f2571..2b16dcf main -> main`) |
| 설정 변경 완료 | `bash ./gradlew compileJava` (property placeholder 오탈자 방지) |
| 질문 답변·설명 | 참조 파일 + 라인 번호 (예: `CLAUDE.md:42`) |
| 분석·설계 결정 | 근거 자료 + 대안 비교 + 리스크 |
| 리뷰 완료 | 검토 파일 목록 + 발견 사항 or "결함 없음" |
| 스킬·설정 수정 | 변경 파일 + 재로드 조건 (스킬은 다음 호출 시 반영) |

## 예시

### 코드 작성 완료

```
✅ 검증
- 실행: bash ./gradlew test --tests '*HoldingsSnapshotServiceTest'
- 결과: 3 passed (12s)
- 영향 범위: HoldingsSnapshotService.java, HoldingsSnapshotServiceTest.java
- 남은 리스크: 통합 테스트 미실행 (DB 미기동)
```

### 문서 작성 완료

```
✅ 검증
- 참조: docs/public/toss/00_인증.md (5 섹션)
- 결과: Credentials / Issue flow / Caching / Code 분리 / Security 포함, 종결어미 없음
- 영향 범위: Toss 도메인 신설 문서
- 남은 리스크: 링크 체크 스크립트 미실행
```

### 커밋·푸시 완료

```
✅ 검증
- 실행: git log -1 / git push origin main
- 결과: 2b16dcf, c2f2571..2b16dcf main -> main
- 영향 범위: .claude/skills/commit-message/SKILL.md
- 남은 리스크: 없음
```

### 질문 답변

```
✅ 검증
- 참조: CLAUDE.md:42, docs/public/05_개발컨벤션.md §1.3
- 근거: controller → service → repository 방향 명시
- 남은 리스크: 없음
```

### 분석·설계 결정

```
✅ 검증
- 참조: openapi.json §HoldingsOverview, docs/public/toss/01_보유자산스냅샷.md §4
- 근거: Wide 스키마 채택 — Price {krw required, usd nullable}
- 영향 범위: 스키마 설계
- 남은 리스크: JPY·EUR 추가 시 재검토 필요 (phase 4)
```

## ⚠️ 검증 보류 (실행 불가 시)

검증 수단이 막힐 때 **완료 선언 대신** 보류 블록:

```
⚠️ 검증 보류
- 사유: <무엇이 막혔는지>
- 가능한 대체 검증: <한 단계 낮은 수단>
- 완료 선언 불가 — <조건 충족 후 재검증>
```

### 예시

```
⚠️ 검증 보류
- 사유: PostgreSQL 미기동 (Connection refused) — CLAUDE.md 상 환경 이슈
- 가능한 대체 검증: bash ./gradlew compileJava (컴파일 OK)
- 완료 선언 불가 — DB 기동 후 `test` 재실행 필요
```

## 금지 표현

증거 없이 쓰면 안 되는 종결 패턴:

- "~ 완료되었습니다 ✓"
- "통과했을 것으로 보입니다"
- "아마 정상 동작할 것입니다"
- "문제 없어 보입니다"

→ 전부 **증거 미제시** = 환각 리스크. ✅ 검증 또는 ⚠️ 보류 로 대체.

## 테스트 실패 시

- "완료" 선언 **자동 취소**
- 실패 로그 요약 제시
- 재시도 횟수·방향 사용자 확인 받고 재진행

```
❌ 검증 실패
- 실행: bash ./gradlew test --tests '*HoldingsSnapshotServiceTest'
- 결과: 1 failed, 2 passed
- 실패: HoldingsSnapshotServiceTest.collect_whenApiFails_shouldSkip
  - expected: no exception
  - actual: TossApiException thrown
- 다음 조치 문의: 예외 catch 누락 수정 vs 테스트 가정 재검토
```

## MEMORY 규칙과 통합

기존 user memory: *"사용자가 Step N 완료라고 하면 자동으로 파일 Read 해서 검증"* 은 이 스킬의 **트리거 2** 로 흡수. 대체가 아니라 체계화.

## 체크리스트

- [ ] 주장 쓰기 전 ✅ / ⚠️ 블록 준비됨
- [ ] "실행/참조" 란에 **실제** 명령·파일 명시 (추정 금지)
- [ ] "결과/근거" 란에 **실제** 결과 요약 (fabricate 금지)
- [ ] 실행 못 하면 ⚠️ 보류, 완료 선언 안 함
- [ ] 테스트·검증 실패 시 완료 선언 자동 취소

## 참고

- CLAUDE.md "작업 방식" — *"성공을 주장할 때는 실행한 명령과 결과를 함께 적는다"* 가 원본 규약, 이 스킬이 체계화
- 블록 포맷은 모든 스킬·대화에서 **통일**
