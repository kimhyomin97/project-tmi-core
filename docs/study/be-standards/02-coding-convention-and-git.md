[← be-standards](./README.md)

# 02. 코딩 컨벤션·코드 스타일·Git 협업

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

**스타일 숫자(4칸/120자 등)는 회사·팀마다 다르다.** 공통인 것은 숫자가 아니라 **규칙을 포매터·린터·CI로 자동 강제**하는 것, 그리고 **작은 PR, 리뷰어 자동 배정, 코멘트 강도 표시, PR 템플릿, 브랜치에 이슈 번호**다.

## 2. 공개 근거로 본 관례

### 2.1 코드 스타일

| 관례 | 등급 | 근거 |
|---|---|---|
| 규칙은 문서가 아니라 Checkstyle·린터·포매터로 강제한다 | 국내 공통 A + B + C✓ | 네이버 rule-config, 우테코, 카카오페이(2022·2024), LY(2024), 컬리(2020) |
| 스타일 숫자는 회사·팀마다 다르다 | A + B + C✓ | 네이버, 우테코, 컬리(2022) |
| 네이밍: 패키지 소문자, 클래스 UpperCamel, 메서드 lowerCamel, 상수 UPPER_SNAKE. 한국어 발음 표기 금지, 약어는 단어처럼(`HttpApiUrl`) | A + B | 네이버, 우테코(Google 준용) |

### 2.2 Git·PR·리뷰

| 관례 | 등급 | 근거 |
|---|---|---|
| PR은 작게 | 국내 공통 C✓ (+ Google [A]) | 뱅크샐러드(2020), 카카오페이(2022·2026) |
| 리뷰는 이유를 설명하고, 답보다 방향을 주고, 잘한 것은 칭찬한다 | 국내 공통 B + C✓ | 우테코, 카카오(2022), LY(2024), 뱅크샐러드(2020) |
| 코멘트에 강도 표시 (Pn 룰, 라벨) | 국내 공통 C✓ | 뱅크샐러드(2020), 당근(2023), 카카오페이(2026), LY(2026) |
| 리뷰어 자동 배정과 리마인더 | 국내 공통 C✓ | 토스(2024), LY(2024), 컬리(2022), 배민(2021) |
| PR 템플릿으로 배경·변경·테스트·리뷰 포인트를 적는다 | 국내 공통 C✓ | 카카오페이(2022·2026), 뱅크샐러드(2020), 카카오(2022), 배민(2021) |
| 브랜치·PR에 이슈(티켓) 번호를 넣는다 | 국내 공통 C✓ | NHN(2024), 컬리(2022), 배민(2017·2021) |

## 3. 갈리는 지점과 선택 기준

### 3.1 들여쓰기·줄 길이

| 선택지 | 누가 | 왜 |
|---|---|---|
| 하드탭(폭 4), 120자 | 네이버[A], spring-javaformat | 탭 폭을 개인이 조절한다 |
| 스페이스 4칸(줄바꿈 +8), 120자 | 우테코[B], palantir-java-format | IDE 기본값과 같고 포매터로 자동 적용된다 |
| 스페이스 2칸, 100자 (Google 스타일) | 컬리 WMS팀[C✓ 2022] | 검증된 외부 규칙을 그대로 가져와 결정 비용을 줄인다 |

**이럴 땐 이것**: 국내 표준 숫자는 없다. 쓰는 포매터가 자동으로 맞춰 주는 쪽을 고르고, 바꾸지 않는다. 강제하지 않으면 어떻게 되는지는 컬리(2020)가 보여 준다. Checkstyle 설정만 공유하고 강제하지 않자 경고가 1,823개 쌓였다.

### 3.2 import 규칙

| 선택지 | 누가 | 내용 |
|---|---|---|
| 네이버식 | 네이버 Checkstyle[A] | static 와일드카드 허용. `java.`, `javax.`, `org.`, `net.`, `com.` 순 그룹 |
| Google식 | 우테코[B], 컬리 한 팀[C✓], palantir | 와일드카드 금지. static 블록 1개 + 일반 블록 1개 |

**이럴 땐 이것**: 포매터의 정렬 방식과 반드시 같게 둔다. 다르면 검사가 매번 깨진다.

### 3.3 PR 크기와 코멘트 강도: 회사별 숫자

| 회사 | 확인한 숫자·규칙 |
|---|---|
| Google [A] | 100줄 적당, 1000줄은 과대 |
| 뱅크샐러드 iOS (2020) | 상한 1,000줄, 실제로는 200~300줄. 테스트 코드는 제한에서 뺀다. Pn: P1 꼭 반영 ~ P5 사소함 |
| 카카오페이손해보험 (2026) | 1 subtask = 1 PR, 중앙값 158줄. AI 리뷰 코멘트 3단계(꼭/웬만하면/사소) |
| 카카오페이 서버 (2022) | "수백 라인 PR은 의미 있는 리뷰가 어렵다" |
| 당근 서비스코어 (2023) | Pn 룰(P1~P5). 직급·연차에서 오는 부담을 줄이려고 쓴다. 다른 팀 개발자에게도 리뷰를 부탁한다 |
| LY Yahoo!플레이스 (2026) | `[must]` `[want]` `[imo]` `[ask]` `[nits]` 라벨 |

### 3.4 리뷰어 배정 방식

| 회사 | 방식 |
|---|---|
| 토스페이먼츠 서버 (2024) | GitHub Actions로 무작위 배정 + Slack 알림. 리뷰 시간이 하루 내외로 줄었다 |
| LY LINE 클라이언트 (2024) | 1차 승인 뒤 봇이 2차 리뷰어를 무작위 지정 |
| 컬리 WMS팀 (2022) | 특정인 지정 없이 출근자 대상, 2명 승인 후 머지 |
| 배민선물하기 FE (2021) | GitLab webhook + Slack 봇이 진행 중 리뷰 수를 보고 배정, 리마인더(월 13:30, 화~금 10:00). FE 16명 레포 사례. MR 템플릿에 Jira 번호 칸 |

### 3.5 브랜치 전략

| 선택지 | 누가 (원문 확인) | 맞는 상황 |
|---|---|---|
| git-flow | 배민 안드로이드(2017, GitHub flow에서 전환. 3주 주기 앱 출시) | 버전 단위 정기 출시 |
| develop 제거 + release 브랜치 | 화해 백엔드(2024, 주 단위 정기 배포) | 정기 배포는 유지하되 고정 브랜치를 main 하나로 |
| Release Flow | NHN Cloud(2024, 출시일 이름의 release 브랜치) | B2B·정기 출시 |
| master/stage/qa/feature | 컬리 WMS팀(2022) | QA 검수 단계가 따로 있는 조직 |
| main 직행 + 기능 플래그 | 뱅크샐러드 iOS(2020) | 상시 통합. trunk 기반에 가깝다[D] |
| GitHub flow | 우테코 미션[B] | 상시 배포하는 작은 웹 BE |

**이럴 땐 이것**: "배포 방식에 맞춰 고르고, 단계는 필요한 만큼만"(화해·NHN 모두 기존 전략을 줄였다). 1인 웹 BE 학습 프로젝트는 GitHub flow로 간다.

### 3.6 커밋 메시지와 머지 방식

| 계열 | 누가 | 규칙 |
|---|---|---|
| Conventional Commits | 우테코 프리코스[B] | `type(scope): subject`, 소문자 시작, 마침표 없음. `feat`→MINOR, `fix`→PATCH |
| Chris Beams 7규칙 | NHN(2017) | 제목 50자, 명령조, 본문 72자, "무엇을·왜". 한글이면 유연하게 |
| 티켓 번호 우선 | 화해(2024), 배민 안드로이드(2017, 티켓 1개 = 커밋 1개 지향) | 이슈 추적 연결을 중시 |

머지는 회사들이 **브랜치 종류마다** 다르게 정한다. 화해(2024)는 feature→release는 Squash, release·hotfix→main은 Merge commit. NHN(2017·2024)은 Squash와 Rebase를 브랜치별로 나눴다.

**이럴 땐 이것**: 커밋 형식은 한 계열만 쓰고 이슈 번호는 넣는다. 브랜치가 main 하나뿐인 학습 프로젝트는 Squash 하나만 허용한다.

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| Spotless + palantir-java-format 2.102.0 | `build.gradle` | 4칸·+8 줄바꿈이 우테코 변형과 같다 |
| 120자, 4 spaces | `docs/public/05_개발컨벤션.md` §2.1 | 국내 단일 표준은 없으므로 "이미 정했고 포매터가 강제해서" 유지한다 |
| `SubmitAnswerRequest`, `SentenceResponse` 같은 DTO 이름 | 05 §2.3 | 커뮤니티 관례와 같다. 팀 합의로 남긴다 |
| 엔티티 Lombok: `@Getter` + `@NoArgsConstructor(access = PROTECTED)` + 정적 `create()` | `SentenceEntity` | `@Setter`, `@Data`가 없다 |
| 접두사 `IErrorType`, `ETmiErrorType`, `ELogLevel` | `common/exception/` | 네이버·Google 관례와 다르지만 바꾸는 비용(7파일·33회 참조)이 커서 D10의 "유지"를 따른다 |

### 4.2 바꿀 것

| 현재 | 바꿀 방향 |
|---|---|
| `src/test`에 탭과 스페이스가 섞여 있다 | `style: spotlessApply 일괄 적용` 단독 커밋 → 해시를 `.git-blame-ignore-revs`에 → `enforceCheck = true` (D5 승인 후) |
| `.editorconfig`가 없다 | `charset=utf-8`, `end_of_line=lf`, `insert_final_newline=true`, `[*.java] indent_style=space`, `indent_size=4`, `max_line_length=120` |
| `.gitattributes`에 일부 규칙만 있다 | 맨 위에 `* text=auto eol=lf` |
| `lombok.config`가 없다 | `lombok.data.flagUsage=error`, `lombok.setter.flagUsage=warning` (팀 결정 사항) |
| 커밋·브랜치·PR 규칙 문서가 없다 | 4.3을 05에 추가하는 것을 검토 (`docs/public`은 사람 확인 후 수정) |

### 4.3 권장 Git 규칙 (학습 프로젝트용)

| 항목 | 규칙 | 근거 |
|---|---|---|
| 브랜치 | `main` 보호(CI 통과 필수) + `feat/<이슈번호>-<요약>`, `fix/...` | 이슈 번호는 국내 공통 |
| 커밋 | `feat(sentence): 문장 메타 조회 API 추가`. 본문에 무엇을·왜, 꼬리말에 이슈 번호 | 우테코[B], NHN |
| 커밋 단위 | 기능 목록 한 항목. 버그는 실패 테스트 커밋 → 수정 커밋 | CLAUDE.md 규칙, 배민 "티켓 1개 = 커밋 1개" |
| PR 크기 | 기능 하나, 테스트 제외 약 300줄 이하 목표 | 3.3 표의 숫자들 |
| PR 템플릿 | 배경(왜) / 이슈 번호 / 변경 / 테스트 방법 / 한계·트레이드오프 / 리뷰 포인트 | 카카오페이, 뱅크샐러드, 배민 |
| 리뷰 코멘트 | 강도 접두사 하나로 고정: `[P1]`~`[P5]` 또는 `[must]`/`[should]`/`[nit]` | 국내 공통 |
| AI 리뷰 | 사람 리뷰 전 사전 점검. 머지 승인은 사람이 한다 | 카카오페이손해보험(2026), LY(2026) |
| 머지 | Squash and merge만 허용 | 우테코[B], 화해·NHN의 feature 머지 |

### 4.4 기존 제안(`docs/ai/conventions-proposal.md`)과 다른 점

| 항목 | 기존 제안 | 이번 조사 |
|---|---|---|
| 포맷 도구 (§7) | Spotless + palantir, `enforceCheck=false`, D5에서 전환 | **방향 일치**. 강제 전환을 늦출수록 위반이 쌓인다는 사례가 있어 D5를 앞당길 근거가 된다 |
| import 규칙 | 명시 없음 | Google식으로 고정. IntelliJ "Class count to use import with *"를 999로 [D] |
| Checkstyle | 없음 | 선택 2단계. 네이버 XML에서 포매터와 겹치는 모듈(`RegexpSinglelineJava`, `Indentation`, `ImportOrder`, `LineLength`)은 끈다 |
| Git·커밋·PR | 범위 밖 | 4.3 추가 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 포매터 vs Checkstyle vs 정적 분석 | 레이아웃은 포매터, 네이밍은 Checkstyle, 버그 패턴은 SonarQube. 같은 항목을 둘이 검사하면 빌드가 계속 깨진다 |
| 브랜치 전략 선택 | 배포 방식으로 고른다. 화해가 develop을, 배민이 GitHub flow를 버린 이유를 설명할 수 있어야 한다 |
| Squash / Merge / Rebase | 히스토리 모양과 revert, `git bisect`에 주는 영향이 다르다 |
| Conventional Commits와 SemVer | `fix`→PATCH, `feat`→MINOR, `BREAKING CHANGE`→MAJOR |
| 작은 PR | 리뷰 품질과 속도가 PR 크기에 반비례한다 |
| 코멘트 강도 표시 | "꼭 고쳐야 하는지"를 오해하지 않게 한다. 연차 차이에서 오는 부담도 줄인다(당근) |
| Lombok과 JPA | `@Data`의 `equals`·`toString`이 연관관계 순환과 프록시 문제를 일으킨다 |
| Object Calisthenics | 우테코 체크리스트는 실무 규칙이 아니라 설계 훈련용 제약이다 |

## 6. 기억할 것

1. 규칙은 **도구로 강제해야** 지켜진다.
2. 스타일 숫자는 회사마다 다르다. 면접에서는 "어떻게 강제하는가"를 말한다.
3. 커밋 형식은 한 계열만 쓰고, 브랜치·커밋·PR에 이슈 번호를 넣는다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 확인한 내용 |
|---|---|---|---|
| https://github.com/naver/hackday-conventions-java | NAVER | 연도 미상 | Checkstyle 규칙 [A] |
| https://github.com/woowacourse/woowacourse-docs | 우아한테크코스 | 2026 | 스타일 가이드, 리뷰 가이드 [B] |
| https://github.com/woowacourse-precourse/java-racingcar-6 | 우아한테크코스 | 2023 | 커밋 규칙 [B] |
| https://helloworld.kurly.com/blog/squad-b-team-building/ | 컬리 WMS팀 | 2022 | Google 스타일, 브랜치, 출근자 리뷰 |
| https://helloworld.kurly.com/blog/fix-style-with-command/ | 컬리 LDD팀 | 2020 | 강제 없는 공유 → 경고 1,823개 |
| https://tech.kakaopay.com/post/remote-work-code-review/ | 카카오페이 서버 | 2022 | 리뷰 그라운드 룰, PR 템플릿 |
| https://tech.kakaopay.com/post/devsecops_sonarqube/ | 카카오페이 SRE | 2024 | PR Quality Gate |
| https://tech.kakaopay.com/post/kakaopayins-slow-pr-fast-dev/ | 카카오페이손해보험 | 2026 | 작은 PR, 중앙값 158줄, AI 리뷰 3단계 |
| https://tech.kakao.com/posts/498 | 카카오 (신입 온보딩) | 2022 | 리뷰어 5원칙 |
| https://toss.tech/article/25431 | 토스페이먼츠 서버 | 2024 | 리뷰어 무작위 배정 |
| https://techblog.lycorp.co.jp/ko/code-review-culture-of-line-client-developers | LY | 2024 | 랜덤 2차 리뷰어 |
| https://techblog.lycorp.co.jp/ko/using-ast-to-verify-the-code-after-code-linting | LY | 2024 | 린트 강제 |
| https://techblog.lycorp.co.jp/ko/resolving-pr-review-bottlenecks-with-ai-and-transforming-review-culture | LY | 2026 | 코멘트 라벨, AI 리뷰 |
| https://blog.banksalad.com/tech/banksalad-code-review-culture/ | 뱅크샐러드 | 2020 | Pn 룰, 1,000줄 상한 |
| https://about.daangn.com/blog/post/당근마켓-개발자-10문-10답-인프콘-2022/ (본문은 사이트 CMS 공개 API `karrot.cdn.prismic.io`로 확인) | 당근 | 2023 | 서비스코어 Pn 룰, 다른 팀 리뷰 |
| https://woowabros.github.io/experience/2021/02/09/cory.html | 우아한형제들 (구 블로그) | 2021 | MR 템플릿(Jira 칸), 리뷰어 자동 배정 봇, 리마인더 |
| https://woowabros.github.io/experience/2017/10/30/baemin-mobile-git-branch-strategy.html | 우아한형제들 (구 블로그) | 2017 | git-flow, 티켓 번호 |
| https://blog.hwahae.co.kr/all/tech/14184 | 화해 | 2024 | 브랜치·머지 |
| https://meetup.nhncloud.com/posts/377, /106, /122 | NHN | 2024, 2017 | Release Flow, 커밋 7규칙, 머지 방식 |
| https://github.com/conventional-commits/conventionalcommits.org | Conventional Commits | 2019 | 공식 명세 [A] |
| https://github.com/google/eng-practices | Google | 2019~ | small-cls [A] |

<details>
<summary>미확인·삭제한 주장</summary>

| 주장 | 처리 |
|---|---|
| 배민 PR 300줄, 우아한형제들 Pn 룰 (techblog.woowahan.com/7152) | [C?]. 403이고 구 블로그(woowabros.github.io) 목록 231건에도 없다. Pn 룰은 뱅크샐러드·당근 원문으로 대체 |
| 토스 FE Trunk-based | 삭제. 원문이 브랜치 전략을 밝히지 않는다 |

</details>
