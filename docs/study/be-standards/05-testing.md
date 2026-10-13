[← be-standards](./README.md)

# 05. 테스트 전략

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

**도메인 정책은 단위 테스트로, 핵심 사용자 시나리오는 적지만 여러 계층을 덮는 통합·인수 테스트로.** 테스트가 느려지는 가장 흔한 원인은 **Mock 빈 때문에 생기는 컨텍스트 재생성**이고, 국내 여러 기업이 같은 결론을 냈다. DB는 운영과 같은 종류를 Testcontainers로 띄우는 쪽이 다수지만 H2를 쓰는 팀(토스)도 있다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 |
|---|---|---|
| 통합 테스트 Mock 빈을 클래스마다 다르게 두지 않는다. 컨텍스트 캐시가 깨져 느려진다 | A + 국내 공통 C✓ | Spring 캐싱 문서, 카카오페이(2023), 토스(2024), 플렉스(2024), 배민(2020) |
| 통합 테스트 DB는 H2 대신 운영과 같은 DB를 Testcontainers로 | A + B + 국내 공통 C✓ | Spring Boot 문서, Tecoble(2023), G마켓(2023), 플렉스(2024), 컬리(2023). 반례: 토스(2024) |
| 협력 객체는 실제 객체 우선, 모의 객체 프레임워크는 최소화 | B + 토스 사례 C✓ | Tecoble(2020), 토스(2024) |
| 외부 HTTP는 책임을 나눠 테스트한다: 클라이언트 자체는 Mock 서버로, 그 위 서비스는 클라이언트를 대역으로 | 국내 공통 C✓ | 카카오페이(2023·2024), 토스(2024) |
| 컨트롤러 테스트로 API 문서를 만든다 (REST Docs + MockMvc) | 국내 공통 C✓ | 배민(2018), 컬리(2022), 카카오페이(2022), 토스(2023) |
| 복잡한 Given 데이터는 객체 대신 JSON·SQL 파일로 준비 | 국내 공통 C✓ | 카카오페이(2024), 토스(2024) |
| 테스트 픽스처를 모듈 사이에 공유할 때 `java-test-fixtures` | 국내 공통 C✓ | 카카오페이(2025), 토스(2024) |
| 속도 레버: 컨텍스트 재사용, 컨테이너 수 억제, 병렬, lazy 초기화 | A + B + C✓ | Spring 문서, Tecoble(2022), 플렉스(2024), 토스(2024), LY(2026) |

공식 문서[A]와 교육 과정[B], 기업 원문[C✓]에서 확인한 중요한 사실:

| 사실 | 근거 |
|---|---|
| 테스트 피라미드는 참고 틀이다. 계층마다 성공·실패를 다 쓰면 양이 감당이 안 되므로, 적지만 여러 계층을 덮는 통합 테스트로 대체할 수 있다 | Tecoble 2022, 토스 2024 |
| 커버리지 향상 자체가 목적인 테스트는 의미가 없다. 핵심 기능(약 20%)부터 가치 판단해 쓴다 | 토스 2024 |
| Mock은 제어할 수 없는 영역(시간, 난수, 외부 API)에 먼저 쓴다. 내부 구현 검증으로 번지면 리팩터링에 깨진다 | Tecoble 2020, 토스 2024 |
| `@MockitoBean` 조합, `@DynamicPropertySource`가 다르면 컨텍스트 캐시 키가 달라져 다시 뜬다. 캐시는 기본 32개 LRU | Spring Framework 7 Context Caching |
| RANDOM_PORT 테스트는 클라이언트와 서버가 다른 스레드·트랜잭션이라 테스트의 `@Transactional` 롤백이 서버 쪽에 적용되지 않는다 | Spring Boot 4 문서 |
| 컨테이너를 빈으로 두면 캐시된 컨텍스트당 1번 뜬다. `static` + `@Testcontainers`는 클래스 사이에 공유되지 않는다 | Spring Boot 4 문서, Tecoble 2023 |
| 인수 테스트는 사용자 여정의 성공 시나리오, 예외 케이스는 아래 계층으로 내린다 | Tecoble 2022, 토스 2024 (블랙박스, 요청은 DTO 대신 Map) |
| Boot 4에서 `@MockBean`은 제거됐다(3.4에서 예고). `@AutoConfigureTestDatabase` 기본은 `NON_TEST`라 `@ServiceConnection` DB는 교체되지 않는다 | Boot 3.5 / 4.0.2 소스 |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 통합 테스트 DB | Testcontainers: G마켓, 플렉스, 컬리, 우테코 2023 / H2: 토스 2024 인수 테스트, 과거 우테코 미션 | native 쿼리, `jsonb`, `RANDOM()`, 락, DB 제약을 검증하면 Testcontainers. H2는 표준 SQL만 쓸 때의 타협이다 |
| 테스트 간 DB 격리 | `@Transactional` 롤백(`@DataJpaTest` 기본) / TRUNCATE(토스, 우테코) / `@Sql` 셋업·정리(카카오페이) / 패키지별 스키마 + 병렬(LY) / `@DirtiesContext`(지양) | 실서버 테스트는 TRUNCATE, 슬라이스·서비스 테스트는 롤백. 병렬이 필요해지면 스키마 분리 |
| 협력 객체 대체 | 실제 객체·Fake 우선(토스, Tecoble) / 계층 경계 Mockito(Tecoble도 허용) | 도메인은 실제 객체. 외부 API는 Fake, 시간은 주입 |
| Mock 빈 배치 | 클래스마다 `@MockitoBean`(플렉스가 겪은 문제) / `@TestConfiguration`에 mock 빈 고정(카카오페이) / 공통 상위 클래스·커스텀 애노테이션(토스, 우테코, 배민) | 통합 테스트는 베이스 하나로 빈 조합을 고정 |
| 외부 HTTP | WireMock·MockServer로 HTTP 흉내(카카오페이) / 클라이언트 빈을 Fake·mock으로(카카오페이, 토스) / 사내 공통 목 서버(카카오페이, 개발 환경용) | 클라이언트 어댑터 테스트만 WireMock, 나머지는 Fake |
| 커버리지 기준 | 100%를 1년 6개월 유지(토스뱅크 2021 발표) / 수치 목표보다 가치 판단(토스 2024) | 리포트부터 만들고 게이트는 나중에 |
| API 테스트 도구 | RestAssured + RANDOM_PORT(토스, 우테코) / MockMvc + REST Docs(배민, 컬리, 카카오페이, 토스페이먼츠) / RestTestClient(Boot 4 신규) | 핵심 시나리오는 실서버, 컨트롤러 계약·문서는 MockMvc |
| 픽스처 | 수기 Fixture 클래스(Object Mother) / JSON·`@Sql` 파일(카카오페이, 토스) / Fixture Monkey(네이버 오픈소스) | 작게 시작하면 수기 픽스처. 연관 데이터가 많은 인수 테스트는 SQL·JSON 파일 |

회사별로 확인한 세부 (2절 근거의 내용):

| 회사 (연도) | 확인한 내용 |
|---|---|
| 카카오페이 (2023) | `@MockBean` 대신 `@TestConfiguration` + `@Primary` mock 빈. Mock 서버 테스트는 HTTP 책임에만 |
| 토스 (2024) | Mock 결합은 "최대한 지양", 커스텀 애노테이션으로 컨텍스트 재사용, `spring.main.lazy-initialization=true`. 외부 서비스는 Fake, 알림은 Dummy. 테이블별 JSON을 TestExecutionListener가 INSERT, 끝나면 TRUNCATE. 인수 테스트는 H2 |
| 플렉스 (2024) | mockBean으로 dirty context → 컨테이너 폭증. Gradle build service로 컨테이너 공유, 로컬 10분 → 약 2분. Testcontainers 약 3년 사용 |
| 배민 정산 (2020) | REST Docs 테스트가 늘자 컨텍스트가 계속 생성돼 느려짐 → 문서 테스트용 공통 상위 클래스로 해결 |
| G마켓 (2023) | 인메모리 DB는 쿼리 호환성 문제 |
| 컬리 (2023) | PostGIS·Kafka·서버 이미지까지 띄운 시나리오 테스트 |
| 카카오페이 (2024·2025) | WireMock으로 타임아웃·서킷 브레이커 검증. 요청 JSON 파일, `@Sql` BEFORE/AFTER. DomainFixture와 테스트용 Mock 빈을 test-fixtures로 공유 |
| LY (2026, Go 프로젝트) | 병렬 + 테스트 패키지별 PostgreSQL 스키마 |

## 4. 이 프로젝트에 적용

현재 테스트: `CoreApplicationTests`(실 DB 필요, 탭 들여쓰기) + `architecture/` ArchUnit 3개(`LayerArchitectureTest`, `ModuleArchitectureTest`, `ConventionArchitectureTest`). 기능 테스트는 없다 (current-state F8).

### 4.1 그대로 유지

| 항목 | 이유 |
|---|---|
| ArchUnit 규칙을 테스트로 유지, 알려진 위반은 `@Disabled` + 사유 | 경계를 테스트로 강제하는 흐름과 같다 (01 문서) |
| 테스트 이름 `shouldXxx_whenYyy` + 한국어 `@DisplayName` | 06 §7.2, 제안 §1.4 유지 |
| 버그는 실패 테스트 먼저 (T2) | 회귀 방지의 기본 |
| `docs/public/06_코드가이드라인.md` §7.2 "Testcontainers 권장, H2 비추천" | Spring 문서[A], Tecoble[B], 국내 기업 사례[C✓]와 같다. 이 프로젝트는 native 쿼리를 쓰므로 토스식 H2 타협은 맞지 않는다 |

### 4.2 바꿀 것 (테스트 4단 구조)

| 단계 | 도구 | 이 프로젝트의 첫 대상 |
|---|---|---|
| 1 도메인·서비스 단위 | JUnit 5 + AssertJ, Spring 없음 | `answer/domain/Grade` 점수 경계값 (토스식 경계값 목록 테스트) |
| 2 웹 슬라이스 | `@WebMvcTest` + 최소 `@MockitoBean` | `AnswerController`의 깨진 JSON → 400 (F3), `@RequestBody` 검증 실패 |
| 3 JPA 슬라이스 | `@DataJpaTest` + Testcontainers PostgreSQL (`@ServiceConnection`) | `SentenceRepository.findRandomBy`(native + SpEL), `findByIdAndDeletedAtIsNull` |
| 4 인수 | `@SpringBootTest(RANDOM_PORT)` + RestTestClient 또는 RestAssured | 문장 출제 → 채점 성공 시나리오 1~2개. Similarity는 Fake 빈 |

세부 규칙:

| 규칙 | 이유 |
|---|---|
| Boot 4 API만 쓴다: `@MockitoBean`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` | 2020~2025년 국내 글 대부분이 Boot 2/3 API(`@MockBean`)다. 그대로 따라 쓰면 컴파일이 안 된다 |
| `@DataJpaTest` + `@ServiceConnection` 조합에 `@AutoConfigureTestDatabase(replace = NONE)`를 붙이지 않는다 | Boot 4 기본값 `NON_TEST`로 충분하다 |
| PostgreSQL 컨테이너는 `@TestConfiguration`의 `@Bean @ServiceConnection` 하나로 공유하고 `@Import`한다 | `static` 필드 방식은 클래스 사이에 공유되지 않는다 |
| 통합·인수 테스트는 베이스 클래스(또는 커스텀 애노테이션) 하나만 쓰고, 개별 클래스에 `@MockitoBean`·`@TestPropertySource`를 추가하지 않는다 | 카카오페이·토스·플렉스가 모두 겪은 컨텍스트 캐시 문제를 막는다 |
| Similarity 클라이언트: 어댑터 자체는 WireMock 또는 `MockRestServiceServer`로 타임아웃·오류 응답 검증, 인수 테스트에서는 `@TestConfiguration`의 Fake 빈 | 카카오페이 2023의 책임 분리 방식. 인수 테스트마다 HTTP 스텁을 쓰지 않는다 |
| 인수 테스트 격리: `@BeforeEach`에서 `TRUNCATE TABLE ... RESTART IDENTITY CASCADE` | Tecoble·토스 예시는 H2·MySQL 문법이다. PostgreSQL 문법은 일반 지식[D] |
| 인수 테스트 데이터가 커지면 `@Sql` 파일로 옮긴다 | 카카오페이·토스 모두 연관 데이터는 파일로 관리한다 |
| Docker가 필요한 테스트는 `@Tag("integration")` + 별도 Gradle 태스크 | `./gradlew test`가 Docker 없이 통과해야 한다 (T1, T3) |
| 픽스처는 `SentenceFixture.기본()` 같은 정적 팩토리 | 테스트 소스셋에 Lombok이 없다 (T4). 모듈이 늘어 공유가 필요해지면 `java-test-fixtures` |
| JaCoCo는 리포트부터. 첫 리포트 수치를 기준선으로 삼고 기능마다 올린다 | 국내 근거는 "100% 유지"(토스뱅크)와 "수치보다 가치"(토스) 둘뿐이다. 시작 게이트 값은 근거가 없다 |

### 4.3 기존 제안(`conventions-proposal.md` §6)과 비교

| 항목 | 기존 제안 | 이번 조사 | 판단 |
|---|---|---|---|
| T1 Docker 없이 `test` 통과, T3 `@Tag("integration")` | 있음 | 같은 방향 | **일치** |
| T4 테스트 소스셋 Lombok 없음, T5 Boot 4 API | 있음 | 소스로 재확인 | **일치** |
| `CoreApplicationTests` 처리 (D3) | 결정 대기 | 인수 테스트 베이스로 흡수할 수 있다 | 보완 |
| 컨텍스트 캐싱 규율, 외부 클라이언트 Fake, TRUNCATE, 인수 테스트 범위 | 명시 없음 | 국내 3개 기업 근거로 추가 권고 | 추가 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 테스트 피라미드 | 아래로 갈수록 빠르고 많다. 계층마다 잡는 결함과 비용이 다르다 |
| 테스트 대역 5종 | Dummy, Stub(정해진 응답), Spy(호출 기록), Mock(호출 검증), Fake(가벼운 실제 구현, 예: 인메모리 저장소). 토스 2024 글이 실무 예시로 설명한다 |
| 고전파 vs 런던파 | 고전파는 실제 객체로 결과를 검증, 런던파는 협력 객체를 Mock해 상호작용을 검증 |
| 컨텍스트 캐싱 | 설정·프로파일·프로퍼티·Mock 빈 조합이 같으면 컨텍스트를 재사용한다. `@DirtiesContext`는 비싸다 |
| 슬라이스 테스트 | `@WebMvcTest`는 웹 계층 빈만, `@DataJpaTest`는 JPA 빈만 올린다. 무엇이 빠지는지 설명할 수 있어야 한다 |
| `@Transactional` 테스트의 함정 | 롤백 때문에 flush가 안 일어나 제약 위반이 숨는다. 필요하면 `flush()`, `clear()` 후 검증한다 |
| RANDOM_PORT에서 롤백 안 됨 | 서버는 다른 스레드의 다른 트랜잭션이다. 그래서 TRUNCATE로 정리한다 |
| Testcontainers 생명주기 | JUnit `@Container`(클래스 단위) vs 싱글턴 vs Spring 빈(컨텍스트당 1회) vs 빌드 도구가 관리(플렉스) |
| 승인(골든) 테스트 | 출력 스냅샷을 저장해 비교한다. 토스는 캐시 직렬화 호환성 검사에 쓴다 |
| 계약 테스트 | 소비자가 기대하는 응답을 계약으로 만들어 제공자가 검증한다 (Pact). Mock 서버의 다음 단계 |
| 커버리지의 한계 | 라인 vs 브랜치 커버리지. 반환값 없는 메서드는 중간에 끝나도 통과하는 거짓 성공이 생긴다(토스) |

## 6. 기억할 것

1. 인터넷 예제는 **Boot 버전을 먼저 확인**한다. Boot 4에서는 `@MockBean`이 없다.
2. Mock 빈 조합이 테스트마다 다르면 컨텍스트가 다시 뜬다. 국내 여러 기업이 같은 문제를 겪었다.
3. 운영과 같은 DB로 테스트해야 native 쿼리 결함이 잡힌다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://toss.tech/article/test-strategy-server | 토스 (가치있는 테스트를 위한 전략과 구현) | 2024 | 기업 블로그 [C✓] |
| https://toss.im/slash-21/sessions/1-6 | 토스뱅크 (SLASH 21, 테스트 커버리지 100%) | 2021 | 발표 소개 페이지 [C✓] (발표 자료 본문은 미확인) |
| https://toss.tech/article/tosspayments-restdocs | 토스페이먼츠 (REST Docs DSL) | 2023 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/mock-test-code/ | 카카오페이 (Part 1 효율적인 Mock Test) | 2023 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/mock-test-code-part-2/ | 카카오페이 (Part 2 테스트 코드로부터 피드백) | 2023 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/given-test-code/ | 카카오페이 (Given 지옥, JSON·`@Sql`) | 2024 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/given-test-code-2/ | 카카오페이 (java-test-fixtures) | 2025 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/katfun-joy-multiple-biz-partner-02/ | 카카오페이 (WireMock 타임아웃·서킷 브레이커) | 2024 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/how-to-simplify-kakaopay-testing-using-a-common-mock-server/ | 카카오페이 SRE (사내 공통 목서버) | 2024 | 기업 블로그 [C✓] |
| https://tech.kakaopay.com/post/openapi-documentation/ | 카카오페이 (REST Docs + OAS) | 2022 | 기업 블로그 [C✓] |
| https://helloworld.kurly.com/blog/delivery-testcontainer-apply/ | 컬리 (Testcontainers 시나리오 테스트) | 2023 | 기업 블로그 [C✓] (URL 소문자여야 열림) |
| https://helloworld.kurly.com/blog/spring-rest-docs-guide/ | 컬리 (Spring REST Docs 가이드) | 2022 | 기업 블로그 [C✓] |
| https://flex.team/blog/2024/07/29/tech-testcontainers | 플렉스 (컨테이너 폭증 해결) | 2024 | 기업 블로그 [C✓] |
| https://dev.gmarket.com/76 | G마켓 (Testcontainers 통합 테스트) | 2023 | 기업 블로그 [C✓] |
| https://techblog.lycorp.co.jp/ko/techverse2026-223 | LY (Flava API Gateway, 병렬·스키마 격리) | 2026 | 기업 블로그 [C✓] (Go 프로젝트) |
| https://github.com/naver/fixture-monkey | NAVER | 2026 | 기업 오픈소스 [A-기업] |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/testing/spring-boot-applications.adoc | Spring | 2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/testing/testcontainers.adoc | Spring | 2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-framework/v7.0.3/framework-docs/modules/ROOT/pages/testing/testcontext-framework/ctx-management/caching.adoc | Spring | 2025 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v3.5.0/spring-boot-project/spring-boot-test/src/main/java/org/springframework/boot/test/mock/mockito/MockBean.java | Spring | 2025 | 공식 소스 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/module/spring-boot-jdbc-test/src/main/java/org/springframework/boot/jdbc/test/autoconfigure/AutoConfigureTestDatabase.java | Spring | 2026 | 공식 소스 |
| https://woowabros.github.io/experience/2018/12/28/spring-rest-docs.html, /2020/05/13/rest-docs.html | 우아한형제들 정산시스템 (REST Docs, 컨텍스트 재생성) | 2018, 2020 | 구 블로그 [C✓] |
| https://tecoble.techcourse.co.kr/post/2020-09-15-test-isolation/ | Tecoble | 2020 | 교육 과정 (GitHub 소스로 확인) |
| https://tecoble.techcourse.co.kr/post/2020-10-16-is-ok-mockito/ | Tecoble | 2020 | 교육 과정 |
| https://tecoble.techcourse.co.kr/post/2020-08-19-rest-assured-vs-mock-mvc/ | Tecoble | 2020 | 교육 과정 |
| https://tecoble.techcourse.co.kr/post/2022-10-15-test-code-optimization/ | Tecoble | 2022 | 교육 과정 |
| https://tecoble.techcourse.co.kr/post/2023-11-06-testcontainers/ | Tecoble | 2023 | 교육 과정 |

### 확인하지 못한 것 [C?]

<details>
<summary>펼치기</summary>

| 대상 | 사유 |
|---|---|
| https://techblog.woowahan.com/14874/ (우아한형제들, 2023) | Cloudflare 403. 제목도 확인 못 해 공개 사본을 찾지 못함. 이 글에 기대던 "배민의 점진적 커버리지 게이트" 주장은 삭제 |
| LINE 테스트 자동화 2·3편 | 원문 URL을 찾지 못함. 이전 "LINE이 Mock 서버를 쓴다" 주장은 삭제 (LY 2026 글은 오히려 인증 서버를 목으로 만들지 않고 로컬에서 띄움) |
| 토스뱅크 SLASH 21 발표 자료 본문 | 소개 페이지만 확인. 100% 유지 방법의 세부는 미확인 |

</details>
