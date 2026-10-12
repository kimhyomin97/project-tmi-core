[← be-standards](./README.md)

# 04. JPA·데이터 접근·트랜잭션

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

**트랜잭션은 짧게, 외부 호출은 트랜잭션 밖에서.** 엔티티는 setter 없이 도메인 메서드로 바꾸고, 연관관계는 LAZY + 필요한 곳만 fetch join으로 가져온다. 동시성은 충돌 빈도로 락 방식을 고르고, 스키마는 **버전 관리된 마이그레이션**으로 바꾼다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 |
|---|---|---|
| 트랜잭션 안에서 외부 HTTP를 호출하지 않는다. 외부 호출과 DB 트랜잭션을 별도 빈으로 나눈다 | A + 카카오페이 사례 C✓ | 카카오페이(2023·2024) |
| `spring.jpa.open-in-view=false` + 서비스에서 DTO로 변환 | A + B + 카카오페이 사례 C✓ | Spring 소스, 우테코 2026 팀, 카카오페이(2023) |
| OSIV가 켜져 있으면 커넥션이 요청 끝까지 잡힌다 | A + 국내 공통 C✓ | 카카오페이(2023), 컬리(2024) |
| 다중 서버 동시 요청은 Redis 분산락 | 국내 공통 C✓ | 토스(2023), 컬리(2023), LINE(2024), 카카오페이(2024) |
| 분산락은 트랜잭션 바깥에서 잡고 **커밋 뒤** 푼다 | 컬리 사례 C✓ | 컬리(2023) |
| 잔액처럼 채널이 많은 갱신은 분산락 + DB 행 락 이중화 | 토스 사례 C✓ | 토스뱅크(2023) |
| 엔티티: `@Getter` + `@NoArgsConstructor(PROTECTED)`, `@Setter` 금지, 정적 팩토리, LAZY 명시 | B | 우테코 2026-Devica, 이동욱 예제 계보 |
| N+1: 연관관계를 최소화하고 읽을 때 fetch join, 목록은 DTO 프로젝션 | B + 국내 공통 C✓ | LINE(2024), 카카오페이(2023) |
| 컬렉션은 `default_batch_fetch_size` | B | 김영한 강의 계보. 국내 기업 원문은 못 찾음 |
| 스키마는 Flyway + `ddl-auto=validate` | B + LINE 사례 C✓ | 우테코 2026 팀 2곳, LINE(2023) |
| IDENTITY 전략이면 `saveAll`이 JDBC 배치가 안 된다 | A + 국내 공통 C✓ | Hibernate 문서, 배민(2020), 카카오페이(2026) |
| 커넥션 풀 크기는 "한 작업이 동시에 쥐는 커넥션 수"까지 보고 정한다 | A + 배민 사례 C✓ | HikariCP wiki, 배민(2020) |

MySQL과 PostgreSQL의 차이(격리 수준, 락, EXPLAIN)는 [07 보안·DB](./07-security-and-db.md)에서 다룬다.

Spring·Hibernate 공식으로 확인한 함정 [A]:

| 사실 | 출처 |
|---|---|
| 같은 객체 안의 호출(self-invocation)에는 `@Transactional`이 적용되지 않는다 | Spring `@Transactional` 문서 |
| 기본 롤백 대상은 RuntimeException과 Error뿐이다 | Spring Rolling Back 문서 |
| `REQUIRES_NEW`는 커넥션을 하나 더 쓴다. 풀이 동시 스레드 수보다 1 이상 커야 데드락이 없다 | Spring Propagation 문서 |
| `readOnly=true`면 Hibernate flush 모드가 MANUAL이 되어 dirty check를 건너뛴다 | Spring Data JPA Transactionality |
| IDENTITY 생성기를 쓰면 Hibernate가 JDBC insert batching을 끈다 | Hibernate Batching 문서 |
| 데드락 회피 최소 풀 크기 = `Tn x (Cm - 1) + 1` | HikariCP wiki |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 동적 쿼리 도구 | QueryDSL 원본 5.1.0(Boot 4 BOM 관리, 정체. 카카오페이 2026 사용) / OpenFeign 포크 7.x / Kotlin JDSL(LINE 2022) / `@Query` + JdbcTemplate / MyBatis 병행(네이버 2026, 07 §2.1) | 동적 검색 조건이 실제로 생겼을 때 QueryDSL. 국내 레거시에는 MyBatis가 남아 있으니 읽을 줄은 알아 둔다 |
| 조회 트랜잭션을 어떻게 걸까 | (가) 조회 서비스에 `readOnly=true`: 우테코 팀, 컬리(2024) / (나) 단건 조회는 트랜잭션 없이, `readOnly`는 `SUPPORTS` 커스텀 어노테이션: 카카오페이(2025) | 둘 다 **MySQL**에서 트랜잭션 시작·종료 왕복을 측정한 결과다(카카오페이: set_option이 QPS의 절반 이상, 컬리: `auto-commit=false`로 응답 40% 단축). 학습 프로젝트는 (가) |
| Read/Write 분리 | `AbstractRoutingDataSource`가 `isCurrentTransactionReadOnly()`로 slave/master를 고르고, `LazyConnectionDataSourceProxy`로 감싸 실제 쿼리 시점에 커넥션을 얻는다: 배민(2020) [C✓]. 같은 라우팅 구조를 샤딩에 씀: 카카오페이(2024) [C✓] | **배민 사례**. Lazy 프록시가 없으면 트랜잭션 시작 시점에 이미 커넥션이 정해져 readOnly 라우팅이 안 된다. 복제 지연(replica lag) 때문에 "쓰고 바로 읽기"는 master로 보낸다 [D] |
| 동시성 제어 | `@Version` 낙관적 락(뱅크샐러드 2026) / DB 행 락(토스뱅크 2023) / Redis 분산락(토스·컬리·LINE·카카오페이) / MySQL `GET_LOCK`(배민 2019, 07 §3.3) | 충돌이 드물고 데이터가 사용자별이면 낙관적, 정합성이 결정적이면 분산락 + 행 락. 재진입 여부를 확인한다(LINE 2024) |
| ID 생성과 커넥션 수 | MySQL에서 `GenerationType.AUTO`는 `hibernate_sequence` 테이블을 별도 트랜잭션으로 조회해 **저장 1건에 커넥션 2개**: 배민(2020) 장애 [C✓] | AUTO를 쓰지 말고 전략을 명시한다. 풀 크기를 공식 + 여유분으로 잡고 부하 테스트로 확인한다 |
| 스키마 관리 | Flyway(LINE 2023, 우테코 팀) / DBA 수동 DDL + validate | 학습 프로젝트는 Flyway. 단 이 저장소는 팀 결정으로 미도입이다 (4.3) |
| 삭제·변경 정책 | `deleted_at` + `@SQLDelete`/`@SQLRestriction` / Hibernate `@SoftDelete` / INSERT-only 이력(토스페이먼츠 2025 원장) | 복구·감사가 필요하면 소프트 삭제, 금전 원장은 INSERT-only. 방식 하나로 통일한다 |

`@SQLRestriction`은 항상 적용되고 끌 수 없다(공식 javadoc). 삭제된 행도 봐야 하는 곳이 있으면 쓰기 어렵다.

### 3.1 배치 (Spring Batch)

- 대량 조회 리더는 offset 대신 **마지막 키 조건(no-offset)**으로 읽는다. 배민 정산(2020)은 Querydsl NoOffset ItemReader를 만들었고, 컬리(2024)는 `MySqlPagingQueryProvider`가 이미 이 방식임을 확인했다. **국내 공통** [C✓]. 03 §2.1과 같은 이야기다.
- 트랜잭션은 **청크 단위**로 걸린다. 청크 하나가 실패하면 그 청크만 롤백된다(배민 2020 [C✓], Spring Batch 동작 [A]).
- 대량 insert는 IDENTITY를 피하거나 JDBC 배치(`rewriteBatchedStatements=true`, MySQL)를 켠다(배민 2020 [C✓]).
- 재시작(JobRepository 메타데이터)과 멱등한 writer 설계는 국내 원문을 찾지 못했다 [D].

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| `@Getter` + `@NoArgsConstructor(PROTECTED)` + 정적 `create()` | `SentenceEntity` | 2절 엔티티 관례와 같다 |
| `@Enumerated(EnumType.STRING)` | `SentenceEntity.level`, `category` | 순서 변경에 안전하다 |
| `BaseEntity` + `JpaAuditingConfig` | `common/domain/BaseEntity` | Devica와 같은 구성 |
| `deleted_at` 소프트 삭제 + 이름에 조건 표시 | `SentenceRepository` | 명시 조건이라 관리자 조회에 유연하다 |
| `ddl-auto=validate` | `application.properties` | 운영에서 스키마 자동 변경을 막는다 |
| native 쿼리의 SpEL `:#{#level.name()}` | `SentenceRepository.findRandomBy` | native 쿼리에서는 필요하다 (F10) |

### 4.2 바꿀 것

| 현재 | 문제 | 바꿀 방향 |
|---|---|---|
| `AnswerService.loadSentence`가 `@Transactional(readOnly = true) protected`이고 같은 클래스에서 호출 | self-invocation이라 무효 (F4) | 조회를 다른 빈의 public 메서드로 옮긴다. 카카오페이(2024)의 "트랜잭션 서비스 / 비트랜잭션 서비스" 분리와 같은 모양 |
| OSIV 기본값(true) | Similarity 호출 동안 커넥션 1개를 점유한다. 컬리(2024) 장애와 같은 구조 | 서비스가 DTO 반환(R5) → `open-in-view=false` (제안 D4 (a)) |
| `spring.jpa.show-sql=true` | 운영에도 켜진다 | `local` 프로필에서 `logging.level.org.hibernate.SQL=DEBUG` |
| `default_batch_fetch_size` 미설정 | 연관관계가 생기면 N+1 | 연관관계를 처음 만들 때 `=100`. 그 전에 연관관계가 필요한지 먼저 본다(LINE 2024) |
| HikariCP 기본값 그대로 | `statement_timeout` 없음 | `prod`에서 `connection-init-sql`로 `SET statement_timeout` 검토 |

카카오페이·컬리의 트랜잭션 제거·`auto-commit=false` 튜닝은 MySQL Connector/J의 왕복을 줄이는 이야기다. 이 저장소는 PostgreSQL이라 효과가 같다고 볼 수 없다 [D]. 쿼리 로그로 먼저 확인한다.

### 4.3 기존 결정·제안과 다른 점

| 항목 | 기존 | 이번 조사 | 판단 |
|---|---|---|---|
| 트랜잭션 경계 | 제안 §4.1~4.3 | 공식 문서[A], 카카오페이 원문 2건[C✓] | **일치** |
| OSIV | 제안 D4 (a) | 카카오페이가 같은 순서(DTO → OSIV off)로 진행[C✓] | **일치** |
| 조회 트랜잭션 | 서비스가 `readOnly=true`로 책임 | 카카오페이는 MySQL 고 TPS 때문에 줄였다[C✓] | **일치**(학습 규모에서는 제안 유지) |
| 마이그레이션 | 05 §3.7: **Flyway 미도입** | 우테코 팀[B], LINE[C✓]은 Flyway | **다르다**. 팀 결정을 존중하되 별도 실습. 바꾸려면 사람 결정 |
| ID 전략 | 05 §3.2: `varchar(36)` UUID 직접 할당 | 배민(2020)의 AUTO 장애와 무관하다 | ID를 미리 채운 엔티티를 `save()`하면 `merge`(추가 SELECT)가 될 수 있다 [D]. UUID 컬럼 타입이 엔티티와 같은지 본다(컬리 2020) |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 영속성 컨텍스트 | 1차 캐시, 동일성 보장, 쓰기 지연, dirty checking |
| N+1 | 목록 1번 + 연관 N번. fetch join, `@EntityGraph`, batch size, DTO 프로젝션으로 푼다 |
| 컬렉션 fetch join + 페이징 | 메모리에서 페이징한다는 경고가 나온다. 컬렉션은 batch size로 푼다 |
| OSIV | 켜면 뷰 렌더링까지 커넥션을 잡는다. 끄면 트랜잭션 밖 지연 로딩이 `LazyInitializationException` |
| `@Transactional` 프록시 | self-invocation 무효, checked 예외는 기본 롤백 안 함, `REQUIRES_NEW`는 커넥션 2개 |
| 전파 속성 | `REQUIRED`(기본), `SUPPORTS`, `REQUIRES_NEW`. 카카오페이는 `readOnly`에 `SUPPORTS`를 쓴다 |
| Read/Write 분리 | readOnly 트랜잭션을 레플리카로. `LazyConnectionDataSourceProxy`가 왜 필요한지, 복제 지연을 어떻게 피하는지 |
| 커넥션 풀 데드락 | 한 작업이 커넥션 2개를 쥐면(REQUIRES_NEW, 테이블 ID 생성기) 풀이 고갈될 수 있다. `Tn x (Cm - 1) + 1` |
| 낙관적 vs 비관적 락 | `@Version` 충돌 시 예외·재시도 vs `FOR UPDATE`로 대기 |
| 커밋 이후 부수효과 | 캐시 무효화·메시지 발행은 `@TransactionalEventListener(phase = AFTER_COMMIT)`(토스뱅크 2025) |
| ID 전략 | IDENTITY는 insert 배치 불가, SEQUENCE는 `allocationSize`로 배치 가능 |
| 배치 | 청크 지향 처리(reader/processor/writer), 청크 단위 트랜잭션, no-offset 리더 |

## 6. 기억할 것

1. 외부 호출 동안 커넥션을 쥐지 않는다 (트랜잭션 밖 + OSIV off).
2. `@Transactional`은 **다른 빈에서 들어오는 호출**에만 걸린다.
3. 쿼리 수와 커넥션 수는 눈으로 확인한다. 남의 튜닝은 DB 종류가 같을 때만 참고한다.

## 7. 출처

### 확인한 출처: 국내 기업 (원문을 열어 읽음, 2026-10-12)

| URL | 발행처 | 연도 | 확인한 내용 |
|---|---|---|---|
| https://tech.kakaopay.com/post/improve-service-performance/ | 카카오페이 | 2023 | OSIV 제거, 3rd party 호출이 트랜잭션에 포함, DTO 쿼리 |
| https://tech.kakaopay.com/post/troubleshooting-logs-as-a-junior-developer/ | 카카오페이 | 2024 | 외부 호출과 트랜잭션 분리, Redis 분산락 |
| https://tech.kakaopay.com/post/jpa-transactional-bri/ | 카카오페이 | 2025 | 단건 트랜잭션 제거, `SUPPORTS` 커스텀 어노테이션 |
| https://tech.kakaopay.com/post/querydsl-paging-strategy/ | 카카오페이 | 2026 | Querydsl 5.1.0, IDENTITY에서 `saveAll` 배치 불가 |
| https://tech.kakaopay.com/post/mydata-platfrom-improvement/ | 카카오페이 | 2024 | `AbstractRoutingDataSource` + `LazyConnectionDataSourceProxy`로 샤딩 라우팅 |
| https://toss.tech/article/slash23-corebanking | 토스뱅크 | 2023 | Redis 락 + JPA `@Lock` |
| https://toss.tech/article/payments-legacy-5 | 토스페이먼츠 | 2025 | 원장 INSERT-only |
| https://toss.tech/article/34481 | 토스뱅크 | 2025 | `AFTER_COMMIT` 캐시 무효화 |
| https://helloworld.kurly.com/blog/distributed-redisson-lock/ | 컬리 | 2023 | 분산락 AOP, 커밋 후 해제 |
| https://helloworld.kurly.com/blog/commit-mvcc-set-autocommit/ | 컬리 | 2024 | OSIV + 트랜잭션 밖 조회 → 오래된 스냅샷 |
| https://helloworld.kurly.com/blog/jpa-uuid-sapjil/ | 컬리 | 2020 | UUID `BINARY(255)` 조회 실패 |
| https://techblog.lycorp.co.jp/ko/experience-in-migrating-order-db-on-ecommerce-platform | LINE(LY) | 2024 | 연관관계 최소화 + fetch join, 분산락 재진입 |
| https://techblog.lycorp.co.jp/ko/how-to-migrate-to-spring-boot-3 | LINE(LY) | 2023 | Flyway 권장 |
| https://engineering.linecorp.com/ko/blog/kotlinjdsl-jpa-criteria-api-with-kotlin | LINE | 2022 | Kotlin JDSL |
| https://blog.banksalad.com/tech/banksalad-optimistic-lock/ | 뱅크샐러드 | 2026 | 재시도 없는 낙관적 락 |
| https://woowabros.github.io/experience/2020/07/06/db-sharding.html | 우아한형제들 | 2020 | readOnly → slave 라우팅, `LazyConnectionDataSourceProxy`, round robin |
| https://woowabros.github.io/experience/2020/02/06/hikaricp-avoid-dead-lock-2.html (+ 이론편) | 우아한형제들 | 2020 | AUTO ID → 커넥션 2개 → 풀 데드락, 풀 크기 공식 |
| https://woowabros.github.io/experience/2020/09/23/hibernate-batch.html | 우아한형제들 | 2020 | IDENTITY면 batch insert 불가, `rewriteBatchedStatements` |
| https://woowabros.github.io/experience/2020/02/05/springbatch-querydsl.html | 우아한형제들 | 2020 | NoOffset ItemReader, 청크 단위 트랜잭션 |

공식·교육·오픈소스: Spring Boot `JpaBaseConfiguration`, Spring Framework 트랜잭션 문서, Spring Data JPA·Commons 문서, Boot 4.0.2 의존성 BOM, Hibernate `Batching.adoc`·`SoftDelete.java`, HikariCP README·wiki, OpenFeign/querydsl, line/kotlin-jdsl, 우테코 2026-Devica·2026-poudy, jojoldu/freelec-springboot2-webservice. 모두 GitHub 원문.

<details>
<summary>확인하지 못한 출처 [C?]</summary>

| 대상 | 사유 |
|---|---|
| 우아콘2020 "수십억건에서 QUERYDSL 사용하기" | 영상 제목만 확인 |
| d2.naver.com JPA·트랜잭션 글 | 이번에 검색하지 못함 |
| 국내 기업의 Spring Batch 재시작·멱등 writer 글 | 찾지 못함 |

</details>
