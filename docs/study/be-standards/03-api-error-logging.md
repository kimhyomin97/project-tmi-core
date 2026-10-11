[← be-standards](./README.md)

# 03. API 설계·예외 처리·로깅·문서화·외부 API 호출

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

실패는 **의미에 맞는 HTTP 상태 + 일관된 에러 본문**으로, 로그는 **요청 ID(traceId)를 붙여 한 곳에서** 남긴다. 외부 호출에는 **타임아웃을 반드시** 두고, 타임아웃은 "실패"가 아니라 **"결과를 모름"**으로 다룬다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 | 링크 |
|---|---|---|---|
| API 문서는 테스트를 통과해야 생기는 Spring REST Docs. 실행 UI가 필요하면 restdocs-api-spec으로 OpenAPI를 뽑아 Swagger UI에 띄운다 | C (도구는 A) | 우아한형제들 2018, 카카오페이 2022, 컬리(연도 미확인) 발췌. restdocs-api-spec README는 확인 | [카카오페이](https://tech.kakaopay.com/post/openapi-documentation/) (미확인) |
| 요청 진입점에서 traceId/requestId를 만들어 MDC에 넣고 모든 로그에 찍는다. 비동기에서는 MDC를 직접 넘긴다 | A + C | Boot Tracing 문서(확인), 카카오페이·우아한 발췌 | [Boot Tracing](https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/tracing.adoc) |
| ERROR는 "사람이 즉시 대응", WARN은 "빈도가 높으면 문제". 이미 처리됐거나 영향 없는 에러는 알림에서 거른다 | C | 컬리, LY, 우아한형제들, 토스 발췌 | 7절 미확인 표 |
| 4xx 비즈니스 예외는 INFO, 5xx는 ERROR + 스택 | **D** | 위 C 행의 레벨 정의에서 끌어낸 결론이다. 출처에 그대로 적힌 문장이 아니다 | — |
| 외부 호출 장애 격리에 Resilience4j 서킷브레이커 | C (A: Spring 7에 서킷브레이커 없음) | 우아한 2024, 토스 발췌 | [Spring Resilience](https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/core/resilience.adoc) |
| 타임아웃은 결과 미상이다. 호출 측은 상태 조회 후 재시도·보상, 제공 측은 멱등 API. 무조건 재시도하지 않는다 | C + D | 카카오페이, 컬리, 우아한 발췌 + 일반 원리 | 7절 미확인 표 |
| 대량 순차 조회는 offset 대신 마지막 키 조건(no-offset) | C | 우아한, 카카오페이 (주로 배치) 발췌 | 7절 미확인 표 |

Spring 공식으로 확인한 사실 [A]:

| 사실 | 출처 |
|---|---|
| `ResponseEntityExceptionHandler`는 Spring MVC 기본 예외 전부를 처리하는 `@ControllerAdvice` 기반 클래스다 | Spring Framework Error Responses |
| `spring.mvc.problemdetails.enabled` 기본값은 `false`다 | Boot `WebMvcProperties` 소스 |
| `logging.structured.format.console/file=ecs`(또는 `logstash`, `gelf`)로 JSON 로그를 낸다. MDC가 JSON에 들어간다 | Boot Logging 문서 |
| Micrometer Tracing을 쓰면 traceId·spanId가 로그에 기본으로 들어간다 | Boot Tracing 문서 |
| Spring Cloud OpenFeign은 feature-complete(버그 수정만). RestTemplate은 7.0에서 폐기 의향 발표, 7.1(잠정 2026-11, 기준일 현재 미출시)에 `@Deprecated` 예정, 8.0에서 제거 예정 | spring-cloud-openfeign README, Spring 블로그 2025-09-30 |
| Spring Framework 7은 `@Retryable`, `@ConcurrencyLimit`을 내장했다. 기본은 **모든 예외를 재시도**한다. 서킷브레이커는 없다 | Spring Framework Resilience 문서 |
| HTTP Service 그룹별 타임아웃: `spring.http.serviceclient.<group>.connect-timeout/read-timeout` | Boot Calling REST Services 문서 |
| API 버저닝 내장: `spring.mvc.apiversion.use.header/path-segment/...` | Spring MVC API Versioning |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 성공 응답을 `ApiResponse<T>`로 감쌀까? | 감쌈: 토스 앱인토스 파트너 API(미확인), 교육 프로젝트 다수 / 안 감쌈: 카카오 공개 API(미확인) | 클라이언트와 모니터링이 HTTP 상태로 분기하면 감싸지 않는다. 근거는 공개 API 문서와 커뮤니티 글뿐이다 |
| 비즈니스 에러의 HTTP 상태 | 의미에 맞는 4xx/5xx: 카카오 / HTTP 200 + `resultType=FAIL`: 토스 파트너 API | 내부 API는 의미에 맞는 상태. 200 방식은 외부 파트너 계약용 특수 선택이다 |
| 에러 본문 형식 | 자체 `{code, message}`: 카카오·토스·이 저장소 / RFC 9457 ProblemDetail: Spring 지원, 국내 채택 사례 미확인 | 이미 공개한 계약이 있으면 유지한다 |
| API 문서 도구 | REST Docs: 국내 공개 사례 다수 / springdoc: 도입이 빠름 | 문서와 코드 일치를 테스트로 강제하고 싶으면 REST Docs. 단 restdocs-api-spec은 maintenance 상태이고 Boot 4는 0.20.x 이상 필요 |
| HTTP 클라이언트 | OpenFeign: 우아한 2019 / WebClient: WebFlux 팀 / RestClient + `@HttpExchange`: Spring 공식 방향 | Boot 4 + MVC면 RestClient + HTTP Interface |
| 타임아웃 후 재시도 | 재시도: 우아한 feign 글, Feign 기본(100ms 시작, 1.5배, 최대 1s, 5회) / 제거·상태 조회: 컬리, 카카오페이 | 멱등 호출이고 5xx·네트워크 오류일 때만 재시도한다 |

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| `BaseException` + `ETmiErrorType` + 단일 `@RestControllerAdvice` | `common/exception/` | 흔한 구현이고, 에러 형식 `{"error":{"code","message"}}`는 외부 계약이다 (03 API 명세 §1.4) |
| 성공 응답을 감싸지 않음 (`SentenceResponse` 등을 그대로 반환) | `sentence/controller/` | HTTP 의미론을 지킨다 |
| 복수형 자원, POST 201 + `Location`, DELETE 204, 커서 페이징 | `docs/public/05_개발컨벤션.md` §4 | 일반적인 REST 관례와 같다 [D] |
| RestClient + HTTP Interface | `infra/similarity/SimilarityClient` | Spring 공식 방향과 같다 |
| URL 버전 `/api/v1` (`rest.api.version`) | `application.properties` | 바꿀 필요 없다. 헤더 버저닝이 필요해지면 Boot 4 내장 기능을 쓴다 |
| springdoc + `*Api` 인터페이스 분리 | `answer/controller/AnswerApi` | 당장은 유지. REST Docs는 기능 하나로 비교 실습 |

### 4.2 바꿀 것

| 현재 | 문제 | 바꿀 방향 |
|---|---|---|
| `GlobalExceptionHandler`가 개별 `@ExceptionHandler`만 가짐 | 깨진 JSON이 500이 된다 (current-state F3) | `extends ResponseEntityExceptionHandler` (제안 D2 (a)) |
| `BaseException`의 기본 `logLevel = ERROR` | 404도 ERROR로 남는다 | 상태 코드로 결정: 4xx INFO, 5xx ERROR + 스택 (제안 §2.3) |
| `AnswerService.callSimilarity`가 `log.error` 후 rethrow | 같은 예외가 두 번 기록된다 | 로그 없이 throw, 핸들러에서 1회 (제안 §5.1) |
| catch가 `HttpStatusCodeException`, `ResourceAccessException` 2개 | POST read timeout이 500이 된다 | catch 3단 (제안 §2.4, **`FIX-F1T`**. 로드맵 1b. 나중에 `ARCH-B2`가 이 catch를 `SimilarityScorer`로 옮긴다) |
| `RestClientConfig`가 `SimpleClientHttpRequestFactory`와 `@Value`로 타임아웃 설정 | 설정이 `common`에 흩어진다 | Boot 4 `spring.http.serviceclient.<group>.*` 프로퍼티 검토. 이를 쓰려면 `spring-boot-starter-restclient`가 필요하다 (제안 D7) |
| requestId·traceId 없음 | 요청 단위로 로그를 묶을 수 없다 | 먼저 `OncePerRequestFilter`로 MDC에 requestId. 나중에 Micrometer Tracing(06 문서) |
| 운영용 JSON 로그 없음 | 수집 도구와 맞지 않는다 | `prod` 프로필에 `logging.structured.format.file=ecs` |

### 4.3 기존 제안과 다른 점

| 항목 | 기존 제안 | 이번 조사 |
|---|---|---|
| 에러 형식 (D2) | (a) 현행 유지 + `ResponseEntityExceptionHandler` | **일치**. 기반 클래스는 Spring 공식[A]. ProblemDetail의 국내 채택 근거를 찾지 못했다 |
| 로그 레벨 | 4xx INFO / 5xx ERROR | **일치**. 다만 근거는 추론[D]이다 (컬리·LY 레벨 정의[C]에서 끌어냄) |
| 재시도 | Toss 재시도 정책 위주 | Spring 7 `@Retryable`은 기본이 "모든 예외 재시도"다. 대상 예외를 반드시 지정한다 |
| 서킷브레이커 | 다루지 않음 | 필요해지면 Resilience4j (Spring 7에 없음) |
| 검증 3단 | 다루지 않음 | 형식·필수값은 DTO `@Valid`, 단일 엔티티 불변식은 도메인 팩토리, 중복·상태 전이는 서비스 (일반 설계 원칙, 국내 근거 미확인) |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| HTTP 상태 의미 | 400 형식 오류, 401 인증 없음, 403 권한 없음, 404, 409 상태 충돌, 422 의미 오류, 502 상류가 잘못 응답, 503 상류 불가, 504 상류 타임아웃 |
| 예외가 응답이 되는 경로 | `@RestControllerAdvice` → `ResponseEntityExceptionHandler` → 처리 못 하면 `/error`(BasicErrorController). 필터 예외는 advice를 거치지 않는다 |
| ProblemDetail | RFC 9457 표준 필드 `type`, `title`, `status`, `detail`, `instance` + 확장 속성 |
| `@Valid` vs `@Validated` | `@Valid`는 표준(중첩 검증), `@Validated`는 Spring 확장(그룹, 메서드 검증) |
| MDC | ThreadLocal 기반이라 스레드가 바뀌면 사라진다. 비동기에서는 직접 넘겨야 한다 |
| 구조화 로그 | 로그를 JSON으로 내면 필드로 검색·집계할 수 있다 (ECS, Logstash 형식) |
| 타임아웃 = unknown | 요청이 안 갔는지, 처리됐는지, 실패했는지 모른다. 그래서 멱등키와 상태 조회가 필요하다 |
| 지수 백오프 | 재시도 간격을 늘려 하류 과부하를 막는다. 재시도 폭풍을 피한다 |
| 서킷브레이커 | CLOSED → 실패율 초과 → OPEN(즉시 실패) → HALF_OPEN(시험 호출). 장애가 번지는 것을 막는다 |
| offset vs 커서 | offset은 깊은 페이지에서 느리다. 커서는 빠르지만 임의 페이지로 건너뛸 수 없다 |

## 6. 기억할 것

1. 외부 호출에는 타임아웃을 반드시 두고, **타임아웃은 결과 미상**이다.
2. 예외 로그는 **한 곳에서 한 번**. 4xx는 INFO, 5xx는 ERROR + 스택.
3. 공개한 에러 형식은 계약이다. 바꾸지 말고 처리 범위를 넓힌다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/web/webmvc/mvc-ann-rest-exceptions.adoc | Spring | 현행 7.x | 공식 |
| https://github.com/spring-projects/spring-boot/blob/main/module/spring-boot-webmvc/src/main/java/org/springframework/boot/webmvc/autoconfigure/WebMvcProperties.java | Spring | 현행 | 공식 소스 |
| https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/logging.adoc | Spring | 현행 | 공식 |
| https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4 | Spring 블로그 | 2024 | 공식 |
| https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/tracing.adoc | Spring | 현행 | 공식 |
| https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/io/rest-client.adoc | Spring | 현행 | 공식 |
| https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/core/resilience.adoc | Spring | 현행 7.x | 공식 |
| https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/web/webmvc-versioning.adoc | Spring | 현행 | 공식 |
| https://spring.io/blog/2025/09/23/http-service-client-enhancements | Spring 블로그 | 2025 | 공식 |
| https://spring.io/blog/2025/09/30/the-state-of-http-clients-in-spring | Spring 블로그 | 2025 | 공식 (RestTemplate 일정) |
| https://github.com/spring-cloud/spring-cloud-openfeign | Spring | 현행 | 공식 |
| https://github.com/ePages-de/restdocs-api-spec | ePages | 현행 | 오픈소스 |
| https://github.com/OpenFeign/feign/blob/master/core/src/main/java/feign/DefaultRetryer.java | OpenFeign | 현행 | 오픈소스 |

### 미확인 (직접 읽어 볼 후보, 상위 5개)

<details>
<summary>펼치기</summary>

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://tech.kakaopay.com/post/openapi-documentation/ | 카카오페이 (REST Docs + OpenAPI) | 2022 | 기업 블로그 (미확인) |
| https://tech.kakaopay.com/post/podo-elk-threadcontext-part-3/ | 카카오페이 (MDC) | 연도 미확인 | 기업 블로그 (미확인) |
| https://helloworld.kurly.com/blog/deliveryproductteam-culture-1/ | 컬리 (로그 레벨) | 연도 미확인 | 기업 블로그 (미확인) |
| https://tech.kakaopay.com/post/msa-transaction/ | 카카오페이 (타임아웃·보상) | 2023 (추정) | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/15694/ | 우아한형제들 (서킷브레이커) | 2024 | 기업 블로그 (미확인) |

그 밖에 검색으로만 본 글: 우아한 2018 REST Docs·2019 feign·2020 no-offset, 컬리 REST Docs 가이드, LY 장애 모니터링, 토스 28738, 카카오 Developers 에러 코드, 토스 앱인토스 응답 형식, 개인 블로그 1건.

</details>
