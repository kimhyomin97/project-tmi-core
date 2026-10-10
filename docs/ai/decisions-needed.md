# 결정 필요 사항 (decisions-needed)

> 아침에 이 문서만 읽고 결정할 수 있도록 정리했다. **중요도 순**. 각 항목: 선택지 · 장단점 · 추천 · "결정하면 바로 시작할 수 있는 다음 작업".
> 근거 문서: [current-state.md](./current-state.md) (사실, F1~F10) · [research.md](./research.md) · [architecture-proposal.md](./architecture-proposal.md) · [conventions-proposal.md](./conventions-proposal.md) · [ai-workflow.md](./ai-workflow.md)
> 다음 작업의 ID 는 [feature_list.example.json](./feature_list.example.json) 항목이다.

---

## 0. 결정 없이 바로 해도 되는 것

**03 명세가 이미 약속한 동작으로 되돌리는 수정**이라 선택지가 없다. DB 가 필요 없는 테스트로 검증되므로 D3(SIG-1)도 기다리지 않는다 (ai-workflow.md §0).

| 순서 | ID | 내용 | 근거 | 규모 |
|---|---|---|---|---|
| 1 | `TEST-1` | DB 없이 도는 테스트 3종 (현재 실패하는 2개는 FIX id 를 단 `@Disabled`) | conventions §6.3 (현재 코드로 실패 재현 검증) | 테스트 3파일 |
| 2 | `FIX-F1` | `SimilarityClient.score` 파라미터에 `@RequestBody` — **채점 API 가 지금 항상 500** | current-state F1, §7.2 P8 [실행] | 1줄 |
| 3 | `FIX-F1T` | read timeout·빈 응답 → 503 (`RestClientException` catch + null 처리) — F1 을 고치는 순간 드러나는 다음 결함 | current-state §3.3(f) [실행·리뷰] | 수 줄 + 테스트 1 |
| 4 | `FIX-F2` | `spring.messages.basename=messages/messages` + `fallback-to-system-locale=false` — 에러 메시지가 코드 문자열로 나가는 문제 | current-state F2 [실행], conventions §2.1 (검증) | 2줄 |
| 5 | `FIX-R5` | `SentenceService.getRandom` 이 DTO 반환 (구조 결정과 무관, A′·B 공통) | conventions §3.1, ArchUnit R5 | 2파일 |
| 6 | `ARCH-A1` | Similarity 빈 설정을 `common/config` 밖(`infra.similarity`)으로 — A′·B 어느 쪽이든 버려지지 않음 | architecture-proposal §4.3 단계 1, ArchUnit R1·R3 | 2파일 + 설정 키 |

---

## 1. 결정 목록 (중요도 순)

| 순위 | ID | 질문 | 추천 | 막고 있는 작업 |
|---|---|---|---|---|
| 1 | D1 | 구조를 A′·B·C 중 무엇으로? | **B** (기능 모듈 + 공개 API). 차이가 작으므로 A′ 도 합리적 | `ARCH-B2`·`B3` 또는 `ARCH-A2`, Toss WIP 배치 |
| 2 | D3 | `./gradlew test` 를 DB 없이 돌게 할 방법은? | **Testcontainers + `integration` 태그 분리** | `SIG-1` → `FIX-F4`, 루프 init.sh |
| 3 | D4 | OSIV 를 끌까? | **예** — `FIX-R5`(서비스 DTO 반환) 후 `FIX-F4` 에서 | `FIX-F4` |
| 4 | D2 | 에러 응답 형식을 RFC 9457 로 바꿀까? | **아니오** — 형식 유지, 핸들러만 `ResponseEntityExceptionHandler` 상속 | `FIX-F3` |
| 5 | D6 | 03 명세와 코드가 다른 곳은 어느 쪽을 고칠까? | **03 을 코드에 맞춤** | `DOC-1` 일부 |
| 6 | D9 | 미커밋 Toss WIP 를 어떻게 할까? | **D1 결정 후 그 배치로 별도 브랜치에서 완성** | Toss 기능 |
| 7 | D5 | 기존 코드 포맷(`spotlessApply`)을 언제 적용할까? | **`FIX-F3` 전에 단독 커밋** (1~2줄 FIX 는 기다리지 않음) | `SIG-2` |
| 8 | D8 | 06 가이드의 틀린 지침을 언제 고칠까? | **`DOC-1` 로 `FIX-F3`·`FIX-F4` 직후 별도 PR** | `DOC-1` |
| 9 | D12 | AI 루프의 사람 확인 지점·hook 을 어떻게 둘까? | **ai-workflow.md §2.2 기본값 + `check-feature.sh`, `check-done.sh` 는 루프 시작 시 내림** | 루프 시작 |
| 10 | D13 | Similarity 의 비정상 응답(빈 본문, 범위 밖 점수, 상태 600+)을 어떻게 다룰까? | **빈 본문 = 503, 범위 밖 점수 = 허용 오차(1e-6)만 보정·그 밖 503, 600+ 는 보류** | `FIX-F1T` 범위 |
| 11 | D7 | `spring-boot-starter-restclient` 를 추가할까? | **보류** | 없음 |
| 12 | D11 | `gradlew` 에 실행 권한을 줄까? | **예** | 없음 (우회 가능) |
| 13 | D10 | 헝가리안 접두사를 바꿀까? | **아니오** | 없음 |

---

## 2. 항목별 상세

### D1. 구조 선택 — A′ / B / C

| 선택지 | 장점 | 단점 | 비용 (estimated) |
|---|---|---|---|
| A′ 레이어드 + 기능 간 접근 규칙 | 기존 `infra/` 규칙(02 §3.2·05 §1.3) 유지. 코드는 B 와 import 경로만 다름. 경계 규칙 1개 | 타 기능의 `service`·`dto` 패키지 전체가 공개 (웹 응답 DTO 재사용도 규칙상 허용) | 8파일, 0.5일 |
| **B 기능 모듈 + 공개 API** | 공개 표면이 base package 에 골라 둔 타입으로 좁고, 넓히려면 파일 이동 diff 가 생김. Spring Modulith 기본 규칙과 같음 (`common` OPEN 1파일) | `infra/` 해체 → 02 §3.2·§5.1·05 §1.3·06 §6.5 변경, 패키지 이동 4파일 | 12파일, 0.5~1일 |
| C 헥사고날 | 도메인 순수성 | 이미 되돌린 구조(02 §2.2), 예시 배치는 `answer ↔ sentence` 순환, 순환 없는 배치는 B 의 경계가 선행 | 22 수정 + 20 이상 신규, 2~3일 |

- **추천: B.** 근거 — architecture-proposal.md §4.1. 다만 A′ 와의 차이는 좁다: B 의 순이득은 공개 표면의 크기·diff 가시성, 비용은 `infra/` 규칙 변경과 약 4파일 (§3.3 의 질문으로 판단). A′ 를 골라도 손해가 크지 않다.
- 둘 다 바꾸는 기존 규칙: 99 §2.3 (`common/config` 에서 빈 등록), 02 §4.2 (시퀀스). B 는 추가로 02 §3.2·§5.1·05 §1.3·06 §6.5. 문서 수정은 `DOC-2`.
- **결정하면 바로 시작**: B 면 `ARCH-B2`(`git mv infra/similarity → similarity`), A′ 면 `ARCH-A2`(계획에 새 경계 규칙 포함). 둘 다 `ARCH-A1`·`FIX-F1T` 이후 — `ARCH-A1` 은 D1 과 무관해 §0 에서 먼저 진행.

### D3. 테스트 기본 신호 — `CoreApplicationTests` 처리

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `@Tag("integration")` + Testcontainers `@ServiceConnection`, `test` 는 태그 제외, `integrationTest` 태스크 신설** | `./gradlew test` 가 DB·Docker 없이 통과 → AI 루프 신호. 통합 테스트(`OsivConnectionIT` 등)도 재현 가능 | `build.gradle` 의존성 2개(`spring-boot-testcontainers`, `testcontainers-postgresql`)·태스크 추가. 통합 테스트엔 Docker 필요 |
| (b) 로컬 PostgreSQL 유지 | 변경 없음 | 모든 환경에서 DB 기동 필요 (이 작업에서 약 5분 + 매 세션 반복, blockers.md §2) |
| (c) H2 | Docker 불필요 | `ORDER BY RANDOM()`·`jsonb`·native enum 바인딩 동작이 달라 오탐 (06 §7.2 도 비권장) |

- **추천: (a).** current-state F8 — F1~F4 가 모두 테스트 부재로 잠복했다.
- **결정하면 바로 시작**: `SIG-1` (verify: DB 접속을 막고 `./gradlew test`).

### D4. OSIV off

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `FIX-R5`(서비스 DTO 반환) → `FIX-F4`(self-invocation 제거 + `open-in-view=false`)** | 외부 호출 중 커넥션 점유 해소 (1 → 0, current-state §3.4 [실행]), DTO 변환 위치 통일 | `SentenceService`·`SentenceController` 시그니처 변경 |
| (b) `open-in-view=false` 만 | 1줄 | 엔티티가 트랜잭션 밖에서 변환됨 — 연관관계 추가 시 `LazyInitializationException` |
| (c) 현행 유지 | 변경 없음 | 커넥션 점유 지속, 기동 WARN |

- **추천: (a).** 순서가 핵심 — `FIX-F4` 는 `FIX-R5` 에 의존 (feature_list).
- **결정하면 바로 시작**: `FIX-F4` (`OsivConnectionIT` 로 커넥션 0 고정, R6 해제).

### D2. 에러 응답 형식

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `{error:{code,message}}` 유지 + `GlobalExceptionHandler extends ResponseEntityExceptionHandler`** | 클라이언트 영향 없음, MVC 표준 예외 전부 처리 (깨진 JSON 500 → 400 등 — conventions §0 에서 After 코드 그대로 실행 검증) | 상위 클래스 동작에 의존 (commit 검사·500 속성은 오버라이드에서 보존) |
| (b) RFC 9457 `ProblemDetail` 로 전환 | 표준 형식 | **외부 계약 변경** — `project-tmi-web` 수정 필요, 03 전면 개정 |
| (c) 현행 개별 `@ExceptionHandler` 에 누락 예외 추가 | 구조 유지 | 누락이 다시 생김 (415, 빈 body 등) |

- **추천: (a).** 새 코드 `BAD_REQUEST`·415 가 응답에 나타나므로 `human_gate: plan`.
- **결정하면 바로 시작**: `FIX-F3` (conventions §2.2 + §2.3 — 둘을 함께 적용해야 404 가 INFO 가 된다).

### D6. 03 명세 ↔ 코드 불일치

| 불일치 | 추천 |
|---|---|
| 파라미터 누락 시 03 은 `INVALID_REQUEST`, 코드는 `MISSING_PARAMETER` | **03 수정** — 코드가 더 구체적, 이미 `ETmiErrorType` 에 있음 |
| 03 §1.5 에 `MISSING_PARAMETER`·`NOT_FOUND_URL`·`METHOD_NOT_SUPPORTED`·(FIX-F3 이후) `BAD_REQUEST` 없음 | **추가** |
| `GET /sentences/meta`, `GET /api/v1/health` 미구현 | 명세에 **"미구현" 표기** 후 별도 항목으로 구현 결정 |
| 404 메시지 예시가 세 곳에서 다름 | 실제 번들 값(`Sentence not found: {0}`)으로 **통일** |

- **결정하면 바로 시작**: `DOC-1` 의 03 부분 — 단 `DOC-1` 은 `FIX-F3`(새 코드 `BAD_REQUEST`)·`FIX-F4` 이후다. 03 의 `MISSING_PARAMETER` 정정만 먼저 하려면 H0 에서 `DOC-1` 을 둘로 나눈다.

### D9. 미커밋 Toss WIP

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) D1 결정 후 그 배치(B: `toss.holdings.client` / A′: `infra/toss/`)로 별도 브랜치에서 완성** | 배치 규칙이 굳기 전에 정함 | D1 대기 |
| (b) 현재 위치(`toss.holdings.infra` + `common/config` 등록)로 먼저 완성 | 즉시 진행 | 외부 클라이언트 규칙 2개 고착, `common ↔ toss` 순환 (R1·R3) |
| (c) 폐기 | — | 작업 손실 |

- WIP 는 [wip-snapshot.patch](./wip-snapshot.patch) 로 커밋돼 있고, 작업 종료 시 작업 트리에 미커밋 상태로 복원한다 (blockers.md §1).
- WIP 의 `HoldingsSnapshotEntity.create(HoldingsApiResponse.Result, …)` 는 `domain → dto` 의존 → ArchUnit R4b 위반 예정. 매핑을 서비스(또는 DTO 쪽 `toEntity`)로 옮길 것.
- **결정하면 바로 시작**: 패치 적용 → `TossHoldingsClient`·`HoldingsApiResponse` 작성 → conventions §4.3 의 스케줄러 패턴 (설계 §6 에러 정책 포함).

### D5. 포맷 적용 시점

| 선택지 | 장점 | 단점 |
|---|---|---|
| **(a) `FIX-F3` 전에 `spotlessApply` 단독 커밋 + `enforceCheck = true`** | 큰 수정(`GlobalExceptionHandler`) 전에 포맷 소음 제거, 이후 에이전트가 포맷을 기계적으로 맞춤 | 30파일(src/main 29 + 테스트 1) 커밋 1개 — 의미 변경 없음 (class 상수 풀 동일, current-state §7.3) |
| (b) `ratchetFrom 'origin/main'` — 변경 파일만 검사 | 대량 커밋 없음 | 파일을 처음 수정하는 PR 마다 포맷 소음 |
| (c) 도입 안 함 | — | 포맷 불일치 지속 |

- `FIX-F1`·`F1T`·`F2` 는 1~수 줄이라 기다리지 않는다.
- **결정하면 바로 시작**: `SIG-2` (H0 에서 결정되면 `human_gate: none`).

### D8. 06 가이드 수정 시점

- 06 §3.3 "권장" 예시가 self-invocation(§3.4 위반), 06 §2.3 "enum 자동 바인딩" 이 native 에서 틀림 (current-state F10). 가이드를 따른 코드가 결함이 됐다.
- 선택지: (a) 관련 FIX 와 같은 PR (b) **`DOC-1` 로 `FIX-F3`·`FIX-F4` 직후 별도 PR**. 추천 (b) — ai-workflow §1.1 "항목 1개 = PR 1개" 와 맞고, 수정 근거(통과한 테스트)가 먼저 머지된 뒤 문서를 고친다. 그 사이 CLAUDE.md 가 "06 §2.3 은 틀렸다" 를 이미 경고한다.
- **결정하면 바로 시작**: `DOC-1` (conventions §8 표 22행).

### D12. AI 루프 운영

- 추천 기본값: ai-workflow.md §2.2 — H0(결정·항목 승인), H1(`human_gate: plan` 항목: 계획 파일 검토 후 **사람이 `plan_approved` 기록**), H2(PR 머지), H3(`blocked`).
- hook: 제안된 `session-base.sh`(SessionStart) + `check-feature.sh`(Stop — 이번 세션의 id 별 verify 기록, 사람 전용 필드 불변, 사람 판정 항목 차단; 14개 시나리오 실행 확인)를 추가하고, 이 문서 세트용 `check-done.sh` 는 내린다. 후자는 `/tmp/stop-hook-count` 를 초기화하지 않아 15회 이후 영구히 꺼지는 결함이 있다 (ai-workflow.md §4.6).
- **결정하면 바로 시작**: `feature_list.example.json` 을 `feature_list.json` 으로 복사하고 D1 에 해당하는 `pending_decision` 묶음을 `items` 로 옮긴다 → 첫 세션 `TEST-1`.

### D13. Similarity 비정상 응답

- 빈 본문(200/204) → 현재 NPE → 500. **`FIX-F1T` 에서 503 으로** (선택지 없음, 03 계약).
- 범위 밖 점수 (03 §5.1 계약은 0.0~1.0): 선택지 (a) 관대한 clamp — 1.5 → 1.0 EXCELLENT, 서버 결함이 "만점" 으로 보임 (b) **허용 오차(1e-6)만 보정, 그 밖은 503 + 원래 값을 예외 메시지에** (c) 전부 503. **추천 (b)** — 부동소수 오차(1.0000001)만 흡수하고 결함은 드러낸다. 코사인 유사도의 음수를 Similarity 서버가 어떻게 환산하는지는 그 서버 문서 확인 필요 (01 기획 §1.2). examples 의 `SimilarityScorer` 는 (b) 로 작성됨.
- 상태 코드 600 이상 → 기본 `ResponseErrorHandler` 가 성공으로 처리 (examples-round-1 M4 [실행·리뷰]). 선택지: (a) 커스텀 `ResponseErrorHandler` 로 2xx 외 전부 에러 (b) 보류. **추천 (b)** — Similarity 서버(FastAPI)가 600+ 를 낼 근거가 없다.

### D7. `spring-boot-starter-restclient`

- Boot 4 에서 `RestClient.Builder` 빈은 이 스타터가 있어야 생긴다 (current-state §4.6). 지금은 정적 `RestClient.builder()` 로 충분. **추천: 보류** — 재검토 조건: Micrometer 관측·공통 인터셉터가 필요해질 때.

### D11. `gradlew` 실행 권한

- `gradlew` 가 `100644` 로 커밋돼 `./gradlew` 가 `Permission denied` (blockers.md §2 #1). **추천: 권한 부여** — `git update-index --chmod=+x gradlew` 1커밋. 그 전까지 `bash ./gradlew`.

### D10. 헝가리안 접두사

- 05·99 에 이 이름으로 정착 (택한 이유는 문서에 없음). 변경 이득(관례 일치) < 비용(`src/main` 7파일·33회 참조, 문서 3곳). **추천: 유지** (conventions §1.4).
