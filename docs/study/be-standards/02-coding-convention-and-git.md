[← be-standards](./README.md)

# 02. 코딩 컨벤션·코드 스타일·Git 협업

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조. 이 문서는 근거가 가장 단단한 편이다: 네이버·우테코 규칙은 GitHub 원문[A·B]이다.

## 1. 한 줄 요약

국내에 공개된 Java 스타일 규칙(네이버, 우테코)은 **"4칸 폭 + 120자"로 수렴**한다. 핵심은 규칙의 내용보다 **포매터와 CI로 자동 강제**하는 것이다. Git은 짧은 브랜치, 작은 PR, 일관된 커밋 형식이 기본이다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 | 링크 |
|---|---|---|---|
| 들여쓰기 폭 4, 한 줄 120자. 네이버는 하드탭, 우테코는 스페이스 4칸 + 줄바꿈 +8 | A + B | 네이버 GitHub, 우테코 GitHub | [네이버 핵데이](https://github.com/naver/hackday-conventions-java), [우테코 스타일](https://github.com/woowacourse/woowacourse-docs/tree/main/styleguide/java) |
| 네이밍: 패키지 소문자, 클래스 명사 UpperCamel, 메서드 동사 lowerCamel, 상수 UPPER_SNAKE, 테스트는 `Test`로 끝남 | A + B | 네이버, 우테코(Google 준용) | 위와 같음 |
| 한국어 발음 표기 금지, 1글자 이름은 임시 변수만. 약어는 단어처럼(`HttpApiUrl`) | A | 네이버 | [네이버 핵데이](https://github.com/naver/hackday-conventions-java) |
| 컨벤션은 Checkstyle·포매터 설정 파일로 배포해 도구로 강제한다 | A + B | 네이버, 우테코, Spring Boot 자체 빌드 | [네이버 rule-config](https://github.com/naver/hackday-conventions-java/tree/master/rule-config), [Spring Boot JavaConventions](https://github.com/spring-projects/spring-boot/blob/main/buildSrc/src/main/java/org/springframework/boot/build/JavaConventions.java) |
| PR은 작게, 리뷰는 빨리. 우테코는 오후 4시 전 요청이면 당일 리뷰. Google은 100줄 적당, 1000줄 과대 | B + A(해외) | 우테코, Google. 배민 300줄은 [C] | [우테코 리뷰 절차](https://github.com/woowacourse/woowacourse-docs/blob/main/reviewer/review-process.md), [Google small CLs](https://github.com/google/eng-practices/blob/master/review/developer/small-cls.md) |
| 리뷰는 이유와 예시로 설명하고, 상대가 고르게 하고, 잘한 것은 칭찬한다 | B | 우테코. 카카오·카카오페이·LINE은 [C] | [우테코 리뷰 가이드](https://github.com/woowacourse/woowacourse-docs/blob/main/reviewer/review-guide.md) |
| 코멘트에 강도 표시(Pn 룰: P1 꼭 반영 ~ P5 사소함) | C | 뱅크샐러드, 우아한형제들 발췌 | 7절 미확인 표 |
| 리뷰어 무작위 자동 지정과 리마인더 | C | 토스페이먼츠, LINE (둘 다 FE·클라이언트 조직) | 7절 미확인 표 |

## 3. 갈리는 지점과 선택 기준

### 3.1 들여쓰기: 탭 vs 스페이스

| 선택지 | 누가 | 왜 |
|---|---|---|
| 하드탭(폭 4) | 네이버, Spring 계열 spring-javaformat | 탭 폭을 개인이 조절한다 |
| 스페이스 4칸, 줄바꿈 +8 | 우테코, palantir-java-format, IntelliJ 기본 | IDE 기본값과 같고 포매터로 100% 자동 적용된다 |

**이럴 땐 이것**: 쓰는 포매터가 100% 자동으로 맞춰 주는 쪽을 고른다.

### 3.2 import 규칙: 네이버식 vs Google식

| 선택지 | 누가 | 내용 |
|---|---|---|
| 네이버식 | 네이버 Checkstyle | static 와일드카드 허용. `java.`, `javax.`, `org.`, `net.`, `com.` 순으로 그룹을 나누고 빈 줄로 구분 |
| Google식 | 우테코, google-java-format, palantir | 와일드카드 전면 금지. static 블록 1개 + 일반 블록 1개 |

**이럴 땐 이것**: 포매터의 정렬 방식과 반드시 같게 둔다. 다르면 검사가 매번 깨진다.

### 3.3 브랜치 전략

| 선택지 | 누가 (모두 미확인) | 맞는 상황 |
|---|---|---|
| git-flow | 배민 안드로이드(2017) | 버전 단위 정기 배포, 여러 버전 병행 유지 |
| GitHub flow | 화해 블로그 권장, 우테코 미션 | 상시 배포하는 작은 웹 BE |
| develop 제거형 | 화해(2024), NHN Release Flow | 정기 배포는 유지하되 단계를 줄인다 |
| Trunk-based | 토스 FE | 하루 여러 번 배포. 강한 CI가 전제 |

**이럴 땐 이것**: 1인 웹 BE 학습 프로젝트는 GitHub flow로 간다.

### 3.4 커밋 메시지

| 계열 | 누가 | 규칙 |
|---|---|---|
| AngularJS / Conventional Commits | 우테코 프리코스(확인) | `type(scope): subject`, 소문자 시작, 마침표 없음, 줄 100자 이하. `feat`→MINOR, `fix`→PATCH |
| Chris Beams 7규칙 | NHN Meetup 소개(미확인) | type 없음, 대문자 시작, 제목 50자, 본문 72자 |

**이럴 땐 이것**: 둘을 섞으면 대소문자·길이 규칙이 충돌한다. 한 계열만 쓴다. 한글 제목이면 길이 기준만 정하면 된다.

### 3.5 머지 방식

| 선택지 | 누가 | 장점 |
|---|---|---|
| Squash and merge | 우테코(확인), 화해(미확인) | main 커밋 1개 = PR 1개라 revert가 쉽다 |
| Merge commit | git-flow 팀 | 브랜치 맥락 보존 |
| Rebase and merge | 선형 이력 선호 팀 | 선형이면서 개별 커밋 보존 |

**이럴 땐 이것**: 저장소 설정으로 한 가지만 허용한다.

## 4. 이 프로젝트에 적용

### 4.1 그대로 유지

| 항목 | 위치 | 이유 |
|---|---|---|
| Spotless + palantir-java-format 2.102.0 | `build.gradle` | 4칸·+8 줄바꿈이 우테코 변형과 같다 |
| 120자, 4 spaces | `docs/public/05_개발컨벤션.md` §2.1 | 네이버[A]·우테코[B] 공개 규칙과 같다 |
| `SubmitAnswerRequest`, `SentenceResponse` 같은 DTO 이름 | 05 §2.3 | 커뮤니티 관례와 같다. 기업 공식 근거는 없으니 팀 합의로 남긴다 |
| 엔티티 Lombok: `@Getter` + `@NoArgsConstructor(access = PROTECTED)` + 정적 `create()` | `sentence/domain/SentenceEntity` | 커뮤니티 관례와 같다. `@Setter`, `@Data`가 없다 |
| 접두사 `IErrorType`, `ETmiErrorType`, `ELogLevel` | `common/exception/` | 네이버·Google 관례와 다르다. 다만 바꾸는 비용(7파일·33회 참조)에 비해 이득이 작아 제안 §1.4·D10의 "유지" 판단을 따른다 |

### 4.2 바꿀 것

| 현재 | 바꿀 방향 |
|---|---|
| `src/test`에 탭(`CoreApplicationTests`)과 스페이스(`architecture/*Test`)가 섞여 있다 | 기능 변경과 분리한 `style: spotlessApply 일괄 적용` 단독 커밋을 만든다. 그 해시를 `.git-blame-ignore-revs`에 넣고 `enforceCheck = true`로 바꾼다 (D5 승인 후) |
| `.editorconfig`가 없다 | 추가한다: `charset=utf-8`, `end_of_line=lf`, `insert_final_newline=true`, `[*.java] indent_style=space`, `indent_size=4`, `max_line_length=120` |
| `.gitattributes`에 `gradlew`, `*.bat`, `*.sh`, `*.jar` 규칙만 있다 | 맨 위에 `* text=auto eol=lf`를 더한다 |
| `lombok.config`가 없다 | `lombok.data.flagUsage=error`, `lombok.setter.flagUsage=warning`으로 엔티티 규칙을 도구로 막는다 (팀 결정 사항) |
| 커밋·브랜치·PR 규칙 문서가 없다 | 아래 4.3을 05에 추가하는 것을 검토한다 (`docs/public`은 사람 확인 후 수정) |

### 4.3 권장 Git 규칙 (학습 프로젝트용)

| 항목 | 규칙 |
|---|---|
| 브랜치 | `main` 보호(직접 push 금지, CI 통과 필수) + `feat/<이슈번호>-<요약>`, `fix/...`, `refactor/...` |
| 커밋 | `feat(sentence): 문장 메타 조회 API 추가`. type은 `feat/fix/refactor/test/docs/style/chore/build/ci/perf`. 본문에 무엇을·왜, 꼬리말에 이슈 번호 |
| 커밋 단위 | 기능 목록 한 항목. 버그는 실패 테스트 커밋 → 수정 커밋 (CLAUDE.md 규칙과 같음) |
| PR | 기능 하나, 테스트 포함 약 300줄 이하 목표. 템플릿: 배경 / 변경 / 테스트 방법 / 리뷰 포인트 |
| 셀프·AI 리뷰 코멘트 | `[P1]`~`[P5]` 접두사로 강도를 표시 |
| 머지 | Squash and merge만 허용 |

### 4.4 기존 제안(`docs/ai/conventions-proposal.md`)과 다른 점

| 항목 | 기존 제안 | 이번 조사 |
|---|---|---|
| 포맷 도구 (§7) | Spotless + palantir, `enforceCheck=false`, D5에서 전환 | **일치**. 네이버·우테코 공개 규칙과도 맞는다 |
| import 규칙 | 명시 없음 | Google식으로 고정. IntelliJ의 "Class count to use import with *"를 999로 바꾼다 (IDE 동작은 일반 지식) |
| Checkstyle | 없음 | 선택 2단계. 네이버 XML을 가져오되 포매터와 겹치는 모듈(탭 강제 `RegexpSinglelineJava`, `Indentation`, `ImportOrder`, `LineLength`)은 끄고 네이밍·중괄호 규칙만 남긴다 |
| Git·커밋·PR | 범위 밖 | 4.3 추가 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 포매터 vs Checkstyle | 레이아웃(들여쓰기, import, 공백)은 포매터, 의미 규칙(네이밍, 중괄호)은 Checkstyle. 같은 항목을 둘이 검사하면 빌드가 계속 깨진다 |
| git-flow vs GitHub flow vs trunk | 배포 방식(정기 버전인지 상시인지)으로 고른다. develop을 없애는 이유를 설명할 수 있어야 한다 |
| Squash / Merge / Rebase | 히스토리 모양과 revert, cherry-pick, `git bisect`에 주는 영향이 다르다 |
| Conventional Commits와 SemVer | `fix`→PATCH, `feat`→MINOR, `BREAKING CHANGE`→MAJOR. CHANGELOG 자동화에 쓴다 |
| 작은 PR | 리뷰 품질과 속도가 PR 크기에 반비례한다. 수치 근거(Google 100/1000줄)를 기억한다 |
| Lombok과 JPA | `@Data`의 `equals`·`toString`이 연관관계 순환과 프록시 문제를 일으킨다. `protected` 기본 생성자는 JPA 요구를 지키면서 무분별한 생성을 막는다 |
| Object Calisthenics | 우테코 체크리스트(들여쓰기 1단계, else 금지, 원시값 포장 등)는 실무 규칙이 아니라 설계 훈련용 제약이다 |

## 6. 기억할 것

1. 규칙은 **도구로 강제해야** 지켜진다. 문서만 두면 무너진다.
2. 포매터와 검사 도구의 규칙을 **같게** 맞춘다 (특히 import 순서).
3. 커밋 형식은 한 계열만 쓴다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://github.com/naver/hackday-conventions-java | NAVER | 연도 추정 (버전·날짜는 페이지에서 확인 못 함) | 기업 공식 GitHub |
| https://github.com/naver/hackday-conventions-java/blob/master/rule-config/naver-checkstyle-rules.xml | NAVER | 연도 추정 | 기업 공식 GitHub |
| https://github.com/woowacourse/woowacourse-docs/tree/main/styleguide/java | 우아한테크코스 | 2026 (최종 커밋) | 교육 과정 |
| https://github.com/woowacourse/woowacourse-docs/blob/main/reviewer/review-process.md | 우아한테크코스 | 2026 | 교육 과정 |
| https://github.com/woowacourse/woowacourse-docs/blob/main/reviewer/review-guide.md | 우아한테크코스 | 2026 | 교육 과정 |
| https://github.com/woowacourse/woowacourse-docs/blob/main/cleancode/pr_checklist.md | 우아한테크코스 | 2026 | 교육 과정 |
| https://github.com/woowacourse-precourse/java-racingcar-6 | 우아한테크코스 | 2023 | 교육 과정 (커밋 규칙) |
| https://gist.github.com/stephenparish/9941e89d80e2bc58a153 | AngularJS 컨벤션 사본 | 2013 무렵 | 비공식 사본 |
| https://github.com/conventional-commits/conventionalcommits.org/blob/master/content/v1.0.0/index.ko.md | Conventional Commits | 2019 | 공식 명세 |
| https://github.com/google/eng-practices/blob/master/review/developer/small-cls.md | Google | 2019~ | 해외 기업 공식 |
| https://github.com/google/eng-practices/blob/master/review/reviewer/speed.md | Google | 2019~ | 해외 기업 공식 |
| https://github.com/spring-projects/spring-boot/blob/main/buildSrc/src/main/java/org/springframework/boot/build/JavaConventions.java | Spring | 2026 | 공식 소스 |

### 미확인 (직접 읽어 볼 후보, 상위 5개)

<details>
<summary>펼치기</summary>

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://blog.banksalad.com/tech/banksalad-code-review-culture/ | 뱅크샐러드 (Pn 룰) | 2021 무렵 (추정) | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/7152/ | 우아한형제들 | 2022 | 기업 블로그 (미확인) |
| https://blog.hwahae.co.kr/all/tech/14184 | 화해 (브랜치 전략) | 2024 | 기업 블로그 (미확인) |
| https://techblog.lycorp.co.jp/ko/code-review-culture-of-line-client-developers | LY | 2024 무렵 (추정) | 기업 블로그 (미확인) |
| https://tech.kakao.com/2022/03/17/2022-newkrew-onboarding-codereview/ | 카카오 | 2022 | 기업 블로그 (미확인) |

그 밖에 검색으로만 본 글: 우아한 2017 git-flow, 토스 리뷰어 자동 지정, NHN Meetup 커밋 메시지, Popit Lombok 글.

</details>
