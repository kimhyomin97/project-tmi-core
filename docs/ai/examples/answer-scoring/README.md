# 예시 — "답안 채점" 기능의 구조 선택지별 형태

> **컴파일 대상이 아니다.** `docs/` 아래라 Gradle 소스셋에 포함되지 않는다. 비교와 판단을 돕는 용도다.
> 각 파일 첫 줄 주석에 **실제로 놓일 경로**와 **AS-IS 대비 변경점**을 적었다.
> 관련 문서: [architecture-proposal.md](../../architecture-proposal.md) §2 · [conventions-proposal.md](../../conventions-proposal.md)

## 1. 왜 이 기능인가

`POST /api/v1/answers` (답안 채점) 는 현재 코드의 구조 문제가 **한 흐름에 모두** 모여 있다.

| 문제 | AS-IS 위치 | current-state.md |
|---|---|---|
| 기능 간 직접 의존 (리포지토리·엔티티·예외) | `answer/service/AnswerService.java:10-11,24`, `answer/dto/AnswerResponse.java:4` | F6, §3.5 |
| 외부 호출 예외 변환이 서비스에 위치 + 분류 빈틈 | `AnswerService.java:41-56` | §3.3(f) |
| `@Transactional` self-invocation | `AnswerService.java:28,35` | F4 |
| DTO 가 도메인 규칙 실행 (`Grade.fromScore`) | `AnswerResponse.java:37` | §1.2 |
| `@RequestBody` 누락으로 항상 500 | `infra/similarity/SimilarityClient.java:10` | F1 |
| `common` → `infra` 역방향 의존 | `common/config/RestClientConfig.java:3` | §2 |

`sentence` 출제(`GET /sentences/random`)는 단일 기능 안에서 끝나 선택지 간 차이가 거의 드러나지 않아 제외했다.

## 2. 세 선택지 공통 (구조와 무관하게 적용)

예시는 **구조 차이만** 비교되도록, 다음 컨벤션 수정은 세 선택지 모두에 똑같이 적용했다.

- F1 수정: HTTP Interface 파라미터에 `@RequestBody`
- 외부 예외 분류: `HttpStatusCodeException` → `RestClientResponseException` (비표준 상태 코드 포함) + 연결 오류 `ResourceAccessException`
- 등급 계산은 DTO 밖에서 (`Grade.fromScore` 를 서비스/도메인이 호출)
- 설정: `@Value` → `@ConfigurationProperties` record. `RestClient.builder()` 정적 호출은 유지 — Boot 4 + 현재 의존성에는 `RestClient.Builder` 빈이 없다 (current-state.md §4.6)
- 외부 호출 직전 `log.error` 제거 — 예외 로그는 `GlobalExceptionHandler` 단일 지점 (conventions-proposal.md §5)
- 생략: `@Schema` 등 Swagger 어노테이션, `AnswerApi`, `SubmitAnswerRequest`, 예외 클래스 본문 (AS-IS 와 동일)

## 3. 파일 목록과 비교

### 선택지 A — 레이어드 정비 ([option-a-layered/](./option-a-layered/))

| 파일 | 실제 경로 | 상태 |
|---|---|---|
| `SimilarityClient.java` | `infra/similarity/` | 수정 (F1) |
| `SimilarityClientConfig.java` | `infra/similarity/` | 신규 (`RestClientConfig` 에서 분리) |
| `SentenceService.java` | `sentence/service/` | 수정 (`getActive` 추가) |
| `AnswerService.java` | `answer/service/` | 수정 |
| `AnswerResponse.java` | `answer/dto/` | 수정 (팩토리 시그니처) |

### 선택지 B — 기능 모듈 + 공개 API ([option-b-modular/](./option-b-modular/)) — **추천**

| 파일 | 실제 경로 | 상태 |
|---|---|---|
| `sentence/SentenceReader.java` | `sentence/` (공개) | 신규 |
| `sentence/SentenceDetail.java` | `sentence/` (공개) | 신규 |
| `similarity/SimilarityScorer.java` | `similarity/` (공개) | 신규 — 예외 변환 전담 |
| `similarity/client/SimilarityClient.java` | `similarity/client/` (internal) | 이동 + 수정 (dto 2개 흡수) |
| `similarity/config/SimilarityClientConfig.java` | `similarity/config/` (internal) | 신규 |
| `answer/AnswerService.java` | `answer/service/` | 수정 |
| `answer/AnswerResponse.java` | `answer/dto/` | 수정 |
| (표시 생략) `similarity/exception/SimilarityException.java` | `similarity/exception/` (internal) | 이동만 (`infra.similarity.exception` → `similarity.exception`), 본문 동일 |

### 선택지 C — 헥사고날 ([option-c-hexagonal/](./option-c-hexagonal/))

| 파일 | 실제 경로 | 상태 |
|---|---|---|
| `domain/AnswerScore.java` | `answer/domain/` | 신규 (순수 값 객체) |
| `domain/Sentence.java` | `sentence/domain/` | 신규 (JPA 와 분리된 도메인 모델) |
| `application/port/in/ScoreAnswerUseCase.java` | `answer/application/port/in/` | 신규 |
| `application/port/out/LoadSentencePort.java` | `answer/application/port/out/` | 신규 |
| `application/port/out/ScoreSimilarityPort.java` | `answer/application/port/out/` | 신규 |
| `application/service/ScoreAnswerService.java` | `answer/application/service/` | 신규 (AS-IS `AnswerService` 대체) |
| `adapter/in/web/AnswerController.java` | `answer/adapter/in/web/` | 이동 + 수정 |
| `adapter/out/persistence/SentencePersistenceAdapter.java` | `sentence/adapter/out/persistence/` | 신규 |
| `adapter/out/similarity/SimilarityHttpAdapter.java` | `answer/adapter/out/similarity/` | 신규 |
| (표시 생략) `SentenceJpaEntity`, `SentenceJpaRepository`, 웹 DTO 이동, `AnswerResponse.from(Result)` | 각 어댑터 패키지 | 개명·이동 |

## 4. 핵심 차이 — `AnswerService` 의 의존 목록

| | AS-IS | A | B | C |
|---|---|---|---|---|
| 타 기능 `repository` | `SentenceRepository` | — | — | — (포트 경유) |
| 타 기능 엔티티 | `SentenceEntity` | `SentenceEntity` | — (`SentenceDetail`) | — (`Sentence` 도메인 모델) |
| 타 기능 예외 | `SentenceException` | — | — | `SentenceException` (C 예시의 서비스가 직접 throw) |
| 외부 시스템 타입 | `SimilarityClient`, `SimilarityRequest`, `SimilarityResponse`, `SimilarityException`, `HttpStatusCodeException`, `ResourceAccessException` | `SimilarityClient`, `SimilarityRequest`, `SimilarityException`, `RestClientResponseException`, `ResourceAccessException` | `SimilarityScorer` 1개 | `ScoreSimilarityPort` 1개 |
| 프로젝트 내부 import 수 (`import com.tmi.*`) | 9 | 8 | 6 | 6 |
| `@Transactional` | self-invocation (무효) | 없음 — `SentenceService.getActive` 가 담당 | 없음 — `SentenceReader.getActive` 가 담당 | 없음 — 어댑터가 담당 |

## 5. 예시가 드러내는 각 선택지의 약점

| 선택지 | 약점 | 예시 위치 |
|---|---|---|
| A | `AnswerService`·`AnswerResponse` 가 여전히 `SentenceEntity` import — 엔티티 컬럼 변경이 `answer` API 로 전파 | `option-a-layered/AnswerResponse.java` 4행 |
| A | 외부 예외 변환 로직이 이 서비스에 남음 → Similarity 를 쓰는 기능이 늘면 복제 | `option-a-layered/AnswerService.java` `callSimilarity` |
| B | `SentenceDetail` 이 엔티티 필드 일부를 그대로 복제 | `option-b-modular/sentence/SentenceDetail.java` |
| B | `SentenceReader` 가 internal 예외(`SentenceException.NotFound`)를 경계 밖으로 던짐 — 호출 측은 catch 하지 않는 전제 | `option-b-modular/sentence/SentenceReader.java` javadoc |
| C | `sentence` 의 영속성 어댑터가 `answer` 의 포트를 구현 → **`sentence → answer` 역방향 의존 발생**. 피하려면 포트를 공유 패키지로 빼거나 `answer` 쪽에 어댑터를 둬야 하는데, 그러면 `answer` 가 `sentence` 의 JPA 리포지토리를 다시 알게 된다 | `option-c-hexagonal/adapter/out/persistence/SentencePersistenceAdapter.java` 4행 |
| C | 한 기능에 파일 9개 이상 신규 — AS-IS `answer` 전체가 8파일 | §3 C 표 |

## 6. 이 예시로 판단할 질문

1. `AnswerResponse` 가 `SentenceEntity` 를 import 해도 괜찮은가? → 괜찮다면 A, 아니라면 B 이상.
2. Similarity 를 `answer` 외의 기능이 쓸 계획이 있는가? → 있다면 B 의 `SimilarityScorer` 가 중복을 막는다.
3. Similarity·Toss 를 다른 구현으로 갈아끼울 계획이 있는가? → 있다면 C 의 포트가 의미를 갖는다. 현재 문서(01 기획, toss 설계)에 그런 계획은 없다.
