# examples 리뷰 — Round 3

> **대상**: `docs/ai/proposals/examples/` 전체(25파일). 기준 커밋은 `83f7b92` 이고, examples 의 마지막 변경 커밋은 `8ce4a33` 이다. 대조 문서(`architecture-proposal.md`, `decisions-needed.md`, `conventions-proposal.md`, `feature_list.example.json`)와 `src/main` 도 `83f7b92` 기준으로 읽었다.
>
> **검증 방법**: 전용 worktree 3개(`wt-examples`, `wt-examples-b`, `wt-examples-c`)를 `83f7b92` 에서 만들었고, 리뷰 후 모두 삭제했다. DB 는 SELECT 만 했다.
> 1. **커밋 여부**: `git ls-files docs/ai/proposals/examples` 와 `find docs/ai/proposals/examples -type f` 를 비교했다. `git check-ignore -v --no-index` 도 돌렸다.
> 2. **C 재현**: README §0 C 행대로 적용했다. 포트 3개·유스케이스 서비스·`AnswerScore`·`AnswerException`·`SentencePersistenceAdapter` 를 넣었다(리포지토리 이름은 AS-IS `SentenceRepository`). AS-IS `AnswerService`·`AnswerController`·`AnswerResponse` 는 삭제했다. 이어서 `compileJava` 를 돌렸다. 그다음 ArchUnit 의 `@Disabled` 를 모두 풀고 `*ArchitectureTest` 를 돌렸다.
> 3. **A′·B 이식 + 점수 정책 프로브**: README §3 표대로 이식했다. `RestClientConfig` 는 삭제했고 `similarity.*` 키는 바꿨다. B 는 `infra/**` 를 삭제하고 `SimilarityException` 을 이동했다.
>    - 가짜 Similarity 서버(python, `127.0.0.1:18777`, chunked 본문 지원)를 띄웠다.
>    - `@SpringBootTest(RANDOM_PORT)` + `HttpClient` 로 `POST /api/v1/answers` 를 호출했다. 대상은 실제 DB 문장 `00000000-…-0001` 이다.
>    - 응답 값 14종: `0.83`, `1.0000001`, `1.000001`, `1.0000011`, `-0.0000005`, `1.5`, `-0.2`, `1e308`, `"NaN"`(문자열), `"Infinity"`(문자열), `"1.5"`(문자열), bare `NaN`, 3초 지연(B), 빈 200(B).
>    - 같은 실행에서 `contextLoads` 와 ArchUnit R1~R7(`@Disabled` 해제)을 함께 돌렸다. 로그는 `-i` 로 수집했다.
> 4. **문서 대조**: README §0·§3·§4·§5·§6 을 `architecture-proposal.md` §2·§3.1·§3.2, `decisions-needed.md` 표·D1·D13, `conventions-proposal.md` §2.4, `feature_list.example.json` 과 줄 단위로 비교했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | §0 의 A′·C 결과와 1.0000001→1.0, 1.5→503 은 실행으로 정확히 재현된다. 그러나 정책 서술 두 개가 실행과 다르다: "그 밖은 `Unavailable`"(NaN 은 200, M1)과 "원래 값은 핸들러가 ERROR + 스택으로 기록"(AS-IS 핸들러에서는 기록되지 않음, M2). D13 의 "`FIX-F1T` 범위" 는 근거가 없다 |
| 구체성 | 4 | A′·B 는 표대로 옮기면 build·기동·채점이 된다. C 도 §0 대로 컴파일된다. 남은 것은 C 재현 절차의 `AnswerApi` 삭제 누락(m5) 정도다 |
| 일관성 | 3 | A′ 우회 방지 규칙 (b) 가 같은 문서의 Toss 배치와 충돌한다. "경계 규칙 1개" 서술 3곳과도 모순되고 feature_list `ARCH-A2` 에도 반영되지 않았다(M3). conventions §2.4 에는 옛 clamp 가 남아 있다(m2). README 안에서도 C 신규 개수가 7/8 로 다르다(m1) |
| 판단 가능성 | 4 | A′·B·C 비교는 이제 공정하다: C 는 순환 없는 표준형이고, A′ 의 우회 가능성도 §5·§6 에 드러나 있다. D13 은 선택지 (a)(b)(c) 를 나란히 보여 줘 사람이 결정할 수 있다. M2·M3 은 결정 이후의 이행 추적성 문제다 |

## 치명 (승인 차단)

없음.

## 주요

- **[M1] `option-a-layered/infra-similarity/SimilarityScorer.java:49-53`, `option-b-modular/similarity/SimilarityScorer.java:51-55`, `option-c-hexagonal/domain/AnswerScore.java:7` — NaN 이 허용 오차 검사와 C 의 불변식을 둘 다 통과한다. 그래서 README §2(41행)·D13(b) 의 "그 밖은 `Unavailable`" 이 성립하지 않는다.**
  - 원인:
    - `Math.clamp(NaN, 0.0, 1.0)` 은 `NaN` 을 돌려준다.
    - `Math.abs(NaN - NaN) > 1e-6` 은 `false` 다. 그래서 예외 없이 `score=NaN` 이 반환된다.
    - C 의 `value < 0.0 || value > 1.0` 도 NaN 에 대해 `false` 라서 불변식이 막지 못한다.
  - 실행 결과 (A′·B 동일):
    - `{"score":"NaN"}`(문자열): Jackson 의 스칼라 강제 변환으로 `double NaN` 이 된다. 결과는 **`200 {"score":"NaN","grade":"POOR"}`** 이고 로그는 `Similarity 응답: score=NaN` 이다. 03 계약의 `score`(number) 타입이 깨진 채 성공 응답이 나간다.
    - bare `NaN` 은 503 이다. 역직렬화 실패로 세 번째 catch 에 걸린다. 반면 `"Infinity"`·`1e308`·`"1.5"` 는 모두 503 이다. 문자열 NaN 만 빠져나간다.
  - 노출 가능성: FastAPI 는 기본 설정에서 NaN 을 `null` 로 내거나 500 을 낸다. 그래서 노출 가능성은 낮다. 그러나 이 코드는 "그대로 복사" 하라고 지정된 기준 구현이고(C `SimilarityHttpAdapter.java:2-4`), 정책 문서는 "범위 밖 전부 503" 을 약속한다.
  - 요구 수정:
    - 비교를 `if (!(raw >= -SCORE_TOLERANCE && raw <= 1.0 + SCORE_TOLERANCE)) throw …;` 로 바꾸고 `score = Math.clamp(raw, 0.0, 1.0)` 을 그 뒤에 둔다. 또는 `!Double.isFinite(raw)` 를 먼저 검사한다.
    - C 불변식도 `!(value >= 0.0 && value <= 1.0)` 로 바꾼다.
    - D13·README §2 의 표에 NaN 행을 추가한다.

- **[M2] `decisions-needed.md:37` "`FIX-F1T` 범위" + `SimilarityScorer.java:48`(A′)/`:50`(B) "원래 값은 예외 메시지로 남긴다 — 핸들러가 5xx 로 ERROR + 스택 기록" — 결정된 점수 정책을 구현·검증하는 feature 항목이 없다. 또 AS-IS 핸들러에서는 원래 값이 어디에도 남지 않는다.**
  - 실행 (A′·B): `1.5`, `-0.2`, `1e308` → 503 이다. 이 경우 로그 전체에 남는 것은 다음 한 줄뿐이다. `out of range` 문자열은 로그에서 0회 검출됐다.
    ```
    ERROR … GlobalExceptionHandler : [TMI.SIMILARITY.UNAVAILABLE] TMI.SIMILARITY.UNAVAILABLE - TMI.SIMILARITY.UNAVAILABLE
    ```
    이유는 `GlobalExceptionHandler.java:77` 이 `e.getMessage()`(= 오류 코드)만 찍고 `Throwable` 을 넘기지 않기 때문이다. 그래서 cause `IllegalStateException("similarity score out of range: 1.5")` 이 버려진다. 스택트레이스 기록은 `FIX-F3`(conventions §2.2 규칙 2)이 적용된 뒤에야 성립한다. 그러나 예시 주석과 D13 (b) "결함은 드러낸다" 는 이 전제를 밝히지 않는다. Round 2 M2 가 요구한 "원래 값으로 로그" 는 현재 순서로는 지켜지지 않는다.
  - 소유 항목 문제:
    - `FIX-F1T`(`feature_list.example.json:51-62`)의 제목·acceptance 는 "read timeout·빈 응답 → 503" 뿐이다. 범위 밖 점수는 없다.
    - `SimilarityScorer` 를 도입하는 `ARCH-B2`·`ARCH-A2` 의 acceptance 에도 점수 정책이 없다.
    - 따라서 D13 을 (b) 로 결정해도 에이전트 루프가 이를 구현·테스트할 항목이 없다. `FIX-F1T` 는 `AnswerService` 를 고치는 항목이라, (b) 정책 없이 끝날 수 있다.
  - 요구 수정:
    - `FIX-F1T` 의 acceptance 에 다음 케이스를 넣는다: `1.0000001 → 200 1.0`, `1.5 → Unavailable`, `NaN → Unavailable`. 또는 별도 항목을 만들고 D13 표의 "영향" 칸을 그에 맞춘다.
    - 예시 주석과 D13 (b) 에 "원래 값 기록은 `FIX-F3` 이후" 라고 적는다. 아니면 `FIX-F1T` 를 `FIX-F3` 뒤로 두거나, 그 전까지는 `SimilarityException.Unavailable` 에 `logMessage(...)` 를 쓰는 등 기록 경로를 명시한다.

- **[M3] `architecture-proposal.md:63` A′ 강제 규칙 (b) — Round 2 M3 의 반영이 문서 한 곳에서 멈췄다. 같은 문서의 Toss 배치와 충돌하고, "경계 규칙 1개" 서술 3곳과 feature_list `ARCH-A2` 에 전파되지 않았다.**
  - Toss 와의 충돌: 규칙 (b) 는 "`infra/{시스템}` 패키지의 타입 중 `*Scorer` 외에는 `infra` 밖에서 접근 금지" 다. 그런데 같은 문서의 A′ 트리(`:58-59`)는 `infra/toss/TossHoldingsClient` 를 두고 `toss/holdings/service` 가 쓰는 구조다. 문자 그대로 적용하면 Toss 서비스가 클라이언트를 쓸 수 없다. Toss 용 `*Scorer` 같은 래퍼를 추가할지(파일 +1~2) 규칙을 시스템별로 쓸지 정해지지 않았다. `*Scorer` 라는 이름 자체도 Similarity 전용이다.
  - 모순 위치:
    - README §4 `:112` "경계 규칙은 둘 다 ArchUnit 조건 1개다"
    - architecture-proposal §3.1 P5 `:156` "A′ … 경계 규칙 1개"
    - decisions-needed D1 표 `:50` "경계 규칙 1개"
    - 이 세 곳은 README §5 `:119`·§6 Q2 `:131`·architecture-proposal `:63` 의 "(a)+(b) 두 규칙" 과 모순된다. A′ 의 강제 비용이 과소 표기된 셈이다.
  - feature_list 누락: `ARCH-A2`(`feature_list.example.json:297`)의 제목은 "R2 를 '타 기능 repository·domain 접근 금지' 규칙으로 교체" 다. (b) 가 없고 `exception` 도 빠졌다(architecture-proposal (a) 는 `repository·domain·exception`). A′ 를 고르면 우회 방지 규칙은 구현되지 않은 채 `passes` 가 된다.
  - 요구 수정:
    - (b) 를 "`infra/{시스템}` 의 클라이언트 인터페이스(`@HttpExchange` 보유 타입)는 같은 `infra/{시스템}` 패키지에서만 참조" 처럼 Toss 에도 맞는 조건으로 다시 쓴다. 또는 Toss 쪽 래퍼를 명시한다.
    - 위 세 곳을 "조건 2개" 로 고친다.
    - `ARCH-A2` 의 제목·acceptance 에 (a)(exception 포함)와 (b) 를 넣는다.

## 경미

- [m1] README §5 C 행 `:124` 은 "한 흐름에 **신규 8**" 이라고 쓴다. 그러나 같은 README §3 `:98` 은 "신규 7개(포트 3·서비스 1·도메인 1·어댑터 2) + 수정 1 + 이동 1" 이고, architecture-proposal §3.2 `:172` 는 "신규 7 + 수정 1" 이다. 표의 행을 세면 7이 맞다. Round 2 m1 의 잔여분이다.
- [m2] `conventions-proposal.md:317` 의 After 주석이 아직 "점수는 `Math.clamp(score, 0.0, 1.0)`" 다. 바뀐 정책(허용 오차, 그 밖 `Unavailable`)과 모순된다. 이 주석은 examples README §2 를 근거로 인용한다.
- [m3] `.gitignore:25` 의 `out/` 는 그대로다. 4파일은 강제 add 로 추적만 되고 있다. 예를 들어 `git check-ignore -v --no-index docs/ai/examples/answer-scoring/option-c-hexagonal/application/port/out/NewPort.java` 의 결과는 `.gitignore:25:out/` 다. 그래서 C 예시의 `port/out/`·`adapter/out/` 에 새 파일을 만들면 또 조용히 빠진다. Round 2 C1 이 요구한 `.gitignore` 축소(또는 `!docs/**/out/`)와 ai-workflow 체크리스트 한 줄(`git status --ignored docs/ai`)은 둘 다 반영되지 않았다(`grep -n ignored docs/ai/proposals/ai-workflow.md` → 0건).
- [m4] README §0 B 행 `:14` 이 "점수 보정을 허용 오차 방식으로 변경 — 재실행 전" 으로 남아 있다. 이번 라운드에서 다시 실행했으므로 결과 칸을 실행 기록으로 바꾼다. 결과: `build`·`contextLoads` 성공, R1·R2·R3·R4·R4b·R6·R7 통과, R5 1건(`getRandom`), 3초 지연·빈 200 → 503, 1.0000001 → 1.0, 1.5 → 503 (단 NaN 은 M1).
- [m5] README §0 C 행 `:15` 의 절차를 문자 그대로 따르면 컴파일이 실패한다. AS-IS `AnswerApi`(`answer/controller/AnswerApi.java:3,37,107`)가 삭제된 `AnswerResponse` 를 참조해 `cannot find symbol` 3건이 난다. `AnswerApi` 도 삭제하면 성공한다. 그러니 "컨트롤러" 를 "`AnswerController`·`AnswerApi`" 로 적는다.
- [m6] architecture-proposal §2 C 트리(`:121`, `:134`)는 "배치는 examples 와 같다" 고 하면서 `sentence/domain/Sentence (순수 모델)` 을 둔다. 그런데 `8ce4a33` 에서 예시의 `domain/Sentence.java` 는 삭제됐다. 어댑터도 JPA 엔티티를 바로 `SentenceForScoring` 으로 매핑한다(`SentencePersistenceAdapter.java:23`). §3.1 P4 C 칸 "해결 (도메인 모델)" 도 예시의 "포트 소유 record" 와 다르다. 결정에는 영향이 없다.
- [m7] C 의 `SentencePersistenceAdapter` → `SentenceRepository` 접근은 R4 를 통과한다. 하지만 이는 어댑터 패키지가 R4 의 어느 레이어에도 속하지 않아서다(`consideringOnlyDependenciesInLayers`). 그래서 C 를 고르면 R2 만이 아니라 R4 도 C 용으로 바꿔야 한다. README §0·§5 와 architecture-proposal §2 C 는 "R2 교체" 만 언급한다.
- [m8] `SimilarityScorer.java:35`(A′)/`:37`(B) 의 `ResourceAccessException` 주석은 "연결 거부 등 I/O 오류 (연결 단계)" 다. conventions §2.4 표에 따르면 GET 요청의 read timeout 도 이 경로로 온다. Similarity(POST)만 다루는 예시에서는 맞는 말이지만, Toss(GET)로 복사하면 오해할 수 있다. "(POST 기준)" 정도로 한정한다.

## 이전 라운드 지적 해소 여부

| 지적 | 상태 | 확인 |
|---|---|---|
| C1 C 4파일 미커밋 | **해소** (재발 방지는 미반영 → m3) | `git ls-files docs/ai/proposals/examples` 25개 = `find` 25개. `LoadSentencePort`·`ScoreSimilarityPort`·`SentencePersistenceAdapter`·`SimilarityHttpAdapter` 가 `83f7b92` 에 있다. `git status --ignored docs/ai` 는 0건이다 |
| M1 C 순환 해법 | **해소** | 포트가 `SentenceForScoring` record 를 반환한다(`LoadSentencePort.java:11,14`). `ScoreAnswerService` 에 `sentence.*` import 가 0개다. [실행] `compileJava` 성공, R1 위반은 `common → infra → common` 1건뿐이고 answer↔sentence 순환은 없다. R2 는 3건으로 모두 `sentence.adapter.out.persistence.SentencePersistenceAdapter → answer.application.port.out.LoadSentencePort(·$SentenceForScoring)` 이다. README §0 과 architecture-proposal `:139-140` 서술이 정확하다. "B 의 경계가 먼저 필요" 문구도 삭제됐다(grep 0건) |
| M2 점수 clamp 정책 | **부분** | 허용 오차 방식 구현, D13 (a)(b)(c) 등록, `AnswerScore` 주석 갱신이 반영됐다. [실행] A′·B 에서 1.0000001 → 200 1.0, 1.5 → 503, -0.2 → 503, -0.0000005 → 200 0.0. 그러나 NaN 은 통과하고(M1), 원래 값 로그가 없으며 소유 항목도 없다(M2) |
| M3 A′ 우회 | **부분** | README §5 `:119`·§6 Q2 `:131`·architecture-proposal `:63` (b) 가 반영됐다. 그러나 "규칙 1개" 3곳, `ARCH-A2`, Toss 와의 충돌이 남았다(M3) |
| m1 C 신규 9 vs 8 | **부분** | architecture-proposal §3.2 와 README §3 은 "신규 7 + 수정 1" 로 맞는다. README §5 `:124` 만 8이다(m1) |
| m2 B dto 삭제 행 | 해소 | README §3 B 표 `:78` |
| m3 "차이는 import 뿐" | 해소 | `:112` 를 "`AnswerService` 기준" 으로 한정했고, 나머지 차이 3개를 괄호로 명시했다 |
| m4 `getActive` "추가" | 해소 | `:57` |
| m5 C 어댑터 한 줄 본문 | 해소 | `SimilarityHttpAdapter.java:20-21` 에 `TODO` + `UnsupportedOperationException` 이 있고, README §3 `:96` 에도 적혀 있다 |
| m6 conventions §2.4 A′·비표준 상태 | 해소 | `grep "private 메서드\|비표준 상태"` 0건. 다만 같은 절의 clamp 주석이 새로 낡았다(m2) |
| m7 B 재실행 기록 | **미해소** | `:14` "재실행 전" 그대로(m4). 이번 라운드 실행으로 동작은 확인했다 |
| m8 회귀 테스트 연결 | 해소 | README §2 `:40` 이 `FIX-F1T`(`SimilarityTimeoutTest`)를 참조한다 |

## 검증한 주장 (문제 없음 확인)

- **README §0 A′ 행 [실행]**:
  - 표대로 이식한 뒤 `contextLoads` 가 통과한다.
  - `@Disabled` 전부 해제 후 결과: R1·R3·R4·R4b·R6·R7 통과, R5 1건(`SentenceService.getRandom`).
  - R2 는 11건이고 첫 위반은 `AnswerService.<init>(…SentenceService, …infra.similarity.SimilarityScorer)` 다. 서술과 일치한다.
- **README §0 B 행 [실행]**: `contextLoads` 통과, R1·R2·R3·R4·R4b·R6·R7 통과, R5 1건. `infra` 패키지는 소멸했다(`src/main` 의 `infra` 참조는 주석 3줄뿐).
- **README §0 C 행 [실행]**: m5 의 `AnswerApi` 삭제만 더하면 `compileJava` 가 성공한다. R1·R2 결과는 위 해소 표와 같다. R4·R4b·R6 은 통과하고, R5·R7 은 AS-IS 잔여분(`getRandom`, `infra.similarity.SimilarityClient` 의 `@RequestBody` 누락 — C 재현은 Similarity 쪽을 건드리지 않음)이다.
- **점수 정책 [실행, A′·B 동일]**:
  - 0.83 → 200 GOOD
  - 1.0000001 → 200 1.0 EXCELLENT, 1.000001 → 200 1.0 (부동소수 차 9.99…e-7 ≤ 1e-6)
  - 1.0000011 → 503, -0.0000005 → 200 0.0, 1.5 → 503, -0.2 → 503
  - 1e308 → 503, `"Infinity"` → 503, `"1.5"`(문자열) → 503, bare NaN → 503
  - (B) 3초 지연 → 503, 빈 200 → 503
  - focus 의 "1.0000001 → 1.0, 1.5 → 503" 은 확인됐다. 예외는 `"NaN"` 하나다(M1).
- **architecture-proposal §3.2 수치**: A′ 8(신규 3 + 수정·삭제 5), B 12(신규 4 + 이동 2 + 삭제 3 + 수정 3)가 README §3 표의 행(F1 행 제외)과 일치한다. C "신규 7 + 수정 1" 도 README §3 C 표와 일치한다.
- **architecture-proposal §2 C `:139-140`**: "순환 없음" 과 "R2 위반 3건 = 어댑터 → 포트" 가 실행 결과와 정확히 일치한다.
- **README §4 import 수**: AS-IS 9(`AnswerService.java:3-11`), A′ 6(`:6-11`), B 6(`:6-11`), C 6(`ScoreAnswerService.java:5-10`).
- **README §2 ↔ D13**: 빈 본문 503, 허용 오차 1e-6, 600+ 보류의 세 정책이 README §2 `:40-41,48` 과 D13 `:139-141` 에서 같다. A′·B `SimilarityScorer` 는 패키지·import 를 빼면 본문이 같다(`SCORE_TOLERANCE`, 정책 블록 동일).
- **C `AnswerException`**: 기존 `ETmiErrorType.SENTENCE_NOT_FOUND` 를 재사용하고 `BaseException(HttpStatus, IErrorType, Object...)` 생성자로 컴파일된다. 응답 코드는 AS-IS `SentenceException.NotFound` 와 같다.
