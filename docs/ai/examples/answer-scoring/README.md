# 예시 — "답안 채점" 기능의 구조 선택지별 형태

> **컴파일 대상이 아니다.** `docs/` 아래라 Gradle 소스셋에 포함되지 않는다. 비교와 판단을 돕는 용도다.
> 각 파일 첫 줄 주석에 **실제로 놓일 경로**와 **AS-IS 대비 변경점**을 적었다.
> 관련 문서: [architecture-proposal.md](../../architecture-proposal.md) §2 · [conventions-proposal.md](../../conventions-proposal.md)
> 개정: Round 1 리뷰([reviews/examples-round-1.md](../../reviews/examples-round-1.md)) 반영 — A 를 A′ 로 재작성(C2), 세 선택지에 같은 예외 분류(catch 3개)·빈 응답·점수 범위 정책(C1, M3, m4), 누락된 마이그레이션 단계(M1), C 의 세 번째 배치(M2).

## 0. 실행 검증

| 선택지 | 방법 | 결과 |
|---|---|---|
| A′ | 전용 worktree 의 `src/main` 에 `option-a-layered/` 파일을 실제 경로로 복사, `common/config/RestClientConfig.java` 삭제, `similarity.*` 키 변경 → `./gradlew build` | **BUILD SUCCESSFUL** (컴파일 + `CoreApplicationTests`). ArchUnit: R1·R3·R4·R4b·R6·R7 통과, R5 1건(`getRandom` — `FIX-R5` 미적용분), R2 11건(B 전용 규칙 — A′ 에서는 교체 대상) |
| B | Round 1 리뷰어가 같은 방식으로 적용 | 컴파일·기존 테스트·R1·R2·R3·R4·R6·R7 통과, 실제 DB 로 E2E 채점 정상 (examples-round-1 "검증한 주장"). 이후 `SimilarityScorer` 에 빈 응답·`Math.clamp` 를 추가 (Java 21 API) |
| C | 미실행 — 여러 파일이 "표시 생략" 이라 그대로는 컴파일 불가 | — |

## 1. 왜 이 기능인가

`POST /api/v1/answers` (답안 채점) 는 현재 코드의 구조 문제가 **한 흐름에 모두** 모여 있다.

| 문제 | AS-IS 위치 | current-state.md |
|---|---|---|
| 기능 간 직접 의존 (리포지토리·엔티티·예외) | `answer/service/AnswerService.java:9-11,24`, `answer/dto/AnswerResponse.java:4` | F6, §3.5 |
| 외부 호출 예외 변환이 서비스에 위치 + 분류 빈틈 (read timeout·역직렬화 실패 → 500) | `AnswerService.java:41-56` | §3.3(f) |
| `@Transactional` self-invocation | `AnswerService.java:28,35` | F4 |
| DTO 가 도메인 규칙 실행 (`Grade.fromScore`) | `AnswerResponse.java:37` | §1.2 |
| `@RequestBody` 누락으로 항상 500 | `infra/similarity/SimilarityClient.java:10` | F1 |
| `common` → `infra` 역방향 의존 | `common/config/RestClientConfig.java:3` | §2 |

`sentence` 출제(`GET /sentences/random`)는 단일 기능 안에서 끝나 선택지 간 차이가 거의 드러나지 않아 제외했다.

## 2. 세 선택지 공통 (구조와 무관하게 적용)

예시는 **구조 차이만** 비교되도록, 다음을 세 선택지 모두에 똑같이 적용했다. 코드는 B 의 `SimilarityScorer` 가 기준이고, A′ 는 같은 본문을 다른 패키지에, C 는 어댑터에 둔다.

| 항목 | 내용 | 근거 |
|---|---|---|
| F1 | HTTP Interface 파라미터에 `@RequestBody` | current-state §7.2 P8 |
| 외부 예외 분류 | `RestClientResponseException`(4xx→`RequestInvalid`, 5xx→`Unavailable`) / `ResourceAccessException`(연결 오류→`Unavailable`) / **`RestClientException`(→`Unavailable`)** | `SimpleClientHttpRequestFactory` 에서 **read timeout 은 `RestClientException`** 으로 온다 — 세 번째 catch 가 없으면 타임아웃이 500 (examples-round-1 C1 실행: 3초 지연에 catch 2개 = 500, 3개 = 503) |
| 빈 응답 | 200/204 빈 본문 → HTTP Interface 가 `null` 반환 → `Unavailable` | examples-round-1 m4 실행 (없으면 NPE → 500) |
| 점수 범위 | `Math.clamp(score, 0.0, 1.0)` — 부동소수 오차(1.0000001) 보정 | examples-round-1 M3. C 의 `AnswerScore` 불변식은 보정 후 값만 받음 |
| 등급 계산 위치 | DTO 밖 (`AnswerService` 가 `Grade.fromScore` 호출) | conventions §3.2 |
| 기능 간 전달 | 엔티티 대신 record `SentenceDetail(id, englishText, koreanRef)` | conventions §3.1·§3.3, R5 |
| 설정 | `@Value` → `@ConfigurationProperties` record + `@DefaultValue`. `RestClient.builder()` 정적 호출 유지 (Builder 빈 없음) | current-state §4.6 |
| 로그 | throw 직전 `log.error` 제거, 키 이름 `answerLength` | conventions §5.1, §1.3 |
| 생략 | `@Schema` 등 Swagger 어노테이션, `AnswerApi`, `SubmitAnswerRequest`, 예외 클래스 본문 (AS-IS 와 동일) | — |

알려진 빈틈 (세 선택지 공통, 미해결): 상태 코드 600 이상은 `DefaultResponseErrorHandler` 가 에러로 보지 않아 **성공으로 처리**된다 (examples-round-1 M4 실행). 2xx 가 아닌 응답을 엄격히 거부할지는 decisions-needed.md D13 에 남긴다.

## 3. 파일 목록과 비교

### 선택지 A′ — 레이어드 + 기능 간 접근 규칙 ([option-a-layered/](./option-a-layered/))

| 파일 (예시 위치) | 실제 경로 | 상태 |
|---|---|---|
| `sentence/SentenceDetail.java` | `sentence/dto/` | 신규 |
| `sentence/SentenceService.java` | `sentence/service/` | 수정 (`getActive` → record) |
| `infra-similarity/SimilarityScorer.java` | `infra/similarity/` | 신규 — 예외 변환 전담 (B 와 본문 동일) |
| `infra-similarity/SimilarityClientConfig.java` | `infra/similarity/` | 신규 |
| `infra-similarity/SimilarityClient.java` | `infra/similarity/` | 수정 (F1 — 구조 무관) |
| `answer/AnswerService.java` | `answer/service/` | 수정 |
| `answer/AnswerResponse.java` | `answer/dto/` | 수정 |
| (파일 없음) `common/config/RestClientConfig.java` | — | **삭제** (남기면 `similarityClient` 빈 중복으로 기동 실패) |
| (파일 없음) `application.properties` | — | 수정 (`similarity.timeout-ms` → `connect-timeout`, `read-timeout`) |

### 선택지 B — 기능 모듈 + 공개 API ([option-b-modular/](./option-b-modular/)) — **추천**

| 파일 | 실제 경로 | 상태 |
|---|---|---|
| `sentence/SentenceReader.java` | `sentence/` (공개) | 신규 |
| `sentence/SentenceDetail.java` | `sentence/` (공개) | 신규 |
| `similarity/SimilarityScorer.java` | `similarity/` (공개) | 신규 — 예외 변환 전담 |
| `similarity/client/SimilarityClient.java` | `similarity/client/` (internal) | 이동 + 수정 (dto 2개를 중첩 record 로 흡수 — 흡수 대신 이동도 가능) |
| `similarity/config/SimilarityClientConfig.java` | `similarity/config/` (internal) | 신규 |
| `answer/AnswerService.java` | `answer/service/` | 수정 |
| `answer/AnswerResponse.java` | `answer/dto/` | 수정 |
| (표시 생략) `SimilarityException.java` | `similarity/exception/` (internal) | 이동만 (`infra.similarity.exception` → `similarity.exception`) |
| (파일 없음) `common/config/RestClientConfig.java` | — | **삭제** |
| (파일 없음) `application.properties` | — | 수정 (A′ 와 같은 키) |

### 선택지 C — 헥사고날 ([option-c-hexagonal/](./option-c-hexagonal/))

| 파일 | 실제 경로 | 상태 |
|---|---|---|
| `domain/AnswerScore.java` | `answer/domain/` | 신규 (순수 값 객체) |
| `domain/Sentence.java` | `sentence/domain/` | 신규 (JPA 와 분리된 도메인 모델) |
| `application/port/in/ScoreAnswerUseCase.java` | `answer/application/port/in/` | 신규 |
| `application/port/out/LoadSentencePort.java` | `answer/application/port/out/` | 신규 |
| `application/port/out/ScoreSimilarityPort.java` | `answer/application/port/out/` | 신규 |
| `application/service/ScoreAnswerService.java` | `answer/application/service/` | 신규 (AS-IS `AnswerService` 대체) |
| `adapter/in/web/AnswerController.java` | `answer/adapter/in/web/` | 이동 + 수정 (package-private → `@WebMvcTest` 는 같은 패키지 테스트에서만) |
| `adapter/out/persistence/SentencePersistenceAdapter.java` | `sentence/adapter/out/persistence/` | 신규 |
| `adapter/out/similarity/SimilarityHttpAdapter.java` | `answer/adapter/out/similarity/` | 신규 |
| (표시 생략) `SentenceJpaEntity`, `SentenceJpaRepository`, `AnswerApi`·웹 DTO 이동, `AnswerResponse.from(Result)`, `SimilarityClient`(중첩 record 형태)·`SimilarityException` 의 `answer/adapter/out/similarity/` 이동, `RestClientConfig` 삭제 | 각 어댑터 패키지 | 개명·이동·삭제 |

- 신규 8개 중 2개(`Sentence`, `SentencePersistenceAdapter`)는 `sentence` 쪽, 이동 1개 별도. AS-IS `answer` 전체는 7파일.

## 4. 핵심 차이 — `AnswerService` 의 의존 목록

| | AS-IS | A′ | B | C |
|---|---|---|---|---|
| 타 기능 `repository` | `SentenceRepository` | — | — | — (포트 경유) |
| 타 기능 엔티티·domain | `SentenceEntity` | — | — | `sentence.domain.Sentence` (포트 반환 타입) |
| 타 기능 공개 타입 | — | `sentence.service.SentenceService`, `sentence.dto.SentenceDetail` | `sentence.SentenceReader`, `sentence.SentenceDetail` | — |
| 타 기능 예외 | `SentenceException` | — | — | `SentenceException` ¹ |
| 외부 시스템 타입 | `SimilarityClient`, `SimilarityRequest`, `SimilarityResponse`, `SimilarityException`, `HttpStatusCodeException`, `ResourceAccessException` | `infra.similarity.SimilarityScorer` 1개 | `similarity.SimilarityScorer` 1개 | `ScoreSimilarityPort` 1개 |
| 프로젝트 내부 import 수 (`import com.tmi.*`) | 9 | 6 | 6 | 6 |
| `@Transactional` | self-invocation (무효) | 없음 — `SentenceService.getActive` 가 담당 | 없음 — `SentenceReader.getActive` 가 담당 | 없음 — 어댑터가 담당 |

¹ 구조 때문이 아니라 예시의 선택. `answer/exception/AnswerException` 이 이미 있으므로 answer 소유 예외로 바꿀 수 있다.

**A′ 와 B 의 코드 차이는 import 경로뿐이다.** 차이는 공개 표면의 크기에 있다: A′ 에서는 `answer` 가 `sentence.service`·`sentence.dto` 의 **어떤 타입이든** 쓸 수 있다(웹 응답 DTO `SentenceResponse` 포함). B 에서는 base package 에 골라 둔 타입만 쓸 수 있고, 넓히려면 파일을 옮기는 diff 가 생긴다. 경계 규칙은 둘 다 ArchUnit 조건 1개다 (architecture-proposal.md §2 A′).

## 5. 예시가 드러내는 각 선택지의 약점

| 선택지 | 약점 | 예시 위치 |
|---|---|---|
| A′ | `sentence.service`·`sentence.dto` 전체가 암묵적으로 공개 — `answer` 가 `SentenceResponse`(웹 응답)를 재사용해도 규칙 위반이 아니다 | `option-a-layered/answer/AnswerService.java` 10-11행 import |
| B | `SentenceDetail` 이 엔티티 필드 일부를 복제 (A′ 도 동일) | `option-b-modular/sentence/SentenceDetail.java` |
| B | `SentenceReader` 가 internal 예외(`SentenceException.NotFound`)를 경계 밖으로 던짐 — 호출 측은 catch 하지 않는 전제 | `option-b-modular/sentence/SentenceReader.java` javadoc |
| B | `infra/` 해체 → 02 §3.2·05 §1.3 규칙 변경 | architecture-proposal.md §0 표 |
| C | 이 예시 배치는 **`answer ↔ sentence` 순환**: `LoadSentencePort` 가 `sentence.domain.Sentence` 를 import(`LoadSentencePort.java:5`)하고, `sentence` 의 어댑터가 `answer` 의 포트를 구현(`SentencePersistenceAdapter.java:5`) — R1·R2 위반 | `option-c-hexagonal/` |
| C | 한 흐름에 신규 8 + 표시 생략 이동 다수 — AS-IS `answer` 전체가 7파일 | §3 C 표 |

**C 의 세 번째 배치** (순환 없음 — 예시 코드에는 없음): 포트가 `answer` 소유 타입(예: `SentenceForScoring` record)을 반환하고, 어댑터를 `answer/adapter/out/sentence/` 에 두어 `sentence` 의 인바운드 포트(유스케이스)나 공개 조회 API 를 호출한다. JPA 를 모르고 순환도 없지만 비용은 포트 1 + 어댑터 1 + `sentence` 인바운드 포트 1 이 추가된다. 이 어댑터가 호출하는 `sentence` 공개 조회 API 는 B 의 `SentenceReader` 와 같은 모양이다 — 즉 C 를 순환 없이 하려면 B 와 같은 경계가 먼저 필요하다.

## 6. 이 예시로 판단할 질문

1. `answer` 가 `sentence` 의 서비스·DTO 아무거나 써도 괜찮은가? → 괜찮다면 A′, 공개 타입을 파일 위치로 고정하고 싶다면 B.
2. Similarity 를 `answer` 외의 기능이 쓸 계획이 있는가? → A′·B 모두 `SimilarityScorer` 가 중복을 막는다 (차이 없음). B 는 이를 독립 모듈로 둔다.
3. Similarity·Toss 를 다른 구현으로 갈아끼울 계획이 있는가? → 있다면 C 의 포트가 의미를 갖는다. 현재 문서(01 기획, toss 설계)에 그런 계획은 없다.
