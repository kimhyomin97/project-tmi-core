[← be-standards](./README.md)

# 07. 인증·보안 기본기, 설정·시크릿, DB(MySQL vs PostgreSQL)와 쿼리 튜닝

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

인증은 **Spring Security 필터 체인** 위에서 세션이나 토큰으로 구현하고, 비밀번호는 **적응형 해시(bcrypt 등)**, 시크릿은 **코드 밖(환경변수·Vault·클라우드 시크릿 저장소)**에 둔다. 국내 기업 원문에서 MySQL 운영 사례가 여러 곳 확인되지만 이 저장소는 PostgreSQL이다. **두 DB의 기본 격리 수준과 락 동작이 다르다**는 점을 알고, 쿼리는 `EXPLAIN`으로 확인한다. 여러 서버의 동시성은 국내 기업 다수가 **Redis 분산 락**으로 푼다.

## 2. 공개 근거로 본 관례

### 2.1 국내 기업 블로그 원문 (주제별, [C✓])

**국내 공통**으로 쓸 수 있는 것: ① 여러 서버의 동시성은 Redis 분산 락, 돈·재고 행은 DB 락 병행 ② 운영 DB로 MySQL 사용 ③ 시크릿은 코드 밖 저장소에서 주입. 로그 마스킹은 토스 사례다.

**락**

| 기업 (연도) | 원문에서 확인한 내용 |
|---|---|
| 컬리 (2023) | Redis(Redisson) 선택. MySQL 락은 "별도 커넥션 풀을 관리해야 하고 락 부하를 RDS가 받는다"며 제외. 락 메서드는 `REQUIRES_NEW`, **커밋 후 락 해제** |
| 배민 광고시스템 (2019) | 반대로 **MySQL `GET_LOCK`**을 택함. 이미 쓰는 RDBMS라 인프라 비용이 없고 사용량이 적어서. 락용 커넥션 풀을 로직용과 **분리**하고, 락 메서드에 `@Transactional`을 붙이지 않음 |
| 하이퍼커넥트 (2019) | 직접 만든 스핀 락은 타임아웃이 없고 Redis에 부하 → Redisson(pub/sub) |
| 토스뱅크 (2023) | Redis 락만으로는 부족 → JPA `@Lock`으로 **계좌 잔액 행에만** DB 락 병행, 재시도·타임아웃 |
| LINE Plus (2024) | 주문 DB 이관 중 Redis 분산 락. 재진입 문제로 구현체를 새로 만듦 |
| 뱅크샐러드 (2026) | 세 방식을 비교하고 낙관적 락(버전 컬럼) 선택 |
| 토스 (2024) | 캐시 핫키 만료 때 DB 중복 조회를 막는 데 분산 락 |

**DB**

| 기업 (연도) | 원문에서 확인한 내용 |
|---|---|
| 네이버 스마트스토어 (2026) | Oracle → MySQL 무중단 전환. JPA와 MyBatis 병행. OR 조건 Index Merge 한계로 풀스캔 → 실행 계획을 보고 수정 |
| 카카오페이 (2025) | MySQL 8.0 + Connector/J에서 PreparedStatement 동작 분석 |
| 컬리 (2024) | Aurora MySQL, **REPEATABLE READ**. `auto-commit: false` + OSIV로 스냅샷이 유지돼 방금 넣은 데이터가 안 보인 장애. RC로 낮추면 팬텀 리드 영향이 있어 RR 유지 |
| 배민 (2019·2020) | MySQL `GET_LOCK`, MySQL 환경 Hibernate 배치, master/slave 라우팅 |
| 하이퍼커넥트 (2020) | **PostgreSQL**. 슬로우 쿼리 → `EXPLAIN` Seq Scan → 부분·표현식 인덱스. "로컬·QA와 운영은 데이터 분포가 달라 실행 계획이 다르다" |

MySQL 운영은 5개 기업(네이버·카카오페이·컬리·LINE Plus·배민)에서 확인했다. "PostgreSQL보다 몇 배 많다" 같은 비율 근거는 없다 [D].

**인증·시크릿·마스킹**

| 기업 (연도) | 원문에서 확인한 내용 |
|---|---|
| 카카오페이 (2023) | 제휴사 서버 간 API에 OAuth 2.1 Client Credentials. Spring Authorization Server, 기본 토큰은 Opaque |
| LY (2026) | JWT는 "발행 후 중앙에서 즉시 무효화하기 어렵다" → 유효 기간을 짧게 |
| 하이퍼커넥트 (2023) | Redis 기반 Spring Session |
| 하이퍼커넥트 (2020) | 설정은 저장소에, **적을 수 없는 값만 Vault**에. 주입 방식 3가지 비교 |
| 카카오페이 (2025, 증권·손보 포함) | Vault 사용, 봉투 암호화(DEK + KEK, AWS KMS), 환경별 프로필 분리 |
| 쏘카 (2024, FE) | 환경변수를 AWS Secrets Manager로 통일 |
| LY (2023) | 커밋 기록에 남은 비밀 키 → **첫 커밋부터 `.env`를 `.gitignore`**에 |
| 토스뱅크 (2025) | 자동 `toString()`이 계좌번호를 로그에 노출할 위험. `@Secret` 규칙은 빠뜨리면 무력 → 빌드 시점에 마스킹하는 컴파일러 플러그인 |

### 2.2 국내 기업 공식 GitHub ([A-기업])

| 저장소 | 확인한 내용 |
|---|---|
| LINE `line-fido2-server` | Spring Boot 2.7.12·Java 11(Boot 4 이전 세대라 그대로 옮기면 안 됨). MySQL/H2/Redis. 모듈 `common`/`core`(도메인)/`base`(JPA)/`demo`. "로컬은 내장 H2, staging·beta·production은 MySQL" · `spring.profiles.active: local`. REST Docs와 Swagger UI 둘 다 |
| 네이버 `fixture-monkey` | 테스트 객체 생성 라이브러리. "국내 대표 모바일 결제 서비스의 테스트 1만 개 이상에서 사용" |
| 네이버 `spring-jdbc-plus` | Spring Data JDBC 확장(README 예시 Boot 4.0.1). 국내 기업이 JPA만 쓰는 것은 아니다 (네이버 2026 글의 MyBatis 병행도 같은 방향) |

### 2.3 공식 문서로 확인한 사실

| 관례·사실 | 등급 | 근거 |
|---|---|---|
| 서블릿 보안은 `DelegatingFilterProxy` → `FilterChainProxy` → `SecurityFilterChain`. 요청마다 **처음 맞는 체인 하나만** 실행 | A | Spring Security Architecture |
| `ExceptionTranslationFilter`가 `AuthenticationException`은 인증 시작(401 계열), `AccessDeniedException`은 거부(403)로 바꾼다. 그래서 401·403 본문은 `@RestControllerAdvice`가 아니라 Security 핸들러에서 맞춘다 | A (뒷문장 D) | 같은 문서 + 03 문서 §5 |
| 비밀번호는 bcrypt·PBKDF2·scrypt·argon2 같은 **적응형 단방향 함수**로. 기본 `DelegatingPasswordEncoder`는 `{bcrypt}` 접두사. bcrypt 기본 strength 10, 검증 약 1초가 되게 조정 권장 | A | Spring Security Password Storage |
| CORS는 Spring Security **보다 먼저** 처리. preflight에는 쿠키가 없어 인증 실패로 막힌다 | A | Spring Security CORS |
| JWT 리소스 서버는 서명, `exp`·`nbf`, `iss`를 검증. `issuer-uri` 또는 `jwk-set-uri`만으로 자동 구성 | A | Spring Security Resource Server JWT |
| 환경변수는 `application.properties`보다 우선. `application-{profile}`이 공통 파일을 덮어씀. `${name:default}`로 기본값. actuator `/env`·`/configprops` 값은 기본 `******` | A | Boot Externalized Configuration, Actuator Endpoints |
| PostgreSQL 기본 격리 수준은 **READ COMMITTED**. REPEATABLE READ에서 다른 트랜잭션이 같은 행을 먼저 바꾸고 커밋하면 `could not serialize access` → 앱이 **전체 재시도** | A | PostgreSQL `mvcc.sgml` |
| MySQL(InnoDB) 기본 격리 수준은 **REPEATABLE READ** | A | MySQL 8.0 문서, 소스 `sys_vars.cc` (컬리 2024 운영 사례와 일치) |
| InnoDB 레코드 락: 기본 **next-key**(`LOCK_ORDINARY`), **갭 락**(`LOCK_GAP`), `LOCK_REC_NOT_GAP`. 삽입은 **insert intention** 락으로 그 틈의 충돌 락이 풀릴 때까지 대기. 유니크 키 단건 조회와 READ COMMITTED 잠금 읽기는 `LOCK_REC_NOT_GAP` | A | MySQL 소스 `lock0lock.h` |
| MySQL `GET_LOCK()`은 **커넥션이 소유하는 사용자 수준 락**(1 획득·0 타임아웃·NULL 오류). PostgreSQL advisory lock은 세션 단위면 롤백돼도 안 풀리고, `pg_advisory_xact_lock`은 트랜잭션 끝에 자동 해제 | A | MySQL `item_func.cc`, PG `mvcc.sgml`·`func-admin.sgml` |
| `EXPLAIN`은 실행 없이 계획을, `EXPLAIN ANALYZE`는 **실제 실행**해 실제 시간·행 수를 보여 준다. 쓰기 문장은 `BEGIN; ... ROLLBACK;`으로 감싼다 | A | PostgreSQL `perform.sgml` |

## 3. 갈리는 지점과 선택 기준

### 3.1 인증 방식

| 선택지 | 맞는 상황 | 주의 |
|---|---|---|
| 세션 + 쿠키 | 웹, 즉시 로그아웃·강제 만료가 중요 | 서버 여러 대면 세션 저장소 공유 필요. 하이퍼커넥트는 Redis 기반 Spring Session [C✓] |
| 자체 발급 JWT (access 짧게 + refresh) | 모바일 앱, 여러 서비스가 토큰 검증 | 발급 후 즉시 무효화가 어렵다 → 짧은 유효 기간 [C✓ LY]. refresh 저장·회전 설계가 핵심 (estimated) |
| 외부 인가 서버 + 리소스 서버(OAuth2) | 소셜 로그인, 사내 SSO, **제휴사 서버 간 API** | 이 앱은 토큰 검증만 한다 [A]. 카카오페이는 서버 간 통신에 Client Credentials [C✓] |

**이럴 땐 이것**: 학습 단계에서는 **세션 방식으로 필터 체인 동작을 먼저 이해**하고, 그다음 JWT로 바꿔 비교한다 [D]. JWT를 고르면 "취소가 어렵다"는 약점을 짧은 수명과 refresh 설계로 어떻게 막았는지 설명할 수 있어야 한다 (LY 원문의 문제의식, 면접 빈도는 estimated).

### 3.2 MySQL vs PostgreSQL (이 저장소는 PostgreSQL)

| 항목 | MySQL (InnoDB) | PostgreSQL | 근거 |
|---|---|---|---|
| 기본 격리 수준 | REPEATABLE READ | READ COMMITTED | A (양쪽) |
| RR 스냅샷 함정 | 트랜잭션이 길게 열려 있으면 다른 트랜잭션이 커밋한 행이 안 보인다 (컬리 장애) | 기본 RC라 문장마다 새 스냅샷 | MySQL C✓, PG A |
| 같은 행 동시 갱신(RR) | 기다린 뒤 최신 값에 갱신 (estimated) | 직렬화 실패 오류 → 앱이 재시도 | PG만 A |
| 범위 조건 락 | 갭 락·next-key 락으로 범위 삽입을 막는다. 예상 밖 데드락의 원인 (estimated) | 갭 락 없음. 범위 보호는 SERIALIZABLE (estimated) | MySQL 락 종류는 A |
| 애플리케이션 락 | `GET_LOCK()`은 커넥션 소유라 **같은 커넥션에서 해제**해야 한다. 그래서 락용 커넥션 풀을 따로 둔다(배민이 실제로 분리 [C✓]). 컬리는 이 부담 때문에 Redis를 택함 [C✓] | advisory lock (`pg_advisory_xact_lock`은 자동 해제) | A + C✓ |
| 실행 계획 | `EXPLAIN`, `EXPLAIN ANALYZE`(8.0.18+ [A]). OR 조건 Index Merge 한계 사례 [C✓ 네이버] | `EXPLAIN (ANALYZE, BUFFERS)`. 부분·표현식 인덱스 [C✓ 하이퍼커넥트] | PG A |
| 인덱스 | 클러스터드 PK, 보조 인덱스가 PK를 담음 (estimated) | 힙 + 인덱스 | — |

**이럴 땐 이것**: 국내 이직을 생각하면 MySQL RR 동작(스냅샷, 갭 락)과 실행 계획 읽기를 꼭 익힌다. 실습은 이 저장소의 PostgreSQL로 하고, 같은 시나리오를 MySQL 컨테이너로 한 번 돌려 비교한다 [D].

### 3.3 여러 서버의 동시성 제어

| 선택지 | 맞는 상황 | 확인한 사례 |
|---|---|---|
| 낙관적 락 (`@Version`) | 충돌이 드물고, 실패 시 사용자에게 다시 시도하게 해도 될 때 | 뱅크샐러드 [C✓] |
| DB 비관적 락 (`SELECT ... FOR UPDATE`, `@Lock`) | 돈·잔액처럼 갱신 경로가 많고 정합성이 최우선. **락 대상 행을 최소로** | 토스뱅크 [C✓] |
| Redis 분산 락 (Redisson) | 여러 서버, DB 밖 자원이나 여러 DB에 걸친 작업. **타임아웃 필수, 커밋 후 해제** | 컬리·하이퍼커넥트·LINE Plus·토스 [C✓] |
| DB 네임드 락 (`GET_LOCK`, advisory) | Redis가 없고 인프라를 늘리기 어려울 때. 락용 커넥션 풀 분리 | 배민 광고시스템(2019) [C✓]. 컬리는 같은 이유로 기각 |

**이럴 땐 이것**: 이 저장소(단일 서버, Redis 없음)는 **낙관적 락 → 필요하면 PostgreSQL 행 락/advisory lock** 순서로 충분하다 [D]. Redis 분산 락은 서버를 여러 대로 늘릴 때 배운다.

### 3.4 시크릿 관리

로컬·소규모는 환경변수 + `${VAR}` [A](지금 이 저장소), 로컬에서 값이 많으면 `.env`(첫 커밋부터 gitignore, LY [C✓]) + docker-compose, 운영에서 회전·감사가 필요하면 Vault·AWS Secrets Manager·KMS(하이퍼커넥트·카카오페이·쏘카 [C✓])를 쓴다.

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| `toss.client-secret=${TOSS_CLIENT_SECRET}` 등 시크릿을 환경변수로 | `application.properties` | 코드와 저장소에 시크릿이 없다 [A] |
| 민감정보: "별도 라이브러리 없이 출력하지 않는 규칙으로 통제" | `docs/public/05_개발컨벤션.md` §6 | "찍지 않기"가 1순위인 것은 맞다. 다만 토스뱅크는 **규칙만으로는 개발자가 빠뜨린다**고 보고 `toString()` 단계에서 강제했다 [C✓]. 민감 필드가 생기면 엔티티·DTO의 `toString`에서 제외하는 것까지 규칙에 넣는 것을 권한다 [D] |
| `idx_sentence_filter ON sentence(level, category) WHERE deleted_at IS NULL` | `docs/public/init.sql` | `findRandomBy`의 WHERE와 맞는 부분 인덱스. 하이퍼커넥트도 같은 PostgreSQL 기능으로 슬로우 쿼리를 풀었다 [C✓] |
| 인증 없음 | `docs/public/03_API명세.md` §1.2 (MVP 단일 사용자) | 팀 결정. 바꾸려면 사람 결정이 필요하다 |

### 4.2 바꿀 것 (또는 실습할 것)

| 현재 | 문제 | 방향 |
|---|---|---|
| `spring.datasource.password=postgres`가 공통 파일에 평문 | `docs/public/07_배포·운영.md` §3은 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD` 환경변수를 정의했지만 코드는 쓰지 않는다 | `${DB_URL:jdbc:postgresql://localhost:15432/tmi_word}`, `${DB_PASSWORD:postgres}`처럼 로컬 기본값 + 환경변수. 운영 값은 환경변수에만 (국내 공통 관례와 같은 방향 [C✓]) |
| `docs/public/07` §3의 `DB_URL` 예시가 `.../tmi`, `DB_USERNAME`이 `tmi_user` | 실제 설정은 DB `tmi_word`, 사용자 `postgres`. 예시대로 넣으면 접속 실패 | **사람 확인 항목**: 어느 쪽이 맞는지 정하고 팀 문서를 고칠지 결정. 이 노트는 docs/public을 고치지 않는다 |
| 프로필 파일 없음 | `show-sql=true` 같은 로컬 설정이 운영에도 적용 | `application-local`, `application-prod` 분리 (04 문서 §4.2). 카카오페이·LINE도 환경별 프로필 사용 [C✓, A-기업] |
| `findRandomBy`의 `ORDER BY RANDOM() LIMIT 1` | 맞는 행마다 난수를 만들고 정렬하므로 비용이 **맞는 행 수에 비례** (estimated) | 아래 EXPLAIN 실습으로 확인. 행이 많아지면 "개수 세기 + 임의 OFFSET" 등과 비교 |
| 인증 기능 없음 | 학습 목표상 필요한 역량 | 로드맵 "인증 추가" 단계에서 별도 실습 (팀 문서 변경은 사람 확인 후) |

EXPLAIN 실습 (psql). 하이퍼커넥트 원문대로 **데이터가 적으면 운영과 다른 계획이 나온다**. 시드 데이터를 수만 건 이상 넣고 분포(레벨·카테고리 비율)도 운영과 비슷하게 맞춘 뒤 본다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM sentence
WHERE level = 'LEVEL_2' AND category = 'DAILY' AND deleted_at IS NULL
ORDER BY RANDOM() LIMIT 1;
```

볼 것: `Index Scan using idx_sentence_filter`인지 `Seq Scan`인지, `Sort` 노드의 방식과 메모리, 예상 행 수와 실제 행 수의 차이.

기존 제안(`docs/ai/*`)과 비교: 인증·보안·DB 차이·동시성 선택표는 기존 범위 밖이라 새로 추가했다.

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 인증 vs 인가 / SecurityContext | 401 "누구인가" vs 403 "할 수 있는가". 인증 결과는 ThreadLocal 기반 SecurityContext에 담기니 비동기에서는 전파 필요 |
| 세션 vs JWT, CSRF·CORS | 상태를 서버에 두는가 토큰에 두는가. CSRF는 쿠키 인증의 방어, preflight `OPTIONS`는 쿠키가 없으니 Security보다 먼저 |
| 해시 vs 암호화 | 비밀번호는 해시(salt + work factor). 복호화가 필요한 개인정보는 암호화 + 키 관리(봉투 암호화, KMS) |
| 격리 수준·MVCC·갭 락 | 이상 현상이 어느 수준에서 막히는지. RR은 첫 읽기 스냅샷을 계속 본다. MySQL RR의 갭 락은 데드락 분석의 출발점 |
| 낙관적·비관적·분산 락 | 충돌 빈도, 서버 수, 정합성 요구로 고른다. 분산 락은 타임아웃과 "커밋 후 해제"가 핵심 |
| 인덱스·실행 계획 | 복합 인덱스는 등호 조건 먼저, 범위·정렬 뒤 (일반 원칙, estimated). 예상 행 수와 실제 행 수가 크게 다르면 통계·추정 문제 |

## 6. 기억할 것

1. 보안은 **필터 체인**에서 일어난다. 401·403 응답은 컨트롤러 어드바이스가 아니라 Security 설정에서 만든다.
2. MySQL은 RR + 갭 락, PostgreSQL은 RC가 기본이다. **같은 코드가 DB에 따라 다르게 동작**한다(컬리 RR 스냅샷 장애).
3. 여러 서버의 동시성은 국내 다수 기업이 Redis 분산 락으로 풀지만(배민은 MySQL `GET_LOCK`), 단일 서버라면 낙관적 락·DB 락으로 충분하다. 쿼리 성능은 **운영과 비슷한 양의 데이터**로 `EXPLAIN ANALYZE`를 본다.

## 7. 출처

### 확인한 출처 — 국내 기업 블로그 (2026-10-12 원문 확인)

| 기업 | URL (연도) |
|---|---|
| 컬리 | helloworld.kurly.com/blog/distributed-redisson-lock/ (2023), /blog/commit-mvcc-set-autocommit/ (2024) |
| 하이퍼커넥트 | hyperconnect.github.io/2019/11/15/redis-distributed-lock-1.html (2019), /2020/08/31/improve-slow-query.html (2020), /2020/06/13/microsrv-deploy-1.html (2020), /2023/01/09/redis-session.html (2023) |
| 토스·토스뱅크 | toss.tech/article/slash23-corebanking (2023), /article/25301 (2024), /article/toss-bank-interns (2025) |
| 네이버 | d2.naver.com/helloworld/6512234 (2026, 본문은 `/api/v1/contents/6512234`) |
| 카카오페이 (손보·증권 포함) | tech.kakaopay.com/post/ how-preparedstatement-works-in-our-apps (2025), spring-oauth2-authorization-server-practice (2023), account-token-swift-concurrency (2022), spring-multi-module-environment-variable (2025), kakaopayins-envelope-encryption (2025), choonsiri (2025) |
| LY·LINE Plus | techblog.lycorp.co.jp/ko/ experience-in-migrating-order-db-on-ecommerce-platform (2024), id-jag-next-generation-authentication-ai-era (2026), designing-software-like-an-open-source (2023) |
| 뱅크샐러드 | blog.banksalad.com/tech/banksalad-optimistic-lock/ (2026) |
| 우아한형제들 (구 블로그) | woowabros.github.io/experience/2019/05/30/mysql-user-level-lock.html (2019), /2020/07/06/db-sharding.html (2020), /2020/09/23/hibernate-batch.html (2020) |
| 쏘카 | tech.socar.kr/fe/2024/11/12/fe-cli (2024) |

### 확인한 출처 — 공식 문서·소스·기업 GitHub

Spring Security(`raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/`): `servlet/architecture.adoc`, `features/authentication/password-storage.adoc`, `servlet/integrations/cors.adoc`, `servlet/oauth2/resource-server/jwt.adoc`, `servlet/authentication/session-management.adoc`(목차만). Spring Boot v4.0.2(`.../spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/`): `features/external-config.adoc`, `actuator/endpoints.adoc`. PostgreSQL(`raw.githubusercontent.com/postgres/postgres/master/doc/src/sgml/`): `mvcc.sgml`, `perform.sgml`, `func/func-admin.sgml`(함수 이름만). MySQL 8.0 문서(`dev.mysql.com/doc/refman/8.0/en/`): `explain.html`, `innodb-transaction-isolation-levels.html`. MySQL 소스(`raw.githubusercontent.com/mysql/mysql-server/trunk/`): `sql/sys_vars.cc`, `storage/innobase/include/lock0lock.h`, `sql/item_func.cc`. 기업 GitHub: `github.com/line/line-fido2-server`(README, demo `application.yml`), `github.com/naver/fixture-monkey`, `github.com/naver/spring-jdbc-plus`.

<details>
<summary>미확인 / 접근 실패 [C?]</summary>

| 대상 | 이유 |
|---|---|
| 여기어때 Secret 플랫폼 구축기 (techblog.gccompany.co.kr, Medium) | 403 |
| 카카오 tech.kakao.com MySQL 글(posts/721, 712, 2025) | 본문이 HTML 내장 JSON에 있으나 이번에 읽지 않음 |

</details>
