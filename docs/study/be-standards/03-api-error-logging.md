[← be-standards](./README.md)

# 03. API 설계·예외 처리·로깅·문서화·외부 API 호출

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

실패는 **의미에 맞는 HTTP 상태 + `{code, message}` 본문**으로, 로그는 **요청 ID를 붙여 한 곳에서** 남긴다. 외부 호출에는 **타임아웃을 반드시** 두고, 타임아웃은 "실패"가 아니라 **"결과를 모름"**으로 다룬다.

## 2. 공개 근거로 본 관례

### 2.1 API·에러·문서

| 관례 | 등급 | 근거 |
|---|---|---|
| 에러는 의미에 맞는 HTTP 상태 + `code`·`message` 본문. 분기는 코드로 한다 | 국내 공통 C✓ (공개 API 문서 기준) | 카카오 Developers(현행), 토스페이먼츠 API 문서(현행) |
| 사내 서비스 간 호출에서도 오류 본문(`message`, `code`, `status`)을 그대로 전달한다 | 카카오페이 사례 C✓ | 카카오페이(2023) |
| API 문서는 테스트를 통과해야 생기는 Spring REST Docs | 국내 공통 C✓ (도구는 A) | 배민(2018), 카카오페이(2022), 컬리(2022), 토스(2023) |
| 대량 순차 조회는 offset 대신 마지막 키 조건(no-offset) | 국내 공통 C✓ (주로 배치) | 배민(2020), 컬리(2024), 카카오페이(2026) |

### 2.2 로그·추적

| 관례 | 등급 | 근거 |
|---|---|---|
| ERROR는 즉시 대응, WARN은 빈도가 높을 때만 문제 → WARN은 임계치 알림 | 컬리 사례 C✓ | 컬리(2025) |
| 의도된 예외(사용자 입력 오류 등)는 ERROR가 아니다 | 컬리 사례 C✓ | 컬리(2025) |
| 4xx 비즈니스 예외는 INFO, 5xx는 ERROR + 스택 | D | 위 정의에서 끌어낸 결론 |
| 요청 진입점에서 requestId를 만들어 MDC에 넣는다 | 카카오페이 사례 C✓ + A | 카카오페이(2024), Boot Tracing 문서 |
| 서비스 간 호출은 추적 ID를 HTTP 헤더로 넘긴다 | 국내 공통 C✓ | 네이버(2015), 카카오페이(2024) |

### 2.3 외부 호출

| 관례 | 등급 | 근거 |
|---|---|---|
| 타임아웃은 성공·실패·**알 수 없음** 중 세 번째다. 멱등 재요청이나 취소로 상태를 맞춘다 | 국내 공통 C✓ | 카카오페이(2022), 토스(2025) |
| 재시도는 기본으로 끄고, 재시도해도 되는 호출만 따로 켠다 | 배민 사례 C✓ + A | 배민(2019), Spring 7 문서 |
| 호출 체인 전체의 타임아웃을 맞춘다 (하위 처리 시간 ≤ 상위 타임아웃) | 토스 사례 C✓ | 토스페이먼츠(2025) |
| 의존 서비스 장애가 번지지 않게 서킷브레이커·격리를 둔다 | 국내 공통 C✓ | 배민(2017, Hystrix), 토스(2024·2025, Resilience4j) |
| HTTP 클라이언트 라이브러리는 모듈 안에 숨기고, 클라이언트는 통신 책임만 진다 | 카카오페이 사례 C✓ | 카카오페이(2023) |

Spring 공식으로 확인한 사실 [A]:

| 사실 | 출처 |
|---|---|
| `ResponseEntityExceptionHandler`는 Spring MVC 기본 예외 전부를 처리하는 `@ControllerAdvice` 기반 클래스다 | Spring Framework Error Responses |
| `spring.mvc.problemdetails.enabled` 기본값은 `false`다 | Boot `WebMvcProperties` 소스 |
| `logging.structured.format.console/file=ecs`(또는 `logstash`, `gelf`)로 JSON 로그. MDC가 JSON에 들어간다 | Boot Logging 문서 |
| Micrometer Tracing을 쓰면 traceId·spanId가 로그에 기본으로 들어간다 | Boot Tracing 문서 |
| OpenFeign은 feature-complete. RestTemplate은 7.1(잠정 2026-11)에 `@Deprecated`, 8.0에서 제거 예정 | spring-cloud-openfeign README, Spring 블로그 2025-09-30 |
| Spring 7 `@Retryable`·`@ConcurrencyLimit` 내장. 기본은 **모든 예외를 재시도**. 서킷브레이커는 없다 | Spring Framework Resilience 문서 |
| HTTP Service 그룹별 타임아웃: `spring.http.serviceclient.<group>.connect-timeout/read-timeout` | Boot Calling REST Services 문서 |
| API 버저닝 내장: `spring.mvc.apiversion.use.header/path-segment/...` | Spring MVC API Versioning |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 성공 응답을 감쌀까? | 안 감쌈: 카카오 API [C✓] / 감쌈: 앱인토스 서버 API `{resultType, success \| error}` [C✓] | 같은 토스 안에서도 토스페이먼츠는 HTTP 상태 + `{code, message}`, 앱인토스는 봉투형이다. 회사가 아니라 API 성격으로 갈린다. HTTP 상태로 분기하면 감싸지 않는다 |
| 에러 본문 형식 | 자체 `{code, message}`: 카카오·토스페이먼츠·카카오페이 [C✓] / RFC 9457 ProblemDetail: Spring 지원 [A], 국내 채택 사례 못 찾음 | 이미 공개한 계약이 있으면 유지한다 |
| 로그 레벨을 나누는 축 | **심각도·빈도 축**: 컬리(2025, 서버). ERROR 즉시 대응, WARN은 5분 100회 같은 임계치 알림 / **원인 위치 축**: LY(2025, 앱). 외부 시스템 연동 실패 = warning(10분 100회 알림), 내부 로직 오류 = error(즉시 알림) [C✓] | 두 축은 다르다. 서버 학습 프로젝트는 컬리 축으로 시작하고, 외부 연동 실패를 따로 집계해야 할 때 LY 축을 더한다 |
| 4xx를 어느 레벨로 | INFO: 이 문서의 추론 [D] / WARN + 빈도 알림: 컬리 [C✓] | 빈도 알림 체계가 있으면 WARN, 없으면 INFO. 어느 쪽이든 ERROR는 아니다 |
| API 문서 도구 | REST Docs(+OpenAPI 변환): 배민·카카오페이·컬리·토스페이먼츠 [C✓] / springdoc + 통합 포털: LY(2024) [C✓] | 문서와 코드 일치를 테스트로 강제하려면 REST Docs. restdocs-api-spec은 maintenance 상태이고 Boot 4는 0.20.x 이상 필요 [A]. 문서 테스트가 늘면 컨텍스트 재생성으로 느려진다(배민 2020: 공통 상위 클래스로 해결) |
| HTTP 클라이언트 | OpenFeign: 배민(2019), 토스페이먼츠(2023) [C✓] / 여러 라이브러리를 모듈 뒤에 숨김: 카카오페이(2023) [C✓] / RestClient + `@HttpExchange`: Spring 공식 방향 [A] | Boot 4 + MVC면 RestClient + HTTP Interface. 라이브러리 타입이 서비스 계층에 새지 않게 한다 |
| 타임아웃 뒤 처리 | 멱등 API에 1회 재요청 → 그래도 모르면 저장 후 후처리: 카카오페이 [C✓] / 망취소로 상태 맞춤: 토스페이먼츠 [C✓] / 비동기 재시도 토픽: 컬리(2026) [C✓] | 제공 측이 멱등이면 재요청, 아니면 상태 조회·취소 |
| 재시도 설정 | Feign: 배민(2019)이 확인한 대로 Spring Cloud OpenFeign 기본 빈은 `NEVER_RETRY`. `Retryer.Default`(100ms 시작, 1.5배, 최대 5회 [A])를 켜면 같은 API를 "재시도 클라이언트 / 비재시도 클라이언트"로 나눠 쓴다 [C✓] / Spring 7 `@Retryable`: 기본이 모든 예외 [A] | 재시도는 대상 예외와 대상 호출을 반드시 좁힌다 |
| 서킷브레이커 격리 방식 | Hystrix 세마포어 → 스레드 격리로 전환, 설정 조정 후 TPS 약 4배: 배민 API Gateway(2017) [C✓] / Resilience4j CircuitBreaker·RateLimiter: 토스(2024·2025) [C✓] | 새로 만든다면 Resilience4j(Hystrix는 유지보수 종료). 격리 방식(스레드/세마포어)은 부하 테스트로 고른다 |

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| `BaseException` + `ETmiErrorType` + 단일 `@RestControllerAdvice` | `common/exception/` | 에러 형식 `{"error":{"code","message"}}`는 외부 계약이다 (03 API 명세 §1.4). 코드로 분기하는 방식은 국내 공개 API 관례와 같다 [C✓] |
| 성공 응답을 감싸지 않음 | `sentence/controller/` | HTTP 의미론을 지킨다. 카카오 API와 같은 방식 [C✓] |
| 복수형 자원, POST 201 + `Location`, DELETE 204, 커서 페이징 | `docs/public/05_개발컨벤션.md` §4 | 일반적인 REST 관례 [D]. 커서 페이징은 국내 사례와도 맞는다 [C✓] |
| RestClient + HTTP Interface, `infra/similarity`에 격리 | `SimilarityClient` | Spring 공식 방향 [A], 카카오페이 사례와 같은 모양 [C✓] |
| URL 버전 `/api/v1` | `application.properties` | 바꿀 필요 없다 |
| springdoc + `*Api` 인터페이스 분리 | `answer/controller/AnswerApi` | 당장은 유지(LY도 springdoc [C✓]). REST Docs는 기능 하나로 비교 실습 |

### 4.2 바꿀 것

| 현재 | 문제 | 바꿀 방향 |
|---|---|---|
| `GlobalExceptionHandler`가 개별 `@ExceptionHandler`만 가짐 | 깨진 JSON이 500이 된다 (F3) | `extends ResponseEntityExceptionHandler` (제안 D2 (a)) |
| `BaseException`의 기본 `logLevel = ERROR` | 404도 ERROR로 남는다. 컬리가 말한 "가짜 에러"다 [C✓] | 4xx INFO(알림 체계가 생기면 WARN + 빈도 알림), 5xx ERROR + 스택 (제안 §2.3) |
| `AnswerService.callSimilarity`가 `log.error` 후 rethrow | 같은 예외가 두 번 기록된다 | 로그 없이 throw, 핸들러에서 1회 (제안 §5.1) |
| catch가 `HttpStatusCodeException`, `ResourceAccessException` 2개 | POST read timeout이 500이 된다 | catch 3단 (제안 §2.4, **`FIX-F1T`**, README 로드맵 3단계. 나중에 `ARCH-B2`가 이 catch를 `SimilarityScorer`로 옮긴다) |
| `RestClientConfig`가 `@Value`로 타임아웃 설정 | 설정이 `common`에 흩어진다 | Boot 4 `spring.http.serviceclient.<group>.*` 검토. `spring-boot-starter-restclient` 필요 (제안 D7) |
| requestId·traceId 없음 | 요청 단위로 로그를 묶을 수 없다 | `OncePerRequestFilter`로 MDC에 requestId (카카오페이 방식 [C✓]). 나중에 Micrometer Tracing |
| 운영용 JSON 로그 없음 | 수집 도구와 맞지 않는다 | `prod` 프로필에 `logging.structured.format.file=ecs` |

채점 호출 타임아웃은 "결과 미상"이지만, 유사도 서버는 **조회성(점수 계산)이라 부작용이 없다**. 그래서 망취소·Unknown 저장까지는 필요 없고, 503으로 응답한 뒤 사용자가 다시 제출하면 된다 [D].

### 4.3 기존 제안과 다른 점

| 항목 | 기존 제안 | 이번 조사 |
|---|---|---|
| 에러 형식 (D2) | (a) 현행 유지 + `ResponseEntityExceptionHandler` | **일치**. ProblemDetail 국내 채택 근거는 여전히 없다 |
| 로그 레벨 | 4xx INFO / 5xx ERROR | **대체로 일치**. ERROR/WARN 정의는 컬리 사례 [C✓]. LY는 다른 축을 쓴다 |
| 재시도 | Toss 재시도 정책 위주 | 국내 원문은 "타임아웃 = Unknown" 후 **멱등 재요청 또는 취소**다. 재시도는 대상을 좁혀 켠다(배민 2019) |
| 서킷브레이커 | 다루지 않음 | 필요해지면 Resilience4j (토스 사례, Spring 7에 없음 [A]) |
| 검증 3단 | 다루지 않음 | 형식은 DTO `@Valid`, 단일 엔티티 불변식은 도메인 팩토리, 중복·상태 전이는 서비스 [D] |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| HTTP 상태 의미 | 400 형식 오류, 401 인증 없음, 403 권한 없음, 404, 409 상태 충돌, 422 의미 오류, 502 상류 오응답, 503 상류 불가, 504 상류 타임아웃 |
| 예외가 응답이 되는 경로 | `@RestControllerAdvice` → `ResponseEntityExceptionHandler` → `/error`. 필터 예외는 advice를 거치지 않는다 |
| ProblemDetail | RFC 9457 표준 필드 `type`, `title`, `status`, `detail`, `instance` |
| `@Valid` vs `@Validated` | `@Valid`는 표준(중첩 검증), `@Validated`는 Spring 확장(그룹, 메서드 검증) |
| MDC | ThreadLocal 기반이라 스레드가 바뀌면 사라진다 |
| 타임아웃 = Unknown | 요청이 안 갔는지, 처리됐는지 모른다. 멱등키와 상태 조회(또는 취소)가 필요하다 |
| 멱등 API | 같은 요청을 여러 번 보내도 같은 결과. POST라도 멱등키로 만들 수 있다 |
| 서킷브레이커 | CLOSED → 실패율 초과 → OPEN → HALF_OPEN. 스레드 격리는 비용이 들지만 네트워크 타임아웃 위에 한 겹 더 막는다(배민 2017) |
| offset vs 커서 | offset은 깊은 페이지에서 느리다. 커서는 빠르지만 임의 페이지로 건너뛸 수 없다 |

## 6. 기억할 것

1. 외부 호출에는 타임아웃을 반드시 두고, **타임아웃은 결과 미상**이다.
2. 예외 로그는 **한 곳에서 한 번**. 4xx는 ERROR가 아니다.
3. 공개한 에러 형식(`code`, `message`)은 계약이다. 바꾸지 말고 처리 범위를 넓힌다.

## 7. 출처

### 확인한 출처: 국내 기업 (원문을 열어 읽음, 2026-10-12)

| URL | 발행처 | 연도 | 확인한 내용 |
|---|---|---|---|
| https://developers.kakao.com/docs/latest/ko/rest-api/reference | 카카오 (공개 API 문서) | 현행 | HTTP 상태 + `code`·`msg`, 처리는 코드로 |
| https://docs.tosspayments.com/reference/error-codes | 토스페이먼츠 (공개 API 문서) | 현행 | HTTP 상태 + `{code, message}` |
| https://developers-apps-in-toss.toss.im/documentation/integration/server-api.md | 토스 앱인토스 | 현행 | 봉투형 응답 |
| https://tech.kakaopay.com/post/make-http-client-design-flexible/ | 카카오페이 | 2023 | 클라이언트 격리, 사내 오류 본문 `{message, code, status}` |
| https://tech.kakaopay.com/post/openapi-documentation/ | 카카오페이 | 2022 | REST Docs + restdocs-api-spec |
| https://helloworld.kurly.com/blog/spring-rest-docs-guide/ | 컬리 | 2022 | REST Docs, Swagger UI |
| https://toss.tech/article/tosspayments-restdocs | 토스페이먼츠 | 2023 | REST Docs 권장, 래퍼 라이브러리 |
| https://woowabros.github.io/experience/2018/12/28/spring-rest-docs.html | 우아한형제들 정산시스템 | 2018 | wiki → REST Docs. "테스트가 성공해야 문서가 생긴다" |
| https://woowabros.github.io/experience/2020/05/13/rest-docs.html | 우아한형제들 정산시스템 | 2020 | 문서 테스트가 늘면 컨텍스트 생성으로 느려짐, 상위 클래스로 해결 |
| https://techblog.lycorp.co.jp/ko/api-document-integration-and-documentation-automation | LY | 2024 | springdoc 기반 문서 통합 |
| https://helloworld.kurly.com/blog/deliveryproductteam-culture-1/ | 컬리 | 2025 | ERROR 즉시 대응, WARN 5분 100회 알림, 의도된 예외는 WARN |
| https://techblog.lycorp.co.jp/ko/outage-monitoring-for-app-success | LY | 2025 | 앱: 외부 연동 실패 = warning(10분 100회), 내부 오류 = error(즉시) |
| https://tech.kakaopay.com/post/podo-elk-threadcontext-part-3/ | 카카오페이 | 2024 | MDC requestId, 자식 스레드 전파 |
| https://d2.naver.com/helloworld/1194202 | 네이버 | 2015 | Pinpoint: 추적 ID를 헤더로 전파 |
| https://tech.kakaopay.com/post/msa-transaction/ | 카카오페이 | 2022 | 성공·실패·Unknown, 멱등 API, 1회 재요청 |
| https://toss.tech/article/payments-legacy-5 | 토스페이먼츠 | 2025 | 망취소, 체인 타임아웃 정렬 |
| https://helloworld.kurly.com/blog/2026-outbox-pattern-and-retry-topic/ | 컬리 | 2026 | 아웃박스 + 재시도 토픽 |
| https://toss.tech/article/overseas-securities-server | 토스증권 | 2024 | Resilience4j RateLimiter |
| https://toss.tech/article/34481 | 토스뱅크 | 2025 | Resilience4j CircuitBreaker로 DB 우회 |
| https://toss.tech/article/engineering-note-3 | 토스페이먼츠 | 2023 | Feign 운영 중 동시성 문제 |
| https://woowabros.github.io/experience/2019/05/29/feign.html, /2019/12/20/feign2.html | 우아한형제들 광고시스템 | 2019 | Feign 적용, 기본 `NEVER_RETRY`, 재시도/비재시도 클라이언트 분리 |
| https://woowabros.github.io/experience/2017/08/21/hystrix-tunning.html | 우아한형제들 배민FRESH | 2017 | API Gateway 502 → Hystrix 스레드 격리, TPS 약 4배 |
| https://tech.kakaopay.com/post/querydsl-paging-strategy/ | 카카오페이 | 2026 | 커서·No Offset 페이징 |
| https://helloworld.kurly.com/blog/2024-mysql-paging-query-provider/ | 컬리 | 2024 | 배치 리더가 마지막 sort key로 조회 |
| https://woowabros.github.io/experience/2020/02/05/springbatch-querydsl.html | 우아한형제들 정산시스템 | 2020 | Querydsl NoOffset ItemReader, 청크 단위 트랜잭션 |

공식·오픈소스 [A]: Spring Framework 문서(`mvc-ann-rest-exceptions`, `core/resilience`, `webmvc-versioning`), Spring Boot 문서·소스(`WebMvcProperties`, `logging`, `tracing`, `rest-client`), Spring 블로그(2024-08-23, 2025-09-23, 2025-09-30), spring-cloud-openfeign README, ePages restdocs-api-spec, OpenFeign `DefaultRetryer.java`. 모두 github.com/raw.githubusercontent.com 원문.

<details>
<summary>미확인 [C?]</summary>

- 우아한형제들 서킷브레이커 techblog.woowahan.com/15694 (2024): 403. 구 블로그에 없는 2021년 이후 글이라 사본 없음. 2017 Hystrix 글로 대체.

</details>
