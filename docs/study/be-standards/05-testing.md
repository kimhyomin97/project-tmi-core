[← be-standards](./README.md)

# 05. 테스트 전략

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조. Tecoble은 우테코 크루의 **교육 과정 글**이지 회사 실무 표준이 아니다.

## 1. 한 줄 요약

**도메인 단위 테스트를 많이, 슬라이스·통합은 필요한 만큼, 인수 테스트는 핵심 성공 시나리오만.** DB는 운영과 같은 PostgreSQL을 Testcontainers로 띄운다. 테스트가 느려지는 가장 흔한 원인은 **컨텍스트 재생성**이다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 | 링크 |
|---|---|---|---|
| H2 대신 Testcontainers로 운영과 같은 DB(Kafka 포함)를 띄워 통합 테스트 | A + B + C | Spring Boot 문서, Tecoble 2023, 컬리·플렉스 발췌 | [Tecoble 2023](https://tecoble.techcourse.co.kr/post/2023-11-06-testcontainers/) |
| 외부 HTTP 의존은 코드 Mock 대신 실제로 응답하는 Mock 서버로 대체 | C | 카카오페이(사내 공통 목서버), LINE, 토스 발췌 | 7절 미확인 표 |
| 속도 개선 레버: 컨텍스트 재사용, 컨테이너 수 억제, 병렬 실행, 무거운 테스트를 슬라이스로 내리기 | A + B + C | Spring 컨텍스트 캐싱 문서, Tecoble 2022, LINE·플렉스·토스 발췌 | [Tecoble 2022](https://tecoble.techcourse.co.kr/post/2022-10-15-test-code-optimization/) |

공식 문서[A]와 교육 과정[B]에서 확인한 중요한 사실:

| 사실 | 근거 |
|---|---|
| 테스트 피라미드는 참고 틀이고, 비율은 도메인에 맞게 조정한다. "항상 피라미드형 비율을 유지할 수는 없다" | Tecoble 2022(확인), 토스(미확인) |
| Mock은 제어할 수 없는 영역(시간, 난수, 외부 API)에 먼저 쓴다. 내부 구현 검증으로 번지면 리팩터링에 깨진다. 그래도 큰 계층 경계에서는 Mock이 유용하다 | Tecoble 2020(확인) |
| `@MockitoBean` 조합, `@DynamicPropertySource`가 다르면 컨텍스트 캐시 키가 달라져 다시 뜬다. 캐시는 기본 32개 LRU | Spring Framework 7 Context Caching(확인) |
| RANDOM_PORT 테스트는 클라이언트와 서버가 다른 스레드·트랜잭션이라 테스트의 `@Transactional` 롤백이 서버 쪽에 적용되지 않는다 | Spring Boot 4 문서(확인) |
| 컨테이너를 빈으로 두면 캐시된 컨텍스트당 1번 뜬다. `static` + `@Testcontainers`는 클래스 사이에 공유되지 않는다 | Spring Boot 4 문서, Tecoble 2023(확인) |
| 인수 테스트는 성공 시나리오, 예외 케이스는 통합 테스트로 내린다 | Tecoble 2022(확인) |
| Boot 4에서 `@MockBean`은 제거됐다(3.4에서 예고). `@AutoConfigureTestDatabase` 기본은 `NON_TEST`라 `@ServiceConnection` DB는 교체되지 않는다 | Boot 3.5 / 4.0.2 소스(확인) |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 통합 테스트 DB | Testcontainers: 컬리, 플렉스, 우테코 2023 / H2: 과거 우테코 미션 | native 쿼리, `jsonb`, `RANDOM()`, 락을 쓰면 Testcontainers. Docker 필요 테스트는 `@Tag`로 분리 |
| 테스트 간 DB 격리 | `@Transactional` 롤백(`@DataJpaTest` 기본) / TRUNCATE Cleaner(우테코) / `@Sql` / 스키마 분리 + 병렬(LY 한 팀, 미확인) / `@DirtiesContext`(지양) | 실서버 테스트는 TRUNCATE, 슬라이스·서비스 테스트는 롤백 |
| 협력 객체 대체 | 실제 객체·Fake 우선(토스, Tecoble) / 계층 경계 Mockito(Tecoble도 허용) | 도메인은 실제 객체. 시간·외부 API는 주입과 Stub |
| Mock 빈 배치 | 클래스마다 `@MockitoBean` / 공통 상위 클래스에 고정(우테코) / java-test-fixtures(카카오페이, 미확인) | 통합 테스트는 베이스 클래스 하나로 조합을 고정 |
| 커버리지 기준 | 점진적 상향 게이트(배민, 미확인) / 100% 유지(토스뱅크 한 팀, 미확인) | 리포트부터 만들고 낮은 게이트에서 시작 |
| API 테스트 도구 | RestAssured + RANDOM_PORT(우테코) / MockMvc·MockMvcTester(+REST Docs, 컬리) / RestTestClient(Boot 4 신규) | Tecoble 결론대로 "정답은 없다". 핵심 시나리오는 실서버, 컨트롤러 계약은 슬라이스 |
| 픽스처 | 수기 Fixture 클래스(Object Mother) / `@Sql` + JSON(카카오페이, 미확인) / Fixture Monkey(네이버 오픈소스) | 작게 시작하면 수기 픽스처. 필드 많은 DTO 테스트가 늘면 Fixture Monkey |

## 4. 이 프로젝트에 적용

현재 테스트: `CoreApplicationTests`(실 DB 필요, 탭 들여쓰기) + `architecture/` ArchUnit 3개(`LayerArchitectureTest`, `ModuleArchitectureTest`, `ConventionArchitectureTest`). 기능 테스트는 없다 (current-state F8).

### 4.1 그대로 유지

| 항목 | 이유 |
|---|---|
| ArchUnit 규칙을 테스트로 유지, 알려진 위반은 `@Disabled` + 사유 | 경계를 테스트로 강제하는 흐름과 같다 (01 문서) |
| 테스트 이름 `shouldXxx_whenYyy` + 한국어 `@DisplayName` | 06 §7.2, 제안 §1.4 유지 |
| 버그는 실패 테스트 먼저 (T2) | 회귀 방지의 기본 |
| `docs/public/06_코드가이드라인.md` §7.2 "Testcontainers 권장, H2 비추천" | Spring 문서[A]·Tecoble[B]과 같다. 국내 기업 사례는 [C] |

### 4.2 바꿀 것 (테스트 4단 구조)

| 단계 | 도구 | 이 프로젝트의 첫 대상 |
|---|---|---|
| 1 도메인·서비스 단위 | JUnit 5 + AssertJ, Spring 없음 | `answer/domain/Grade` 점수 경계값 |
| 2 웹 슬라이스 | `@WebMvcTest` + 최소 `@MockitoBean` | `AnswerController`의 깨진 JSON → 400 (F3), `@RequestBody` 검증 실패 |
| 3 JPA 슬라이스 | `@DataJpaTest` + Testcontainers PostgreSQL (`@ServiceConnection`) | `SentenceRepository.findRandomBy`(native + SpEL), `findByIdAndDeletedAtIsNull` |
| 4 인수 | `@SpringBootTest(RANDOM_PORT)` + RestTestClient 또는 RestAssured | 문장 출제 → 채점 성공 시나리오 1~2개. Similarity는 WireMock/`MockRestServiceServer` |

세부 규칙:

| 규칙 | 이유 |
|---|---|
| Boot 4 API만 쓴다: `@MockitoBean`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` | 2020~2023년 국내 글 대부분이 Boot 2/3 API다. 그대로 따라 쓰면 컴파일이 안 된다 |
| `@DataJpaTest` + `@ServiceConnection` 조합에 `@AutoConfigureTestDatabase(replace = NONE)`를 붙이지 않는다 | Boot 4 기본값 `NON_TEST`로 충분하다 |
| PostgreSQL 컨테이너는 `@TestConfiguration`의 `@Bean @ServiceConnection` 하나로 공유하고 `@Import`한다 | `static` 필드 방식은 클래스 사이에 공유되지 않는다 |
| 인수 테스트 격리: `@BeforeEach`에서 `TRUNCATE TABLE ... RESTART IDENTITY CASCADE` | Tecoble 예시의 `SET REFERENTIAL_INTEGRITY`는 H2 전용이다. PostgreSQL 문법은 일반 지식(출처 미확인) |
| 통합 테스트는 추상 베이스 클래스 하나를 상속하고, 개별 클래스에 `@MockitoBean`·`@TestPropertySource`를 추가하지 않는다 | 컨텍스트 캐시를 지킨다 |
| Docker가 필요한 테스트는 `@Tag("integration")` + 별도 Gradle 태스크 | `./gradlew test`가 Docker 없이 통과해야 한다 (T1, T3) |
| 픽스처는 `SentenceFixture.기본()` 같은 정적 팩토리 | 테스트 소스셋에 Lombok이 없다 (T4). `@Builder`를 쓰려면 `testCompileOnly`와 `testAnnotationProcessor`를 추가 |
| JaCoCo는 리포트부터. 첫 리포트 수치를 기준선으로 삼고 기능마다 올린다 | "60%" 같은 시작값은 근거 없는 예시일 뿐이다 |

### 4.3 기존 제안(`conventions-proposal.md` §6)과 비교

| 항목 | 기존 제안 | 이번 조사 | 판단 |
|---|---|---|---|
| T1 Docker 없이 `test` 통과, T3 `@Tag("integration")` | 있음 | 같은 방향 | **일치** |
| T4 테스트 소스셋 Lombok 없음, T5 Boot 4 API | 있음 | 소스로 재확인 | **일치** |
| `CoreApplicationTests` 처리 (D3) | 결정 대기 | 4단 구조에서는 인수 테스트 베이스로 흡수할 수 있다 | 보완 |
| 컨텍스트 캐싱 규율, TRUNCATE Cleaner, 인수 테스트 범위 | 명시 없음 | 추가 권고 | 추가 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 테스트 피라미드 | 아래로 갈수록 빠르고 많다. 계층마다 잡는 결함과 비용이 다르다 |
| 테스트 대역 5종 | Dummy, Stub(정해진 응답), Spy(호출 기록), Mock(호출 검증), Fake(가벼운 실제 구현, 예: 인메모리 저장소) |
| 고전파 vs 런던파 | 고전파는 실제 객체로 결과를 검증, 런던파는 협력 객체를 Mock해 상호작용을 검증 |
| 컨텍스트 캐싱 | 설정·프로파일·프로퍼티·Mock 빈 조합이 같으면 컨텍스트를 재사용한다. `@DirtiesContext`는 비싸다 |
| 슬라이스 테스트 | `@WebMvcTest`는 웹 계층 빈만, `@DataJpaTest`는 JPA 빈만 올린다. 무엇이 빠지는지 설명할 수 있어야 한다 |
| `@Transactional` 테스트의 함정 | 롤백 때문에 flush가 안 일어나 제약 위반이 숨는다. 필요하면 `flush()`, `clear()` 후 검증한다 |
| RANDOM_PORT에서 롤백 안 됨 | 서버는 다른 스레드의 다른 트랜잭션이다. 그래서 TRUNCATE로 정리한다 |
| Testcontainers 생명주기 | JUnit `@Container`(클래스 단위) vs 싱글턴 vs Spring 빈(컨텍스트당 1회) |
| 계약 테스트 | 소비자가 기대하는 응답을 계약으로 만들어 제공자가 검증한다 (Pact). Mock 서버의 다음 단계 |
| 커버리지의 한계 | 라인 vs 브랜치 커버리지. 높은 수치가 좋은 테스트를 보장하지 않는다 |

## 6. 기억할 것

1. 인터넷 예제는 **Boot 버전을 먼저 확인**한다. Boot 4에서는 `@MockBean`이 없다.
2. 컨텍스트를 다시 띄우게 만드는 설정 차이가 테스트를 느리게 한다.
3. 운영과 같은 DB로 테스트해야 native 쿼리 결함이 잡힌다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/testing/spring-boot-applications.adoc | Spring | 2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/testing/testcontainers.adoc | Spring | 2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-framework/v7.0.3/framework-docs/modules/ROOT/pages/testing/testcontext-framework/ctx-management/caching.adoc | Spring | 2025 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v3.5.0/spring-boot-project/spring-boot-test/src/main/java/org/springframework/boot/test/mock/mockito/MockBean.java | Spring | 2025 | 공식 소스 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/module/spring-boot-jdbc-test/src/main/java/org/springframework/boot/jdbc/test/autoconfigure/AutoConfigureTestDatabase.java | Spring | 2026 | 공식 소스 |
| https://tecoble.techcourse.co.kr/post/2020-09-15-test-isolation/ | Tecoble | 2020 | 교육 과정 (GitHub 소스로 확인) |
| https://tecoble.techcourse.co.kr/post/2020-10-16-is-ok-mockito/ | Tecoble | 2020 | 교육 과정 |
| https://tecoble.techcourse.co.kr/post/2020-08-19-rest-assured-vs-mock-mvc/ | Tecoble | 2020 | 교육 과정 |
| https://tecoble.techcourse.co.kr/post/2022-10-15-test-code-optimization/ | Tecoble | 2022 | 교육 과정 |
| https://tecoble.techcourse.co.kr/post/2023-11-06-testcontainers/ | Tecoble | 2023 | 교육 과정 |
| https://github.com/naver/fixture-monkey | NAVER | 2026 | 기업 오픈소스 |

### 미확인 (직접 읽어 볼 후보, 상위 5개)

<details>
<summary>펼치기</summary>

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://toss.tech/article/test-strategy-server | 토스 (서버 테스트 전략) | 2024 (검색 표시값) | 기업 블로그 (미확인) |
| https://tech.kakaopay.com/post/how-to-simplify-kakaopay-testing-using-a-common-mock-server/ | 카카오페이 (공통 목서버) | 2024 | 기업 블로그 (미확인) |
| https://helloworld.kurly.com/blog/delivery-testContainer-apply/ | 컬리 (Testcontainers) | 2023 (추정) | 기업 블로그 (미확인) |
| https://flex.team/blog/2024/07/29/tech-testcontainers | 플렉스 | 2024 | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/14874/ | 우아한형제들 | 2023 | 기업 블로그 (미확인) |

그 밖에 검색으로만 본 글: 카카오페이 mock·given 테스트 글, 우아한 JaCoCo, 토스 SLASH 21, LINE 테스트 자동화 2·3편, LY Techverse 2026, 개인 블로그 1건.

</details>
