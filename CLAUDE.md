# CLAUDE.md — TMI Core

> **⚠ 확정 전 초안.** 아래 규칙 중 `[제안]` 표시는 [docs/ai/decisions-needed.md](docs/ai/decisions-needed.md) 의 결정을 기다리는 항목이다.
> 결정 전에는 `[제안]` 규칙을 근거로 **기존 코드를 고치지 말 것** — 새 코드에만 참고하고, 충돌하면 사람에게 묻는다.
> 근거·상세: [docs/ai/](docs/ai/) (current-state · conventions-proposal · architecture-proposal · ai-workflow)

## 프로젝트

영어 문장 번역 → 의미 유사도 채점 학습 서비스의 API 서버. Spring Boot 4.0.2 · Java 21 · Gradle 9.3 · JPA/PostgreSQL · Lombok · springdoc.
기능 모듈: `sentence`(출제) · `answer`(채점) · `infra.similarity`(외부 유사도 서버) · `toss.holdings`(보유자산 스냅샷, 구현 중) · `common`(공통).
사내 설계·규칙 문서: `docs/public/` (01 기획 ~ 07 운영, 99 공통).

## 빌드·테스트

```bash
bash ./gradlew build                 # 컴파일 + 전체 테스트. gradlew 에 실행 권한이 없어 bash 로 실행
bash ./gradlew test                  # 테스트만
bash ./gradlew test --tests 'com.tmi.core.architecture.*'   # 아키텍처 규칙 (ArchUnit)
bash ./gradlew spotlessCheck         # 포맷 검사 (현재 build 에 미포함, 기존 코드 위반 있음)
bash ./gradlew spotlessApply         # 포맷 자동 수정 — 사람 승인 없이 기존 src/main 전체에 돌리지 말 것
```

- **`./gradlew test` 는 PostgreSQL 이 필요하다** (`CoreApplicationTests` 가 `localhost:15432/tmi_word` 접속, `ddl-auto=validate`).
  DB 없으면 `PSQLException: Connection refused` 로 실패 — 코드 문제가 아니다. 기동 방법: README §시작하기 또는 [docs/ai/blockers.md](docs/ai/blockers.md) §2.
  스키마: `psql -h localhost -p 15432 -U postgres -d tmi_word -f docs/public/init.sql`
- 아키텍처 테스트 중 `@Disabled` 는 **알려진 위반** (사유 주석에 해소 작업 ID). 위반을 고쳤으면 `@Disabled` 를 지워 통과를 확인한다. 새 위반을 `@Disabled` 로 숨기지 말 것.
- 테스트 소스셋에는 **Lombok 이 없다** (main 에만 선언). 테스트 코드는 Lombok 없이 작성.
- Gradle `--tests` 를 여러 개 주면 **OR** 다 — 특정 테스트들이 실제로 돌았는지 확인하려면 클래스마다 따로 실행.
- `.gitignore` 의 `out/` 규칙이 `adapter/out/` 같은 경로도 무시한다 — 그런 경로에 새 파일을 만들면 `git status` 로 추적 여부 확인.
- Boot 4: `@MockitoBean` 사용 (`@MockBean` 제거됨). `@WebMvcTest` 는 `org.springframework.boot.webmvc.test.autoconfigure`.

## 패키지·의존 규칙

현재 (05 개발컨벤션 §1.3): `{기능}/{controller,service,repository,domain,dto,exception}`, 외부 클라이언트는 `infra/{시스템}/`, 공통은 `common/`.

- controller → service → repository 방향만. controller 에서 repository 직접 사용 금지 (ArchUnit R4).
- `domain` 은 controller·service·repository·dto 에 의존 금지 (R4b).
- `common` 은 기능 모듈에 의존 금지 (R3) — 현재 `RestClientConfig` 가 위반 중.
- `[제안]` 다른 기능의 `repository`·`domain`·`exception` 직접 import 금지. 추천안(B)이면 더 좁게 — 다른 모듈은 그 모듈의 **base package 타입(공개 API)** 으로만 사용 (R2). A′/B 는 decisions-needed D1 에서 결정.
- 헥사고날(포트·어댑터) 구조로 바꾸지 말 것 — 팀이 한 번 도입했다 되돌렸다 (02 설계 §2.2).

## 반드시 지킬 것 (실제 결함에서 나온 규칙)

- **HTTP Interface(`@PostExchange` 등) 메서드 파라미터에는 `@RequestBody`/`@RequestParam`/`@PathVariable` 필수.** 없으면 호출 시 항상 `IllegalStateException` (현재 `SimilarityClient.score` 가 이 상태 — 채점 API 500).
- **`@Transactional` 메서드를 같은 클래스 안에서 호출하지 말 것** (프록시 미경유 → 무효). 트랜잭션이 필요한 조회·저장은 다른 빈으로 (R6).
- **리포지토리에 선언한 쿼리 메서드(파생·`@Query`)에는 기본 트랜잭션이 없다.** 트랜잭션 경계는 서비스가 `@Transactional(readOnly = true)` 등으로 책임진다.
- **외부 HTTP 호출은 트랜잭션 밖에서.** 현재 `spring.jpa.open-in-view` 기본값(true) 때문에 트랜잭션 밖 조회 후에도 커넥션이 요청 끝까지 잡힌다 — `[제안]` 서비스가 DTO 반환 후 OSIV off.
- native `@Query` 에 enum 을 넘길 때는 SpEL `:#{#level.name()}` 이 필요하다 (Hibernate 7 은 ORDINAL 로 바인딩). 06 가이드 §2.3 의 "자동 바인딩" 문구는 틀렸다.
- `RestClient.Builder` 빈은 **없다** (Boot 4, `spring-boot-starter-restclient` 미포함). `RestClient.builder()` 정적 호출을 쓴다.
- `[제안]` 서비스 public 메서드는 `@Entity` 를 반환하지 않는다 — 컨트롤러엔 응답 DTO (R5).

## 예외·로깅

- 예외는 `BaseException` 하위의 "외부 클래스 + 정적 중첩 클래스" (`SentenceException.NotFound`). 새 에러 = `ETmiErrorType` 상수 + 중첩 클래스 + `messages/messages*.properties` 한 줄씩.
- `@RestControllerAdvice` 는 `GlobalExceptionHandler` 하나뿐. 컨트롤러에서 try/catch 금지.
- 에러 응답 형식 `{"error":{"code":"TMI.{DOMAIN}.{REASON}","message":...}}` 은 외부 계약 (03 API 명세 §1.4). 바꾸지 말 것.
- 현재 메시지 번들이 로드되지 않는다 (`spring.messages.basename` 미설정) — 응답 message 가 코드 문자열이면 이것 때문.
- 로그: `@Slf4j`, placeholder `{}`, 형식 `"{대상} {동작}: key=value"`. throw 직전에 로그 남기지 말 것 (핸들러가 단일 기록). 토큰·시크릿 로그 금지.
- 외부 호출 예외는 **catch 3개**로 도메인 예외로 변환: `RestClientResponseException`(4xx→502 `RequestInvalid`, 5xx→503 `Unavailable`) / `ResourceAccessException`(→503) / `RestClientException`(→503). 세 번째를 빼면 POST 의 read timeout 이 500 이 된다 (현재 `AnswerService` 가 이 상태). 빈 응답(`null`)도 503.

## 작업 방식

- 작업 단위 = 검증 명령이 정해진 항목 1개, 모듈 1개, `src/main` 변경 5파일 이하. 상세: [docs/ai/ai-workflow.md](docs/ai/ai-workflow.md).
- 버그 수정은 **실패하는 테스트 먼저** 커밋, 그다음 수정.
- 외부 API 응답 변경 · 패키지 이동 · 의존성 추가 · `docs/public` 수정은 구현 전에 사람에게 계획 확인.
- 스키마 변경 시 3곳 동기화: `docs/public/04_데이터모델.md`, `docs/public/init.sql`, `*Entity.java` (Flyway 미사용).
- 성공을 주장할 때는 실행한 명령과 결과를 함께 적는다.
