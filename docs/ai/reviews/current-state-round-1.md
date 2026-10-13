# current-state.md 리뷰 — Round 1

> 리뷰 대상: `docs/ai/current-state.md` @ `f1cca69` (469줄, 트랜잭션 서술 정정본). 라인 번호는 이 버전 기준.
> 검증 방법: `src/main` 34개 파일 전부 정독, `docs/public/*` 대조, 그리고 **일회성 프로브 테스트 4종**(`src/test/.../zzreviewprobe/*` — 실행 후 삭제, `git status` 로 원상복구 확인)을 로컬 PostgreSQL(`localhost:15432`, `init.sql` 시드)에서 돌렸다. WIP 컴파일은 scratchpad 의 `git archive HEAD` 사본에 `wip-snapshot.patch` 를 적용해 확인했다. Mermaid 는 `mermaid@11.4.1` 파서(jsdom)로 파싱했다.
> 아래에서 [R실행] 은 리뷰어가 직접 실행해 얻은 결과다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | 대부분 file:line 근거가 있고 P1~P7 프로브도 재현되지만, 실행으로 뒤집히는 결론 1건(OSIV 커넥션), Boot 4 에서 틀린 주장 1건(`RestClient.Builder` 빈), WIP 기준으로 적은 라인 번호, 없는 research 절 인용이 있다 |
| 구체성 | 4 | 프로브 표·로그·라인 번호 수준이라 바로 따라가 볼 수 있다. 다만 SpEL·i18n 처럼 프로브 한 번이면 끝날 문제를 "미검증"으로 남겨 두었다 |
| 일관성 | 2 | §1.7 과 F2 의 검증 등급이 다르고, §5 "되돌리지 않았다" 는 실제 stash 상태와 다르며, 비어 있는 §6 와 없는 문서·절을 인용한다. 헤더의 브랜치·HEAD 표기도 실제와 다르다 |
| 판단 가능성 | 3 | F1~F8 요약은 결정에 쓸 만하다. 그러나 C1 이 OSIV 결정을 잘못 이끌고, 06 가이드 자체가 결함의 원인이라는 점과 API 계약 위반을 빠뜨렸다 |

## 치명 (승인 차단)

- [C1] §3.4 L295 ("06 §3.3 의 의도(커넥션 점유 방지)는 결과적으로 충족된다"), §3.2 L208 ("지금은 연관관계가 없어 증상은 없음 [정적]") — **실행해 보면 틀린 결론이다.** OSIV(기본 `true`) 상태에서 `findByIdAndDeletedAtIsNull` 이 트랜잭션 없이 실행되면, 요청에 묶인 EntityManager 가 JDBC 커넥션을 요청이 끝날 때까지 쥐고 있다. 따라서 `POST /answers` 는 Similarity 외부 호출(read 2s + connect 1s) 동안 DB 커넥션 1개를 점유한다. 06 §3.3 이 막으려던 안티패턴이 **지금 이미** 일어나고 있다. — 근거: `answer/service/AnswerService.java:28-30,37` + `application.properties` 에 `spring.jpa.open-in-view` 키 없음. [R실행] `@MockitoBean SimilarityClient` 의 응답 콜백 안에서 `HikariPoolMXBean.getActiveConnections()` 를 측정했다. 기본 설정은 `activeConnections=1 txActive=false`, 같은 테스트에 `spring.jpa.open-in-view=false` 만 주면 `activeConnections=0` 이다. — 요구 수정: (1) L295·L208 을 이 실행 결과로 고친다. (2) F4(L22) 또는 별도 F 항목에 "OSIV + 무트랜잭션 조회 → 외부 호출 중 커넥션 점유" 를 추가하고 [실행] 으로 표기한다. (3) §1.7 L109 의 `open-in-view` 항목을 "경고뿐" 이 아니라 실제 증상으로 격상한다. (4) research.md §4.1 이 OSIV 를 언급하므로 그쪽과 함께 결정 항목(OSIV off 여부)으로 연결한다.

## 주요

- [M1] §2 L175 "순환 | 없음" — `sentence↔answer` 만 검사했다. 최상위 슬라이스 기준으로 보면 **`common ↔ infra` 순환**이 있다: `common.config → infra.similarity`(`RestClientConfig.java:3`), `infra.similarity.exception → common.exception`(`SimilarityException.java:3-4`). 다이어그램(L161 `ISE --> CE`, L163 `CC ==> IS`)에 두 화살표가 다 그려져 있는데 표는 "없음" 이라고 적었다. 6단계 ArchUnit 에 `slices().matching("com.tmi.core.(*)..").should().beFreeOfCycles()` 를 넣으면 바로 걸린다. WIP 가 들어오면 `common ↔ toss` 순환도 같은 방식으로 생긴다. — 요구 수정: 순환 행을 "common↔infra 1건 (+WIP 시 common↔toss)" 으로 고치고 F6 에 반영한다.

- [M2] §1.6 L94 ④, §4.6 L389 "Boot 가 제공하는 `RestClient.Builder` 프로토타입 빈" — **Boot 4.0.2 의 이 프로젝트에는 그런 빈이 없다.** Boot 4 는 모듈로 쪼개졌고 `RestClientAutoConfiguration` 은 `spring-boot-restclient`(즉 `spring-boot-starter-restclient`)에 들어 있다. 이 모듈은 `runtimeClasspath` 에 없다(`gradlew dependencies` 결과에 `spring-boot-restclient` 없음, `build.gradle:26-38`). [R실행] `ctx.getBeanNamesForType(RestClient.Builder.class).length == 0`. 문서를 믿고 `RestClient.Builder` 를 주입하면 컨텍스트 기동이 실패한다. 인용한 "research.md §1.3" 절도 존재하지 않는다(research.md 에는 §1.1·§1.2 뿐). — 요구 수정: "Boot 4 에서는 `spring-boot-starter-restclient` 를 추가해야 Builder 빈과 `spring.http.serviceclient.*`/`@ImportHttpServices` 기반 HTTP Service 자동 구성을 쓸 수 있다. 현재는 의존성이 없다" 로 고치고 출처를 붙인다.

- [M3] §1.6 L94 ③ `RestClientConfig.java:33`, §4.6 L389 `RestClientConfig.java:36` — **라인 번호가 HEAD 가 아니라 WIP 패치 적용본 기준이다.** HEAD 에서는 `setConnectTimeout` 이 `:26`, `RestClient.builder()` 가 `:29` 다. WIP 패치는 import 1줄과 `@Value` 필드 6줄을 더하므로 정확히 +7 이 된다. L5 "이 문서의 표는 커밋된 코드 기준" 과 모순된다. — 요구 수정: 26/29 로 고치고, 다른 라인 인용도 HEAD 기준인지 한 번 더 기계적으로 대조한다(나머지 인용은 리뷰어가 대조해 일치함을 확인).

- [M4] §3.3(e) L265 "`logLevel(...)` 은 사용되지만" — **틀렸다.** `grep -rn "\.logLevel(" src/main` 결과 setter 호출은 0건이다. 핸들러가 `getLogLevel()` getter 만 읽는다(`GlobalExceptionHandler.java:27`). 그래서 모든 `BaseException` 이 기본값 ERROR(`BaseException.java:15`)로 남고, `logLevel` 체이닝 API 도 `logMessage` 와 마찬가지로 죽은 기능이다. L467 의 "404 가 ERROR" 의 근본 원인이 바로 이것인데 연결하지 않았다. 또 05 §6.6(`05_개발컨벤션.md:409`) 은 "ERROR — 예외 발생" 이라고 규정하므로, 404 를 ERROR 로 남기는 것이 컨벤션상 **위반이 아니라 컨벤션 자체의 모호함**이라는 점을 밝혀야 판단할 수 있다. — 요구 수정: L265 와 §1.6 L97 ① 을 "logLevel/logMessage 둘 다 호출처 0" 으로 고치고, 05 §6.6 과의 관계를 적는다.

- [M5] §1.3 L68, §3.7 L342, §4.3 L376 — SpEL 필요성을 "미검증, Testcontainers 로 확인할 사항" 으로 남겼다. 이미 있는 DB 로 바로 판정할 수 있다. [R실행] `em.createNativeQuery("select count(*) from sentence where level = :level").setParameter("level", Level.LEVEL_1)` 을 실행하면 `SQLGrammarException: operator does not exist: character varying = smallint` 가 난다. Hibernate 7 은 native 파라미터의 enum 을 ORDINAL(smallint)로 바인딩한다. 즉 **06 §2.3(`06_코드가이드라인.md:107`) "`@Enumerated` 컨버터가 자동 바인딩" 이 틀렸고, 코드의 SpEL 이 맞다.** 리뷰어 검증은 Spring Data `@Query` 경유가 아니라 `EntityManager` 직접 호출이었다는 한계가 있다. 그래도 §3.7 L342 처럼 "코드가 문서에서 벗어남" 으로 분류하면 사람이 코드를 고치는 잘못된 결정을 내린다. — 요구 수정: §3.7 행을 "문서 오류(06 §2.2 예시·§2.3 주의)" 로 바꾸고 §4.3 을 실행 결과로 대체한다. 대안으로 `String` 파라미터(`level.name()`)를 넘기는 방식을 함께 적는다.

- [M6] §3.4 L293 "06 §3.4 가 직접 경고한 함정이 실제 코드에 있다" — 원인 진단이 빠졌다. **06 §3.3 의 "권장" 예시 코드(`06_코드가이드라인.md:165-173`)가 바로 같은 클래스 안의 `loadSentence` 를 `@Transactional` 로 호출하는 self-invocation 패턴**이고, 현재 코드는 그 가이드를 그대로 따랐다. 가이드끼리(§3.3 과 §3.4) 모순인데 §3.7 불일치 목록에도 없다. 결정권자에게는 "코드 수정" 보다 "가이드 수정" 이 먼저라는 정보가 핵심이다. — 요구 수정: §3.7 에 "06 §3.3 권장 예시 = self-invocation(06 §3.4 위반)" 행을 추가하고 §3.4 에서 원인으로 명시한다.

- [M7] §3.7 (L330-346) — 외부 API 계약(03)과 실제 동작의 불일치를 빠뜨렸다.
  - 03 §2.1 Errors(`03_API명세.md:89`) 와 `SentenceApi.java:43` 은 "level·category **누락**" 도 400 `TMI.COMMON.INVALID_REQUEST` 라고 적었다. 실제로는 `TMI.COMMON.MISSING_PARAMETER` 다(`GlobalExceptionHandler.java:50-53`). [R실행] `GET /sentences/random?category=DAILY` → `400 {"code":"TMI.COMMON.MISSING_PARAMETER",...}`. 03 §1.5 에러 코드 목록(`:51-57`)에는 `MISSING_PARAMETER`·`NOT_FOUND_URL`·`METHOD_NOT_SUPPORTED` 가 아예 없다.
  - 03 §2.2 `GET /sentences/meta`(`:92-113`) 와 03 §4.1 / 02 §6.3 `GET /api/v1/health` 는 명세에 있지만 미구현이다. L62 는 `SentenceMetaResponse` 를 "사용처 0" 으로만 적었는데, 명세 위반으로 다뤄야 한다.
  - 404 메시지 예시가 세 곳에서 서로 다르다: `AnswerApi.java:72` `"01HTM3ABCXYZ"`, `SentenceApi.java:64` `"level=LEVEL_5, category=TECH"`, 03 §1.4 / 05 §5.2 / 99 §3 `"Sentence {0} was not found."`. 실제 번들은 `Sentence not found: {0}`(`messages.properties:10`)이다.
  - 요구 수정: 위 항목들을 §3.7 에 추가하고, 계약 위반(누락 시 코드, 미구현 엔드포인트)은 F 요약에 올릴지 검토한다.

- [M8] §3.3(b) L223-233 / §1.7 L110 — basename 만 고친다고 i18n 이 맞아지지 않는다는 점을 놓쳤다. [R실행] `spring.messages.basename=messages/messages` 를 주고 JVM 기본 로케일을 `ko_KR` 로 둔 채 `Accept-Language: en` 과 `fr` 로 요청하면 둘 다 한글 메시지(`영어 문장을 찾을 수 없습니다: ...`)가 나온다. `spring.messages.fallback-to-system-locale` 기본값이 `true` 라서, `messages_en` 이 없으면 시스템 로케일인 ko 로 폴백한다. 03 §1.4 "영문 기본" 계약은 운영 서버 로케일에 따라 깨진다. 실제 Tomcat 에서는 헤더가 없을 때 `request.getLocale()` 이 서버 기본 로케일을 돌려주는 문제도 있다(이 부분은 [정적]). research.md §3.2 가 "slash 경로 형식 — estimated" 라고 적은 부분은 위 실행으로 **동작 확인**됐다. — 요구 수정: F2 에 "basename 수정 + `fallback-to-system-locale=false`(+ 필요 시 `spring.web.locale=en`)가 함께 필요" 를 사실로 기록한다.

- [M9] §5 L395 "커밋하지도 되돌리지도 않았다", blockers.md §1 "작업 종료 시 `git stash pop` 으로 복원" — 현재 저장소 상태와 맞지 않는다. 지금 작업 트리에는 WIP 가 **없고**(`git status` 에 `src/main` 변경 없음) `stash@{0}` 에만 있다. 게다가 그 stash 에는 WIP 외에 `.claude/hooks/check-done.sh`, `.claude/settings.json`, `docs/ai/TASK.md` 가 함께 들어 있다. 이 파일들은 이미 HEAD 에 커밋돼 있어서 [R실행] `git stash show -p stash@{0} | git apply --check` 가 `already exists in working directory` 로 실패한다. 문서에 적힌 복원 절차를 그대로 따르면 실패한다. `git apply --check docs/ai/wip-snapshot.patch` 는 깨끗하게 통과한다. — 요구 수정: §5 를 "WIP 는 현재 stash@{0} 에 있음, 복원은 `git apply docs/ai/wip-snapshot.patch` (stash pop 은 충돌)" 로 고치고 blockers.md 와 맞춘다. 작업 종료 시 복원할 계획이면 그 시점에 다시 확인한다.

- [M10] 존재하지 않는 문서·절을 근거로 인용한다(일관성). `decisions-needed.md`(L3, L376), `conventions-proposal.md §4`(L306), `conventions-proposal.md`(L461)는 아직 없는 파일이고, `research.md §1.3`(L389)은 없는 절이다. F6(L24) "ArchUnit §6" 과 표기 규칙 L10 "§6 ArchUnit 결과 참조" 는 내용이 "(6단계에서 채움)"(L413)뿐인 절을 [실행] 근거로 든다. — 요구 수정: 미작성 문서는 "(작성 예정)" 으로 표기하고, F6 의 검증 열은 §6 이 채워질 때까지 [정적] 으로만 둔다. research 인용은 실제 절 번호로 고친다(MessageSource 는 §3.2 로 맞게 인용돼 있음).

## 경미

- [m1] L4 "브랜치 `main` HEAD `e85dba5`" — 실제로 분석한 브랜치는 `claude/ai-dev-prep`(HEAD `f1cca69`)다. `git diff e85dba5 HEAD -- src/main` 이 0줄이라 내용은 같지만, 표기를 "`claude/ai-dev-prep` @ f1cca69, `src/main` 은 e85dba5 와 동일" 로 바꾼다. §7.1 의 "본 브랜치" 도 같이 고친다.
- [m2] §1.7 L110 "[정적, §4.1] … 가능성 높음" — F2(L20)와 §7.2 L443 에서 이미 [실행] 으로 확정됐다. 낡은 추정 문구를 [실행] 으로 갱신한다.
- [m3] §7.1 L425 "WIP 포함 → 컴파일 실패 예상 [정적]" — [R실행] 사본에 패치를 적용하고 `compileJava` 를 돌리면 `package com.tmi.core.toss.holdings.dto does not exist`(`HoldingsSnapshotEntity.java:3`), `package com.tmi.core.toss.holdings.infra does not exist`(`RestClientConfig.java:4`)로 실패한다. [실행] 으로 올려도 된다. 반대로 §5 L405 "결론: … 실패한다" 는 지금 단정형이면서 등급 표기가 없으니 등급을 맞춘다.
- [m4] §5 L402 "**컨텍스트 기동 실패**" — 굵은 단정이 과장이다. WIP 의 `RestClientConfig` 는 `toss.base-url`/`toss.timeout-ms` 만 읽고 둘 다 값이 있다(`wip-snapshot.patch` 의 `+toss.base-url=https://openapi.tossinvest.com`, `+toss.timeout-ms=3000`). `@ConfigurationProperties` 클래스도 아직 없으므로 지금 WIP 그대로라면 placeholder 때문에 기동이 실패하지는 않는다(컴파일 실패가 먼저). 괄호 안 단서를 본문으로 올린다.
- [m5] §3.3(c) L240-241 — `text/plain` 은 "동일" 이라고만 적고 근거가 없다. [R실행] 결과 `Content-Type: text/plain` → 500, **빈 body**(`Required request body is missing`) → 500 이다. 행을 추가하고 [실행] 으로 표기한다.
- [m6] `SentenceService` 행(L69) — 미사용 import 3개(`SentenceService.java:13-15` `Collections`, `List`, `ThreadLocalRandom`)를 빠뜨렸다. `SentenceException` 의 미사용 import 는 잡았으면서 이것은 놓쳐 일관성이 없다.
- [m7] §3.8 L355 — 콜론 앞 공백 혼재는 클래스 간 문제가 아니라 **같은 클래스 안에서도** 일어난다: `AnswerService.java:42` `"Similarity 호출 : ..."` vs `:45` `"Similarity 응답: ..."`. 06 §3.2 예시는 `"문장 출제: ..."`(공백 없음)이다.
- [m8] L49 `@NotBlank` + `@Size(min = 1)` 중복 — 06 §5.3 예시(`06_코드가이드라인.md:311`)에 똑같이 적혀 있다. 코드가 가이드를 따른 것이므로 "가이드 예시의 중복" 으로 분류한다.
- [m9] 05 §1.3 "domain 은 다른 패키지에 의존하지 않음"(`05_개발컨벤션.md:43`) 적용이 일관되지 않다. WIP 의 `domain → dto` 는 위반으로 지적하면서(L401), HEAD 의 `sentence.domain → common.domain`(`SentenceEntity.java:3`, 다이어그램 L162)은 언급하지 않았다. 허용 예외로 볼지 명시한다.
- [m10] §3.7 누락 항목들:
  - 99 §1(`99_공통.md:16-21`)에 `ELogLevel` 이 없다.
  - 99 §2.3 "`RestClient` 빈 등록" 과 달리 실제로는 빈으로 등록하지 않는다.
  - 06 §1.3 `src/main/resources/init.sql`(`:67`)은 실제로 `docs/public/init.sql` 에 있다.
  - 04 §4.5 "테스트 `create-drop`" 와 04 §5.2 `db/seed.sql` 둘 다 미적용이다.
  - 02 §4.1 "파라미터 검증 (Bean Validation)"(`02_설계.md:126`)과 달리 실제로는 타입 변환만 한다.
- [m11] §1.7 L109 — 기동 시 `HHH90000025: PostgreSQLDialect does not need to be specified explicitly` WARN 이 나온다([R실행] 로그). `application.properties:14` 의 `database-platform` 은 지워도 되는 설정이다. 또 `application.properties` 는 마지막 줄 개행이 없다(WIP 패치가 이것을 고친다).
- [m12] §1 L33 "의존 대상은 `import com.tmi.core.*` 기준" 이라고 했지만 `GlobalExceptionHandler`, `BaseException` 행은 import 가 없는 같은 패키지 타입을 적었다. 반대로 `AnswerController`/`SentenceController` 는 같은 패키지의 `AnswerApi`/`SentenceApi` 구현 관계를 빠뜨렸다. 기준 문장을 "같은 패키지 포함" 으로 고치고 두 행을 보완한다.
- [m13] 다이어그램 L164 — `TI[toss.holdings.infra]` 를 `toss` subgraph 밖에서 선언해 toss 영역 바깥에 렌더된다. subgraph 안으로 옮긴다(파싱 자체는 정상).

## 검증한 주장 (문제 없음 확인)

- 타입 수: Java 34 파일, 중첩 포함 40 타입. 표의 행(1+7+13+6+2+11=40)과 일치하고 중첩 6개(`LevelItem`, `CategoryItem`, `NotFound`, `RequestInvalid`, `Unavailable`, `ErrorResponse.Error`)가 모두 들어 있다. `ELogLevel` 상수 본문(익명 클래스)은 제외해도 무방하다.
- 의존 대상 열: 모든 클래스의 `com.tmi.core.*` import 와 대조해 일치한다(m12 의 기준 문장 문제만 있음).
- Mermaid: `mermaid@11.4.1` 파서로 파싱 성공(`flowchart-v2`)했고, 대조용으로 문법을 일부러 깨뜨린 입력은 실패했다. WIP 가 아닌 실선·굵은 화살표 27개가 실제 import 와 1:1 로 맞고, 누락되거나 근거 없는 간선이 없다.
- F1 / P7 [R실행]: 정상 body 로 `POST /answers` → 500. 로그는 `Could not resolve parameter [0] in ... SimilarityClient.score(SimilarityRequest): No suitable resolver` 로, 문서 인용(L455-456)과 같다. `SimilarityClient.java:10` 라인도 맞다.
- F2 / P2·P3·P4 [R실행]: `messageSource` 빈 타입은 `DelegatingMessageSource`, 응답 `message` 는 코드 문자열이고 `Accept-Language: ko` 를 줘도 같다. 문서와 일치한다.
- F3 / P5 [R실행]: 깨진 JSON → 500 `TMI.COMMON.INTERNAL_ERROR`. ERROR 로그가 한 줄뿐이고 스택트레이스가 없다(`GlobalExceptionHandler.java:77` 의 마지막 인자가 `String`). 404 도 ERROR 레벨로 남는다. 05 §6.5 인용(`05_개발컨벤션.md:399`)도 맞다.
- P1·P6 [R실행] 결과가 문서와 같다. 추가로 확인한 것: 알 수 없는 URL → 404 `NOT_FOUND_URL`, `DELETE /answers` → 405 `METHOD_NOT_SUPPORTED`(정상 처리).
- F4 정정본 [R실행]: 리포지토리 프록시의 `TransactionAttributeSource` 를 직접 조회했다. `findByIdAndDeletedAtIsNull → null`, `findRandomBy → null`, `findById → PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly`. §3.4 표(L299-303)와 일치한다. self-invocation(`AnswerService.java:28,35-36`)과 protected 메서드 설명(L372)도 맞다. 단 커넥션 결론은 C1 참조.
- OSIV 기동 WARN(L444)이 실제 로그에 있다.
- WIP 패치 내용(§5 표): 4개 `src/main` 파일 + `.gitattributes`, `toss.*` 7줄, 존재하지 않는 두 클래스 import 가 패치 원문과 일치하고, 패치는 HEAD 에 깨끗하게 적용된다.
- 라인 인용 대조: `AnswerApi.java:29,86`, `AnswerController.java:17`, `AnswerResponse.java:4,8,30,37`, `SubmitAnswerRequest.java:10-11`, `AnswerService.java:10,11,24,28,32,35-39,47-55`, `GlobalExceptionHandler.java:26-28,34-41,44-48,67,68-71,76,77`, `BaseException.java:4,11,15,16,19,52-56`, `BaseEntity.java:30`, `Level.java:10-14`, `SentenceEntity.java:36`, `SentenceResponse.java:10`, `SentenceException.java:5,8`, `SentenceRepository.java:16-17`, `SentenceService.java:25,30`, `SimilarityClient.java:9-10`, `RestClientConfig.java:3`, `application.properties:8` — 모두 HEAD 와 일치한다(M3 의 두 건만 예외).
- 공개 문서 인용 대조: 02 §2.1 `Difficulty`, 02 §4.1 `pickRandom`·`level=3`, 02 §4.2 `findById`·"등급 변환 Service 책임", 04 §4.2 `schema.sql`, 05 §3.2 vs 06 §1.1 UUID 위치, 05 §4.1·§6.3 `application.yml`, 05 §4.2 POST→201, 06 §3.1 dirty checking, 06 §3.5 / 06 §6.6 로깅 충돌, 06 §4.3 / §3.3 변환 방식 공존, 06 §7.2 Testcontainers, 03 §1.1 `06. 배포·운영` 링크, 99 §1 `SchedulerConfig` 누락 — 모두 원문과 일치한다.
- §3.3(f): `UnknownHttpStatusCodeException` 이 `HttpStatusCodeException` 의 하위가 아니라는 점, `ResourceAccessException` 경로의 이중 ERROR 로깅(`AnswerService.java:53` + 핸들러) — 정적 분석으로 확인했다.
- 사용처 0 주장: `AnswerException`, `SentenceMetaResponse`(+중첩), `ETmiErrorType.BAD_REQUEST`, `SentenceEntity.create`, `HoldingsSnapshotRepository` — grep 으로 확인했다.
- springdoc 2.7.0 이 Boot 4 에서 깨지는지 추가로 확인했다: [R실행] `GET /v3/api-docs` → 200, 정상 생성. 문서가 이 위험을 언급하지 않은 것은 문제가 아니다.
