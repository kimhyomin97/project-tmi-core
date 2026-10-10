# 컨벤션 제안 (conventions-proposal)

> 상태: **제안 — 미확정**. 결정 항목은 [decisions-needed.md](./decisions-needed.md) 에 D 번호로 연결했다.
> 범위: 네이밍 · 예외 처리 · DTO 변환 · 트랜잭션 경계 · 로깅 · 테스트 (+ 포맷 도구).
> 기존 규칙 [05_개발컨벤션.md](../public/05_개발컨벤션.md), [06_코드가이드라인.md](../public/06_코드가이드라인.md) 을 **대체하지 않고 고친다**. 각 항목에 "05/06 과의 관계" 를 적었다.
> 구조 선택(architecture-proposal.md D1)과 무관하게 적용되는 규칙이 기본이다. **규칙은 같고 After 코드의 클래스 위치·이름만 선택지에 따라 다른 경우** `[B 전제]` 로 표시하고 A′ 형태를 한 줄 덧붙였다.
> 개정: Round 1 리뷰([reviews/conventions-proposal-round-1.md](./reviews/conventions-proposal-round-1.md)) 반영 — `BaseException` 생성자 NPE(C1), 상위 `handleExceptionInternal` 동작 보존(M1), Toss 예시를 설계 §6 과 일치(M2), §8 재작성(M5).

## 0. 검증 방법

이 문서의 After 코드 중 **테스트 코드와 `GlobalExceptionHandler`·설정 변경은 실제로 컴파일·실행해 확인했다.** `src/main` 수정이 금지되어 있으므로 다음 방식을 썼다 (검증용 파일은 실행 후 삭제, 커밋하지 않음).

| 검증 대상 | 방법 | 결과 |
|---|---|---|
| §2.1 basename 수정 | `@WebMvcTest(properties = "spring.messages.basename=messages/messages")` | en/ko 메시지 정상 해석 ✓ |
| §2.2 `GlobalExceptionHandler` After (1차) | 테스트 소스셋에 Lombok 을 걷어낸 같은 로직(`@Slf4j`→`LoggerFactory`, `@RequiredArgsConstructor`→생성자)을 다른 이름으로 두고, `@WebMvcTest` 에서 기존 핸들러를 `excludeFilters` 로 빼고 `@Import` | 7개 시나리오 중 7개 의도대로 동작 (아래 §2.2 표) ✓ |
| §2.2 + §2.3 + §2.1 After (2차, Round 1 반영본) | 전용 git worktree 의 `src/main` 에 **문서 After 코드를 그대로** 적용 (worktree 는 실행 후 삭제, 이 브랜치의 `src/main` 은 불변) + `@WebMvcTest` + `OutputCaptureExtension` | 404 → `"Sentence not found: x"` + **INFO, 스택트레이스 없음** / 503 → **ERROR + 원인 스택트레이스** / 깨진 JSON 400 `BAD_REQUEST` / text/plain 415 / DELETE 405 + `Allow` ✓ |
| §6 테스트 예시 3종 | 현재 코드에 그대로 실행 | `GradeTest` 통과, `SimilarityClientTest` 는 **F1 을 재현하며 실패**, 메시지 테스트는 **F2 를 재현하며 실패** — 의도한 결과 ✓ |

검증하지 못한 것: §3·§4 의 서비스 코드 After (구조 변경을 동반 — examples/ 에 있으며 컴파일 대상 아님), §4.3 Toss 예시 (WIP 클래스 미구현). §7 Spotless 는 사본에서 적용 결과를 계수 (current-state.md §7.3).

---

## 1. 네이밍

### 1.1 정적 팩토리 이름 — `from` 과 `of` 를 구분한다

| 규칙 | 의미 |
|---|---|
| `from(source, ...extras)` | **첫 인자가 변환 원천**(엔티티·다른 DTO)일 때 |
| `of(values...)` | 원천 객체 없이 **값을 조립**할 때 |
| `create(...)` | 엔티티 생성 (06 §1.1 유지) |

Before — 원천이 있는데 `of` (`answer/dto/AnswerResponse.java:30`)

```java
public static AnswerResponse of(SentenceEntity sentence, String userAnswer, double score) {
```

After `[B 전제]` — A′ 이면 `SentenceDetail` 이 `sentence.dto` 패키지에 있을 뿐 시그니처 동일

```java
public static AnswerResponse from(SentenceDetail sentence, String userAnswer, double score, Grade grade) {
```

- 유지: `SentenceResponse.from(SentenceEntity)`, `ErrorResponse.of(code, message)` (값 조립) — 이미 규칙에 맞다.
- 근거: JDK 의 `LocalDate.from(TemporalAccessor)`(변환) / `List.of(...)`(조립) 관례. 출처: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html#from(java.time.temporal.TemporalAccessor) (접근 미확인 — 관례 설명용, 결정 근거 아님).
- 이 규칙 자체(from/of 구분)는 구조와 무관하다.
- 05/06 관계: 06 §5.1 "Response 는 `from(Entity)`" 를 일반화. 06 수정 필요 없음.

### 1.2 리포지토리 쿼리 메서드 — 조건을 이름에 드러낸다

Before (`sentence/repository/SentenceRepository.java:22`)

```java
Optional<SentenceEntity> findRandomBy(@Param("level") Level level, @Param("category") Category category);
```

After

```java
Optional<SentenceEntity> findRandomActiveByLevelAndCategory(@Param("level") Level level, @Param("category") Category category);
```

- 이유: 06 §2.1 "모든 조회 메서드에 `AndDeletedAtIsNull` 명시" 의 목적은 soft delete 조건을 **이름으로** 보이게 하는 것. `@Query` 메서드는 파생 규칙이 없으므로 `Active` 로 대신 드러낸다.
- SpEL `:#{#level.name()}` 은 **유지** — **native query** 에서는 필요하다 (current-state.md §4.3; round-2 리뷰가 Spring Data `@Query(nativeQuery = true)` 경유로도 실패를 재현. JPQL 에서는 `:level` 바인딩이 동작). 06 §2.3 의 "자동 바인딩" 문구는 native 한정으로 정정 (→ D8).

### 1.3 로그 메시지 형식 — `"{대상} {동작}: key=value"`

Before (`answer/service/AnswerService.java:42,45` — 같은 클래스 안에서 혼재)

```java
log.info("Similarity 호출 : userAnswerLength={} referenceLength={}", ...);
log.info("Similarity 응답: score={}", response.score());
```

After

```java
log.info("Similarity 호출: answerLength={} referenceLength={}", ...);
log.info("Similarity 응답: score={}", score);
```

- 콜론 앞 공백 없음, `key=value` 공백 구분. 06 §3.2 예시(`"문장 출제: ..."`)와 일치. 기계적 규칙이라 리뷰 체크리스트에 넣는다 (ai-workflow.md §3).

### 1.4 유지하는 네이밍 (변경 비권장)

| 항목 | 현재 | 판단 |
|---|---|---|
| 헝가리안 접두사 `IErrorType`, `ETmiErrorType`, `ELogLevel` | 05 §5.1·99 §2 에 이 이름으로 정착 (접두사를 택한 이유는 문서에 없음) | **유지**. Java 관례와 다르지만 변경 시 `src/main` 7파일·33회 참조 수정 (`grep -rhoE "IErrorType|ETmiErrorType|ELogLevel" src/main | wc -l`), 이득은 관례 일치뿐 (→ D10, 낮은 우선순위) |
| 엔티티 접미사 `XxxEntity` | 06 §1.1 | 유지. 선택지 B 의 공개 record(`SentenceDetail`)와 이름이 겹치지 않는 장점 |
| 테스트 메서드 `shouldXxx_whenYyy` + 한국어 `@DisplayName` | 06 §7.2 | 유지 |

---

## 2. 예외 처리

### 2.1 메시지 번들 경로 — 즉시 수정 (F2)

Before (`src/main/resources/application.properties` — `spring.messages.*` 키 없음)

```properties
rest.api.version=/api/v1
```

After

```properties
rest.api.version=/api/v1

# messages/messages*.properties — 기본 basename(messages)은 classpath 루트만 찾는다
spring.messages.basename=messages/messages
# messages_en 이 없으므로 서버 로케일(ko 등)로 폴백하지 않고 기본 파일(영문)을 쓴다
spring.messages.fallback-to-system-locale=false
```

- 근거: research.md §3.2 ([J] `MessageSourceProperties`, `ResourceBundleCondition`), current-state.md §3.3(b)·§7.2 P2 [실행].
- 검증: §0 표. basename 만 주면 en/ko 해석은 되지만, 서버 기본 로케일이 ko 이면 `Accept-Language: en` 에도 한글이 나온다 (round-1 리뷰 M8 [실행·리뷰]) → 두 줄 모두 필요.
- 05/06 관계: 05 §5.5 "messages.properties 영문(기본)" 의도를 실제로 동작하게 함. 99 §3 에 설정 2줄 추가.

### 2.2 `GlobalExceptionHandler` — Spring MVC 표준 예외를 전부 받는다 (F3)

**규칙**

1. `GlobalExceptionHandler` 는 `ResponseEntityExceptionHandler` 를 상속하고 `handleExceptionInternal` 을 오버라이드해 body 만 기존 `ErrorResponse` 로 바꾼다. 응답 형식(`{error:{code,message}}`)은 **그대로** — 외부 계약(03 §1.4) 변경 없음.
2. 4xx 는 `INFO` + 스택트레이스 없음, 5xx 는 `ERROR` + **`Throwable` 마지막 인자** (스택트레이스).
3. 개별 매핑이 없는 4xx 는 `TMI.COMMON.BAD_REQUEST` (현재 사용처 0 인 기존 상수 재사용), 5xx 는 `INTERNAL_ERROR`.

Before (`common/exception/GlobalExceptionHandler.java:31-78` 요지) — 표준 예외 5종을 개별 `@ExceptionHandler` 로 나열, 나머지는 catch-all 500

```java
@ExceptionHandler(MethodArgumentNotValidException.class) ...
@ExceptionHandler(MethodArgumentTypeMismatchException.class) ...
@ExceptionHandler(MissingServletRequestParameterException.class) ...
@ExceptionHandler(NoResourceFoundException.class) ...
@ExceptionHandler(HttpRequestMethodNotSupportedException.class) ...
@ExceptionHandler(Exception.class)   // 깨진 JSON, 415 등이 모두 여기로 → 500
...
logLevel.log(log, "[{}] {} - {}", code, message, e.getMessage());   // 스택트레이스 없음
```

After (검증된 코드 — §0)

```java
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException e) {
        return respond(e.getHttpStatus(), e.getErrorType(), e.getLogLevel(), e, e.getArgs());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ETmiErrorType.INTERNAL_ERROR, ELogLevel.ERROR, e);
    }

    // Spring MVC 표준 예외 전부(400/404/405/415/...)가 여기로 온다. 분류·상태 코드는 상위 클래스가 결정.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // 상위 구현의 두 동작을 보존: (1) 이미 commit 된 응답엔 쓰지 않음 (2) 500 이면 원인 예외를 요청 속성에 기록
        if (request instanceof ServletWebRequest swr && swr.getResponse() != null && swr.getResponse().isCommitted()) {
            return null;
        }
        if (status.value() == 500) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        ResponseEntity<ErrorResponse> mapped = switch (ex) {
            case MethodArgumentNotValidException e -> respondValidation(e);
            case MissingServletRequestParameterException e ->
                    respond(status, ETmiErrorType.MISSING_PARAMETER, ELogLevel.INFO, e, e.getParameterName());
            case TypeMismatchException e ->
                    respond(status, ETmiErrorType.INVALID_REQUEST, ELogLevel.INFO, e, e.getPropertyName(), e.getValue());
            case NoResourceFoundException e ->
                    respond(status, ETmiErrorType.NOT_FOUND_URL, ELogLevel.INFO, e, e.getHttpMethod(), e.getResourcePath());
            case HttpRequestMethodNotSupportedException e ->
                    respond(status, ETmiErrorType.METHOD_NOT_SUPPORTED, ELogLevel.INFO, e, e.getMethod());
            default -> status.is4xxClientError()
                    ? respond(status, ETmiErrorType.BAD_REQUEST, ELogLevel.INFO, ex)
                    : respond(status, ETmiErrorType.INTERNAL_ERROR, ELogLevel.ERROR, ex);
        };
        // headers 유지 — 405 의 Allow 헤더 등
        return ResponseEntity.status(mapped.getStatusCode()).headers(headers).body(mapped.getBody());
    }

    private ResponseEntity<ErrorResponse> respondValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElseGet(() -> resolve(ETmiErrorType.INVALID_REQUEST));
        log.info("[{}] {}", ETmiErrorType.INVALID_REQUEST.getErrorCode(), message);
        return ResponseEntity.badRequest().body(ErrorResponse.of(ETmiErrorType.INVALID_REQUEST.getErrorCode(), message));
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatusCode status, IErrorType type, ELogLevel level,
                                                  Exception e, Object... args) {
        String code = type.getErrorCode();
        String message = resolve(type, args);
        if (level == ELogLevel.ERROR) {
            level.log(log, "[{}] {} args={}", code, message, Arrays.toString(args), e);   // Throwable 마지막 → 스택트레이스
        } else {
            level.log(log, "[{}] {} args={} cause={}", code, message, Arrays.toString(args), e.toString());
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }

    private String resolve(IErrorType type, Object... args) {
        return messageSource.getMessage(type.getErrorCode(), args, type.getErrorCode(), LocaleContextHolder.getLocale());
    }
}
```

필요 import: `org.springframework.beans.TypeMismatchException`, `org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler`, `org.springframework.web.context.request.{WebRequest,ServletWebRequest}`, `org.springframework.web.util.WebUtils`, `org.springframework.http.{HttpHeaders,HttpStatusCode}`, `org.jspecify.annotations.Nullable`, `java.util.Arrays`.

- 규칙 2(4xx 는 INFO)는 **§2.3 을 함께 적용해야** 도메인 예외에도 성립한다. §2.2 만 적용하면 `SentenceException.NotFound` 는 여전히 `getLogLevel() == ERROR` (round-1 리뷰 m3 [실행]).
- 405 등에서는 상위 클래스의 `PageNotFound` 로거가 별도로 `WARN` 한 줄을 남긴다 (round-1 m4 [실행]) → 이중 로깅을 원치 않으면 `logging.level.org.springframework.web.servlet.PageNotFound=ERROR`.
- `Accept: application/xml` 요청에서 예외가 나면 JSON 에러 응답을 쓸 수 없어 `HttpMediaTypeNotAcceptableException` 이 된다 — AS-IS 와 같은 동작, 개선 대상 아님 (round-1 m11).

검증 결과 (§0 방식, 기존 핸들러 대신 After 핸들러 + §2.1 basename 적용)

| 요청 | AS-IS (current-state §7.2) | After |
|---|---|---|
| 404 `SentenceException.NotFound` | message = 코드 문자열 | `"Sentence not found: level=LEVEL_5, category=TECH"`, ko 헤더 시 `"영어 문장을 찾을 수 없습니다: ..."` |
| 깨진 JSON | 500 `INTERNAL_ERROR` | **400** `TMI.COMMON.BAD_REQUEST` |
| `Content-Type: text/plain` | 500 | **415** `TMI.COMMON.BAD_REQUEST` |
| 필수 파라미터 누락 | 400 `MISSING_PARAMETER`, message = 코드 | 400 `MISSING_PARAMETER`, `"Required parameter category is missing."` |
| 잘못된 enum 값 | 400 `INVALID_REQUEST` | 400 `INVALID_REQUEST` (동일) |
| `DELETE /answers` | 405 | 405 + `Allow` 헤더 유지 |
| body 검증 실패 | 400 field 메시지 | 400 field 메시지 (동일) |

주의

- `MissingServletRequestParameterException` 은 계속 `MISSING_PARAMETER` 를 낸다. 03 §2.1 은 `INVALID_REQUEST` 라고 적었으므로 **03 을 고칠지 코드를 고칠지** 결정 필요 (→ D6, 추천: 03 수정 — 코드가 더 구체적).
- `@ExceptionHandler(Exception.class)` 와 상위 클래스의 `@ExceptionHandler` 목록이 공존해도 더 구체적인 타입이 우선한다 (Spring MVC 의 예외 핸들러 해석 규칙, research.md §3.1 [S]). 검증에서 충돌 없음.
- 응답을 RFC 9457 `ProblemDetail` 로 바꾸는 것은 외부 계약 변경이라 별도 결정 (→ D2, 추천: 현행 형식 유지).
- 05/06 관계: 05 §5.4 "추가 `@RestControllerAdvice` 작성 금지" 유지 (핸들러는 여전히 1개). 06 §4.5 표에 415·깨진 JSON 행 추가.

### 2.3 `BaseException` — 기본 로그 레벨을 상태 코드로 결정하고, 죽은 API 를 정리

Before (`common/exception/BaseException.java:15-16,46-56`) — 기본값 `ERROR` 고정, 체이닝 API 호출처 0

```java
private ELogLevel logLevel = ELogLevel.ERROR;
private String logMessage;
...
public <T extends BaseException> T logLevel(ELogLevel logLevel) { ... }     // 호출처 0
public <T extends BaseException> T logMessage(String logMessage) { ... }   // 호출처 0, 읽는 곳도 0
```

After — **생성자 4개 모두** 하나의 경로로 위임 (Round 1: 1개만 보여 404 에서 NPE 가 나던 결함 수정, §0 2차 검증)

```java
private ELogLevel logLevel;

protected BaseException(HttpStatus httpStatus, IErrorType errorType) {
    this(httpStatus, errorType, null, EMPTY_ARGS);
}

protected BaseException(HttpStatus httpStatus, IErrorType errorType, Throwable cause) {
    this(httpStatus, errorType, cause, EMPTY_ARGS);
}

protected BaseException(HttpStatus httpStatus, IErrorType errorType, Object... args) {
    this(httpStatus, errorType, null, args);
}

protected BaseException(HttpStatus httpStatus, IErrorType errorType, Throwable cause, Object... args) {
    super(errorType.getErrorCode(), cause);
    this.httpStatus = httpStatus;
    this.errorType = errorType;
    this.args = args;
    this.logLevel = httpStatus.is5xxServerError() ? ELogLevel.ERROR : ELogLevel.INFO;   // 404 → INFO
}
// logLevel(...) 체이닝은 유지 (명시적 override 용). logMessage 필드·메서드는 삭제.
```

- `(HttpStatus, IErrorType, Throwable)` 과 `(HttpStatus, IErrorType, Object...)` 의 오버로드 해석은 AS-IS 와 같다 (Throwable 인자는 전자로).

- 효과: `SentenceException.NotFound`(404)가 `INFO`, `SimilarityException.*`(502/503)는 `ERROR` + 스택트레이스(§2.2 규칙 2).
- 05/06 관계: 99 §2.6 표의 `logMessage` 필드·chaining 행 수정, "기본 logLevel" 서술 추가. 05 §6.6 "`ERROR` — 예외 발생" 을 "`ERROR` — 서버 측 원인(5xx)" 으로 수정 (→ §5.1).

### 2.4 외부 시스템 예외 변환 — 분류를 `RestClientResponseException` 기준으로

Before (`answer/service/AnswerService.java:47-55`)

```java
} catch(HttpStatusCodeException e) {          // UnknownHttpStatusCodeException 누락
    ...
} catch(ResourceAccessException e) {
    log.error("Similarity 네트워크 오류", e);   // 핸들러와 이중 로깅
    throw new SimilarityException.Unavailable(e);
}
// read timeout·응답 역직렬화 실패(RestClientException) → catch-all 500 (current-state §3.3(f) [실행·리뷰])
```

After ([examples/answer-scoring/option-b-modular/similarity/SimilarityScorer.java](./examples/answer-scoring/option-b-modular/similarity/SimilarityScorer.java))

```java
} catch (RestClientResponseException e) {        // 4xx/5xx + 비표준 상태 코드
    throw e.getStatusCode().is4xxClientError()
            ? new SimilarityException.RequestInvalid(e)
            : new SimilarityException.Unavailable(e);
} catch (ResourceAccessException e) {            // 연결 거부 등 연결 단계 I/O 오류
    throw new SimilarityException.Unavailable(e);
} catch (RestClientException e) {                // read timeout (SimpleClientHttpRequestFactory), 역직렬화 실패
    throw new SimilarityException.Unavailable(e);  // 이 catch 를 지우면 타임아웃이 503 → 500 으로 회귀
}
// + 빈 응답(null) → Unavailable, 점수는 Math.clamp(score, 0.0, 1.0) — examples/answer-scoring/README.md §2
```

- 변환 위치: `[B 전제]` 외부 시스템 모듈의 공개 클래스(`SimilarityScorer`) 하나. 선택지 A 이면 서비스의 private 메서드에 둔다.
- 근거: `UnknownHttpStatusCodeException extends RestClientResponseException` 이며 `HttpStatusCodeException` 의 하위가 아니다 — spring-web 7.0.3 바이트코드(`javap`)로 확인 (round-1 리뷰 m5 [실행·리뷰]).
- 05/06 관계: 05 §5.7 예시 코드를 위 After 로 교체하고, 마지막 문단("`HttpClientErrorException`/`HttpServerErrorException`/`ResourceAccessException` 던지므로 위 try/catch 그대로 적용")도 수정.

### 2.5 HTTP Interface 클라이언트 규칙

| 규칙 | 근거 |
|---|---|
| 복합 타입 파라미터에는 `@RequestBody` (또는 `@RequestParam`·`@PathVariable`) 필수 | F1 — 누락 시 호출 전에 `IllegalStateException` (current-state §7.2 P8). ArchUnit R7 로 강제 |
| 클라이언트마다 `MockRestServiceServer` 테스트 1개 이상 (§6.2) | F1 은 이 테스트 하나로 잡힘 |
| `RestClient.builder()` 정적 호출 유지. `RestClient.Builder` 주입은 `spring-boot-starter-restclient` 추가 결정 후 | Boot 4 에서 Builder 빈 없음 (current-state §4.6) → D7 |
| 설정은 `@ConfigurationProperties` record + `@EnableConfigurationProperties` | `@Value` 필드 주입은 불변·검증 불가 (`RestClientConfig.java:18-22`) |

Before (`infra/similarity/SimilarityClient.java:9-10`)

```java
@PostExchange("/similarity")
SimilarityResponse score(SimilarityRequest request);
```

After

```java
@PostExchange("/similarity")
SimilarityResponse score(@RequestBody SimilarityRequest request);
```

---

## 3. DTO 변환

### 3.1 서비스의 public 메서드는 엔티티를 반환하지 않는다

Before — 엔티티가 컨트롤러로 나감 (`sentence/service/SentenceService.java:25`, `sentence/controller/SentenceController.java:26-27`)

```java
// SentenceService
public SentenceEntity getRandom(Level level, Category category) { ... }
// SentenceController
SentenceEntity sentence = sentenceService.getRandom(level, category);
return ResponseEntity.ok(SentenceResponse.from(sentence));
```

After

```java
// SentenceService — 트랜잭션 안에서 변환 완료
@Transactional(readOnly = true)
public SentenceResponse getRandom(Level level, Category category) {
    log.info("문장 출제: level={} category={}", level, category);
    return sentenceRepository.findRandomActiveByLevelAndCategory(level, category)
            .map(SentenceResponse::from)
            .orElseThrow(() -> new SentenceException.NotFound("level=" + level + ", category=" + category));
}
// SentenceController
return ResponseEntity.ok(sentenceService.getRandom(level, category));
```

- 이유: (1) `answer` 는 이미 이 방식(`AnswerService.java:32`) — 두 방식 혼재(F5) 해소 (2) 엔티티가 트랜잭션 밖으로 나가지 않아야 OSIV 를 끌 수 있다 (§3.4).
- 반환 타입: 컨트롤러용 서비스 → **응답 DTO**. 다른 기능용 메서드 → **record** (`SentenceDetail`). `[B 전제]` 위치는 모듈 base package (`sentence.SentenceDetail`, `SentenceReader`); A′ 이면 `sentence.dto.SentenceDetail`, `SentenceService.getActive`.
- 강제: ArchUnit R5 (`@Service` public 메서드 반환 타입에 `@Entity` 금지).
- 이 규칙은 A′·B 모두에 적용된다 (architecture-proposal.md §2 — A′ 도 서비스 간에 record 를 쓴다). 따라서 R5 는 어느 선택지에서도 완화하지 않는다. 해제 작업: `FIX-R5`.
- 05/06 관계: 06 §4.3 예시(컨트롤러가 변환)와 §3.3 예시(서비스가 변환) 중 **§3.3 쪽으로 통일**. 06 §3.2·§4.3 코드 수정 (`getRandom` 반환 타입).

### 3.2 DTO 는 변환만, 규칙 계산은 도메인·서비스

Before (`answer/dto/AnswerResponse.java:37`) — DTO 가 등급 규칙을 실행

```java
return new AnswerResponse(..., score, Grade.fromScore(score));
```

After — 서비스가 계산해 넘김

```java
// AnswerService
return AnswerResponse.from(sentence, request.userAnswer(), score, Grade.fromScore(score));
```

- 02 §4.2 "점수 → 등급 변환은 Service 책임" 과 일치하게 됨. 규칙 자체는 `Grade.fromScore` (domain) 에 그대로.

### 3.3 기능 간 데이터 전달

- 다른 기능에 넘기는 값은 **record** 로. 엔티티·웹 응답 DTO 재사용 금지. `[B 전제]` 위치는 모듈 base package (A′ 이면 `{기능}/dto/`).
- 공개 record 는 **호출 측이 실제로 쓰는 필드만** 담는다 (`SentenceDetail(id, englishText, koreanRef)` — level·category 제외).
- 예시: [examples/answer-scoring/option-b-modular/sentence/](./examples/answer-scoring/option-b-modular/sentence/)

### 3.4 OSIV 끄기 — §3.1 적용 후

Before — `application.properties` 에 키 없음 (기본 `true`, 기동 WARN)

After

```properties
spring.jpa.open-in-view=false
```

- 근거: current-state.md §3.4 [실행] — 기본값에서 외부 호출 중 커넥션 1개 점유, `false` 에서 0개.
- **순서 조건**: §3.1 을 먼저 적용해야 한다. 엔티티가 컨트롤러로 나가는 동안 끄면 연관관계 추가 시 `LazyInitializationException` (지금은 연관관계 0개라 즉시 장애는 없음 [정적]).
- → D4.

### 3.5 MapStruct 는 도입하지 않는다

- 엔티티 2개, 매핑 3곳 규모에서 정적 팩토리로 충분. Lombok 과 annotation processor 순서 이슈(`lombok-mapstruct-binding`)가 추가 비용 (research.md §4.2).
- 재검토 조건: 매핑 대상 DTO 가 10개를 넘거나 같은 필드 매핑이 3곳 이상 중복될 때.

---

## 4. 트랜잭션 경계

### 4.1 `@Transactional` 은 "다른 빈에서 호출되는 public 메서드" 에만

Before (`answer/service/AnswerService.java:28,35-36`) — 같은 클래스에서 호출 → 무효

```java
SentenceEntity sentence = loadSentence(request.sentenceId());
...
@Transactional(readOnly = true)
protected SentenceEntity loadSentence(String sentenceId) { ... }
```

After `[B 전제]` — 조회는 소유 기능의 빈이 트랜잭션을 연다 ([examples/.../option-b-modular/sentence/SentenceReader.java](./examples/answer-scoring/option-b-modular/sentence/SentenceReader.java)). A′ 이면 `sentenceReader.getActive` 대신 `sentenceService.getActive` — 규칙(다른 빈 경유)은 같다.

```java
// AnswerService — @Transactional 없음
SentenceDetail sentence = sentenceReader.getActive(request.sentenceId());   // 다른 빈 → 프록시 경유

// SentenceReader
@Transactional(readOnly = true)
public SentenceDetail getActive(String sentenceId) { ... }
```

- 강제: ArchUnit R6 (같은 클래스 안에서 `@Transactional` 메서드 호출 금지).
- 05/06 관계: **06 §3.3 의 "권장" 예시를 교체**한다 — 그 예시 자체가 self-invocation 이다 (current-state.md F10). 06 §3.4 는 유지.

### 4.2 리포지토리 쿼리 메서드는 트랜잭션을 걸어주지 않는다 — 서비스가 책임

- 사실: `SimpleJpaRepository` 의 `readOnly` 트랜잭션은 **상속 메서드(`findById` 등)에만** 적용되고, 인터페이스에 선언한 파생·`@Query` 메서드에는 없다 (research.md §5 표 5.3, current-state.md §3.4 프로브 [실행]).
- 규칙: 리포지토리를 호출하는 서비스 메서드는 **반드시** `@Transactional` 또는 `@Transactional(readOnly = true)` 를 가진다. 리포지토리 인터페이스에는 붙이지 않는다 (경계를 한 곳에서 관리).
- 05/06 관계: 06 §3.1 "메서드별 `@Transactional`" 유지, 위 사실을 06 §2.3 주의에 추가.

### 4.3 외부 호출은 트랜잭션 밖, 오케스트레이터는 트랜잭션 없음

| 흐름 | 패턴 |
|---|---|
| 조회 → 외부 호출 → 응답 (답안 채점) | 오케스트레이터(`AnswerService.score`)는 `@Transactional` 없음. 조회 = 다른 빈의 readOnly 메서드 |
| 외부 호출 → 저장 (Toss 스냅샷, 향후 답안 이력) | 외부 호출은 오케스트레이터에서, 저장은 저장 전용 빈의 `@Transactional` 메서드 |

Toss 스냅샷 적용 예 — **트랜잭션 경계와 에러 정책만** 보이는 축약 예시. 이름·정책은 설계 문서 `docs/public/toss/01_보유자산스냅샷.md` §3(시퀀스)·§6(에러 정책)을 따른다.

```java
// toss/holdings/service/HoldingsSnapshotService.java — 트랜잭션 없음 (설계 §3 의 진입점 이름)
@Scheduled(cron = "${toss.holdings.schedule-cron}", zone = "${toss.holdings.timezone}")
public void collect() {
    long accountSeq = properties.defaultAccountSeq();              // 설계 §3 (2) toss.default-account-seq
    Instant snapshotAt = clock.instant().truncatedTo(ChronoUnit.HOURS);
    try {
        HoldingsApiResponse response = fetchWithOneRetry(accountSeq);    // 외부 호출: 트랜잭션 밖
        snapshotWriter.save(accountSeq, snapshotAt, response);           // 다른 빈 → 짧은 쓰기 트랜잭션
    } catch (HttpClientErrorException e) {
        log.warn("Toss 보유자산 조회 거부: accountSeq={} status={}", accountSeq, e.getStatusCode());   // 설계 §6: 4xx → WARN, skip
    } catch (DataIntegrityViolationException e) {
        log.info("Toss 스냅샷 중복 — 무시: accountSeq={} snapshotAt={}", accountSeq, snapshotAt);       // 설계 §6: UNIQUE 위반 → INFO
    } catch (RuntimeException e) {
        log.error("Toss 스냅샷 수집 실패: accountSeq={}", accountSeq, e);   // 재시도 후에도 실패. 스케줄러는 예외로 종료 금지 (설계 §6)
    }
}

private HoldingsApiResponse fetchWithOneRetry(long accountSeq) {
    try {
        return tossHoldingsClient.get(tokenProvider.bearer(), accountSeq);
    } catch (HttpServerErrorException | ResourceAccessException e) {        // 설계 §6: 5xx·타임아웃 → 재시도 1회
        return tossHoldingsClient.get(tokenProvider.bearer(), accountSeq);
    }
}
```

- 스케줄러는 `GlobalExceptionHandler` 를 거치지 않으므로 **스케줄러 진입점만** 직접 catch·로깅한다 (§5.1 예외).
- WIP 클래스 이름(`TossHoldingsClient`, `HoldingsApiResponse`)을 그대로 썼다 — 미구현이므로 시그니처(`get(bearer, accountSeq)`, `bearer()`)는 설계 문서 §3·00_인증.md §3 에서 가정. `clock` 주입은 테스트에서 시각 고정용.

### 4.4 유지하는 규칙

- 06 §3.1 "dirty checking 의식적 미사용 — 변경 후 명시 `save()`" 유지. 현재 쓰기 경로가 0개라 검증할 대상이 없다.

---

## 5. 로깅

### 5.1 예외 로그는 한 곳에서 — 예외 경로별 책임

| 경로 | 로그 위치 | 레벨 |
|---|---|---|
| HTTP 요청 처리 중 예외 | `GlobalExceptionHandler` **만** | 4xx `INFO` (스택 없음), 5xx `ERROR` (스택 포함) |
| 스케줄러·비동기 진입점 | 그 진입점의 catch 블록 | `ERROR` (스택 포함) |
| 그 외 (서비스·외부 클라이언트 래퍼) | **로그 없이 throw** | — |

Before — 서비스가 로그 후 rethrow, 핸들러가 다시 기록 (`AnswerService.java:53` + `GlobalExceptionHandler.java:77`)

```java
} catch(ResourceAccessException e) {
    log.error("Similarity 네트워크 오류", e);
    throw new SimilarityException.Unavailable(e);
}
```

After

```java
} catch (ResourceAccessException e) {
    throw new SimilarityException.Unavailable(e);   // 원인(e)을 cause 로 보존 → 핸들러가 스택트레이스와 함께 1회 기록
}
```

- 05/06 관계: 06 §3.5 표의 "외부 호출 실패 — ERROR `log.error(\"Similarity 실패\", e)`" 행과 06 §6.6 "throw 직전 로그 금지" 의 **충돌을 §6.6 쪽으로 해소**. 06 §3.5 행 삭제. 05 §6.6 `ERROR` 정의를 "서버 측 원인(5xx) — 스택트레이스 필수" 로 수정.

### 5.2 로그에 원인 추적 정보를 남긴다

- 현재는 예외 `args`(예: `level=LEVEL_5, category=TECH`)가 로그에 남지 않는다 (current-state.md §7.2 로그 관찰). §2.2 의 `respond(...)` 가 `args=[...]` 를 함께 기록한다.

### 5.3 SQL 로그는 프로필로

Before (`application.properties:16-17`)

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

After

```properties
# application.properties — 아래 두 줄을 삭제 (기본 off)
# spring.jpa.show-sql=true
# spring.jpa.properties.hibernate.format_sql=true

# application-local.properties (신규)
logging.level.org.hibernate.SQL=DEBUG
spring.jpa.properties.hibernate.format_sql=true
```

- 앱에는 기본 프로필 설정이 없다 — `SPRING_PROFILES_ACTIVE=local` 을 주지 않으면 SQL 로그가 꺼진다 (07 §3 의 `local` 은 환경 변수 표의 예시값일 뿐).

- `show-sql` 은 stdout 직접 출력이라 로그 레벨·포맷 통제를 벗어난다 (estimated — Hibernate 일반 동작). 05 §6.7 "운영 환경 DEBUG 활성화 금지" 와 맞추려면 프로필 분리가 필요.
- 05/06 관계: 07 §3 의 `SPRING_PROFILES_ACTIVE` (`local`/`prod`) 와 연결. 현재 프로필 파일 없음.

### 5.4 민감 정보

- Toss `access_token`·`client_secret` 은 로그 금지 (toss/00_인증.md §5 유지). 리뷰 체크리스트에 포함 (ai-workflow.md §3).

---

## 6. 테스트

### 6.1 기본 원칙

| # | 규칙 | 이유 |
|---|---|---|
| T1 | `./gradlew test` 는 **외부 DB·Docker·네트워크 없이** 통과해야 한다 | AI 루프·CI 의 기본 신호 (ai-workflow.md §1). 현재 유일한 테스트가 이를 어김 (blockers.md §2) |
| T2 | 버그 수정은 **실패하는 테스트를 먼저** 커밋 | F1~F3 이 모두 테스트 부재로 잠복 (current-state F8) |
| T3 | 실 DB 가 필요한 테스트는 `@Tag("integration")` + 별도 Gradle 태스크 | T1 과 양립 |
| T4 | 테스트 소스셋엔 **Lombok 이 없다** — 쓰려면 `testCompileOnly`/`testAnnotationProcessor` 추가, 아니면 쓰지 않는다 | `build.gradle:32,34` (`compileOnly`, `annotationProcessor`) 가 main 에만 선언 (§0 검증 중 컴파일 오류로 확인 [실행]) |
| T5 | Boot 4 API 사용: `@MockitoBean` (`@MockBean` 제거됨), `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | research.md §6.1 [J], §0 컴파일 확인 [실행] |

### 6.2 계층별 테스트와 잡는 결함

| 계층 | 도구 | 대상 | 잡았을 결함 | DB/Docker |
|---|---|---|---|---|
| 도메인 단위 | JUnit + AssertJ | `Grade.fromScore` 경계값 | (회귀 방지) | 불필요 |
| 웹 슬라이스 | `@WebMvcTest` + `@MockitoBean` | 컨트롤러 + `GlobalExceptionHandler` + 메시지 | **F2, F3** | 불필요 |
| HTTP 클라이언트 | `MockRestServiceServer.bindTo(RestClient.Builder)` | `SimilarityClient` 요청 형식 | **F1** | 불필요 |
| HTTP 타임아웃 | JDK `com.sun.net.httpserver.HttpServer` 로 지연 응답 + 짧은 readTimeout (MockRestServiceServer 는 소켓 타임아웃을 재현하지 못함 — estimated) | 예외 변환 (catch 3개) | read timeout → 500 (current-state §3.3(f)) | 불필요 |
| 서비스 단위 | Mockito `@ExtendWith(MockitoExtension.class)` | `AnswerService` 분기, 예외 변환 | §2.4 분류 빈틈 | 불필요 |
| 영속성 | `@DataJpaTest` + Testcontainers `@ServiceConnection` | native query, soft delete 조건 | SpEL 바인딩 (current-state §4.3) | **Docker** |
| 아키텍처 | ArchUnit | 패키지 규칙 R1~R7 | F4(R6), F1(R7), F6(R2·R3) | 불필요 |

### 6.3 Before / After 예시 (검증됨 — §0)

**HTTP 클라이언트 — F1 을 잡는 테스트**

Before: 테스트 없음 (`src/test` 에 `CoreApplicationTests` 1개).

After (현재 코드에서 실행하면 `IllegalStateException: Could not resolve parameter [0] ...` 로 **실패** = F1 재현)

```java
class SimilarityClientTest {

    @Test
    @DisplayName("요청 본문을 JSON 으로 보내고 score 를 읽는다")
    void shouldPostJsonBody_whenScore() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://similarity");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SimilarityClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build())).build()
                .createClient(SimilarityClient.class);

        server.expect(requestTo("http://similarity/similarity"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"answer\":\"좋아\",\"reference\":\"좋아요.\"}"))
                .andRespond(withSuccess("{\"score\":0.91}", MediaType.APPLICATION_JSON));

        assertThat(client.score(new SimilarityRequest("좋아", "좋아요.")).score()).isEqualTo(0.91);
        server.verify();
    }
}
```

**웹 슬라이스 — F2 를 잡는 테스트**

After (현재 코드에서 실행하면 `expected:<Sentence not found: ...> but was:<TMI.SENTENCE.NOT_FOUND>` 로 **실패** = F2 재현)

```java
@WebMvcTest(SentenceController.class)
class SentenceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SentenceService sentenceService;

    @Test
    @DisplayName("문장이 없으면 404 와 영문 메시지")
    void shouldReturn404WithMessage_whenNoSentence() throws Exception {
        given(sentenceService.getRandom(Level.LEVEL_5, Category.TECH))
                .willThrow(new SentenceException.NotFound("level=LEVEL_5, category=TECH"));

        mockMvc.perform(get("/api/v1/sentences/random").param("level", "LEVEL_5").param("category", "TECH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TMI.SENTENCE.NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Sentence not found: level=LEVEL_5, category=TECH"));
    }
}
```

**웹 슬라이스 — F3 을 잡는 테스트** (FIX-F3 의 verify. §0 2차 검증과 같은 시나리오)

```java
@WebMvcTest(AnswerController.class)          // 슬라이스는 지정한 컨트롤러만 등록 — SentenceController 슬라이스에선 /answers 가 404
class AnswerControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AnswerService answerService;

    @Test
    @DisplayName("깨진 JSON 은 400 BAD_REQUEST")
    void shouldReturn400_whenBodyMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/answers").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TMI.COMMON.BAD_REQUEST"));
    }
}
```

**도메인 단위 — 경계값**

```java
class GradeTest {
    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("점수 경계값마다 등급이 바뀐다")
    @CsvSource({"0.9, EXCELLENT", "0.8999, GOOD", "0.7, GOOD", "0.6999, FAIR", "0.5, FAIR", "0.4999, POOR", "0.0, POOR"})
    void shouldMapBoundary_whenFromScore(double score, Grade expected) {
        assertThat(Grade.fromScore(score)).isEqualTo(expected);
    }
}
```

### 6.4 기존 `CoreApplicationTests` 처리

Before (`src/test/java/com/tmi/core/CoreApplicationTests.java`) — 실 PostgreSQL 필요, `./gradlew build` 를 환경에 묶음

After (두 안 — D3)

| 안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| (a) **추천** | `@Tag("integration")` + `@Import(TestcontainersConfig.class)` (`@ServiceConnection PostgreSQLContainer`), `test` 태스크는 `excludeTags 'integration'`, `integrationTest` 태스크 신설 | T1 충족, 통합 테스트도 재현 가능 | Docker 필요 (`integrationTest` 한정) |
| (b) | 로컬 PostgreSQL 유지 (README §1 의 docker run) | 변경 없음 | `./gradlew build` 가 환경 의존 — AI 루프에서 매번 DB 기동 필요 |

- H2 는 비권장 — `ORDER BY RANDOM()`, `jsonb` 등 PostgreSQL 종속 (06 §7.2 유지).
- Testcontainers 2.x 아티팩트: `org.testcontainers:testcontainers-postgresql`, Boot BOM 관리 (research.md §0 표).

---

## 7. 포맷 도구

| 규칙 | 내용 |
|---|---|
| 도구 | Spotless 8.10.4 + palantir-java-format 2.102.0 (research.md §7) |
| 이유 | 4-space 들여쓰기 = 05 §2.1 과 일치 (google-java-format 기본은 2-space). `spotlessApply` 로 자동 수정 → AI 루프에서 사람 개입 없이 닫힘 |
| 도입 방식 | `build.gradle` 에 플러그인 추가, **`enforceCheck = false`** 로 `check`/`build` 에 묶지 않음. `src/main` 수정 금지 규칙 때문에 기존 코드를 지금 포맷할 수 없기 때문 |
| 전환 조건 | 사람이 `./gradlew spotlessApply` 단독 커밋을 승인하면 `enforceCheck = true` 로 전환 (→ D5) |

7단계에서 실제로 추가하고 `spotlessCheck` 를 실행한 결과는 [current-state.md §7.3](./current-state.md#73-포맷-검사-spotless).

---

## 8. 기존 문서 수정 목록 (승인 시 — `DOC-1`)

본문 절과 1:1 로 대조했다. "근거" 열이 이 문서의 절이다.

| 문서 | 절 | 수정 | 근거 |
|---|---|---|---|
| 05 | §5.7 | 예외 분류 예시를 `RestClientResponseException` 기준으로, 마지막 문단 정정 | §2.4 |
| 05 | §6.5 | 예시 `log.error("유사도 서버 호출 실패 …", e)` 를 "throw 직전 로그 금지" 와 맞게 교체 | §5.1 |
| 05 | §6.6 | `ERROR` = 서버 측 원인(5xx), 4xx = `INFO` | §2.3, §5.1 |
| 06 | §2.2 | 예시 `findRandomBy` → `findRandomActiveByLevelAndCategory`, native 에선 SpEL 유지 | §1.2 |
| 06 | §2.3 | "enum 자동 바인딩" 을 native 한정 정정 + "선언 쿼리 메서드엔 기본 트랜잭션 없음" 추가 | §1.2, §4.2 |
| 06 | §3.2 | 예시 `getRandom` 반환 타입 → `SentenceResponse` | §3.1 |
| 06 | §3.3 | "권장" 예시를 다른 빈 호출로 교체 (self-invocation 제거) | §4.1 |
| 06 | §3.5 | "외부 호출 실패 — ERROR" 행 삭제 | §5.1 |
| 06 | §4.3 | 컨트롤러가 서비스의 DTO 를 그대로 반환하는 예시로 | §3.1 |
| 06 | §4.5 | 415·깨진 JSON·빈 body 행 추가 | §2.2 |
| 06 | §5.3 | `@Size(min = 1)` 제거 — `@NotBlank` 와 중복, 빈 문자열 메시지 비결정 | current-state.md §1.2 `SubmitAnswerRequest`, §7.2 P6 |
| 06 | §7.1 | "Phase 5 WireMock" → HTTP Interface 는 `MockRestServiceServer` 로 충분 | §6.2 |
| 06 | §7.4 | 예시 `findRandomBy` 이름 변경 | §1.2 |
| 06 | §7 (신규 항) | Boot 4 패키지·`@MockitoBean`, 계층별 표, 테스트 소스셋 Lombok 부재 | §6.1 T4·T5, §6.2 |
| 99 | §2.3 | "`RestClient` 빈" → 빈은 클라이언트 인터페이스뿐, 등록 위치는 D1 결과에 따름 | current-state.md §1.6, architecture-proposal.md §0 표 |
| 99 | §2.6 | 생성자 위임 구조, 기본 logLevel = 상태 코드 기준, `logMessage` 삭제 | §2.3 |
| 99 | §2.7 | 처리 대상 → "`ResponseEntityExceptionHandler` 상속, MVC 표준 예외 전부" | §2.2 |
| 99 | §3 | 메시지 설정 2줄 (`basename`, `fallback-to-system-locale`) | §2.1 |
| 03 | §2.1, §1.5 | 누락 시 `MISSING_PARAMETER` 로 정정, 코드 목록 보강 | §2.2 주의, decisions-needed.md D6 |
