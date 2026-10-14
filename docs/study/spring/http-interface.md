# HTTP Interface (`@PostExchange`)

## 한 줄 요약

외부 API 를 호출할 때 구현 클래스를 직접 쓰지 않고, **인터페이스에 메서드만 선언**하면 Spring 이 구현체(프록시)를 만들어 준다. 대신 각 파라미터가 요청의 어디에 들어갈지(body, query, path)를 **어노테이션으로 반드시 알려 줘야** 한다.

## 이 프로젝트에서 일어난 일

`infra/similarity/SimilarityClient.java`:

```java
public interface SimilarityClient {
    @PostExchange("/similarity")
    SimilarityResponse score(SimilarityRequest request);   // ← 어노테이션 없음
}
```

- `request` 를 body 에 넣을지 query 에 넣을지 Spring 이 알 수 없다. 그래서 호출하는 순간 `IllegalStateException` 이 난다.
- 결과적으로 채점 API(`POST /api/v1/answers`)가 **항상 500** 이다 (current-state F1, 실행으로 확인).
- 컴파일도 되고 앱도 정상 기동된다. **호출해야만 드러나는 결함**이라 테스트가 없으면 놓친다.

## 동작 원리

```
AnswerService ──▶ SimilarityClient (인터페이스)
                        │  Spring 이 만든 프록시
                        ▼
                  어노테이션을 읽어 HTTP 요청 조립
                  - @PostExchange("/similarity") → POST /similarity
                  - @RequestBody → 객체를 JSON body 로
                        │
                        ▼
                    RestClient 가 실제 전송
```

프록시는 런타임에 어노테이션만 보고 요청을 만든다. 어노테이션이 없는 파라미터는 처리할 방법이 없어서 예외가 난다.

## 코드로 보기

```java
// ✗ 현재
SimilarityResponse score(SimilarityRequest request);

// ✓ 수정 (FIX-F1 — 1줄)
SimilarityResponse score(@RequestBody SimilarityRequest request);
```

자주 쓰는 파라미터 어노테이션:

| 어노테이션 | 들어가는 곳 | 예 |
|---|---|---|
| `@RequestBody` | 요청 body (JSON) | `score(@RequestBody SimilarityRequest req)` |
| `@RequestParam` | 쿼리스트링 `?level=1` | `find(@RequestParam int level)` |
| `@PathVariable` | URL 경로 `/items/{id}` | `get(@PathVariable String id)` |

## 이 프로젝트의 안전장치

ArchUnit 규칙 R7(`ConventionArchitectureTest`)이 "HTTP Interface 메서드의 모든 파라미터에 바인딩 어노테이션이 있는지" 를 검사한다. 지금은 위반 중이라 `@Disabled` 상태이고, `FIX-F1` 을 하면서 켠다. 그 뒤로는 같은 실수를 빌드가 막는다.

## 기억할 것

1. HTTP Interface 메서드의 **모든 파라미터**에 `@RequestBody` / `@RequestParam` / `@PathVariable` 중 하나를 붙인다.
2. 이 실수는 컴파일과 기동을 통과하므로, 호출하는 테스트가 있어야 잡힌다.
3. Boot 4 에는 `RestClient.Builder` 빈이 없다. 클라이언트를 설정할 때 `RestClient.builder()` 정적 메서드를 쓴다.

## 더 읽을거리

- Spring Framework — HTTP Interface: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface
- 이 저장소의 근거: `docs/ai/context/current-state.md` F1, §7.2 P8
