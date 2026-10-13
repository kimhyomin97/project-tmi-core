# architecture-proposal 리뷰 — Round 2

> 대상: `docs/ai/architecture-proposal.md` @ `3e136c6` (전용 worktree 에서 읽음. 라인 번호는 이 커밋 기준)
> 검증 방법:
> 1. 정적 대조: 대상 문서, `reviews/architecture-proposal-round-1.md`, `examples/answer-scoring/**`(README + A′·B 예시 전 파일), `feature_list.example.json`, `decisions-needed.md` D1, `ai-workflow.md` §4, `current-state.md` §6, `src/test/java/com/tmi/core/architecture/*`, `src/main/**` 34파일, `docs/public/{02,05,06,99}`, `docs/public/toss/00_인증.md`, `wip-snapshot.patch`.
> 2. 실행 1 (A′ 적용, 파일 수 재계수): worktree `3e136c6` 의 `src/main` 에 `option-a-layered/` 7파일을 실제 경로로 복사하고 `RestClientConfig` 를 삭제했다. `similarity.timeout-ms` 는 `connect-timeout`/`read-timeout` 로 바꿨다. `git status` 결과 변경 9파일이 나왔다. F1 `SimilarityClient` 를 빼면 **8**이다.
> 3. 실행 2 (같은 상태에서 ArchUnit): `junit-platform.properties` 로 `@Disabled` 를 끄고 `./gradlew test --offline --tests 'com.tmi.core.architecture.*'` 를 돌렸다. 컴파일에 성공했다. R1·R3·R4·R4b·R6·R7 통과, R5 1건(`getRandom`), R2 11건이다. examples README §0 과 같다.
> 4. 실행 3 (A′ 규칙 프로브): 기능 쌍을 열거하지 않는 **단일** `ArchCondition` 을 임시 테스트 `APrimeRuleProbeTest` 로 작성했다. 조건은 "origin 모듈 ≠ target 모듈이고 target 이 `{모듈}.repository|domain` 이면 위반" 이고, 기존 `ArchitectureClasses.moduleOf` 를 재사용한다. 결과: A′ 적용 상태에서 **통과**, AS-IS(`git checkout HEAD -- src/main`)에서 **9건 위반**.
> 5. Spring Modulith 2.0.8 `spring-modulith-core` 소스(scratchpad 에 이미 받아 둔 사본, `pom.properties` version=2.0.8)를 읽었다. 대상은 `ApplicationModule.java:1408`(OPEN 대상 의존은 무조건 허용), `ApplicationModules.java:625-632`(사이클 검사가 모든 모듈 대상), `NamedInterfaces.java:71-90`(`ofAnnotatedPackages`·`ofAnnotatedTypes`)이다.
> 6. DB 는 쓰지 않았다. 임시 파일은 worktree 안에만 만들었고 worktree 는 `git worktree remove --force` 로 정리했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | 파일 수·위반 수·단계 의존은 실행으로 맞는다. 그러나 B 를 A′ 와 가르는 근거 3개(규칙 1개 vs 쌍마다, "생길 수 없다", Modulith 재배치) 중 2개는 근거가 반대이고 1개는 과장이다 |
| 구체성 | 3 | 단계표·파일 목록은 바로 쓸 수 있다. 하지만 A′ 규칙 스니펫은 실행 불가 의사코드다. B 의 외부 클라이언트 배치 규칙은 "가능성", "별도 개념" 처럼 판정 불가한 조건이라 Toss 설계(`TossAuthClient`)에 적용할 수 없다 |
| 일관성 | 3 | "feature_list 와 1:1" 이라 했지만 `FIX-F1T`·`DOC-1` 이 빠졌다. B 가 바꾸는 기존 문서 목록에 02 §5.1·06 §6.5 가 없다. 소요 산정식이 표의 수치를 재현하지 못한다. ARCH-A2 acceptance 와 A′ 규칙 범위가 다르다 |
| 판단 가능성 | 3 | §0 표·§3.3 질문표로 결정 구조는 좋아졌다. 하지만 A′ 의 약점이 실제보다 크게 그려져 있어, 결정자가 B 의 순이득을 정확히 볼 수 없다 |

## 치명 (승인 차단)

- **[C1] §0·§3.1(P5, Modulith 행)·§4.1-1·D1 — B 를 A′ 와 가르는 근거가 검증되지 않았거나 사실과 반대다. 새 A′ 정의 자체는 공정하지만 비교가 공정하지 않다.**
  - 근거:
    - (a) **"A′ 는 경계 규칙을 기능 쌍마다 선언"** — §3.1 P5 행(`:152`), §4.1-1 "규칙도 기능 쌍마다가 아니라 R2 하나다"(`:189`), examples README §5 "3기능이면 최대 6개 방향", decisions-needed D1 단점(`decisions-needed.md:49`)이 모두 이 주장을 쓴다. 그런데 현재 R2 자체가 기능 쌍을 열거하지 않는 커스텀 `ArchCondition` 이다(`ModuleArchitectureTest.java:71-91`, `ArchitectureClasses.moduleOf`). A′ 규칙도 같은 방식으로 **규칙 1개**가 된다. 실행 3: 단일 조건 하나로 A′ 적용 코드는 통과하고 AS-IS 는 9건을 탐지했다. §2 A′ 의 스니펫(`:60`)은 `resideInAPackage("com.tmi.core.(X)..")` 와 `(Y)` 를 서로 참조하는데, ArchUnit 의 두 술어는 캡처 그룹을 공유하지 못한다. 그래서 그대로는 쓸 수 없는 의사코드다. 결국 "쌍마다" 라는 약점은 이 스니펫 표기 방식에서 나온 것이지 A′ 구조의 성질이 아니다.
    - (b) **"B 는 … 이것이 생길 수 없다"**(`:189`, `SentenceResponse` 재사용) — 같은 문서 §4.2 마지막 행(`:203`)이 "base package 에 아무 클래스나 두면 공개됨" 을 약점으로 인정한다. 그러니 B 에서도 `SentenceResponse` 를 base package 로 옮기거나 `SentenceReader` 가 웹 DTO 를 반환하면 같은 결합이 생긴다. B 의 실제 이득은 "불가능" 이 아니다. 공개 표면을 넓히려면 **파일을 base package 로 옮기는 diff 가 반드시 생겨 리뷰에서 보인다** 는 것이다.
    - (c) **§3.1 "Spring Modulith 로의 확장 — A′: 패키지 재배치 필요"**(`:158`) — Modulith 는 base package 외에 `@NamedInterface` 를 붙인 패키지·타입도 공개 인터페이스로 인정한다(`NamedInterfaces.discoverNamedInterfaces` → `ofAnnotatedPackages`, `ofAnnotatedTypes`). A′ 는 `sentence/service`·`sentence/dto`(그리고 `infra/similarity`)에 `package-info.java` + `@NamedInterface` 를 두면 재배치 없이 표현된다. B 의 "`common` OPEN 1파일" 과 비교하면 A′ 는 "기능당 package-info 1~2파일" 이다. 차이는 있지만 "재배치 필요" 는 아니다.
  - 영향: §0 이 꼽은 두 차이 중 첫째("모듈 출입구를 패키지 규칙 **하나로** 고정")에서 "하나로" 부분이 성립하지 않는다. 남는 B 의 순이득은 두 가지다. (i) 공개 표면이 패키지 전체(service·dto)가 아니라 base package 에 둔 타입으로 좁다. (ii) Modulith 기본 규칙과 같아 annotation 이 덜 든다. 비용은 02 §3.2·05 §1.3(+02 §5.1·06 §6.5, [M2]) 규칙 변경과 4파일이다. "`infra/` 해체" 는 §0 에 "차이" 로 적혀 있지만 이득이 아니라 비용이다.
  - 요구 수정: §2 A′ 스니펫을 "기능 쌍 열거 없이 `moduleOf` 기반 조건 1개(R2 와 같은 구조)" 로 바꾼다. 실행 3 의 형태를 그대로 써도 된다. §3.1 P5·§4.1-1·examples README §5·D1 에서 "쌍마다" 를 삭제한다. §4.1-1 의 "생길 수 없다" 는 "공개 표면을 넓히려면 파일 이동이 필요해 리뷰에서 드러난다" 로 고친다. §3.1 Modulith 행 A′ 는 "`@NamedInterface` package-info 기능당 1~2파일" 로 고친다. §0 은 B 의 순이득 (i)(ii) 와 비용을 한 문장으로 다시 쓴다. 이렇게 고친 뒤에도 B 추천은 유지할 수 있다(아래 "추천 B 의 정당성" 참조). 다만 근거가 지금보다 좁다는 점을 문서가 스스로 말해야 한다.

## 주요

- **[M1] §4.3 "feature_list.example.json 과 1:1"(`:205`) — 항목 2개가 빠졌고, 빠진 항목이 구조 단계와 같은 코드를 건드린다.**
  - 근거:
    - `items` 의 11개 id(TEST-1, FIX-F1, FIX-F1T, FIX-F2, SIG-1, SIG-2, FIX-F3, FIX-R5, FIX-F4, ARCH-A1, DOC-1) 중 **`FIX-F1T`·`DOC-1`** 이 §4.3 에 없다. 표에 있는 9개 항목과 `pending_decision` 3개의 `depends_on` 은 모두 feature_list 와 일치함을 확인했다. ai-workflow §4 표(`ai-workflow.md:192-202`)와 decisions-needed §0(`:17`)에는 둘 다 있다.
    - `FIX-F1T` 는 `AnswerService` 에 `RestClientException` catch 와 null 처리를 넣는다. `ARCH-B2`/`ARCH-A2` 는 같은 catch 를 `SimilarityScorer` 로 옮긴다. FIX-F1T acceptance 도 "ARCH-B2/A2 에서 SimilarityScorer 로 이동 시 테스트 유지" 라고 적는다. 그런데 `ARCH-B2`·`ARCH-A2` 의 `depends_on` 은 `ARCH-A1` 뿐이다. 그래서 B2 가 F1T 보다 먼저 실행될 수 있고, 그러면 F1T 의 수정 대상(`AnswerService` 의 catch)이 이미 없다. 순서가 정의되지 않았다.
    - §4.2(`:199`)는 문서 수정을 `DOC-1` 에 포함한다고 쓰지만, `DOC-1` 의 범위(feature_list title "05/06/99/03", acceptance "conventions §8 표")에는 B 가 바꾸는 02 §3.2·§3.1·§5.1 이 없다.
  - 요구 수정: §4.3 에 `FIX-F1T`(단계 0, depends_on FIX-F1)와 `DOC-1` 을 추가한다. `ARCH-B2`·`ARCH-A2` 의 `depends_on` 에 `FIX-F1T` 를 넣거나, "F1T 이전에 B2/A2 를 하면 F1T 는 SimilarityScorer 에 적용" 이라고 명시한다. D1 결정 시 02 문서 수정을 맡을 항목(`DOC-1` 확장 또는 `DOC-2`)을 정한다.

- **[M2] §0 표·§3.2 "공통 문서 수정"(`:172`) — B(그리고 C)가 바꾸는 기존 문서가 두 곳 더 있다 (Round 1 C2 의 잔여).**
  - 근거: `docs/public/02_설계.md:165` §5.1 "위치 | `com.tmi.core.infra.similarity.SimilarityClient`". `docs/public/06_코드가이드라인.md:418` §6.5 "같은 패턴을 `infra/` 외부 시스템 예외에도 적용". 두 곳 모두 B 에서 틀린 문장이 된다. §0 표와 §3.2 는 B 의 변경을 "02 §3.2, 02 §3.1 트리, 05 §1.3, 99 §2.3" 으로만 적는다. 02 §4.2 의 3번 단계(`SentenceRepository.findById` 를 AnswerService 흐름에 적은 시퀀스)는 A′·B 모두에서 틀린 문장이 된다.
  - 요구 수정: §0 표와 §3.2 에 02 §5.1·06 §6.5(B·C)와 02 §4.2 시퀀스(A′·B·C)를 추가한다.

- **[M3] §2 B "외부 시스템 클라이언트 배치 규칙"(`:67`) — 판정 조건이 주관적이고, Toss 자체 설계와 맞지 않는다.**
  - 근거:
    - 규칙의 조건은 "쓰는 모듈이 2개 이상이 될 **가능성**이 있거나 **별도 개념**이면 독립 모듈" 이다. Similarity 를 지금 쓰는 모듈은 `answer` 1개다. 그런데도 "별도 개념" 이라서 독립 모듈이 되고, Toss 는 그렇지 않다. 같은 질문에 에이전트·사람이 다른 답을 낼 수 있다. P5(AI 작업 단위)·ai-workflow 체크리스트로 검증할 수 없다.
    - `docs/public/toss/00_인증.md:37-39` 는 `TossAuthClient`·`TossTokenProvider`(인증 공통)와 "도메인 클라이언트(`TossHoldingsClient` **등**)" 를 분리한다. Toss 기능이 holdings 외로 늘어날 것을 전제한 설계다. 그런데 B 트리는 클라이언트를 `toss/holdings/client`(`:81`)에 둔다. 인증 클라이언트를 어디에 둘지 정하지 않았으니, P2("지금 정하지 않으면 굳는다")를 Toss 에 대해서는 풀지 못한 셈이다.
  - 요구 수정: 조건을 관찰 가능한 사실로 바꾼다. 예: "현재 호출 모듈이 1개면 그 모듈 internal, 2개 이상이 되면 독립 모듈로 승격(별도 항목)". Similarity 를 지금 독립 모듈로 두는 이유는 따로 적는다. B 트리의 toss 를 `toss/{auth, holdings/{client,…}}` 처럼 00_인증 §4 와 맞춘다.

- **[M4] §3.2 소요 산정(`:162`, `:166-168`) — 적은 산정식으로 표의 수치가 나오지 않는다 (Round 1 m10 미해소).**
  - 근거: 기준은 "파일당 15~20분 + PR 리뷰 1회" 다. A′ 8파일 = 2.0~2.7h(+리뷰) → 0.5일이라 맞다. B 12파일 = 3.0~4.0h(+리뷰). 리뷰가 2회(ARCH-B2 plan gate, B3)여도 0.5~1일 정도인데 표는 **1~1.5일**이다. C 42파일 이상 = 10.5~14h → 약 2일인데 표는 **3~5일**이다. 파일 수 비율은 A′:B = 1:1.5 인데 시간 비율은 1:2~3 이어서, 결정자에게 B 가 실제보다 비싸게 보인다. D1(`decisions-needed.md:49-51`)도 같은 수치를 쓴다.
  - 요구 수정: 산정식으로 수치를 다시 계산하거나, 추가 요인(`git mv` 별도 커밋, plan gate 대기, 테스트 이동)을 항목별 가산으로 적는다.

## 경미

- [m1] §1 P1(`:29`) 이 근거로 "ArchUnit R2 위반 19건" 을 든다. 그런데 19건 중 9건(`answer→infra.similarity.*`)은 B 규칙에서만 위반이다(`current-state.md:463`). 선택지 중립이어야 할 문제 정의에 B 기준 수치가 들어가 있다. "현행 규칙으로도 문제인 10건(`answer→sentence.*`)" 으로 적는다.
- [m2] `feature_list.example.json` ARCH-A2 acceptance 는 "sentence.repository·domain·**exception** 을 import 하지 않음" 이다. §2 A′ 규칙(`:46`, `:60`)은 repository·domain 만 금지한다. A′ 에서 타 기능 `exception` 패키지 접근을 허용하는지 정하고 둘을 맞춘다. 실행 3 의 프로브는 repository·domain 만 막는 형태라 AS-IS 위반이 9건이고, exception 까지 막으면 10건이 된다.
- [m3] §4.1-4(`:192`)는 "이건 A′ 에도 있다 — B 만의 이득은 아니다" 라고 스스로 적는다. 그렇다면 B 추천 근거 목록에서 빼고 공통 사항으로 옮긴다.
- [m4] §4.3 단계 3 의 depends_on "2"(`:216`)는 id 가 아니다. "ARCH-B3 또는 ARCH-A2" 로 적는다.
- [m5] §3.2 계수 기준은 `src/main` 만 센다. 그래서 B 의 테스트 이동(TEST-1 의 `SimilarityClientTest`, FIX-F1T 의 `SimilarityTimeoutTest` 가 `infra.similarity` 패키지 → `similarity.*`)과 A′ 의 ArchUnit 교체(`ModuleArchitectureTest` R2, `ArchitectureClasses.isModuleApi`, `current-state §6` 갱신)가 빠진다. 양쪽 다 1~3파일이라 결론은 바뀌지 않지만, 제외한다는 사실을 한 줄로 적는다.
- [m6] §5 R2 "현재" 열(`:230`)은 "→ `ARCH-B3`" 인데, 테스트의 `@Disabled` 사유(`ModuleArchitectureTest.java:41`)는 "ARCH-B2·B3 완료 시 해제" 다. 한쪽으로 맞춘다.
- [m7] §4.1-5 의 Modulith 근거가 "round-1 리뷰 M1" 재인용뿐이다. 문서 자체에 소스 위치(`ApplicationModule.java:1408` OPEN 허용, `ApplicationModuleInformationFactory.java:173`)를 적어 두면 리뷰 문서 없이도 확인할 수 있다.
- [m8] §4.3 하단(`:218`)의 "ARCH-A1 의 `SimilarityClientConfig` 는 B 에서 `git mv` 로 이동만" 은 정확하지 않다. B 예시는 같은 클래스를 package-private 으로 바꾸고 `similarity.client.SimilarityClient` 를 import 한다(`option-b-modular/similarity/config/SimilarityClientConfig.java:9,24`). 즉 "이동 + 2줄 수정" 이다. 버려지는 작업이 없다는 결론은 유지된다.
- [m9] 머리말 개정 줄(`:6`)은 C1·C2·M1·M2 만 언급한다. M3~M6·m1~m10 도 본문에 반영됐으니 함께 적는다.

## 이전 라운드 지적 해소 여부 (해당 시)

| 지적 | 판정 | 확인 내용 |
|---|---|---|
| C1 §4.3 신호·의존 불일치 | **대부분 해소** | 표에 있는 항목의 id·depends_on·신호가 feature_list 와 일치한다. R4 신호 삭제, R1 단계 1 이동, AnswerService 경유는 `ARCH-A2` 로 분리, Toss 는 "D9 결정 후 등록" 으로 처리됐다. 단 `FIX-F1T`·`DOC-1` 누락 → [M1] |
| C2 기존 문서 규칙 은폐 | **대부분 해소** | §0 표가 02 §2.2·02 §3.2·05 §1.3·99 §2.3 을 선택지별로 구분한다. 02 §5.1·06 §6.5·02 §4.2 누락 → [M2] |
| M1 Modulith "그대로 verify()" | **해소** | "common OPEN 이면 같은 규칙, 그 외 미검증" 으로 정정됐다. 소스로 재확인했다: OPEN 대상 의존은 무조건 허용(`ApplicationModule.java:1408`). 사이클 검사는 `common` 을 포함한 모든 모듈 대상이다(`ApplicationModules.java:625-632`). 모든 모듈이 `common` 에 의존하므로 `common → 기능` 의존은 사이클로 잡혀 R3 도 사실상 같다. 문서의 주장은 맞다 (출처 표기는 [m7]) |
| M2 A 를 약하게 정의 | **해소, 새 문제 발생** | A′ 정의(record 반환, `SimilarityScorer`, repository·domain 차단)는 공정하다. 실행 1·2 로 A′ 가 컴파일되고 R5 외 모든 컨벤션 규칙을 통과함을 확인했다. 그러나 비교 근거에 새 불공정이 생겼다 → [C1] |
| M3 R5 단계 누락·OSIV 순서 | **해소** | `FIX-R5`(depends TEST-1) → `FIX-F4`(depends FIX-R5, SIG-1). ARCH-B3 acceptance 는 R2 만 남았다. conventions §3.1 "R5 는 어느 선택지에서도 완화하지 않는다" 와 일치한다 |
| M4 파일 수 근거 | **해소** | 계수 기준을 명시하고 목록을 열거했다. A′ 8 은 실행 1 로 확인했다(9 − F1). B 12 는 정적 재계수로 일치한다: 신규 4 + `SimilarityClient` 이동 1 + dto 삭제 2 + `SimilarityException` 이동 1 + `RestClientConfig` 삭제 1 + 수정 3. A′ 와의 차이 4 = (+`SentenceReader`, +client 이동, +dto 2, +exception 이동) − `SentenceService` 수정 |
| M5 R5 구현 범위 | **해소** | `ConventionArchitectureTest.java:99` 가 `getReturnType().getAllInvolvedRawTypes()` 를 쓴다. §5 설명과 일치한다 |
| M6 C 트리 | **해소** | §2 C 트리와 순환 설명이 examples README §3·§5 와 일치한다. §3.1 에 "포트 소유·어댑터 위치" 행이 추가됐다 |
| m1 R4b 누락 | 해소 | §5 표에 있음 |
| m2 ArchUnit 개수 행 | 해소 | 해당 행 삭제 |
| m3 F5·F6·F7 | 해소 | §0 문구 삭제, §1 에서 F5 분리 |
| m4 F4 구조 여부 | 해소 | §1 `:35` 에서 "어느 선택지에서도 같은 수정" 으로 정리 |
| m5 R7 설명 | 해소 | "**모든** 파라미터 + 예외 타입" |
| m6 R6 한계 | 해소 | 클래스 레벨 미검사 명시 |
| m7 `similarity/exception` | 해소 | B 트리 `:79` |
| m8 배치 규칙 문장 | 형식상 해소 | 문장은 생겼지만 조건이 판정 불가하다 → [M3] |
| m9 WIP 근거 절반 | 해소 | §4.1-2 에 명시 |
| m10 소요 산정 기준 | **미해소** | 기준은 생겼지만 수치를 재현하지 못한다 → [M4] |

## 추천 B 의 정당성 (FOCUS 요청 판단)

[C1] 을 고친 뒤 남는 비교는 다음과 같다.

- B 의 순이득: (i) 공개 표면이 "base package 에 둔 타입" 으로 좁고, 표면이 넓어지면 반드시 파일 이동 diff 가 생긴다. A′ 는 `service`·`dto` 패키지 전체가 공개되어 웹 응답 DTO(`SentenceResponse`)와 기능 간 계약이 같은 패키지에 섞인다. (ii) Modulith 기본 규칙과 같다 (`common` OPEN 1파일 vs A′ 의 기능별 `@NamedInterface`).
- B 의 비용: 4파일(+테스트 이동 1~2), 02 §3.1·§3.2·§5.1, 05 §1.3, 06 §6.5 수정, `infra/` 라는 팀 규칙 폐기.
- 판단: Toss·이력·사용자 등 기능이 늘어날 예정이고(01 §4.2, toss 설계), 기능 간 계약용 타입을 웹 DTO 와 분리하는 것이 R5·conventions §3.1 의 취지와 맞는다. 그래서 **B 추천은 여전히 방어 가능하다**. 다만 차이는 "규칙 개수" 가 아니라 "공개 표면의 입도와 가시성" 이다. 결정자가 "기존 `infra/` 규칙 유지" 를 더 중시하면 A′ 가 합리적이라는 D1 의 단서(`decisions-needed.md:27,53`)가 맞다. 문서는 이 결론을 지지하는 근거만 남기고, 성립하지 않는 근거([C1] a·b·c)는 빼야 한다.

## 검증한 주장 (문제 없음 확인)

- A′ 예시를 실제로 적용하면 컴파일된다. ArchUnit 결과는 R1·R3·R4·R4b·R6·R7 통과, R5 1건(FIX-R5 미적용), R2 11건(B 전용 규칙)이다(실행 2). examples README §0 의 A′ 행과 같다.
- §3.2 A′ 8파일·B 12파일과 그 목록이 examples README §3 의 A′·B 표와 일치한다(F1 `SimilarityClient` 제외 기준).
- §4.3 에 있는 항목의 `depends_on` 이 모두 feature_list 와 일치한다: TEST-1 —, FIX-F1/F2 ← TEST-1, SIG-1 —, SIG-2 ← SIG-1, FIX-F3 ← FIX-F2·SIG-2, FIX-R5 ← TEST-1, FIX-F4 ← FIX-R5·SIG-1, ARCH-A1 ← FIX-F1, ARCH-B2 ← ARCH-A1, ARCH-B3 ← ARCH-B2, ARCH-A2 ← ARCH-A1. 완료 신호(R7·R5·R6·R1·R3·R2)도 각 항목의 verify/acceptance 와 맞다.
- `pending_decision` 의 키 `"D1=B"` / `"D1=A'"` 와 "결정 후 하나만 items 로" 는 feature_list `_comment`, ai-workflow §4(`:54`, `:203`)와 일치한다.
- §0 표의 02 §2.2·02 §3.2(`02_설계.md:108`)·05 §1.3(`05_개발컨벤션.md:36`)·99 §2.3(`99_공통.md:78`) 인용은 원문과 맞다.
- §4.1-2 의 WIP 근거: `wip-snapshot.patch:34` 가 `common/config/RestClientConfig` 에 `toss.holdings.infra.TossHoldingsClient` import 를 추가한다. "근거가 절반" 이라는 정정은 정확하다.
- §4.2 의 `SentenceException.NotFound` 처리(`GlobalExceptionHandler` 의 `BaseException` 핸들러)와 `SentenceDetail` 3필드는 B 예시(`SentenceReader.java`, `SentenceDetail.java:7`)와 일치한다.
- §2 C 의 순환 설명과 "순환 없는 배치는 B 의 공개 API 선행" 은 examples README §5 와 일치한다.
- Modulith 2.0.8: OPEN 모듈은 의존 검사를 면제받는다. 사이클 검사는 모든 모듈 대상이다. base package 의 unnamed interface 는 하위 패키지를 제외한다. §4.1-5 의 정정된 주장과 맞다.
