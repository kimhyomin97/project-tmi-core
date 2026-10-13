# 예외가 HTTP 응답이 되기까지

## 한 줄 요약

컨트롤러 밖으로 던져진 예외는 `@RestControllerAdvice` 의 `@ExceptionHandler` 가 받아서 HTTP 응답으로 바꾼다. **맞는 핸들러가 없으면** 마지막 catch-all(`Exception`)에 걸려 500 이 된다. 이 프로젝트에서 엉뚱한 500 이 나오는 원인은 대부분 이것이다.

## 이 프로젝트에서 일어난 일

| 상황 | 기대 | 실제 | 원인 |
|---|---|---|---|
| 깨진 JSON 전송 | 400 | **500** | Spring MVC 의 `HttpMessageNotReadableException` 을 처리하는 핸들러가 없음 |
| `Content-Type: text/plain` | 415 | **500** | `HttpMediaTypeNotSupportedException` 핸들러 없음 |
| 유사도 서버 read timeout | 503 | **500** | POST 의 timeout 은 `RestClientException` 으로 오는데, 서비스가 이 타입을 catch 하지 않음 |
| 404 | WARN/INFO 로그 | ERROR 로그 | 모든 도메인 예외의 기본 로그 레벨이 ERROR |

(근거: `docs/ai/current-state.md` §3.3, §7.2)

## 동작 원리

```
Controller / Service 에서 throw
        │
        ▼
@RestControllerAdvice (GlobalExceptionHandler)
   ├─ @ExceptionHandler(BaseException)        → 도메인 예외: 404, 503 ...
   ├─ @ExceptionHandler(MethodArgumentNotValid) → 400
   └─ @ExceptionHandler(Exception)            → 그 밖의 모든 것: 500   ← 여기로 새는 게 문제
```

Spring 은 **가장 구체적인 타입**의 핸들러를 고른다. 그래서 특정 예외 타입에 맞는 핸들러가 없으면 `Exception` 핸들러로 떨어진다.

## 해결 방향 — `ResponseEntityExceptionHandler` 상속

Spring MVC 가 던지는 표준 예외(깨진 JSON, 잘못된 Content-Type, 지원하지 않는 메서드 등)를 **하나하나 핸들러로 만들 필요가 없다**. 이 예외들을 이미 알맞은 상태 코드로 처리해 주는 기본 클래스가 있기 때문이다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 상위 클래스가 400/405/415 등을 결정한 뒤 이 메서드를 부른다.
    // 여기서 body 만 우리 형식 {"error":{"code","message"}} 으로 바꾼다.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ...
    }
}
```

- 상태 코드는 Spring 이 정하고, 응답 형식은 우리가 정한다. 그래서 외부 API 계약(03 명세의 에러 형식)은 그대로 유지된다.
- 이것이 decisions-needed D2 의 추천안이다. 전체 After 코드는 `docs/ai/conventions-proposal.md` 에 있다.

## 외부 HTTP 호출 예외 — catch 순서

RestClient 예외의 상속 관계:

```
RestClientException                      ← 모든 RestClient 예외의 부모 (POST timeout 은 여기로 옴)
 ├─ ResourceAccessException              ← 연결 실패 등 I/O 오류
 └─ RestClientResponseException          ← 서버가 응답은 했는데 4xx/5xx
     └─ HttpStatusCodeException
         ├─ HttpClientErrorException     (4xx)
         └─ HttpServerErrorException     (5xx)
```

자식 타입부터 catch 하고, 마지막에 부모 타입(`RestClientException`)을 catch 해야 빠지는 경우가 없다.

```java
try {
    response = similarityClient.score(request);
} catch (RestClientResponseException e) {          // 4xx → 502, 5xx → 503
    ...
} catch (ResourceAccessException e) {              // 연결 실패 → 503
    throw new SimilarityException.Unavailable(e);
} catch (RestClientException e) {                  // 나머지 전부 (POST timeout 포함) → 503
    throw new SimilarityException.Unavailable(e);
}
```

현재 코드는 세 번째 catch 가 없어서 timeout 이 500 이 된다 (`FIX-F1T`).

## 기억할 것

1. 500 이 나오면 먼저 **"이 예외 타입을 잡는 핸들러가 있나"** 를 본다.
2. Spring MVC 표준 예외는 `ResponseEntityExceptionHandler` 를 상속해서 한 번에 처리한다.
3. catch 는 **자식 타입 → 부모 타입** 순서로 쓰고, 마지막에 부모 타입을 꼭 둔다.

## 더 읽을거리

- Spring Framework — REST 에러 응답 (`ResponseEntityExceptionHandler`): https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- 이 저장소의 근거: `docs/ai/current-state.md` §3.3, 제안 코드: `docs/ai/conventions-proposal.md`
