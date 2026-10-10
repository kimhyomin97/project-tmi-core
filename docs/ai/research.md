# 리서치 — 구조·예외·DTO·트랜잭션·테스트·도구 (research)

> 상태: **제안 단계 산출물**. 조사일 2026-10-10.
> 목적: architecture-proposal.md · conventions-proposal.md 의 각 제안이 기대는 외부 근거를 한곳에 모으고, **이 프로젝트에 대한 적합성**을 평가한다.

## 0. 근거 등급

이 세션에서는 docs.spring.io 를 포함한 다수 호스트에 직접 접근할 수 없었다 (§9 접근 실패). 그래서 주장마다 근거의 강도를 표시한다.

| 표기 | 의미 | 강도 |
|---|---|---|
| **[J]** | Maven Central 의 소스 jar·POM 을 받아 **코드에서 직접 확인** | 가장 강함 |
| **[R]** | 이 저장소에서 **실행으로 확인** (current-state.md §7) | 가장 강함 |
| **[F]** | 접근 가능한 공식 페이지(spring.io 블로그, anthropic.com, code.claude.com, plugins.gradle.org) 본문을 직접 읽음 | 강함 |
| **[S]** | 검색 결과 스니펫으로만 확인 — 본문 미열람 | 중간 |
| (미검증) | 2차 출처만 있거나 확인 못 함 | 약함 — 결정 근거로 쓰지 말 것 |

버전은 2026-10-10 기준 Maven Central `maven-metadata.xml` / Gradle Plugin Portal 값이다.

| 항목 | 최신 안정 | 이 프로젝트에 맞는 선택 | 출처 |
|---|---|---|---|
| Spring Boot | 4.1.1 (4.0.x 최신 4.0.8) | 현행 4.0.2 유지 — 업그레이드는 별도 결정 | [J] https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml |
| Spring Modulith | 2.1.1 (Boot 4.1), 2.0.8 (Boot 4.0) | **2.0.x** | [J] https://repo1.maven.org/maven2/org/springframework/modulith/spring-modulith-bom/maven-metadata.xml |
| ArchUnit | 1.5.1 | `archunit-junit5` 1.5.1 (§6.3 호환성 메모) | [J] https://repo1.maven.org/maven2/com/tngtech/archunit/archunit-junit5/maven-metadata.xml |
| Testcontainers | 2.0.5 (Postgres 모듈명 `testcontainers-postgresql`) | Boot BOM 관리 버전 사용 | [J] https://repo1.maven.org/maven2/org/testcontainers/testcontainers-postgresql/maven-metadata.xml |
| Spotless Gradle | 8.10.4 (configuration cache ✓) | 8.10.4 | [F] https://plugins.gradle.org/plugin/com.diffplug.spotless |
| palantir-java-format | 2.102.0 | — | [J] https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format/maven-metadata.xml |
| google-java-format | 1.37.0 | — | [J] https://repo1.maven.org/maven2/com/google/googlejavaformat/google-java-format/maven-metadata.xml |
| Error Prone | core 2.50.0, Gradle 플러그인 `net.ltgt.errorprone` 5.1.1 | 2단계 도입 후보 | [J]/[F] https://plugins.gradle.org/plugin/net.ltgt.errorprone |
| MapStruct | 1.6.3 | 도입 비권장 (§4.3) | [J] https://repo1.maven.org/maven2/org/mapstruct/mapstruct/maven-metadata.xml |

---

## 1. Spring Boot 공식 권장 구조와 Spring Modulith

### 1.1 Spring Boot "Structuring Your Code"

- default package 금지, reversed domain 사용. `@SpringBootApplication` 메인 클래스는 **루트 패키지**에 둔다 — 컴포넌트 스캔·`@Entity` 탐색의 기준. [S] https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html
- 예시 레이아웃은 `com.example.myapplication` 아래 `customer`, `order` 같은 **기능별 하위 패키지**에 `Customer`, `CustomerController`, `CustomerService`, `CustomerRepository` 를 함께 둔다 — 공식 문서가 보여주는 기본형은 package-by-feature. [S] 같은 URL

**이 프로젝트 적합성**: 현재 `com.tmi.core.CoreApplication` + `sentence/`, `answer/` 기능 패키지는 공식 권장과 일치. 차이는 기능 패키지 안에 레이어 하위 패키지(`controller/`, `service/` …)를 한 단계 더 둔 것뿐이다. **적합 — 변경 불필요.**

### 1.2 Spring Modulith — 모듈 경계

- 도입 배경: stereotype(`@Controller`/`@Service`/`@Repository`)은 기술 중심 구조로 이끌기 쉽고, 코드 구조를 **도메인에 정렬**해야 이해·유지보수가 쉽다. [F] https://spring.io/blog/2022/10/21/introducing-spring-modulith
- 모듈 탐지 기본 규칙: 메인 클래스 패키지의 **직접 하위 패키지 1개 = 모듈 1개** (`ApplicationModuleDetectionStrategy.directSubPackage()`). 대안 `explicitlyAnnotated()`. [J] https://repo1.maven.org/maven2/org/springframework/modulith/spring-modulith-core/2.0.8/spring-modulith-core-2.0.8-sources.jar
- 모듈 base package 가 공개 API, **하위 패키지는 internal**. 다른 모듈의 internal 참조 = 위반. 하위 패키지를 공개하려면 `@NamedInterface`. `ApplicationModules.of(App.class).verify()` 로 검증. [S] https://docs.spring.io/spring-modulith/reference/fundamentals.html , https://docs.spring.io/spring-modulith/reference/verification.html — named interface 세부 문법 (미검증), `NamedInterface` 클래스 존재는 [J].
- Boot 4 호환: Modulith 2.0 GA(2025-11-21) 부터 baseline Boot 4 / Framework 7. [F] https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/
- Modulith 2.0.8 core 는 ArchUnit 1.4.2 에 의존 [J] — 직접 ArchUnit 1.5.1 을 함께 쓰면 버전 정렬 필요 (estimated).

**이 프로젝트 적합성**

| 현재 패키지 | Modulith 해석 | 결과 |
|---|---|---|
| `sentence`, `answer`, `toss`, `common` | 각각 모듈 | — |
| `infra` | **하나의 모듈** (`infra.similarity` 는 그 internal) | `answer → infra.similarity` 가 internal 참조 → 위반 |
| `sentence.repository`, `sentence.domain`, `sentence.exception` | `sentence` 의 internal | `answer` 의 직접 참조 3건 위반 (current-state.md §2) |

→ 구조 철학은 잘 맞지만, **현재 패키지 그대로 `verify()` 를 켜면 즉시 실패**한다 (estimated — 실제 Modulith 미실행. 같은 규칙을 ArchUnit 으로 실행한 결과는 current-state.md §6). 도입하려면 `infra` 해체 또는 named interface 지정이 선행돼야 한다. 의존성 추가 없이 같은 효과를 ArchUnit 규칙 몇 개로 낼 수 있다.

---

## 2. Hexagonal / Layered 실무 사례

| 출처 | 핵심 | 등급 |
|---|---|---|
| Alistair Cockburn, Hexagonal Architecture (2005) | 앱을 사람·프로그램·테스트·배치 어느 쪽에서든 동일하게 구동하고, 실제 DB·장치 없이 개발·테스트 가능하게 하는 것이 목적 | [S] https://alistair.cockburn.us/hexagonal-architecture/ (본문 fetch 실패), 2차 https://en.wikipedia.org/wiki/Hexagonal_architecture_(software) |
| Martin Fowler, PresentationDomainDataLayering | presentation / domain / data source 3 논리 레이어, 위→아래 의존. 도메인이 데이터 소스에 의존하지 않게 mapper 를 끼우는 변형을 흔히 Hexagonal 이라 부름 | [S] https://martinfowler.com/bliki/PresentationDomainDataLayering.html |
| Netflix TechBlog, Ready for changes with Hexagonal Architecture (2020) | gRPC·JSON API·GraphQL 등 **여러 데이터 소스**를 비즈니스 로직 변경 없이 교체하기 위해 채택 | [S] https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749 |
| Tom Hombergs, Get Your Hands Dirty on Clean Architecture | 레이어드의 약점에서 출발. 경계 간 매핑 전략 4종(No / Two-Way / Full / One-Way) | [S] https://www.oreilly.com/library/view/get-your-hands/9781839211966/ — 전략별 권고 세부 (미검증) |
| Simon Brown, architecturally-evident coding style | 코드가 아키텍처를 드러내야 함. 컴포넌트마다 패키지, public interface + package-private 구현 | [S] https://dzone.com/articles/architecturally-evident-coding |
| 실무자 의견 (복수) | 단순 CRUD·소규모·변화 적은 시스템에선 포트·어댑터·매핑 보일러플레이트 비용 > 이득. 통합 대상이 많고 수명이 길 때 이득 | [S] https://milanjovanovic.tech/blog/when-to-use-clean-architecture.md (실무자 의견 수준) |

**이 프로젝트 적합성**

- 사내 문서 02 §2.2 의 "헥사고날 → 레이어드 전환" 판단(단일 인바운드 REST, 단일 아웃바운드 JPA, 파일 수 2~3배)은 위 문헌의 "소규모 CRUD 에선 과함" 과 일치한다.
- 다만 이 판단 이후 상황이 바뀌었다: 아웃바운드가 **JPA + Similarity HTTP + Toss HTTP** 3개로 늘었고, Toss 는 스케줄러(인바운드 2번째)를 동반한다. Netflix 사례의 "여러 데이터 소스" 조건에 **부분적으로** 다가간다.
- 결론: 전면 헥사고날은 여전히 과하다. **외부 HTTP 경계에만 포트 성격의 인터페이스를 두는 부분 적용**은 비용이 낮다 — 이미 `SimilarityClient`(`@PostExchange` 인터페이스) 가 사실상 포트다. → architecture-proposal.md 선택지 B 에 반영.

---

## 3. 예외 처리

### 3.1 접근법 비교

| 접근 | 내용 | 출처 |
|---|---|---|
| (a) 자체 `@RestControllerAdvice` + 커스텀 body | 현재 방식. 응답 형식 자유, 프레임워크 예외를 하나하나 직접 매핑해야 함 | — |
| (b) `ResponseEntityExceptionHandler` 상속 | Spring MVC 내장 예외 **전부**와 `ErrorResponseException` 을 RFC 9457 body 로 렌더링하는 base class. `MessageSourceAware` 라 title/detail 국제화 가능 | [S] https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html , [J] spring-webmvc 7.0.8 sources |
| (c) Boot `spring.mvc.problemdetails.enabled=true` | Boot 가 `ProblemDetailsExceptionHandler` 를 등록. **기본값 false**. 앱이 `ResponseEntityExceptionHandler` 하위 advice 를 가지면 `@ConditionalOnMissingBean` 으로 등록 안 됨 | [J] https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.0.8/spring-boot-webmvc-4.0.8-sources.jar |
| (d) 도메인 예외가 `ErrorResponseException` 상속 | 예외 자체가 status·ProblemDetail 보유 | [S] https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/web/ErrorResponseException.html |

**적합성**

- (a) 를 유지하되 **(b) 의 "내장 예외 전부 처리" 이점만 흡수**하는 방법이 있다: `GlobalExceptionHandler extends ResponseEntityExceptionHandler` 로 바꾸고 `handleExceptionInternal` 을 오버라이드해 body 를 기존 `ErrorResponse` 로 만든다. 그러면 current-state F3 의 `HttpMessageNotReadableException` → 500 문제가 개별 핸들러 추가 없이 해소된다 (estimated — 오버라이드 시그니처는 Framework 7 소스로 확인 필요).
- 응답 형식을 RFC 9457 로 바꾸는 것(b·c·d)은 **외부 계약 변경**(03 API 명세 §1.4) 이므로 사람 결정 사항 → decisions-needed.md.

### 3.2 MessageSource 자동 구성 — 현재 결함의 근거

- `spring.messages.basename` 기본값 `messages` (`MessageSourceProperties.basename = List.of("messages")`). [J] https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.0.8/spring-boot-autoconfigure-4.0.8-sources.jar
- `ResourceBundleCondition` 이 `classpath*:<basename>.properties` 를 찾고 없으면 noMatch → **자동 구성 MessageSource 미생성**. [J] 같은 jar. 공식 문서 문구: "If no properties file is found that matches any of the configured base names, there will be no auto-configured MessageSource". [S] https://docs.spring.io/spring-boot/reference/features/internationalization.html
- 그 경우 컨텍스트는 빈 `DelegatingMessageSource` 를 등록하고, 기본 메시지 오버로드는 기본 메시지를 그대로 반환. [J] spring-context 7.0.8 sources — **이 저장소에서 빈 타입 `DelegatingMessageSource` 확인 [R]**.
- 해법: `spring.messages.basename=messages/messages`. (값은 slash 경로 형식으로 지정 가능 — estimated, 테스트로 확인 필요)

---

## 4. DTO 변환

### 4.1 Open Session In View

- Boot 의 `spring.jpa.open-in-view` 기본 **true**. 미설정 시 기동 WARN: "spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering…" [J] https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jpa/4.0.8/spring-boot-jpa-4.0.8-sources.jar — **이 저장소 테스트 로그에서 동일 WARN 확인 [R]**.
- Vlad Mihalcea 는 OSIV 를 anti-pattern 으로 규정, 연관은 서비스 트랜잭션 안에서 fetch 하고 `open-in-view=false` 권고. [S] https://vladmihalcea.com/the-open-session-in-view-anti-pattern/

### 4.2 변환 위치 접근법

| 접근 | 장점 | 단점 | 이 프로젝트 근거 |
|---|---|---|---|
| (a) 서비스가 엔티티 반환, 컨트롤러가 `Response.from(entity)` | 서비스 재사용 쉬움 | 엔티티가 트랜잭션 밖으로 → OSIV 의존, 타 기능이 엔티티에 결합 | 현재 `SentenceController.java:26` |
| (b) 서비스가 응답 DTO 반환 | 트랜잭션 안에서 변환 완료 → OSIV off 가능 | 서비스가 웹 응답 형태를 앎 | 현재 `AnswerService.java:32` |
| (c) 서비스가 기능 전용 결과 record 반환, 컨트롤러가 응답 DTO 로 변환 | 계층 분리 가장 깨끗 | 파일·매핑 1 단계 추가 | 없음 |
| (d) MapStruct | 대규모 매핑 자동화 | Lombok 과 annotation processor 순서 이슈(`lombok-mapstruct-binding` 필요) | [S] https://mapstruct.org/documentation/1.6/reference/html |

- "서비스가 엔티티를 반환하지 말라" 는 **공식 Spring 권고 문서는 찾지 못함** (미검증). OSIV 를 끄려면 엔티티가 트랜잭션 밖에서 지연 로딩되지 않아야 한다는 기술적 사실(§4.1)이 유일한 강한 근거다.

### 4.3 적합성

- 엔티티 2개, 연관관계 0개, 응답 DTO 3개 규모에서 (d) MapStruct 는 이득 < 비용. **비권장.**
- (a)/(b) 혼재가 current-state F5 의 실체. 어느 쪽이든 **하나로 통일**하는 것이 핵심이며, OSIV 를 끄려면 (b) 또는 (c). → conventions-proposal.md §3.

---

## 5. 트랜잭션 경계

| # | 사실 | 출처 |
|---|---|---|
| 5.1 | 프록시 모드에서는 프록시를 통한 외부 호출만 가로챈다. **self-invocation 은 `@Transactional` 이 있어도 미적용.** AspectJ 모드로만 해결 | [S] https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html , [J] spring-tx 7.0.8 `@EnableTransactionManagement` Javadoc "local calls within the same class cannot get intercepted that way" |
| 5.2 | Spring 6.0 부터 CGLIB 프록시는 **protected / package-private** 메서드에도 트랜잭션 적용 (`new AnnotationTransactionAttributeSource(false)`) | [J] https://repo1.maven.org/maven2/org/springframework/spring-tx/7.0.8/spring-tx-7.0.8-sources.jar |
| 5.3 | `SimpleJpaRepository` 는 클래스 레벨 `@Transactional(readOnly = true)`. 단 **인터페이스에 선언한 쿼리 메서드(파생·`@Query`)는 기본 트랜잭션 없음** | [S] https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html , [J] spring-data-jpa 4.0.7 sources, **[R] current-state.md §3.4 프로브** |
| 5.4 | `readOnly` 는 하위 계층에 전달되는 힌트이며 쓰기 차단 보장이 아님 | [S] 5.3 과 같은 문서 |
| 5.5 | 프로그래매틱 경계: `TransactionTemplate` | [S] https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html |
| 5.6 | "원격 호출은 트랜잭션 밖" — 커넥션 점유·롤백 불가 부작용. **공식 문서 문구는 찾지 못함** (미검증, 일반 실무 원칙) | — |

**적합성**

- `AnswerService.loadSentence` (current-state F4) 는 5.1 + 5.3 이 겹친 사례: 서비스 어노테이션은 self-invocation 으로 무효, 리포지토리는 선언 쿼리라 기본 트랜잭션 없음 → **완전 무트랜잭션**.
- 이 프로젝트의 흐름 "조회 → 원격 호출 → (향후) 저장" 에는 **"조회·저장은 각각 다른 빈의 짧은 트랜잭션, 원격 호출은 트랜잭션 없는 오케스트레이터에서"** 가 가장 단순하다. 기능 간 공개 API(선택지 B)를 도입하면 조회가 자연히 다른 빈(`SentenceReader` 등)으로 가므로 self-invocation 문제가 구조적으로 사라진다.
- Toss 스냅샷 설계 문서 `docs/public/toss/01_보유자산스냅샷.md` §3 "save 호출만 트랜잭션, 외부 API 호출은 트랜잭션 밖" 도 같은 원칙.

---

## 6. 테스트 전략

### 6.1 Spring Boot 4 테스트 모듈 변화

- 기술별 `-test` starter 로 분리 (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` …), 패키지명이 `org.springframework.boot.<module>` 로 변경. [F] https://spring.io/blog/2025/10/28/modularizing-spring-boot
- 확인된 위치 [J]:

| 어노테이션 | 패키지 | 아티팩트 |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure` | `spring-boot-webmvc-test` |
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure` | `spring-boot-data-jpa-test` |
| `@RestClientTest` | `org.springframework.boot.restclient.test.autoconfigure` | `spring-boot-restclient-test` |
| `@AutoConfigureMockMvc` | `org.springframework.boot.webmvc.test.autoconfigure` | `spring-boot-webmvc-test` — **이 저장소에서 컴파일·실행 확인 [R]** |

- `@MockBean` / `@SpyBean` **제거** → Spring Framework `@MockitoBean` / `@MockitoSpyBean`. [J] spring-boot-test 4.0.8 sources
- 이 저장소 `build.gradle` 은 이미 `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` 를 선언 — 슬라이스 테스트 작성에 **의존성 추가 불필요**.

### 6.2 접근법과 적합성

| 접근 | 용도 | 출처 | 이 프로젝트 적합성 |
|---|---|---|---|
| 순수 단위 테스트 (JUnit + Mockito) | `Grade.fromScore`, 서비스 분기 | — | **높음** — DB·Docker 불필요, 에이전트 루프의 빠른 신호 |
| `@WebMvcTest` + `@MockitoBean` | 컨트롤러 + `GlobalExceptionHandler` + 메시지 | [J] 위 표 | **높음** — current-state F2·F3 회귀 방지 |
| `MockRestServiceServer.bindTo(RestClient.Builder)` | HTTP Interface 클라이언트 | [J] spring-test 7.0.8 (6.1+) | **높음** — F1(`@RequestBody` 누락) 은 이 테스트 한 개로 잡힘 |
| `@DataJpaTest` + Testcontainers `@ServiceConnection` | native query(`RANDOM()`, SpEL enum 바인딩), jsonb | [S] https://docs.spring.io/spring-boot/reference/testing/testcontainers.html | 중간 — Docker 필요. 이 세션엔 `docker` CLI 는 있으나 데몬 사용 가능성 미검증 |
| WireMock (`wiremock-spring-boot` 4.4.3) | 실제 HTTP 전송 계층까지 | [S] https://wiremock.org/docs/spring-boot/ | 낮음 — `MockRestServiceServer` 로 충분 |
| `@SpringBootTest` + 실 DB | 현재 유일한 테스트 | — | 낮음 — 외부 DB 필요, 빌드 재현성 저하 (blockers.md §2) |

### 6.3 ArchUnit

- JUnit 5: `@AnalyzeClasses(packages = …)` + `@ArchTest`. `layeredArchitecture().consideringAllDependencies()…whereLayer(..).mayOnlyBeAccessedByLayers(..)`. `FreezingArchRule.freeze(rule)` 로 기존 위반 동결. [S] https://archunit.org/userguide/html/000_Index.html
- 호환성 메모: Boot 4.0.x BOM 의 JUnit Jupiter 는 6.x (4.0.8 기준 6.0.3 [J]). `archunit-junit5` 엔진이 JUnit Platform 6 에서 동작하는지 공식 표기를 찾지 못함 (미검증) → **이 저장소에서 실제 실행으로 확인** (current-state.md §6, 6단계 결과).

---

## 7. 포맷·정적 분석 Gradle 플러그인

| 도구 | 버전 | 성격 | Gradle 9 | 출처 | 적합성 |
|---|---|---|---|---|---|
| Spotless + palantir-java-format | 8.10.4 / 2.102.0 | 포맷 (자동 수정 `spotlessApply`) | CC ✓ 표기, Gradle 9 명시 표기 없음 (미검증) → **이 저장소에서 실행 확인 예정** | [F] https://plugins.gradle.org/plugin/com.diffplug.spotless | **높음** — 에이전트가 포맷을 기계적으로 맞출 수 있음 |
| Spotless + google-java-format | 8.10.4 / 1.37.0 | 포맷 (2-space 기본) | 동일 | [J] | 중간 — 기존 05 §2.1 "들여쓰기 4 spaces" 와 충돌, AOSP 옵션 필요 (estimated) |
| Error Prone | 2.50.0 / 플러그인 5.1.1 | 컴파일 시 버그 패턴 | CC ✓ | [F] https://plugins.gradle.org/plugin/net.ltgt.errorprone | 중간 — Lombok 과 오탐 가능성 (미검증) |
| Checkstyle (Gradle 내장) | 14.3.0 | 스타일 규칙 | 내장 | [J] | 낮음 — Spotless 와 중복 |
| PMD (Gradle 내장) | 7.28.0 | 코드 냄새 | PMD 7 은 Gradle 8.6+ 필요 | [S] https://docs.gradle.org/current/userguide/pmd_plugin.html | 낮음 — 초기 소음 큼 (estimated) |

**적합성**: 첫 단계는 **Spotless 하나**. 이유: (1) pass/fail 이 명확하고 `spotlessApply` 로 자동 복구 가능 → AI 루프에서 사람 개입 없이 닫힘 (2) 기존 코드에 이미 포맷 불일치가 다수 있음 (current-state §1 `AnswerController.java:17`, `SentenceException.java:8` 등) (3) 버그 탐지 도구(Error Prone)는 테스트가 먼저 생긴 뒤 도입해야 신호 대 소음이 맞다.

---

## 8. AI 코딩 에이전트 장시간 작업

| 출처 | 핵심 | 등급 |
|---|---|---|
| Anthropic Engineering, "Effective harnesses for long-running agents" (2025-11-26) | ① initializer 세션이 `init.sh`, `claude-progress.txt`, 첫 커밋을 만들고 ② 이후 coding 세션은 **한 번에 기능 1개** ③ `feature_list.json` 각 항목에 `"passes": false`, 에이전트는 **`passes` 만 수정**("It is unacceptable to remove or edit tests…") ④ JSON 을 쓴 이유: Markdown 보다 모델이 부적절하게 덮어쓸 가능성이 낮음 ⑤ 세션 시작 루틴: `pwd` → progress 읽기 → feature list 읽기 → `git log --oneline -20` → `init.sh` 로 기동·기본 테스트 | [F] https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents |
| 같은 글의 실패 유형 | 조기 완료 선언 / 버그·미기록 상태로 종료 / 검증 없이 passing 표시 / 실행 방법 탐색에 시간 낭비 → 각각 feature list, git+progress, 신중한 테스트 후 표시, `init.sh` 로 대응 | [F] 같은 URL |
| Claude Code best practices | 검증 수단(테스트·빌드·린터)을 주면 루프가 스스로 닫힘. explore → plan → implement → commit. Stop hook 으로 결정적 게이트 | [F] https://code.claude.com/docs/en/best-practices |
| CLAUDE.md / memory | 파일당 **200줄 미만** 목표, `@path` import, `.claude/rules/` 경로별 규칙 | [F] https://code.claude.com/docs/en/memory |
| 후속 글 "Harness design for long-running application development" (2026-03-24) | 하네스 설계 후속 | [F] https://www.anthropic.com/engineering/harness-design-long-running-apps (세부 미반영) |

**적합성**

- 이 저장소는 이미 Stop hook(`.claude/hooks/check-done.sh`) 으로 `checklist.json` 을 게이트로 쓰는 구조를 갖고 있다 → `feature_list.json` 루프로 확장하기 쉽다.
- "기능 1개씩 + 검증 후 passes" 원칙은 **검증 신호가 있어야** 성립한다. 현재 테스트가 사실상 0개이고 그나마 외부 DB 가 필요하므로, AI 루프 도입 전 **DB 없이 도는 테스트 세트 + Spotless** 가 선행 조건이다. → ai-workflow.md §1.

---

## 9. 접근 실패

| URL | 오류 |
|---|---|
| https://docs.spring.io/ (전체) | WebFetch `getaddrinfo ENOTFOUND`, curl `CONNECT tunnel failed, response 403` |
| https://alistair.cockburn.us/hexagonal-architecture/ | WebFetch DNS 실패 |
| https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749 | WebFetch DNS 실패 |
| https://dzone.com/articles/package-component-and | WebFetch DNS 실패 |
| https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide | HTTP 403 |
| https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes | HTTP 403 |
| api.github.com 릴리스 조회 (ArchUnit, Spotless, Modulith, Testcontainers 등) | 세션에 GitHub 저장소 접근 미허용 |
| archunit.org, martinfowler.com, testcontainers.com, junit.org, hibernate.org, mapstruct.org, wiremock.org, checkstyle.org, pmd.github.io, errorprone.info, docs.gradle.org, docs.anthropic.com, vladmihalcea.com 등 | curl 응답 000 (프록시 연결 거부/타임아웃) |
| repo.maven.apache.org / repo1.maven.org | 간헐적 HTTP 429 — 재시도·미러로 해결 (blockers.md §2) |

docs.spring.io 차단의 보완: 같은 내용을 **소스 jar([J])** 와 **이 저장소 실행([R])** 으로 교차 확인했다. [S] 로만 남은 주장은 결정 근거로 쓰지 않도록 각 제안서에서 표시한다.
