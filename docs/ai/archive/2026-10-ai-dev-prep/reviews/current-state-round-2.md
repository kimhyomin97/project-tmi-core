# current-state 리뷰 — Round 2

> 대상: `docs/ai/context/current-state.md` @ `99aa1e3` (566줄). 이후 HEAD `7207e31` 에서도 이 파일은 바뀌지 않았다. 라인 번호는 이 버전 기준이다.
> 검증 방법: 전용 worktree(`git worktree add --detach … HEAD`, `99aa1e3`)에서 실행했다. 끝난 뒤 제거했다.
> - **ArchUnit @Disabled 무시 실행**: worktree 에만 `src/test/resources/junit-platform.properties`(`junit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`)를 두고 `bash ./gradlew test --tests 'com.tmi.core.architecture.*'` 를 돌렸다. 결과는 8 tests, 6 failed. 실패 메시지 원문을 `build/test-results` XML 에서 뽑아 §6.2 와 한 줄씩 대조했다.
> - **정상 빌드**: 위 파일을 지운 뒤 `bash ./gradlew build` 를 돌렸다(로컬 PostgreSQL).
> - **Spotless**: `spotlessCheck` 와 `spotlessApply` 를 돌린 뒤 `git diff --stat -- src/main` 로 셌다. 적용 전후 `compileJava` 산출물 40개 class 의 상수 풀(Utf8/String)을 `javap -v` 로 비교했다.
> - **일회성 프로브**(`@SpringBootTest` + MockMvc, 실행 후 삭제): P9·P2 응답 원문, `/sentences/meta`·`/health`, OSIV 활성 커넥션, Spring Data `@Query` 경유 native enum 바인딩, ArchUnit R5 제네릭 반환 픽스처.
> - round-1 지적 23건을 문서 본문과 하나씩 대조했다. docs/ai 의 다른 문서와 docs/public 의 인용 절도 대조했다.
>
> [R실행] 은 이번 리뷰어가 직접 실행해 얻은 결과다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | §6 위반 목록, §7.1 테스트 수, §7.3 Spotless 수치, F9·F10 근거가 모두 실행으로 재현된다. 다만 `[실행]` 으로 표기된 "실제 응답" JSON 의 형태가 실제와 다르다. 지금 바로 확인할 수 있는 항목도 estimated 로 남아 있다(§4.3) |
| 구체성 | 4 | 규칙별 위반 원문, 해제 조건, 라인 번호까지 있어 바로 작업 항목이 된다. 규칙의 탐지 한계(R5·R6)가 적혀 있지 않아, 규칙을 믿고 쓰는 사람이 놓치는 경우가 생긴다 |
| 일관성 | 3 | R2 위반 19건의 절반(9건)은 §2·F6 이 문제로 보지 않는 의존이다. §6.1 해제 조건 열이 현재 feature_list 와 어긋난다. research.md 의 두 절이 이 문서와 반대로 말한다 |
| 판단 가능성 | 4 | round-1 의 치명 결함(OSIV)이 바로잡혀 F4 와 D4 결정을 올바르게 이끈다. 다만 R2 수치를 "현행 위반" 으로 읽게 만드는 표기 문제가 D1 판단에 소음을 준다 |

## 치명 (승인 차단)

없음.

## 주요

- [M1] §6.1 L461 "R2 … 19 의존 (2 클래스)", §6.2 L475·L478, F6 L25 — **R2 위반 19건 중 9건은 `answer → infra.similarity` 이다. 이 의존은 현행 컨벤션에서 허용되고, 이 문서의 §2 도 문제로 보지 않는다.**
  - 근거 [R실행]: R2 실패 메시지 19줄 중 `answer → sentence.*` 는 10건이다. 생성자 파라미터 1, 필드 1, `AnswerResponse.of` 4, `loadSentence` 3, `score` 1. `answer → infra.similarity.*` 는 9건이다. 생성자 파라미터 1, 필드 1, `callSimilarity` 7.
  - 05 §1.3 은 `infra/{외부시스템}/` 배치를 규정한다(§3.6 L345 도 그렇게 인용한다). 그래서 `answer.service → infra.similarity` 는 현행 규칙상 정상이다. §2 다이어그램도 L159-161 `AS --> IS/ISD/ISE` 를 굵은 화살표(문제)로 그리지 않았다. 이것이 위반이 되는 것은 `infra` 를 하나의 "모듈" 로 보고 `similarity` 를 그 내부 패키지로 보는 선택지 B 의 해석(`ArchitectureClasses.isModuleApi`) 때문이다. 해소 작업도 `ARCH-B2`(D1=B 결정 후 등록)다.
  - §6.2 는 이 9건을 §3.6(외부 클라이언트 **배치** 분열)과 §3.3(f)(예외 분류 빈틈)에 연결했다. 그러나 두 절 모두 이 의존 자체를 위반으로 서술하지 않는다.
  - F6 은 "기능 간 직접 의존 (`answer → sentence…`)" 의 [실행] 근거로 R2 를 든다. 그러면 독자는 19건이 모두 `answer → sentence` 라고 읽는다.
  - 요구 수정: (1) §6.1 R2 행을 "19 = `answer→sentence` 10 + `answer→infra.similarity` 9" 로 나눈다. (2) 뒤의 9건에는 "선택지 B 기준에서만 위반(현행 05 §1.3 은 허용)" 이라고 표기한다. (3) §6.2 의 연결 열을 고친다. (4) F6 의 근거는 R1·R3 과 "R2 중 10건" 으로 한정한다.

- [M2] §6.3 L491 "탐지력 (음성 대조) … R5 위반 1건 탐지" — **R5 에 큰 미탐지 구멍이 있는데 적혀 있지 않다.** HEAD `ConventionArchitectureTest.notReturnEntity()` 는 `method.getRawReturnType().isAnnotatedWith(Entity.class)` 만 검사한다. 그래서 `Optional<Entity>`, `List<Entity>`, `Page<Entity>` 반환은 통과한다.
  - 근거 [R실행]: `@Service` 픽스처에 `SentenceEntity direct()`, `Optional<SentenceEntity> optional()`, `List<SentenceEntity> list()` 를 두고 같은 조건으로 실행했다. 결과는 `total=1` 로, `direct` 만 잡혔다.
  - 지금의 위반 목록(1건)은 이 구멍 때문에 틀어지지 않는다. 현재 서비스에 제네릭 엔티티 반환이 없기 때문이다. 그러나 R5 는 conventions §3.1 의 "강제" 수단(`conventions-proposal.md:347`)이고, feature_list `FIX-R5` 의 acceptance 다. 서비스에 `findAll` 류가 생기는 순간 규칙은 통과하는데 컨벤션은 깨진다.
  - R6 도 한계가 적혀 있지 않다 [정적]. 메서드 레벨 `org.springframework…Transactional` 만 본다. 클래스 레벨 `@Transactional` 에서 self-invocation 하는 경우(예: `REQUIRES_NEW` 를 클래스에 건 경우)와 `jakarta.transaction.Transactional` 은 잡지 못한다.
  - 참고: 리뷰 시점의 작업 트리에는 다른 작업자가 만든 **미커밋** 수정(`getReturnType().getAllInvolvedRawTypes()` 로 변경)이 있다. 이 리뷰는 커밋본 기준이다.
  - 요구 수정: §6.3 에 "규칙별 탐지 한계" 행을 추가한다(R5 제네릭, R6 클래스 레벨·jakarta). R5 수정이 커밋되면 음성 대조에 `Optional`/`List` 사례를 넣어 다시 실행하고 결과를 적는다.

## 경미

- [m1] §7.2 P2·P4·P9(L517-522), §3.3(b) L238 — "실제 응답" 열의 JSON 형태가 실제와 다르다. 실제 응답은 `ErrorResponse(Error error)` 래핑이다. [R실행] P9 응답은 `400 {"error":{"code":"TMI.COMMON.MISSING_PARAMETER","message":"TMI.COMMON.MISSING_PARAMETER"}}`, P2 응답은 `404 {"error":{"code":"TMI.SENTENCE.NOT_FOUND",...}}` 이다. 문서는 `{"code":…,"message":…}` 로 평탄화해 적었다. 03 §1.4 의 래핑 계약은 실제로 지켜지고 있다는 사실이 이 표기 때문에 가려진다(D2 판단과 관련). 원문 그대로 적거나 "(error 래핑 생략)" 이라고 명시한다.
- [m2] §4.3 L409 "Spring Data 경유도 … 같을 것으로 본다 (estimated)" — 이제 확정할 수 있다. [R실행] `Repository<SentenceEntity,String>` 에 `@Query(nativeQuery=true)` 로 `where level = :level` 을 두고 실행하면 `InvalidDataAccessResourceUsageException: operator does not exist: character varying = smallint` 가 난다. 같은 조건을 SpEL `:#{#level.name()}` 로 바꾸면 `1` 이 나온다. **JPQL** `where s.level = :level` 도 `1` 이 나온다. 따라서 06 §2.3 의 "`@Enumerated` 컨버터가 자동 바인딩" 은 JPQL 에서는 맞고 **native 에서만** 틀리다. F10 L29 의 "실제로는 smallint 로 바인딩되어 실패" 는 범위를 "native query 에서" 로 한정해야 정확하다(§3.7 L362 는 이미 native 로 한정했다). 대안 목록에 "JPQL + `ORDER BY function('random')`" 도 검토 항목으로 둘 수 있다(미검증).
- [m3] §6.1 L464 R4b "WIP 적용 시 1 예상" — 셈의 단위가 다른 행과 다르다. 다른 행은 ArchUnit 의존 건수를 센다(R2 19, R3 2). WIP `HoldingsSnapshotEntity.create(..., HoldingsApiResponse.Result result, ...)` 는 파라미터 타입 1건에 `result.totalPurchaseAmount().krw()` 같은 접근자 체인 20여 건이 더해진다(`wip-snapshot.patch` 의 +96, +103~+112). WIP 는 컴파일되지 않으므로 [정적] 추정이라고 표기하고, "1 클래스 쌍(의존 수십 건)" 으로 쓴다.
- [m4] §6.1 "해제 조건 (feature_list.example.json)" 열이 현재 feature_list(`7207e31`)와 맞지 않는다.
  - R5 는 "conventions §3.1 적용" 으로 적혀 있다. 그러나 feature_list 에는 `FIX-R5` 가 있다(`feature_list.example.json:113`). 테스트의 `@Disabled` 문구도 "FIX-R5 완료 시 해제" 로 바뀌었다(작업 트리).
  - R2 는 "ARCH-B2, ARCH-B3" 로 적혀 있다. 그러나 현재 R2 해제는 `ARCH-B3` 의 acceptance 에만 있다(`:221`).
  - 이 파일이 갱신된 뒤 feature_list 가 바뀐 것이 원인이다. 동기화가 필요하다.
- [m5] §7.3 L561·L566, decisions-needed D5 "`src/main` 29파일을 건드리므로" — Spotless `target` 은 `src/*/java/**/*.java` 다. 그래서 `spotlessApply` 단독 커밋에는 `CoreApplicationTests.java`(+2/−4)도 들어가 **30 파일**이 된다. 29/34 와 +178/−214 수치 자체는 [R실행] 으로 정확하다. "의미 변경 없음" 은 표본 1개가 근거지만 더 강하게 쓸 수 있다. [R실행] 적용 전후 `src/main` 40개 class 파일의 상수 풀(Utf8/String, text block 포함)이 전부 동일하다. 근거를 이것으로 바꾸면 D5 판단이 쉬워진다.
- [m6] §6.3 — R4·R4b 는 "통과" 만 있고 음성 대조(일부러 위반시켜 잡히는지)가 없다. 통과가 규칙이 동작해서인지, 비어 있어서인지 구분되지 않는다. R2·R5 처럼 픽스처 1건씩을 추가한다. 또 픽스처 코드가 삭제돼 §6.3 의 양성·음성 대조를 아무도 재현할 수 없다. `docs/ai/proposals/examples/` 등에 코드만이라도 보존한다.
- [m7] §7.2 표 — P 번호가 P1…P6, **P9**, P7 순서이고 P8 이 없다. 번호를 다시 매기거나 P8 이 빠진 이유를 적는다.
- [m8] 다른 문서와의 모순 (current-state 쪽 서술이 맞고 research.md 가 낡았다):
  - research.md §3.2 는 여전히 slash basename 을 "estimated, 테스트로 확인 필요" 로 둔다. current-state §3.3(b) L241 은 "실행으로 확인됨" 이라고 한다.
  - research.md §6.3 은 "`archunit-junit5` 가 JUnit Platform 6 에서 동작하는지 … current-state.md §6 에서 실제 실행으로 확인" 이라고 한다. 그러나 §6 L496 은 그 엔진을 **일부러 쓰지 않았다**고 적는다. 즉 호환성은 여전히 미검증이다.
  - 두 곳 중 한쪽을 고친다(research.md 를 고치는 것이 맞다).
- [m9] §3.7 L376 "03 §1.1 `06. 배포·운영` 링크 | 실제 파일명 `07_배포·운영.md`" — 대상 파일의 H1 자체가 `# 06. 배포·운영`(`07_배포·운영.md:3`)이다. 링크만의 문제가 아니라 파일명과 제목이 서로 다르다는 점을 함께 적는다.
- [m10] §5 L437 "작업 종료 시 이 명령으로 복원" — 현재(`7207e31`) 작업 트리에 WIP 가 없고 `stash@{0}` 만 남아 있어 문서 서술과 맞는다. 다만 "작업 종료" 가 어느 시점인지(7단계 커밋 직후인지 등) 정해져 있지 않다. 복원을 잊으면 WIP 가 stash 에만 남는다. 트리거와 확인 명령(`git status --short src/main` 에 4파일)을 적는다.

## 이전 라운드 지적 해소 여부

| 지적 | 상태 | 확인 내용 |
|---|---|---|
| C1 OSIV 커넥션 점유 | **해소** | F4 L23, §1.7 L112, §3.2 L214, §3.4 L306-315 반영. [R실행] 재현: Similarity 콜백 시점 `active=1 tx=false` |
| M1 `common↔infra` 순환 | **해소** | §2 L179, F6. ArchUnit R1 실패 메시지(`common -> infra -> common`)와 일치 |
| M2 `RestClient.Builder` 빈 | **해소** | §1.6 L97, §4.6. 보강 확인: `spring-boot-autoconfigure-4.0.2.jar` 에 `RestClientAutoConfiguration` 없음, `spring-boot-restclient` 미해석. 출처로 든 research §6.1 은 테스트 모듈 절이라 근거가 약하지만 결론은 맞다 |
| M3 WIP 기준 라인 번호 | **해소** | `RestClientConfig.java:26`(connect timeout), `:29`(`builder()`), `:38`(`createClient`) 가 HEAD 와 일치 |
| M4 `logLevel` 호출처 | **해소** | `grep -rnE "\.(logLevel|logMessage)\(" src/main` 0건. 05 §6.6 과의 관계 서술 추가(L275) |
| M5 SpEL 필요성 | **해소** (estimated 1건 잔존 → m2) | §1.3 L71, §3.7 L362, §4.3 |
| M6 06 §3.3 권장 예시 = self-invocation | **해소** | §3.4 L304, §3.7 L363, F10. `06_코드가이드라인.md:165-173` 확인 |
| M7 API 계약 불일치 | **해소** | §3.7 L366-369, F9. [R실행] P9 400 `MISSING_PARAMETER`, `/sentences/meta`·`/api/v1/health` 둘 다 404 `NOT_FOUND_URL`(미구현). `03_API명세.md:89`, 03 §1.5(`:51-57`)에 3개 코드 없음 확인 |
| M8 i18n 폴백 | **해소** | F2, §3.3(b) L240. conventions §2.1 After 와도 일치 |
| M9 stash 상태 | **해소** | §5 L436-437, blockers.md §1 일치. `git stash list` → `stash@{0}` 존재, 작업 트리 `src/main` 변경 없음 |
| M10 없는 문서·절 인용 | **해소** | decisions-needed.md, conventions §2.1·§2.5·§4·§5·§7, research §5(표 5.1~5.3)·§6.1, architecture-proposal 선택지 C 모두 존재 |
| m1 브랜치·HEAD 표기 | 해소 | L4 |
| m2 §1.7 등급 | 해소 | L113 [실행] |
| m3 WIP 컴파일 실패 등급 | 해소 | §5 L447, §7.1 L506 |
| m4 "기동 실패" 과장 | 해소 | §5 L444 |
| m5 text/plain·빈 body | 해소 | §3.3(c) L248-249 |
| m6 `SentenceService` 미사용 import | 해소 | L72 |
| m7 콜론 공백 같은 클래스 혼재 | 해소 | §3.8 L386 |
| m8 `@NotBlank`+`@Size` 가이드 예시 | 해소 | L52, §3.7 L364 |
| m9 `domain → common.domain` | 해소 | §2 L181 |
| m10 §3.7 누락 5건 | 해소 | L365, L370-373 |
| m11 Dialect WARN, 개행 | 해소 | L112 |
| m12 의존 대상 기준 문장 | 해소 | L36, L49, L61 |
| m13 `TI` subgraph | 해소 | L147 |

## 검증한 주장 (문제 없음 확인)

- **§6 ArchUnit 위반 목록 전체** [R실행, @Disabled 무시]: 8 tests 중 R4·R4b 통과, 6개 실패. 규칙별 건수가 문서와 정확히 같다. R1 사이클 1건, R2 19건·2클래스(`AnswerService`, `AnswerResponse`), R3 2건·1메서드, R5 1, R6 1, R7 1. §6.2 의 각 행도 실패 메시지 원문과 1:1 로 맞는다.
  - R1 의 infra 쪽: `RequestInvalid`·`Unavailable extends BaseException`, `ETmiErrorType.SIMILARITY_*` 참조.
  - R2 의 `callSimilarity` 7건 구성: `SimilarityRequest` 생성 1, `score` 1, `score()` ×2, 예외 생성 ×3.
  - R3 의 위치: `RestClientConfig.java:38`.
  - R6 의 위치: `AnswerService.java:28`.
  - R7 의 메시지: `parameter[0] SimilarityRequest has no binding annotation`.
- §6.3 "R2 1차 21건 → `common` 출발 제외 후 19건": R3 가 보고하는 2건(`RestClientConfig → SimilarityClient`)을 더하면 정확히 21이다. 논리상 일치한다.
- **§7.1** [R실행]: `bash ./gradlew build` → `BUILD SUCCESSFUL`. 테스트 9개 = `CoreApplicationTests` 1 통과 + `LayerArchitectureTest` 2 통과 + `Module`·`Convention` 각 3 skipped. 문서와 일치한다.
- **§7.3 Spotless** [R실행]: `spotlessCheck` 실패("Violations also present in 28 other files", 표시 1 + 28 = 29). `spotlessApply` 후 `git diff --stat -- src/main` → `29 files changed, 178 insertions(+), 214 deletions(-)`. Java 파일 34개 중 무변경 5개는 `Grade`, `AnswerException`, `BaseException`, `SimilarityClient`, `SimilarityException` 이다. 이 작업이 추가한 ArchUnit 4파일은 diff 0, `CoreApplicationTests` 는 탭 들여쓰기 위반이다. 모두 문서와 일치한다. 적용본도 컴파일되고, 상수 풀이 동일하다(m5).
- **F9 근거**: `GlobalExceptionHandler.java:50-53` 이 `MISSING_PARAMETER` 를 쓴다. `03_API명세.md:89` 는 "누락·잘못된 값" 을 `INVALID_REQUEST` 로 적었다. 03 §1.5 에 `MISSING_PARAMETER`·`NOT_FOUND_URL`·`METHOD_NOT_SUPPORTED` 가 없다. `/sentences/meta`(03 §2.2 `:92`)와 `/api/v1/health`(03 §4.1 `:184`, 02 §6.3, 07 §4)는 미구현이다 [R실행 404]. actuator 의존성도 없다.
- **F10 근거**: `06_코드가이드라인.md:107`(§2.3 "자동 바인딩"), `:165-173`(§3.3 권장 예시의 같은 클래스 `@Transactional loadSentence` 호출). native enum 바인딩 실패는 Spring Data 경유로도 [R실행] 재현했다(m2 의 범위 한정만 필요).
- §3.4 OSIV 프로브 표 [R실행]: 기본 설정에서 콜백 시점 활성 커넥션 1, 트랜잭션 비활성. `@MockitoBean` 으로 F1 을 우회한 `POST /answers` 는 200, `grade=EXCELLENT` 다.
- §4.5 "3개 클래스·8개 규칙", ai-workflow.md L18 "8개 중 6개 `@Disabled`" 가 일치한다.
- §6 L496 "archunit core 1.5.1 만 사용": `build.gradle` 은 `com.tngtech.archunit:archunit:1.5.1` 이고 `archunit-junit5` 는 없다.
