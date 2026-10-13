[← 학습 노트](../README.md)

# 국내 IT 기업 BE 관례 — 학습용 정리

> 조사 기준일 2026-10-12. 대상: Spring Boot 4.0.2 · Java 21 · Gradle · JPA/PostgreSQL · Lombok.

## 목적

이 프로젝트에서 기능을 하나씩 만들면서 **국내 유명 IT 회사(네이버, 카카오, LY, 쿠팡, 배민, 토스, 당근 등)에서도 통하는 BE 역량**을 쌓기 위한 기준표다.

- 각 문서는 "공개 근거로 본 관례(근거 등급 표시) / 갈리는 것 / 이 프로젝트에서 무엇을 할지"를 나눠 적는다.
- `docs/ai/`의 기존 제안과 비교해 **일치하는 점과 다른 점**을 표시했다.

### 근거를 읽는 법 (중요)

| 등급 | 뜻 |
|---|---|
| **[A]** 공식 문서 | 원문을 직접 열어 확인했다. Spring·Hibernate·HikariCP·PostgreSQL·MySQL 문서와 소스, 네이버 GitHub 컨벤션 등 |
| **[A-기업]** 기업 공개 저장소 | 국내 기업 공식 GitHub의 README·설정 원문. 사내 표준 문서는 아니다 (07 §2.2) |
| **[B]** 교육과정·공개 저장소 | 우아한테크코스·Tecoble 원문. 회사 실무 표준이 아니다 |
| **[C✓]** 기업 글 원문 확인 | 국내 기업 기술블로그·발표·채용 공고 원문을 열어 읽었다. 각 문서 출처 표에 URL과 연도가 있다 |
| **[C?]** 기업 글 미확인 | 원문을 열지 못했다. 각 문서 맨 아래 접기 안에만 둔다. 근거로 쓰지 않는다 |
| **[D]** 추론 | 위 근거에서 끌어낸 판단이다. 출처에 그대로 적힌 문장이 아니다 |

**"국내 공통"** 은 서로 다른 2개 이상 기업의 [C✓]가 있을 때만 쓴다. 1개 기업이면 **"○○ 사례"** 로 쓴다.
기업 수 세는 규칙: 같은 기술블로그를 쓰는 계열사는 1개로 센다. 토스·토스뱅크·토스페이먼츠·토스증권은 "토스", 카카오페이·카카오페이손해보험·카카오페이증권은 "카카오페이"다. 카카오, 카카오뱅크, 카카오페이는 블로그와 법인이 따로라 별개로 센다. 배민은 우아한형제들이다.

읽을 때 주의: 기업 글 대부분은 **회사 전체가 아니라 한 팀의 사례**다. 각 문서 본문에 팀명을 적어 두었다. "국내 공통"도 **이 문서가 원문을 확인한 기업들 사이의 공통**이지 업계 전체 통계가 아니다.

### 원문을 확인한 기업 (2026-10-12)

토스, 카카오페이, 카카오뱅크, 카카오, 네이버, LY/LINE, 컬리, NHN, G마켓, 뱅크샐러드, 하이퍼커넥트, 화해, 플렉스, 올리브영, 사람인, 쏘카, **우아한형제들(배민)**, **당근**.

- 배민: `techblog.woowahan.com`은 막혀 있지만, 2016~2021년 글은 같은 회사의 **구 블로그 `woowabros.github.io`에 그대로 남아 있다**(글 231건). 이번에 15건을 열어 읽었다(분산 락, HikariCP, 멀티모듈, Feign, Hystrix, REST Docs, 배치, 샤딩·라우팅, 코드 리뷰 등).
- 당근: `about.daangn.com/blog` 글 본문(사이트가 쓰는 CMS 공개 API로 확인)과 `careers.daangn.com` 채용 공고.
- 채용 공고: 토스(`toss.im/career`가 부르는 공개 JSON), 당근. 06 §2.3에 반영했다.

<details>
<summary>열지 못한 곳과 시도한 대체 경로</summary>

| 대상 | 시도 | 결과 |
|---|---|---|
| `techblog.woowahan.com` (배민 2021년 이후 글) | 구 블로그 `woowabros.github.io` 목록 231건 | 2021-06까지만 있다. 서킷브레이커(2024), PR 300줄, Pn 룰, Kafka 글은 없음 → [C?] |
| 〃 | speakerdeck 검색(`우아한형제들`, `woowahan`, `배달의민족`, `우아콘`) | 검색 결과가 스크립트로 그려져 목록을 못 받음. `speakerdeck.com/woowahan` 등 계정 404 |
| 〃 | YouTube `@woowatech` 검색 결과의 설명란 | 제목·설명 첫 줄만 보임(영상 페이지는 403). MSA 여행기(2020, `BnS6343GTkY`)만 01에 인용. 분산 락(2024, `3HCVD26zycM`), Kafka(2022~2025) 발표는 제목만 확인 |
| 〃 | 우테코 GitHub | 교육 저장소라 회사 관례 근거가 아님 |
| 당근 Medium (`medium.com/daangn`) | `about.daangn.com/blog` | 대부분 인터뷰. Pn 룰·gRPC·언어 다양성은 확인. MSA·Kafka는 채용 공고 기술 스택으로만 확인, 상세 글 없음 |
| 〃 | YouTube `@daangnteam` 검색 | "Kafka 뉴비의 마이그레이션"(2024, `zheA2qdyCnM`) 제목만 |
| 쿠팡 (`medium.com/coupang-engineering`, `coupang.jobs`) | GitHub `coupang`, YouTube | 모두 403 또는 찾지 못함 |
| 무신사 (`medium.com/musinsa-tech`) | `techblog.musinsa.com`, `musinsa.tech` | 403 또는 무관한 사이트 |
| 여기어때 (`techblog.gccompany.co.kr`) | — | 403 |
| `tech.kakao.com` | 1회 재시도 | 열림. 본문은 HTML 내장 JSON에 있다 |

제3자 프록시·아카이브는 쓰지 않았다.

</details>

## 읽는 순서

| 순서 | 문서 | 다루는 것 |
|---|---|---|
| 1 | [01-project-structure.md](./01-project-structure.md) | 패키지 구조, 레이어드·헥사고날, 멀티모듈, 모듈러 모놀리스, MSA |
| 2 | [02-coding-convention-and-git.md](./02-coding-convention-and-git.md) | 코드 스타일, 네이밍, 포매터, 브랜치, 커밋, PR, 리뷰 |
| 3 | [03-api-error-logging.md](./03-api-error-logging.md) | REST 관례, 에러 응답, 예외 처리, 로그, API 문서, 외부 호출 |
| 4 | [04-jpa-transaction.md](./04-jpa-transaction.md) | 엔티티, N+1, 트랜잭션 경계, OSIV, 락, Read/Write 분리, 배치, 커넥션 풀 |
| 5 | [05-testing.md](./05-testing.md) | 테스트 피라미드, 슬라이스, Testcontainers, 격리, Mock, 컨텍스트 캐싱 |
| 6 | [06-operations-and-career.md](./06-operations-and-career.md) | 관측, 배포, Redis, Kafka, 장애 대응, 채용 요구 역량 |
| 7 | [07-security-and-db.md](./07-security-and-db.md) | 인증, 비밀번호 해시, CORS, 로그 마스킹, 시크릿, MySQL vs PostgreSQL, EXPLAIN, 분산 락 |

01 → 04 → 05는 이어서 읽으면 좋다. 지금 저장소의 결함(F4 트랜잭션, F6 기능 간 의존)이 이 세 문서에 걸쳐 있다. 07은 인증 기능을 만들기 전에 읽는다.

## 한 장 요약

| 주제 | 핵심 | 근거 |
|---|---|---|
| 01 구조 | 단일 배포 + 기능(관심사) 패키지 + 기능 안 레이어드로 시작 | A + 카카오페이 사례 C✓ |
| 01 구조 | 기능 사이 경계는 테스트(ArchUnit, Modulith `verify()`)로 강제 | A + **국내 공통** C✓ |
| 01 구조 | 헥사고날·멀티모듈은 코어를 여러 실행 단위가 공유할 때만. `common`에 기능을 쌓지 않는다 | **국내 공통** C✓ |
| 01 구조 | 모듈러 모놀리스(Spring Modulith) | 카카오뱅크 사례 C✓ |
| 02 컨벤션 | 스타일 숫자는 회사마다 다르다. 공통점은 포매터·린터·CI로 강제하는 것 | **국내 공통** C✓ |
| 02 Git·PR | 작은 PR, PR 템플릿, 리뷰어 자동 배정, 코멘트 강도 표시, 이슈 번호 | **국내 공통** C✓ |
| 03 API·에러 | 의미에 맞는 HTTP 상태 + `{code, message}` 본문 | A + **국내 공통** C✓ (공개 API 문서 기준) |
| 03 API 문서 | 테스트로 만드는 Spring REST Docs (LY는 springdoc) | **국내 공통** C✓ |
| 03 로그 | ERROR = 즉시 대응, WARN = 빈도 알림. 4xx를 INFO로 둘지는 추론 | 컬리 사례 C✓ + D |
| 03 외부 호출 | 타임아웃 필수, 타임아웃은 "결과 미상". 멱등이면 재요청, 아니면 조회·취소. 서킷브레이커로 격리 | A + **국내 공통** C✓ |
| 04 트랜잭션 | 외부 호출은 트랜잭션 밖, OSIV off + 서비스에서 DTO 변환 | A + 카카오페이 사례 C✓ |
| 04 트랜잭션 | OSIV가 켜져 있으면 커넥션이 요청 끝까지 잡힌다 | A + **국내 공통** C✓ |
| 04 데이터 | readOnly → 레플리카 라우팅(`LazyConnectionDataSourceProxy`), 배치는 no-offset 리더 | 배민 사례 C✓ / **국내 공통** C✓ |
| 04·07 락 | 여러 서버의 동시성은 Redis 분산 락(배민은 MySQL `GET_LOCK`), 돈·재고 행은 DB 락 병행 | **국내 공통** C✓ |
| 04 엔티티 | setter 없는 엔티티, LAZY + fetch join, `default_batch_fetch_size` | B (N+1 대응은 C✓ 2개사) |
| 05 테스트 | 운영과 같은 DB를 Testcontainers로 (토스는 H2 반례) | A + B + **국내 공통** C✓ |
| 05 테스트 | Mock 빈 조합을 통일해 컨텍스트 재생성을 막는다 | A + **국내 공통** C✓ |
| 06 운영 | Micrometer 계측 + Prometheus/Grafana. 점진 배포 + 롤백 기준 지표 | A + **국내 공통** C✓ |
| 06 운영 | Look-aside 캐시 + 장애 시 DB 우회, Outbox, 같은 키 같은 파티션, 멱등 컨슈머 | **국내 공통** C✓ |
| 06 채용 | 필수 JVM 언어 + Spring + RDBMS, 우대 Redis·Kafka·대규모 트래픽·장애 대응·MSA | **국내 공통** C✓ (토스·당근 공고 2026-10) |
| 07 보안 | Security 필터 체인, 적응형 비밀번호 해시, CORS는 Security 앞 | A |
| 07 시크릿 | 시크릿은 저장소 밖(환경변수, Vault, 클라우드 저장소)에서 주입 | A + **국내 공통** C✓ |
| 07 DB | 국내 운영 DB는 MySQL이 흔하다. MySQL 기본 RR, PostgreSQL 기본 RC | A + **국내 공통** C✓ |

**반영 범위**: 쿠팡·무신사 관례는 이 문서에 **반영되지 않았다**. 배민은 2016~2021년 구 블로그 글과 발표 제목까지, 당근은 회사 블로그 인터뷰와 채용 공고까지만 반영했다. "국내 공통"을 업계 전체로 읽지 않는다. 막힌 글은 브라우저로 직접 열 수 있다(각 문서 맨 아래 접기에 목록).

## 기존 `docs/ai` 제안과 이번 조사 비교

| 항목 | 기존 제안 | 이번 조사 근거 | 관계 |
|---|---|---|---|
| 구조 | [architecture-proposal](../../ai/architecture-proposal.md) 추천 **B**: 기능 모듈 + base package만 공개 API | Modulith 공식 규칙과 같은 모양 [A]. 카카오페이 관심사 패키징 [C✓] | **일치** |
| 헥사고날 | C 비추천 | 카카오페이 홈이 적용 후 걷어냄 [C✓]. 코어를 여러 실행 단위가 공유할 때만 쓴다는 것이 국내 공통 | **일치** |
| Modulith | 확장 가능성으로 언급 | 카카오뱅크가 `verify()`로 경계 강제 [C✓] | 이번 조사가 더 적극적 |
| 포맷 | [conventions-proposal](../../ai/conventions-proposal.md) §7: Spotless, D5 후 강제 | 도구 강제는 국내 공통 [C✓]. 숫자는 회사마다 다름 | **일치**. 강제 시점은 앞당길 근거 (02 §4) |
| Git·PR·리뷰 | 범위 밖 | 작은 PR, PR 템플릿, 리뷰어 자동 배정 등 국내 공통 [C✓] | 추가 |
| 에러 응답 (D2) | (a) 현행 형식 유지 + `ResponseEntityExceptionHandler` | `{code, message}`는 국내 공개 API 문서 공통 [C✓], 사내 호출은 카카오페이 사례. ProblemDetail 국내 채택 근거 없음 | **일치** |
| 로그 레벨 | 4xx INFO / 5xx ERROR | 컬리 사례 [C✓]: 사용자 오류는 WARN + 빈도 알림. LY는 원인 위치(외부/내부)로 나눈다 | **대체로 일치** |
| 트랜잭션·OSIV (D4) | 외부 호출은 트랜잭션 밖, OSIV off | Spring 동작 [A], 카카오페이 사례 [C✓], OSIV 커넥션 함정은 국내 공통 | **일치** |
| 마이그레이션 | `docs/public/05` §3.7: **Flyway 미도입** (팀 결정) | 우테코 팀 [B], LINE 사례 [C✓] | **다름**. 팀 결정을 유지하고 별도 실습으로 둔다 |
| 테스트 | T1~T5 (Docker 없이 `test`, `@Tag("integration")`) | Testcontainers·컨텍스트 재사용 국내 공통 [C✓] | 일치 + 보완 |
| 재시도 | Toss 재시도 정책 위주 | 타임아웃 = 결과 미상 국내 공통 [C✓]. 재시도는 대상을 좁혀 켠다(배민) | 보완 |
| `RestClient.Builder` (D7) | 의존성 추가 여부 문제 | 트레이스 전파 조건이기도 함 [A] | 근거 추가 |
| 운영·관측·채용 | 범위 밖 | Prometheus/Grafana, 점진 배포, 공고 요구 역량 국내 공통 [C✓] | 추가 (06) |
| 인증·시크릿·DB | 범위 밖 (MVP 인증 없음) | 시크릿 외부 저장소·MySQL 운영·분산 락 국내 공통 [C✓] | 추가 (07) |

## 이 프로젝트에서 순서대로 연습할 역량 로드맵

각 단계는 **기능 하나 또는 리팩터링 하나**다. 1~6단계는 [decisions-needed §0](../../ai/decisions-needed.md)의 "결정 없이 바로 해도 되는 것" 순서와 같다. `[결정]` 표시는 사람 결정이 먼저 필요하다는 뜻이다.

| 단계 | 할 일 | 선행 | 익히는 역량 | 문서 |
|---|---|---|---|---|
| 0 | `.editorconfig`, `.gitattributes`, 브랜치·커밋·PR 템플릿 규칙 | — | Git 협업 | [02](./02-coding-convention-and-git.md) |
| 1 | `TEST-1`: DB 없이 도는 테스트 3종. 실패하는 2개는 FIX id를 단 `@Disabled` | — | 실패 테스트 먼저 쓰기 | [05](./05-testing.md) |
| 2 | `FIX-F1`: `SimilarityClient.score`에 `@RequestBody` → R7 해제 | TEST-1 | HTTP Interface 바인딩 | [03](./03-api-error-logging.md) |
| 3 | `FIX-F1T`: read timeout·빈 응답 → 503 | FIX-F1 | 타임아웃 = 결과 미상 | [03](./03-api-error-logging.md) |
| 4 | `FIX-F2`: 메시지 basename + `fallback-to-system-locale=false` | TEST-1 | 메시지 소스 | [03](./03-api-error-logging.md) |
| 5 | `FIX-R5`: 서비스가 DTO 반환 → R5 해제 | TEST-1 | 계층 경계 | [04](./04-jpa-transaction.md) |
| 6 | `ARCH-A1`: Similarity 빈 설정을 `common/config` 밖으로 → R1·R3 해제 | FIX-F1 | 순환 의존 끊기 | [01](./01-project-structure.md) |
| 7 | `SIG-2`: `spotlessApply` 단독 커밋 `[결정 D5]` | — | 포매터 CI 강제 | [02](./02-coding-convention-and-git.md) |
| 8 | `SIG-1`: 통합 테스트를 `@Tag("integration")` + Testcontainers로 `[결정 D3]` | — | 테스트 피라미드 | [05](./05-testing.md) |
| 9 | `FIX-F3`: `ResponseEntityExceptionHandler` 상속 `[결정 D2]` | FIX-F2, SIG-2 | 예외 → 상태 매핑 | [03](./03-api-error-logging.md) |
| 10 | `ARCH-B2` → `ARCH-B3`: `similarity` 모듈 승격 → R2 해제 `[결정 D1, D3]` ¹ | ARCH-A1, FIX-F1T, SIG-1 | 모듈 공개 API | [01](./01-project-structure.md) |
| 11 | `FIX-F4`: 조회를 다른 빈으로, OSIV off → R6 해제 `[결정 D4]` | FIX-R5, SIG-1 | 트랜잭션 프록시, OSIV | [04](./04-jpa-transaction.md) |
| 12 | 타임아웃 프로퍼티, 멱등 호출에만 `@Retryable` | FIX-F1T | 재시도 조건 | [03](./03-api-error-logging.md) |
| 13 | 프로필 분리, DB 접속 정보 환경변수화 ² | — | 프로필, 시크릿 | [07](./07-security-and-db.md) |
| 14 | `EXPLAIN (ANALYZE, BUFFERS)`로 `findRandomBy` 비용 확인 | — | 실행 계획, 인덱스 | [07](./07-security-and-db.md) |
| 15 | 신규 기능: 답안 이력 저장 + 커서 페이징, N+1 확인 | 11 | 엔티티 설계, 인수 테스트 | [04](./04-jpa-transaction.md), [05](./05-testing.md) |
| 16 | 인증: Security 세션 → JWT 비교, BCrypt, CORS ³ | 15 | 필터 체인 | [07](./07-security-and-db.md) |
| 17 | 동시성: `@Version`과 `PESSIMISTIC_WRITE`, 동시 요청 테스트 | 8 | 락, 격리 수준 | [04](./04-jpa-transaction.md), [07](./07-security-and-db.md) |
| 18 | 관측: actuator + Prometheus + Grafana, requestId MDC | — | 메트릭, MDC | [06](./06-operations-and-career.md) |
| 19 | CI: GitHub Actions에서 `./gradlew build` | SIG-1 | Pipeline as Code | [06](./06-operations-and-career.md) |
| 20 | k6 부하 테스트 → 캐시 도입 → 전후 비교 | 18 | 부하 테스트, 캐시 무효화 | [06](./06-operations-and-career.md) |
| 21 | 답안 이력 저장에 outbox → Kafka, 멱등 컨슈머 | 15 | Outbox, 순서·중복 | [06](./06-operations-and-career.md) |
| 22 | OpenTelemetry 트레이싱, 자동 구성 `RestClient.Builder` `[결정 D7]` | 18 | 분산 추적 | [06](./06-operations-and-career.md) |
| 선택 | Modulith `verify()`, REST Docs 비교, Flyway 실습, nGrinder 비교 | — | 도구 선택 근거 | 01, 03, 04, 06 |

- **전체 의존은 [architecture-proposal §4.3](../../ai/architecture-proposal.md) 표와 `docs/ai/feature_list.example.json`이 기준**이다. 12단계 이후의 선행은 학습 순서 제안이다 [D].
- D13 결정 후 `FIX-F1S`(점수 범위 정책, 선행 FIX-F1T). `FIX-F3`·`FIX-F4` 뒤 `DOC-1`(docs/public 수정, 사람 판정).
- ¹ D1=B 기준. D1=A′이면 10단계 대신 `ARCH-A2`(선행 ARCH-A1, FIX-F1T)에서 R2를 A′ 규칙으로 교체한다.
- ² `docs/public/07` 예시의 DB 이름 `tmi`와 코드의 `tmi_word`가 다르다. 사람 확인.
- ³ 03 API 명세 §1.2 "인증 없음"을 바꾸려면 사람 결정이 필요하다.

1~11단계가 끝나면 ArchUnit R1~R7(+R4b)이 모두 `@Disabled` 없이 통과해야 한다 (D1=B 기준).

단계를 마칠 때마다 확인할 것:

1. 무엇을 바꿨고 **왜** 그렇게 골랐는지, 다른 선택지와 비교해 말할 수 있는가 (면접 대비).
2. 그 결정을 지키는 테스트나 도구 설정이 있는가.
3. 배운 개념을 `docs/study/<주제>/`에 노트로 남겼는가 (`docs/study/README.md` 형식).
