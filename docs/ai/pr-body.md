## 요약

`docs/ai/TASK.md` 지시에 따른 **AI 기반 개발 준비 작업**이다. 목표는 결정이 아니라 **아침에 결정할 수 있는 제안서**이며, `src/main` 은 한 줄도 바꾸지 않았다.

**먼저 읽을 문서**: [`docs/ai/decisions-needed.md`](docs/ai/decisions-needed.md) — 결정 13건을 중요도 순으로 정리했고, 각 항목에 추천안과 "결정하면 바로 시작할 작업"(feature_list id)을 붙였다.

### 분석 중 실행으로 확인한 결함 (current-state.md)

| # | 결함 | 확인 방법 |
|---|---|---|
| F1 | `POST /api/v1/answers` 가 **항상 500** — `SimilarityClient.score` 에 `@RequestBody` 누락 | 프로브 테스트 |
| F2 | 메시지 번들이 로드되지 않아 에러 `message` 가 코드 문자열로 나가고 i18n 이 동작하지 않음 (`messages/` 경로인데 basename 미설정) | `DelegatingMessageSource` 확인 |
| F3 | 깨진 JSON·`text/plain` 이 500 으로 처리됨, 500 로그에 스택트레이스 없음, 404 가 ERROR 레벨 | 프로브 테스트 |
| F4 | `@Transactional` self-invocation + 선언 쿼리 메서드라 조회가 트랜잭션 없이 실행되고, OSIV 때문에 외부 호출 동안 DB 커넥션 1개 점유 | Hikari 활성 커넥션 측정 (1 → OSIV off 시 0) |
| — | F1 을 고치면 바로 드러나는 다음 결함: read timeout 이 503 이 아닌 500 (POST 는 `RestClientException` 으로 옴) | 가짜 지연 서버 |
| F9·F10 | API 명세(03)와 코드 불일치, 사내 가이드(06)의 틀린 지침 2건 (self-invocation 예시, native enum 바인딩) | 실행 |
| — | 루트 `README.md` 가 폐기된 헥사고날 구조를 설명 | 정적 |

### 산출물

| 파일 | 내용 | 최종 rubric (근거·구체·일관·판단) | 라운드 |
|---|---|---|---|
| `docs/ai/current-state.md` | `src/main` 34파일·40타입 전수 표, 의존 Mermaid, 불일치·결함, ArchUnit 위반 목록(§6), 빌드·프로브 결과(§7) | 4/4/4/4 | 3 |
| `docs/ai/research.md` | Boot 구조·Modulith·헥사고날/레이어드·예외·DTO·트랜잭션·테스트·포맷 도구·AI 루프. 근거 등급([J] 소스 jar, [R] 실행, [F] 본문, [S] 검색)과 URL | 4/4/4/4 (final-consistency) | 2 |
| `docs/ai/architecture-proposal.md` | A′(레이어드 + 접근 규칙) / **B(기능 모듈 + 공개 API, 추천)** / C(헥사고날), 비용·바뀌는 기존 규칙·단계 계획 | 4/4/4/4 | 4 |
| `docs/ai/conventions-proposal.md` | 네이밍·예외·DTO·트랜잭션·로깅·테스트·포맷, 항목마다 Before/After. 핵심 After 코드는 worktree 에서 실제 컴파일·실행 검증 | 4/4/4/4 | 4 |
| `docs/ai/ai-workflow.md` + `feature_list.example.json` | 작업 단위, 계획→구현→리뷰와 사람 확인 지점(H0~H3), feature_list 루프, Stop/SessionStart hook (20개 시나리오 실행 확인) | 4/4/4/4 | 4 |
| `docs/ai/examples/answer-scoring/` | 답안 채점 기능을 A′·B·C 로 재구성 (A′·B 는 실제 적용해 빌드·ArchUnit 확인, C 는 핵심부 컴파일·순환 없음 확인) | 4/4/4/4 | 4 |
| `docs/ai/decisions-needed.md` | 결정 13건, 중요도 순 | 4/4/4/4 (final-consistency) | 2 |
| `src/test/java/com/tmi/core/architecture/` | ArchUnit 규칙 8개 (R1~R7 + R4b). 위반 6개는 `@Disabled` + 사유·해소 작업 id | 4/4/4/4 (final-consistency) | 2 |
| `CLAUDE.md` | 확정 전 초안 (빌드·테스트 명령, 알려진 함정, `[제안]` 표시) | 4/4/4/4 (final-consistency) | 2 |
| `build.gradle` | ArchUnit core 1.5.1 (test), Spotless 8.10.4 + palantir-java-format (`enforceCheck = false` — 기존 코드 29파일 미포맷이라 build 에 묶지 않음) | — | — |

리뷰 파일은 `docs/ai/reviews/` 에 21개 있다. 5개 산출물과 최종 정합성 점검(나머지 4개 항목 묶음)마다 새 서브에이전트가 "까다로운 시니어 Spring 엔지니어" 역할로 리뷰했고, 리뷰어는 전용 git worktree 에서 프로브를 직접 실행했다.

## 리뷰 라운드별 주요 개선

### current-state (3/4/2/3 → 4/4/3/4 → 4/4/4/4)
- **R1**: OSIV 때문에 외부 호출 중 커넥션을 점유한다는 사실(C1). 이 사실은 원래 "충족된다" 고 잘못 결론 냈던 부분이다. 그 밖에 `RestClient.Builder` 빈 부재(Boot 4 모듈화), native enum 바인딩에서 SpEL 이 필요하다는 점(06 가이드 오류), `common ↔ infra` 순환, 03 계약 불일치를 찾아 반영했다.
- **R2**: R2 위반 19건을 현행 규칙 위반 10건과 B 기준 위반 9건으로 나눴다. R5 의 제네릭 반환 미탐지를 고치고 음성 대조로 확인했다. read timeout → 500 을 추가했다.
- **R3**: 해소 작업 id 를 연결했다. class 43개의 바이트코드가 동일한지 확인했다.

### architecture-proposal (3/3/2/3 → 3/3/3/3 → 4/3/3/4 → 4/4/4/4)
- **R1**: 단계 계획을 feature_list 와 1:1 로 맞췄다. 바뀌는 기존 문서 규칙을 명시했다. A 를 공정하게 다시 정의했고(A′), Modulith 관련 주장을 소스로 정정했다.
- **R2**: "A′ 는 기능 쌍마다 규칙이 필요" 라는 서술이 틀렸다는 지적을 받았다(리뷰어가 실행으로 반증). B 추천 근거를 "공개 표면의 크기와 diff 가시성" 으로 좁혔고, 산정식과 Toss auth 배치를 정리했다.
- **R3**: A′ 의 우회 방지 규칙 (b) 가 A′ 자신의 Toss 배치와 충돌했다. 규칙 (b) 를 다시 쓰고 "A′ 규칙 2개 / B 1개" 로 정정했다.

### conventions-proposal (4/3/3/4 → 4/4/3/4 → 4/4/3/4 → 4/4/4/4)
- **R1**: `BaseException` After 를 그대로 적용하면 404 에서 NPE 가 났다. 생성자 4개를 한 경로로 위임해 고쳤다. 상위 `handleExceptionInternal` 의 commit 검사와 500 속성 기록을 보존했고, Toss 예시를 설계 §6 에 맞췄다.
- **R2**: read timeout 예외 분류를 요청 방식별 실행 결과 표로 바꿨다(POST 는 `RestClientException`, GET 은 `ResourceAccessException`). Toss 토큰 단계를 분리했다.
- **R3**: 스케줄러가 raw 예외를 분류해도 되는 예외 규칙을 명시하고 `[B 전제]` 표시를 달았다.

### ai-workflow (3/3/2/3 → 3/3/4/3 → 3/3/4/3 → 4/4/4/4)
- **R1**: Stop hook 의 결함 5종(다른 id 의 기록, compact JSON, `HEAD~1` 등)을 고쳤다. verify 를 "수정 전에는 실패" 하도록 바꾸고 `plan_approved` 필드를 추가했다.
- **R2**: `stop_hook_active` 때문에 두 번째 종료 시도에서 hook 이 풀리던 문제, `--tests` 여러 개가 OR 로 묶여 verify 가 거짓 통과하던 문제, 계획 반려 시 교착이 생기던 문제를 고쳤다.
- **R3**: 클래스 단위 skip 검사가 항목 사이 교착을 만들던 것을 메서드 단위 필터로 해소했다. base 에 session_id 를 넣고, SessionStart 의 matcher 를 startup 으로 정하고 pull 을 추가했다.
- **R4**: 계획·구현·변경 요청별 브랜치 규칙을 정했다.

### final-consistency — research·decisions-needed·CLAUDE.md·ArchUnit·blockers (주요 7건 → 4/4/4/4)
- **R1**: FIX-F1T 안에 숨어 있던 D13 결정을 `FIX-F1S` 로 분리했다. 결정마다 빠져 있던 추가 의존(D3·D5·Docker)을 적었다. D5≠(a) 일 때 생기는 교착을 해소했다. research 의 낡은 주장 5건을 실행 근거로 갱신했다. CLAUDE.md 에 `[제안]` 표기와 정확한 README 경로를 넣었고, blockers 에 Docker 부재를 추가했다.
- **R2**: 경미 9건을 반영했다. DOC-1 의 간접 Docker 의존을 D6·D8 에 적었고, 시나리오 수의 출처와 주석 정합성을 맞췄다.

### examples (3/3/2/2 → 3/4/3/3 → 3/4/3/4 → 4/4/4/4)
- **R1**: A 예시에만 세 번째 catch 가 없어 timeout 이 500 이 되던 문제를 고쳤다. A 를 A′ 로 다시 썼다.
- **R2**: `.gitignore` 의 `out/` 규칙 때문에 C 예시 4파일이 커밋에서 빠져 있었다. C 를 순환 없는 표준형으로 교체했다. 관대한 clamp 대신 허용 오차 정책을 적용했다.
- **R3**: 문자열 `"NaN"` 응답이 200 으로 새던 결함을 고쳤다. 점수 정책은 소유 항목(`FIX-F1S`, D13)으로 분리했다.

## 검증

- `bash ./gradlew build` → **BUILD SUCCESSFUL** (로컬 PostgreSQL 16 `localhost:15432/tmi_word` + `docs/public/init.sql` 기준). 테스트는 9개다: 기존 `CoreApplicationTests` 1개 통과, ArchUnit 2개 통과, 6개는 `@Disabled`(알려진 위반).
- DB 없이 실행하면 기존 `CoreApplicationTests` 가 실패한다. 이 작업 이전부터 그랬다(blockers.md §2, decisions-needed D3).

## 참고 · 남은 것

- 수령 당시 작업 트리에 있던 미커밋 Toss WIP(`src/main` 4파일, 존재하지 않는 클래스를 import 해 컴파일 불가)는 커밋하지 않았다. `docs/ai/wip-snapshot.patch` 로 보존했고, 작업 트리에는 미커밋 상태로 복원했다(blockers.md §1, D9).
- remote 미설정·GH_TOKEN 무효로 push·PR 생성은 하지 못했다(blockers.md §4). 이 본문은 `docs/ai/pr-body.md` 에 있다.
- `gradlew` 가 실행 권한 없이 커밋돼 있어 `bash ./gradlew` 로 실행했다(D11).
- 이 클라우드 세션에는 Docker 데몬이 없어 Testcontainers 기반 검증은 하지 못했다(blockers.md §2 #4).

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01C2tq4KgE9RapbKVEFgcj95
