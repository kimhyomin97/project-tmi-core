# final-consistency 리뷰 — Round 1

> 대상 커밋: `01898ba` (전용 worktree `scratchpad/wt-final-consistency`, detached HEAD)
> 직접 평가 대상: `docs/ai/research.md`, `docs/ai/decisions-needed.md`, `/CLAUDE.md`, `src/test/java/com/tmi/core/architecture/*.java`, `docs/ai/blockers.md`. 다른 docs/ai 문서·docs/public·src/main 은 교차 확인용으로 읽었다.
> 검증 방법:
> - [실행] worktree 에서 `bash ./gradlew build --no-daemon` → **BUILD SUCCESSFUL** (CoreApplicationTests 1 통과, Layer 2 통과, Convention 3·Module 3 skip)
> - [실행] `bash ./gradlew test --tests 'com.tmi.core.architecture.*'` → 8개 실행, skip 6. CLAUDE.md 명령 동작 확인
> - [실행] feature_list 의 메서드 단위 verify 형식 `--tests '*ConventionArchitectureTest.r7_*' && grep -q 'skipped="0"' …` → XML 이 r7 1건만 담고(`tests="1" skipped="1"`) 이전 실행 결과는 남지 않음, grep 이 rc=1 로 정확히 실패 (현재 @Disabled 이므로 의도대로)
> - [실행] `bash ./gradlew spotlessCheck` → 실패, "Violations also present in 29 other files" (CLAUDE.md "기존 코드 위반 있음" 과 일치)
> - [실행] `docker info` → `failed to connect to the docker API` (데몬 없음)
> - [실행] `git apply --check docs/ai/wip-snapshot.patch` 통과 (5파일), `/home/user/repo` 작업 트리 clean, `git stash list` 에 `stash@{0}` 존재, `~/.gradle/init.d/mirror.gradle` 존재, PostgreSQL 16 이 `/var/tmp/pgdata -p 15432` 로 기동 중
> - [정적] 인용 절 번호 전수 대조: decisions-needed·CLAUDE.md·ArchUnit 주석이 인용하는 current-state / conventions / architecture-proposal / ai-workflow / blockers / docs/public 절, 다른 문서가 인용하는 research §0·§3.1·§3.2·§4.2·§5·§5.2·§5.3·§6.1·§6.3·§7·§8·§9

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | 빌드·ArchUnit·verify 명령·위반 수가 실행으로 재현됨. 다만 research 에 나중 문서가 실행으로 뒤집은 "estimated/미검증/예정" 주장이 4곳 남아 있음 (M4) |
| 구체성 | 4 | 결정마다 추천·다음 작업 id 가 있고, verify 명령은 그대로 실행 가능. D9 만 feature_list id 가 없음 (m1) |
| 일관성 | 3 | §0 "결정 없이 가능" 에 D13 결정이 숨어 있음(M1), "결정하면 바로 시작" 이 다른 결정에 걸린 의존을 빼먹음(M2), D5≠(a) 시 FIX-F3 교착(M3), CLAUDE.md 의 `[제안]` 표기 누락(M5)·README 지시(M6) |
| 판단 가능성 | 3 | 문서 자체는 아침에 읽고 결정하기 좋게 짜여 있으나, D2·D4·D1(B) 를 각각 단독으로 결정해도 작업이 시작되지 않는다는 사실이 decisions-needed 에 드러나지 않음 |

문서별 한 줄 판정:

| 문서 | 판정 |
|---|---|
| decisions-needed.md | **수정 필요** — 순서·추천·id 는 대체로 맞음. §0 과 D13 의 모순(M1), 결정 간 의존 누락(M2·M3) |
| CLAUDE.md | **경미 수정 후 승인 가능에 가까움** — 67줄, 명령 전부 동작, 사실 규칙은 정확. `[제안]` 표기 누락 3건(M5), 존재하지 않는 README 절 지시(M6) |
| research.md | **수정 필요** — 등급·URL 체계는 좋음. 나중 문서가 실행으로 뒤집은 주장이 남아 있음(M4), §0 ArchUnit 선택이 실제와 다름 |
| ArchUnit 테스트 | **승인 가능** — `./gradlew build` 통과, 주석·@Disabled 사유·위반 수가 current-state §6 과 feature_list verify grep 패턴에 맞음. R2 사유 문구에 A′ 경로 누락만 남음 (m4) |
| blockers.md | **경미 수정** — 내용은 사실과 맞음. Docker 데몬 부재와 "현재 WIP 가 어디 있는지" 가 빠짐 (M7, m5) |

## 치명 (승인 차단)

없음. 빌드가 통과하고, 문서가 주장하는 명령·위반 수·절 번호가 모두 실재한다.

## 주요

- **[M1] decisions-needed.md:17 (§0 표 3행 `FIX-F1T`) ↔ :37·:140 (D13) ↔ feature_list.example.json:52·:62 — "결정 없이 바로 해도 되는 것" 에 결정 사항이 들어 있다.**
  §0 머리말(:11)은 이 표의 항목이 "선택지가 없다" 고 말한다. 그런데 `FIX-F1T` 의 title 은 "점수 범위 정책(D13 (b))" 이고, acceptance 는 `1.0000001 → 1.0, 1.5 → Unavailable, "NaN" → Unavailable` 이다. 이것은 D13 의 선택지 (a) clamp / (b) 허용 오차 / (c) 전부 503 중 (b) 를 이미 고른 결과다. D13 표(:37)도 "막고 있는 작업: `FIX-F1T`" 라고 적어 같은 문서 안에서 모순이다. ai-workflow.md:24 도 FIX-F1T 를 "SIG-1 을 기다리지 않는" 항목으로 넣었다. 그래서 루프가 H0 에서 D13 을 확정하지 않고 시작하면, 에이전트는 사람이 고르지 않은 정책을 acceptance 로 구현한다. 또 D13 은 13개 중 10위인데 §0 의 3번째 작업을 막고 있어 중요도 순서와도 맞지 않는다.
  **요구 수정**: (i) §0 의 FIX-F1T 를 "빈 응답·timeout 503 부분만 결정 불필요, 점수 정책은 D13" 으로 나누거나, FIX-F1T 를 둘로 쪼갠다(예: `FIX-F1T` = timeout·null, `FIX-F1S` = 점수 범위, depends_on D13 결정). (ii) 쪼개지 않는다면 D13 을 D1 앞쪽으로 올리고 §0 머리말에 "단 FIX-F1T 는 D13 확정 필요" 를 적는다.

- **[M2] decisions-needed.md:56 (D1), :78 (D4), :89 (D2) — "결정하면 바로 시작" 이 다른 결정에 걸린 depends_on 을 빼먹었다.**
  - D1=B → `ARCH-B2`: :56 은 "둘 다 `ARCH-A1`·`FIX-F1T` 이후" 라고만 쓴다. 그러나 feature_list.example.json:241-245 의 ARCH-B2 depends_on 은 `["ARCH-A1","FIX-F1T","SIG-1"]` 이다. ai-workflow.md:219 와 architecture-proposal.md:225 도 "+ B2 는 SIG-1" 이라고 명시한다(ai-workflow round-3 M4 반영분). 즉 B 를 고르면 **D3 도 정해져야** B2 를 시작할 수 있는데, decisions-needed 만 이 반영을 놓쳤다.
  - D2 → `FIX-F3`: :89 는 "결정하면 바로 시작: FIX-F3" 이지만 FIX-F3 의 depends_on 은 `["FIX-F2","SIG-2"]` 이다(feature_list:130-133). SIG-2 는 D5 에 달려 있다. 그런데 D5 행(:34)의 "막고 있는 작업" 칸에는 `SIG-2` 만 있고 FIX-F3 이 없다. 그래서 D2(4위)를 결정한 사람은 D5(7위)도 같이 정해야 한다는 걸 알 수 없다.
  - D4 → `FIX-F4`: :77 은 FIX-R5 의존만 말한다. 실제 depends_on 에는 `SIG-1`(D3)이 있고 `requires: ["docker"]` 도 붙어 있다(feature_list:172-189). 이 환경에는 Docker 데몬이 없다([실행] `docker info` 실패, ai-workflow.md:22). 따라서 이 세션 환경에서는 FIX-F4 가 "바로 시작" 될 수 없다.
  **요구 수정**: 각 "결정하면 바로 시작" 줄에 "추가로 필요: D3(SIG-1)" / "D5(SIG-2)" / "Docker 환경" 을 적는다. §1 표에는 "함께 결정해야 하는 항목" 열을 두거나 순위 사유에 반영한다.

- **[M3] feature_list.example.json:130-133, :110 ↔ ai-workflow.md:96 ↔ decisions-needed.md D5(:114-123) — D5 가 (a) 가 아니면 FIX-F3 가 영구히 시작되지 않는다.**
  SIG-2 title(:110)과 ai-workflow H0(:96)은 "D5 가 (a) 가 아니면 SIG-2 를 items 에서 뺀다" 고 한다. 그러나 FIX-F3 의 `depends_on: ["FIX-F2","SIG-2"]` 를 고치라는 지시는 어느 문서에도 없다. ai-workflow §4.4(:272)의 선택 규칙은 "`depends_on` 미충족이면 제외" 이므로, 존재하지 않는 SIG-2 는 끝내 충족되지 않는다. 결과적으로 D5=(b)/(c) 를 고르면 FIX-F3, 그리고 FIX-F3 에 걸린 DOC-1 까지 교착된다. D5 상세는 (b)·(c) 를 정상 선택지로 제시하면서 이 결과를 적지 않았다.
  **요구 수정**: D5 상세와 ai-workflow H0 에 "D5≠(a) 이면 FIX-F3 의 depends_on 에서 SIG-2 를 지운다" 를 추가한다. 또는 feature_list 에 그 규칙을 `_comment` 로 둔다.

- **[M4] research.md — 나중 문서가 실행으로 뒤집은 주장 4건과 실제와 다른 선택 1건이 그대로 남아 있다** (focus (3)).
  1. :24 §0 버전 표: ArchUnit 의 "이 프로젝트에 맞는 선택" 이 `archunit-junit5 1.5.1` 이다. 실제 build.gradle 은 core `com.tngtech.archunit:archunit:1.5.1` 이다. 같은 문서 §6.3(:182)과 current-state §6.3 은 "junit5 엔진을 쓰지 않는다" 고 말한다. 문서 내부 모순이다.
  2. :95 §3.1 적합성: "(estimated — 오버라이드 시그니처는 Framework 7 소스로 확인 필요)". conventions-proposal.md §0(:20)은 After 코드를 worktree 에 적용해 실행 검증을 마쳤다 → [R] 로 바꿔야 한다.
  3. :175 §6.2: "`docker` CLI 는 있으나 데몬 사용 가능성 미검증". ai-workflow.md:22 와 이번 [실행] 에서 **데몬이 없다**는 것이 확인됐다. FIX-F4 의 `requires: docker` 근거이므로 결정에 영향을 준다.
  4. :190 §7: Spotless "Gradle 9 … → 이 저장소에서 실행 확인 예정". current-state §7.3(:573)이 이미 "Gradle 9.3.0 + JDK 21 에서 정상 실행 ✓" 로 확인했다.
  5. :212 §8 적합성: "Stop hook(`check-done.sh`)… → `feature_list.json` 루프로 확장하기 쉽다". ai-workflow.md §4.6(:403)과 D12 는 이 hook 에 영구 비활성 결함이 있다고 보고 **내리고 새 hook 으로 교체**하자고 한다. 정반대 방향이다.
  **요구 수정**: 위 5곳을 실행 결과로 고치고 해당 문서 절을 링크한다. "결정 근거로 쓰지 말 것" 등급 체계를 둔 문서인 만큼, 승격·강등을 반영하지 않으면 등급의 의미가 약해진다.

- **[M5] CLAUDE.md:58-59 — 제안 단계 규칙 3개가 `[제안]` 표기 없이 "현재 규칙" 처럼 적혀 있고, 사내 문서 05 와 충돌한다.**
  CLAUDE.md:4 는 "`[제안]` 규칙을 근거로 기존 코드를 고치지 말 것" 이라고 한다. 그런데 다음 셋은 표기가 없다.
  - "외부 호출 예외는 **catch 3개**로 … `RestClientResponseException`": conventions §2.4 의 제안이다. 05 §5.7(`05_개발컨벤션.md:306-339`)은 `HttpClientErrorException` / `ResourceAccessException | HttpServerErrorException` 2단 catch 를 규정한다. 현재 코드(`AnswerService.java:47,52`)는 `HttpStatusCodeException`/`ResourceAccessException` 을 쓴다. 고치는 일은 DOC-1(conventions §8 1·2행)과 FIX-F1T 의 몫이다.
  - "throw 직전에 로그 남기지 말 것 (핸들러가 단일 기록)": conventions §5.1 의 제안이다. 05 §6.5(`:385-388`)의 공식 예시가 바로 `log.error("유사도 서버 호출 실패 …", e)` 이고, 현재 `AnswerService.java:53` 도 그렇게 한다. DOC-1 대상(conventions §8 4행)이다.
  - 로그 형식 `"{대상} {동작}: key=value"`: conventions §1.3 의 제안이다.
  표기가 없으면 에이전트는 이 규칙을 근거로 기존 `AnswerService` 를 "정리" 할 수 있다. CLAUDE.md:4 의 안전장치가 무력화된다.
  **요구 수정**: 세 줄에 `[제안]` 을 붙이고 05 §5.7/§6.5 와 다르다는 점을 한 마디 적는다. read timeout → 500 이라는 **사실**(current-state §3.3(f) [실행])은 표기 없이 남겨도 된다.

- **[M6] CLAUDE.md:24 "기동 방법: README §시작하기" — 루트 `README.md` 에는 그 절이 없다. 게다가 루트 README 는 헥사고날 패키지 표다.**
  [정적] 루트 `README.md` 는 7줄짜리 `domain.port.in` / `adapter.out.persistence` 표뿐이다(init 커밋 `f50705c` "헥사고날 아키텍쳐 적용" 이후 그대로). "## 시작하기"(`docker run … -p 15432:5432`)는 `docs/public/README.md:50` 에 있다. 이렇게 되면 에이전트가 CLAUDE.md 의 지시대로 루트 README 를 열고, CLAUDE.md:40 "헥사고날로 바꾸지 말 것" 과 정반대인 구조 설명을 읽게 된다. current-state §3.7(문서·코드 불일치 목록)과 conventions §8(DOC-1)에도 이 낡은 루트 README 가 빠져 있다. conventions-proposal.md:725 의 "README §1" 도 존재하지 않는 절 번호다.
  **요구 수정**: CLAUDE.md:24 를 `docs/public/README.md` §시작하기로 고친다. current-state §3.7 과 DOC-1 목록에 "루트 README.md 가 폐기된 헥사고날 구조를 설명" 행을 추가한다.

- **[M7] blockers.md §2 — Docker 데몬 부재가 없다. 그런데 다른 문서는 이 절을 그 근거로 인용한다.**
  ai-workflow.md:412 는 "`gradlew` 권한, Maven 429, PostgreSQL 기동, **Docker 데몬 부재** (blockers.md §2 — 이 작업에서 약 15분)" 라고 쓴다. 그러나 blockers §2 표는 #1~#3(권한·429·PostgreSQL)뿐이다. 소요 합계도 1+10+5=16분이라 "약 15분" 과 대략만 맞는다. Docker 부재는 FIX-F4·`integrationTest`·SIG-1 의 Testcontainers 검증을 이 환경에서 막는 실제 환경 제약이다([실행] `docker info` 실패).
  **요구 수정**: §2 에 "#4 Docker 데몬 없음 — 조치: `requires: docker` 항목 건너뜀, 우회 없음" 행을 추가한다. 이 행은 D3(a)의 단점 칸(decisions-needed.md:62)에도 연결한다.

## 경미

- **[m1] decisions-needed.md:112 (D9)** — "결정하면 바로 시작" 에 feature_list id 가 없다(패치 적용 → 클라이언트 작성 서술뿐). architecture-proposal.md:229 는 "(D9 결정 후 등록)" 이라고 한다. focus 요구("feature_list id")에 맞추려면 `pending_decision` 에 `TOSS-1`(depends_on `ARCH-B3` / `ARCH-A2`) 같은 자리 표시 항목을 두거나, D9 에 "id 없음 — H0 에서 신설" 을 명시한다. D11(`gradlew` 권한)도 id 가 없지만 "없음 (우회 가능)" 이라고 했으므로 문제없다.
- **[m2] architecture-proposal.md:221 `SIG-1, SIG-2 … depends_on "— / SIG-1"`** — 같은 표의 "a / b" 표기 관례(:219 "— / TEST-1")대로 읽으면 SIG-2 가 SIG-1 에 의존한다는 뜻이 된다. feature_list:113 `depends_on: []` 과 ai-workflow.md:222 "SIG-2 자체는 SIG-1 에 의존하지 않는다 (round-2 m9)" 와 모순이다. "— / —" 로 고친다.
- **[m3] research.md §3.1·§3.2·§4.1 의 [J] 근거 버전** — 소스 jar 는 Boot 4.0.8 / Framework 7.0.8 이고 프로젝트는 Boot 4.0.2 다. 대부분 [R] 로 교차 확인됐으니 결론은 유지된다. 다만 §0 에 "[J] 는 4.0.8 기준, 4.0.2 와 차이 미확인" 한 줄을 넣는다. 또 :89 `spring-webmvc 7.0.8 sources`, :138 `spring-data-jpa 4.0.7 sources`, :193 Checkstyle [J] 는 URL 이 없다. §0 이 표기 규칙을 "등급 + URL" 로 정했으니 맞춘다.
- **[m4] ModuleArchitectureTest.java:280 R2 `@Disabled` 사유** — "ARCH-B2·B3 완료 시 해제" 만 있고, current-state §6.1 의 "A′ 면 규칙 교체 `ARCH-A2`" 경로가 없다(:279 주석도 "D1=B 결정 전제"). feature_list 의 ARCH-A2 verify `! grep '"R2 위반'` 은 이 문자열을 지우거나 교체해야 통과하므로 동작에는 문제가 없다. 사유를 "ARCH-B3(B) 또는 ARCH-A2(A′, 규칙 교체)" 로 바꾸면 세 문서가 일치한다. 또 ARCH-A2 의 verify `--tests '*ModuleArchitectureTest.r2_*'` 는 교체 규칙의 메서드 이름이 `r2_` 로 시작해야 매칭된다. 이름이 `r2a_…` 면 Gradle 이 "No tests found" 로 실패한다 → ARCH-A2 acceptance 에 메서드 이름 규칙을 한 줄 적는다.
- **[m5] blockers.md §1** — 상태가 "작업 종료 시 복원" 이라는 미래형으로만 적혀 있다. 지금(01898ba 시점) 실제 상태는 이렇다: [실행] `/home/user/repo` 작업 트리 clean(WIP 미복원), `stash@{0}` 에 WIP 포함, 패치는 `git apply --check` 통과. "현재 위치: stash@{0} + wip-snapshot.patch, 작업 트리에는 없음" 을 명시하고, 복원 후 이 줄을 갱신하도록 적는다. 또 "src/main 4개 파일" 중 하나는 `src/main/resources/application.properties` 다(패치 diff 헤더 기준). "Java 3 + properties 1" 로 쓰면 오해가 없다.
- **[m6] CLAUDE.md:23** — "`./gradlew test` 는 PostgreSQL 이 필요하다" 뒤에 "단 `--tests` 로 아키텍처·단위 테스트만 돌리면 DB 불필요" 를 덧붙이면 ai-workflow.md:24 의 핵심 운영 사실과 맞는다. [실행] `--tests 'com.tmi.core.architecture.*'` 는 CoreApplicationTests 를 실행하지 않는다.
- **[m7] decisions-needed.md:62 D3 (a) 단점 "통합 테스트엔 Docker 필요"** — "이 클라우드 세션에는 Docker 데몬 없음 → integrationTest·FIX-F4 는 다른 환경 필요" 까지 적어야 (a)/(b) 를 비교할 수 있다. (b)는 이 환경에서도 동작하는 유일한 통합 검증 경로다.

## 이전 라운드 지적 해소 여부

이 이름(final-consistency)의 이전 라운드는 없다. 대상 문서에 영향을 주는 다른 리뷰의 지적이 반영됐는지 확인했다.

| 출처 | 지적 | 대상 문서 반영 |
|---|---|---|
| examples-round-3 M1 | NaN 이 허용 오차 검사를 통과 | **해소** — D13(:140)에 `!(raw >= -tol && raw <= 1 + tol)` 형태와 문자열 `"NaN"` 사례, feature_list FIX-F1T acceptance 에 `"NaN" → Unavailable` |
| examples-round-3 M2 | 원래 값 로그 소유 항목 없음 | **해소** — D13·FIX-F1T acceptance "원래 값의 로그 기록은 FIX-F3 이후" |
| ai-workflow-round-3 C1 | 클래스 단위 skip 검사가 항목 간 교착 | **해소** — 메서드 단위 `--tests '*X.r7_*'` 로 바뀜, [실행] 으로 XML 이 해당 메서드만 담는 것 확인 |
| ai-workflow-round-3 M4 | ARCH-B2 의 SIG-1 의존 | **부분** — feature_list·ai-workflow·architecture-proposal 은 반영, **decisions-needed D1(:56)은 미반영** (M2) |
| architecture-proposal-round-3 M1 | A′ 경계 규칙 수 2개 | **해소** — D1 표 A′ 단점 "(a)+(b)", B 장점 "1개(R2)" 가 architecture-proposal §4.1-1 과 일치 |

## 검증한 주장 (문제 없음 확인)

- **ArchUnit**: `./gradlew build` 통과 [실행]. 테스트 8개(R1~R3, R4·R4b, R5~R7)가 ai-workflow.md:18 "ArchUnit 8개" 와 일치한다. @Disabled 6개의 해제 id(R1·R3→ARCH-A1, R5→FIX-R5, R6→FIX-F4, R7→FIX-F1, R2→ARCH-B2·B3)는 current-state §6.1 표 및 feature_list 각 항목의 verify `! grep '"Rn 위반'` 패턴과 정확히 대응한다(`@Disabled("R7 위반 …` 이 `"R7 위반` 에 매칭). 주석의 위반 수(R1 1사이클, R2 19건/2클래스, R3 2건, R5·R6·R7 1건)와 위치(`AnswerService.java:28`, `RestClientConfig.java:38`)는 current-state §6.2 및 실제 소스와 맞는다. R6 의 한계 주석(클래스 레벨·jakarta 미검사)은 current-state §6.3 한계 표와 같다.
- **CLAUDE.md**: 67줄(<200, research §8 [F] 기준). 상단에 "확정 전 초안" 이 있다. `build`·`test --tests 'com.tmi.core.architecture.*'`·`spotlessCheck`(위반으로 실패 = 서술대로) 명령이 동작한다 [실행]. `gradlew` 100644(`git ls-files -s`), 테스트 소스셋 Lombok 부재(build.gradle 에 testCompileOnly 없음), `messages/` 하위 번들 + basename 미설정(`application.properties`), `RestClientConfig` 의 R3 위반, `@Transactional protected loadSentence` self-invocation, `SimilarityException` 4xx→502·5xx→503 상태값, `.gitignore` 의 `out/` 규칙이 모두 사실과 맞는다.
- **decisions-needed.md**: §1 표의 13개 결정이 §2 상세와 1:1 이다. feature_list id(TEST-1, FIX-F1/F1T/F2/F3/F4/R5, SIG-1/2, ARCH-A1/A2/B2/B3, DOC-1/2)가 모두 feature_list.example.json 에 있다. §0 의 순서는 depends_on 과 맞는다(ARCH-A1←FIX-F1). D1 비용(A′ 8파일 0.5일 / B 12파일 1일 / C 23+20)은 architecture-proposal §3.2 와, D1 이 바꾸는 기존 규칙 목록은 architecture-proposal §0 표와 같다. D5 "30파일(29+1)" 은 current-state §7.3 및 spotlessCheck 출력 "29 other files" 와 일치한다. D8 "conventions §8 표 22행" 은 실제로 22행이다. D12 의 check-done.sh 결함(카운터 미초기화, 15회)은 `.claude/hooks/check-done.sh` 원문과 같다. 인용 절(current-state F1~F10·§3.3(f)·§3.4·§4.6·§7.2 P8·§7.3, conventions §0·§1.4·§2.1~2.3·§3.1·§4.3·§6.3·§8, architecture-proposal §3.3·§4.1·§4.3, ai-workflow §0·§1.1·§2.2·§4.6, docs/public 02 §2.2·§3.2·§4.2·§5.1 / 03 §1.4·§1.5·§5.1 / 05 §1.3 / 06 §2.3·§3.3·§3.4·§6.5·§7.2 / 99 §2.3 / 01 §1.2)은 모두 실재한다.
- **research.md**: 다른 문서가 인용하는 절(§0, §3.1, §3.2, §4.2, §5, §5.2, §5.3, §6.1, §6.3, §7, §8, §9)이 모두 실재한다. 등급 [J]/[R]/[F]/[S]/(미검증) 이 거의 모든 주장에 붙어 있다. §9 접근 실패 목록과 [S] 사용(docs.spring.io 는 스니펫만)이 서로 맞는다. §3.2 의 `DelegatingMessageSource`, §4.1 의 OSIV WARN, §6.1 의 `@AutoConfigureMockMvc` 패키지는 [R] 표기대로 이 저장소 실행 근거가 있다.
- **blockers.md**: `gradlew` 권한, `~/.gradle/init.d/mirror.gradle`(Google 미러, build.gradle 무변경), PostgreSQL 16 `/var/tmp/pgdata -p 15432 -k /var/tmp` 기동 명령은 현재 실행 중인 프로세스 인자와 같다. `wip-snapshot.patch` 의 `git apply --check` 통과, stash 에 다른 커밋 파일이 섞여 있다는 경고의 전제(stash@{0} 존재)도 확인했다.
