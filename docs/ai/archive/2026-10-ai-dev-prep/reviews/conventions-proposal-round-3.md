# conventions-proposal 리뷰 — Round 3

> 대상: `docs/ai/proposals/conventions-proposal.md` @ `83f7b92`. 이 커밋의 문서는 Round 2 반영 커밋 `fc49d18` 과 같다(`3e136c6..83f7b92` diff 에서 확인).
> 검증 방법: 전용 worktree(`scratchpad/wt-conventions-proposal`, `--detach 83f7b92`)에서 `bash ./gradlew test --no-daemon --offline` 로 실행했다. 끝난 뒤 worktree 는 제거했다. `/home/user/repo` 에서는 이 파일만 만들었다.
> - (a) worktree `src/main` 에 문서 After 를 적용했다. §2.2 `GlobalExceptionHandler` 는 코드 블록을 **글자 그대로** 쓰고 `:220` 의 import 만 더했다. 여기에는 이번에 추가된 commit WARN 이 들어 있다. §2.3 `BaseException` 생성자 4개 위임과 `logMessage` 삭제, §2.1 설정 2줄, §2.5 `@RequestBody` 도 적용했다.
> - (b) `@WebMvcTest(AnswerController)` 에 테스트 전용 `ProbeController` 를 붙이고 `OutputCaptureExtension` 으로 실행했다. 시나리오는 commit 된 응답(표준 예외 / `BaseException`), 깨진 JSON, 빈 body, text/plain, DELETE 다.
> - (c) **§4.3 Toss 예시를 임시 클래스로 컴파일**했다. 문서 코드 블록을 그대로 추출하고 `TossTokenProvider`·`TossHoldingsClient`·`HoldingsApiResponse`·리포지토리·매퍼는 최소 스텁으로 두었다. 람다 스텁으로 9개 시나리오도 실행했다: 토큰 401, 토큰 502→성공, 토큰 I/O 2회, `/holdings` 403, 본문 정지 `RestClientException`→성공, 503→401, UNIQUE, 600(`UnknownHttpStatusCodeException`).
> - (d) JDK `HttpServer`(1초 지연)와 `SimpleClientHttpRequestFactory`(readTimeout 300ms)에 HTTP Interface 를 연결했다. 요청은 GET, 헤더·파라미터를 붙인 GET, 본문 있는 POST, **본문 없는 POST**, 본문 있는 PUT 이다. §2.4 의 "본문 유무" 분류를 확인했다.
> - (e) 위 상태에서 전체 `test` 를 실행했다(CoreApplicationTests 와 ArchUnit 포함, DB 는 `ddl-auto=validate` 조회만).
> - 대조한 문서: `docs/public/{02 §5.2·§6.1, 03 §1.5·§2.1·§3.1·§5.2, 05 §5.7·§6.5·§6.6, 06 §4.4·§4.5·§7, 99 §2.3}`, `toss/00_인증.md`, `toss/01_보유자산스냅샷.md §3·§5·§6`, `docs/ai/{architecture-proposal §2, decisions-needed D8·D9·D13, current-state §3.3(f), examples/answer-scoring/{README, option-a-layered, option-b-modular}}`, `wip-snapshot.patch`

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | §2.2 commit WARN, §2.4 표, §4.3 정책은 모두 실행으로 재현됐다. §0 표는 Round 2 변경분(WARN 줄)과 §4.3 이 이제 컴파일 가능하다는 사실을 반영하지 않았다(경미) |
| 구체성 | 4 | §2.2·§2.3·§4.3 코드를 그대로 붙여 넣어도 컴파일되고 의도대로 동작한다. `withOneRetry` 의 catch 순서도 합법이고 `token` 은 effectively final 이다 |
| 일관성 | 3 | §4.3 Toss 예시가 서비스에서 raw `HttpClientErrorException` 을 직접 다룬다. 이것은 A′ 강제 규칙 (b)(architecture-proposal §2), 이 문서 §2.4 "변환은 외부 시스템 쪽 클래스 하나", 05 §5.7 과 맞지 않는다. `[B 전제]` 표시도 없다(M1). 나머지 교차 문서 일관성(A′ §2.4, D13, D8 22행)은 해소됐다 |
| 판단 가능성 | 4 | 규칙마다 Before/After, 근거, D 번호, 05/06 수정 목록이 있다. §8 22행이 본문 절과 대응한다 |

## 치명 (승인 차단)

없음.

## 주요

- **[M1] §4.3 Toss 예시는 외부 예외 처리 방식이 §2.4·05 §5.7 과 다르고, 선택지 A′ 의 강제 규칙 (b) 를 위반한다. 그런데 `[B 전제]` 표시가 없다 (`conventions-proposal.md:481-527`)**
  - 문제 1 (A′ 규칙 위반):
    - 예시 위치는 `toss/holdings/service/HoldingsSnapshotService` 이고, `tossHoldingsClient.get(...)` 과 `HoldingsApiResponse` 를 직접 쓴다.
    - A′ 에서 이 두 타입은 `infra/toss/{TossHoldingsClient, …, dto/}` 에 있다(`architecture-proposal.md:58`).
    - A′ 강제 규칙 (b)(`architecture-proposal.md:63`)는 "`infra/{시스템}` 패키지의 타입 중 `*Scorer`(변환 클래스) 외에는 `infra` 밖에서 접근 금지" 다. 따라서 A′ 를 고르면 이 예시는 ArchUnit 위반이다.
    - B 에서는 `toss/holdings/client` 가 같은 모듈의 internal 이라 문제가 없다(`architecture-proposal.md:70`). 즉 이 예시는 B 전제 코드다.
  - 문제 2 (규칙 이중화):
    - §2.4 의 규칙(`:320`)은 "외부 시스템 쪽 클래스 하나가 변환" 이고 근거는 "세 catch 를 모두 둔다"(`:332`)다. 05 §5.7 도 "raw HTTP/네트워크 예외는 … `BaseException` 자식으로 변환 후 throw" 라고 한다.
    - §4.3 은 오케스트레이터 서비스가 raw `HttpClientErrorException`·`HttpServerErrorException`·`ResourceAccessException`·`RestClientException` 을 직접 분류한다(`:491, :502, :515-520`).
    - 스케줄러 경로라 HTTP 응답 변환이 필요 없다는 점은 이해할 수 있다. 그러나 문서는 "외부 예외는 언제 변환 클래스에 두고, 언제 호출 측이 raw 로 다루는가" 를 적지 않았다. Toss 를 완성할 사람(D9)은 두 패턴 중 무엇을 따라야 할지 정할 수 없다.
  - 근거: [실행 (c)] 예시 자체는 컴파일되고 설계 §6 대로 동작한다(아래 "검증한 주장"). 문제는 동작이 아니라 규칙 간 모순이다.
  - 요구 수정: 아래 중 하나를 고른다.
    - (a) §4.3 에 `[B 전제]` 를 붙이고, A′ 형태를 한 줄 적는다. 예: "A′ 이면 `infra/toss` 에 Toss 변환 클래스(예: `TossHoldingsFetcher`)를 두고 서비스는 그 결과·도메인 예외만 받는다 — architecture §2 A′ (b) 의 `*Scorer` 허용 목록에 그 이름을 추가."
    - (b) §2.4 에 "스케줄러처럼 HTTP 응답으로 변환하지 않는 호출 측은 재시도·레벨 정책 때문에 raw 예외를 직접 분류할 수 있다. 단 같은 모듈 internal 클라이언트에 한함" 이라는 예외 규칙을 두고, 05 §5.7 수정 행(§8)에 반영한다.
    - 어느 쪽이든 architecture-proposal §2 A′ (b) 와 맞춘다.

## 경미

- [m1] §0 `:18` 의 "2차, Round 1 반영본" 검증 행은 Round 2 에서 추가된 commit WARN 줄(`:169`)을 포함하지 않는다. `:21` 은 "§4.3 Toss 예시 검증하지 못함" 이라고 적었지만, 스텁으로 컴파일·정책 실행이 가능하다(이번 [실행 (c)]). 근거 표를 갱신한다. 이번 리뷰가 확인한 결과는 아래 "검증한 주장" 에 있다.
- [m2] §2.2 의 commit 보존은 **표준 예외 경로(`handleExceptionInternal`)에만** 적용된다. [실행 (b)]: commit 뒤 `SimilarityException.Unavailable` 이 나면 `handleBase` 가 ERROR 를 남긴다. 이어서 `WARN …ExceptionHandlerExceptionResolver : Failure in @ExceptionHandler …handleBase` 가 나오고, 원 예외가 `ServletException` 으로 다시 던져진다. AS-IS 와 같은 동작이라 회귀는 아니다. 그러나 주석 "상위 구현의 두 동작을 보존" 은 `BaseException`·`Exception` 경로에는 해당하지 않는다고 한 줄 남긴다(현재 스트리밍 응답이 없어 영향 없음).
- [m3] 같은 문서 안의 "05/06 관계" 줄 일부가 §8 보다 좁다. 결정에는 영향이 없다.
  - §2.2 `:244` 는 "06 §4.5 표에 415·깨진 JSON 행 추가" 만 적었다. §8 은 06 §4.5 에 빈 body 를 더하고, 03 §3.1 행도 있다.
  - §3.1 `:393` 은 "06 §3.2·§4.3 코드 수정" 만 적었다. §8 에는 06 §4.4 가 있다.
- [m4] §2.4 After 블록은 출처를 `option-b-modular/similarity/SimilarityScorer.java` 로 밝혔지만 주석이 그 파일과 다르다.
  - 파일의 `:32` 는 "DefaultResponseErrorHandler 기준", `:36` 은 "연결 거부 등 I/O 오류 (연결 단계)" 다. 문서는 "커스텀 ResponseErrorHandler 대비", "본문 없는 요청(GET)의 read timeout" 이다.
  - Similarity 는 POST 라서 파일 주석도 틀리지는 않다.
  - 다만 current-state §3.3(f) 와 examples README `:39` 는 아직 "read timeout 은 `RestClientException`" 을 조건 없이 적고 있다. "본문 있는 요청에서" 를 붙이면 세 문서가 맞는다.
- [m5] §4.3 `:526` "매핑은 … 엔티티 밖(`HoldingsSnapshotMapper`)에 둔다": 매퍼의 패키지가 없다. `domain` 에 두면 R4b 위반이 그대로 남는다. 또 WIP 의 `HoldingsSnapshotEntity.create(…, HoldingsApiResponse.Result, …)`(`wip-snapshot.patch`)는 엔티티 팩토리 시그니처 자체가 dto 를 받는다. 따라서 팩토리를 원시값 인자로 바꿔야 한다. "매퍼는 `service`(B: 모듈 internal) 패키지, 엔티티 팩토리는 dto 를 받지 않음" 을 한 줄 추가한다.
- [m6] §4.3 `withOneRetry` 의 마지막 `catch (RestClientException)` 은 본문 정지 timeout 외에 **역직렬화 실패**(스키마 불일치)도 재시도한다. 결정적인 실패라 재시도해도 같은 결과다. [실행 (c)]: `UnknownHttpStatusCodeException`(600) 도 2회 호출됐다. 설계 §6 이 금지하지는 않으므로 문제는 아니다. 주석 "본문 수신 중 정지는 RestClientException" 에 "역직렬화 실패도 1회 더 시도됨(무해)" 을 덧붙이면 의도가 분명해진다.
- [m7] (examples, Round 2 m8 의 남은 부분) `examples/answer-scoring/option-a-layered/sentence/SentenceService.java:27` 은 아직 `findRandomBy` 다. §1.2 의 `findRandomActiveByLevelAndCategory` 와 다르다.

## 이전 라운드 지적 해소 여부

| R2 | 판정 | 확인 |
|---|---|---|
| M1 §2.4 A′ 서술 | **해소** | `:320` "A′ 이면 `infra.similarity.SimilarityScorer` — 규칙(변환 클래스 1개)은 같다" 는 `architecture-proposal.md:46`(규칙 (3)), `:57` 트리, `option-a-layered/infra-similarity/SimilarityScorer.java:4` 패키지와 일치한다. 문서에 "선택지 A" 단독 표기가 0건이다(grep) |
| M2 timeout·상태 코드 일반화 | **해소** | `:312·:314` 주석과 `:323-330` 표가 요청 방식별로 바뀌었다. [실행 (d)] GET → `ResourceAccessException`, 헤더·파라미터 GET → `ResourceAccessException`, 본문 POST → `RestClientException`, **본문 없는 POST → `ResourceAccessException`**, 본문 PUT → `RestClientException`(모두 원인 `SocketTimeoutException`). 문서의 분류 기준인 "본문 유무"(`:312, :314`)가 HTTP 메서드보다 정확한 기준임을 확인했다. 600/299 행은 D13 "보류" 로 연결됐고, `:332` 는 `UnknownHttpStatusCodeException` 을 "기본 설정에서 관찰되지 않음" 으로 낮췄다. current-state §3.3(f)·D13 과 모순이 없다 |
| M3 Toss 토큰 단계 | **해소** | `:488-497` 에서 토큰 획득을 재시도 블록 밖에서 따로 catch 한다. [실행 (c)] 토큰 401 → `ERROR "Toss 토큰 발급 거부 — 스냅샷 skip: status=401"`(스택 포함), 토큰 호출 1회(4xx 재시도 없음), `/holdings` 호출 0회. 502→성공 → 토큰 2회, holdings 1회. I/O 2회 → `ERROR "토큰 발급 실패"`. 설계 §6 1·2행과 일치한다. 메서드 이름은 설계 §3 의 `getAccessToken()` 이다. 단 다른 규칙과의 관계 → 새 M1 |
| M4 §8 누락·근거 열 | **해소** | 02 §5.2·03 §3.1·06 §4.4 행 추가, 빈 body 를 §2.2 표 `:232` 에 추가, 머리말 `:743` 에서 근거 열 규칙을 "외부 근거는 함께 적음" 으로 바꿈. 행 수 22 = decisions-needed D8 "§8 표 22행"(`decisions-needed.md:129`). 행별 대응은 아래 "검증한 주장" |
| m1 commit 시 WARN | **해소** | [실행 (b)] `/probe/committed` → status 200, body `partial`, `WARN c.t.c.c.e.GlobalExceptionHandler : Response already committed. Ignoring: org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'PATCH' is not supported` 1줄. 2차 예외는 없다. 적용 범위 한계 → m2 |
| m2 503 → INTERNAL_ERROR | 해소 (`:128` 의도적 단순화 + 재검토 조건 명시) |
| m3 `initCause` | 해소 (`:286`) |
| m4 MockRestServiceServer timeout | 해소 (`:619`, 사용 금지 이유 명시) |
| m5 §0 테스트 4종 | 해소 (`:19`) |
| m6 §5.1 스케줄러 레벨 | 해소 (`:542`) |
| m7 snapshotWriter | 해소 (`:477, :501, :526` 상속 `save` 직접 호출, 다중 쓰기 시에만 전용 빈). 매퍼 위치 → m5 |
| m8 examples `findRandomBy` | **미해소** → m7 (examples 쪽 문제) |

## 검증한 주장 (문제 없음 확인)

- **§4.3 Toss 예시 컴파일** [실행 (c)]: 문서 코드 블록을 그대로 넣은 임시 클래스가 Java 21 에서 경고 없이 컴파일됐다.
  - `withOneRetry` 의 catch 순서는 `HttpServerErrorException | ResourceAccessException` → `HttpClientErrorException` → `RestClientException` 이다. 하위 타입이 상위 타입보다 앞에 있으므로 "already caught" 오류가 없다. multi-catch 두 타입은 서로 하위 관계가 아니다.
  - `String token;` 은 try 안에서 한 번만 대입되고, 모든 catch 가 `return` 한다. 그래서 확정 대입이 성립하고 effectively final 이다. 람다 `() -> tossHoldingsClient.get(token, accountSeq)` 가 컴파일된다.
- **§4.3 정책** [실행 (c)]:
  - `/holdings` 403 → `WARN "Toss 보유자산 조회 거부: accountSeq=7 status=403"`, 호출 1회
  - 본문 정지 `RestClientException` → 재시도 후 저장(`snapshotAt=…T01:00:00Z`, 정각 절삭 확인)
  - 503→401 → 2회 호출 후 WARN
  - UNIQUE(`DataIntegrityViolationException`) → `INFO "Toss 스냅샷 중복 — 무시"`
  - 그 밖의 예외 → `ERROR` + 스택, 메서드는 예외 없이 반환(스케줄러 종료 금지)
  - 위 결과는 설계 `01_보유자산스냅샷.md` §6 의 5행과 모두 일치한다. 프로퍼티 이름(`toss.holdings.schedule-cron`, `toss.holdings.timezone`, `toss.default-account-seq`)도 설계 §5 및 WIP `application.properties` 와 같다.
- **§2.4 표 6행** [실행 (d)·정적]: 연결 거부·499/599·600 행은 Round 2 실행 결과와 같다. `SimilarityException.RequestInvalid` = `BAD_GATEWAY`(502), `Unavailable` = `SERVICE_UNAVAILABLE`(503) 이므로 "1번째 catch → 502 / 503" 이 맞다(`infra/similarity/exception/SimilarityException.java`). 토큰 엔드포인트는 form 본문이 있는 POST 라 read timeout 이 `RestClientException` 이 된다. 그래도 `withOneRetry` 의 3번째 catch 가 재시도하므로 설계 §6 2행 "네트워크" 가 덮인다.
- **§2.2 + §2.3 + §2.1 After 회귀** [실행 (b)(e)]: 깨진 JSON 400 `BAD_REQUEST`, 빈 body 400 `BAD_REQUEST`(`Required request body is missing`), text/plain 415 `BAD_REQUEST`, DELETE 405 + `Allow: POST` 가 모두 문서 표 `:231-236` 과 같다. 4xx 는 INFO + `cause=` 이고 스택은 없다. 전체 `test` 는 통과했다: CoreApplicationTests 1, LayerArchitectureTest 2, Convention·Module ArchUnit 은 `@Disabled` 6, 프로브 3.
- **§8 표 22행 ↔ 본문·대상 문서**:
  - 05 §5.7: 대상 문서에 `HttpClientErrorException`/`ResourceAccessException | HttpServerErrorException` 2-catch 예시와 "위 try/catch 그대로 적용" 마지막 문단이 실제로 있다. 근거는 §2.4.
  - 02 §5.2: 대상 문서가 3종 표다. 근거는 §2.4 `:333`.
  - 03 §3.1: Errors 표에 400 `INVALID_REQUEST`/404/502/503 만 있다. 근거 §2.2 표 `:231-233` 과 대응한다.
  - 05 §6.5: `log.error("유사도 서버 호출 실패 …", e)` 가 실제로 있다. 근거는 §5.1.
  - 05 §6.6: "ERROR — 예외 발생" 이 실제로 있다. 근거는 §2.3·§5.1.
  - 06 §4.4: 대상 문서에 `SentenceEntity sentence = sentenceService.create(request)` 가 실제로 있다. 근거 §3.1 의 반환 규칙과 대응한다.
  - 06 §4.5: 대상 문서 표 5행에 415·깨진 JSON·빈 body 가 없다. 근거는 §2.2.
  - 06 §7.1: 06 `:435` 에 "Phase 5 WireMock" 이 실제로 있다. 근거는 §6.2.
  - 99 §2.3: "`RestClient` 빈" 이 실제로 있다(99 `:74`). 근거는 외부 문서로 명시돼 머리말 `:743` 과 일치한다.
  - 03 §2.1·§1.5: §1.5 에 `MISSING_PARAMETER`·`BAD_REQUEST`·`NOT_FOUND_URL`·`METHOD_NOT_SUPPORTED` 가 없다. "코드 목록 보강" 이 필요하다는 점이 맞고, D6 과 연결된다.
  - 나머지 행(06 §2.2·§2.3·§3.2·§3.3·§3.5·§4.3·§5.3·§7.4·§7 신규, 99 §2.6·§2.7·§3)은 Round 2 에서 확인한 대상 내용에서 바뀌지 않았다.
- **다른 문서와의 일관성**:
  - architecture-proposal §2 A′ (3)·트리와 §2.4 변환 위치가 일치한다.
  - examples `SimilarityScorer`(A′·B)의 3-catch 순서·빈 응답 `Unavailable`·`SCORE_TOLERANCE=1e-6` 이 §2.4 `:317` 및 D13 (b) 와 일치한다.
  - D13 의 "600+ 보류" 가 §2.4 표 마지막 행과 일치한다.
  - D8 의 "22행" 이 §8 과 일치한다.
