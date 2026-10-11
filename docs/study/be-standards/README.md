[← 학습 노트](../README.md)

# 국내 IT 기업 BE 관례 — 학습용 정리

> 조사 기준일 2026-10-11. 대상: Spring Boot 4.0.2 · Java 21 · Gradle · JPA/PostgreSQL · Lombok.

## 목적

이 프로젝트에서 기능을 하나씩 만들면서 **국내 유명 IT 회사(네이버, 카카오, LY, 쿠팡, 배민, 토스, 당근 등)에서도 통하는 BE 역량**을 쌓기 위한 기준표다.

- 각 문서는 "공개 근거로 본 관례(근거 등급 표시) / 갈리는 것 / 이 프로젝트에서 무엇을 할지"를 나눠 적는다.
- `docs/ai/`의 기존 제안과 비교해 **일치하는 점과 다른 점**을 표시했다.

### 근거를 읽는 법 (중요)

| 등급 | 뜻 |
|---|---|
| **[A]** 공식문서 확인 | 원문을 직접 열어 확인했다. Spring·Hibernate·HikariCP·PostgreSQL 문서, MySQL 소스, 네이버 GitHub 컨벤션 등 |
| **[A-기업]** 기업 실무 흔적 | 국내 기업이 **공식 GitHub에 공개한 원문**(README, 설정 파일)을 열어 확인했다. 그 회사가 실제로 쓰는 기술 구성을 보여 주지만, 사내 표준 문서는 아니다. 예: `naver/fixture-monkey`, `line/line-fido2-server` |
| **[B]** 교육과정 확인 | 우아한테크코스·Tecoble 원문을 확인했다. 회사 실무 표준이 아니다 |
| **[C]** 사례 미확인 | 기업 블로그 원문을 열지 못했다. 검색 발췌만 근거다. "국내 기업이 이렇게 한다"는 증거로 쓰지 말고, 직접 읽어 보고 판단할 것 |
| **[D]** 추론 | 위 근거에서 끌어낸 판단이다. 출처에 그대로 적힌 문장이 아니다 |
| (estimated) | 근거가 약한 추정이다 |

> **한계: 국내 기업 기술블로그 원문은 0건 확인했다.** 조사 세션과 2026-10-11 재검증 세션 모두에서 블로그 도메인이 막혔다. 재검증 때 우선 대상 5개를 직접 열어 봤으나 전부 실패했다(아래 표). 웹 검색도 한도(200회)를 다 써서 실행되지 않았다. 그래서 01(모듈러 모놀리스), 03(로그 레벨·REST Docs), 04(트랜잭션 밖 외부 호출·락), 06(배포·캐시·Kafka)의 "국내 관례"는 모두 **[C] 사례 미확인**이다. "국내 유명 기업에서 통하는가"는 아직 검증되지 않았다. 국내 기업 근거로 확인된 것은 GitHub 원문 [A-기업]뿐이다(07 §2.1).
>
> | 우선 대상 (재검증) | 쓰인 곳 | 결과 |
> |---|---|---|
> | tech.kakaopay.com/post/home-hexagonal-architecture | 01 | ENOTFOUND |
> | tech.kakaobank.com/posts/2507-legacy-to-modular-monolith-with-spring-modulith | 01 | ENOTFOUND |
> | techblog.woowahan.com/2663 (HikariCP) | 04 | ENOTFOUND, 프록시 CONNECT 403 |
> | helloworld.kurly.com/blog/distributed-redisson-lock | 04 | ENOTFOUND |
> | toss.tech/article/test-strategy-server | 05 | ENOTFOUND, 프록시 CONNECT 403 |

### 사람이 확인할 것

1. **블로그 접속 환경 확보.** 위 도메인에 접속되는 환경(DNS·프록시 정상)이나 검색 한도(`CLAUDE_CODE_MAX_WEB_SEARCHES_PER_SESSION`)를 늘린 후속 세션에서 위 5개부터 연다. 열어 본 글은 [C] → [A-기업]으로 올리고, 끝내 못 열면 그 행을 지운다.
2. **대안 출처 지정.** 블로그 접근이 끝내 불가능하면, 근거로 삼을 국내 기업 공식 GitHub 저장소를 사람이 직접 골라 준다 (예: `kakao/`, `woowabros/`, `toss/`, `naver/`, `line/` 조직의 Spring 서버 저장소). 에이전트가 임의로 고르면 "그 회사의 대표 관례"인지 판단할 수 없다.
3. 그 전까지 [C] 행은 "후보 관례"로만 읽는다.

각 문서 머리말에는 이 절을 가리키는 한 줄과, 그 문서에만 해당하는 한계만 적는다.

## 읽는 순서

| 순서 | 문서 | 다루는 것 |
|---|---|---|
| 1 | [01-project-structure.md](./01-project-structure.md) | 패키지 구조, 레이어드·헥사고날, 멀티모듈, 모듈러 모놀리스, MSA |
| 2 | [02-coding-convention-and-git.md](./02-coding-convention-and-git.md) | 코드 스타일, 네이밍, 포매터, 브랜치, 커밋, PR, 리뷰 |
| 3 | [03-api-error-logging.md](./03-api-error-logging.md) | REST 관례, 에러 응답, 예외 처리, 로그, API 문서, 외부 호출 |
| 4 | [04-jpa-transaction.md](./04-jpa-transaction.md) | 엔티티, N+1, 트랜잭션 경계, OSIV, 락, 커넥션 풀, 마이그레이션 |
| 5 | [05-testing.md](./05-testing.md) | 테스트 피라미드, 슬라이스, Testcontainers, 격리, Mock, 컨텍스트 캐싱 |
| 6 | [06-operations-and-career.md](./06-operations-and-career.md) | 관측(메트릭·트레이스), 배포, Redis, Kafka, 장애 대응, 채용 요구 역량 |
| 7 | [07-security-and-db.md](./07-security-and-db.md) | Spring Security·인증 방식, 비밀번호 해시, CORS, 로그 마스킹, 설정·시크릿, MySQL vs PostgreSQL, EXPLAIN |

01 → 04 → 05는 이어서 읽으면 좋다. 지금 저장소의 결함(F4 트랜잭션, F6 기능 간 의존)이 이 세 문서에 걸쳐 있다. 07은 인증 기능을 만들기 전에 읽는다.

## 한 장 요약

| 주제 | 핵심 | 등급 |
|---|---|---|
| 01 구조 | 단일 배포 + 기능 패키지 + 기능 안 레이어드로 시작. 경계는 테스트(ArchUnit, Spring Modulith)로 강제 | A (Spring 문서·Modulith 규칙). 국내 모듈러 모놀리스 사례는 C |
| 01 구조 | 헥사고날·멀티모듈은 실행 단위가 여러 개일 때 도입 | C + D |
| 02 컨벤션 | 공개된 국내 Java 스타일(네이버, 우테코)은 4칸 폭 + 120자. 포매터·CI로 자동 강제 | A (네이버 GitHub) + B (우테코) |
| 02 Git | 작은 PR, 빠른 리뷰, 한 계열 커밋 형식, Squash | B + A(Google). 기업 사례는 C |
| 03 API·에러 | 의미에 맞는 HTTP 상태 + 일관된 에러 본문, `ResponseEntityExceptionHandler` | A |
| 03 로그 | 4xx는 INFO, 5xx는 ERROR + 스택, 로그는 한 곳에서 한 번 | **D** (C의 레벨 정의에서 추론) |
| 03 외부 호출 | 타임아웃 필수, 타임아웃은 결과 미상. `@Retryable`은 대상 예외 지정 | A (Spring Resilience) + C + D |
| 04 JPA | 트랜잭션은 짧게, 외부 호출은 트랜잭션 밖. OSIV off + 서비스에서 DTO 변환 | A (Spring 동작) + B. 기업 사례는 C |
| 04 엔티티 | setter 없는 엔티티, LAZY + fetch join/batch size | B (우테코·강의 계보) |
| 05 테스트 | 운영과 같은 DB를 Testcontainers로, 컨텍스트 재생성 방지, 인수는 성공 시나리오만 | A + B |
| 06 운영 | Micrometer/Actuator로 계측, 백엔드는 회사마다 다름. Boot 기본값(health만 노출, 샘플링 10%) | A. 기업별 도구는 C |
| 06 채용 | 필수 Java/Kotlin + Spring + JPA + RDBMS, 우대 트래픽·Redis·Kafka·모니터링 | C (공고 발췌) |
| 07 보안 | Security 필터 체인, 적응형 비밀번호 해시, CORS는 Security 앞, 시크릿은 환경변수 | A |
| 07 DB | MySQL 기본 RR(+갭 락), PostgreSQL 기본 RC. `EXPLAIN ANALYZE`로 확인 | A (MySQL 소스로 갭 락·`GET_LOCK` 확인) |
| 07 기업 흔적 | LINE FIDO2 서버: Spring Boot + JPA, 로컬 H2 / 운영 MySQL, 멀티모듈. 네이버: Fixture Monkey(테스트), Spring JDBC Plus | A-기업 (GitHub README·설정 원문) |

## 기존 `docs/ai` 제안과 이번 조사 비교

| 항목 | 기존 제안 | 이번 조사 근거 (등급) | 관계 |
|---|---|---|---|
| 구조 | [architecture-proposal](../../ai/architecture-proposal.md) 추천 **B**: 기능 모듈 + base package만 공개 API, 내부 레이어드 | Spring Modulith 공식 규칙(루트 직속 = 모듈, 루트만 API)과 같은 모양 [A, 확인]. 국내 사례 카카오뱅크·여기어때는 사례 미확인 [C] | **일치** |
| 헥사고날 | C 비추천 (02 §2.2에서 되돌림) | 카카오페이 홈 회고(적용 후 걷어냄) [C] | **일치** |
| Modulith | 확장 가능성으로 언급 | Modulith 2.x가 Boot 4 세대. 학습용으로 `verify()`를 시험할 가치가 있음 | 이번 조사가 더 적극적 |
| 포맷 | [conventions-proposal](../../ai/conventions-proposal.md) §7: Spotless + palantir, D5 후 강제 | 4칸 + 120자, 도구로 강제 | **일치** |
| import, Checkstyle, Git·커밋·PR | 범위 밖 | Google식 import, Checkstyle은 의미 규칙만, GitHub flow + Conventional Commits + Squash | 추가 |
| 에러 응답 (D2) | (a) 현행 형식 유지 + `ResponseEntityExceptionHandler` | Spring 공식 기반 클래스 [A]. ProblemDetail의 국내 채택 근거는 찾지 못함 | **일치** |
| 로그 레벨 | 4xx INFO / 5xx ERROR, 예외 로그 1회 | 컬리·LY 레벨 정의[C]에서 끌어낸 추론 [D] | **일치** |
| 트랜잭션·OSIV (D4) | 다른 빈의 public 메서드, 외부 호출은 트랜잭션 밖, OSIV off | Spring 공식 동작 [A], 카카오페이 사례 [C] | **일치** |
| 마이그레이션 | `docs/public/05` §3.7: **Flyway 미도입** (팀 결정) | 우테코 팀들은 Flyway + `ddl-auto=validate` + CI 가드 [B] | **다름**. 팀 결정을 유지하되 별도 실습 과제로 둔다 |
| 테스트 | §6: T1~T5 (Docker 없이 `test`, `@Tag("integration")`, Boot 4 API) | Testcontainers, 컨텍스트 캐싱, TRUNCATE 격리 | 일치 + 보완 |
| 재시도 | Toss 재시도 정책 위주 | Spring 7 `@Retryable`은 기본이 모든 예외 재시도. 서킷브레이커는 Resilience4j | 보완 |
| `RestClient.Builder` (D7) | 의존성 추가 여부 문제 | 트레이스 전파 조건이기도 함 | 근거 추가 |
| 운영·관측 | 범위 밖 | actuator → Prometheus/Grafana → OTel [A] | 추가 |
| 인증·시크릿·DB 차이 | 범위 밖 (03 API 명세: MVP 인증 없음) | Spring Security, 프로필·환경변수, MySQL vs PostgreSQL, EXPLAIN [A] | 추가 (07) |

## 이 프로젝트에서 순서대로 연습할 역량 로드맵

각 단계는 **기능 하나 또는 리팩터링 하나**다. 앞 단계의 테스트가 다음 단계의 안전망이 된다. `[결정]` 표시는 `docs/ai/decisions-needed.md`의 사람 결정이 먼저 필요하다는 뜻이다.

| 단계 | 할 일 (이 저장소 기준) | 선행 | 익히는 역량 | 문서 |
|---|---|---|---|---|
| 0 | `.editorconfig`, `.gitattributes`(`* text=auto eol=lf`) 추가. 브랜치·커밋·PR 규칙. `SIG-2`: `spotlessApply` 단독 커밋 `[결정 D5]` | — | 포매터와 CI 강제, Git 협업 | [02](./02-coding-convention-and-git.md) |
| 0b | `TEST-1`: DB 없이 도는 테스트 3종. 지금 실패하는 2개는 `@Disabled("FIX-F1 …")`·`@Disabled("FIX-F2 …")`로 추가 | — | 실패 테스트 먼저 쓰기 | [05](./05-testing.md) |
| 1 | `FIX-F1`: `SimilarityClient.score`에 `@RequestBody` → **R7 해제** | TEST-1 | HTTP Interface 바인딩 | [03](./03-api-error-logging.md) |
| 1b | `FIX-F1T`: `AnswerService` catch 3단 — read timeout·빈 응답 → 503 (`SimilarityTimeoutTest`) | FIX-F1 | 타임아웃 = 결과 미상, 예외 → 상태 매핑 | [03](./03-api-error-logging.md) |
| 2 | `SIG-1`: `CoreApplicationTests`를 `@Tag("integration")` + Testcontainers로 분리 `[결정 D3]`. 이어서 `Grade` 단위, `@WebMvcTest`, `@DataJpaTest` | — | 테스트 피라미드, Boot 4 테스트 API | [05](./05-testing.md) |
| 2b | `FIX-F2`: `spring.messages.basename=messages/messages` + `fallback-to-system-locale=false` (영문 메시지) | TEST-1 | 메시지 소스, 설정 바인딩 | [03](./03-api-error-logging.md) |
| 3 | `FIX-F3`: `GlobalExceptionHandler extends ResponseEntityExceptionHandler`, 4xx INFO / 5xx ERROR `[결정 D2]` | FIX-F2, SIG-2 | 예외 처리 경로, HTTP 상태 의미 | [03](./03-api-error-logging.md) |
| 4a | `ARCH-A1`: 클라이언트 빈 설정을 `common/config/RestClientConfig`에서 소유 모듈로 이동 → **R1·R3 해제** | FIX-F1 | 순환 의존 끊기 | [01](./01-project-structure.md) |
| 4b | `ARCH-B2` → `ARCH-B3`: `similarity` 모듈 승격(공개 API `SimilarityScorer`), `SentenceReader`·`SentenceDetail` → **R2 해제** `[결정 D1, D3]` ¹ | ARCH-A1, FIX-F1T, SIG-1 | 모듈 경계, 공개 API 설계 | [01](./01-project-structure.md) |
| 5 | `FIX-R5`: 서비스가 DTO 반환 → **R5 해제**. 이어서 `FIX-F4`: 조회를 다른 빈으로, OSIV off → **R6 해제** `[결정 D4]` (F4는 Docker 필요) | R5: TEST-1 / F4: FIX-R5, SIG-1 | 트랜잭션 프록시, OSIV | [04](./04-jpa-transaction.md) |
| 6 | 타임아웃 프로퍼티(`spring.http.serviceclient.*`), 멱등 GET에만 `@Retryable` | FIX-F1T | 타임아웃 설정, 재시도 조건 | [03](./03-api-error-logging.md) |
| 7 | `application-local/prod.properties` 분리, `${DB_URL:...tmi_word}`·`${DB_PASSWORD:...}` ² | — | 프로필, 환경변수, 시크릿 | [07](./07-security-and-db.md) |
| 8 | EXPLAIN 실습: `EXPLAIN (ANALYZE, BUFFERS)`로 `findRandomBy`의 `ORDER BY RANDOM()` 비용, `idx_sentence_filter` 사용 확인 | — | 실행 계획, 인덱스 | [07](./07-security-and-db.md) |
| 9 | 신규 기능: 답안 이력 저장(쓰기 + 목록). 커서 페이징, N+1, `default_batch_fetch_size` | 5 | 엔티티 설계, N+1, 인수 테스트 | [04](./04-jpa-transaction.md), [05](./05-testing.md) |
| 10 | 인증: Spring Security 세션 → JWT 비교. BCrypt, CORS, 401·403 형식 ³ | 9 | 필터 체인, 인증 방식 선택 | [07](./07-security-and-db.md) |
| 11 | 동시성: `@Version`과 `PESSIMISTIC_WRITE`, `CountDownLatch` 테스트. 가능하면 MySQL 컨테이너 비교 | 2 | 락, 격리 수준 | [04](./04-jpa-transaction.md), [07](./07-security-and-db.md) |
| 12 | 관측 1: actuator + Prometheus + Grafana, requestId MDC, 커스텀 메트릭 | — | 메트릭, p95/p99, MDC | [06](./06-operations-and-career.md) |
| 13 | CI: GitHub Actions에서 `./gradlew build` | SIG-1 | CI 파이프라인 | [06](./06-operations-and-career.md) |
| 14 | k6 부하 테스트 → Redis 또는 Caffeine 캐시 → 전후 비교 | 12 | 부하 테스트, 캐시 무효화 | [06](./06-operations-and-career.md) |
| 15 | 답안 이력 저장 시 outbox → Kafka 릴레이, 멱등 컨슈머 | 9 | Outbox, Kafka 순서·중복 | [06](./06-operations-and-career.md) |
| 16 | OpenTelemetry 트레이싱, 자동 구성 `RestClient.Builder` `[결정 D7]` | 12 | 분산 추적, 전파 조건 | [06](./06-operations-and-career.md) |
| 선택 | Spring Modulith `verify()`, REST Docs 비교, Flyway 별도 실습, 기능 1개 Kotlin | — | 도구 비교와 선택 근거 | 01, 03, 04, 06 |

- D13 결정 후 `FIX-F1S`(점수 범위 정책, 선행 FIX-F1T), `FIX-F3`·`FIX-F4` 뒤 `DOC-1`(docs/public 수정, 사람 판정). **전체 의존은 [architecture-proposal §4.3](../../ai/architecture-proposal.md) 표가 기준**이다. 6단계 이후의 선행은 `docs/ai` 의존이 아니라 학습 순서 제안이다 (estimated).
- ¹ D1=B 기준. D1=A′이면 4b 대신 `ARCH-A2`(선행 ARCH-A1, FIX-F1T)에서 R2를 A′ 규칙으로 교체한다. `FIX-F1T`의 catch는 4b에서 `SimilarityScorer`로 옮겨진다.
- ² `docs/public/07` 예시의 DB 이름 `tmi`와 코드의 `tmi_word`가 다르다. 사람 확인.
- ³ 사람 결정 필요: 03 API 명세 §1.2 "인증 없음"을 바꿔야 한다.

작업 ID(`FIX-*`, `ARCH-*`, `SIG-*`, `TEST-*`)와 결정(`D*`)은 `docs/ai/decisions-needed.md`를 따른다. 1~5단계가 끝나면 ArchUnit R1~R7(+R4b)이 모두 `@Disabled` 없이 통과해야 한다 (D1=B 기준. A′이면 R2는 교체된 규칙으로 통과).

단계를 마칠 때마다 확인할 것:

1. 무엇을 바꿨고 **왜** 그렇게 골랐는지, 다른 선택지와 비교해 말할 수 있는가 (면접 대비).
2. 그 결정을 지키는 테스트나 도구 설정이 있는가.
3. 배운 개념을 `docs/study/<주제>/`에 노트로 남겼는가 (`docs/study/README.md` 형식).
