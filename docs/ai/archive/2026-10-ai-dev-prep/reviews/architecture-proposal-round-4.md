# architecture-proposal 리뷰 — Round 4

> 대상: `docs/ai/proposals/architecture-proposal.md` @ `01898ba`. 전용 worktree(`git worktree add --detach … 01898ba`)에서 읽었고, 라인 번호는 이 커밋 기준이다.
> 검증 방법:
> 1. 정적 대조에 쓴 파일: 대상 문서, `reviews/architecture-proposal-round-{1,2,3}.md`, `decisions-needed.md` D1·D9, `examples/answer-scoring/README.md`, `option-a-layered/` 7개 파일, `feature_list.example.json`(items 11개, pending_decision `D1=B` 3개·`D1=A'` 2개), `conventions-proposal.md` §2.4·§4.3, `ai-workflow.md` §6 표, `docs/public/{02,99}` 의 §0 표 신규 인용 위치, `src/test/java/com/tmi/core/architecture/*`, 그리고 `git diff 83f7b92 01898ba`.
> 2. 프로브 구현: 임시 `APrimeRulesProbeTest` 에 §2 A′ 규칙 (a)(b)(`:66-67`)를 **문구 그대로** ArchUnit `ArchCondition` 으로 구현했다.
>    - (a): 의존 대상의 `moduleOf` 가 출발 모듈과 다르고 `common`·`infra` 가 아닐 때, 모듈 이하 경로에 `.repository`·`.domain`·`.exception` 세그먼트가 있으면(깊이 무관) 위반이다.
>    - (b): 의존 대상이 `infra.{시스템}..` 에 있고 출발지가 같은 `infra.{시스템}..` 밖일 때, 대상(중첩 타입은 최상위 기준)이 다음 중 하나면 위반이다. `@HttpExchange` 계열(메타 포함) 메서드를 가진 타입, `infra.{시스템}.dto..`, 이름이 `*Config`, 이름이 `*AuthClient`.
> 3. 실행 1 (AS-IS): (a) **10건**이고 모두 `answer → sentence.{repository,domain,exception}` 이다. (b) **8건**이다. `AnswerService → SimilarityClient`·`dto` 가 6건, `RestClientConfig → SimilarityClient` 가 2건이다. 새 (b)는 `infra.*.exception` 을 막지 않아서 round-3 프로브의 11건보다 3건 적다.
> 4. 실행 2 (A′ 적용): `option-a-layered/` 7개 파일을 실제 경로에 복사하고 `RestClientConfig` 를 삭제했다. `similarity.timeout-ms` 는 `connect-timeout`/`read-timeout` 로 바꿨다. 결과는 (a)(b) **모두 통과**다.
> 5. 실행 3 (A′ + §2 트리의 Toss 배치, 스텁): 실행 2 위에 `infra/toss/` 를 만들었다. 내용은 `TossAuthClient`(`@PostExchange`), `TossTokenProvider`, `TossHoldingsClient`(`@GetExchange`), `TossHoldingsFetcher`(재시도·분류 → `TossException.Rejected/Unavailable`), `TossClientConfig`, `dto/HoldingsApiResponse`, `exception/TossException` 이다. 여기에 conventions §4.3 대로 `toss/holdings/service/{HoldingsSnapshotService, HoldingsSnapshotMapper}` 를 두었다.
>    - 3a: Fetcher 가 `dto.HoldingsApiResponse` 를 그대로 반환하는 경우다. (a)는 통과했다. (b)는 **3건 위반**이다. 모두 `HoldingsSnapshotMapper → infra.toss.dto.HoldingsApiResponse`(파라미터 타입, `result()`, `Result.symbol()`)다. 서비스 자신은 지역 변수 타입이 바이트코드 의존에 남지 않아 걸리지 않는다.
>    - 3b: Fetcher 가 `infra/toss` base package 의 record(`TossHoldings`)로 바꿔 반환하는 경우다. (a)(b) **모두 통과**했다.
> 6. 실행 4 (탐지력, 3b 위에 위반 픽스처를 추가):
>    - `toss.holdings.service` 에서 `TossHoldingsClient`·`TossAuthClient`·`SimilarityClient`·`TossClientConfig` 를 참조하게 했다. (b) **4건**을 모두 탐지했다.
>    - `answer.service` 에서 `toss.holdings.repository.HoldingsSnapshotRepository`·`toss.holdings.domain.HoldingsSnapshotEntity`·`sentence.dto.SentenceResponse` 를 참조하게 했다. (a)는 앞의 2건을 탐지했고, 웹 DTO 는 통과했다. 문서가 A′ 의 약점으로 적은 그대로다.
>    - `infra.toss → toss.holdings.domain` 은 (a) 1건으로 탐지했다.
> 7. 명령은 모두 `bash ./gradlew test --offline --no-daemon --tests '*APrimeRulesProbeTest'` 로 실행했다. DB 는 쓰지 않았다. 임시 파일은 worktree 안에만 만들었고, 실행 후 `git checkout -- . && git clean -fd` 로 되돌렸다. worktree 는 `git worktree remove --force` 로 정리했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | 새 A′ 규칙 (a)(b)를 문구 그대로 구현해 실행했다. A′ 적용 코드는 통과했고, AS-IS 는 (a) 10건·(b) 8건이 나왔으며, 2단 패키지(`toss.holdings.repository`)가 보호되고 Toss 클라이언트 우회가 탐지됐다. 문서의 주장은 실행과 맞는다. 남은 것은 Toss 반환 타입이 명시되지 않은 점 하나다(m1) |
| 구체성 | 4 | B 경로는 바로 적용할 수 있다. A′ 경로도 규칙 판정 대상이 관찰 가능한 조건(HTTP Interface·`dto`·`*Config`·`*AuthClient`, `..repository..` 깊이 무관)으로 바뀌어 그대로 ArchUnit 코드가 된다. Fetcher 의 반환 타입만 정해져 있지 않다(m1) |
| 일관성 | 4 | "A′ 2개 / B 1개" 는 §0·§3.1·§4.1-1·D1·README §4·§5·conventions §4.3 에서 모두 같다. §4.3 과 feature_list 는 depends_on 1곳(SIG-2)을 빼면 1:1 이다. 그 밖에는 문서 목록의 열거 누락(99 §1 트리)과 머리말 같은 문구 수준만 남았다 |
| 판단 가능성 | 4 | §0 에 B 의 순이득 3개와 비용이 한 문단으로 정리돼 있고, §3.3 질문표와 D1 표로 결정할 수 있다. A′ 의 Toss 진입점 비용도 결정자에게 보인다(`:68`, D1). m1 은 A′ 비용을 1타입 더 늘리는 방향이라 추천 B 를 바꾸지 않는다 |

## 치명 (승인 차단)

없음.

## 주요

없음.

## 경미

- [m1] **§2 A′ (b)(`:67`)·`:68` 은 Toss 진입점의 반환 타입을 적지 않았다. Fetcher 가 클라이언트 dto 를 그대로 돌려주면, conventions §4.3 의 매퍼 배치가 (b) 위반이 된다** (round-3 M1 의 잔여).
  - 근거: 실행 3a 에서 (b) 위반 3건이 나왔다(`toss.holdings.service.HoldingsSnapshotMapper → infra.toss.dto.HoldingsApiResponse`).
    - conventions §4.3(`conventions-proposal.md:482`)은 A′ 에서 "서비스는 그 결과와 도메인 예외만 받는다" 라고만 적는다.
    - 같은 절의 매퍼(`:530`)는 `toss/holdings/service` 에 있고 응답 dto 를 받는다.
    - 매퍼를 `infra/toss` 로 옮기면 이번에는 `infra.toss → toss.holdings.domain` 이 되어 (a)가 막는다(실행 4).
    - 그래서 통과하는 형태는 Fetcher 가 `infra/toss` base package 의 별도 record 를 반환하는 경우뿐이다(실행 3b).
  - 영향: A′ 를 골라 Toss 를 완성하는 시점(D9)에만 드러난다. A′ 비용이 "진입점 클래스 1개" 에서 "진입점 1 + 결과 record 1" 로 늘어난다. 추천 B 에 유리한 방향이고 D1 판단을 바꾸지 않는다.
  - 요구 수정: `:67` 에 "진입점은 `infra/{시스템}` base package 의 타입(결과 record 포함)만 반환한다(dto 반환 금지)" 를 한 줄 추가한다. `:68` 은 "Fetcher + 결과 record" 로 고친다. 아울러 `TossException` 의 위치(`infra/toss/exception/`)를 `:60` 트리에 적는다. 현재 트리에는 `exception/` 이 없다.
- [m2] **§4.3 SIG 행의 depends_on(`:221` "— / SIG-1")이 feature_list 와 다르다.** 이 표기는 SIG-2 가 SIG-1 에 의존한다는 뜻이다. 그런데 `feature_list.example.json:112` 의 SIG-2 는 `depends_on: []` 이고, ai-workflow §6 표(`ai-workflow.md:213`)도 "—" 다. round-3 도 놓친 기존 불일치다(`83f7b92` 에서도 같음). 표 제목이 "1:1" 이므로 "— / —" 로 고친다.
- [m3] **`99 §1 트리` 변경이 §0 표 밖 목록에서 빠졌다.** §0 표(`:25`)는 A′·B 모두 99 §1 트리(`99_공통.md:13` `RestClientConfig`)를 바꾼다고 적는다. 그런데 아래 세 곳에는 이 항목이 없다.
  - §3.2 "기존 문서 수정"(`:182`)
  - DOC-2 title 두 개(`feature_list.example.json:281`, `:323`)
  - D1 "둘 다 바꾸는 기존 규칙"(`decisions-needed.md:55`). 이 줄에는 A′ 의 05 §1.3 신설, B 의 02 §3.1 트리도 없다.

  DOC-2 acceptance 가 "§0 표 … 해당 행을 모두" 를 기준으로 하므로 실제로 누락될 위험은 낮다. 목록에 추가한다.
- [m4] **(b) 의 제한 대상 표기가 위치마다 조금씩 다르다.** 대상 문서(`:67`)는 "HTTP Interface·`dto`·`*Config`·`*AuthClient`" 다. ARCH-A2 title(`feature_list.example.json:302`)과 README §5(`:120`)는 "클라이언트·dto·config" 다. `TossAuthClient` 도 HTTP Interface 라서 판정 결과는 같다(실행 4). 결정에 영향이 없는 문구 차이다. 구현자가 ARCH-A2 의 H1 계획에서 대상 문서의 정의를 쓰도록 title 에 "(§2 A′ (b) 정의)" 를 붙이면 충분하다.
- [m5] **머리말 Round 2 개정 줄(`:7`)이 그대로다** (round-3 m11 미반영). Round 3 줄(`:8`)은 "경미 11건" 을 반영했다고 적는다. 그러나 m11 이 요구한 round-2 m1·m4~m8 의 기재는 추가되지 않았다.
- [m6] **§5 R2 행 "B 기준 9"(`:242`)와 새 (b)의 범위 차이가 설명돼 있지 않다** (참고). R2 의 9건에는 `AnswerService → infra.similarity.exception` 3건이 들어 있다. 새 A′ (b)는 exception 을 막지 않으므로 같은 AS-IS 에서 8건(AnswerService 6 + RestClientConfig 2)을 낸다(실행 1). 둘 다 사실이고 결론에도 영향이 없다. 다만 A′ 구현자가 "(b) 9건" 을 기대하지 않도록, §2 (b) 끝에 "`exception` 은 제한하지 않음 — `GlobalExceptionHandler` 가 `BaseException` 으로 처리" 를 한 구절 덧붙이면 좋다.

## 이전 라운드 지적 해소 여부 (Round 3)

| 지적 | 판정 | 확인 내용 |
|---|---|---|
| M1 A′ 규칙 (b)가 Toss 배치를 막음 / "규칙 1개" | **해소** (잔여 경미 → m1) | (b)가 시스템별 진입점 목록 형태로 다시 쓰였다(Similarity=`SimilarityScorer`, Toss=`TossHoldingsFetcher`·`TossTokenProvider`). `TossAuthClient`·`TossTokenProvider` 위치는 트리 `:60` 에 있다. (a)의 깊이 무관 판정(`..repository..`)이 명시됐다. 실행 2·3b 로 A′ 와 Toss 가 함께 통과하고, 실행 1·4 로 AS-IS 위반과 Toss·Similarity 우회를 탐지한다. "A′ 2개 / B 1개" 는 §0(ii) `:14`, §3.1 P5 `:162`, §4.1-1 `:199`, D1 `decisions-needed.md:50,54`, README §4 `:113`·§5 `:120`, conventions `:482` 에서 모두 같다. A′ 비용의 Toss 진입점 1개는 `:68`·D1 에 반영됐다. 반환 타입 미지정만 남았다 |
| m1 D1 C 행 | 해소 | `decisions-needed.md:52` 가 "포트가 answer 소유 record 를 반환해야 순환이 없고, R2·R4 를 C 전용 규칙으로 교체" 로 바뀌었다 |
| m2 A′ exception 포함 위치별 상이 | 해소 | §0 표 `:26`, §2 (1) `:51`, (a) `:66`, ARCH-A2 title `:302`·acceptance 가 모두 `repository·domain·exception` 이다 |
| m3 C 트리 과장 | 해소 | `:126` 이 예시 범위와 Sentence 모델·매퍼를 구분하고, `:131` 에 `exception/AnswerException.SentenceNotFound` 가 추가됐다 |
| m4 C 23 | 해소 | `:178` "최대 23 (`src/main` 34 − `common` 10 − `CoreApplication` 1)". 실제 파일 수(34, common 10)와 맞다 |
| m5 B 소요 하한 | 해소 | `:177` "약 1일", `:201` "시간 차 약 0.5일", D1 `:51` "약 1일" 이 서로 같다 |
| m6 §0 표 누락 3곳 | 해소 | `:24` 02 §4.2 3·4단계(`02_설계.md:147-148`), `:25` 02 §3.1 트리(`:91` `infra/`)·99 §1 트리(`99_공통.md:13`). 원문 위치를 확인했다. 표 밖 목록의 전파는 → m3 |
| m7 §4.3 2-A′ 05 누락 | 해소 | `:227` "→ 02·05·99 문서" |
| m8 "2줄 수정" | 해소 | `:230` "몇 줄 수정 — 패키지·import·가시성" |
| m9 (a) 출처 | 해소 | `:66` "round-3 리뷰 실행 1·2; round-2 의 exception 제외형은 9건" |
| m10 README 수치 | 해소 | README §3 `:99`·§5 `:125` 모두 "신규 7", §0 C 행 `:16` 에 `AnswerApi` 삭제가 포함됐다 |
| m11 머리말 | **미해소** | → m5 |

## 검증한 주장 (문제 없음 확인)

- **A′ 규칙 (a)(`:66`)**:
  - 문구 그대로 구현하면 A′ 적용 코드는 통과하고 AS-IS 는 10건이다(실행 1·2). round-3 실행값과 같다.
  - `toss.holdings.repository|domain` 같은 2단 패키지도 보호한다(실행 4).
  - 웹 DTO `sentence.dto.SentenceResponse` 재사용은 통과한다. 이는 §3.1·§4.1-1·README §5 가 A′ 약점으로 적은 그대로다.
- **A′ 규칙 (b)(`:67`)**:
  - Similarity 쪽: A′ 적용 코드는 통과한다. AS-IS 에서는 `AnswerService → SimilarityClient`·`dto` 우회 6건과 `common.config.RestClientConfig → SimilarityClient` 2건을 탐지한다.
  - Toss 쪽: §2 트리대로 배치하면 진입점(`TossHoldingsFetcher`·`TossTokenProvider`) 경유는 통과하고, `TossHoldingsClient`·`TossAuthClient`·`TossClientConfig` 직접 참조는 탐지한다(실행 3b·4). 반환 타입 조건만 추가로 필요하다(m1).
  - "B 는 R2 하나로 같은 효과" 는 `ModuleArchitectureTest.isModuleApi` 의 base package 판정과 맞는다. 이 판정에서는 `toss.holdings.client`·`toss.auth` 가 internal 이다.
- **"A′ 2개 / B 1개" 전파**: §0 `:14`, §3.1 `:162`, §4.1-1 `:199`, D1 `:50`·`:54`(경계 규칙 수 1 vs 2), README §4 `:113`·§5 `:120`, conventions §4.3 `:482` 가 서로 같다. "1개로 같다" 는 문장은 대상 문서·D1·README 어디에도 남아 있지 않다.
- **§0 표(`:18-26`) ↔ 원문·feature_list**:
  - 신규 인용 위치 3곳(02 §3.1 트리 `02_설계.md:91`, 02 §4.2 4단계 `:148`, 99 §1 트리 `99_공통.md:13`)이 원문과 맞다.
  - DOC-2(B) title(`:281`)은 §0 B 열의 02 §3.1·§3.2·§4.2·§5.1, 05 §1.3, 06 §6.5, 99 §2.3 을 담고 있다. 99 §1 만 빠졌다(m3).
- **§3.2(`:172-182`) ↔ README §3·feature_list**:
  - A′ 8 = 신규 3 + 수정·삭제 5. `SimilarityClient` F1 수정은 구조 무관이라 제외했고, README §3 A′ 표와 같다.
  - B 12 와 C "23 + 20 이상" 의 내역도 README §3 과 같다.
  - 산정식은 A′ 8×15~20분 + 리뷰 1회 → 0.5일, B 12×15~20분(3~4h) + 리뷰 2회 + 가산 → 약 1일로 재현된다.
- **§4.3(`:215-231`) ↔ feature_list**:
  - items 11개와 pending 5개가 모두 표에 있다.
  - depends_on 이 일치한다: TEST-1→FIX-F1·F2, FIX-F1T←FIX-F1, FIX-F3←FIX-F2·SIG-2, FIX-R5←TEST-1, FIX-F4←FIX-R5·SIG-1, ARCH-A1←FIX-F1, DOC-1←FIX-F3·F4, ARCH-B2←ARCH-A1·FIX-F1T·SIG-1, ARCH-B3←B2, DOC-2←B3 / ARCH-A2←ARCH-A1·FIX-F1T, DOC-2←A2. 예외는 SIG-2 하나다(m2).
  - 완료 신호가 각 항목의 verify·acceptance(R7, `SimilarityTimeoutTest`, DB 차단 test, `spotlessCheck`, R5→R6, `OsivConnectionIT`, R1·R3, `infra` 소멸, R2, human)와 맞는다.
  - ARCH-B2 의 SIG-1 의존은 ai-workflow `:219` 와도 같다.
- **D1(`decisions-needed.md:50-56`)**: A′ "8파일·0.5일", B "12파일·약 1일", "약 4파일·0.5일" 차이, Toss 진입점 1개가 대상 문서 §3.2·§4.1-3·`:68` 과 같다.
