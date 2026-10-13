# conventions-proposal 리뷰 — Round 2

> 대상: `docs/ai/conventions-proposal.md` @ `3e136c6`. 이 커밋의 문서는 `18eea46` 과 같고, Round 1 반영은 `38a37e8`, `18eea46` 은 §2.4·§6.2 의 timeout 서술만 바꿨다.
> 검증 방법: 전용 worktree(`scratchpad/wt-conventions-proposal`, `--detach 3e136c6`)에서 `bash ./gradlew test --no-daemon --offline` 로 실행했다. 검증이 끝난 뒤 worktree 는 제거했다. `/home/user/repo` 에서는 이 파일만 만들었다.
> - (a) worktree `src/main` 에 문서 After 를 **글자 그대로** 넣었다. §2.3 `BaseException` 은 4개 생성자 위임과 `logMessage` 삭제, §2.2 `GlobalExceptionHandler` 는 commit 검사와 500 속성 포함, §2.1 설정 2줄, §2.5 `@RequestBody` 를 적용했다.
> - (b) `@WebMvcTest({SentenceController, AnswerController, ProbeController})` + `OutputCaptureExtension` + `Locale.setDefault(KOREA)` 로 16개 요청을 실행했다. `ProbeController` 는 테스트 전용이며 500 표준 예외, commit 된 응답, `AsyncRequestTimeoutException` 을 만든다.
> - (c) JDK `HttpServer` 에 지연 응답, 본문 중간 정지, 연결 거부, 상태 499/599/600/299 를 두고 `SimpleClientHttpRequestFactory`(readTimeout 300ms)를 연결했다. `SimilarityClient`(POST)와 Toss 형태의 `@GetExchange` 클라이언트(GET)가 각각 어떤 예외를 받는지 기록했다. AS-IS `AnswerService` 를 실제 클라이언트에 연결해 timeout 이 어떤 예외로 핸들러까지 오는지도 확인했다. `MockRestServiceServer.withException(SocketTimeoutException)` 도 비교했다.
> - (d) §6.3 테스트 4종을 문서 그대로 추가했다. After 상태에서 실행한 뒤 `git stash -- src/main` 으로 HEAD 상태로 되돌려 다시 실행했다.
> - (e) 그 상태에서 전체 `test`(CoreApplicationTests 와 ArchUnit 포함)를 실행했다. DB 는 조회만 했다(`ddl-auto=validate`).
> - 대조한 문서: `docs/public/{02,03,05,06,99}`, `toss/00·01`, `docs/ai/{architecture-proposal, decisions-needed, current-state, ai-workflow, feature_list.example.json, wip-snapshot.patch, examples/}`

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | §0·§2.2 표·§6.3 의 실행 주장은 모두 재현됐다. 다만 §2.4 의 예외 분류 근거 두 가지(read timeout 은 `RestClientException`, `RestClientResponseException` 이 비표준 상태 코드를 잡는다)는 조건부이거나 실제 동작과 다르다 |
| 구체성 | 4 | §2.2·§2.3 After 를 글자 그대로 붙여 넣어도 컴파일되고 의도대로 동작한다(C1 해소). §4.3 Toss 예시는 토큰 실패를 설계와 다른 분기로 보낸다 |
| 일관성 | 3 | §2.4 의 A′ 서술이 architecture-proposal 과 반대다. §8 은 "1:1" 이라고 적었지만 06 §4.4, 02 §5.2, 03 §3.1 이 빠졌다. §2.4 주석은 decisions-needed D13 과 충돌한다 |
| 판단 가능성 | 4 | 규칙마다 Before/After, 05/06 관계, D 번호, 적용 순서(§2.2→§2.3, §3.1→§3.4)가 있다. 그대로 채택하거나 거부할 수 있다 |

## 치명 (승인 차단)

없음. Round 1 C1 은 실행으로 해소를 확인했다(아래 해소 여부 표).

## 주요

- **[M1] §2.4 "변환 위치" 의 A′ 서술이 architecture-proposal 및 examples 와 반대다 (`conventions-proposal.md:316`)**
  - 문제: 문서는 "`[B 전제]` 외부 시스템 모듈의 공개 클래스(`SimilarityScorer`) 하나. **선택지 A 이면 서비스의 private 메서드에 둔다**" 라고 적었다.
  - 근거:
    - `architecture-proposal.md:46` 은 A′ 가 바꾸는 규칙 (3) 으로 "외부 예외 변환은 `infra/{시스템}/` 의 클래스 1개 (`SimilarityScorer`)" 를 든다. `:41` 은 세 선택지 모두 "외부 예외 변환 클래스 1개" 컨벤션을 적용한다고 적는다. `:54` 의 트리도 `infra/similarity/{…, SimilarityScorer, …}` 다.
    - A′ 예시 `examples/answer-scoring/option-a-layered/infra-similarity/SimilarityScorer.java:4,19` 가 실제로 `com.tmi.core.infra.similarity.SimilarityScorer` 다.
    - 이 문서 서두(`:6`)는 `[B 전제]` 를 "규칙은 같고 클래스 위치·이름만 다른 경우" 로 정의하고, 이름도 "선택지 A" 가 아니라 **A′** 로 쓴다.
  - 요구 수정: "A′ 이면 `infra.similarity.SimilarityScorer` — 규칙(변환 클래스 1개)은 같다" 로 고친다. 문서 전체에서 "선택지 A" 표기를 A′ 로 통일한다.

- **[M2] §2.4 의 timeout·상태 코드 서술은 POST 와 기본 에러 핸들러에서만 맞는다. 일반 규칙으로 쓰면 틀리고 D13 과도 충돌한다 (`:298, :304, :308, :310`)**
  - 근거 [실행 (c), `SimpleClientHttpRequestFactory`, Spring Framework 7.0.3]:

    | 상황 | 받은 예외 |
    |---|---|
    | POST(`SimilarityClient`) 응답 헤더 전 read timeout | `RestClientException("Error while extracting response …")` ← `SocketTimeoutException` ✓ (문서와 일치, AS-IS `AnswerService` 에서도 이 예외가 catch 를 빠져나감) |
    | **GET**(Toss 형태 `@GetExchange`) 응답 헤더 전 read timeout | **`ResourceAccessException`** ← `SocketTimeoutException` (`I/O error on GET request …: Read timed out`) |
    | POST·GET 모두 본문 수신 중 정지 | `RestClientException` ← `HttpMessageNotReadableException` |
    | 연결 거부 | `ResourceAccessException` ← `ConnectException` (POST·GET) |
    | 상태 499 / 599 | `HttpClientErrorException` / `HttpServerErrorException` |
    | 상태 600 / 299 | **예외 없음, 정상 반환** `SimilarityResponse[score=0.4]` |

  - 문제 1: `:308` "`ResourceAccessException` — 연결 거부 등 연결 단계 I/O 오류", `:310` "`RestClientException` — read timeout (SimpleClientHttpRequestFactory)" 는 POST 에서만 맞다. 본문이 없는 GET 은 `execute()` 안에서 응답 코드를 읽으므로 read timeout 이 `ResourceAccessException` 이 된다. 이 규칙을 05 §5.7 에 옮기면(§8 첫 행) Toss 처럼 GET 을 쓰는 클라이언트 작성자가 잘못 배운다. §4.3 Toss 예시는 GET 이라서 `ResourceAccessException` 으로 timeout 을 잡는 것이 우연히 맞다. 그러나 본문 정지형 timeout(`RestClientException`)은 재시도 대상에서 빠진다.
  - 문제 2: `:304` "`RestClientResponseException` — 4xx/5xx + **비표준 상태 코드**" 와 `:292` "`UnknownHttpStatusCodeException` 누락" 은 기본 `DefaultResponseErrorHandler` 에서 관찰되지 않는다. 499·599 는 일반 Client/ServerError 예외로 오고 600 은 성공으로 처리된다. `decisions-needed.md` D13 도 "600 이상 → 기본 `ResponseErrorHandler` 가 성공으로 처리, 보류" 라고 적었다. 따라서 이 문서의 주석은 D13 과 모순된다. 상위 타입으로 catch 하는 After 코드 자체는 무해하다. 하지만 규칙의 근거로 든 "누락" 은 실제 결함이 아니다.
  - 요구 수정: (1) `:308`·`:310` 주석을 "read timeout 은 요청 방식에 따라 `ResourceAccessException`(본문 없는 요청) 또는 `RestClientException`(본문 있는 요청·응답 본문 수신 중) — 둘 다 `Unavailable`" 로 바꾼다. (2) "비표준 상태 코드" 와 Before 의 "`UnknownHttpStatusCodeException` 누락" 을 "커스텀 `ResponseErrorHandler` 를 쓸 때 대비" 로 낮추고 D13 을 링크한다. (3) §4.3 재시도 catch 에 `RestClientException`(본문 수신 timeout)을 넣을지 적는다.

- **[M3] §4.3 Toss 예시는 "설계 §3·§6 을 따른다" 고 하지만 토큰 단계 실패를 설계와 다르게 처리한다 (`:466-491`)**
  - 근거: `docs/public/toss/01_보유자산스냅샷.md:111-112` 는 `/oauth2/token` 4xx 를 **ERROR** 후 skip, 5xx·네트워크 오류를 재시도 1회로 정한다. 예시는 `tokenProvider.bearer()` 를 `fetchWithOneRetry` 안(`:486,488`)에서 호출한다. 그래서 토큰 발급 4xx(`HttpClientErrorException`)가 `/holdings` 4xx 용 catch(`:475-476`)에 걸려 **WARN "Toss 보유자산 조회 거부"** 로 남는다. 인증 실패가 보유자산 권한 문제로 잘못 기록된다.
  - 이름도 다르다. 설계 `:32` 은 `TossTokenProvider.getAccessToken()` 인데, 예시는 `bearer()` 를 쓰면서 "설계 §3·00_인증.md §3 에서 가정" 이라고 적었다(`:494`). 00_인증 §3 에는 `bearer()` 가 없다.
  - 나머지는 설계와 맞다. `/holdings` 4xx WARN, 5xx·timeout 재시도 1회(GET 헤더 timeout 은 [실행]으로 `ResourceAccessException` 확인, M2 참고), UNIQUE INFO, `RuntimeException` catch 로 스케줄러 종료 방지, `toss.default-account-seq`, cron·zone 프로퍼티 이름(설계 §5)이 모두 일치한다.
  - 요구 수정: 토큰 획득을 재시도 블록 밖에서 별도 catch 로 처리해 설계 §6 의 1·2행(ERROR / 재시도 1회)을 보이거나, "토큰 단계 정책은 생략" 이라고 명시한다. 메서드 이름은 설계의 `getAccessToken()` 으로 맞춘다.

- **[M4] §8 이 "본문 절과 1:1 로 대조했다" 고 적었지만 빠진 행이 있고, 본문에 근거가 없는 행도 남아 있다 (`:710-732`)**
  - 빠진 기존 문서 변경:
    - **06 §4.4** (`06_코드가이드라인.md:239-246`): `SentenceEntity sentence = sentenceService.create(request)` 로 서비스가 엔티티를 반환한다. §3.1 규칙(R5) 위반이다. §8 은 06 §3.2·§4.3 만 고친다.
    - **02 §5.2** (`02_설계.md:171-177`): 외부 예외 매핑 표가 `ResourceAccessException`/`HttpServerErrorException`/`HttpClientErrorException` 3종이다. §2.4(`RestClientResponseException` + `RestClientException`)와 충돌하는데 §8 에 02 행이 없다. 05 §5.7 만 고치면 02 와 05 가 서로 어긋난다.
    - **03 §3.1** `POST /answers` Errors 표: §2.2 로 깨진 JSON·빈 body 가 400 `BAD_REQUEST`, text/plain 이 415 가 된다([실행]). §8 의 03 행은 §2.1·§1.5 만 다룬다.
  - 본문에 근거가 없는 행:
    - 06 §4.5 "빈 body 행 추가": 본문 §2.2 표에 빈 body 시나리오가 없다. 실행하면 400 `TMI.COMMON.BAD_REQUEST` 다.
    - 06 §5.3 행과 99 §2.3 행: 근거가 current-state 와 architecture-proposal 이다. 표 머리말 "근거 열이 이 문서의 절이다"(`:710`)와 맞지 않는다(Round 1 M5 의 같은 지적이 남아 있다).
  - 요구 수정: 위 3행을 추가한다. 빈 body 는 §2.2 표에 한 행 넣는다. 외부 근거 행은 본문에 짧은 절을 만들거나 "근거" 열 규칙을 바꾼다. decisions-needed D8 의 "§8 표 19행" 도 함께 갱신한다.

## 경미

- [m1] §2.2 `:166-169` "상위 구현의 두 동작을 보존": 상위 구현은 commit 된 응답에서 `WARN "Response already committed. Ignoring: …"` 를 남기고 `null` 을 반환한다(Round 1 M1 javap). After 는 `return null` 만 하므로 **예외가 어떤 로그에도 남지 않는다**. [실행] `/probe/committed` 에서 status 200, body `partial`, `GlobalExceptionHandler`·상위 로그가 모두 0줄이었다. §5.1 "HTTP 예외 로그는 핸들러에서 1회" 와 어긋난다. `return null` 앞에 `log.warn(...)` 한 줄을 넣는다.
- [m2] §2.2 규칙 3 "5xx 는 `INTERNAL_ERROR`": 상위 클래스가 500이 아닌 5xx 를 주는 경우가 있다. [실행] `AsyncRequestTimeoutException` 은 **503** 인데 body 가 `TMI.COMMON.INTERNAL_ERROR` / "Internal server error." 로 상태와 메시지가 어긋났다. 의도한 동작이면 그렇다고 적고, 아니면 503 매핑을 둔다.
- [m3] §2.3 위임 경로는 `super(code, null)` 로 cause 를 **명시적으로 null** 로 넘긴다. 그래서 AS-IS 의 `(HttpStatus, IErrorType)`·`(…, Object...)` 생성자와 달리 이후 `initCause()` 를 부르면 `IllegalStateException` 이 난다(JDK `Throwable(String, Throwable)` 계약 [정적]). 현재 호출처는 0이라 영향은 없다. 99 §2.6 갱신 시 한 줄 남긴다.
- [m4] §6.2 `:586` "MockRestServiceServer 는 소켓 타임아웃을 재현하지 못함 — estimated": [실행]으로 올릴 수 있고 이유도 더 구체적이다. `withException(new SocketTimeoutException())` 은 **`ResourceAccessException`** 을 낸다. 실제 POST timeout 은 `RestClientException` 이다. 이 방식으로 회귀 테스트를 쓰면 `RestClientException` catch 를 지워도 통과한다. 그래서 JDK `HttpServer` 를 써야 한다.
- [m5] §0 `:18` 은 "§6 테스트 예시 3종" 의 HEAD 실행 결과만 적었다. §6.3 에는 `AnswerControllerTest` 가 추가돼 4종이다. [실행 (d)] 이 테스트도 HEAD 에서 실패(깨진 JSON → 500)하고 After 에서 통과한다. 결과 표에 한 행 추가한다.
- [m6] §5.1 표 "스케줄러·비동기 진입점 → `ERROR` (스택 포함)" 는 §4.3 의 설계 정책(4xx WARN, UNIQUE INFO)과 다르다. "진입점 catch 에서 설계 정책에 따른 레벨, 최종 실패는 ERROR" 로 고친다.
- [m7] §4.3 `snapshotWriter.save(...)` 를 "저장 전용 빈의 `@Transactional`" 로 둔 것은 설계 §3(`HoldingsSnapshotRepository.save` 직접 호출)보다 한 단계 많다. 상속 메서드 `save` 에는 이미 `SimpleJpaRepository` 의 트랜잭션이 있다(§4.2 의 사실). 별도 빈이 필요한 이유(매핑·`raw_response` 직렬화를 트랜잭션 안에서 하기 위해 등)를 한 줄 적거나, 리포지토리를 직접 호출하는 형태로 맞춘다.
- [m8] (examples, Round 1 m9 의 남은 부분) `examples/answer-scoring/option-a-layered/sentence/SentenceService.java:27` 은 아직 `findRandomBy` 다. 로그 키 `userAnswerLength` 는 `answerLength` 로 고쳐졌다(`option-a-layered/infra-similarity/SimilarityScorer.java:25`).

## 이전 라운드 지적 해소 여부

| R1 | 판정 | 확인 |
|---|---|---|
| C1 `BaseException` 생성자 NPE | **해소** | [실행 (a)(b)] 문서 After 를 그대로 적용하니 컴파일됐다. 404 `NotFound`(`Object...` 생성자) → **INFO**, `\tat` 0줄. 로그: `[TMI.SENTENCE.NOT_FOUND] Sentence not found: level=LEVEL_5, category=TECH args=[level=LEVEL_5, category=TECH] cause=…$NotFound`. 503 `Unavailable`(`Throwable` 생성자) → **ERROR**, 스택 126줄, `Caused by: org.springframework.web.client.ResourceAccessException`. 502 `RequestInvalid` → ERROR + 스택. `(…, Throwable)` 과 `(…, Object...)` 오버로드 해석도 문서 설명과 같다 |
| M1 상위 `handleExceptionInternal` 동작 | **대부분 해소** | [실행] 500 표준 예외(`HttpMessageNotWritableException`) → 요청 속성 `jakarta.servlet.error.exception` 설정, ERROR + 스택. commit 된 응답 → `null` 반환, 2차 예외 없음. WARN 로그는 빠졌다 → m1 |
| M2 Toss 예시 ↔ 설계 | **부분 해소** | 진입점 이름, `/holdings` 4xx·5xx·timeout·UNIQUE 정책, account-seq 출처는 맞다. 토큰 단계 정책과 메서드 이름이 다르다 → M3 |
| M3 `[B 전제]` 누락 | **해소** (§1.1·§3.1·§3.3·§4.1). 단 §2.4 의 A′ 서술이 틀렸다 → M1 |
| M4 R5 와 선택지 A | **해소** | §3.1 `:377` "A′·B 모두 적용" 은 `architecture-proposal.md:41`, `option-a-layered/sentence/SentenceService.java:33`(`SentenceDetail` 반환), decisions-needed `FIX-R5`("A′·B 공통")와 일치한다 |
| M5 §8 누락 | **부분 해소** | R1 이 든 8항목은 모두 들어갔다. 새로 빠진 항목 3개와 근거 열 불일치가 있다 → M4 |
| m1 build.gradle 라인 | 해소 (`:32,34` 일치) |
| m2 §0 "같은 코드" | 해소 (`:16` 에 Lombok 제거 방식 명시) |
| m3 §2.2→§2.3 순서 | 해소 (`:220`, D2 "둘을 함께") |
| m4 PageNotFound 이중 로그 | 해소 (`:221`, [실행] `WARN o.s.web.servlet.PageNotFound : Request method 'DELETE' is not supported` 재현) |
| m5 javap 근거 | 해소 (`:317`). 단 그 계층 사실이 기본 핸들러에서는 발생하지 않는다 → M2 |
| m6 GradeTest | 해소 (`@DisplayName`, `0.6999 → FAIR`, [실행] 7/7 통과) |
| m7 F3 테스트 | 해소 (`AnswerControllerTest`, [실행] HEAD 실패 / After 통과) |
| m8 §5.3 삭제·기본 프로필 | 해소 (`:547-556`) |
| m9 examples 불일치 | 부분 해소 → m8 |
| m10 헝가리안 "결정" 표현 | 해소 (`:90`) |
| m11 `Accept: xml` | 해소 (`:222` 에 AS-IS 동작으로 기록) |

## 검증한 주장 (문제 없음 확인)

- §2.2 + §2.3 + §2.1 After 를 문서 그대로 적용했을 때 [실행]:
  - Spring Boot 4.0.2 / Java 21 에서 수정 없이 컴파일된다. `:218` 의 import 목록으로 충분했다.
  - 기존 테스트에 영향이 없다. 전체 `test` 가 통과했다: CoreApplicationTests 1, LayerArchitectureTest 2, Convention/Module ArchUnit 은 `@Disabled` 6.
- §2.2 검증 결과 표 7행을 전부 재현했다:
  - 404 en `"Sentence not found: level=LEVEL_5, category=TECH"` / ko `"영어 문장을 찾을 수 없습니다: …"` / 헤더 없음(서버 로케일 ko)은 영문
  - 깨진 JSON 400 `BAD_REQUEST`
  - text/plain 415 `BAD_REQUEST`
  - 파라미터 누락 400 `MISSING_PARAMETER` `"Required parameter category is missing."`
  - 잘못된 enum 400 `INVALID_REQUEST`
  - DELETE 405 + `Allow: POST`
  - body 검증 400 `"sentenceId: must not be blank"`
  - 표 밖: 잘못된 URL 404 `NOT_FOUND_URL`, 빈 body 400 `BAD_REQUEST`
- §2.2 규칙 2 [실행]: 4xx 는 모두 INFO 이고 `cause=` 문자열만 남는다(스택 0줄). 5xx 는 ERROR 이고 `Throwable` 마지막 인자로 스택이 남는다. §5.2 `args=[…]` 기록도 확인했다.
- §6.3 [실행 (d)]: HEAD 에서 `SimilarityClientTest`(`IllegalStateException`), `SentenceControllerTest`(코드 문자열), `AnswerControllerTest`(500)가 실패하고 `GradeTest` 7/7 이 통과했다. After 에서는 4종 모두 통과했다.
- §2.4 / §6.2 의 Similarity(POST) 부분 [실행 (c)]: read timeout 은 `RestClientException` ← `SocketTimeoutException` 이고 AS-IS `AnswerService` 의 두 catch 를 빠져나가 catch-all 500 이 된다. 3-catch After 는 모든 경우를 `Unavailable` 로 보낸다. 빈 200 본문은 `null` 을 반환한다(문서·D13 "빈 응답 → Unavailable" 의 전제와 일치).
- §4.3 이 설계 §5 와 맞는 부분: `toss.holdings.schedule-cron`·`toss.holdings.timezone`·`toss.default-account-seq` 이름. WIP 의 `SimpleClientHttpRequestFactory` 사용(`wip-snapshot.patch:58-60`) 때문에 GET timeout 이 `ResourceAccessException` 이 된다는 점도 [실행]으로 확인했다.
- `[B 전제]` 와 A′ 서술 중 §1.1(`option-a-layered/answer/AnswerResponse.java:11` 시그니처 동일, `SentenceDetail` 은 `sentence.dto`), §3.1·§3.3(`option-a-layered/sentence/SentenceDetail.java:3`), §4.1(`option-a-layered/answer/AnswerService.java:24` `sentenceService.getActive`)은 architecture-proposal §2 A′ 트리와 일치한다.
- §8 행 수 19 는 decisions-needed D8 "§8 표 19행" 과 일치한다. 05 §5.7·§6.5·§6.6, 06 §2.2·§2.3·§3.2·§3.3·§3.5·§4.3·§4.5·§5.3·§7.1·§7.4, 99 §2.3·§2.6·§2.7·§3 의 현재 내용이 각 행의 "수정" 설명과 맞는다. 06 §6.6 "throw 직전 로그 금지" 도 실제로 있다(§5.1 서술 정확).
- §2.2 `:239` "더 구체적인 타입 우선": `BaseException`·`Exception` 핸들러와 상위 클래스의 표준 예외 핸들러가 공존해도 표준 예외는 모두 `handleExceptionInternal` 로 갔다 [실행].
