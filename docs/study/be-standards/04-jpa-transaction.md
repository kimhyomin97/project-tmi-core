[← be-standards](./README.md)

# 04. JPA·데이터 접근·트랜잭션

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조. 이 주제는 근거 대부분이 **공식 문서 + 교육 계보(김영한 강의, 우테코)**다.

## 1. 한 줄 요약

**트랜잭션은 짧게, 외부 호출은 트랜잭션 밖에서.** 엔티티는 setter 없이 도메인 메서드로 바꾸고, 연관관계는 LAZY + 필요한 곳만 fetch join으로 가져온다. 스키마는 코드가 아니라 **버전 관리된 마이그레이션**으로 바꾼다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 | 링크 |
|---|---|---|---|
| 트랜잭션 안에서 외부 HTTP를 호출하지 않는다. 커넥션과 락을 쥔 채 기다리면 풀이 고갈된다 | C + D | 카카오페이 2023, LEAPHOP 발췌 + 커넥션 점유 원리 | 7절 미확인 표 |
| 잔액·재고는 DB 비관적 락(`SELECT ... FOR UPDATE`) + 타임아웃. 다중 서버 중복 요청은 Redis 분산락 | C | 토스뱅크, 카카오페이, 컬리, SSG 발췌 | 7절 미확인 표 |
| `spring.jpa.open-in-view=false`를 명시하고 서비스에서 DTO로 변환한다 | A + B | Spring 기본값 true + 경고 로그(소스 확인), 우테코 2026 팀 | [JpaBaseConfiguration](https://github.com/spring-projects/spring-boot/blob/main/module/spring-boot-jpa/src/main/java/org/springframework/boot/jpa/autoconfigure/JpaBaseConfiguration.java) |
| 엔티티: `@Getter` + `@NoArgsConstructor(access = PROTECTED)`, `@Setter` 금지, 정적 팩토리와 도메인 메서드, LAZY 명시, `BaseTimeEntity` + `JpaAuditingConfig` | B | 우테코 2026-Devica, 이동욱 예제 계보 | [2026-Devica](https://github.com/woowacourse-teams/2026-Devica) |
| N+1: ToOne은 fetch join/`@EntityGraph`, 컬렉션은 `default_batch_fetch_size`, 목록 API는 DTO 프로젝션 | C + B | 우아콘2020 발표(개인 정리 노트로 확인), 김영한 강의 계보 | [우아콘2020 노트](https://github.com/ces518/TIL/blob/master/conference/%EC%9A%B0%EC%95%84%EC%BD%982020/%EC%88%98%EC%8B%AD%EC%96%B5%EA%B1%B4%EC%97%90%EC%84%9C_QUERYDSL_%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0.md) |
| `ddl-auto=validate` + Flyway 마이그레이션. 적용된 파일은 수정하지 않는다(CI 검사) | B | 우테코 2026 팀 2곳 | [2026-poudy](https://github.com/woowacourse-teams/2026-poudy) |
| 커넥션 풀: `minimumIdle` 미설정(고정 크기), `maxLifetime`은 DB 제한보다 몇 초 짧게 | A | HikariCP 공식 | [HikariCP README](https://github.com/brettwooldridge/HikariCP/blob/dev/README.md) |

MySQL과 PostgreSQL의 차이(격리 수준, 락, EXPLAIN)는 [07 보안·DB](./07-security-and-db.md)에서 다룬다.

Spring·Hibernate 공식으로 확인한 함정 [A]:

| 사실 | 출처 |
|---|---|
| 같은 객체 안의 호출(self-invocation)에는 `@Transactional`이 적용되지 않는다. 6.0부터 class 프록시는 protected·package-private도 적용된다 | Spring `@Transactional` 문서 |
| 기본 롤백 대상은 RuntimeException과 Error뿐이다. checked exception은 `rollbackFor`가 필요하다 | Spring Rolling Back 문서 |
| `REQUIRES_NEW`는 커넥션을 하나 더 쓴다. 풀이 동시 스레드 수보다 1 이상 커야 데드락이 없다 | Spring Propagation 문서 |
| `readOnly=true`면 Hibernate flush 모드가 MANUAL이 되어 dirty check를 건너뛴다 | Spring Data JPA Transactionality |
| IDENTITY 생성기를 쓰면 Hibernate가 JDBC insert batching을 끈다 | Hibernate Batching 문서 |
| 데드락 회피 최소 풀 크기 = `Tn x (Cm - 1) + 1` | HikariCP wiki |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 동적 쿼리 도구 | QueryDSL 원본 5.1.0(Boot 4 BOM이 관리, 정체) / OpenFeign 포크 7.x(활발, 버전 직접 관리) / jOOQ(SK DevOcean, 미확인) / Kotlin JDSL(LINE) / `@Query` + JdbcTemplate | 동적 검색 조건이 실제로 생겼을 때 QueryDSL. 원본과 포크 모두 정당하다 |
| 조회에 `readOnly=true`를 항상 붙일까 | 붙임: 김영한 강의, 우테코 2026-poudy / 핫 테이블 단건 조회는 트랜잭션 없이: 카카오페이(MySQL 고 TPS, 미확인) | 학습 프로젝트는 붙인다. 고트래픽에서는 쿼리 로그로 추가 왕복을 측정한 뒤 판단 |
| 동시성 제어 | `@Version` 낙관적 락(교재) / 비관적 락(토스뱅크) / Redis 분산락(컬리 등) / MySQL 네임드 락(개인 사례) | 충돌이 드물면 낙관적, 잦고 정합성이 중요하면 비관적, 서버가 여러 대면 분산락 |
| 스키마 관리 | Flyway(우테코 팀) / Liquibase(국내 사례 미확인) / DBA 수동 DDL + validate | 학습 프로젝트는 Flyway. 단 이 저장소는 팀 결정으로 미도입이다 (4.3) |
| 삭제 정책 | `deleted_at` + `@SQLDelete`/`@SQLRestriction` / Hibernate `@SoftDelete`(6.4+, TIMESTAMP는 7.0+) / 물리 삭제 + 이력 테이블 | 복구·감사 요구가 있으면 소프트 삭제. 방식 하나로 통일한다 |

`@SQLRestriction`은 항상 적용되고 끌 수 없다(공식 javadoc). 관리자 조회처럼 삭제된 행도 봐야 하는 곳이 있으면 쓰기 어렵다.

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| `@Getter` + `@NoArgsConstructor(PROTECTED)` + 정적 `create()` | `sentence/domain/SentenceEntity` | 2절 엔티티 관례와 같다 |
| `@Enumerated(EnumType.STRING)` | `SentenceEntity.level`, `category` | 순서 변경에 안전하다 |
| `BaseEntity`(`@MappedSuperclass`, `@CreatedDate`, `@LastModifiedDate`) + `common/config/JpaAuditingConfig` | `common/domain/BaseEntity` | Devica와 같은 구성 |
| `deleted_at` 소프트 삭제 + 이름에 조건 표시(`findByIdAndDeletedAtIsNull`) | `sentence/repository/SentenceRepository` | `@SQLRestriction` 대신 명시 조건이라 관리자 조회에 유연하다 |
| `spring.jpa.hibernate.ddl-auto=validate` | `application.properties` | 운영에서 스키마 자동 변경을 막는다 |
| native 쿼리의 SpEL `:#{#level.name()}` | `SentenceRepository.findRandomBy` | native 쿼리에서는 필요하다 (current-state F10) |

### 4.2 바꿀 것

| 현재 | 문제 | 바꿀 방향 |
|---|---|---|
| `AnswerService.loadSentence`가 `@Transactional(readOnly = true) protected`이고 같은 클래스의 `score()`에서 호출 | self-invocation이라 무효. 파생 쿼리라 리포지토리 기본 트랜잭션도 없다 (F4) | 조회를 다른 빈(`SentenceReader` 또는 `SentenceService`)의 public 메서드로 옮긴다 |
| OSIV 기본값(true) | Similarity 호출 동안 DB 커넥션 1개를 점유한다 | 서비스가 DTO 반환(R5) → `open-in-view=false` (제안 D4 (a)) |
| `spring.jpa.show-sql=true` | stdout 직접 출력, 운영에도 켜진다 | `local` 프로필에서 `logging.level.org.hibernate.SQL=DEBUG` (제안 §5.3) |
| `default_batch_fetch_size` 미설정 | 연관관계가 생기면 N+1 | 연관관계를 처음 만들 때 `spring.jpa.properties.hibernate.default_batch_fetch_size=100` |
| HikariCP 기본값 그대로 | `statement_timeout` 없음 | `prod`에서 `connection-init-sql`로 `SET statement_timeout`을 검토 (poudy 구성) |

### 4.3 기존 결정·제안과 다른 점

| 항목 | 기존 | 이번 조사 | 판단 |
|---|---|---|---|
| 트랜잭션 경계 | 제안 §4.1~4.3: 다른 빈의 public 메서드, 외부 호출은 트랜잭션 밖 | 공식 문서[A]와 같다. 카카오페이 사례는 [C] | **일치** |
| OSIV | 제안 D4 (a) | 같다 | **일치** |
| 마이그레이션 | `docs/public/05_개발컨벤션.md` §3.7: **Flyway 미도입**, 04 문서·`init.sql`·엔티티 3곳 동기화 | 우테코 팀들은 Flyway + validate + CI 가드 | **다르다**. 팀 결정을 존중하되, 학습 목표로 "Flyway를 쓰면 무엇이 달라지는가"를 따로 실습해 볼 가치가 있다. 바꾸려면 사람 결정이 필요하다 |
| ID 전략 | 05 §3.2: `varchar(36)` UUID를 서비스에서 생성 (`UUID.randomUUID()`) | 조사 권고는 "AUTO 금지, IDENTITY/SEQUENCE 명시" | 이 저장소는 직접 할당 ID라 AUTO 문제와 무관하다. 다만 ID를 미리 채운 엔티티를 `save()`하면 Spring Data가 "새 엔티티 아님"으로 보고 `merge`(추가 SELECT)할 수 있다 (estimated, 일반 지식). 저장 기능을 만들 때 쿼리 로그로 확인한다 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 영속성 컨텍스트 | 1차 캐시, 동일성 보장, 쓰기 지연, dirty checking. setter 없이 도메인 메서드만으로 UPDATE가 나가는 이유 |
| N+1 | 목록 1번 + 연관 N번. fetch join, `@EntityGraph`, batch size, DTO 프로젝션으로 푼다 |
| 컬렉션 fetch join + 페이징 | 메모리에서 페이징한다는 경고가 나온다. 컬렉션은 batch size로 푼다 |
| `@OneToOne` LAZY | 연관관계 주인이 아닌 쪽은 바이트코드 향상 없이는 LAZY가 동작하지 않는다 |
| OSIV | 켜면 뷰 렌더링까지 커넥션을 잡는다. 끄면 트랜잭션 밖 지연 로딩이 `LazyInitializationException` |
| `@Transactional` 프록시 | self-invocation 무효, checked 예외는 기본 롤백 안 함, `REQUIRES_NEW`는 커넥션 2개 |
| 격리 수준 | MySQL 기본 REPEATABLE READ, PostgreSQL 기본 READ COMMITTED. 자세한 차이는 07 문서 |
| 낙관적 vs 비관적 락 | `@Version` 충돌 시 예외·재시도 vs `FOR UPDATE`로 대기. 충돌 빈도로 고른다 |
| 분산락과 트랜잭션 순서 | 락은 트랜잭션 바깥에서 잡고 커밋 뒤 푼다. 반대로 하면 커밋 전 다른 요청이 들어온다 |
| ID 전략 | IDENTITY는 insert 배치 불가, SEQUENCE는 `allocationSize`로 배치 가능 |
| 마이그레이션 | 체크섬 불변 원칙, baseline, expand-contract(컬럼 추가 → 이전 → 삭제) |

## 6. 기억할 것

1. 외부 호출 동안 커넥션을 쥐지 않는다 (트랜잭션 밖 + OSIV off).
2. `@Transactional`은 **다른 빈에서 들어오는 호출**에만 걸린다.
3. 쿼리 수는 눈으로 확인한다. SQL 로그를 보는 습관이 N+1을 막는다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://github.com/spring-projects/spring-boot/blob/main/module/spring-boot-jpa/src/main/java/org/springframework/boot/jpa/autoconfigure/JpaBaseConfiguration.java | Spring | 2026 | 공식 소스 |
| https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/data-access/transaction/declarative/annotations.adoc | Spring | 2026 | 공식 |
| https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/data-access/transaction/declarative/tx-propagation.adoc | Spring | 2026 | 공식 |
| https://github.com/spring-projects/spring-framework/blob/main/framework-docs/modules/ROOT/pages/data-access/transaction/declarative/rolling-back.adoc | Spring | 2026 | 공식 |
| https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/transactions.adoc | Spring | 2026 | 공식 |
| https://github.com/spring-projects/spring-data-commons/blob/main/src/main/antora/modules/ROOT/pages/repositories/core-extensions-querydsl.adoc | Spring | 2026 | 공식 |
| https://github.com/spring-projects/spring-boot/blob/v4.0.2/platform/spring-boot-dependencies/build.gradle | Spring | 2026 | 공식 소스 |
| https://github.com/hibernate/hibernate-orm/blob/main/documentation/src/main/asciidoc/userguide/chapters/batch/Batching.adoc | Hibernate | 2026 | 공식 |
| https://github.com/hibernate/hibernate-orm/blob/main/hibernate-core/src/main/java/org/hibernate/annotations/SoftDelete.java | Hibernate | 2026 | 공식 소스 |
| https://github.com/brettwooldridge/HikariCP/blob/dev/README.md, .../wiki/About-Pool-Sizing | HikariCP | 현행 | 공식 |
| https://github.com/OpenFeign/querydsl, https://github.com/line/kotlin-jdsl | OpenFeign, LINE | 2026 | 오픈소스 |
| https://github.com/woowacourse-teams/2026-Devica, https://github.com/woowacourse-teams/2026-poudy | 우테코 팀 저장소 | 2026 | 교육 과정 |
| https://github.com/jojoldu/freelec-springboot2-webservice | 이동욱 | 2019 | 개인 저장소 |
| https://github.com/ces518/TIL/ (우아콘2020 QueryDSL 정리) | 개인 노트 | 2020 | 개인 (발표 2차 정리) |

### 미확인 (직접 읽어 볼 후보, 상위 5개)

<details>
<summary>펼치기</summary>

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://tech.kakaopay.com/post/improve-service-performance/ | 카카오페이 (트랜잭션 밖 외부 호출) | 2023 | 기업 블로그 (미확인) |
| https://toss.tech/article/slash23-corebanking | 토스 SLASH23 (비관적 락) | 2023 | 기업 발표 (미확인) |
| https://helloworld.kurly.com/blog/distributed-redisson-lock/ | 컬리 (Redisson 분산락) | 2023 (추정) | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/2663/ | 우아한형제들 (HikariCP 데드락) | 2020 | 기업 블로그 (미확인) |
| https://www.youtube.com/watch?v=zMAX7g6rO_Y | 우아콘2020 QueryDSL | 2020 | 기업 발표 (미확인) |

그 밖에 검색으로만 본 글: 카카오페이 `@Transactional` 2건, 우아한 배치 insert, 쏘카 2023, SK DevOcean jOOQ, LEAPHOP, 개인 블로그 1건.

</details>
