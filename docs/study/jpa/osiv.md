# OSIV (Open Session In View)

## 한 줄 요약

OSIV 는 **HTTP 요청이 끝날 때까지 JPA 영속성 컨텍스트(EntityManager)를 열어 두는** 기능이다. 편리하지만, 요청 중에 느린 작업(외부 API 호출 등)이 있으면 그동안 **DB 커넥션을 계속 붙잡는다**. Spring Boot 는 기본값으로 OSIV 를 켜 둔다(`spring.jpa.open-in-view=true`).

## 이 프로젝트에서 일어난 일

채점 API(`POST /answers`)의 흐름:

1. 문장을 DB 에서 조회한다.
2. 외부 Similarity 서버를 호출한다 (connect 1초 + read 2초까지 걸릴 수 있음).
3. 결과를 응답한다.

사내 가이드는 "외부 호출은 트랜잭션 밖에서" 라고 하는데, 이 규칙의 목적은 외부 호출 동안 커넥션을 잡지 않는 것이다. 그런데 OSIV 때문에 그 목적이 달성되지 않았다. 실제로 측정한 결과는 다음과 같다 (current-state §3.4).

| 설정 | 외부 호출 중 사용 중인 DB 커넥션 |
|---|---|
| 기본 (OSIV 켜짐) | **1개** |
| `spring.jpa.open-in-view=false` | 0개 |

커넥션 풀이 10개라면, 외부 서버가 느려질 때 채점 요청 10개만으로 풀이 바닥나고, **채점과 상관없는 다른 API 까지 멈출 수 있다** (estimated).

## 동작 원리

```
요청 시작 ──────────────────────────────────────────────▶ 요청 끝
   │ OSIV: EntityManager 열림                               │ 닫힘
   │                                                        │
   ├─ DB 조회 ─┤ (커넥션 획득)                                │
   │           ├──── 외부 API 호출 (최대 3초) ────┤             │
   │           │◀──────── 커넥션 계속 점유 ─────────────────▶│ 반환
```

- OSIV 가 꺼져 있으면, 트랜잭션(또는 조회)이 끝날 때 커넥션이 반환된다.
- 켜져 있으면, 요청이 끝날 때 반환된다.

## 왜 기본값이 켜져 있나

컨트롤러나 뷰에서 지연 로딩(`LAZY`) 연관 객체에 접근해도 `LazyInitializationException` 이 나지 않게 하려고 켜 둔 것이다. OSIV 를 끄면 이 편의가 사라지므로, **서비스가 엔티티 대신 DTO 를 반환**해야 한다. 그래서 이 프로젝트의 계획은 `FIX-R5`(서비스가 DTO 반환)를 먼저 하고 그다음 `FIX-F4`(OSIV off) 순서다.

## 코드로 보기

```properties
# application.properties
spring.jpa.open-in-view=false
```

```java
// ✗ OSIV off 이후 위험 — 트랜잭션 밖에서 엔티티의 LAZY 필드 접근
SentenceEntity e = sentenceService.getRandom();   // 엔티티 반환
return SentenceResponse.from(e);                  // 여기서 LAZY 접근 시 예외

// ✓ 서비스 안(트랜잭션 안)에서 DTO 로 변환해 반환
SentenceResponse r = sentenceService.getRandom(); // DTO 반환
```

## 기억할 것

1. Spring Boot 에서 OSIV 는 **기본으로 켜져 있다**. 기동 로그의 `spring.jpa.open-in-view is enabled by default` 경고가 이것이다.
2. 요청 중에 외부 호출처럼 느린 작업이 있으면, OSIV 때문에 커넥션 점유 시간이 길어진다.
3. OSIV 를 끄려면 먼저 **서비스가 DTO 를 반환**하도록 바꾼다.

## 더 읽을거리

- Spring Boot — JPA 설정 속성 (`spring.jpa.open-in-view`): https://docs.spring.io/spring-boot/appendix/application-properties/index.html
- 이 저장소의 측정 근거: `docs/ai/current-state.md` §3.4, 결정 항목: `docs/ai/decisions-needed.md` D4
