# conventions-proposal 리뷰 — Round 1

> 대상: `docs/ai/conventions-proposal.md` @ `b39d3e2` (문서 내용은 `99aa1e3` 과 동일)
> 검증 방법: 전용 worktree(`scratchpad/wt-conventions-proposal`, `--detach HEAD`)에서 `bash ./gradlew test --offline` 로 실행 검증. `/home/user/repo` 는 이 파일 외 수정하지 않음. worktree 는 검증 후 제거.
> - (a) 수정 없는 HEAD 에 §6.3 테스트 3종을 문서 그대로 추가해서 실행
> - (b) worktree 의 `src/main` 에 §2.1 설정 2줄, §2.5 `@RequestBody`, §2.2 After 핸들러를 **문서 코드 그대로** 넣고(main 소스셋이라 Lombok 사용 가능) `@WebMvcTest({SentenceController, AnswerController})` 시나리오 12건을 `Locale.setDefault(KOREA)` + `OutputCaptureExtension` 으로 실행
> - (c) §2.3 After 를 문서에 적힌 그대로 `BaseException` 에 적용해서 (b) 를 다시 실행
> - (d) `fallback-to-system-locale=false` 를 뺀 상태로 다시 실행 (§2.1 두 번째 줄이 필요한지 확인)
> - (e) `javap` 로 Spring Framework 7.0.3 의 `ResponseEntityExceptionHandler.handleExceptionInternal` 과 `UnknownHttpStatusCodeException` 계층 확인
> - Before 인용은 `src/main` 의 실제 파일·라인과 하나씩 대조함

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | 검증했다는 주장(§0, §2.2 표, §6.3 실패 메시지)을 재현해 보니 전부 맞았다. Before 인용도 전부 일치. 다만 검증하지 않은 §2.3 After 가 그대로 쓰면 깨지고, 라인 번호 오류가 1건 있다 |
| 구체성 | 3 | 대부분 그대로 붙여 넣어 쓸 수 있는 수준이다. 그러나 §2.3 After 를 문자 그대로 적용하면 모든 404 가 NPE 로 깨지고(C1), §4.3 Toss 예시는 설계 문서의 에러 정책과 다르다 |
| 일관성 | 3 | `[B 전제]` 표시가 빠진 B 전용 코드가 있다(§1.1·§4.1). §8 수정 목록에서 05/06/99 의 영향 절이 여럿 빠졌다. 선택지 A 에서 R5 가 어떻게 되는지 적지 않았다 |
| 판단 가능성 | 4 | 규칙마다 Before/After, 05/06 과의 관계, D 번호가 있어 채택/거부를 바로 정할 수 있다. 순서 의존(§2.2 → §2.3)이 드러나지 않는 점만 아쉽다 |

## 치명 (승인 차단)

- **[C1] §2.3 `BaseException` After — 그대로 적용하면 `SentenceException.NotFound` 처리가 NPE 로 깨진다**
  - 문제: After 는 필드를 `private ELogLevel logLevel;   // 생성자에서 결정` 으로 바꾸면서, 4개 생성자 중 `(HttpStatus, IErrorType, Throwable, Object...)` **하나에만** `logLevel` 대입을 보여 준다. 그런데 `SentenceException.NotFound` 는 `(HttpStatus, IErrorType, Object...)` 생성자를 쓴다(`sentence/exception/SentenceException.java:15`, `BaseException.java:32-37`). 그래서 `logLevel == null` 이 되고, 핸들러의 `level.log(...)` 에서 NPE 가 난다.
  - 근거 [실행]: 문서 After 를 문자 그대로 적용하고 (b) 시나리오를 돌린 결과 `java.lang.NullPointerException: Cannot invoke "...ELogLevel.log(...)" because "level" is null` → `Failure in @ExceptionHandler GlobalExceptionHandler#handleBase` → `ServletException`. 404 가 응답 본문 없이 실패한다. 99 §2.6 도 생성자를 "4 종" 으로 명시하고 있다.
  - 요구 수정: 4개 생성자를 모두 하나의 private 초기화 경로로 모으는 코드를 After 에 그대로 보인다. 예: 모든 생성자가 `this(httpStatus, errorType, null, args)` 로 위임하거나, `logLevel` 을 `resolveDefault(httpStatus)` 로 계산한다. 그리고 "검증하지 못한 것"(§0) 목록에 §2.3 을 넣거나, 실제로 검증한다.

## 주요

- **[M1] §2.2 `handleExceptionInternal` 오버라이드가 상위 구현의 두 가지 동작을 말없이 버린다**
  - 근거 [javap, spring-webmvc 7.0.3]: 상위 `handleExceptionInternal` 은 (1) `ServletWebRequest` 의 응답이 이미 commit 됐으면 WARN 로그를 남기고 `null` 을 반환하고, (2) status 가 500 이면 `request.setAttribute("jakarta.servlet.error.exception", ex, SCOPE_REQUEST)` 를 설정한다. After 는 `super` 를 호출하지 않아 두 동작이 모두 사라진다. 그래서 commit 된 응답(스트리밍 중 오류 등)에 다시 쓰려다 2차 예외가 날 수 있고, 관측 도구가 500 의 원인 예외를 잃는다.
  - 요구 수정: 오버라이드 첫머리에 commit 검사(`if (request instanceof ServletWebRequest swr && swr.getResponse() != null && swr.getResponse().isCommitted()) return null;`)와 500 속성 설정을 넣거나, 의도적으로 뺀 것이라면 이유를 적는다. 이 차이는 §2.2 "검증 결과" 표의 7개 시나리오에서는 드러나지 않는다.

- **[M2] §4.3 Toss 스냅샷 예시가 그 근거로 든 설계 문서와 다르다**
  - 근거: `docs/public/toss/01_보유자산스냅샷.md` §6 은 실패 유형별 처리를 나눈다. `/holdings` 4xx → **WARN**, 5xx·타임아웃 → **재시도 1회**, DB `UNIQUE` 위반 → **INFO 후 무시**. 예시는 `catch (RuntimeException e) { log.error(...) }` 하나로 모두 ERROR 처리한다. 또 설계 §3 의 진입점 이름은 `HoldingsSnapshotService.collect()` 인데 예시는 `HoldingsSnapshotJob` 이고, `accountSeq`·`snapshotAt` 의 출처(설계 §3 (2) `toss.default-account-seq`)가 없다.
  - 요구 수정: 예시를 설계 §6 표와 맞추거나, "트랜잭션 경계만 보이는 축약 예시이며 에러 정책은 설계 §6 을 따른다" 고 명시한다. 진입점 이름을 바꾸려는 것이라면 그 결정을 D9 에 연결한다.

- **[M3] `[B 전제]` 표시가 빠진 B 전용 After 코드가 있다 (서두 §0 의 "선택지에 따라 달라지는 규칙은 `[B 전제]`" 위반)**
  - §1.1 After `AnswerResponse.from(SentenceDetail sentence, ...)`: `SentenceDetail` 은 B 에만 있다. 선택지 A 의 예시(`examples/.../option-a-layered/AnswerResponse.java:17`)는 `from(SentenceEntity, ...)` 이다.
  - §4.1 After `sentenceReader.getActive(...)` / `SentenceReader`: 이것도 B 전용이다. A 에서는 `SentenceService.getActive` 이다(`option-a-layered/SentenceService.java:101-105`, architecture-proposal.md:48).
  - 요구 수정: 두 곳에 `[B 전제]` 를 붙이고, A 일 때의 형태를 한 줄씩 적는다. 규칙 자체(from/of 구분, 다른 빈 경유)는 구조와 무관하므로 After 코드만 B 전용이라는 점을 분명히 한다.

- **[M4] §3.1 의 "선택지 A 면 서비스 간 엔티티 허용" 이 ArchUnit R5 와 충돌하는데, 이 점을 적지 않았다**
  - 근거: R5(`ConventionArchitectureTest.java:58-66`)는 `@Service` 의 **모든** public 메서드에 대해 `@Entity` 반환을 금지한다. 선택지 A 의 `SentenceService.getActive` 는 `SentenceEntity` 를 반환하므로 R5 에 걸린다. 한편 decisions-needed D4 는 "R5 는 `getRandom` DTO 반환 시 해제" 라고 적어, A 를 택해도 R5 를 켤 수 있는 것처럼 읽힌다.
  - 요구 수정: "A 를 택하면 R5 를 '컨트롤러가 호출하는 메서드만' 으로 좁히거나 R5 를 쓰지 않는다" 를 §3.1 에 적고, D4 문구와 맞춘다.

- **[M5] §8 "05/06 문서 수정 목록" 이 본문의 변경을 다 담지 못한다. 승인 후 DOC-1 이 이 표만 보고 작업하면 문서가 어긋난 채 남는다**
  - 빠진 항목(모두 확인함):
    - 99 §2.7 `GlobalExceptionHandler` 의 "처리 대상" 목록 (§2.2 로 `ResponseEntityExceptionHandler` 상속 구조가 됨)
    - 99 §2.6 의 "기본 logLevel" 서술과 생성자 동작 (§2.3)
    - 06 §2.2 코드 예시와 06 §7.4 테스트 예시의 `findRandomBy` → `findRandomActiveByLevelAndCategory` (§1.2)
    - 06 §3.2 예시 `SentenceService.getRandom` 의 반환 타입 `SentenceEntity` (§3.1 과 충돌)
    - 06 §2.3 에 "리포지토리 쿼리 메서드는 트랜잭션 없음" 추가 (§4.2 본문에는 적었지만 §8 의 06 §2.2·§2.3 행은 SpEL 만 언급)
    - 05 §6.5 예시 `log.error("유사도 서버 호출 실패: sentenceId={}", sentenceId, e)` (§5.1 의 "서비스는 로그 없이 throw" 와 정면 충돌)
    - 05 §5.7 마지막 문단 "기본 동작이 `HttpClientErrorException`/`HttpServerErrorException`/`ResourceAccessException` 던지므로 위 try/catch 그대로 적용" (§2.4 와 충돌)
    - 06 §7.1 의 "Phase 5 WireMock" 과 §6.2 의 `MockRestServiceServer` 선택 관계
  - 반대로 §8 에만 있고 본문 근거가 없는 항목도 있다: 06 §5.3 `@Size(min = 1)` 제거, 99 §2.3 `RestClient` 빈 표현 정정.
  - 요구 수정: §8 을 본문 절과 1:1 로 대조해서 다시 작성한다. 본문에 근거가 없는 행은 근거 절을 추가하거나 삭제한다.

## 경미

- [m1] §6.1 T4 "`build.gradle:33,35` 가 main 에만 선언": 실제 Lombok 선언은 **32**(`compileOnly`)와 **34**(`annotationProcessor`)다. 33 은 `postgresql`, 35 는 `data-jpa-test` 이다.
- [m2] §0 "테스트 소스셋에 같은 코드를 다른 이름으로 두고": After 코드는 `@Slf4j`·`@RequiredArgsConstructor` 를 쓰는데 테스트 소스셋에는 Lombok 이 없으니(T4) "같은 코드" 일 수 없다. 어떻게 바꿨는지 한 줄 적는다. 이번 리뷰에서는 main 소스셋에 원문 그대로 넣어 컴파일·동작을 확인했으므로 결론은 맞다.
- [m3] §2.2 순서 의존이 드러나지 않는다: §2.3 없이 §2.2 만 적용하면 `SentenceException.NotFound`(404)는 여전히 `e.getLogLevel() == ERROR` 라서 **ERROR + 스택트레이스**로 남는다([실행] 로그 확인). 그러면 규칙 2("4xx 는 INFO")를 만족하지 못한다. "§2.2 규칙 2 는 §2.3 적용 후 성립" 을 명시한다.
- [m4] §2.2 와 §5.1 "HTTP 예외 로그는 `GlobalExceptionHandler` **만**": 405 에서는 상위 클래스의 `pageNotFoundLogger` 가 `WARN o.s.web.servlet.PageNotFound : Request method 'DELETE' is not supported` 를 따로 남긴다([실행]). 결국 로그가 이중으로 남는다. 허용할 예외로 적거나 `logging.level.org.springframework.web.servlet.PageNotFound=ERROR` 를 제안한다.
- [m5] §2.4 근거 "정적 확인 (estimated)": `javap` 로 `UnknownHttpStatusCodeException extends RestClientResponseException` 을 확인할 수 있다(spring-web 7.0.3). 근거를 [실행/바이트코드] 로 올려도 된다.
- [m6] §6.3 `GradeTest` 에 `@DisplayName` 이 없어 §1.4 에서 유지한다고 한 "한국어 `@DisplayName`" 규칙과 다르다. 경계값도 GOOD/FAIR 경계 아래쪽(`0.6999 → FAIR`)이 빠져 있다.
- [m7] §6.2 는 웹 슬라이스가 F3 을 잡는다고 했지만, F3(깨진 JSON → 400) 테스트 코드는 없고 "AnswerController 슬라이스에 둔다" 는 문장 하나뿐이다. ai-workflow `TEST-1` 은 `AnswerControllerTest` 를 산출물로 요구한다. 예시를 하나 추가한다.
- [m8] §5.3 After 의 `# application.properties — 기본 off` 주석만으로는 기존 `show-sql=true`·`format_sql=true` 두 줄을 **삭제**하라는 뜻인지 분명하지 않다. 또 07 §3 의 `SPRING_PROFILES_ACTIVE` 기본값 `local` 은 환경 변수 표의 값일 뿐이다. 앱에는 기본 프로필 설정이 없어서, 미설정 시 SQL 로그가 아예 꺼진다는 점도 적는다.
- [m9] examples 와 본문의 사소한 불일치: `option-a-layered/AnswerService.java:58` 의 로그 키 `userAnswerLength` (§1.3 After 는 `answerLength`), `option-a-layered/SentenceService.java:97` 의 `findRandomBy` (§1.2 개명 전 이름).
- [m10] §1.4 "05 §5.1·99 §2 에 문서화된 팀 결정": 05 §5.1·99 §2 는 이름을 사용할 뿐, 접두사 방식을 택한 이유나 결정을 적지 않았다. "기존 문서에 이 이름으로 정착" 정도로 표현을 낮춘다.
- [m11] (AS-IS 와 같은 동작이라 참고만) `Accept: application/xml` 요청에서 `BaseException` 이 나면 핸들러의 JSON 응답을 쓸 수 없어 `HttpMediaTypeNotAcceptableException` → `ServletException` 이 된다([실행]). 개선 대상으로 남길지 결정하면 된다.

## 이전 라운드 지적 해소 여부 (해당 시)

- 이 문서의 이전 라운드는 없다(Round 1).
- 이 문서가 인용한 current-state round-1 **M8**("basename 만 주면 서버 로케일 ko 에서 `Accept-Language: en` 에도 한글"): [실행 (d)] 으로 재현됐다. `fallback-to-system-locale=false` 를 빼고 `Locale.setDefault(KOREA)` 로 돌리면 en/헤더 없음/ko 세 경우 모두 한글이 나온다. 두 줄을 모두 넣으면 en·헤더 없음은 영문, ko 는 한글이다. 따라서 §2.1 의 "두 줄 모두 필요" 가 맞다.

## 검증한 주장 (문제 없음 확인)

- Before 인용 일치(파일:라인):
  - `AnswerResponse.java:30` (`of`), `:37` (`Grade.fromScore`)
  - `SentenceRepository.java:22`
  - `AnswerService.java:28,35-36,42,45,47-55,53`
  - `GlobalExceptionHandler.java:31-78`, `:77`
  - `BaseException.java:15-16,46-56`
  - `SimilarityClient.java:9-10`
  - `SentenceService.java:25`, `SentenceController.java:26-27`
  - `application.properties:16-17`
  - `RestClientConfig.java:18-22`
- §1.4 "7파일·33회": `grep` 재실행 결과 33회, 7파일로 일치.
- §2.2 "`BAD_REQUEST` 사용처 0": `ETmiErrorType.java:16` 의 선언 외에 사용처 없음. §2.3 "`logLevel(...)`/`logMessage(...)` 호출처 0, `logMessage` 읽는 곳 0": 일치.
- §2.2 After 는 Spring Boot 4.0.2 / Spring Framework 7.0.3 / Java 21 에서 **수정 없이 컴파일**된다.
  - `handleExceptionInternal(Exception, @Nullable Object, HttpHeaders, HttpStatusCode, WebRequest)` 시그니처 일치
  - `org.jspecify.annotations.Nullable` 은 클래스패스에 있음
  - 패턴 매칭 `switch` 의 case 순서에 지배(dominance) 오류 없음
  - 필요 import 목록 정확
- §2.2 검증 결과 표 7행을 [실행]으로 전부 재현했다.
  - 404 en `"Sentence not found: level=LEVEL_5, category=TECH"` / ko `"영어 문장을 찾을 수 없습니다: ..."`
  - 깨진 JSON → 400 `TMI.COMMON.BAD_REQUEST`
  - `text/plain` → 415 `BAD_REQUEST`
  - 파라미터 누락 → 400 `MISSING_PARAMETER` `"Required parameter category is missing."`
  - 잘못된 enum → 400 `INVALID_REQUEST`
  - `DELETE /answers` → 405 + `Allow: POST`
  - body 검증 실패 → 400 `"sentenceId: must not be blank"`
- 표 밖 추가 시나리오:
  - 잘못된 URL → 404 `NOT_FOUND_URL`
  - `SimilarityException.Unavailable` → 503, ERROR 로그에 원인(`ResourceAccessException`)까지 포함된 스택트레이스가 남음 (규칙 2·§5.1 After 의 "cause 보존 → 1회 기록" 성립)
- §6.3 의 HEAD 실행 결과:
  - `SimilarityClientTest`: `IllegalStateException: Could not resolve parameter [0] in ... SimilarityClient.score(...): No suitable resolver` 로 실패 (문서와 일치)
  - `SentenceControllerTest`: `expected:<Sentence not found: level=LEVEL_5, category=TECH> but was:<TMI.SENTENCE.NOT_FOUND>` 로 실패 (문서와 일치)
  - `GradeTest`: 6/6 통과
  - 수정(§2.1 + `@RequestBody`) 후에는 세 테스트 모두 통과
- §6.1 T5:
  - `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` 가 `spring-boot-webmvc-test-4.0.2.jar` 에 있음
  - `spring-boot-test-4.0.2.jar` 에 `MockBean` 없음
  - `@MockitoBean`(`org.springframework.test.context.bean.override.mockito`)으로 컴파일·실행됨
- 05/06 관계 서술 중 다음은 정확하다.
  - 06 §3.3 "권장" 예시가 self-invocation 이다
  - 06 §3.5 "외부 호출 실패 ERROR" 행이 06 §6.6 "throw 직전 로그 금지" 와 충돌한다
  - 06 §2.1 `AndDeletedAtIsNull`, 06 §2.3 "자동 바인딩" 문구, 06 §5.1 `from(Entity)`, 06 §4.3(컨트롤러 변환)과 06 §3.3(서비스 변환)이 혼재한다
  - 05 §5.4 "추가 `@RestControllerAdvice` 금지", 05 §5.5 영문 기본, 05 §6.6 `ERROR` 정의, 05 §6.7 운영 DEBUG 금지
  - 03 §2.1 은 누락도 `INVALID_REQUEST` 로 적었다(D6)
- §4.3 이 인용한 WIP 클래스명(`TossHoldingsClient`, `HoldingsApiResponse`)은 `docs/ai/wip-snapshot.patch` 에 실제로 있다. 설계 §3 의 "save 만 트랜잭션" 인용도 정확하다(에러 정책 불일치는 M2).
- D 번호(D2~D8, D10)는 모두 decisions-needed.md 에 있다. ArchUnit R5·R6·R7 은 `ConventionArchitectureTest` 에 구현되어 `@Disabled` 상태이며, 해제 조건이 본문 §3.1·§4.1·§2.5 와 맞다.
- ai-workflow.md §3 체크리스트에 §1.3 로그 형식(#8), §5.4 민감 정보(#8), §5.1 throw 직전 로그 금지(#7)가 실제로 들어 있다.
