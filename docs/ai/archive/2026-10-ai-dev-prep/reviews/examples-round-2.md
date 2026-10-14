# examples 리뷰 — Round 2

> 대상: `docs/ai/proposals/examples/` 전체. 기준 커밋은 `3e136c6`이다. examples 의 마지막 변경 커밋은 `9a8a1f9` 이다. 대조 문서(`architecture-proposal.md`, `conventions-proposal.md`, `decisions-needed.md`, `docs/public/03_API명세.md`)도 `3e136c6` 기준으로 읽었다. 리뷰 도중 작업 브랜치가 `fc49d18` 까지 진행됐다. 이후 커밋에서 이미 해소된 사항은 따로 표시했다.
>
> 검증 방법: 전용 worktree 3개를 `3e136c6` 에서 만들었고, 리뷰 후 모두 삭제했다. DB 는 SELECT 만 했다.
> 1. **A′ 이식** (`wt-examples`): README §3 A′ 표대로 7파일을 실제 경로에 복사했다. `common/config/RestClientConfig.java` 는 삭제했다. `similarity.timeout-ms=2000` 은 `connect-timeout=1s`·`read-timeout=2s` 로 바꿨다. 그 뒤 `bash ./gradlew build` 를 돌렸다. 이어서 ArchUnit 의 `@Disabled` 를 모두 풀고 `test` 를 돌렸다.
> 2. **B 이식** (`wt-examples-b`): 표대로 7파일을 복사했다. `SimilarityException` 은 패키지만 바꿔 이동했다. `infra/**` 와 `RestClientConfig` 는 삭제했고 키도 바꿨다. 그 뒤 `build` 와 `test` 를 돌렸다(`@Disabled` 전부 해제). 이어서 키를 AS-IS(`timeout-ms`)로 되돌리고 `CoreApplicationTests` 를 다시 돌렸다(`@DefaultValue` 확인).
> 3. **동작 프로브**: 가짜 Similarity 서버(python, `127.0.0.1:18766`, chunked 요청 본문 지원)를 띄웠다. A′·B 각각에서 `@SpringBootTest(RANDOM_PORT)` + `HttpClient` 로 `POST /api/v1/answers` 를 실제 DB 문장 `00000000-…-0001` 에 대해 13가지 응답으로 호출했다. 응답 종류: 정상, 3초 지연, 200 빈 본문, 204, 1.5, 1.0000001, -0.2, NaN, `{"score":null}`, `{}`, 400, 503. 추가로 `SimilarityScorer` 빈을 직접 호출해 예외 cause 체인도 출력했다.
> 4. **C 정적 + 부분 컴파일** (`wt-examples-c`): C 예시 9파일(주의: C1)을 복사했다. 생략 파일(`AnswerApi`, 웹 DTO, `SentenceJpaEntity/Repository`, 중첩 record 형 `SimilarityClient`)은 최소 스텁으로 채웠다. 그 상태에서 `ModuleArchitectureTest`(R1~R3)를 돌렸다. 이어서 "포트가 answer 소유 record 를 반환" 하도록 최소로 바꾸고 다시 돌렸다(M1).
> 5. 줄 번호, 파일 수, import 수는 HEAD 파일을 `cat -n` 으로 다시 셌다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | A′·B 에 대한 서술(§0 결과, §2 정책, import 수, 줄 번호)은 실행과 모두 맞는다. 그러나 C 근거 파일 4개가 커밋에 없다(C1). C 의 "순환 없이 하려면 B 경계가 먼저 필요" 는 실행으로 반증된다(M1) |
| 구체성 | 4 | A′·B 는 README §3 표대로 옮기면 그대로 build·기동·채점된다(실행). B 표에 dto 2개 삭제 행이 없는 정도만 남는다 |
| 일관성 | 3 | A′·B 의 동작은 서로 같다(실행). 그러나 architecture-proposal §3.2 의 "신규 9" 가 README 의 8 과 다르다. conventions §2.4 는 HEAD 기준으로 A′ 와 모순된다. clamp 정책은 03 계약·D13 과 연결돼 있지 않다 |
| 판단 가능성 | 3 | A′ vs B 비교는 공정해졌다. 그러나 C 의 순환 해법을 실제보다 비싸게, 그리고 B 에 종속되는 것처럼 서술한다(M1). §6 질문 2 의 "차이 없음" 도 틀렸다(M3). 결정자가 C 와 A′ 를 과소평가하거나 과대평가하게 된다 |

## 치명 (승인 차단)

- **[C1] `.gitignore:25` `out/` — C 예시 9파일 중 4파일이 커밋되지 않았다. README §3·§5 와 architecture-proposal §2 C 가 인용하는 근거가 HEAD 에 없다.**
  - 근거:
    - `git ls-files docs/ai/proposals/examples | grep option-c` 결과는 5파일(`AnswerController`, `ScoreAnswerUseCase`, `ScoreAnswerService`, `AnswerScore`, `Sentence`)이다.
    - `git check-ignore -v …/application/port/out/LoadSentencePort.java` 의 결과는 `.gitignore:25:out/` 다. IntelliJ 출력 디렉터리 규칙에 `application/port/out/` 과 `adapter/out/` 이 걸린다. 예외 규칙은 `!**/src/main/**/out/` 뿐이고 `docs/` 는 덮지 않는다.
    - `git show 3e136c6:docs/ai/proposals/examples/answer-scoring/option-c-hexagonal/application/port/out/LoadSentencePort.java` 의 결과는 `fatal: … does not exist`.
    - 빠진 파일: `LoadSentencePort`, `ScoreSimilarityPort`, `SentencePersistenceAdapter`, `SimilarityHttpAdapter`. 작성자의 작업 트리에는 `!!`(ignored) 상태로만 있다. `fc49d18` 에서도 여전히 0개다.
  - 영향:
    - README §5 C 행의 핵심 근거 `LoadSentencePort.java:5`, `SentencePersistenceAdapter.java:5` 와 §3 C 표의 4행을 커밋에서 확인할 수 없다.
    - `ScoreAnswerService` 는 없는 타입(`LoadSentencePort`, `ScoreSimilarityPort`)을 import 한다.
    - Round 1 리뷰도 작업 트리에서 이 파일들을 읽었기 때문에 이 문제를 놓쳤다.
  - 요구 수정:
    - `.gitignore` 의 `out/` 를 `/out/` 로 좁힌다(또는 `!docs/**/out/` 을 추가한다). 이어서 4파일을 `git add` 한다.
    - 같은 문제를 막도록 `docs/ai/` 아래 ignored 파일이 0인지 확인하는 한 줄을 ai-workflow 체크리스트에 둔다. 예: `git status --ignored docs/ai`.

## 주요

- **[M1] README §5 "C 의 세 번째 배치" (125행) + architecture-proposal §2 C 136행·§3.1 154행 — "C 를 순환 없이 하려면 B 의 경계가 먼저 필요하다" 는 틀렸다. 제시된 해법도 최소 해법이 아니다.**
  - 실행:
    - 예시 배치 그대로 R1 을 돌리면 `Cycle detected: answer -> sentence -> answer` 가 나온다. README 의 순환 서술은 맞다. R2 도 위반한다(`SentencePersistenceAdapter implements answer…LoadSentencePort`, `LoadSentencePort.loadActive` 반환 타입 `Optional<sentence.domain.Sentence>`).
    - 여기서 다음만 바꿨다:
      - (1) `answer/application/port/out/SentenceForScoring(id, englishText, koreanRef)` record 를 추가했다.
      - (2) `LoadSentencePort` 가 그 record 를 반환하게 했다.
      - (3) `SentencePersistenceAdapter` 는 그대로 sentence 쪽에 두고 그 record 로 매핑했다.
      - (4) `ScoreAnswerService` 에서 `sentence.*` import 를 없앴다. 예외는 answer 소유로 바꿨다.
    - 결과: R1 위반에서 answer↔sentence 순환이 사라졌다. AS-IS 의 `common` 순환 1건만 남았다. 추가 파일은 record 1개다. 방향은 sentence → answer(포트) 단방향이다. 이것이 헥사고날의 "필요 인터페이스(required interface)" 표준형이다.
  - 문제:
    - README 가 제시한 "세 번째 배치" 는 순환 해소의 최소 수단이 아니다. 비용은 "포트 1 + 어댑터 1 + sentence 인바운드 포트 1" 로 적혀 있다. 실제로는 record·sentence 인바운드 포트 구현·sentence 아웃바운드 포트까지 늘어나며, 이 계수도 모호하다.
    - "B 의 경계가 먼저 필요" 라는 결론은 C 의 문제를 B 에 종속시킨다. 인바운드 포트는 C 고유의 개념이라 B 가 선행할 이유가 없다. 이 결론은 추천안(B) 쪽으로 기운 서술이 된다.
    - §3.1 "포트 소유·어댑터 위치 문제: **있음** — 순환 없는 배치는 B 의 공개 API 가 선행" 도 같은 오류다. `a80e666` 에도 남아 있다.
  - 요구 수정:
    - 순환 해소 수단으로 "포트 반환 타입을 answer 소유 record 로(+1 파일)" 를 먼저 제시한다. "세 번째 배치" 는 sentence 가 answer 를 몰라야 할 때의 대안으로 낮춘다.
    - "B 의 경계가 먼저 필요" 를 삭제한다. 대신 "어느 배치든 R2 대신 C 전용 경계 규칙(포트 패키지만 교차 허용)이 필요" 처럼 구조 중립적으로 쓴다.
    - 가능하면 예시 C 자체를 순환 없는 배치로 고친다. 지금은 "가장 불리한 배치 하나" 만 코드로 보여 준다(Round 1 M2 의 원래 취지).

- **[M2] README §2 "점수 범위" (41행), `SimilarityScorer.java:44-45`(A′)/`:46-47`(B) — clamp 가 부동소수 오차뿐 아니라 계약 위반 값 전체를 정상 점수로 바꾼다. 이 정책을 정한 결정 항목이 없다.**
  - 실행 (A′·B 동일):
    - `{"score":1.5}` → `200 score=1.0 grade=EXCELLENT`
    - `{"score":-0.2}` → `200 score=0.0 grade=POOR`
    - `{"score":1.0000001}` → `200 1.0 EXCELLENT`
  - Similarity 계약은 `score 0.0 ~ 1.0` 이다(`docs/public/03_API명세.md:155`, §5.1). 1.5 는 오차가 아니라 서버 결함(정규화 누락 등)이다. 지금 정책은 이 결함을 "만점" 으로 바꿔 사용자에게 보여 주고 로그도 남기지 않는다. 로그는 보정된 값(`score=1.0`)만 찍는다(`:46`/`:48`).
  - 코사인 유사도는 원래 [-1, 1] 이다(01 기획 §1.2 "코사인 유사도를 점수로 환산"). 음수가 정상값인지 결함인지는 서버의 환산식에 달렸다. 그런데 문서는 이 점을 다루지 않는다.
  - README 의 근거는 "부동소수 오차(1.0000001) 보정" 뿐이다. 반면 decisions-needed D13(`:136-139`)은 빈 본문·600+ 만 다루고 범위 밖 점수를 다루지 않는다. Round 1 M3 이 요구한 것은 "범위 정책을 **정해**" 였다. 지금은 사람의 결정 없이 가장 관대한 쪽이 기본값이 됐다.
  - 요구 수정:
    - 허용 오차(예: `|score−clamp| ≤ 1e-6`)만 보정하고, 그 밖은 `Unavailable` 로 처리한다. 최소한 원래 값으로 `WARN` 로그를 남긴다.
    - D13 에 "범위 밖 점수" 행을 추가해 사람이 결정하게 한다.
    - C `AnswerScore` 불변식 주석(`AnswerScore.java:6`)도 같은 정책에 맞춘다.

- **[M3] README §6 질문 2 (138행) "A′·B 모두 `SimilarityScorer` 가 중복을 막는다 (차이 없음)" — A′ 에서는 우회를 막는 규칙이 없다. 이 차이가 §5 A′ 약점에서도 빠졌다.**
  - A′ 규칙은 "타 기능의 `repository`·`domain` 접근 금지" 다(architecture-proposal §2 A′ `:46`, `:59`). `infra.similarity.SimilarityClient` 는 그 대상이 아니다. 그래서 A′ 에서는 어떤 기능이든 `SimilarityClient` 를 직접 주입해 예외 변환·빈 응답 처리·clamp 를 건너뛸 수 있다. 그러면 timeout → 500 회귀(Round 1 C1)가 그대로 재발한다.
  - B 에서는 `similarity.client` 가 internal 이라 R2 가 이를 막는다. 이 점은 B 이식에서 R2 통과로 확인했고, A′ 이식의 R2 11건 중에는 `infra.similarity.SimilarityScorer` 같은 타 모듈 하위 패키지 의존이 포함된다.
  - 요구 수정:
    - §5 A′ 행에 "외부 클라이언트 직접 사용 우회 가능 — `infra/{시스템}` 의 Scorer 외 타입 접근 금지 규칙이 추가로 필요" 를 넣는다.
    - §6 질문 2 의 "(차이 없음)" 을 고친다.
    - architecture-proposal §2 A′ 의 강제 규칙에도 이 규칙을 추가한다.

## 경미

- [m1] architecture-proposal §3.2 C 행(`:168`) "examples C 기준 `answer` 한 흐름에만 신규 9" 가 README §3(95행) "신규 8개 중 2개는 sentence 쪽, 이동 1개 별도" 와 다르다. Round 1 m2 는 README 쪽에서만 고쳐졌다. `a80e666` 에도 남아 있다.
- [m2] README §3 B 표(67-78행)에 `infra/similarity/dto/SimilarityRequest.java`·`SimilarityResponse.java` 의 **삭제** 행이 없다. "흡수" 라는 말로만 암시한다. architecture-proposal §3.2 는 "삭제 2" 로 센다(합계 12). 표대로만 옮기면 dto 2개가 고아로 남는다. 컴파일과 R2 에는 영향이 없다.
- [m3] README §4 "A′ 와 B 의 코드 차이는 import 경로뿐이다"(111행) 는 `AnswerService`·`AnswerResponse` 에만 맞다. 나머지 차이:
  - `SentenceDetail.from` 이 A′ 에서는 `public` 이다(`option-a-layered/sentence/SentenceDetail.java:9`). 그래서 어느 기능이든 `SentenceEntity` 에서 record 를 만들 수 있다.
  - 조회 메서드의 위치가 다르다: A′ 는 기존 `SentenceService` 에 추가, B 는 신규 `SentenceReader`.
  - dto 파일이 A′ 는 분리돼 있고 B 는 중첩이다.
  - "`AnswerService` 기준" 이라고 한정한다.
- [m4] README §3 A′ 표 `sentence/SentenceService.java` "수정 (`getActive` → record)" 는 기존 메서드를 바꾸는 것처럼 읽힌다. 실제로 `getActive` 는 AS-IS 에 없고 **추가**다(`src/main/.../SentenceService.java` 에는 `getRandom` 만 있음). 또 예시는 AS-IS 의 미사용 import 3개를 지우고 로그 문구 공백("문장 출제 :" → "문장 출제:")도 바꿨다. "getRandom 은 AS-IS 유지"(`:3`)와 미세하게 다르다.
- [m5] C `SimilarityHttpAdapter.java:17-19` 는 본문을 생략한다고 주석으로 밝혔다. 그러나 실제 코드 줄은 `similarityClient.score(...).score()` 다. README §2 "세 선택지 모두에 똑같이 적용했다"(33행)와 달리, 이 줄을 그대로 쓰면 AS-IS 결함(timeout 500, 빈 응답 NPE)이 남는다. 주석 대신 `// TODO: B SimilarityScorer 본문 복사` 를 두거나 본문을 넣는 편이 오해가 적다.
- [m6] (`3e136c6` 기준, `fc49d18` 에서 해소된 것으로 보임) conventions §2.4 에 두 가지가 남아 있다. "선택지 A 이면 서비스의 private 메서드에 둔다" 는 A′ 의 `infra/similarity/SimilarityScorer` 와 모순된다. After 주석 "4xx/5xx + 비표준 상태 코드" 는 Round 1 M4 실행 결과와 README 주석("DefaultResponseErrorHandler 기준")과 다르다. `fc49d18` 의 conventions 에는 두 문구가 없다(grep). 다음 라운드에서 확인한다.
- [m7] README §0 B 행 "이후 `SimilarityScorer` 에 빈 응답·`Math.clamp` 를 추가" 는 추가 후 재실행했다는 기록이 없다. 이번 라운드에서 재실행해 확인했으므로(아래) 결과 칸을 실행 기록으로 바꾼다. R4b 도 통과했으니 목록에 넣는다.
- [m8] 회귀 테스트 "지연 응답 + 짧은 readTimeout"(`SimilarityScorer.java:38`/`:40`)이 주석으로만 요구되고, feature_list 의 어느 항목 완료 조건에 들어가는지 README 에 연결돼 있지 않다. FIX-F1T 를 참조하는 한 줄을 넣는다.

## 이전 라운드 지적 해소 여부

| 지적 | 상태 | 확인 |
|---|---|---|
| C1 A timeout → 500 | **해소** | A′·B 모두 3초 지연 → `503 TMI.SIMILARITY.UNAVAILABLE` (2.0s). cause 체인 `Unavailable → RestClientException → SocketTimeoutException` 으로 세 번째 catch 경로 확인 |
| C2 A 가 A′ 와 다름 | **해소** | `SentenceService.getActive → sentence.dto.SentenceDetail`, `infra/similarity/SimilarityScorer`. A′ 이식 시 R5 위반은 `getRandom` 1건뿐(신규 코드 위반 0) |
| M1 마이그레이션 누락 | **해소** | 두 표에 `RestClientConfig` 삭제·키 변경 행이 있다. 표대로 A′·B 모두 `build` 성공. `@DefaultValue` 로 AS-IS 키 그대로도 기동(B `CoreApplicationTests` 통과). B 표의 dto 삭제 행 누락은 m2 |
| M2 C 배치 | **부분** | 순환 서술·R1/R2 위반·§4 각주는 정확하다(실행). 그러나 세 번째 배치의 결론이 틀렸다 → M1. 근거 파일 미커밋 → C1 |
| M3 점수 범위 | **부분** | 세 선택지 동작은 통일했다(clamp, 실행). 정책 자체가 결정 없이 가장 관대한 쪽 → M2 |
| M4 비표준 상태 코드 | **해소**(examples) | README §2 47행·주석 "DefaultResponseErrorHandler 기준", 600+ 빈틈을 D13 에 등록. conventions 는 m6 |
| m1 줄 번호 | 해소 | `LoadSentencePort.java:5`, `SentencePersistenceAdapter.java:5` 는 작업 트리 파일 기준으로 맞다(단 C1) |
| m2 파일 수 | 해소(README) | AS-IS `answer` 7, C 신규 8 + 이동 1. architecture-proposal 은 m1 |
| m3 `:9-11,24` | 해소 | |
| m4 빈 응답 NPE | 해소 | 200 빈 본문·`Content-Type` 만 있는 빈 본문·204 → 503, cause `IllegalStateException("empty similarity response")` |
| m5 로그 키 | 해소 | A′·B 공통 `answerLength` |
| m6 C 표시 생략 | 해소 | §3 C 표 100행 |
| m7 package-private 컨트롤러 | 해소 | `AnswerController.java:3`, §3 표 |
| m8 "차이는 패키지뿐" | 해소 | A′ `:5`, B `:2` 에 가시성 차이 명시 |

## 검증한 주장 (문제 없음 확인)

- **README §0 A′ 행 [실행]**:
  - `bash ./gradlew build` → 성공(compile + `CoreApplicationTests`).
  - `@Disabled` 전부 해제 후 결과: R1·R3·R4·R4b·R6·R7·contextLoads **통과**, R5 **1건**(`SentenceService.getRandom`), R2 **11건**(첫 위반 `AnswerService.<init>(sentence.service.SentenceService, infra.similarity.SimilarityScorer)`). 서술과 정확히 일치한다.
- **README §0 B 행 [실행]**: `build` 성공. R1·R2·R3·R4·R4b·R6·R7 통과, R5 1건(`getRandom`)만 남는다. package-private `@Configuration`·중첩 `@ConfigurationProperties` record 도 동작한다.
- **README §2 공통 정책 [실행, A′·B 결과 동일]**:
  - 정상 → 200 `0.83 GOOD`. 요청 본문 `{"answer":"ok","reference":"좋아요."}` 이 chunked 로 전송됐다(F1 수정 동작).
  - 3초 지연 → 503 (2.0s, readTimeout 2s). 200 빈 본문 / 204 → 503.
  - NaN·`{"score":null}`·`{}` → 503 (cause `HttpMessageNotReadableException` → Jackson 3 `StreamReadException`/`MismatchedInputException`, 세 번째 catch).
  - 400 → 502 `REQUEST_INVALID`, 503 → 503.
  - 1.5 → 1.0, 1.0000001 → 1.0, -0.2 → 0.0 (clamp 서술대로. 정책의 타당성은 M2).
- **import 수 (README §4)**: AS-IS 9(`AnswerService.java:3-11`), A′ 6(`:6-11`), B 6(`:6-11`), C 6(`ScoreAnswerService.java:5-10`, 작업 트리). B 헤더의 "AS-IS import 7개(sentence 3 + infra 4) 사라짐" 도 맞다.
- **줄 번호**:
  - README §1: `AnswerService.java:9-11,24`, `:28,35`, `:41-56`, `AnswerResponse.java:4`, `:37`, `SimilarityClient.java:10`, `RestClientConfig.java:3` 모두 맞다.
  - README §5 A′ "`AnswerService.java` 10-11행 import" = `sentence.dto.SentenceDetail`, `sentence.service.SentenceService` 로 맞다.
- **파일 수**:
  - A′ 예시 7파일, B 7파일, C 9파일(커밋에는 5 — C1). §3 표의 행 수, 첫 줄 "위치" 주석, package 선언이 서로 일치한다.
  - architecture-proposal §3.2 의 A′ 8 / B 12 / 차이 4 를 다시 셌고 맞다.
- **C 순환 서술 [실행, 스텁 컴파일]**:
  - R1: `answer → sentence` 는 `LoadSentencePort.loadActive` 반환 타입과 `ScoreAnswerService` 의 `Sentence`·`SentenceException` 사용에서 생긴다. `sentence → answer` 는 `SentencePersistenceAdapter implements LoadSentencePort` 에서 생긴다. 이 둘로 순환이 검출된다.
  - R2 위반도 나온다. architecture-proposal §2 C 135행의 순환 서술과 일치한다.
- **03 계약과 일치**: 4xx → 502, 5xx·타임아웃(2초)·네트워크 → 503 (`03_API명세.md:233-237`). 빈 본문 503 은 D13 "03 계약" 판단과 맞다.
- **`Math.clamp(double,double,double)`**: Java 21 API 이고 toolchain 21 에서 컴파일된다.
