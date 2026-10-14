---
paths:
  - "src/test/**"
---

# 테스트 코드 규칙

- 테스트 소스셋에는 **Lombok 이 없다** (`build.gradle` 은 main 에만 선언). 생성자·getter 를 직접 쓴다.
- Boot 4: `@MockitoBean` 을 쓴다 (`@MockBean` 은 제거됨). `@WebMvcTest` 는 `org.springframework.boot.webmvc.test.autoconfigure` 패키지.
- `@WebMvcTest(X.class)` 는 지정한 컨트롤러만 등록한다. 다른 컨트롤러의 URL 은 404 가 된다.
- 외부 HTTP 클라이언트 테스트는 `MockRestServiceServer` 로 요청 형태(메서드·URL·body)를 검증한다. 예: `docs/ai/proposals/conventions-proposal.md` §6.3.
- `CoreApplicationTests` 는 실 PostgreSQL(`localhost:15432/tmi_word`)이 필요하다. 새 테스트는 DB 없이 돌게 만든다 (슬라이스 테스트, 단위 테스트). DB 가 꼭 필요하면 `@Tag("integration")` (D3 결정 전에는 사람에게 확인).
- Gradle `--tests` 를 여러 개 주면 OR 다. 특정 테스트가 실제로 돌았는지는 `build/test-results/test/TEST-*.xml` 의 `tests`·`skipped` 로 확인한다.
- 버그 수정은 **실패하는 테스트를 먼저** 커밋한다. 아직 고치지 않은 결함의 테스트는 `@Disabled("<작업 ID> <사유>")` 로 두고, 그 작업에서 지운다.
- `com.tmi.core.architecture` 의 `@Disabled` 는 알려진 ArchUnit 위반이다. 고쳤으면 `@Disabled` 를 지워 통과를 확인한다. 새 위반을 `@Disabled` 로 숨기지 않는다.
- 테스트 메서드 이름: `should<결과>_when<조건>` + 한국어 `@DisplayName` (06 코드가이드라인 §7.2).
- read timeout 테스트에 `MockRestServiceServer.withException(new SocketTimeoutException())` 를 쓰지 않는다. 실제 POST timeout(`RestClientException`)과 다른 예외(`ResourceAccessException`)가 나와 거짓 통과한다. 지연 응답 서버를 쓴다 (conventions-proposal §6.2).
