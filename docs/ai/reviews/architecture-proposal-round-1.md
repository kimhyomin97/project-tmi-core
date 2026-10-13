# architecture-proposal 리뷰 — Round 1

> 대상: `docs/ai/architecture-proposal.md` (마지막 변경 커밋 `1f3e4c0`, 리뷰 시점 HEAD `b39d3e2` — `99aa1e3..b39d3e2` 는 CLAUDE.md 추가뿐이라 대상 문서와 테스트는 그대로)
> 검증 방법:
> 1. 정적 대조: `src/main/**` 34개 Java 파일, `src/test/java/com/tmi/core/architecture/*.java`, `docs/ai/{current-state,research,decisions-needed,conventions-proposal,ai-workflow}.md`, `feature_list.example.json`, `examples/answer-scoring/**`, `docs/public/{02,05,99}`.
> 2. 실행 1: 전용 worktree(`99aa1e3`)에 임시 `junit-platform.properties`(`junit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`)를 두고 `bash ./gradlew test --tests 'com.tmi.core.architecture.*'` 를 돌렸다. 결과: R1 1, R2 19, R3 2, R5 1, R6 1, R7 1 위반. R4·R4b 는 통과.
> 3. 실행 2 (ARCH-A1 모사): 같은 worktree 에서 `common/config/RestClientConfig` 를 `infra/similarity/config/SimilarityClientConfig` 로 옮기고 다시 실행했다. 결과: **R1·R3 통과**, R2 는 계속 실패.
> 4. Spring Modulith 2.0.8 `spring-modulith-core` 소스 jar(Maven Central)를 받아 internal 판정 로직을 직접 읽었다.
> 5. worktree 는 `git worktree remove --force` 로 정리했다. DB 는 쓰지 않았다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 3 | 라인 참조와 위반 수는 정확하다. 그러나 "팀 결정과 정합", "Modulith 그대로 verify() 가능" 두 핵심 주장은 근거와 반대이고, §3.2 의 "실측 목록" 은 합계가 열거되지 않았다 |
| 구체성 | 3 | 트리·예시·규칙은 바로 쓸 수 있는 수준이다. 그런데 §4.3 의 완료 신호(R 번호)와 의존이 틀려서 그대로 실행하면 게이트를 잘못 판정한다 |
| 일관성 | 2 | §4.3 이 테스트 주석·feature_list·D1 과 어긋난다. §2 C 트리가 examples README 와 다르다. 02 §3.2·05 §1.3·99 §2.3 과의 충돌을 문서가 언급하지 않는다 |
| 판단 가능성 | 3 | 구성(문제→선택지→비교→약점→비용)은 좋다. 다만 A 가 R5 와 충돌하게 정의돼 있어 실제보다 약하게 그려졌고, 정합성 표가 결정자를 B 쪽으로 기울게 만든다 |

## 치명 (승인 차단)

- **[C1] §4.3 단계적 실행 계획 — 완료 신호와 의존이 테스트·feature_list·D1 과 맞지 않는다.**
  - 근거:
    - 단계 1 의 완료 신호는 "R3·R4 통과" 다(`architecture-proposal.md:195`). 그런데 R4 는 지금도 통과하므로 신호 역할을 못 한다(실행 1).
    - R1 은 단계 2 에 배정됐다(`:196`). 실행 2 에서 Similarity 빈 설정만 `common` 밖으로 옮겨도 R1·R3 이 통과했다. 테스트 주석 `ModuleArchitectureTest.java` R1 `@Disabled("… ARCH-A1 완료 시 해제")`, `feature_list.example.json` ARCH-A1 acceptance "R1(순환)·R3", decisions-needed D1 "ARCH-A1 … R1·R3 해제", current-state §6.1 이 모두 R1 을 단계 1 로 본다. 이 문서만 다르다.
    - 단계 2 의 신호는 "R1·R2" 다. feature_list ARCH-B3 는 "R2·R5" 다.
    - 단계 0 은 "F1·F2·F3, 의존 없음" 이다. feature_list 에서는 FIX-F1·F2 가 `TEST-1`(→`SIG-1`)에 의존하고, FIX-F3 은 FIX-F2 에 의존한다. FIX-F4·SIG-1·SIG-2·TEST-1 은 §4.3 에 아예 없다.
    - 단계 1 은 "단계 0 전체에 의존" 한다. feature_list ARCH-A1 은 FIX-F1 에만 의존한다.
    - 단계 1 의 "`AnswerService → SentenceService` 경유" 에 대응하는 feature_list 항목이 없다. ARCH-A1 제목은 빈 설정 이동뿐이다. 게다가 이 작업은 단계 2 의 `SentenceReader` 로 대체돼 버려지므로 "중간에 멈춰도 손해가 없다"(`:190`)와도 충돌한다. D1 은 "A 단계(ARCH-A1)까지는 낭비 없음" 이라고 범위를 좁혀 놓았다.
    - 단계 3(Toss)에 대응하는 feature_list 항목이 없다(D9 에만 있다).
  - 요구 수정: §4.3 을 feature_list 의 id·`depends_on`·acceptance 와 1:1 로 다시 쓴다. 단계 1 은 ARCH-A1 = R1·R3 이고 R4 신호는 삭제한다. 단계 2 는 ARCH-B2·B3 = R2 다. R5 는 [M3] 참조. 단계 0 에는 SIG-1→TEST-1→FIX-F1/F2→F3, FIX-F4 를 넣는다. "AnswerService → SentenceService" 는 A 를 확정할 때만 하는 작업으로 분리한다. Toss 단계에 항목 id 를 부여하거나 "D9 결정 후 등록" 이라고 명시한다.

- **[C2] §0·§3.1 "팀 기존 결정과의 정합" — B(그리고 A)가 뒤집는 기존 문서 규칙을 숨긴다.**
  - 근거:
    - §0 은 "기존 팀 결정(02 §2.2)을 뒤집지 않으면서" 라고 쓰고, §3.1 은 A "완전 일치", B "일치" 로 표기한다.
    - 그러나 `docs/public/02_설계.md` §3.2 는 "외부 시스템 클라이언트: **도메인 패키지 안이 아닌 `infra/{시스템명}/` 아래**. 도메인은 외부 통신 세부에 묶이지 않음" 이라고 정한다. `05_개발컨벤션.md:36` 도 `infra/{외부시스템}/` 을 규칙으로 둔다. B 는 `infra` 를 해체하고 Toss 클라이언트를 기능 모듈 안(`toss/holdings/client`)에 둔다. 02 §3.2·05 §1.3 의 명시 규칙을 뒤집는 것이다.
    - `99_공통.md` §2.3 은 "`infra/{시스템명}/` 에 인터페이스로 선언, **본 Config(common/config/RestClientConfig)에서 빈 등록**" 을 명시 설계로 둔다. P3("common 역방향 의존")은 이 설계의 직접 결과다. A·B·C 모두 이 결정을 뒤집는데, A 를 "완전 일치" 로 표기한 것은 틀렸다.
    - §3.2 의 문서 수정 행도 A 를 "05 §1.3 소폭" 으로만 적어 99 §2.3 을 빠뜨렸다.
  - 요구 수정: §3.1 의 "팀 기존 결정과의 정합" 행을 결정별로 나눈다. 02 §2.2 (헥사고날→레이어드): A·B 유지, C 충돌. 02 §3.2·05 §1.3 (`infra/` 배치): A 유지, B 변경, C 변경. 99 §2.3 (common 에서 빈 등록): A·B·C 모두 변경. §0 문구를 "02 §2.2 는 유지하되 02 §3.2·05 §1.3·99 §2.3 의 배치 규칙은 바꾼다" 로 정정하고, 이 변경도 D1 결정 범위에 포함됨을 명시한다.

## 주요

- **[M1] §2 B 설명(`:52`)·§3.1 Modulith 행(`:137`)·§4.1-5(`:176`) — "그대로 `verify()` 가능 / 의존성만 추가하면 된다" 는 Modulith 소스와 맞지 않는다.**
  - 근거 (Modulith 2.0.8 소스 직접 확인, [J]):
    - `NamedInterfaces.discoverNamedInterfaces` 는 base package 를 `NamedInterface.unnamed(basePackage)`("excluding any sub-packages by convention")로 만든다.
    - `ApplicationModule.isValidDependencyWithin` 는 `targetModule.isOpen()` 이 아니고 `!targetModule.isExposed(target)` 이면 `"Module '%s' depends on non-exposed type %s within module '%s'!"` 를 보고한다.
    - B 는 `common` 을 "공유 커널" 로 정하고 R2 에서 예외 처리한다(`ModuleArchitectureTest` `SHARED_KERNEL`). Modulith 에는 그런 예외가 없다. 따라서 `common.exception.BaseException`·`ETmiErrorType`·`common.domain.BaseEntity` 를 쓰는 모든 모듈이 위반이 된다. 현재 다른 모듈에서 `common.{exception,domain,config}` 를 import 하는 곳이 8곳이다.
    - 통과하려면 `common` 에 `@ApplicationModule(type = Type.OPEN)`(`ApplicationModuleInformationFactory.java:173`) 또는 named interface 선언이 필요하다.
    - 같은 문서는 "Modulith 기본 규칙과 같은 모양" 의 근거로 research §1.2 를 들지만, research 에서 "하위 패키지 = internal" 은 [S] 등급이다. 이 문서는 등급을 옮겨 적지 않았다.
  - 요구 수정: "`common` 을 OPEN 모듈로 선언(`package-info.java` 1파일)하면 같은 규칙이 된다 — 그 외 차이는 미검증" 으로 정정한다. 근거 등급은 이번 확인으로 [J] 로 올려 표기한다.

- **[M2] §2 A·§3.1 — A 를 실제보다 약하게 정의했다 (공정성).**
  - 근거:
    - A 는 "엔티티 공유 허용" 을 규칙으로 정했다(`:48`). 그래서 `SentenceService.getActive` 가 `SentenceEntity` 를 반환한다(`examples/.../option-a-layered/SentenceService.java:31`). 이것은 §5 에 무조건 규칙으로 나온 **R5 위반**이다(`ConventionArchitectureTest` R5 는 `@Service` public 메서드의 `@Entity` 반환을 금지한다).
    - conventions-proposal §3.1 은 "A 를 고르면 R5 를 완화한다" 고 적었지만 이 문서에는 그 내용이 없다.
    - A 에서도 `SentenceService` 가 record 를 반환하고, `infra/similarity` 에 `SimilarityScorer` 같은 예외 변환 클래스를 두고, ArchUnit "다른 기능의 repository·domain 접근 금지" 규칙(`noClasses().that().resideInAPackage("..answer..").should().dependOnClassesThat().resideInAnyPackage("..sentence.repository..","..sentence.domain..")`)을 쓸 수 있다. 그러면 P1·P4·P5 의 차이가 크게 줄어든다.
    - examples README §5 의 A 약점 2건(엔티티 import, `callSimilarity` 잔존)은 구조 때문이 아니라 A 에 일부러 적용하지 않은 컨벤션 때문이다.
  - 요구 수정: "A′ = 레이어드 + 서비스 record 반환 + 기능 간 repository 접근 금지 ArchUnit" 을 비교표에 추가한다. 또는 A 의 P1/P4/P5 판정이 "엔티티 공유 허용" 이라는 선택 때문임을 명시하고, A 선택 시 R5 를 완화한다는 점을 §5 에 적는다. 그러면 B 의 순수 이득("공개 API 위치가 base package 로 고정되고, 규칙이 모듈 일반화로 표현된다")이 분명해진다.

- **[M3] §4.3·§5 — R5(conventions §3.1, 서비스 DTO 반환)를 해소하는 단계가 계획에 없다. 연쇄적으로 OSIV 순서가 깨진다.**
  - 근거:
    - R5 위반은 `SentenceService.getRandom` 이다(current-state §6.2). `SentenceReader` 도입(ARCH-B3)과는 무관하다.
    - 그런데 feature_list 는 R5 해제를 ARCH-B3 acceptance 에 넣었다. ARCH-B3 은 D1=B 가 전제이고 ARCH-A1→B2 뒤에 온다.
    - FIX-F4 는 `open-in-view=false` 를 포함하고 TEST-1 에만 의존한다. 그래서 feature_list 순서대로 하면 **서비스가 DTO 를 반환하기 전에 OSIV 가 꺼진다.** D4("순서가 중요 — DTO 반환 먼저, OSIV off 나중")와 conventions §3.4("§3.1 을 먼저")를 모두 어긴다.
    - A 를 고르면 R5 는 영원히 `@Disabled` 로 남는다.
  - 요구 수정: §4.3 에 "conventions §3.1 (getRandom DTO 반환, R5 해제)" 단계를 구조와 무관한 단계 0 쪽에 넣고 FIX-F4 앞에 둔다. feature_list 쪽 수정(ARCH-B3 에서 R5 제거, 별도 항목 추가, FIX-F4 depends_on 추가)도 함께 요구한다.

- **[M4] §3.2 마이그레이션 비용 — "파일 단위 실측 목록" 이라고 했지만 합계 근거가 열거되지 않았고, 일부 수치가 서로 맞지 않는다.**
  - B 재계수 (examples 기준, 일치 확인): 신규 4 (`SentenceReader`, `SentenceDetail`, `SimilarityScorer`, `SimilarityClientConfig`), 이동·수정 1 (`SimilarityClient`), 이동 또는 흡수 2 (`SimilarityRequest`·`SimilarityResponse`), 이동 1 (`SimilarityException`), 삭제·수정 1 (`RestClientConfig`), 수정 2 (`AnswerService`, `AnswerResponse`), 그리고 `application.properties` (`timeout-ms` → `connect-timeout`/`read-timeout`, B 예시 `SimilarityProperties`). 합계 약 12 이고 수치는 맞다. 다만 `application.properties` 가 표에 빠져 있다.
  - A 재계수: 같은 방식으로 6~7 (`SimilarityClientConfig` 신규, `RestClientConfig`, `SentenceService`, `AnswerService`, `AnswerResponse`, `application.properties`, F1 포함 시 `SimilarityClient`). "약 5" 보다 크고, §3.1 "+2" 와 §3.2 "1파일 신규" 도 서로 다르다.
  - C: "약 30+ (`src/main` 34파일 대부분)". 그런데 `common` 11파일과 `CoreApplication` 은 C 에서 바뀌지 않으므로 기존 파일은 최대 22개다. 30+ 는 신규 파일을 합친 수로 보이지만 기준이 적혀 있지 않다.
  - "6개 타입 패키지 이동" 은 파일로는 4개다(중첩 클래스 `RequestInvalid`·`Unavailable` 포함 6타입). 게다가 B 예시는 dto 2개를 `SimilarityClient` 에 흡수한다(3→1). 그러면 §4.2 의 "`git mv` 로 blame 유지" 가 dto 2파일에는 성립하지 않는다.
  - 요구 수정: 선택지별 파일 목록을 부록으로 열거하거나 examples README §3 표에 링크한다. 계수 기준(신규·수정·이동·삭제 포함 여부)을 명시한다. A 수치, `application.properties`, "6개 타입(4파일)" 표기를 정정한다.

- **[M5] §5 R5 — 규칙 설명과 구현 범위가 다르다.**
  - 근거: `ConventionArchitectureTest.notReturnEntity()` 는 `method.getRawReturnType().isAnnotatedWith(Entity.class)` 만 검사한다. 그래서 `Optional<SentenceEntity>`, `List<SentenceEntity>`, `Page<…>` 반환은 통과한다. §5 는 "@Entity 타입을 반환하지 않는다 — P4 (OSIV 의존)" 라고 하는데, 서비스가 목록·Optional 을 반환하는 순간 구멍이 생긴다. 목록 조회는 06 §5.6 에 Phase 5+ 로 예정돼 있다.
  - 요구 수정: 구현을 `method.getReturnType().getAllInvolvedRawTypes()` 기준으로 바꾸거나, §5 에 "raw 반환 타입만 검사 — 제네릭 인자는 미검사" 라는 한계를 적는다.

- **[M6] §2 C 트리 — examples README 와 모순되고, C 의 핵심 약점이 비교에서 빠졌다.**
  - 근거:
    - §2 C 트리(`:113-114`)는 `SentencePersistenceAdapter, SentenceJpaEntity, SentenceMapper` 를 `answer.adapter.out.persistence` 에 둔다. examples README §3 C 표는 같은 어댑터를 `sentence/adapter/out/persistence/` 에 두고, §5 에서 그 결과로 생기는 "`sentence → answer` 역방향 의존" 을 C 의 최대 약점으로 든다. D1 표도 이 약점을 인용한다.
    - 이 문서의 트리는 `answer` 가 `sentence` 의 JPA 엔티티를 소유하는 세 번째 배치다. §3.1 에는 이 약점(포트 소유권 문제)이 한 줄도 없다.
  - 요구 수정: 트리를 examples 와 맞추고, §3.1 에 "포트 소유·어댑터 위치 문제(README §5)" 행을 추가한다. C 에 불리한 내용이라도 결정자가 볼 수 있어야 한다. 반대로 지금 트리는 C 를 실제와 다르게(더 이상하게) 보여 줘서 공정성 문제도 있다.

## 경미

- [m1] §5 표에 R4b(`LayerArchitectureTest.r4b_…`, domain → controller·service·repository·dto 금지)가 없다. D9 가 이 규칙을 근거로 쓴다("ArchUnit R4b 위반 예정"). §5 에 추가한다.
- [m2] §3.2 ArchUnit 행 "레이어 4 + 모듈 경계 2" 는 실제 구현(모듈 R1~R3 3개, 레이어 R4·R4b 2개, 컨벤션 R5~R7 3개)과 다르다. A 의 "레이어 규칙 4개" 도 무엇을 뜻하는지 알 수 없다.
- [m3] §0 "F5·F6·F7 을 구조로 막고". F5 의 절반(컨트롤러 vs 서비스 변환 위치 혼재)은 구조가 아니라 R5·conventions §3.1 로 해소된다. "F5 중 기능 간 엔티티 결합" 으로 한정한다.
- [m4] §1 "기능 버그 F1~F3 은 구조와 무관" 에서 F4 가 빠졌다. §5 R6 은 F4 재발을 막는다고 하고, B 예시는 "F4 구조적 해소" 라고 주석을 단다. F4 가 구조 문제인지 아닌지 한쪽으로 정리한다.
- [m5] §5 R7 설명 "복합 타입 파라미터는 … 필수" 는 구현과 다르다. 구현(`haveBoundParameters`)은 `URI`·`HttpMethod`·`UriBuilderFactory` 를 뺀 **모든** 파라미터를 검사한다. 설명을 "모든 파라미터(허용 특수 타입 제외)" 로 고친다.
- [m6] §5 R6 은 메서드 레벨 `@Transactional` 만 본다(`target.get().isAnnotatedWith(Transactional.class)`). 클래스 레벨 `@Transactional` 이 붙은 클래스 안의 self-invocation 은 탐지하지 못한다. 한계를 적거나 owner 의 클래스 어노테이션도 검사한다.
- [m7] §2 B 트리의 `similarity/` 에 `exception/` 이 없다. examples README §3 B 는 `similarity/exception/` 을 둔다. 트리를 맞춘다.
- [m8] P2 의 해법(B)에서 외부 클라이언트 배치 규칙이 명시 문장으로 적혀 있지 않다. Similarity 는 독립 모듈이고 Toss 는 기능 모듈 internal 이다. "외부 시스템 클라이언트는 그것을 소유하는 모듈의 internal, 소유 기능이 없거나 2개 이상이 쓰면 독립 모듈" 같은 한 줄 규칙이 있어야 P2 가 "해결" 됐다고 할 수 있다.
- [m9] §4.1-2 는 WIP 가 "이미 B 의 방향" 이라는 근거로 쓰인다. 그런데 같은 WIP 는 빈을 `common/config/RestClientConfig` 에 등록한다(current-state §5). 이것은 B 의 R3 과 반대 방향이다. 근거가 절반만 맞는다는 점을 적는다.
- [m10] 소요 추정 0.5 / 1~1.5 / 3~5일은 (estimated) 표기만 있고 산정 기준(파일당 시간, 리뷰 포함 여부)이 없다.

## 이전 라운드 지적 해소 여부 (해당 시)

- 대상 문서의 첫 리뷰라 해당 없음. `reviews/current-state-round-1.md` 는 다른 문서 대상이다.

## 검증한 주장 (문제 없음 확인)

- 라인 참조가 모두 실제 코드와 맞는다: `AnswerService.java:24`(SentenceRepository 필드), `:37-38`(findByIdAndDeletedAtIsNull / SentenceException.NotFound), `AnswerResponse.java:30`(`of(SentenceEntity, …)`), `RestClientConfig.java:3`(infra.similarity import).
- `src/main` Java 파일 수는 34 다.
- `infra.similarity` 타입은 6개(파일 4개)다: SimilarityClient, SimilarityRequest, SimilarityResponse, SimilarityException + 중첩 2.
- current-state §6 의 위반 수(R1 1사이클, R2 19, R3 2, R5 1, R6 1, R7 1)와 R4·R4b 통과를 재실행으로 확인했다.
- R2 구현(`moduleOf`, `isModuleApi`, `common` 예외)은 §2 B 정의("직속 패키지 = 모듈, base package = 공개, common 은 공유 커널")와 맞는다.
- R1(`slices().matching("com.tmi.core.(*)..")`), R3, R4(공개 API `@Service` 를 Service 레이어로 포함)의 구현은 §5 설명과 맞는다.
- B 예시(`option-b-modular/answer/AnswerService.java`)는 다른 모듈의 base package 타입만 import 한다. 그래서 R2 를 통과하는 형태다. `SentenceDetail` 은 §4.2 대로 3필드(`id`, `englishText`, `koreanRef`)다.
- Modulith 의 "직속 하위 패키지 = 모듈"(`directSubPackage`)과 "base package = 공개, 하위 패키지 = internal"(unnamed interface 가 하위 패키지를 제외) 은 소스로 확인했다. 즉 B 의 모듈 규칙 자체는 Modulith 와 같다. 차이는 `common` 처리뿐이다([M1]).
- 02 §2.2 의 "파일 수 2~3배" 와 "헥사고날에서 전환" 은 원문에 있다. C 를 "되돌린 결정의 재도입" 이라고 한 것은 정확하다.
- decisions-needed D1 의 비용 수치(약 5 / 12 / 30+, 0.5 / 1~1.5 / 3~5일)와 추천 B 는 이 문서와 같다(수치 자체의 정확성은 [M4]).
- §4.2 의 "`SentenceException.NotFound` 가 경계를 넘어도 `GlobalExceptionHandler` 가 `BaseException` 으로 처리" 는 `GlobalExceptionHandler.java:25` `@ExceptionHandler(BaseException.class)` 로 확인했다.
- §4.2 의 "CLAUDE.md 에 규칙 명시" 는 `CLAUDE.md:37` 에 `[제안]` 으로 반영돼 있다. ai-workflow §3 체크리스트 3번(R2)도 있다.
