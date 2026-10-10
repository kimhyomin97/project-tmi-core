# 결정 필요 사항 (decisions-needed)

> 아침에 이 문서만 읽고 결정할 수 있도록 정리했다. **중요도 순**. 각 항목: 선택지 · 장단점 · 추천 · "결정하면 바로 시작할 수 있는 다음 작업".
> 근거 문서: [current-state.md](./current-state.md) (사실, F1~F10) · [research.md](./research.md) · [architecture-proposal.md](./architecture-proposal.md) · [conventions-proposal.md](./conventions-proposal.md) · [ai-workflow.md](./ai-workflow.md)
> 다음 작업의 ID 는 [feature_list.example.json](./feature_list.example.json) 항목이다.

---

## 0. 결정 없이 바로 해도 되는 것

다음 두 건은 **현재 명세(03)가 약속한 동작으로 되돌리는 수정**이라 선택지가 없다. 결정 대기 없이 착수를 권한다.

| ID | 내용 | 근거 | 규모 |
|---|---|---|---|
| `FIX-F1` | `SimilarityClient.score` 파라미터에 `@RequestBody` — **채점 API 가 지금 항상 500** | current-state F1, §7.2 P7 [실행] | 1줄 + 테스트 1개 |
| `FIX-F2` | `spring.messages.basename=messages/messages` + `spring.messages.fallback-to-system-locale=false` — 에러 메시지가 코드 문자열로 나가는 문제 | current-state F2 [실행], conventions §2.1 (검증됨) | 2줄 + 테스트 1개 |

---

## 1. 결정 목록 (중요도 순)

| 순위 | ID | 질문 | 추천 | 막고 있는 작업 |
|---|---|---|---|---|
| 1 | D1 | 구조를 A·B·C 중 무엇으로? | **B** (기능 모듈 + 공개 API) | `ARCH-B2`, `ARCH-B3`, Toss WIP 배치 |
| 2 | D3 | `./gradlew test` 를 DB 없이 돌게 할 방법은? | **Testcontainers + `integration` 태그 분리** | `SIG-1` → AI 루프 전체 |
| 3 | D4 | OSIV 를 끄고 서비스가 DTO 를 반환하게 할까? | **예** (서비스 DTO 반환 → OSIV off 순서) | `FIX-F4`, R5·R6 해제 |
| 4 | D2 | 에러 응답 형식을 RFC 9457 로 바꿀까? | **아니오** — 형식 유지, 핸들러만 `ResponseEntityExceptionHandler` 상속 | `FIX-F3` |
| 5 | D6 | 03 명세와 코드가 다른 곳은 어느 쪽을 고칠까? | **03 을 코드에 맞춤** (누락 = `MISSING_PARAMETER`), 미구현 엔드포인트는 명세에 "미구현" 표기 | `DOC-1` 일부 |
| 6 | D9 | 미커밋 Toss WIP 를 어떻게 할까? | **D1 결정 후 `toss.holdings.client` 에 배치해 별도 브랜치로 완성** | Toss 기능 |
| 7 | D5 | 기존 코드 포맷(`spotlessApply`)을 언제 적용할까? | **`FIX-F*` 시작 전에 단독 커밋** | `SIG-2` |
| 8 | D8 | 06 가이드의 틀린 지침 2건을 누가·언제 고칠까? | **`FIX-F4` 와 같은 PR** | `DOC-1` |
| 9 | D7 | `spring-boot-starter-restclient` 를 추가할까? | **보류** — 관측(observation) 도입 시 | 없음 |
| 10 | D12 | AI 루프의 사람 확인 지점을 어디에 둘까? | **ai-workflow.md §2.2 기본값 (H0~H3, `human_gate` 규칙)** | 루프 시작 |
| 11 | D11 | `gradlew` 에 실행 권한을 줄까? | **예** (`git update-index --chmod=+x gradlew`) | 없음 (우회 가능) |
| 12 | D10 | 헝가리안 접두사(`IErrorType`, `ETmiErrorType`, `ELogLevel`)를 바꿀까? | **아니오** | 없음 |

---

## 2. 항목별 상세

### D1. 구조 선택 — A / B / C

| 선택지 | 장점 | 단점 | 비용 (estimated) |
|---|---|---|---|
| A 레이어드 정비 | 변경 최소, 팀 결정과 완전 일치 | 기능 간 엔티티 공유가 남음 (`AnswerResponse → SentenceEntity`) | 약 5파일, 0.5일 |
| **B 기능 모듈 + 공개 API** | 기능 결합을 빌드(ArchUnit R2)로 차단, Toss 배치 규칙 확정, AI 작업 단위 = 모듈 | 공개 record 가 엔티티 필드 일부 중복, 패키지 이동 | 약 12파일, 1~1.5일 |
| C 헥사고날 | 도메인 순수성, 외부 시스템 교체 용이 | 이미 되돌린 구조, 파일 2~3배, `sentence → answer` 역방향 의존 문제 (examples README §5) | 30+ 파일, 3~5일 |

- **추천: B.** 근거 — architecture-proposal.md §4.1. B 는 A 를 포함하므로 A 단계(`ARCH-A1`)까지는 어느 쪽을 골라도 낭비가 없다.
- 판단 질문 (examples/answer-scoring/README.md §6): `AnswerResponse` 가 `SentenceEntity` 를 import 해도 괜찮은가? 아니라면 B.
- **결정하면 바로 시작**: `ARCH-A1` (Similarity 빈 설정을 `common` 밖으로 — ArchUnit R1·R3 `@Disabled` 해제). B 라면 이어서 `ARCH-B2` (`git mv infra/similarity → similarity`).

### D3. 테스트 기본 신호 — `CoreApplicationTests` 처리

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `@Tag("integration")` + Testcontainers `@ServiceConnection`, `test` 는 태그 제외, `integrationTest` 태스크 신설** | `./gradlew test` 가 DB·Docker 없이 통과 → AI 루프 신호. 통합 테스트도 재현 가능 | `build.gradle` 에 의존성 2개(`spring-boot-testcontainers`, `testcontainers-postgresql`)·태스크 추가. 통합 테스트엔 Docker 필요 |
| (b) 로컬 PostgreSQL 유지 | 변경 없음 | 모든 환경에서 DB 기동 필요 (이 작업에서 약 5분 + 매 세션 반복, blockers.md §2) |
| (c) H2 | Docker 불필요 | `ORDER BY RANDOM()`·`jsonb`·native enum 바인딩 동작이 달라 오탐 (06 §7.2 도 비권장) |

- **추천: (a).** current-state F8 — F1~F4 가 모두 테스트 부재로 잠복. 테스트를 늘리려면 기본 테스트가 환경 없이 돌아야 한다.
- **결정하면 바로 시작**: `SIG-1` → `TEST-1` (conventions §6.3 의 검증된 테스트 3종 추가).

### D4. OSIV off + 서비스 DTO 반환

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) 서비스가 응답 DTO 반환(conventions §3.1) → `spring.jpa.open-in-view=false`** | 외부 호출 중 커넥션 점유 해소 (1 → 0, current-state §3.4 [실행]), DTO 변환 위치 통일 (F5) | `SentenceService`·`SentenceController` 시그니처 변경 |
| (b) `open-in-view=false` 만 | 1줄 | 엔티티가 트랜잭션 밖에서 변환됨 — 연관관계 추가 시 `LazyInitializationException` |
| (c) 현행 유지 (`true`) | 변경 없음 | 커넥션 점유 지속, 기동 WARN |

- **추천: (a).** 순서가 중요 — DTO 반환 먼저, OSIV off 나중.
- **결정하면 바로 시작**: `FIX-F4` (self-invocation 제거 + OSIV off, ArchUnit R6 해제). R5 는 `SentenceService.getRandom` DTO 반환 시 해제.

### D2. 에러 응답 형식

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `{error:{code,message}}` 유지 + `GlobalExceptionHandler extends ResponseEntityExceptionHandler`** | 클라이언트 영향 없음, MVC 표준 예외 전부 처리 (깨진 JSON 500 → 400 등, conventions §2.2 검증 7/7) | 상위 클래스 동작에 일부 의존 |
| (b) RFC 9457 `ProblemDetail` 로 전환 | 표준 형식, Spring 기본 지원 | **외부 계약 변경** — `project-tmi-web` 수정 필요, 03 전면 개정 |
| (c) 현행 개별 `@ExceptionHandler` 에 누락 예외 추가 | 구조 유지 | 누락이 다시 생김 (415, 빈 body 등) |

- **추천: (a).** 응답 상태 코드가 500 → 400/415 로 바뀌는 것은 **버그 수정이지만 관찰 가능한 변경**이라 `human_gate: plan`.
- **결정하면 바로 시작**: `FIX-F3` (conventions §2.2 After 코드 + §2.3 `BaseException` 로그 레벨).

### D6. 03 명세 ↔ 코드 불일치

| 불일치 | 선택지 | 추천 |
|---|---|---|
| 파라미터 누락 시 03 은 `INVALID_REQUEST`, 코드는 `MISSING_PARAMETER` | 03 수정 / 코드 수정 | **03 수정** — 코드가 더 구체적, 이미 `ETmiErrorType` 에 있음 |
| 03 §1.5 에 `MISSING_PARAMETER`·`NOT_FOUND_URL`·`METHOD_NOT_SUPPORTED`·(신규 사용) `BAD_REQUEST` 없음 | 03 에 추가 | **추가** |
| `GET /sentences/meta`, `GET /api/v1/health` 미구현 | 구현 / 명세에서 "미구현" 표기 | **표기** 후 별도 항목으로 구현 결정 |
| 404 메시지 예시가 세 곳에서 다름 | 실제 번들 값으로 통일 | **통일** (`Sentence not found: {0}`) |

- **결정하면 바로 시작**: `DOC-1` 의 03 부분.

### D9. 미커밋 Toss WIP

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) D1 결정 후 해당 위치(B: `toss.holdings.client`)에 배치해 별도 브랜치에서 완성** | 배치 규칙이 굳기 전에 정함 | D1 대기 |
| (b) 현재 위치(`toss.holdings.infra`)로 먼저 완성 | 즉시 진행 | 외부 클라이언트 규칙 2개 고착 (current-state §3.6) |
| (c) 폐기 | — | 작업 손실 |

- WIP 는 [wip-snapshot.patch](./wip-snapshot.patch) 로 보존돼 있고 작업 트리에도 복원돼 있다 (blockers.md §1).
- 주의: WIP 의 `HoldingsSnapshotEntity.create(HoldingsApiResponse.Result, …)` 는 `domain → dto` 의존 → ArchUnit R4b 위반 예정. 매핑을 서비스(또는 DTO 쪽 `toEntity`)로 옮기는 것을 권한다.
- **결정하면 바로 시작**: 패치 적용 → `TossHoldingsClient`·`HoldingsApiResponse` 작성 → conventions §4.3 의 스케줄러 패턴.

### D5. 포맷 적용 시점

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `FIX-F*` 시작 전에 `spotlessApply` 단독 커밋 + `enforceCheck = true`** | 이후 diff 에 포맷 소음 없음, 에이전트가 포맷을 기계적으로 맞춤 | `src/main` 29파일 변경 커밋 1개 (의미 변경 없음 — current-state §7.3) |
| (b) `ratchetFrom 'origin/main'` — 변경 파일만 검사 | 대량 커밋 없음 | 파일을 처음 수정하는 PR 마다 포맷 소음 |
| (c) 도입 안 함 | — | 포맷 불일치 지속 |

- **결정하면 바로 시작**: `SIG-2`.

### D8. 06 가이드 수정

- 06 §3.3 "권장" 예시가 self-invocation(§3.4 위반), 06 §2.3 "enum 자동 바인딩" 이 틀림 (current-state F10). 가이드를 따른 코드가 결함이 된다 — **코드보다 가이드가 먼저**.
- 선택지: (a) 관련 FIX 와 같은 PR (b) 문서만 먼저 별도 PR. **추천 (a)** — 수정 근거(테스트)와 함께 리뷰.
- **결정하면 바로 시작**: `DOC-1` 의 06 부분 (conventions §8 표).

### D7. `spring-boot-starter-restclient`

- Boot 4 에서 `RestClient.Builder` 빈은 이 스타터가 있어야 생긴다 (current-state §4.6). 지금은 정적 `RestClient.builder()` 로 충분.
- **추천: 보류.** 재검토 조건: Micrometer 관측·공통 인터셉터가 필요해질 때.

### D12. AI 루프 운영

- 추천 기본값: ai-workflow.md §2.2 — H0(결정·항목 승인), H1(`human_gate: plan` 항목 계획 승인), H2(PR 머지), H3(`blocked`). 외부 계약·패키지 이동·의존성 추가·05/06/03 수정은 `plan`.
- **결정하면 바로 시작**: `feature_list.example.json` 을 `feature_list.json` 으로 복사하고 항목 승인 → 첫 세션은 `SIG-1`.

### D11. `gradlew` 실행 권한

- `gradlew` 가 `100644` 로 커밋돼 `./gradlew` 가 `Permission denied` (blockers.md §2 #1). `bash ./gradlew` 로 우회 가능.
- **추천: 권한 부여** — `git update-index --chmod=+x gradlew` 1커밋.

### D10. 헝가리안 접두사

- 05·99 에 문서화된 팀 결정. 변경 이득(관례 일치) < 비용(`src/main` 7파일·33회 참조, 문서 3곳). **추천: 유지** (conventions §1.4).
