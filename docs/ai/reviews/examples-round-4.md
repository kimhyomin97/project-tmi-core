# examples 리뷰 — Round 4

> **대상**: `docs/ai/examples/` 전체(25파일, `git ls-files` = `find` = 25). 기준 커밋은 `01898ba` 다. 대조 문서(`architecture-proposal.md`, `decisions-needed.md`, `conventions-proposal.md`, `ai-workflow.md`, `feature_list.example.json`)와 `src/main` 도 `01898ba` 에서 읽었다. 이전 라운드는 `reviews/examples-round-1~3.md` 다.
>
> **검증 방법**: 전용 worktree 4개(`wt-examples` 읽기용, `-a`, `-b`, `-c` 실행용)를 `01898ba` 에서 만들었고, 리뷰 후 모두 삭제했다. DB 는 SELECT 만 했다.
> 1. **A′·B 이식**: README §3 표대로 복사했다. `RestClientConfig` 는 삭제했고, `similarity.timeout-ms` 는 `connect-timeout=1s`·`read-timeout=2s` 로 바꿨다. B 는 추가로 `infra/**` 를 삭제하고 `SimilarityException` 을 `similarity.exception` 으로 옮겼다. ArchUnit 의 `@Disabled` 를 모두 풀고 `build`(B)·`test`(A′) 를 `-i` 로 돌렸다.
> 2. **점수 정책 프로브**: python 으로 가짜 Similarity 서버를 띄웠다(`127.0.0.1:18943`, chunked 요청 본문 지원, `/__mode/<값>` 으로 응답 전환). `@SpringBootTest(RANDOM_PORT)` + `HttpClient` 로 `POST /api/v1/answers` 를 호출했다. 대상은 실제 DB 문장 `00000000-…-0001` 이다. 응답 값은 13종이다: `0.83`, `"NaN"`(문자열), bare `NaN`, `1.5`, `-0.2`, `1.0000001`, bare `Infinity`, `"Infinity"`, `1e308`, `-1e-7`, `1.0000011`, 빈 200(`Content-Length: 0`), 3초 지연.
> 3. **A′ 규칙 (b) 프로브**: A′ worktree 에 임시 ArchUnit 조건을 넣었다. 조건은 "`infra.similarity..` 밖의 클래스는 그 안의 타입 중 `SimilarityScorer`·`exception` 외에 의존 금지" 다. 이 조건이 예시 코드를 통과시키는지 확인했다. 이어서 `sentence.service` 에 `SimilarityClient` 를 주입하는 우회 클래스를 넣어 탐지되는지도 확인했다.
> 4. **C 절차 문자 그대로 재현**: README §0 C 행에 적힌 파일만 적용했다. 대체 대상 4파일을 삭제하고 `compileJava` 를 돌렸다. 이어서 누락분을 보완해 ArchUnit 전체를 돌렸다.
> 5. **문서 대조**: README §0·§2·§3·§4·§5 를 `architecture-proposal.md` §0·§2·§3.1·§3.2·§4.3, `decisions-needed.md` §0·§1·D1·D13, `conventions-proposal.md:319`, `ai-workflow.md` §2.2·§4, `feature_list.example.json` 의 `FIX-F1T`·`ARCH-A2` 와 줄 단위로 비교했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | README §0 의 A′·B·C 결과와 §2 점수 정책 표가 전 구간 실행으로 그대로 재현된다. 13종 응답 모두 문서대로였고, R2 11건·R5 1건, C 의 R1 1건·R2 3건까지 일치했다. "원래 값은 `FIX-F3` 이후 로그" 라는 전제도 실행으로 맞았다(`out of range` 0회). 남은 것은 C 절차 문구에서 `AnswerScore` 가 빠진 것뿐이다(m1) |
| 구체성 | 4 | A′·B 는 §3 표대로 옮기면 바로 기동·채점된다. `FIX-F1T` acceptance 에는 이제 테스트할 점수 케이스가 들어 있다. A′ 규칙 (b) 도 ArchUnit 조건 하나로 옮겨 적을 수 있는 수준이고, 실제로 그렇게 적은 조건이 우회를 잡았다 |
| 일관성 | 4 | "경계 규칙 A′ 2개 / B 1개" 가 README·architecture-proposal·D1·`ARCH-A2` 에서 같다. C "신규 7 + 수정 1"·"23 수정" 도 맞는다. `FIX-F1T` 를 한 줄로 요약한 곳 3군데만 옛 범위(timeout·빈 응답)로 남았다(m2) |
| 판단 가능성 | 4 | A′·B·C 를 같은 기준으로 비교할 수 있게 됐다. 규칙 수, 우회 가능성, Toss 진입점 비용, C 의 R2·R4 교체가 모두 드러나 있다. D13 은 H0 에서 결정되는 구조라 `FIX-F1T` 에 앞서 사람이 고를 수 있다 |

## 치명 (승인 차단)

없음.

## 주요

없음.

Round 3 주요 3건은 모두 실행 또는 문서 대조로 해소를 확인했다(아래 해소 표). 이번 라운드에 새로 찾은 문제는 모두 결정에 실질 영향이 없는 문구·절차 수준이다.

## 경미

- **[m1] README §0 C 행 `:16` — 적힌 절차를 문자 그대로 따르면 컴파일이 실패한다.**
  - 원인: 적용 목록 "포트·유스케이스·서비스·`AnswerException`·`SentencePersistenceAdapter`" 에 `domain/AnswerScore.java` 가 없다.
  - 실행 결과 `cannot find symbol` 이 3건 났다: `ScoreAnswerUseCase.java:4,12`, `ScoreAnswerService.java:9`.
  - "JPA 리포지토리 이름만 AS-IS 로" 를 따르면 import 도 하나 필요하다. 예시는 같은 패키지의 `SentenceJpaRepository` 를 전제해 import 가 없기 때문이다. 이름만 바꾸면 `SentencePersistenceAdapter.java:14,17` 에서 2건이 더 난다.
  - `AnswerScore` 와 `import com.tmi.core.sentence.repository.SentenceRepository` 를 더하면 `compileJava` 가 성공했다. 그 뒤의 결과는 §0 서술과 정확히 일치했다(아래 검증 목록).
  - 수정: 목록을 "포트·유스케이스·서비스·도메인(`AnswerScore`)·…" 으로 고치고, 리포지토리 행에 "(import 추가)" 를 붙인다. Round 3 m5(`AnswerApi`) 와 같은 종류다.
- **[m2] `FIX-F1T` 를 한 줄로 요약한 곳 3군데가 옛 범위로 남았다.**
  - 위치: `decisions-needed.md:17`(§0 표), `architecture-proposal.md:220`(§4.3 단계 0), `ai-workflow.md:210`.
  - 세 곳 모두 "read timeout·빈 응답 → 503" 이다. `feature_list` 제목·acceptance 와 README §2 `:42` 는 점수 범위 정책(D13 (b))까지 포함한다.
  - `decisions-needed.md:11` 은 §0 항목을 "선택지가 없다" 고 소개한다. 그런데 같은 문서 `:37` 은 D13 이 `FIX-F1T` 를 막는다고 적는다.
  - 실해는 없다. D13 은 H0(`ai-workflow.md:96`, 루프 시작 전)에 결정되고, acceptance 는 사람 전용 필드라서 다른 선택지가 나오면 H0 에서 고치면 된다.
  - 수정: §0 행 3에 "+ 점수 범위 정책 (D13, H0 결정)" 을 붙이고, 나머지 두 곳도 같은 문구로 맞춘다.
- **[m3] README §0 B 행 `:15` 끝부분 — NaN 수정 이후 결과가 "로컬 단위 확인" 인데, HTTP 응답처럼 "→ 503" 으로 적혀 있다.**
  - 이번 라운드에 전 구간을 실행해 보니 서술대로였다. A′·B 동일하게 `"NaN"`·`NaN`·`1.5`·`-0.2`·`Infinity`·`"Infinity"`·`1e308`·`1.0000011`·빈 200·3초 지연은 503 이었다. `1.0000001` 은 200 `1.0` EXCELLENT, `-1e-7` 은 200 `0.0` POOR 였다.
  - 수정: 이 실행 기록으로 바꾼다. 선택 사항이다.
- **[m4] README 머리 개정 줄 `:6-8` 의 순서가 Round 1 → Round 3 → Round 2 다.** 시간 순으로 정렬한다.
- **[m5] `.gitignore:25` 의 `out/` 는 그대로다.**
  - `git check-ignore -v --no-index docs/ai/examples/answer-scoring/option-c-hexagonal/application/port/out/NewPort.java` 를 돌리면 여전히 `.gitignore:25:out/` 가 나온다.
  - 대신 `ai-workflow.md:143` 시나리오 14 에 수동 점검(`git status --short --ignored docs/ai src`)이 추가됐다. Round 3 m3 의 두 요구 중 하나만 반영된 것이다.
  - 예시 디렉터리에 파일을 더할 일이 생기면 그때 `!docs/**/out/` 한 줄을 넣는다.
- **[m6] `ARCH-A2` verify `feature_list.example.json:311` — `--tests '*ModuleArchitectureTest.r2_*'` 만 실행한다.**
  - 에이전트가 규칙 (b) 를 `r2_` 접두사가 아닌 별도 메서드로 만들면, 그 메서드는 verify 에서 돌지 않는다.
  - acceptance 가 "계획(H1)에 (a)(b) ArchUnit 코드 포함·사람 승인" 이라 H1 에서 잡히긴 한다.
  - 수정: acceptance 나 verify 에 "(a)(b) 모두 `r2_` 접두사 메서드" 를 명시하거나, 클래스 전체를 실행한다.

## 이전 라운드 지적 해소 여부

| 지적 (Round 3) | 상태 | 확인 |
|---|---|---|
| M1 NaN 이 점수 검사 통과 | **해소** | 판정식이 `!(raw >= -tol && raw <= 1 + tol)` 로 바뀌었다(A′ `SimilarityScorer.java:52`, B `:54`). clamp 는 그 뒤로 옮겨졌다. C 불변식도 `!(value >= 0.0 && value <= 1.0)` 다(`AnswerScore.java:7`). [실행, A′·B 동일] `"NaN"` 은 Round 3 의 200 에서 **503** 으로 바뀌었고, bare `NaN` 도 503 이다. README §2 `:42`·D13 `:37,140`·conventions `:319` 에 NaN 이 명시됐다 |
| M2 점수 정책 소유 항목·로그 전제 | **해소** | `FIX-F1T` 제목·acceptance(`feature_list.example.json:52,62`)에 `1.0000001 → 1.0`, `1.5 → Unavailable`, `"NaN" → Unavailable` 이 들어갔다. "원래 값 로그는 FIX-F3 이후" 도 명시됐다. 예시 주석(A′ `:48-49`, B `:50-51`), README §2, D13 (b) 에도 같은 전제가 적혀 있다. [실행] 503 경로의 로그는 `GlobalExceptionHandler` 한 줄뿐이고 `out of range` 는 0회였다. 이유는 `GlobalExceptionHandler.java:77` 이 `e.getMessage()` 만 찍기 때문이다. 이제 문서 서술과 일치한다. 남은 요약 3곳은 m2 |
| M3 A′ 규칙 (b) 전파 | **해소** | (b) 를 "HTTP Interface·dto·`*Config`·`*AuthClient` 는 같은 `infra/{시스템}` 안에서만, 밖에서는 시스템별 진입점만" 으로 다시 썼다(`architecture-proposal.md:67`). 진입점은 Similarity = `SimilarityScorer`, Toss = `TossHoldingsFetcher`·`TossTokenProvider` 다. 이로써 Toss 트리(`:60`)와의 충돌이 없어졌고, Toss 진입점 비용 +1 은 `:68` 과 D1 표에 적혔다. "1개" 서술 3곳은 README §4 `:113`, architecture-proposal P5 `:161`, D1 `:50` 에서 "2개" 로 고쳐졌다. `ARCH-A2` 제목·acceptance 에는 (a)(exception 포함)·(b) 가 들어갔다. [실행] 임시 (b) 조건은 A′ 예시 코드를 통과시켰고, `sentence.service` 에 넣은 `SimilarityClient` 주입 우회 클래스를 위반으로 잡았다 |
| m1 C 신규 8 vs 7 | 해소 | README §5 `:125` 가 "신규 7 + 수정 1" 이다 |
| m2 conventions clamp 주석 | 해소 | `conventions-proposal.md:319` |
| m3 `.gitignore` `out/` | **부분** | 수동 점검 줄은 추가됐고, `.gitignore` 는 그대로다(m5) |
| m4 B 재실행 기록 | 해소 | `:15` 에 Round 3 재실행 결과가 기록됐다. NaN 이후 부분은 m3 |
| m5 C 절차 `AnswerApi` | 해소 | `:16` 에 `AnswerApi` 가 추가됐다. 다만 같은 목록에서 `AnswerScore` 가 빠져 있다(m1) |
| m6 architecture-proposal C 트리 | 해소 | `:127` 에 "answer 흐름 부분이 examples 와 같다 — sentence 도메인 모델·매퍼는 예시에 없음" 이 추가됐다. P4 C 칸도 "포트 소유 record `SentenceForScoring`" 이다 |
| m7 C 의 R4 교체 | 해소 | README §0 `:16`, architecture-proposal `:146`, D1 `:52` |
| m8 `ResourceAccessException` 주석 | 해소 | A′ `:35`, B `:37` 에 "본문 없는 요청(GET 등)의 read timeout" 이 들어갔다 |

## 검증한 주장 (문제 없음 확인)

- **README §0 A′ 행 [실행]**: 컴파일과 `contextLoads` 가 통과한다. `@Disabled` 를 해제하면 R1·R3·R4·R4b·R6·R7 통과, R5 1건, R2 11건이다. 첫 R2 위반은 `AnswerService.<init>(…SentenceService, …infra.similarity.SimilarityScorer)` 다.
- **README §0 B 행 [실행]**: `build` 에서 `contextLoads` 가 통과한다. R1·R2·R3·R4·R4b·R6·R7 은 통과하고 R5 만 1건이다. `src/main` 의 `infra` 문자열은 주석 4줄뿐이다.
- **README §2 점수 정책 표 [실행, A′·B 동일, HTTP 응답 기준]**:
  - `0.83` → 200 GOOD
  - `1.0000001` → 200 `1.0` EXCELLENT
  - `-1e-7` → 200 `0.0` POOR
  - `1.0000011`·`1.5`·`-0.2`·`1e308` → 503
  - `"NaN"`·bare `NaN`·bare `Infinity`·`"Infinity"` → 503
  - 빈 200 → 503 (응답 시간 13~26ms)
  - 3초 지연 → 503 (2019~2028ms, read-timeout 2s 와 일치)
  - focus 의 8개 값이 모두 표대로였다.
- **README §0 C 행 [실행, m1 보완 후]**: `compileJava` 가 성공한다.
  - R1 위반은 1건이고 `common → infra → common`(`RestClientConfig`) 이다. `answer ↔ sentence` 순환은 없다.
  - R2 위반 3건은 모두 `sentence.adapter.out.persistence.SentencePersistenceAdapter → answer.application.port.out.LoadSentencePort(·$SentenceForScoring)` 이다.
  - R4·R4b 는 통과하고, R5·R7·R3 는 AS-IS 잔여분이다.
- **수치 대조**:
  - README §3 표에서 F1 행을 빼고 세면 A′ 8, B 12(dto 2개 포함)다. architecture-proposal §3.2 `:177-179` 의 A′ 8, B "신규 4 + 이동 2 + 삭제 3 + 수정 3 = 12" 와 일치한다.
  - C "신규 7 + 수정 1" 은 README §3 `:106`·§5 `:125` 와 architecture-proposal `:179` 에서 같다.
  - "23 수정" 은 `src/main` 34 − `common` 10 − `CoreApplication` 1 = 23 으로 계산이 맞다(`find` 확인). D1 표 `:52` 와도 같다.
  - README §4 import 수(9/6/6/6)도 다시 셌고 일치한다.
- **D1 ↔ README**: 규칙 수(A′ 2 / B 1), A′ 의 공개 표면 약점, C 의 R2·R4 교체가 D1 `:50-52`, README §4 `:113`·§5 `:120,124`, architecture-proposal §0 `:14`·§4.1-1 `:199` 에서 같다.
- **D13 ↔ README §2 ↔ 예시 코드 ↔ `FIX-F1T`**: 다섯 항목이 네 곳에서 같다. 빈 본문 503, 허용 오차 1e-6, NaN 거부, 원래 값은 cause(로그는 FIX-F3 이후), 600+ 보류다. A′·B `SimilarityScorer` 는 패키지·import·머리 주석을 빼면 본문이 같다.
- **C `SimilarityHttpAdapter`**: 본문 대신 `TODO` + `UnsupportedOperationException` 을 둔 형태가 유지된다. 그래서 한 줄 구현이 AS-IS 결함을 옮기는 일은 생기지 않는다.
