# current-state 리뷰 — Round 3

> 대상: `docs/ai/context/current-state.md` @ `3e136c6` (579줄, 마지막 변경 커밋 `18eea46`). 라인 번호는 이 버전 기준이다.
> 검증 방법: 전용 worktree(`git worktree add --detach … 3e136c6`)에서 실행했다. 끝난 뒤 제거했다.
> - **ArchUnit @Disabled 무시 실행**: worktree 에만 `src/test/resources/junit-platform.properties`(`junit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`)를 두고 `bash ./gradlew test --tests 'com.tmi.core.architecture.*'` 를 돌렸다. 결과는 8 tests, 6 failed. `build/test-results` XML 의 실패 메시지 원문을 §6.1·§6.2 와 한 줄씩 대조했다.
> - **음성 대조 2 재현**: worktree 의 `sentence.service`·`sentence.controller`·`sentence.domain` 에 위반 클래스 3개를 넣고 전체 규칙을 다시 돌렸다. `@Service` 는 `Optional<E>`·`List<E>`·`Map<String,E[]>`·`E[]` 를 반환하게 했다. 확인 후 삭제했다.
> - **read timeout 프로브**: 일회성 테스트를 만들었다. `RestClientConfig` 와 같은 `SimpleClientHttpRequestFactory`(connect 1s, read 2000ms)를 쓰고, 인터페이스는 `@RequestBody` 를 붙인 사본(F1 우회)을 썼다. 이것으로 3초 지연 서버(`com.sun.net.httpserver`)와 닫힌 포트를 호출해 예외 체인을 출력했다. 확인 후 삭제했다.
> - **Spotless**: `clean compileJava` → class 사본 보관 → `spotlessApply` → `git diff --stat` → `compileJava` 를 돌렸다. 적용 전후 class 를 `javap -v`(Utf8/String 상수)와 `javap -c -p`(바이트코드 전체)로 비교했다.
> - **정상 빌드**: `bash ./gradlew build`(로컬 PostgreSQL)를 돌리고 테스트 수를 XML 에서 셌다.
> - round-2 지적 12건(주요 2, 경미 10)을 본문과 하나씩 대조했다. feature_list.example.json, ai-workflow.md, decisions-needed.md, architecture-proposal.md, research.md, ArchUnit 테스트 소스와 교차 대조했다.
>
> [R실행] 은 이번 리뷰어가 직접 실행해 얻은 결과다.

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | 이번에 새로 들어간 주장(read timeout → 500, R2 10+9, 음성 대조 2, 30파일, 상수 풀 동일)은 모두 실행으로 재현된다. 다만 "R6 한계는 테스트 코드 주석에 명시" 는 절반만 맞다. "40개 class 파일" 도 실제로는 43개다 |
| 구체성 | 4 | 위반 원문, 해제 조건 id, 재현 절차까지 적혀 있어 바로 작업할 수 있다. 다만 §3.3(f) 의 새 결함에서 해소 작업 id(`FIX-F1T`)로 가는 연결이 없다 |
| 일관성 | 4 | 대상 문서는 feature_list·테스트 `@Disabled` 문구·decisions-needed D5 와 맞는다. 다만 architecture-proposal P1 은 아직 "R2 19건" 을 `answer→sentence` 결합의 근거로 쓴다. ai-workflow 도 "29파일" 로 남아 있다 |
| 판단 가능성 | 4 | F1 을 고치는 즉시 드러나는 다음 결함(타임아웃 500)을 먼저 알려 주어 실행 순서(D 게이트) 판단에 바로 쓰인다. R2 수치도 현행 규칙 기준과 B 기준으로 나뉘어 있어 D1 판단의 소음이 사라졌다 |

## 치명 (승인 차단)

없음.

## 주요

없음.

## 경미

- [m1] (교차 문서, 우선) `architecture-proposal.md:29` P1 — **round-2 M1 과 같은 오독이 결정 문서에 남아 있다.** "`answer` 가 `sentence` 의 리포지토리·엔티티·예외를 직접 사용 — ArchUnit R2 위반 19건 (§6)" 이라고 적혀 있다.
  - 근거 [R실행]: R2 의 19건 중 `answer→sentence.*` 는 10건이다. 나머지 9건은 `answer→infra.similarity.*` 다. current-state §6.1 L463 과 F6 L25 는 이미 이렇게 나눠 적었다.
  - D1 을 판단하는 사람은 architecture-proposal 을 먼저 읽는다. 그래서 P1 의 규모가 약 2배로 읽힌다.
  - 요구 수정: P1 근거를 "R2 중 10건 (나머지 9건은 B 기준에서만 위반 — current-state §6.1)" 으로 바꾼다. 같은 문서 L230 의 "위반 19 → `ARCH-B3`" 은 틀리지 않다(B3 가 B2 에 의존). 다만 "10 + 9(B2 에서 해소)" 로 쓰면 더 정확하다.
- [m2] §6.3 L503 R6 행 "테스트 코드 주석에 명시" — 주석에 적힌 것은 클래스 레벨 한계뿐이다. `jakarta.transaction.Transactional` 미탐지는 주석에 없다(`ConventionArchitectureTest.java:107-108`). 주석은 클래스 레벨의 경우를 "바깥 메서드도 같은 트랜잭션 설정을 받으므로 대부분 무해" 라고 설명한다. 반면 문서는 "클래스에 `REQUIRES_NEW`" 를 위험 사례로 든다. 두 설명의 결론이 다르다. architecture-proposal L232 의 R6 한계 칸에도 jakarta 가 빠져 있다.
  - 요구 수정: 문서를 "클래스 레벨은 주석에 근거와 함께 명시, jakarta 는 미명시(현재 코드에 사용처 0)" 로 고친다. 아니면 테스트 주석에 jakarta 를 추가한다. `REQUIRES_NEW` 예시는 주석의 "대부분 무해" 판단과 맞춰 정리한다.
- [m3] §7.3 L574 "`src/main` 40개 class 파일의 상수 풀 … 전부 동일" — 실제 class 파일은 **43개**다. 40개 타입에 `ELogLevel$1`·`$2`·`$3`(enum 상수 본문의 익명 클래스) 3개가 더해진다. 결론은 맞다 [R실행: 43개 모두 Utf8/String 상수 동일]. 근거는 더 강하게 쓸 수 있다. [R실행] `javap -c -p` 바이트코드 전체도 43개 모두 같다(diff 0). 상수 풀만 비교하면 문장 순서가 바뀐 경우를 잡지 못한다. 바이트코드 비교는 그 경우까지 배제한다. 요구 수정: "40개 타입(43 class 파일)의 상수 풀과 바이트코드(`javap -c -p`)가 동일" 로 고친다.
- [m4] §3.3(f) L287 과 F3 L22 — read timeout → 500 결함을 새로 적었지만 해소 작업 id 가 없다. feature_list `FIX-F1T`, decisions-needed 순서 3번, ai-workflow L194 는 이미 이 결함을 근거로 든다. F1·F4 등 다른 발견은 §6.1 에서 id 로 연결된다. 요구 수정: §3.3(f) 첫 항목 끝에 "→ `FIX-F1T`" 를 붙인다(빈 본문 NPE 항목도 같다).
- [m5] §7.2 표 L528-535 — round-2 m7 이 일부만 해소됐다. P9 는 P8 로 바뀌었다. 그러나 행 순서가 여전히 P1…P6, **P8, P7** 이다. P7 행을 맨 아래로 둔 것은 아래의 "P7 치명 결함" 설명과 이어지게 하려는 의도로 보인다. 그렇다면 번호를 바꾸는 편이 낫다(P7↔P8).
- [m6] 수치 표기가 문서마다 다르다.
  - current-state §7.3 L578 은 "이 커밋은 `src/main` 29파일을 건드리므로" 라고 쓴다. 같은 표 L573 의 "일괄 적용 커밋 30 파일" 과 decisions-needed D5(`decisions-needed.md:117` "30파일(src/main 29 + 테스트 1)") 와 표현이 다르다.
  - `ai-workflow.md:43` 은 "`SIG-2` (29파일)" 이다.
  - 셋 다 "30파일 (src/main 29 + 테스트 1)" 로 통일한다. `ai-workflow.md:19` 의 "기존 `src/main` 29파일 미포맷" 은 맞으므로 그대로 둔다.
- [m7] L5 개정 이력 — "Round 1 리뷰 반영" 만 있다. 이번 개정(round 2 반영: R2 분해, R5 수정, 음성 대조 2, 타임아웃 500)은 적혀 있지 않다. 다음 독자가 어느 리뷰까지 반영됐는지 알 수 없다. 한 줄을 추가한다.

## 이전 라운드 지적 해소 여부

| 지적 | 상태 | 확인 내용 |
|---|---|---|
| M1 R2 19건 중 9건은 현행 허용 의존 | **해소** (교차 문서 잔존 → m1) | §6.1 L463 이 "10(현행 규칙으로도 문제) + 9(B 기준에서만)" 로 나눴다. §6.2 L477·L480 은 "현행 규칙상 허용" 이라고 적는다. F6 L25 는 "R2 중 `answer→sentence` 10건" 으로 한정했다. [R실행] 건수 구성(생성자 1·필드 1·`of` 4·`loadSentence` 3·`score` 1 / 생성자 1·필드 1·`callSimilarity` 7)이 원문과 일치한다 |
| M2 R5 제네릭 미탐지, R6 한계 미기재 | **해소** (서술 일부 부정확 → m2) | HEAD `ConventionArchitectureTest.java:99` 가 `getReturnType().getAllInvolvedRawTypes()` 를 쓴다. §6.3 L498-505 에 탐지 한계 표가 추가됐다. [R실행] 현행 R5 위반은 계속 1건이다. 음성 대조로 `Optional`·`List` 와 `Map<String,E[]>`·`E[]` 까지 탐지된다 |
| m1 응답 JSON 래핑 | 해소 | §7.2 헤더 L526 에 "`{"error":{...}}` 로 래핑 — 표에서는 안쪽만" 이 적혀 있다. §3.3(b) L238 은 전체 형태로 적었다 |
| m2 Spring Data 경유 native enum | 해소 | §4.3 L411 이 [실행·리뷰] 로 확정했다. F10 L29 는 "native query 에서는" 으로 범위를 한정했다. JPQL `function('random')` 대안은 넣지 않았다(선택 사항이라 문제없다) |
| m3 R4b WIP 셈 단위 | 해소 | L466 "1 클래스 쌍(파라미터 1 + 접근자 체인 20여 건) [정적]". `wip-snapshot.patch` 의 +103~+112 에서 접근자 호출 26건을 셌다 |
| m4 해제 조건 열 동기화 | 해소 | R5 `FIX-R5`, R2 `ARCH-B3` / A′ 면 `ARCH-A2`. feature_list(`:130`, `:226`, `:246`)와 테스트 `@Disabled` 문구(`FIX-R5 완료 시 해제`)가 일치한다. 테스트의 R2 문구 "ARCH-B2·B3" 는 B3→B2 의존 관계라 모순이 아니다 |
| m5 30파일, 상수 풀 근거 | 해소 (수치 표기 → m3, m6) | [R실행] `git diff --stat` 전체 `30 files changed, 180(+)/218(−)`, `src/main` 만 보면 `29 files, 178/214`, `CoreApplicationTests` `+2/−4` |
| m6 R4·R4b 음성 대조, 픽스처 보존 | 부분 해소 (수용 가능) | 음성 대조 2 가 추가됐다 [R실행 재현: R4 +1, R4b +1]. 픽스처는 커밋하지 않았다. 대신 L505 에 한계와 재현 절차를 적었다. 이번에 그 설명만으로 재현할 수 있었으므로 수용한다 |
| m7 P 번호 | **부분 해소** → m5 | P9→P8 로 바뀌었다. 순서는 여전히 P8, P7 이다 |
| m8 research.md 모순 2건 | 해소 | `research.md:103` 은 slash basename 을 실행 확인으로 적는다 [R]. `:182` 는 "엔진을 쓰지 않아 호환성은 여전히 미검증" 이라고 적는다 |
| m9 07 파일명·H1 불일치 | 해소 | §3.7 L378 |
| m10 WIP 복원 트리거 | 해소 | §5 L439: "7단계 마지막 커밋·push 직후", `git status --short src/main` 4파일(+`.gitattributes`) 확인, blockers.md §1 기록 |

## 검증한 주장 (문제 없음 확인)

- **§3.3(f) read timeout → 500** [R실행]: 3초 지연 서버에 readTimeout 2000ms 로 호출했다. 결과는 `RestClientException("Error while extracting response for type [SimilarityResponse] …") ← SocketTimeoutException("Read timed out")` 이다. `instanceof ResourceAccessException` 과 `instanceof HttpStatusCodeException` 은 둘 다 false 였다. 따라서 `AnswerService.java:47-55` 의 두 catch 를 지나 `GlobalExceptionHandler.handleUnknown`(`:68-71`)에서 500 이 된다. 대조로 연결 거부는 `ResourceAccessException ← ConnectException` 이어서 503 경로를 탄다. `AnswerApi.java:93` 의 503 설명 "Similarity 서버 다운·타임아웃·네트워크 오류" 와 다르다. "F1 때문에 지금은 드러나지 않는다" 는 서술도 맞다. 프로브는 `@RequestBody` 를 붙인 사본으로만 요청을 보낼 수 있었다.
- **§6.1·§6.2 전체** [R실행]: R1 사이클 1(`common → infra → common`, infra 쪽 6건 원문 일치), R2 19(10+9), R3 2(`RestClientConfig.java:38`), R5 1(`returns @Entity SentenceEntity`), R6 1(`AnswerService.java:28`), R7 1, R4·R4b 통과. `callSimilarity` 의 예외 생성 3건 위치는 `:49`, `:51`, `:54` 다.
- **§6.3 음성 대조 2** [R실행]: R5 는 `optional()`·`list()` 를 잡았고(추가한 `nested()`·`array()` 도 잡았다), R4 는 `ProbeController.repo has type SentenceRepository` 1건, R4b 는 `ProbeDomain.r has type SentenceResponse` 1건을 잡았다. 다른 규칙의 건수는 바뀌지 않았다(R1 1, R2 19, R3 2, R6 1, R7 1).
- **§7.1** [R실행]: `bash ./gradlew build` → 성공. 테스트 9개 중 skipped 6, failure 0.
- **§7.3** [R실행]: 29/34 파일, +178/−214, 일괄 커밋 30파일. 적용본도 컴파일된다. 상수 풀과 바이트코드가 동일하다(m3 의 표기 문제만 있다).
- feature_list `FIX-F1T`(`:44-58`)의 acceptance(3초 지연·빈 200 → `Unavailable`)와 decisions-needed L17·L138, D13(L36)이 §3.3(f) 서술(타임아웃 500, 빈 본문 NPE 500, 600+ 성공 처리)과 서로 맞는다.
- architecture-proposal §5 표(L228-235)의 R1~R7 해제 id·한계가 §6.1 과 일치한다(R6 jakarta 누락만 m2).
- ai-workflow L16·L18("8개 중 6개 `@Disabled`")이 §7.1 실행 결과와 일치한다.
- §1.2 `AnswerApi` 행: `AnswerApi.java:86` 의 "Invalid request to similarity service"(마침표 없음)와 `messages.properties:13`(마침표 있음)의 차이를 확인했다.
