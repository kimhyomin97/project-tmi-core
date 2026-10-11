[← be-standards](./README.md)

# 07. 인증·보안 기본기, 설정·시크릿, DB(MySQL vs PostgreSQL)와 쿼리 튜닝

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조. 이 문서만의 한계: `dev.mysql.com`, `postgresql.org`는 DNS 실패라 MySQL은 **소스 코드 주석**, PostgreSQL은 GitHub 문서 소스로 대신 확인했다.

## 1. 한 줄 요약

인증은 **Spring Security 필터 체인** 위에서 세션이나 JWT 중 하나로 구현하고, 비밀번호는 **적응형 해시(bcrypt 등)**, 시크릿은 **코드 밖(환경변수·시크릿 저장소)**에 둔다. DB는 국내 서비스에 MySQL이 많지만 이 저장소는 PostgreSQL이다. **두 DB의 기본 격리 수준과 락 동작이 다르다**는 점을 알고, 쿼리는 `EXPLAIN`으로 확인한다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

이 문서에는 국내 기업 블로그 근거가 없다. 대신 국내 기업 공식 GitHub 원문(2.1)을 넣었다. "국내 서비스는 MySQL이 많다"는 여전히 [D](estimated)다.

### 2.1 국내 기업 실무 흔적 (공식 GitHub 원문, [A-기업])

| 무엇을 보여 주나 | 기업 | 원문에서 확인한 내용 |
|---|---|---|
| 인증 서버의 실제 구성 | LINE(LY) `line-fido2-server` | FIDO Alliance 인증을 받은 FIDO2 서버. Spring Boot 2.7.12(배지) · Java 11 — Boot 4 이전 세대(javax→jakarta, Security 5→6/7 변경 전)라 설정·API를 그대로 옮기면 안 됨. 저장소 MySQL/H2/Redis 배지. 모듈을 `common`/`core`(도메인)/`base`(Spring JPA: 서비스 구현·Repository·Entity)/`demo`로 나눔 |
| 로컬과 운영의 DB 분리 | 같은 저장소 | "로컬은 내장 H2, staging·beta·production은 MySQL 같은 독립 DB로 교체" (README). `application.yml`은 `spring.profiles.active: local` |
| API 문서 도구 | 같은 저장소 | Spring REST Docs와 Swagger UI를 둘 다 제공 |
| 테스트 데이터 생성 | 네이버 `fixture-monkey` | Java·Kotlin 테스트 객체 생성 라이브러리. "국내 대표 모바일 결제 서비스의 테스트 1만 개 이상에서 사용" (README) |
| JPA 외 데이터 접근 | 네이버 `spring-jdbc-plus` | Spring Data JDBC 확장. README 예시가 Spring Boot 4.0.1 기준. 국내 기업이 JPA만 쓰는 것은 아니다 |

읽는 법: LINE 사례는 이 저장소의 계획(로컬 프로필 분리, 도메인과 JPA 계층 구분)과 같은 방향이다. 다만 **한 회사의 오픈소스 데모 1건**이라 "표준"의 근거로는 약하다 [D].

### 2.2 공식 문서로 확인한 사실

| 관례·사실 | 등급 | 근거 |
|---|---|---|
| 서블릿 앱의 보안은 `DelegatingFilterProxy` → `FilterChainProxy` → `SecurityFilterChain`으로 흐른다. 요청마다 **처음 맞는 체인 하나만** 실행된다 | A | Spring Security Architecture |
| `ExceptionTranslationFilter`가 `AuthenticationException`은 인증 시작(401 계열)으로, `AccessDeniedException`은 거부 처리(403)로 바꾼다 | A | 같은 문서 |
| 필터에서 난 예외는 `@RestControllerAdvice`를 거치지 않는다. 401·403 본문 형식은 Security 쪽 핸들러에서 맞춘다 | D | 위 구조 + 03 문서 §5 |
| 비밀번호는 bcrypt·PBKDF2·scrypt·argon2 같은 **적응형 단방향 함수**로 저장. 기본 `DelegatingPasswordEncoder`는 `{bcrypt}` 접두사로 저장한다. bcrypt 기본 strength 10, 검증이 약 1초 걸리게 조정 권장 | A | Spring Security Password Storage |
| CORS는 Spring Security **보다 먼저** 처리해야 한다. preflight 요청에는 쿠키가 없어 인증 실패로 막히기 때문이다 | A | Spring Security CORS |
| JWT 리소스 서버는 서명, `exp`·`nbf`, `iss`를 검증한다. `issuer-uri` 또는 `jwk-set-uri`만 설정하면 자동 구성된다 | A | Spring Security OAuth2 Resource Server JWT |
| 환경변수는 `application.properties`보다 우선한다. `application-{profile}.properties`가 공통 파일을 덮어쓴다. `${name:default}`로 기본값 | A | Spring Boot Externalized Configuration |
| actuator `/env`, `/configprops` 값은 기본으로 전부 `******`로 가려진다 (`show-values` 기본 `never`) | A | Spring Boot Actuator Endpoints |
| PostgreSQL 기본 격리 수준은 **READ COMMITTED**. REPEATABLE READ에서 다른 트랜잭션이 같은 행을 먼저 바꾸고 커밋하면 `could not serialize access` 오류로 롤백되고, 앱이 **전체를 재시도**해야 한다 | A | PostgreSQL `mvcc.sgml` |
| MySQL(InnoDB) 기본 격리 수준은 **REPEATABLE READ** | A | MySQL 소스 `sys_vars.cc` `DEFAULT(ISO_REPEATABLE_READ)` |
| InnoDB 레코드 락은 세 종류다: 기본 **next-key 락**(`LOCK_ORDINARY`), 레코드 앞 틈만 잡는 **갭 락**(`LOCK_GAP`), 레코드만 잡는 `LOCK_REC_NOT_GAP`. 삽입은 **insert intention** 락으로 그 틈의 충돌 락이 풀릴 때까지 기다린다 | A | MySQL 소스 `lock0lock.h` 주석 |
| 같은 주석: 유니크 키로 한 행을 찾을 때와 **READ COMMITTED의 일반 잠금 읽기**는 `LOCK_REC_NOT_GAP`(갭을 막지 않음)을 쓴다 | A | 같은 파일 |
| `GET_LOCK()`은 **커넥션이 소유하는 사용자 수준 락**이다. 반환값은 1(획득), 0(타임아웃), NULL(오류) | A | MySQL 소스 `item_func.cc` 주석 |
| PostgreSQL advisory lock: 세션 단위는 롤백돼도 풀리지 않는다. 트랜잭션 단위(`pg_advisory_xact_lock`)는 트랜잭션 끝에 자동 해제 | A | PostgreSQL `mvcc.sgml`, `func-admin.sgml` |
| `EXPLAIN`은 실행하지 않고 계획(비용, 예상 행 수)을, `EXPLAIN ANALYZE`는 **실제로 실행**하고 실제 시간·행 수를 보여 준다. 쓰기 문장은 `BEGIN; ... ROLLBACK;`으로 감싼다 | A | PostgreSQL `perform.sgml` |

## 3. 갈리는 지점과 선택 기준

### 3.1 인증 방식

| 선택지 | 맞는 상황 | 주의 |
|---|---|---|
| 세션(서버 저장) + 쿠키 | 웹 단일 도메인, 즉시 로그아웃·강제 만료가 중요 | 서버 여러 대면 세션 저장소 공유(Spring Session + Redis) 필요 (estimated) |
| 자체 발급 JWT(access 짧게 + refresh) | 모바일 앱, 여러 서비스가 토큰을 검증 | 발급 후 취소가 어렵다. refresh 저장·회전 설계가 핵심 (estimated) |
| 외부 인가 서버 + 리소스 서버(OAuth2) | 소셜 로그인, 사내 SSO | 이 앱은 토큰 검증만 한다 [A] |

**이럴 땐 이것**: 학습 단계에서는 **세션 방식으로 필터 체인 동작을 먼저 이해**하고, 그다음 JWT로 바꿔 보며 차이를 비교한다 [D]. 면접에서는 "왜 JWT를 골랐나"보다 "JWT의 단점(취소, 탈취)을 어떻게 막았나"를 묻는다 (estimated).

### 3.2 MySQL vs PostgreSQL (이 저장소는 PostgreSQL)

| 항목 | MySQL (InnoDB) | PostgreSQL | 근거 |
|---|---|---|---|
| 기본 격리 수준 | REPEATABLE READ | READ COMMITTED | A (양쪽 원문) |
| 같은 행 동시 갱신(RR) | 기다린 뒤 최신 값에 갱신 (estimated) | 직렬화 실패 오류 → 앱이 재시도 | PG만 A |
| 범위 조건 락 | 갭 락·next-key 락으로 범위에 삽입을 막는다. 예상 밖 데드락의 흔한 원인 (estimated) | 갭 락 없음. 범위 보호가 필요하면 SERIALIZABLE (estimated) | MySQL 락 종류는 A(소스) |
| 애플리케이션 락 | 네임드 락 `GET_LOCK()`. 커넥션 소유라 **같은 커넥션에서 `RELEASE_LOCK`**해야 한다. 커넥션 풀에서는 락용 커넥션을 따로 잡는 설계가 필요 [D] | advisory lock (`pg_advisory_xact_lock`은 트랜잭션 끝에 자동 해제) | 양쪽 A |
| 실행 계획 | `EXPLAIN`, `EXPLAIN ANALYZE`(8.0.18+, 미확인) | `EXPLAIN (ANALYZE, BUFFERS)` | PG만 A |
| 인덱스 | 클러스터드 PK. 보조 인덱스가 PK를 담아 커버링 인덱스가 중요 (estimated) | 힙 + 인덱스. 부분 인덱스·표현식 인덱스 지원 | — |

**이럴 땐 이것**: 국내 이직을 생각하면 MySQL 차이(RR + 갭 락, 클러스터드 PK)를 개념으로 꼭 익힌다. 실습은 이 저장소의 PostgreSQL로 하고, 같은 시나리오를 MySQL 컨테이너로 한 번 돌려 비교하면 가장 좋다 [D].

### 3.3 시크릿 관리

| 선택지 | 맞는 상황 |
|---|---|
| 환경변수 + `${VAR}` 자리표시자 | 로컬·소규모 배포. 지금 이 저장소의 방식 [A] |
| `.env`(gitignore) + docker-compose | 로컬에서 여러 값을 한 번에 주입 (estimated) |
| 시크릿 매니저(Vault, 클라우드 Secrets Manager) | 운영. 회전·감사가 필요할 때 (estimated) |

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| `toss.client-secret=${TOSS_CLIENT_SECRET}` 등 시크릿을 환경변수로 주입 | `application.properties` | 코드와 저장소에 시크릿이 없다 [A] |
| 민감정보 마스킹: "별도 라이브러리 없이 출력하지 않는 규칙으로 통제" | `docs/public/05_개발컨벤션.md` §6 | 마스킹보다 먼저 "찍지 않기"가 기본이다 [D] |
| `idx_sentence_filter ON sentence(level, category) WHERE deleted_at IS NULL` | `docs/public/init.sql` | `findRandomBy`의 WHERE 조건과 맞는 부분 인덱스다 |
| 인증 없음 | `docs/public/03_API명세.md` §1.2 (MVP 단일 사용자) | 팀 결정. 바꾸려면 사람 결정이 필요하다 |

### 4.2 바꿀 것 (또는 실습할 것)

| 현재 | 문제 | 방향 |
|---|---|---|
| `spring.datasource.password=postgres`가 공통 파일에 평문 | `docs/public/07_배포·운영.md` §3은 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD` 환경변수를 정의했지만 코드는 쓰지 않는다 | `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:15432/tmi_word}`, `spring.datasource.password=${DB_PASSWORD:postgres}`처럼 로컬 기본값 + 환경변수로. 운영 값은 환경변수에만 |
| `docs/public/07` §3의 `DB_URL` 예시가 `.../tmi`, `DB_USERNAME` 예시가 `tmi_user` | 실제 `application.properties`는 DB 이름 `tmi_word`, 사용자 `postgres`. 예시를 그대로 넣으면 접속이 실패한다 | **사람 확인 항목**: 어느 쪽이 맞는지 정하고 팀 문서(docs/public)를 고칠지 결정. 이 노트는 docs/public을 고치지 않는다 |
| 프로필 파일 없음 (`application.properties` 하나) | `show-sql=true` 같은 로컬 설정이 운영에도 적용된다 | `application-local.properties`, `application-prod.properties` 분리 (04 문서 §4.2와 함께) |
| `findRandomBy`의 `ORDER BY RANDOM() LIMIT 1` | 조건에 맞는 행마다 난수를 만들고 정렬하므로 비용이 **맞는 행 수에 비례**한다 (estimated) | 아래 EXPLAIN 실습으로 실제 계획을 확인. 행이 많아지면 "개수 세기 + 임의 OFFSET" 등과 비교 |
| 인증 기능 없음 | 학습 목표상 꼭 필요한 역량 | 로드맵 단계 "인증 추가"에서 별도 기능으로 실습 (팀 문서 변경은 사람 확인 후) |

EXPLAIN 실습 (psql, 시드 데이터를 충분히 넣은 뒤):

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM sentence
WHERE level = 'LEVEL_2' AND category = 'DAILY' AND deleted_at IS NULL
ORDER BY RANDOM() LIMIT 1;
```

볼 것: `Index Scan using idx_sentence_filter`인지 `Seq Scan`인지, `Sort` 노드의 방식과 메모리, 예상 행 수와 실제 행 수의 차이.

### 4.3 기존 제안과 다른 점

| 항목 | 기존 (`docs/ai/*`) | 이번 조사 |
|---|---|---|
| 인증·보안 | 범위 밖 | 학습 로드맵에 추가 |
| 설정·시크릿 | 프로필 분리(`local`)는 로깅 맥락에서만 언급 | 환경변수 우선순위와 프로필 분리를 체계적으로 |
| DB 차이·EXPLAIN | 없음 | MySQL 대비표, EXPLAIN 실습 추가 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 인증 vs 인가 | 인증은 "누구인가"(401), 인가는 "할 수 있는가"(403) |
| SecurityContext | 인증 결과를 담는 곳. 기본은 ThreadLocal이라 비동기에서는 전파가 필요하다 |
| 세션 vs JWT | 상태를 서버에 두는가 토큰에 두는가. 취소·확장·탈취 대응이 다르다 |
| 해시 vs 암호화 | 비밀번호는 복호화가 필요 없으니 해시. salt와 work factor로 무차별 대입을 늦춘다 |
| CSRF | 쿠키 인증일 때 필요한 방어. 헤더로 토큰을 보내는 API는 영향이 다르다 |
| CORS preflight | 브라우저가 `OPTIONS`로 먼저 묻는다. 쿠키가 없으니 Security보다 먼저 처리 |
| 로그 마스킹 | 1순위는 민감 필드를 찍지 않기(`toString` 제외). 2순위로 로그 패턴 단계 치환 (estimated) |
| 격리 수준과 이상 현상 | Dirty Read, Non-repeatable Read, Phantom, Lost Update가 어느 수준에서 막히는지 |
| MVCC | 읽기가 쓰기를 막지 않도록 행 버전을 유지한다. 두 DB 모두 MVCC지만 구현이 다르다 |
| 갭 락 | MySQL RR에서 레코드 사이 틈에 삽입을 막는 락(소스 확인). 데드락 분석 질문에 자주 나온다는 점은 (estimated) |
| 커버링 인덱스 | 인덱스만으로 결과를 만들어 테이블 접근을 없앤다 |
| 복합 인덱스 순서 | 등호 조건 컬럼 먼저, 범위·정렬 컬럼 뒤 (estimated, 일반 원칙) |
| 실행 계획 읽기 | 예상 행 수와 실제 행 수가 크게 다르면 통계가 낡았거나 조건이 잘못 추정된 것 |

## 6. 기억할 것

1. 보안은 **필터 체인**에서 일어난다. 401·403 응답은 컨트롤러 어드바이스가 아니라 Security 설정에서 만든다.
2. MySQL은 RR + 갭 락, PostgreSQL은 RC가 기본이다. **같은 코드가 DB에 따라 다르게 동작**할 수 있다.
3. 쿼리 성능은 추측하지 말고 `EXPLAIN ANALYZE`로 본다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/servlet/architecture.adoc | Spring | 현행 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/features/authentication/password-storage.adoc | Spring | 현행 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/servlet/integrations/cors.adoc | Spring | 현행 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/servlet/oauth2/resource-server/jwt.adoc | Spring | 현행 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-security/main/docs/modules/ROOT/pages/servlet/authentication/session-management.adoc | Spring | 현행 | 공식 (목차만 확인) |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/external-config.adoc | Spring | 2026 (v4.0.2) | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/endpoints.adoc | Spring | 2026 (v4.0.2) | 공식 |
| https://raw.githubusercontent.com/postgres/postgres/master/doc/src/sgml/mvcc.sgml | PostgreSQL | 현행 | 공식 문서 소스 |
| https://raw.githubusercontent.com/postgres/postgres/master/doc/src/sgml/perform.sgml | PostgreSQL | 현행 | 공식 문서 소스 |
| https://raw.githubusercontent.com/postgres/postgres/master/doc/src/sgml/func/func-admin.sgml | PostgreSQL | 현행 | 공식 문서 소스 (함수 이름만) |
| https://raw.githubusercontent.com/mysql/mysql-server/trunk/sql/sys_vars.cc | Oracle MySQL | 현행 | 공식 소스 (기본 격리 수준) |
| https://raw.githubusercontent.com/mysql/mysql-server/trunk/storage/innobase/include/lock0lock.h | Oracle MySQL | 현행 | 공식 소스 (갭·next-key·insert intention 락) |
| https://raw.githubusercontent.com/mysql/mysql-server/trunk/sql/item_func.cc | Oracle MySQL | 현행 | 공식 소스 (`GET_LOCK`) |
| https://github.com/line/line-fido2-server (README, `fido2-demo/demo/src/main/resources/application.yml`) | LINE(LY) | 현행 (연도 미표기) | 기업 공식 GitHub |
| https://github.com/naver/fixture-monkey (README) | NAVER | 현행 | 기업 공식 GitHub |
| https://github.com/naver/spring-jdbc-plus (README) | NAVER | 현행 | 기업 공식 GitHub |

### 미확인 / 접근 실패

| URL | 이유 |
|---|---|
| https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html | DNS 실패. 갭 락·`GET_LOCK`은 소스로 대신 확인했다. MySQL `EXPLAIN ANALYZE`(8.0.18+)는 여전히 미확인 |
| https://www.postgresql.org/docs/current/transaction-iso.html | DNS 실패 (같은 내용을 GitHub 문서 소스로 대신 확인) |
| https://techblog.woowahan.com/ (MySQL 네임드 락 분산락 글 후보) | DNS 실패(ENOTFOUND). 웹 검색도 한도 소진 |
| 국내 기업의 인증·MySQL 락 사례 | 웹 검색 예산 소진으로 찾지 못함. 직접 찾아 읽을 후보 주제: 우아한형제들 MySQL 네임드 락 분산락, 국내 기업 JWT·세션 도입 회고, 개인정보 로그 마스킹 |
