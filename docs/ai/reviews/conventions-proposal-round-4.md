# conventions-proposal 리뷰 — Round 4

> 대상: `docs/ai/conventions-proposal.md` @ `01898ba`. Round 3 이후 문서 변경은 `c577977`·`da98b42`·`01898ba` 이며, `83f7b92..01898ba` diff 로 확인했다. 코드 블록의 실행 코드는 바뀌지 않았고 주석·서술만 바뀌었다(§2.2 주석 1줄, §2.4 주석 1줄과 예외 규칙 bullet, §4.3 머리말·주석·bullet).
> 검증 방법: 전용 worktree(`scratchpad/wt-conventions-proposal`, `--detach 01898ba`)를 만들어 `bash ./gradlew test --no-daemon --offline` 로 실행했다. 실행 뒤 worktree 는 제거했고, `/home/user/repo` 에서는 이 파일만 만들었다.
> - (a) worktree `src/main` 에 문서 After 를 **문서에서 그대로 추출해** 적용했다.
>   - §2.2 `GlobalExceptionHandler`: `:147-219` 를 그대로 쓰고, `:222` 의 import 와 기존 import 를 더했다.
>   - §2.3 `BaseException`: `:263-283` 의 생성자 4개 위임을 적용하고 `logMessage` 필드·메서드를 삭제했다.
>   - §2.1 설정 2줄, §2.5 `@RequestBody` 도 적용했다.
> - (b) `@WebMvcTest({SentenceController, AnswerController})` 와 `OutputCaptureExtension` 으로 8개 시나리오를 돌렸다: 404, 503, 깨진 JSON, 빈 body, text/plain, DELETE, 필수 파라미터 누락, 잘못된 enum. `MockRestServiceServer` 로 §6.3 `SimilarityClientTest` 도 돌렸다.
> - (c) 위 상태에서 전체 `test` 를 실행했다. CoreApplicationTests 와 ArchUnit 이 포함되며, DB 는 `ddl-auto=validate` 로 조회만 했다.
> - (d) 정적 대조한 문서: `architecture-proposal.md §2`(A′ 트리 `:60`, 규칙 (b) `:67-68`, B 배치 `:75`, B 트리 `:89-90`), `src/test/.../architecture/{ArchitectureClasses, ModuleArchitectureTest}`(B 의 R2 판정 로직), `examples/answer-scoring/{README, option-a-layered/infra-similarity/SimilarityScorer, option-b-modular/similarity/SimilarityScorer, option-a-layered/sentence/SentenceService}`, `current-state.md §3.3(f)`, `decisions-needed.md D8`, `feature_list.example.json FIX-R5`, `docs/public/05 §5.7`, `toss/00_인증.md §4`, `wip-snapshot.patch`.
> - §4.3 코드는 Round 3 [실행 (c)]에서 컴파일·9개 시나리오 실행을 마쳤다. 이번 커밋에서 바뀐 것은 주석·서술뿐이라(diff 확인) 다시 실행하지 않았다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | §2.1~§2.3·§2.5 After 를 그대로 적용하면 컴파일되고 §2.2 표 8행이 재현된다. §0 표와 "검증하지 못한 것" 이 실제 검증 범위와 맞게 갱신됐다 |
| 구체성 | 4 | B 기준 코드는 바로 붙여 쓸 수 있다. A′ 형태 한 줄은 토큰 단계와 fetcher 반환 타입이 비어 있다(m2). A′ 는 추천안이 아니고 D9 시점의 일이라 경미로 본다 |
| 일관성 | 4 | Round 3 M1 이 해소됐다. §2.4 예외 규칙은 B 배치·ArchUnit R2 판정과 맞고, A′ (b) 에서는 해당하는 경우가 없어 fetcher 가 필요하다는 결론과도 맞는다. 남은 문제는 §8 의 05 §5.7 행이 이 예외를 반영하지 않은 것(m1)과 A′ 서술의 작은 틈(m2·m3)이다 |
| 판단 가능성 | 4 | Toss 를 완성할 사람이 B·A′ 각각에서 어떤 패턴을 따를지 정할 수 있다. 규칙마다 Before/After, 근거, D 번호, §8 22행이 있다 |

## 치명 (승인 차단)

없음.

## 주요

없음.

## 경미

- **[m1] §8 의 05 §5.7 행(`:751`)과 §2.4 "05/06 관계"(`:336`)에 새 예외 규칙이 빠져 있다**
  - 05 §5.7 은 예외 없이 규정한다: "raw HTTP/네트워크 예외는 … `BaseException` 자식으로 변환 후 throw"(`05_개발컨벤션.md:308`), "raw HTTP 클라이언트 예외 전파 금지"(`:341`).
  - §2.4 `:334` 의 예외 규칙은 스케줄러 + 같은 모듈 internal 클라이언트일 때 raw 분류를 허용한다. 그런데 §8 행은 "예시를 `RestClientResponseException` 기준으로, 마지막 문단 정정" 만 적었다.
  - 그래서 `DOC-1` 을 §8 표대로 수행하면 05 는 계속 무조건 규칙으로 남는다. Round 3 M1 요구 수정 (b) 의 "05 §5.7 수정 행(§8)에 반영" 부분이 남은 것이다.
  - 수정: 행에 "+ 스케줄러 예외 규칙 1문단(§2.4)" 을 더한다. 행 수는 그대로라 D8 "22행" 에 영향이 없다.
- **[m2] §4.3 의 A′ 형태 한 줄(`:482`)이 토큰 단계와 fetcher 반환 타입을 정하지 않았다**
  - 토큰: A′ 규칙 (b)(`architecture-proposal.md:67`)는 `TossTokenProvider` 를 `infra` 밖에 공개하는 진입점으로 둔다. 그러나 이 클래스가 raw 예외를 던지는지 도메인 예외로 바꾸는지는 어느 문서에도 없다. A′ 의 서비스가 B 코드처럼 `getAccessToken()` 을 직접 부르고 `HttpClientErrorException` 을 catch 하면 §2.4 예외 규칙의 "같은 모듈 internal" 조건을 벗어난다. 그런데 ArchUnit (b) 는 Spring 예외 타입 참조를 잡지 못하므로 이 위반은 기계적으로 막히지 않는다.
  - 반환 타입: `HoldingsApiResponse` 는 A′ 에서 `infra/toss/dto` 에 있다. (b) 가 `dto` 를 밖에서 참조하지 못하게 하므로 "서비스는 그 결과를 받는다" 의 "결과" 가 dto 이면 (b) 위반이다. 같은 이유로 `:530` 의 `toss/holdings/service/HoldingsSnapshotMapper` 가 dto 를 받는 형태도 A′ 에서는 성립하지 않는다.
  - A′ 의 `infra/toss` 트리(`architecture-proposal.md:60`)에는 `TossException` 을 둘 `exception/` 도 없다.
  - 수정: 한 줄을 덧붙인다. 예: "A′ 이면 토큰 획득도 `TossHoldingsFetcher` 안에서(또는 `TossTokenProvider` 가 도메인 예외로 변환), fetcher 는 dto 가 아닌 record/원시값을 반환, `infra/toss/exception/TossException`."
  - 추천안 B 와 D1 결정에는 영향이 없어 경미로 분류한다.
- [m3] `:482` 는 "architecture-proposal.md §2 A′ (b) 의 허용 목록에 그 이름 추가" 라고 적었다. 그러나 `da98b42` 에서 이미 `architecture-proposal.md:67` 에 `TossHoldingsFetcher` 가 들어갔고, `:60` 트리와 `:68` 비용 줄에도 있다. "추가" 는 할 일처럼 읽히므로 "(허용 목록에 포함됨)" 으로 바꾼다.
- [m4] 머리말 `:6` 은 `[B 전제]` 를 "규칙은 같고 클래스 위치·이름만 다른 경우" 로 정의한다. §4.3 의 A′ 형태는 이와 다르다. 책임이 다른 클래스(`TossHoldingsFetcher`)가 하나 더 생기고, 서비스의 예외 처리 패턴도 달라진다. 본문이 차이를 명시하고 있으므로 판단에는 지장이 없다. 정의에 "(§4.3 은 클래스 1개 추가)" 를 붙이거나, §4.3 표시를 `[B 전제 — A′ 는 구조 다름]` 으로 바꾸면 정확해진다.
- [m5] (Round 3 m4 의 남은 부분) §2.4 After `:310` 의 주석 "상위 타입 — 커스텀 ResponseErrorHandler 대비" 는 출처 파일 `option-b-modular/similarity/SimilarityScorer.java:33` 의 "DefaultResponseErrorHandler 기준" 과 다르다.
  - 두 주석은 모순은 아니다. 문서는 왜 상위 타입을 잡는지를 말하고(`:335` 에서 부연), 파일은 기본 핸들러에서의 동작을 말한다.
  - 나머지 두 주석은 사실상 일치한다. `:314` "본문 없는 요청(GET)" 은 파일 `:37` 의 "(GET 등)" 과 같고, `:316` 은 파일 `:40-41` 과 같다.
  - current-state `:289` 와 README `:40` 에는 "본문 있는 요청" 이 들어가서 세 문서가 맞게 됐다. 파일 `:33` 주석에 "커스텀 핸들러 대비" 를 더하면 완전히 맞는다.
- [m6] (Round 3 m7) `option-a-layered/sentence/SentenceService.java:27` 는 여전히 `findRandomBy` 를 쓴다. 대신 "§1.2 개명은 FIX-R5 에서" 라는 주석이 붙었다. §3.1 After(`:384`)가 개명된 메서드를 쓰므로 FIX-R5 가 §3.1 을 적용하면 개명도 따라온다. 그러나 `feature_list.example.json` 의 FIX-R5 title·acceptance·verify(`:149-162`)에는 개명이 없다. acceptance 에 "§1.2 개명 포함" 을 넣으면 소유 작업이 명확해진다.
- [m7] 머리말 개정 이력의 순서가 Round 1 → **Round 3** → Round 2 다(`:7-9`). 시간 순서로 맞춘다.

## 이전 라운드 지적 해소 여부

| R3 | 판정 | 확인 |
|---|---|---|
| M1 §4.3 raw 예외 처리 vs §2.4·A′ (b) | **해소** (경미 잔존 m1·m2) | 요구 수정 (a)·(b) 를 둘 다 적용했다. (b): §2.4 `:334` 에 예외 규칙을 명시했다. 조건은 HTTP 응답으로 변환하지 않는 진입점, 재시도·레벨 정책, 같은 모듈 internal 클라이언트, 세 catch 범주 처리다. (a): §4.3 `:482` 에 `[B 전제]` 를 붙이고 A′ 형태(`infra/toss/TossHoldingsFetcher` + 도메인 예외)를 적었다. **B 와의 정합**은 B 배치에서 `toss/auth/`·`toss/holdings/client/` 가 같은 `toss` 모듈이라는 데서 나온다(`architecture-proposal.md:75, :89-90`). `ArchitectureClasses.moduleOf` 는 첫 하위 패키지를 모듈로 보고, `ModuleArchitectureTest.onlyAccessOtherModulesThroughApi` 는 같은 모듈 안의 의존을 검사하지 않는다(`:75-90`). 그래서 `toss.holdings.service → toss.auth`·`toss.holdings.client` 는 R2 를 통과하고, 예외 규칙의 "같은 모듈 internal" 조건과도 맞는다. **A′ 와의 정합**: A′ 에서는 `infra` 가 기능과 다른 모듈이라 예외 규칙에 해당하는 경우가 없다. 따라서 fetcher 가 필요하다는 결론이 (b) 의 허용 목록(`:67`)·비용 줄(`:68`)·README `:120` 과 맞는다. 세 catch 범주는 `withOneRetry` 가 덮는다. `HttpServerErrorException`·`HttpClientErrorException` 이 1번째 범주, `ResourceAccessException` 이 2번째이고, `RestClientException` 은 `UnknownHttpStatusCodeException` 까지 포함한다. Round 3 [실행 (c)] 결과로 확인했다 |
| m1 §0 근거 표 | 해소 | `:19` 에 Round 2 commit WARN 재현 출처, `:22` 에 §4.3 스텁 컴파일·5개 정책 실행이 들어갔다 |
| m2 commit 보존 범위 | 해소 | `:169` 주석 "표준 MVC 예외 경로에만 해당…" |
| m3 05/06 관계 줄 | 해소 | `:246` 에 빈 body·03 §3.1, `:396` 에 06 §4.4 가 들어가 §8 과 일치한다 |
| m4 After 주석 ↔ examples | 대부분 해소 → m5 | README `:40`·current-state `:289` 에 "본문 있는 요청" 이 추가됐다. `:310` 주석 1개만 다르다 |
| m5 매퍼 패키지·엔티티 팩토리 | 해소 (B 기준) | `:530` 은 `toss/holdings/service/HoldingsSnapshotMapper` 와 팩토리 원시값 인자를 명시한다. WIP 의 `HoldingsSnapshotEntity.create(…, HoldingsApiResponse.Result, …)` 는 `wip-snapshot.patch:83,96` 과 일치한다. A′ 에서의 형태 → m2 |
| m6 `withOneRetry` 마지막 catch | 해소 | `:515` "역직렬화 실패·600+ 상태도 1회 더 시도 — 무해" |
| m7 examples `findRandomBy` | 부분 해소 → m6 | 남겨 둔 이유를 주석으로 적었다. 소유 작업의 acceptance 에는 없다 |

## 검증한 주장 (문제 없음 확인)

- **§2.1 + §2.2 + §2.3 + §2.5 After 컴파일·동작** [실행 (a)(b)]
  - 문서 블록을 그대로 추출해 `src/main` 에 넣었고 경고 없이 컴파일됐다. `BaseException` 의 `this(httpStatus, errorType, null, EMPTY_ARGS)` 는 4-인자 생성자로 해석되며 모호성 오류가 없다.
  - 404 `SentenceException.NotFound` → `"Sentence not found: level=LEVEL_5, category=TECH"`, 로그 INFO, 스택 없음(§2.3 의 상태 기반 레벨)
  - `SimilarityException.Unavailable(ConnectException)` → 503, ERROR + `java.net.ConnectException: refused` 원인 스택
  - 깨진 JSON → 400 `TMI.COMMON.BAD_REQUEST`
  - 빈 body → 400 `TMI.COMMON.BAD_REQUEST`
  - text/plain → 415 `TMI.COMMON.BAD_REQUEST`
  - `DELETE /answers` → 405 + `Allow` 헤더 + `METHOD_NOT_SUPPORTED`
  - category 누락 → 400 `MISSING_PARAMETER`, `"Required parameter category is missing."`
  - 잘못된 enum → 400 `INVALID_REQUEST`
  - 위 결과는 모두 §2.2 표 `:232-239` 와 같다.
- **§6.3 `SimilarityClientTest`** [실행 (b)]: §2.5 `@RequestBody` 를 적용하면 통과한다. POST, JSON 본문 `{"answer","reference"}`, `score=0.91` 을 확인했다.
- **전체 `test`** [실행 (c)]: CoreApplicationTests 1, LayerArchitectureTest 2, 프로브 9 가 통과했다. Convention·Module ArchUnit 6개는 `@Disabled` 로 건너뛰었다. 실패는 0이다.
- **§4.3 `[B 전제]` 서술** [정적]
  - "B 에서 `TossHoldingsClient` 는 같은 `toss` 모듈의 internal" 은 `architecture-proposal.md:75`(`toss/holdings/client/`)·`:89-90` 과 일치한다.
  - 예시 경로 `toss/holdings/service/HoldingsSnapshotService` 는 B 트리의 `holdings/{client,service,…}` 와 일치한다.
  - A′ 쪽 이름 `TossHoldingsFetcher` 는 architecture `:60·:67·:68`, README `:120` 과 일치한다.
- **§2.4 After ↔ examples** [정적]
  - catch 순서 `RestClientResponseException` → `ResourceAccessException` → `RestClientException` 과 각 분기의 예외 타입이 두 examples 파일과 같다. 두 파일은 패키지·import 만 다르다(`diff` 확인).
  - `:319` 의 "허용 오차(1e-6) 밖·NaN 이면 Unavailable, 통과 값만 clamp" 는 파일 `:23, :54-57`(`!(raw >= -tol && raw <= 1+tol)` 형태라 NaN 도 걸림)과 D13 에 맞는다.
- **§8 표**: `:751-772` 는 22행이고 `decisions-needed.md:129` 의 "§8 표 22행" 과 같다. 본문의 "05/06 관계" 줄 중 Round 3 m3 에서 좁았던 두 곳이 이제 §8 과 같다. 05 §5.7 행의 내용 누락 → m1.
- **§2.4 표·예외 규칙과 §5.1 표의 정합**: §5.1 `:546` 의 "스케줄러·비동기 진입점 → 그 진입점의 catch 블록" 은 §2.4 예외 규칙의 "HTTP 응답으로 변환하지 않는 진입점" 과 같은 범위다. `:547` 의 "그 외 → 로그 없이 throw" 는 "그 밖의 호출 측은 도메인 예외만 받는다" 와 모순이 없다.
