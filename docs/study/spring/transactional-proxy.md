# @Transactional 과 프록시

## 한 줄 요약

`@Transactional` 은 메서드에 붙은 표시일 뿐이고, 실제로 트랜잭션을 여는 것은 **Spring 이 빈 앞에 세운 프록시**다. 그래서 프록시를 거치지 않는 호출(같은 클래스 안에서 `this.method()`)에는 트랜잭션이 걸리지 않는다.

## 이 프로젝트에서 일어난 일

`AnswerService` (current-state F4):

```java
public AnswerResponse score(SubmitAnswerRequest request) {
    SentenceEntity sentence = loadSentence(request.sentenceId());   // ← this.loadSentence()
    ...
}

@Transactional(readOnly = true)
protected SentenceEntity loadSentence(String sentenceId) { ... }
```

- 어노테이션이 있어도 `loadSentence` 에는 트랜잭션이 없다.
- 사내 가이드 06 §3.3 의 "권장" 예시가 바로 이 패턴이다. 가이드를 따랐는데 결함이 생긴 경우다.
- DEBUG 로그로 확인했다: 이 조회에서 `Creating new transaction` 로그가 나오지 않는다.

## 동작 원리

```
Controller ──▶ [프록시] ──▶ AnswerService.score()
                 │                 │
                 │ 여기서 트랜잭션   │ this.loadSentence()  ← 프록시를 안 거침
                 │ 시작/종료         ▼
                 │           AnswerService.loadSentence()   (트랜잭션 없음)
```

- 다른 빈이 `answerService.xxx()` 를 부르면, 실제로는 프록시를 부르는 것이다. 프록시가 트랜잭션을 연 다음 진짜 객체를 호출한다.
- 진짜 객체 안에서 `this` 는 프록시가 아니라 자기 자신이다. 그래서 가로챌 기회가 없다.

## 코드로 보기

```java
// ✗ 같은 클래스 안에서 호출 — 트랜잭션 무시
class AnswerService {
    public AnswerResponse score(...) { loadSentence(id); }
    @Transactional(readOnly = true) SentenceEntity loadSentence(String id) { ... }
}

// ✓ 다른 빈으로 분리 — 프록시를 거침
class SentenceService {
    @Transactional(readOnly = true)
    public SentenceDetail getDetail(String id) { ... }
}
class AnswerService {
    private final SentenceService sentenceService;
    public AnswerResponse score(...) { sentenceService.getDetail(id); }
}
```

## 함께 알아 둘 함정 — 리포지토리 쿼리 메서드

`JpaRepository` 의 기본 메서드(`findById`, `save` 등)는 이미 트랜잭션이 걸려 있다. 하지만 **직접 선언한 쿼리 메서드**(`findByIdAndDeletedAtIsNull`, `@Query`)에는 기본 트랜잭션이 **없다**.

| 호출 | 트랜잭션 |
|---|---|
| `repository.findById(...)` | 있음 (readOnly) |
| `repository.findByIdAndDeletedAtIsNull(...)` | **없음** |
| `repository.findRandomBy(...)` (`@Query`) | **없음** |

그래서 트랜잭션 경계는 리포지토리가 아니라 **서비스**가 정한다.

## 기억할 것

1. `@Transactional` 은 **다른 빈에서 들어오는 호출**에만 동작한다.
2. 트랜잭션이 필요한 로직을 같은 클래스의 다른 메서드로 빼지 말고, 다른 빈으로 뺀다.
3. 리포지토리에 쿼리 메서드를 추가했다면, 트랜잭션은 서비스에서 건다.

## 더 읽을거리

- Spring Framework — `@Transactional` 사용 (프록시 모드 설명): https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- Spring Data JPA — 트랜잭션 (쿼리 메서드 기본값): https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html
- 이 저장소의 실행 근거: `docs/ai/current-state.md` §3.4
