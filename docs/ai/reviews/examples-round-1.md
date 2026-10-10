# examples 리뷰 — Round 1

> 대상: `docs/ai/examples/` 전체 (마지막 변경 커밋 `99aa1e3`, 리뷰 시점 HEAD `7207e31`). 대조 문서: `architecture-proposal.md` 는 **리뷰 중 작업 트리에 반영된 최신본(A′ 개정)**, `conventions-proposal.md`, `current-state.md`, ArchUnit 테스트, `src/main/**`.
> 검증 방법 (전용 worktree `scratchpad/wt-examples`, 리뷰 후 삭제. DB 는 SELECT 만 했다):
> 1. 정적 대조: 예시 23파일과 AS-IS `src/main` 을 줄 단위로 비교. import 수, 줄 번호, 파일 수를 다시 셌다.
> 2. **선택지 B 이식**: 예시 7파일을 `src/main` 에 복사했다. `SimilarityException` 은 패키지만 바꿔 이동했다. `infra/**` 와 `RestClientConfig` 는 삭제했다. 그 뒤 `compileJava` 와 `test` 를 돌렸다. ArchUnit R1·R2·R3·R6·R7 의 `@Disabled` 도 풀고 실행했다.
> 3. **선택지 A 이식**: 예시 5파일을 복사했다. 먼저 README 표대로(`RestClientConfig` 유지) 돌린 다음, `RestClientConfig` 를 지우고 다시 돌렸다. R1·R3·R6·R7 의 `@Disabled` 를 풀었다.
> 4. 동작 프로브: 가짜 Similarity 서버(python, 127.0.0.1:18765)를 띄우고 `@SpringBootTest` + MockMvc 로 `POST /api/v1/answers` 를 실제 DB 문장 `…0001` 에 대해 호출했다. 응답은 정상 / 400 / 503 / 3초 지연(read-timeout 2s) / 없는 ID 다섯 가지로 했다. `SimilarityScorer` 단위 프로브는 `MockRestServiceServer` 로 400·500·599·600·깨진 JSON·`{}`·빈 본문·204·1.5·text/plain·연결 거부 응답을 줬다. read timeout 은 `SimpleClientHttpRequestFactory` 와 `JdkClientHttpRequestFactory` 의 예외 체인을 비교했다.
> 5. 선택지 C 는 생략된 파일(`SentenceJpaEntity`, `AnswerApi` 이동, `AnswerResponse.from(Result)` 등)이 많아 이식하지 않았다. 정적으로만 검토했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | import 수(9/8/6/6), B 의 R2 통과는 실행으로 맞았다. 그러나 줄 번호·파일 수가 여럿 틀렸고, "타임아웃 → `ResourceAccessException`", "비표준 상태 코드 포함" 은 실행 결과와 다르다 |
| 구체성 | 3 | B 는 그대로 옮겨도 컴파일·기동·채점이 된다(실행). 하지만 README 표에 `RestClientConfig` 삭제와 `application.properties` 키 변경이 빠져 있다. 표대로 옮기면 A·B 둘 다 기동에 실패한다 |
| 일관성 | 2 | A 예시가 최신 architecture-proposal 의 A′ 정의와 다르다. A 와 B 의 예외 분류가 서로 달라 README §2 "공통 적용" 이 거짓이다. C 의 `AnswerScore` 만 범위 검증을 한다 |
| 판단 가능성 | 2 | A 는 실제 결함(타임아웃 → 500)을 가진 채, 정의에서 이미 빠진 약점(엔티티 공유)으로 비교된다. C 는 한 가지 배치만 보여 준다. 결정자가 잘못 기운 비교로 판단하게 된다 |

## 치명 (승인 차단)

- **[C1] `option-a-layered/AnswerService.java:36-49` 와 README §2 — A 에만 `RestClientException` catch 가 없다. 그래서 Similarity read timeout 이 500 으로 나간다. "세 선택지 공통" 이라는 전제가 깨진다.**
  - 근거 (실행):
    - 현재 설정(`SimpleClientHttpRequestFactory`)에서 read timeout 은 `ResourceAccessException` 이 아니다. `RestClientException("Error while extracting response …") → SocketTimeoutException` 으로 올라온다. 비교용 `JdkClientHttpRequestFactory` 는 `ResourceAccessException → HttpTimeoutException` 을 낸다.
    - E2E 3초 지연 요청 결과: **A = `500 TMI.COMMON.INTERNAL_ERROR`**, **B = `503 TMI.SIMILARITY.UNAVAILABLE`**. B 가 503 을 낸 것은 세 번째 catch(`SimilarityScorer.java:36`) 덕분이다.
    - `AnswerApi.java:93` 은 "타임아웃 → 503" 을 계약으로 문서화했다. 같은 결함이 AS-IS 에도 있지만 문서 세트(current-state §3.3(f))는 "역직렬화 실패" 만 적고 타임아웃을 놓쳤다.
    - conventions-proposal §2.4 는 "선택지 A 이면 서비스의 private 메서드에 둔다" 고 해서 catch 3개를 전제한다. README §2 도 "다음 컨벤션 수정은 세 선택지 모두에 똑같이 적용했다" 고 한다. 그런데 A 예시는 catch 가 2개다.
  - 요구 수정:
    - A 에 B 와 같은 catch 3개를 둔다. 아니면 A′ 정의대로 `infra/similarity/SimilarityScorer` 로 옮긴다(C2).
    - B·conventions §2.4 의 주석 `// 연결 거부·타임아웃`(RAE), `// 역직렬화 실패 등`(RCE)을 실제 분류로 고친다. "read timeout 은 SimpleClientHttpRequestFactory 에서 RestClientException 으로 온다 — 이 catch 를 지우면 503 → 500 회귀" 처럼 쓴다.
    - current-state §3.3(f) 에 "AS-IS read timeout → 500" 을 추가한다.
    - 회귀 테스트를 요구한다: 지연 응답 + 짧은 readTimeout → `SimilarityException.Unavailable`.

- **[C2] `option-a-layered/` 전체, README §4·§5·§6 — 예시가 최신 architecture-proposal 의 "A′" 와 다른 선택지를 보여 준다.**
  - 최신 architecture-proposal §2 A′ (작업 트리) 의 내용:
    - `SentenceService.getActive(id) → dto/SentenceDetail (record)`
    - 외부 예외 변환은 `infra/similarity/SimilarityScorer`
    - "세 선택지 모두 같은 컨벤션(R5, 외부 예외 변환 클래스 1개 …)"
    - §3.2 신규 목록 `infra/similarity/SimilarityScorer`, `sentence/dto/SentenceDetail`, `RestClientConfig(→삭제)`
  - 예시 A 의 내용:
    - `SentenceService.getActive` 가 `SentenceEntity` 를 반환한다 (`option-a-layered/SentenceService.java:31`). 이것은 R5 위반이다.
    - `AnswerResponse` 가 `SentenceEntity` 를 import 한다 (`:7`).
    - 예외 변환이 `AnswerService.callSimilarity` 안에 있다.
  - 그래서 README 의 A 관련 서술이 지금 정의된 A′ 에 맞지 않는다:
    - §5 의 A 약점 2건(엔티티 import, `callSimilarity` 복제)
    - §4 표의 A 열(타 기능 엔티티 `SentenceEntity`, 외부 시스템 타입 5개, import 8)
    - §6 질문 1 "SentenceEntity 를 import 해도 괜찮은가? → 괜찮다면 A"
  - architecture-proposal-round-1 리뷰도 "A 약점은 구조가 아니라 일부러 적용하지 않은 컨벤션 때문" 이라고 지적했다.
  - 요구 수정: `option-a-layered/` 를 A′ 로 다시 쓴다. 대상은 `SentenceDetail`(sentence/dto), `SimilarityScorer`(infra/similarity), `SentenceService.getActive → SentenceDetail` 이다. 이어서 README §3 A 표, §4 A 열, §5 A 약점, §6 질문 1 을 다시 계산한다. A′ 의 진짜 약점은 이것이다: 타 기능의 `service`·`dto` 전체가 암묵적으로 공개되고, 기능 쌍마다 ArchUnit 규칙이 필요하다 (architecture-proposal §4.1-1).

## 주요

- **[M1] README §3 A·B 표 — 마이그레이션 필수 변경 2건이 빠졌다. 표대로 옮기면 기동에 실패한다.**
  - 빠진 것: (1) `common/config/RestClientConfig.java` 삭제 (2) `application.properties` 의 `similarity.timeout-ms` → `connect-timeout`/`read-timeout`.
  - 실행 결과:
    - `RestClientConfig` 를 남기면 `BeanDefinitionOverrideException: Invalid bean definition with name 'similarityClient'` 가 난다 (A).
    - 키를 바꾸지 않으면 `IllegalArgumentException: ConnectTimeout must not be null` 이 난다 (B, `SimilarityClientConfig.java:28`).
  - 키 변경은 A 설정 파일 주석(`:23-24`)에만 있다.
  - B 주석 "`RestClientConfig` 는 Similarity·Toss 빈이 각 모듈로 빠지면 삭제된다" 는 WIP 패치 기준이다. 커밋된 `src/main` 에는 Similarity 빈만 있으므로 B 를 적용하는 시점에 바로 삭제해야 한다.
  - 요구 수정:
    - 두 표에 "삭제 `common/config/RestClientConfig`", "수정 `application.properties`(키 2개)" 행을 추가한다.
    - `SimilarityProperties` 에 `@DefaultValue("1s")`/`@DefaultValue("2s")` 를 주거나, `@Validated` + `@NotNull` 로 설정 누락이 원인 불명 NPE 성 메시지가 되지 않게 한다.

- **[M2] `option-c-hexagonal/` + README §5 C 행 — C 를 가장 불리한 배치 하나로만 보여 준다. §4 표도 그 배치의 산물을 구조 비용으로 센다.**
  - (a) 예시 C 에는 모듈 간 양방향 의존이 생긴다:
    - `LoadSentencePort.java:5` 가 `sentence.domain.Sentence` 를 import 한다 (answer → sentence internal).
    - `ScoreAnswerService.java:10` 이 `sentence.exception.SentenceException` 을 import 한다.
    - `SentencePersistenceAdapter.java:5` 가 `answer.application.port.out` 을 import 한다 (sentence → answer internal).
    - 결과적으로 answer ↔ sentence **순환**이다. 정적으로 보면 R1·R2 를 둘 다 위반한다. README 는 "역방향 의존" 이라고만 적었다.
  - (b) README §5 와 architecture-proposal §2 C 는 "포트 공유 패키지" 또는 "answer 쪽 어댑터 = JPA 리포지토리를 앎" 두 갈래만 제시한다. 그러나 표준적인 세 번째 배치가 있다:
    - 포트가 answer 소유 타입(예: `SentenceForScoring` record)을 반환한다.
    - 어댑터를 answer 의 `adapter/out` 에 두고, sentence 의 인바운드 포트(유스케이스)나 공개 조회 API 를 호출한다.
    - 이 배치는 B 의 `SentenceReader` 와 같은 모양이고 JPA 를 모르며 순환도 없다.
  - (c) §4 표 C 열의 "타 기능 예외 `SentenceException`" 은 구조 때문이 아니다. 예시가 answer 소유 예외(`AnswerException` 클래스가 이미 있음 — `answer/exception/AnswerException.java`)를 쓰지 않았기 때문이다. 또 "타 기능 엔티티 —" 라고 했지만 실제로는 타 기능 `domain` 타입에 의존한다.
  - 요구 수정: §5 에 세 번째 배치와 그 비용(포트 + 어댑터 + sentence 인바운드 포트 1개)을 추가한다. §4 C 열에 "타 기능 domain 타입 `Sentence`" 를 표시하고, `SentenceException` 은 예시 선택이라고 각주를 단다. 이 결과는 architecture-proposal §2 C 에도 반영한다.

- **[M3] `option-c-hexagonal/domain/AnswerScore.java:6-8` — C 만 점수 범위 검증을 한다. 1.0 을 조금 넘는 점수가 500 이 된다.**
  - Similarity 서버는 Python/FastAPI + sentence-transformers 다 (docs/public/02_설계.md:31). 코사인 유사도는 부동소수 오차로 `1.0000001` 같은 값을 낼 수 있다. 그러면 `IllegalArgumentException` 이 나고 catch-all 500 이 된다. 완벽한 답안에서 장애가 나는 셈이다.
  - A·B 는 1.5 를 그대로 통과시킨다 (실행: `PROBE score1.5 -> 1.5`). 세 선택지의 동작이 서로 다르다 (README §2 위반). 범위 밖 점수를 어떻게 처리할지 정한 문서도 없다.
  - 요구 수정: 범위 정책(클램프 / `SimilarityException.Unavailable` / 거부)을 정해 세 선택지에 같은 위치에서 적용한다. 검증 위치(도메인 vs scorer)는 구조 차이로 보여 줘도 좋지만 동작은 같아야 한다.

- **[M4] README §2 "외부 예외 분류 … (비표준 상태 코드 포함)", `SimilarityScorer.java:30` 주석 — 근거가 실행 결과와 다르다.**
  - `MockRestServiceServer` 실행 결과:
    - 599 → `HttpServerErrorException` (이미 `HttpStatusCodeException` 의 하위라 AS-IS 도 잡는다)
    - 600 → **에러로 보지 않고 성공 처리** (`PROBE 600 -> 0.3`)
  - 기본 `DefaultResponseErrorHandler` 아래에서는 `UnknownHttpStatusCodeException` 경로가 이 두 경우 모두 나오지 않는다. catch 를 `RestClientResponseException` 으로 넓혀서 생기는 실익이 README 가 말하는 것보다 작다. 실익은 C1 의 세 번째 catch 에 있다.
  - 600 이상이 조용히 성공 처리되는 빈틈은 어디에도 적혀 있지 않다.
  - 요구 수정: 근거 문장을 실행 결과로 바꾼다 (conventions §2.4 의 "(estimated)" 근거도 함께). 2xx 가 아닌 응답을 엄격히 처리할지 결정 항목으로 남긴다.

## 경미

- [m1] README §5 줄 번호:
  - "`option-a-layered/AnswerResponse.java` 4행" → `SentenceEntity` import 는 **7행**이다 (4행은 `package`).
  - "`SentencePersistenceAdapter.java` 4행" → `LoadSentencePort` import 는 **5행**이다 (4행은 빈 줄).
- [m2] README §5 C "한 기능에 파일 9개 이상 신규 — AS-IS `answer` 전체가 8파일":
  - AS-IS `answer` 는 **7파일**이다 (`controller/2, domain/1, dto/2, exception/1, service/1`).
  - C 표의 신규는 **8개**이고, 그중 2개(`Sentence`, `SentencePersistenceAdapter`)는 `sentence` 에 있다. 이동 1개가 따로 있다.
- [m3] README §1 표 "기능 간 직접 의존 (리포지토리·엔티티·예외) `AnswerService.java:10-11,24`" — 엔티티 import 는 `:9` 다. `9-11,24` 로 고친다.
- [m4] 응답 본문이 없는 200·204 는 `similarityClient.score(...)` 가 `null` 을 반환한다. 이어지는 `.score()` 에서 **NPE → 500** 이 난다 (실행: `PROBE 200-nobody/204 -> NullPointerException`). 세 선택지 공통이고, "호출자는 double 만 받는다"(`SimilarityScorer.java:3`) 는 계약과 어긋난다. null 응답을 `Unavailable` 로 바꾸는 한 줄을 넣는다.
- [m5] A 로그 키 `userAnswerLength`(`option-a-layered/AnswerService.java:37`)가 conventions §1.3 After(`answerLength`)·B 와 다르다. 사소하지만 README §2 의 "같은 컨벤션" 과 어긋난다.
- [m6] C 표(§3)의 "표시 생략" 행에 `SimilarityClient`(+요청·응답 record)·`SimilarityException`·`AnswerApi` 의 `answer.adapter.*` 이동이 없다. `SimilarityHttpAdapter.java:18` 은 B 식 중첩 record(`SimilarityClient.SimilarityRequest`)를 전제하는데 그 점도 적혀 있지 않다.
- [m7] C `AnswerController` 는 package-private 이다 (`adapter/in/web/AnswerController.java:17`). conventions §6.3 이 권하는 `@WebMvcTest(AnswerController.class)` 는 같은 패키지의 테스트에서만 쓸 수 있다. C 를 고르면 테스트 컨벤션도 바뀐다는 점을 적는다.
- [m8] B `SimilarityClientConfig.java:2` "내용은 A 와 같다. 차이는 패키지뿐" — 실제로는 가시성(클래스·record package-private)도 다르다. 실행상 문제는 없다(아래 확인). 문구만 고친다.

## 이전 라운드 지적 해소 여부

- examples 는 Round 1 이다. 관련 지적인 architecture-proposal-round-1 [M6](C 트리가 examples 와 모순)은 architecture-proposal 쪽을 examples 에 맞춰 해소했다(작업 트리 §2 C). 다만 맞춘 대상인 examples 의 배치 자체에 M2 문제가 있다. 같은 리뷰의 "A 약점은 구조가 아닌 컨벤션 미적용 때문" 지적은 architecture-proposal 에만 반영됐고 examples 에는 반영되지 않았다 → C2.

## 검증한 주장 (문제 없음 확인)

- **B 이식 [실행]**:
  - 예시 그대로 `compileJava` 가 성공했다.
  - 키 변경 후 `test` 전체가 통과했다: `CoreApplicationTests` 기동, R1·R2·R3·R4·R4b·R6·R7 통과(`@Disabled` 해제). R5 만 기존 `SentenceService.getRandom` 위반으로 남는다.
  - package-private `@Configuration` 클래스·중첩 `@ConfigurationProperties` record 의 `@EnableConfigurationProperties` 등록, `Duration` 바인딩(`1s`/`2s`), package-private `SentenceDetail.from` 이 모두 동작했다.
- **B E2E [실행]**:
  - 정상 → 200 `{score:0.83, grade:GOOD}`, 요청 본문 `{"answer":…,"reference":…}` 전송 확인 (F1 수정 동작)
  - 400 → 502 `REQUEST_INVALID`
  - 503 → 503 `UNAVAILABLE`
  - 3초 지연 → 503 (readTimeout 2s 적용)
  - 없는 ID → 404 `SENTENCE_NOT_FOUND`
- **A 이식 [실행]**: `RestClientConfig` 를 지우면 `test` 가 통과한다. R1·R3·R4·R6·R7 통과로 architecture-proposal §4.3 단계 1 완료 신호가 맞다. 정상·400·503·404 응답은 B 와 같다 (타임아웃만 다름 → C1).
- **`SimilarityScorer` 분류 [실행]**: 400 → `RequestInvalid`, 500·599 → `Unavailable`, 깨진 JSON·`{}`·`{"score":null}`·`text/plain` → `Unavailable`(세 번째 catch), 연결 거부 → `Unavailable`(RAE). catch 순서(하위 → 상위)도 맞다.
- **README §4 import 수**: AS-IS 9 / A 8 / B 6 / C 6 이 맞다. B 헤더 "AS-IS import 7개(sentence 3 + infra 4) 사라짐" 도 맞다. §4 의 `@Transactional` 행(조회는 다른 빈의 readOnly)은 R6 통과로 확인했다.
- **AS-IS 위치 인용**: `AnswerService.java:41-56`, `:28,35`, `AnswerResponse.java:4`, `:37`, `SimilarityClient.java:10`, `RestClientConfig.java:3` 이 모두 맞다.
- **첫 줄 "위치" 주석**: A 5개, B 7개, C 9개 모두 README §3 표와 일치하고 package 선언과도 맞다.
- **README 머리말** "docs/ 아래라 Gradle 소스셋에 포함되지 않는다": 맞다 (`src/*/java/**` 만 컴파일·Spotless 대상).
- **B 의 `SimilarityException` 이동**: "본문 동일, 패키지만" 이 맞다. `GlobalExceptionHandler` 는 `BaseException` 으로 처리하므로 수정할 필요가 없다.
