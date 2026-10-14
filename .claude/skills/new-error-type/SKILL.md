---
name: new-error-type
description: 새 도메인 에러(예외 + 에러 코드 + 메시지)를 추가할 때 사용. ETmiErrorType 상수, 기능별 예외 클래스의 정적 중첩 클래스, messages*.properties 두 파일, API 명세 에러 코드 표를 빠짐없이 맞춘다.
---

# 새 에러 타입 추가

에러 응답 형식 `{"error":{"code":"TMI.{DOMAIN}.{REASON}","message":...}}` 은 외부 계약이다 (`docs/public/03_API명세.md` §1.4). 형식은 바꾸지 않는다.

## 절차

1. **코드 이름 정하기**: `TMI.{DOMAIN}.{REASON}` (대문자 + 밑줄). 예: `TMI.SENTENCE.NOT_FOUND`.
2. **`ETmiErrorType` 상수 추가** — `src/main/java/com/tmi/core/common/exception/ETmiErrorType.java` 의 도메인 주석 블록 아래:
   ```java
   // Sentence
   SENTENCE_NOT_FOUND  ("TMI.SENTENCE.NOT_FOUND"),
   ```
3. **예외 클래스** — `<기능>/exception/<기능>Exception.java` (외부 클래스 + 정적 중첩 클래스, 외부 클래스 생성자는 private). 없으면 같은 모양으로 만든다:
   ```java
   public static class NotFound extends BaseException {
       public NotFound(String detail) {
           super(HttpStatus.NOT_FOUND, ETmiErrorType.SENTENCE_NOT_FOUND, detail);
       }
   }
   ```
   HTTP 상태: 클라이언트 잘못 4xx, 외부 시스템 4xx 응답 → 502, 외부 시스템 장애·timeout → 503.
4. **메시지 2곳** — 같은 키로 두 파일 모두:
   - `src/main/resources/messages/messages.properties` (영문)
   - `src/main/resources/messages/messages_ko.properties` (한글)
   `{0}` 은 예외 생성자의 detail 인자다.
5. **API 명세** — `docs/public/03_API명세.md` §1.4 의 에러 코드 표와, 그 에러가 나는 엔드포인트 절의 에러 표에 행을 추가해야 한다. `docs/public` 은 팀 문서라 **수정 전에 사람에게 확인**한다.
6. **테스트** — 이 예외가 나는 경로에 상태 코드와 `error.code` 를 검증하는 테스트를 추가한다 (`@WebMvcTest` 슬라이스, `.claude/rules/testing.md`).
7. **확인**: `bash ./gradlew test --tests '<새 테스트 클래스>'`.

## 주의

- 현재 메시지 번들이 로드되지 않는다 (`spring.messages.basename` 미설정, `FIX-F2`). F2 를 고치기 전에는 응답 `message` 가 코드 문자열로 나온다 — 테스트에서 메시지 본문을 검증하려면 F2 이후에 한다.
- 컨트롤러에서 try/catch 하지 않는다. `GlobalExceptionHandler` 가 `BaseException` 을 처리한다.
