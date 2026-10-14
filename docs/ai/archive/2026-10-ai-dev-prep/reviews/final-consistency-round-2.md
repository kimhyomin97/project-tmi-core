# final-consistency 리뷰 — Round 2

> 대상 커밋: `f3703ee` (전용 worktree `scratchpad/wt-final-consistency`, detached HEAD). 이전 라운드 기준 `01898ba` → 이번 `f3703ee` 사이 diff 를 함께 확인했다 (`git diff 01898ba f3703ee`, 17파일).
> 직접 평가 대상: `docs/ai/context/research.md`, `docs/ai/proposals/decisions-needed.md`, `/CLAUDE.md`, `src/test/java/com/tmi/core/architecture/*.java`, `docs/ai/blockers.md`. current-state·architecture-proposal·conventions-proposal·ai-workflow·examples·feature_list.example.json 은 교차 확인용으로 읽었다.
> 검증 방법:
> - [실행] `bash ./gradlew build --no-daemon` → **BUILD SUCCESSFUL** (EXIT=0). 결과 XML: CoreApplicationTests 1/skip 0, Layer 2/skip 0, Convention 3/skip 3, Module 3/skip 3
> - [실행] `bash ./gradlew test --tests 'com.tmi.core.architecture.*'` → 성공, 결과 XML 이 아키텍처 3클래스만 남음 (CoreApplicationTests 미실행 = CLAUDE.md:25 의 "DB 불필요" 주장과 일치)
> - [실행] `--tests '*ModuleArchitectureTest.r2_*'` → XML `tests="1" skipped="1"`. 두 줄로 바뀐 R2 `@Disabled(` 사유에서도 `grep -rn '"R2 위반' src/test` 가 :42 에 매칭됨 (ARCH-B3/A2 verify 동작 유지)
> - [실행] `bash ./gradlew spotlessCheck` → 실패, "2개 + Violations also present in 28 other files" = 30. worktree 에서 `spotlessApply` 후 `git status` → `src/main` 29 + `src/test/.../CoreApplicationTests.java` 1 (아키텍처 테스트 3파일은 위반 0), 확인 후 `git checkout -- .` 로 되돌림. D5·SIG-2·current-state §7.3 의 "30파일(29+1)" 과 일치
> - [실행] `jq -e .` 로 feature_list.example.json 유효성 확인, id 목록 추출 (items 11 + pending 6, `DOC-2` 는 D1 묶음별 1개씩)
> - [정적] FIX-F1T/F1S, SIG-1/SIG-2, Docker, D3·D5 의존을 decisions-needed·feature_list·ai-workflow·architecture-proposal·examples README·current-state 에서 grep 으로 전수 대조. CLAUDE.md 의 인용 절(`docs/public/README.md` §시작하기 :50, 05 §1.3·§5.7·§6.5)과 루트 `README.md` 내용 확인

## 점수

| 기준 | 점수 | 한 줄 사유 |
|---|---|---|
| 근거성 | 4 | research 의 등급 5곳이 실행 근거([R])로 갱신됐고 빌드·spotless·verify 형식이 재현된다. 다만 §3.1 의 "8개 시나리오" 는 인용한 conventions §0 에 없는 숫자다 (m3) |
| 구체성 | 4 | 결정마다 다음 작업 id·추가 의존이 적혀 있고, D9 도 `TOSS-1` 신설로 id 가 생겼다. verify 명령은 그대로 실행된다 |
| 일관성 | 4 | FIX-F1T/F1S 분리, D3·D5·Docker 의존, D5≠(a) 처리가 5개 문서에서 일치한다. 남은 것은 DOC-1 의 간접 Docker 의존(m1)과 표의 누락 칸(m2) 등 경미한 것뿐 |
| 판단 가능성 | 4 | D1·D2·D4 를 읽으면 "함께 정해야 하는 결정" 과 환경 조건이 바로 보인다. D8 만 DOC-1 이 결국 Docker 환경을 기다린다는 점이 드러나지 않는다 (m1) |

문서별 한 줄 판정:

| 문서 | 판정 |
|---|---|
| decisions-needed.md | **승인 가능** — round-1 M1·M2·M3 해소. D8 의 간접 Docker 의존(m1), §1 D3 행의 `ARCH-B2` 누락(m2), D12 의 D13 묶음 누락(m7)만 남음 |
| CLAUDE.md | **승인 가능** — `[제안]` 표기 3건과 05 §5.7/§6.5 차이 명시, README 경로 수정, `--tests` 시 DB 불필요 문구 모두 정확. 명령 5개 동작 확인. 줄 배치 1건(m5) |
| research.md | **승인 가능** — 지적 5곳 모두 실행 근거로 갱신. 시나리오 수 출처(m3)와 URL 규칙 예외(m4)만 남음 |
| ArchUnit 테스트 | **승인 가능** — build 통과, R2 사유에 A′ 경로 추가, palantir 위반 0. 주석 1곳이 사유와 덜 맞음(m6) |
| blockers.md | **승인 가능** — #4 Docker 행과 "현재 위치" 행이 생겼고 사실과 맞는다 (`git stash`·패치 경로, Java 3 + properties 1) |

## 치명 (승인 차단)

없음.

## 주요

없음. round-1 주요 7건이 모두 대상 문서에서 해소됐다 (아래 표). 새로 찾은 결함은 모두 결정 내용을 바꾸지 않는 수준이라 경미로 분류했다.

## 경미

- **[m1] decisions-needed.md:130 (D8), :100 (D6) — DOC-1 이 간접적으로 Docker 환경을 기다린다는 점이 적혀 있지 않다.**
  feature_list 의 `DOC-1` depends_on 은 `["FIX-F3","FIX-F4"]` 이다(feature_list:218-221). `FIX-F4` 는 `requires: ["docker"]` 이고 SIG-1(D3)에도 의존한다(:172-189). 따라서 `DOC-1` 은 D2·D3·D4·D5 가 정해지고 Docker 가 있는 환경에서 F4 가 끝나야 시작할 수 있다. D8 의 "결정하면 바로 시작: `DOC-1`" 에는 이 조건이 없다. D6 은 "FIX-F3·FIX-F4 이후" 와 분할 방법을 적었지만 Docker 는 언급하지 않는다. round-1 M2 에서 D1·D2·D4 에 붙인 "**추가로 필요**" 문구가 D8 에는 빠졌다. 이 클라우드 세션에서는 Docker 가 없으므로 (b) 를 고르면 03·05·06 문서 수정이 모두 F4 를 다른 환경에서 끝낼 때까지 밀린다. D6 이 제안한 `DOC-1` 분할(F3 분 / F4 분)을 D8 쪽에도 적으면 (a)/(b) 를 비교하기 쉬워진다.
- **[m2] decisions-needed.md:29 §1 D3 행 "막고 있는 작업"** — `SIG-1 → FIX-F4, 루프 init.sh` 만 적혀 있고, `ARCH-B2` 가 빠졌다(feature_list:241-245 depends_on 에 `SIG-1`). 추천 구조 B 의 첫 단계가 D3 에 걸린다는 사실은 D1 행(:28)에만 있다. 양쪽 행에 모두 적어야 표만 읽어도 보인다.
- **[m3] research.md:95 §3.1** — "[R] … conventions-proposal.md §0 의 2차 검증에서 … **8개 시나리오**가 의도대로 동작" 이라고 쓴다. 그러나 conventions §0:19 의 2차 행에 나오는 시나리오는 404·503·깨진 JSON 400·415·405 의 5개다. 1차 행(:18)은 7개다. "8개" 는 reviews/conventions-proposal-round-4.md:9 의 재실행에서 나온 숫자다. [R] 등급은 맞으므로, 출처를 그 리뷰로 바꾸거나 숫자를 "5개(2차)" 로 고친다.
- **[m4] research.md:18·:24·:193** — §0 에 "URL 이 생략된 [J] 는 `…/org/springframework/...` 경로" 라는 규칙을 새로 넣었다. 그런데 URL 이 없는 Checkstyle [J](:193, `com.puppycrawl`)는 이 규칙에 맞지 않는다. 또 ArchUnit 행(:24)의 선택은 core `archunit` 으로 고쳤지만 출처 URL 은 여전히 `archunit-junit5/maven-metadata.xml` 이다. 버전 값은 같아서 결론에는 영향이 없다.
- **[m5] CLAUDE.md:26-27** — 새로 넣은 "루트 `README.md` … 폐기된 헥사고날" 불릿이 기존 들여쓰기 줄 `  스키마: psql … init.sql` 앞에 들어갔다. 그래서 스키마 명령이 README 불릿의 연속 줄로 렌더링된다. 스키마 줄을 :23-25 의 PostgreSQL 불릿 아래로 옮긴다.
- **[m6] ModuleArchitectureTest.java:40, ConventionArchitectureTest.java:109-110** — R2 `@Disabled` 사유(:42)는 "ARCH-B3(B) 완료 시 해제, A′ 면 ARCH-A2 에서 규칙 교체" 로 고쳐졌다. 그러나 바로 위 주석 :40 은 아직 "해소 작업: … (ARCH-B2, ARCH-B3 — D1=B 결정 전제)" 다. Convention 쪽 R6 한계 주석은 palantir 줄바꿈 때문에 `(현재 사용처` / `// 0).` 로 나뉘었다. 동작에는 영향이 없다.
- **[m7] decisions-needed.md:136 (D12), ai-workflow.md:219, feature_list FIX-F1S `module`** — D12 의 "D1 에 해당하는 `pending_decision` 묶음을 `items` 로 옮긴다" 에는 `D13=(b)` 묶음이 없다. 이 내용은 D13(:141)과 feature_list `_comment` 에만 있다. ai-workflow §4.2 의 "보류" 행은 depends_on 칸이 `ARCH-A1, FIX-F1T (+B2 는 SIG-1)` 하나라서, `FIX-F1S` 도 ARCH-A1 에 의존하는 것처럼 읽힌다(실제로는 `FIX-F1T` 만). `FIX-F1S` 의 `module: "answer"` 는 acceptance 의 "ARCH-B2/A2 이후면 SimilarityScorer 에 적용" 과 맞지 않는다. B 이후라면 모듈이 `similarity` 가 된다. 에이전트는 module 을 고칠 수 없으므로(ai-workflow:305), H0 에서 정할 사항으로 한 줄 적는다.
- **[m8] feature_list:216 / ai-workflow:217 `DOC-1` title "05/06/99/03 문서 수정"** — conventions §8 표에 루트 `README.md` 행이 추가됐다(round-1 M6 반영). 그런데 DOC-1 제목과 ai-workflow 표에는 이 대상이 빠졌다. acceptance 는 "§8 표의 모든 행" 이라 판정은 맞게 되지만, 제목만 보면 범위를 좁게 읽게 된다.
- **[m9] research.md:141 §5.6** — "원격 호출은 트랜잭션 밖" 이 "(미검증, 일반 실무 원칙)" 으로 남아 있다. 공식 문구가 없다는 점은 그대로 맞다. 다만 이 저장소에서 커넥션 점유 1 → 0 을 실행으로 측정했다(current-state §3.4 [실행]). 등급 체계가 "결정 근거로 쓰지 말 것" 으로 읽히므로, 효과는 [R] 로 확인됐다고 링크를 덧붙인다. D4 의 근거는 current-state 이므로 결정에는 영향이 없다.

## 이전 라운드 지적 해소 여부

| 지적 | 판정 | 근거 |
|---|---|---|
| M1 §0 의 FIX-F1T 에 D13 결정이 숨어 있음 | **해소** | feature_list:51-68 FIX-F1T 는 timeout·빈 응답만, 점수 정책은 `pending_decision["D13=(b)"]` 의 `FIX-F1S`(:341-360, depends_on FIX-F1T). decisions-needed §0:17 "점수 범위 정책은 포함하지 않음 (D13 → FIX-F1S)", §1 D13 행:37·상세:140-141, ai-workflow:24·:210·:219, examples answer-scoring README:41(F1T)·:42(F1S) 가 모두 일치 |
| M2 "결정하면 바로 시작" 에서 빠진 의존 | **대부분 해소** | D1:56 "B2 는 D3(SIG-1)", D4:78 "D3 + Docker(`requires`)", D2:89 "D5 — SIG-2 의존". feature_list depends_on/requires(ARCH-B2 :241-245, FIX-F4 :172-189, FIX-F3 :130-133)와 정확히 맞음. §1 표 :28·:30·:31·:34 에도 반영. 남은 것은 D8 의 DOC-1(m1)과 D3 행(m2) |
| M3 D5≠(a) 일 때 FIX-F3 교착 | **해소** | decisions-needed D5:123, ai-workflow H0:96, feature_list `_comment`:2 세 곳에 "FIX-F3 의 depends_on 에서 SIG-2 를 지운다" |
| M4 research 의 낡은 주장 5건 | **해소** | :24 core `archunit`(build.gradle:40 과 일치), :95 [R](conventions §0), :175 Docker 데몬 없음 [R](blockers #4), :190 Spotless [R](current-state §7.3), :212 check-done.sh 를 교체(ai-workflow §4.6·D12 와 일치). 남은 것은 출처 세부(m3·m4) |
| M5 CLAUDE.md `[제안]` 누락 | **해소** | :60 로그 형식·throw 직전 로그, :61 catch 3개에 `[제안]` 과 05 §6.5/§5.7 차이 명시. read timeout 500 은 사실로 표기 없이 유지 |
| M6 존재하지 않는 README 절 | **해소** | CLAUDE.md:24 → `docs/public/README.md` §시작하기(:50 실재), :26 루트 README 경고. current-state:378 과 conventions §8 에 루트 README 행 추가. conventions 의 "README §1" 표현은 사라짐 |
| M7 blockers 에 Docker 부재 없음 | **해소** | blockers §2 #4. D3(a) 단점:62 이 이 행을 링크하고, ai-workflow:423 은 "#1~#4 — #1~#3 약 16분" 으로 합계를 바로잡음 |
| m1 D9 에 id 없음 | **해소** | D9:112 "H0 에서 `TOSS-1`(depends_on ARCH-B3 또는 ARCH-A2) 신설". architecture-proposal:228 "(D9 결정 후 등록)" 과 충돌하지 않음 |
| m2 architecture-proposal SIG 의존 표기 | **해소** | :221 `— / —` |
| m3 [J] 버전·URL | **부분** | §0:18 에 4.0.8 기준 문구 추가. URL 규칙 예외 남음(m4) |
| m4 R2 사유·A2 메서드 이름 | **해소** | @Disabled :42 에 A′ 경로 추가, ARCH-A2 acceptance:314 에 `r2_` 접두 규칙 추가. 주석 :40 만 남음(m6) |
| m5 blockers WIP 현재 위치·파일 구성 | **해소** | §1 "현재 위치" 행, "Java 3 + `application.properties`" |
| m6 CLAUDE.md `--tests` 시 DB 불필요 | **해소** | :25. [실행] 으로 CoreApplicationTests 미실행 확인 |
| m7 D3(a) Docker 부재 | **해소** | :62 "이 클라우드 세션엔 Docker 데몬이 없어 … (blockers §2 #4)", (b) 장점:63 "유일한 통합 검증 경로" |

## 검증한 주장 (문제 없음 확인)

- **빌드·ArchUnit**: `./gradlew build` 통과 [실행]. `@Disabled` 6개의 사유 문자열 `"R1/R2/R3/R5/R6/R7 위반` 이 모두 grep 으로 잡힌다. R2 는 두 줄로 나뉜 뒤에도 :42 에서 잡힌다. 메서드 단위 필터 XML 이 해당 메서드만 담는 것도 확인했다(R2 `tests="1" skipped="1"`). 아키텍처 테스트 3파일은 palantir 위반 0 이다(`spotlessApply` 후 변경 없음).
- **feature_list ↔ decisions-needed**: §0 의 6개 id(TEST-1, FIX-F1, FIX-F1T, FIX-F2, FIX-R5, ARCH-A1)는 모두 depends_on 이 결정 없는 항목뿐이다(TEST-1/FIX-F1). `requires` 가 비어 있고 verify 가 `--tests` 필터라 DB 없이 돈다. ai-workflow §0:24 의 목록도 같다. D13 "(b) 면 `FIX-F1S` 를 items 로, (a)·(c) 면 acceptance 를 사람이 고쳐" 는 `_comment` 와 일치하고, FIX-F1S acceptance(NaN 판정식, `"NaN"` 문자열, 로그는 F3 이후)는 D13:141·examples README:42 와 같다. JSON 은 유효하다.
- **D5 수치**: "30파일(src/main 29 + 테스트 1)" — 이번 커밋에서 테스트 파일 포맷이 바뀌었지만 [실행] 결과 여전히 29+1 이다 (`CoreApplicationTests.java` 만).
- **D8 "conventions §8 표 23행"**: 루트 README 행이 추가돼 실제로 23행이다(round-1 시점 22행 → +1).
- **CLAUDE.md**: 69줄. `build`·`test`·`test --tests 'com.tmi.core.architecture.*'`·`spotlessCheck`(위반으로 실패 = "기존 코드 위반 있음" 서술대로)·`spotlessApply` 가 서술대로 동작한다 [실행]. 루트 `README.md` 는 실제로 `domain.port.in`/`adapter.out.persistence` 헥사고날 표다. 05 §1.3·§5.7·§6.5 절이 실재한다. `[제안]` 이 붙은 4곳(R2 규칙 :41, OSIV :49, R5 :52, 로그 :60, catch 3개 :61)은 모두 decisions-needed·conventions 의 미확정 제안과 대응한다.
- **research.md**: 갱신한 5곳의 [R] 근거가 각 링크 문서에 실재한다 — conventions §0 2차 실행, blockers §2 #4, current-state §7.3 "Gradle 9.3.0 + JDK 21 ✓", ai-workflow §4.6. build.gradle:40 `com.tngtech.archunit:archunit:1.5.1`, :5 Spotless 8.10.4, :55 palantir 2.102.0 이 §0 표와 같다.
- **blockers.md**: #4 의 `docker info` 오류 문구는 ai-workflow:22 와 같다. §1 "현재 위치"(stash@{0} + 패치)는 round-1 [실행] 결과와 같고, 복원 시점(마지막 커밋·push 직후)이 decisions-needed D9:110 "작업 종료 시 복원" 과 모순되지 않는다.
