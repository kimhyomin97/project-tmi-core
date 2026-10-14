# architecture-proposal 리뷰 — Round 3

> 대상: `docs/ai/proposals/architecture-proposal.md` @ `83f7b92`. 전용 worktree(`git worktree add --detach … 83f7b92`)에서 읽었고, 라인 번호는 이 커밋 기준이다.
> 검증 방법:
> 1. 정적 대조에 쓴 파일: 대상 문서, `reviews/architecture-proposal-round-{1,2}.md`, `feature_list.example.json`(items 11개, pending_decision 5개), `decisions-needed.md` D1, `examples/answer-scoring/README.md`, `option-a-layered/`·`option-b-modular/`·`option-c-hexagonal/` 전 파일, `src/main/**` 34개 java 파일, `src/test/java/com/tmi/core/architecture/*`, `docs/public/{02,05,06,99}`, `docs/public/toss/00_인증.md`, `wip-snapshot.patch`, Modulith 2.0.8 `spring-modulith-core` 소스(scratchpad 사본, `pom.properties` version=2.0.8).
> 2. 실행 1 (A′ 규칙 (a)(b) 프로브, A′ 적용 상태): `option-a-layered/` 7개 파일을 실제 경로에 복사하고 `RestClientConfig` 를 삭제했다. `similarity.timeout-ms` 는 `connect-timeout`/`read-timeout` 로 바꿨다. 임시 `APrimeProbeTest` 에 두 조건을 넣었다. ruleA 는 `moduleOf` 기준으로 다른 기능 모듈(`common`·`infra` 제외)의 `{모듈}.(repository|domain|exception)` 에 의존하면 위반으로 본다. **exception 을 포함한** 형태다. ruleB 는 `infra` 밖에서 `infra.*` 의 `*Scorer` 가 아닌 타입에 의존하면 위반으로 본다. 결과는 **둘 다 통과**다.
> 3. 실행 2 (같은 프로브, AS-IS 상태): ruleA 는 **10건**이고 모두 `answer → sentence.*` 이다. ruleB 는 11건이다. `AnswerService → infra.similarity.*` 가 9건, `common.config.RestClientConfig → SimilarityClient` 가 2건이다.
> 4. 실행 3 (A′ 의 Toss 배치에 ruleB 적용): A′ 적용 상태에 §2 A′ 트리대로 fixture 2개를 추가했다. `infra/toss/TossHoldingsClient`(인터페이스)와, 이를 주입하는 `toss/holdings/service/HoldingsSnapshotService` 다. 결과는 ruleB **위반 3건**이다(필드·생성자·메서드 호출).
> 5. 실행 4 (C 예시): AS-IS 에 `option-c-hexagonal/` 의 포트 3개·유스케이스 서비스·`AnswerScore`·`AnswerException`·`SentencePersistenceAdapter` 를 적용했다. 리포지토리 이름은 AS-IS 로 두었다. 대체되는 AS-IS `AnswerService`·`AnswerController`·`AnswerResponse` 는 삭제했고, `AnswerApi` 도 `AnswerResponse` 를 참조해서 함께 삭제했다. 그 뒤 `@Disabled` 를 끄고 `ModuleArchitectureTest` 를 돌렸다. **컴파일에 성공했다.** R1 은 `common ↔ infra` 1건뿐이고 `answer ↔ sentence` 순환은 없다. R2 는 3건이고 모두 `sentence.adapter.out.persistence.SentencePersistenceAdapter → answer.application.port.out.*` 이다. R3 은 2건(AS-IS)이다.
> 6. 명령은 모두 `./gradlew test --offline --no-daemon --tests …` 로 실행했다. DB 는 쓰지 않았다. 임시 파일은 worktree 안에만 만들고 실행 후 `git checkout`/`git clean` 으로 되돌렸다. worktree 는 `git worktree remove --force` 로 정리했다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | A′ 규칙 (a)(exception 포함), C 의 순환 없음과 R2 3건, R2 19 = 10 + 9, Modulith 클래스명은 실행과 소스로 모두 확인된다. 남은 문제는 A′ 규칙 (b) 하나다. 실행 없이 적었고, 실제로 돌리면 A′ 자신의 Toss 배치와 충돌한다 |
| 구체성 | 3 | B 경로(단계표, 파일 목록, 배치 규칙, Toss auth)는 바로 쓸 수 있다. A′ 경로는 그렇지 않다. 규칙 (b)를 적힌 그대로 구현하면 §2 A′ 트리의 `toss/holdings/service → infra/toss/TossHoldingsClient` 가 막힌다. 규칙 (a)는 `toss.holdings.domain` 같은 2단 패키지에 대한 판정이 정해져 있지 않다 |
| 일관성 | 3 | §4.3 과 feature_list 는 1:1 이고, §0 표는 원문과 맞는다. 그러나 문서 스스로 A′ 규칙을 (a)(b) 두 개로 정의해 놓고 §3.1·§4.1-1·D1 에서는 "경계 규칙 1개, B 와 같다" 라고 쓴다. A′ 의 `exception` 포함 여부는 §0·ARCH-A2 title 과 §2·ARCH-A2 acceptance 가 다르게 적는다. decisions-needed D1 의 C 단점은 고치기 전 예시를 기준으로 남아 있다 |
| 판단 가능성 | 4 | B 근거가 "공개 표면 크기·diff 가시성" 으로 좁혀졌고, 비용·질문표·단계 계획으로 결정할 수 있다. 다만 A′ 의 비용 중 규칙 (b)의 외부 시스템별 진입점 요구는 결정자에게 보이지 않는다(→ M1). 이 누락은 A′ 쪽 비용을 낮게 보이게 하는 방향이라 추천 B 를 뒤집지는 않는다 |

## 치명 (승인 차단)

없음.

## 주요

- **[M1] §2 A′ 규칙 (b)(`:63`) — 적힌 대로 구현하면 A′ 자신의 Toss 배치(`:58-59`)를 막는다. 또 이 규칙이 생기면서 "A′·B 모두 경계 규칙 1개"(`:156`, `:193`, decisions-needed `:50`, examples README `:112`)가 사실이 아니게 됐다.**
  - 근거:
    - (b)의 내용은 "`infra/{시스템}` 패키지의 타입 중 `*Scorer`(변환 클래스) 외에는 `infra` 밖에서 접근 금지" 다. 그런데 §2 A′ 트리는 `infra/toss/{TossHoldingsClient, TossClientConfig, dto/}` 와 `toss/holdings/{service,…}` 로 나뉘어 있다. 따라서 `toss.holdings.service` 는 `infra.toss.TossHoldingsClient` 를 써야 한다. 실행 3 에서 이 형태의 fixture 를 넣자 ruleB 가 **3건 위반**을 냈다. Toss 쪽에는 `*Scorer` 에 해당하는 변환 클래스가 정의되어 있지 않다. 00_인증 §4 의 `TossAuthClient`·`TossTokenProvider` 는 A′ 에서 위치조차 정해져 있지 않다. P2(외부 클라이언트 배치)는 Toss 를 위해 지금 정해야 하는 문제인데(§1 `:33`), A′ 는 바로 그 Toss 에 대해 규칙이 성립하지 않는다.
    - 문서는 A′ 의 강제 규칙을 (a)와 (b) **두 개**로 정의한다(`:63`). B 는 R2 하나로 우회까지 막는다(`:63` 끝 문장, README §6-2). 그런데 §3.1 P5 행은 "경계 규칙 1개 (A′ 규칙, §2)" 이고, §4.1-1 은 "규칙 개수는 A′·B 모두 1개로 같다 (round-2 리뷰 C1)" 이다. D1 의 A′ 장점도 "경계 규칙 1개" 다. round-2 C1 이 "1개" 라고 한 것은 (b)가 추가되기 **전** 이야기다(examples-round-2 M3 이후 추가). 지금 문서 기준으로 이 서술은 B 에게 불리한 쪽으로 틀렸다.
    - (a)의 판정 대상은 "다른 기능 모듈의 `repository`·`domain`·`exception` **하위 패키지**" 다. 그런데 A′ 트리의 `toss/holdings/{repository,domain}` 은 `moduleOf` 가 `toss` 이고 하위 패키지가 `holdings.repository` 다. `{모듈}.repository` 로 구현하면(round-2 프로브와 실행 1 의 형태) 이 패키지는 보호받지 못한다. `..repository..` 로 구현하면 보호된다. 어느 쪽인지 정해져 있지 않다.
  - 요구 수정:
    - (b)를 외부 시스템 일반형으로 다시 쓴다. 예: "`infra/{시스템}` 마다 공개 진입점(Similarity=`SimilarityScorer`, Toss=도메인 클라이언트 `TossHoldingsClient`·`TossTokenProvider`?)을 지정하고, 그 외 타입(`*Config`, `dto`, `exception` 의 생성 등)은 `infra` 밖에서 접근 금지". 또는 Toss 에도 변환 클래스 1개를 두는 비용을 A′ 비용에 넣는다.
    - A′ 트리에 `TossAuthClient`·`TossTokenProvider` 의 위치를 적는다.
    - §3.1 P5·§4.1-1·D1·README §4 의 "규칙 1개" 를 "A′ 2개((a) 기능 간 + (b) infra 진입점), B 1개(R2)" 로 고친다. 이렇게 고치면 B 의 순이득이 하나 늘어난다.
    - (a)의 하위 패키지 판정을 `..repository..` 처럼 깊이와 무관하게 할지 명시한다.

## 경미

- [m1] **decisions-needed.md D1 C 행(`decisions-needed.md:52`)**: "예시 배치는 `answer ↔ sentence` 순환, 순환 없는 배치는 B 의 경계가 선행" 이라고 적혀 있다. 고친 예시는 순환이 없고 B 에 의존하지도 않는다. 대상 문서 §2 C(`:139-140`), examples README §0·§5 와 모순되며, 실행 4 결과(R1 에 `answer ↔ sentence` 없음, R2 3건은 어댑터 → 포트)와도 맞지 않는다. C 의 다른 단점(02 §2.2 역전, 2~3일)만으로도 결론은 같아서 경미로 둔다. "포트 반환 타입을 answer 소유 record 로 해야 순환이 없다. C 전용 경계 규칙 필요" 로 고친다.
- [m2] **A′ 의 `exception` 포함 여부가 위치마다 다르다** (round-2 m2 일부 잔존). §2 A′ (1)(`:49`)과 ARCH-A2 acceptance 는 `repository·domain·exception` 이다. §0 표 05 §1.3 행(`:24`)과 ARCH-A2 title(`feature_list.example.json:298`)은 `repository·domain` 이다. 실행 1·2 로 exception 포함형이 A′ 통과 / AS-IS 10건임은 확인했다. 표와 title 을 exception 포함으로 맞춘다.
- [m3] **§2 C "배치는 examples 와 같다"(`:121`)는 과장이다.** C 트리에는 `sentence/domain/Sentence (순수 모델)` 이 있고, §3.2 신규 목록에는 "도메인 모델 1·매퍼 1" 이 있다. 그런데 예시 `SentencePersistenceAdapter` 는 JPA 엔티티를 `SentenceForScoring` 으로 직접 매핑하고, Sentence 모델과 매퍼는 없다. 또 트리에는 C 에서 순환을 피하는 데 쓰인 `answer/exception/AnswerException.SentenceNotFound` 가 빠져 있다. "예시는 이 흐름에 필요한 것만, Sentence 모델·매퍼는 sentence 자체 유스케이스용" 이라고 구분하고, 트리에 `exception/` 을 추가한다.
- [m4] **§3.2 C "기존 수정·이동 최대 22 (`common` 11 + `CoreApplication` 제외한 전부)"(`:172`)**: `src/main` 의 java 파일은 34개이고 `common` 은 **10개**다(config 3, domain 1, exception 6). 산식대로면 23이다. 결론에는 영향이 없다.
- [m5] **§3.2 B 소요 "0.5~1일"(`:171`)의 하한**: 산정식으로 3~4h + 리뷰 2회(2h)면 이미 5~6h 이고, 여기에 가산 요인이 붙는다. 그래서 하한 0.5일(4h)은 나올 수 없다. "약 1일" 또는 "0.7~1일" 로 적는 것이 산정식과 맞다. §4.1-3 의 "시간 차 0~0.5일" 도 그에 맞춰 조정한다.
- [m6] **§0 표에서 빠진 문서 위치 3곳**:
  - B 는 `02 §3.1 트리`(`02_설계.md:91` `infra/`)를 바꾼다. §3.2 `:176` 과 DOC-2(B) title 에는 있지만 §0 표에는 없다. 그런데 DOC-2 acceptance 가 "**§0 표**의 해당 열 모든 행" 이라서 이 위치가 사람 확인에서 빠질 수 있다.
  - A′·B 모두 `99 §1 트리`(`99_공통.md:13` `RestClientConfig`)를 바꾼다.
  - 02 §4.2 는 3단계뿐 아니라 4단계(`02_설계.md:148` `SimilarityClient.score` → `SimilarityScorer`)도 바뀐다.
- [m7] **§4.3 2-A′ 행 "→ 02·99 문서"(`:221`)**: DOC-2(A′) title 과 §3.2 `:176` 에는 05 §1.3(기능 간 규칙 신설)도 있다. 행에 05 를 추가한다.
- [m8] **§4.3 하단 "`git mv` + 2줄 수정 — 패키지·가시성"(`:224`)**: A′/B 의 `SimilarityClientConfig` 를 diff 하면 package 1줄, `similarity.client.SimilarityClient` import 추가 1줄, 클래스·record 가시성 2줄로 모두 4줄이다. "몇 줄" 로 적으면 충분하다. 결론(버려지는 작업 없음)은 유지된다.
- [m9] **§2 A′ (a) "round-2 리뷰가 이 형태로 실행해 … 확인"(`:63`)**: round-2 프로브는 exception 을 **제외한** 형태였다(round-2 검증 방법 4). exception 포함형은 이번 실행 1·2 로 확인했다(A′ 통과, AS-IS 10). 출처 문구를 그에 맞게 고친다.
- [m10] **examples README 내부 수치 불일치(대상 외, 참고)**: §3 은 C 를 "신규 7"(`:98`)로, §5 는 "신규 8"(`:124`)로 적는다. §0 C 행은 삭제 대상을 `AnswerService`·컨트롤러·`AnswerResponse` 로 적지만, 실제로는 `AnswerApi` 도 함께 지워야 컴파일된다(실행 4). 대상 문서의 "신규 7 + 수정 1" 은 §3 과 일치한다.
- [m11] **머리말 개정 줄(`:7`)**: round-2 의 C1·M1~M4 만 적혀 있다. 본문에 반영된 m1(P1 10건), m4, m5, m6, m7, m8 도 함께 적는다(round-2 m9 와 같은 유형).

## 이전 라운드 지적 해소 여부 (Round 2)

| 지적 | 판정 | 확인 내용 |
|---|---|---|
| C1 B·A′ 비교 근거 | **해소, 새 문제 발생** | (a) A′ 규칙이 `moduleOf` 기반 조건 1개로 바뀌었다(`:63`). 실행 1·2 로 A′ 통과, AS-IS 10건을 확인했다. "쌍마다" 문구는 대상 문서·README·D1 어디에도 없다(grep 0). (b) "생길 수 없다" 가 "base package 로 옮기는 diff 가 보여야" 로 바뀌었다(`:193`). (c) Modulith 행이 "`@NamedInterface` 기능당 1~2파일" 로 바뀌었다(`:162`). §0 은 (i)(ii)와 비용을 한 문장으로 적는다(`:13`). 그런데 그 사이 A′ 에 규칙 (b)가 추가돼 "규칙 1개로 같다" 가 다시 틀린 문장이 됐다 → [M1] |
| M1 F1T·DOC-1 누락, 순서 | **해소** | §4.3 에 `FIX-F1T`(← FIX-F1)·`DOC-1`(← FIX-F3, FIX-F4)이 있다. feature_list 에서 `ARCH-B2`·`ARCH-A2` 의 depends_on 은 `[ARCH-A1, FIX-F1T]` 이고 표와 같다. 02 문서 수정은 `DOC-2`(pending_decision 양쪽)가 맡는다 |
| M2 02 §5.1·06 §6.5·02 §4.2 누락 | **해소** (잔여 경미) | §0 표 `:22-23` 에 추가됐고 원문 `02_설계.md:165`, `06_코드가이드라인.md:418`, `02_설계.md:147` 과 맞다. 02 §3.1 트리·99 §1 트리·02 §4.2 4단계 → [m6] |
| M3 B 배치 규칙 판정 불가 | **해소** | "현재 호출 모듈 1개 → internal, 2개 이상 → 독립 모듈 승격" 이라는 관찰 가능한 조건이 됐다. Similarity 예외의 사유(02 §1 별도 서비스, 이중 이동 회피)가 적혀 있고, Toss 는 `toss/auth/`·`toss/holdings/client/` 로 00_인증 §4(`:37-39`)를 따른다 |
| M4 산정식 재현 불가 | **대부분 해소** | A′: 8×15~20분 = 2~2.7h, 리뷰 1회 포함 0.5일로 재현된다. C: 42×15~20분 = 10.5~14h, 테스트 재작성 포함 2~3일로 재현된다. B 는 하한만 맞지 않는다 → [m5] |
| m1 P1 B 기준 수치 | 해소 | `:32` "answer→sentence.* 10건". 실행 2 의 ruleA 10건과 같다 |
| m2 ARCH-A2 exception | **부분 해소** | §2 (1)과 acceptance 는 맞췄다. §0 표·ARCH-A2 title 은 아직 다르다 → [m2] |
| m3 §4.1-4 공통 근거 | 해소 | "참고 (A′·B 공통 — B 만의 근거 아님)" 로 분리됐다(`:196`) |
| m4 단계 3 depends_on | 해소 | "ARCH-B3 또는 ARCH-A2" (`:222`) |
| m5 테스트 파일 제외 명시 | 해소 | `:166` |
| m6 R2 해제 항목 | 해소 | `:236` "→ `ARCH-B2`·`B3`" 로 바뀌어 `ModuleArchitectureTest` 의 `@Disabled` 사유와 같다 |
| m7 Modulith 출처 | 해소 | `:197` 에 클래스·메서드명이 있다. 소스에서 `ApplicationModuleDetectionStrategy.directSubPackage`(:63), `NamedInterfaces.discoverNamedInterfaces`(:83), `ApplicationModule.isValidDependencyWithin`(:1357, OPEN 분기 :1408), `ApplicationModuleInformationFactory`(:173 `Type.OPEN`)가 실제로 있음을 확인했다 |
| m8 `git mv` 만 | 형식상 해소 | "+ 2줄 수정" 이 추가됐지만 실제로는 4줄이다 → [m8] |
| m9 머리말 | 부분 해소 | → [m11] |

## 검증한 주장 (문제 없음 확인)

- **B 추천 근거(§0 `:13`, §4.1-1 `:193`)**:
  - A′ 에서 `answer` 가 `sentence.dto.SentenceResponse` 를 써도 규칙 (a)는 위반이 아니다. 실행 1 의 ruleA 가 `dto` 를 검사하지 않는다.
  - B 에서는 R2 가 base package 밖 타입을 막는다(`ModuleArchitectureTest.java` `isModuleApi`). 그래서 표면을 넓히려면 파일을 base package 로 옮겨야 한다.
  - B 에서 "공개 API 가 웹 DTO 를 반환" 하는 경우에도, 호출 측이 그 값을 쓰는 순간 `sentence.dto` 의존이 생겨 R2 가 기계적으로 잡는다. 이 근거는 문서가 적은 것보다 오히려 강하다.
  - 근거를 "공개 표면의 크기·diff 가시성" 으로 좁힌 서술은 사실과 맞다.
- **A′ 규칙 (a)**: exception 포함형으로 A′ 적용 코드는 통과하고 AS-IS 는 10건(`AnswerService`·`AnswerResponse` → `sentence.repository|domain|exception`)을 낸다(실행 1·2). 기능 쌍을 열거하지 않는 조건 1개로 표현된다.
- **A′ 규칙 (b)의 Similarity 부분**: A′ 적용 코드는 통과한다. AS-IS 에서는 `AnswerService → SimilarityClient`·`dto`·`exception` 우회를 탐지한다(실행 1·2). 문제는 Toss 쪽뿐이다 → [M1].
- **C 서술(§2 C `:139-140`, §3.1 `:158`)**:
  - 포트가 answer 소유 `SentenceForScoring` 을 반환하면 의존 방향은 sentence → answer 하나뿐이다.
  - 컴파일에 성공하고, R1 의 `answer ↔ sentence` 순환은 없다.
  - R2 3건은 모두 `SentencePersistenceAdapter → LoadSentencePort`(implements, 제네릭 반환 타입, record 생성자)다(실행 4).
  - examples README §0·§3·§5, 그리고 `option-c-hexagonal` 9개 파일(패키지 선언·import)과 일치한다. 예외는 [m3] 이다.
- **§3.2 A′ 8·B 12 목록**: README §3 A′·B 표와 항목이 같다. B 의 12개는 신규 4, 이동 2, dto 삭제 2, `RestClientConfig` 삭제 1, 수정 3 이다. §4.2 의 "4파일(6타입)" 은 `infra/similarity` 의 `SimilarityClient`, `SimilarityRequest`, `SimilarityResponse`, `SimilarityException`(+`RequestInvalid`, `Unavailable`)이다.
- **§4.3 ↔ feature_list 1:1**:
  - `items` 11개(TEST-1, FIX-F1, FIX-F1T, FIX-F2, SIG-1, SIG-2, FIX-F3, FIX-R5, FIX-F4, ARCH-A1, DOC-1)가 모두 표에 있다.
  - `pending_decision` 의 `D1=B`(ARCH-B2, ARCH-B3, DOC-2)와 `D1=A'`(ARCH-A2, DOC-2)도 모두 표에 있다.
  - 각 id 의 depends_on 이 표와 같다: FIX-F1T←FIX-F1, FIX-F3←FIX-F2·SIG-2, FIX-F4←FIX-R5·SIG-1, DOC-1←FIX-F3·FIX-F4, ARCH-B2/A2←ARCH-A1·FIX-F1T, ARCH-B3←ARCH-B2, DOC-2←ARCH-B3 / ARCH-A2.
  - 완료 신호도 각 항목의 verify·acceptance 와 맞는다: R7, `SimilarityTimeoutTest`, DB 차단 test, spotlessCheck, R5→R6, `OsivConnectionIT`, R1·R3, infra 소멸, R2, human.
- **§0 표 ↔ docs/public 원문**:
  - 02 §2.2(`02_설계.md:56`), 02 §3.2(`:108`), 05 §1.3(`05_개발컨벤션.md:36`), 99 §2.3(`99_공통.md:67,78`), 02 §5.1(`02_설계.md:165`), 06 §6.5(`06_코드가이드라인.md:418`), 02 §4.2 3단계(`02_설계.md:147`)의 인용이 원문과 맞다.
  - "05 §1.3 기능 간 의존 규칙 명시 없음" 도 맞다. 05 §1.3 에는 모듈 내부 의존 방향(`:43`)만 있다.
- **§2 B Toss 배치**: `toss/auth/{TossAuthClient, TossTokenProvider}` 와 `toss/holdings/client/` 의 분리는 `00_인증.md:37-39` 와 맞다. WIP(`wip-snapshot.patch`)의 `toss.holdings.infra` 와 `common/config` 등록에 대한 §4.1-2 서술도 맞다.
- **§5 R2 "19 = 10 + 9"**: 실행 2 의 ruleA 10건(sentence)과 ruleB 의 `AnswerService → infra.similarity` 9건 합과 같다.
- **§4.2 `GlobalExceptionHandler.java:25`**: `@ExceptionHandler(BaseException.class)` 행이 맞다.
