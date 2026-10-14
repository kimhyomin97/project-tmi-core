# MessageSource 와 로케일 폴백

## 한 줄 요약

Spring 은 에러 메시지를 `messages*.properties` 에서 **요청 로케일**로 찾는다. 그 언어 파일이 없으면 기본값으로 **서버(JVM) 기본 로케일** 파일로 폴백하므로, 서버가 ko 면 영어 요청에도 한글이 나올 수 있다.

## 이 프로젝트에서 일어난 일

- 메시지 파일은 `src/main/resources/messages/messages.properties`(영문), `messages_ko.properties`(한글)인데 `spring.messages.basename` 이 없어 번들이 아예 로드되지 않는다. 그래서 `message` 에 코드 문자열(`TMI.SENTENCE.NOT_FOUND`)이 나간다 (F2, `docs/ai/context/current-state.md` §3.3(b)).
- basename 만 고치면 반쯤 고쳐진다. `messages_en.properties` 가 없어서, `Accept-Language: en` 요청이 서버 기본 로케일(ko)로 폴백해 한글이 나온다.
- `TEST-1` 의 `SentenceControllerTest` 는 처음에 이 "반쯤 고친 상태" 를 못 잡았다. 테스트 환경의 기본 로케일이 en 이라 폴백해도 영문이 나왔기 때문이다. 리뷰에서 발견해 테스트가 기본 로케일을 ko 로 고정하게 바꿨다.

## 동작 원리

```
요청 Accept-Language: en
   │
   ▼
messages_en.properties 있나? ── 있음 ──▶ 영문
   │ 없음
   ▼
fallback-to-system-locale = true(기본) ──▶ messages_<JVM 기본 로케일>.properties
   │                                         (서버가 ko 면 → 한글)
   │ false
   ▼
messages.properties (영문 기본 파일)
```

## 코드로 보기

```properties
# ✗ 반쯤 고침 — 서버 로케일이 ko 면 en 요청에도 한글
spring.messages.basename=messages/messages

# ✓ 요청 언어 파일이 없으면 기본 파일(영문)로
spring.messages.basename=messages/messages
spring.messages.fallback-to-system-locale=false
```

## 기억할 것

1. 메시지 파일이 `messages/` 폴더 안에 있으면 basename 을 `messages/messages` 로 지정해야 한다.
2. 언어별 파일이 없는 언어의 요청은 기본적으로 **서버 로케일**로 폴백한다. 서버마다 결과가 달라질 수 있다.
3. 로케일에 따라 달라지는 동작을 테스트할 때는 테스트가 기본 로케일을 **고정**해야 한다. 그렇지 않으면 실행 환경에 따라 결과가 바뀐다.

## 더 읽을거리

- Spring Boot — Internationalization (`spring.messages.*`): https://docs.spring.io/spring-boot/reference/features/internationalization.html
