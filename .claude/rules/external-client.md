---
paths:
  - "src/main/java/**/infra/**"
  - "src/main/java/**/client/**"
  - "src/main/java/**/config/**"
  - "src/main/java/**/answer/service/**"
  - "src/main/java/**/toss/**"
---

# 외부 HTTP 호출 규칙

- **HTTP Interface(`@PostExchange` 등) 메서드의 모든 파라미터에 `@RequestBody` / `@RequestParam` / `@PathVariable` 중 하나를 붙인다.** 없으면 호출할 때마다 `IllegalStateException` (현재 `SimilarityClient.score` — 채점 API 500, `FIX-F1`, ArchUnit R7).
- `RestClient.Builder` 빈은 **없다** (Boot 4, `spring-boot-starter-restclient` 미포함, D7). `RestClient.builder()` 정적 호출을 쓴다.
- `common` 패키지는 기능 모듈에 의존하지 않는다 (R3). 클라이언트 빈 설정은 그 클라이언트의 패키지(`infra/<시스템>/`)에 둔다. 현재 `common/config/RestClientConfig` 가 위반 중 (`ARCH-A1`).
- `[제안]` 외부 호출 예외는 **catch 3개**로 도메인 예외로 바꾼다:
  1. `RestClientResponseException` — 4xx → 502 `RequestInvalid`, 5xx → 503 `Unavailable`
  2. `ResourceAccessException` — 연결 실패 → 503
  3. `RestClientException` — 나머지 (POST 의 read timeout 포함) → 503
  세 번째를 빼면 POST timeout 이 500 이 된다 (현재 `AnswerService`, `FIX-F1T`). 빈 응답(`null`)도 503.
- 외부 호출은 트랜잭션 밖에서 한다 (persistence 규칙 참고).
- 토큰·시크릿·인증 헤더는 로그에 남기지 않는다. `[제안]` throw 직전에 로그를 남기지 않는다 — `GlobalExceptionHandler` 가 한 번 기록한다.
