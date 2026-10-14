---
paths:
  - "src/main/java/**/repository/**"
  - "src/main/java/**/domain/**"
  - "src/main/java/**/service/**"
  - "src/main/resources/application*.properties"
  - "docs/public/init.sql"
---

# 영속성·트랜잭션 규칙

- **`@Transactional` 메서드를 같은 클래스 안에서 호출하지 않는다.** 프록시를 거치지 않아 무효다. 트랜잭션이 필요한 조회·저장은 다른 빈으로 뺀다 (ArchUnit R6). 현재 `AnswerService.loadSentence` 가 이 상태 (`FIX-F4`).
- **리포지토리에 선언한 쿼리 메서드(파생·`@Query`)에는 기본 트랜잭션이 없다.** `JpaRepository` 기본 메서드(`findById` 등)만 readOnly 트랜잭션이 있다. 경계는 서비스가 `@Transactional(readOnly = true)` 로 정한다.
- **외부 HTTP 호출은 트랜잭션 밖에서.** `spring.jpa.open-in-view` 기본값(true) 때문에 트랜잭션 밖 조회 후에도 커넥션이 요청 끝까지 잡힌다. `[제안]` 서비스가 DTO 를 반환한 뒤 OSIV off (D4).
- `[제안]` 서비스 public 메서드는 `@Entity` 를 반환하지 않는다 — 컨트롤러엔 응답 DTO (R5).
- native `@Query` 에 enum 을 넘길 때는 SpEL `:#{#level.name()}` 가 필요하다. Hibernate 7 은 enum 을 ORDINAL 로 바인딩한다. 06 가이드 §2.3 의 "자동 바인딩" 문구는 틀렸다.
- 스키마를 바꾸면 3곳을 함께 고친다: `docs/public/04_데이터모델.md`, `docs/public/init.sql`, `*Entity.java`. Flyway 는 쓰지 않는다 (05 개발컨벤션 §3.7, 팀 결정). `ddl-auto=validate` 라 셋이 어긋나면 기동이 실패한다.
- `docs/public` 수정은 구현 전에 사람에게 계획을 확인한다.
