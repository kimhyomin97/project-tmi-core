[← be-standards](./README.md)

# 01. 프로젝트·패키지 구조와 아키텍처

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

**Gradle 모듈 하나, 루트 아래 기능(관심사) 패키지, 기능 안은 레이어드**로 시작한다. 기능 사이 경계는 문서가 아니라 **테스트(ArchUnit 또는 Spring Modulith)로 강제**한다. 헥사고날과 멀티모듈은 **같은 도메인을 쓰는 실행 단위가 2개 이상**이 될 때 도입한다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 |
|---|---|---|
| 메인 클래스는 루트 패키지에 두고, 그 아래를 기능 패키지로 나눈다 | A | Spring Boot 공식 문서 |
| 모듈 사이 의존은 순환 없는 단방향으로 두고, 다른 모듈은 루트 패키지만 쓴다 | A | Spring Modulith 공식 규칙 |
| 패키지는 레이어가 아니라 **관심사(기능)로 먼저** 묶는다 | A + 카카오페이 사례 C✓ | Spring Boot 문서, 카카오페이(2025) |
| 경계·사용 규칙은 **테스트/CI로 강제**한다 | 국내 공통 C✓ | 카카오뱅크(2025), 토스(2025) |
| 헥사고날·멀티모듈은 **코어를 여러 실행 단위가 공유할 때** 쓴다 | 국내 공통 C✓ | 카카오페이(2025), 카카오뱅크(2023), LINE(2020), 배민(2019) |
| `common` 모듈에 기능 코드를 쌓지 않는다 ("common의 저주") | 국내 공통 C✓ | 배민(2019), LINE(2020) |
| 배포 단위는 하나로 두고 내부 모듈 경계만 강제한다 (모듈러 모놀리스) | 카카오뱅크 사례 C✓ | 카카오뱅크(2025) |
| 큰 모놀리스는 MSA로 **한 서비스씩 점진** 전환한다 | 국내 공통 C✓ | 토스(2023·2025), 카카오뱅크(2024·2025). 배민은 발표 설명란만 확인(2020) |

확인된 공식 사실 두 가지:

- Spring Boot 문서는 "특정 레이아웃을 요구하지 않는다"고 적는다. 예시는 `customer/{Customer, CustomerController, CustomerService, CustomerRepository}`처럼 기능 패키지 안을 **평평하게** 둔다.
- Spring Modulith는 루트 바로 아래 패키지 하나를 모듈 하나로 본다. 다른 모듈은 그 모듈의 **기본 패키지(루트)만** 쓸 수 있고, 하위 패키지는 public 타입이어도 internal로 취급한다. 카카오뱅크 수신(2025)이 이 규칙(루트 직속 패키지 = 모듈)으로 운영한다.

## 3. 갈리는 지점과 선택 기준

"이럴 땐 이것"은 아래 원문 사례와 공식 문서에서 끌어낸 판단 [D]이다.

### 3.1 기능 내부 아키텍처: 레이어드 vs 헥사고날

| 선택지 | 누가 (원문 확인) | 왜 |
|---|---|---|
| 관심사별 패키지 + 내부 레이어드 | 카카오페이 홈 [C✓ 2025] | 헥사고날을 넣었다가 걷어냄. 지킬 도메인이 애매했고 실행 단위가 API 하나뿐이었다. Port 40개 이상과 DTO 매핑이 기능마다 비용이 됐고, 제거 후 8000줄 이상 줄었다 |
| 헥사고날 + 멀티모듈 | 카카오뱅크 메시지허브 [C✓ 2023] | api, data-api, loader, consumer 4개 서비스가 코어 하나를 공유한다 |
| 헥사고날 + 3개 모듈 | 카카오뱅크 홈 [C✓ 2024] | 계정계 모델이 도메인에 새어 드는 것을 Gradle 의존으로 막는다. 러닝 커브와 보일러플레이트를 단점으로 적었다 |
| 포트와 어댑터 | LINE [C✓ 2020], LY SRE 봇 [C✓ 2026] | 입력이 여럿일 때 코드 공유가 컸다(LINE). 외부 시스템이 많아 격리했다(LY) |

**실제로 갈리는 곳**: "외부 연동이 많으면 헥사고날?"에 대한 답이 회사마다 다르다. LY는 외부 시스템이 많아서 택했고, 카카오페이는 연동이 넓으면 로직이 Port·Adapter로 쏠린다며 비추천했다. 차이는 **보호할 코어 로직이 따로 있느냐**다.

카카오페이가 제시한 헥사고날 적합 조건 3가지: 도메인 모델을 확실히 정의할 수 있다, 외부 의존이 넓지 않다, 코어 모듈을 쓰는 모듈이 2개 이상이다. 아니면 **out Port 부분만 추상화**(DIP 국소 적용)하라고 권한다.

**이럴 땐 이것**: 아래 세 질문에 모두 "아니오"면 레이어드로 간다.
1. 같은 도메인을 쓰는 실행 단위(API, 배치, 컨슈머)가 2개 이상인가?
2. 외부 시스템 모델이 도메인 코드로 새어 드는 문제가 실제로 있고, 보호할 코어 로직이 따로 있는가?
3. Port와 DTO 매핑 비용을 감당할 인원이 있는가?

### 3.2 경계를 강제하는 수단: Gradle 멀티모듈 vs 단일 모듈 + 규칙 테스트

| 선택지 | 누가 (원문 확인) | 왜 |
|---|---|---|
| Gradle 멀티모듈 | 배민 [C✓ 2019], 카카오뱅크 메시지허브 [C✓ 2023], 카카오페이 여신코어 [C✓ 2025], G마켓 [C✓ 2024], LINE [C✓ 2020] | 실행 단위 여러 개가 도메인을 재사용한다. 배민 프론트 서버 개발자 글은 internal API·external API·batch가 같은 도메인을 복사해 쓰던 문제를 멀티모듈로 푼 과정을 적었다 |
| 단일 모듈 + Modulith `verify()` | 카카오뱅크 수신 [C✓ 2025] | 경계가 불확실한 초기라 **쉽게 나누고 적은 비용으로 되돌릴 수 있는** 논리 분리를 택했다 |
| 멀티모듈 → 단일 모듈 회귀 | 카카오페이 홈 [C✓ 2025] | 헥사고날 제거와 함께 합쳤다. 컴포넌트 스캔용 의존과 모듈 간 버전 어긋남이 함께 사라졌다 |

함께 알아둘 것: "common의 저주"는 배민(2019)과 LINE(2020)이 같은 말로 경고했다. 배민 글은 원인을 세 가지로 든다. 기능 코드가 공통 모듈로 계속 들어오고, 공통 모듈이 거의 모든 의존성(web, JPA, Redis)을 끌고 오며, 커넥션 풀·타임아웃 같은 설정까지 공통으로 묶인다. 카카오뱅크는 ArchUnit 규칙을 직접 다 쓰는 공수가 크다고 지적했다 [C✓ 2025].

**이럴 땐 이것**: 실행 단위가 하나이고 경계가 아직 흔들리면 단일 모듈과 테스트를 쓴다. 배치나 컨슈머 같은 두 번째 실행 단위가 같은 도메인을 쓰기 시작하면 Gradle 모듈로 올린다.

### 3.3 시스템 분해: 모놀리스 / 모듈러 모놀리스 / MSA

| 선택지 | 누가 (원문 확인) | 왜 |
|---|---|---|
| MSA로 점진 전환 | 토스뱅크 [C✓ 2023], 토스페이먼츠 [C✓ 2025], 카카오뱅크 [C✓ 2024·2025] | 토스뱅크는 트래픽이 가장 많은 서비스 하나를 먼저 떼어 냈다. 모놀리스에선 특정 서비스만 스케일 아웃할 수 없고 장애가 전 업무로 번진다는 이유다 |
| 한 팀 안은 모듈러 모놀리스 | 카카오뱅크 수신 [C✓ 2025] | 5명이 5개 상품을 각각 MSA로 두면 서버·레포·모니터링이 파편화된다. 한 경계로 묶은 서버 안을 모듈로 나눴다 |

**이럴 땐 이것**: MSA 분리 단위는 팀이 운영을 감당할 수 있는 크기로 잡는다. 한 팀 안의 여러 기능은 모듈러 모놀리스로 경계부터 지킨다. 장애 격리나 개별 스케일 아웃 같은 실제 문제가 생긴 뒤 그 기능 하나부터 분리를 검토한다.

멀티모듈(빌드 구조)과 MSA(배포 구조)는 **서로 다른 축**이다. 카카오뱅크 수신은 "MSA 서버 하나 + 내부 모듈"이고, 메시지허브는 "멀티모듈 하나 → 서비스 4개"다.

## 4. 이 프로젝트에 적용

현재 구조 (`src/main/java/com/tmi/core`):

```
sentence/{controller,service,repository,domain,dto,exception}
answer/{controller,service,domain,dto,exception}
infra/similarity/{SimilarityClient, dto, exception}
toss/holdings/{domain,repository}          ← WIP
common/{config,domain,exception}
```

### 4.1 그대로 유지

| 항목 | 이유 |
|---|---|
| 단일 Gradle 모듈 (`settings.gradle`, 실행 단위 1개) | 멀티모듈을 택한 국내 사례들의 이유(실행 단위 여러 개가 도메인 공유)가 아직 없다 [C✓] |
| `com.tmi.core` 루트의 `CoreApplication` + 기능 패키지 | Spring 공식 권장 [A], 카카오페이의 관심사 단위 패키징과 같다 [C✓] |
| 기능 내부 레이어드, 헥사고날 미도입 | `docs/public/02_설계.md` §2.2에서 이미 헥사고날을 되돌렸다. 카카오페이 홈과 같은 판단이다 [C✓] |
| ArchUnit 규칙 R1~R7(+R4b). 현재 R1·R2·R3·R5·R6·R7이 알려진 위반으로 `@Disabled` | "경계는 테스트로 강제"는 Modulith 공식 규칙 [A], 카카오뱅크·토스 사례 [C✓]와 같은 방향이다 |

### 4.2 바꿀 것

`ModuleArchitectureTest`의 R2 위반 19건은 `answer → sentence.*` 10건과 `answer → infra.similarity.*` 9건이다 (current-state §6). 그래서 R2는 아래 세 행을 **모두** 고쳐야 해제된다.

| 현재 | 문제 | 바꿀 방향 | 해제되는 규칙 (작업 ID) |
|---|---|---|---|
| `common/config/RestClientConfig`가 `infra.similarity.SimilarityClient`(WIP에선 `TossHoldingsClient`도)를 import | `common`이 기능에 의존한다. `common ↔ infra` 순환. 배민·LINE이 경고한 "common의 저주"의 시작이다 | 클라이언트 빈 설정을 각 소유 모듈로 옮긴다. **D1과 무관하게 먼저** | R1·R3 (`ARCH-A1`) |
| `AnswerService`가 `SentenceRepository`, `SentenceEntity`, `SentenceException`을 직접 import | 다른 기능의 내부를 직접 쓴다 | `sentence`의 공개 API(`SentenceReader`, `SentenceDetail`)를 거친다 | R2 일부 (`ARCH-B3`) |
| `AnswerResponse.of(SentenceEntity, …)` | 응답 DTO가 다른 기능의 **엔티티**에 결합 (F5) | `SentenceDetail`을 받는다 | R2 일부 (`ARCH-B3`) |
| `AnswerService`가 `infra.similarity`의 클라이언트·DTO·예외를 import | B 기준에서 `infra.similarity` 내부 패키지 접근 | 루트 직속 `similarity` 모듈로 올리고 공개 API(`SimilarityScorer`)만 쓴다 | R2 나머지 (`ARCH-B2`) |

마지막 행의 근거 보강: 카카오페이 홈은 헥사고날을 걷어낸 뒤에도 **Controller DTO와 연동 DTO 분리만은 유지**했다 [C✓ 2025].

### 4.3 기존 제안(`docs/ai/proposals/architecture-proposal.md`)과 비교

| 항목 | 기존 제안 | 이번 조사 | 판단 |
|---|---|---|---|
| 구조 | 추천 B: 기능 모듈 + base package만 공개 API | Modulith 공식 규칙과 같다 [A]. 카카오뱅크 수신이 같은 방식 [C✓] | **일치** |
| 헥사고날(C) | 비추천 | 카카오페이 적합 조건 3가지 중 하나도 맞지 않는다 [C✓] | 일치 |
| Modulith | "확장 가능성"으로 언급 | Modulith 2.0 GA(2025-11-21)가 Boot 4 기준선. 카카오뱅크가 실무 도입 [C✓] | `verify()`를 학습용으로 한 번 붙여 보길 권한다 |
| 멀티모듈 승격 기준 | 다루지 않음 | "같은 도메인을 쓰는 두 번째 실행 단위가 생기면 승격" (국내 공통) | 추가 제안 |

Modulith를 붙일 때 주의할 점: `infra`와 `common`도 루트 직속이라 각각 모듈로 잡힌다. `common`은 공유 모듈로 따로 설정해야 한다.

### 4.4 부수 관찰 (빌드 상태 확인 필요)

- `HoldingsSnapshotEntity`가 import하는 `toss.holdings.dto.HoldingsApiResponse`, `RestClientConfig`가 import하는 `toss.holdings.infra.TossHoldingsClient` 파일이 없다. 미커밋 Toss WIP(D9)와 관련된 것으로 보인다 [D].

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 루트 패키지와 컴포넌트 스캔 | `@SpringBootApplication`이 있는 패키지가 빈 스캔과 `@Entity` 탐색의 기준이다 |
| 계층형 vs 기능형 패키지 | 기능으로 먼저 나누면 경계와 공개 범위(package-private)를 정하기 쉽다 |
| DIP | 상위 정책이 인터페이스를 소유해 의존 방향을 뒤집는다. 외부 API 연동에 국소 적용하기 좋다 |
| 헥사고날의 비용 | Port, Adapter, 매퍼가 늘어난다. 이점은 도메인을 여러 입출력이 공유할 때 생긴다 |
| 순환 의존 끊기 | 새 객체로 분리, 인터페이스로 역전, 도메인 이벤트로 느슨하게 |
| Gradle `implementation` vs `api` | `implementation`은 의존을 하위 모듈에 노출하지 않는다 |
| 모듈러 모놀리스 | 배포는 하나, 내부 모듈 경계는 MSA처럼 엄격하다 |
| Bounded Context | 같은 단어가 같은 뜻으로 쓰이는 범위. 패키지·모듈·MSA 분리의 기준 |

## 6. 기억할 것

1. 구조는 **실행 단위 수, 보호할 코어 로직, 팀 규모**로 고른다.
2. 경계 규칙은 테스트로 만들어야 유지된다. 먼저 `common → infra` 순환(R1·R3)을 끊는다.
3. 멀티모듈(빌드)과 MSA(배포)는 다른 축이다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음, 2026-10-12)

| URL | 발행처 | 연도 | 확인한 내용 |
|---|---|---|---|
| raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/.../using/structuring-your-code.adoc | Spring | 2026 | 루트 패키지, 기능 패키지 예시 [A] |
| raw.githubusercontent.com/spring-projects/spring-modulith/main/.../fundamentals.adoc, verification.adoc | Spring | 2026 | 모듈 = 루트 직속 패키지, `verify()` [A] |
| https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/ | Spring 블로그 | 2025 | 2.0 GA [A] |
| https://tech.kakaopay.com/post/home-hexagonal-architecture/ | 카카오페이 | 2025 | 헥사고날 제거, 관심사 패키징, 단일 모듈 회귀, 적합 조건 |
| https://tech.kakaopay.com/post/backend-domain-driven-design/ | 카카오페이 | 2025 | Bounded Context = Gradle 서브프로젝트 |
| https://tech.kakaobank.com/posts/2507-legacy-to-modular-monolith-with-spring-modulith/ | 카카오뱅크 | 2025 | 모듈러 모놀리스, `verify()` |
| https://tech.kakaobank.com/posts/2311-hexagonal-architecture-in-messaging-hub/ | 카카오뱅크 | 2023 | 멀티모듈 헥사고날, 서비스 4개 |
| https://tech.kakaobank.com/posts/2411-creating-banking-app-home-screen-for-30-million-traffic/ | 카카오뱅크 | 2024 | 홈 MSA 분리, 헥사고날 3모듈 |
| https://toss.tech/article/slash23-corebanking | 토스뱅크 | 2023 | 점진 MSA, 분리 기준 |
| https://toss.tech/article/payments-legacy-1 | 토스페이먼츠 | 2025 | 레거시 점진 개선, MSA 150개 이상 |
| https://toss.tech/article/msa-enum | 토스 | 2025 | ArchUnit 규칙으로 CI 실패 |
| https://engineering.linecorp.com/ko/blog/port-and-adapter-architecture/ | LINE | 2020 | 포트와 어댑터, 입력 다중화 |
| https://engineering.linecorp.com/ko/blog/mono-repo-multi-project-gradle-plugin/ | LINE | 2020 | 멀티 프로젝트, "common의 저주" |
| https://techblog.lycorp.co.jp/ko/reduce-repetitive-tasks-with-sre-bot | LY | 2026 | 외부 시스템 다수 → 포트와 어댑터 |
| https://dev.gmarket.com/111 | G마켓 | 2024 | 웹 UI와 API가 도메인 모델 공유 |
| https://woowabros.github.io/study/2019/07/01/multi-module.html | 우아한형제들 (구 블로그) | 2019 | 실행 단위 3개가 도메인 공유 → 멀티모듈, common 모듈의 저주 3가지 |
| youtube.com/watch?v=BnS6343GTkY (우아한테크 채널 검색 결과의 설명란) | 우아한형제들 | 2020 | "거대한 모놀리틱 서비스를 마이크로서비스로 이전한 지난 5년간의 여정". 설명란 한 줄만 확인, 영상 내용은 미확인 |

<details>
<summary>확인하지 못한 출처 [C?]</summary>

| URL 또는 글 | 사유 |
|---|---|
| 여기어때 "Spring Modulith" (techblog.gccompany.co.kr) | 사이트 403, 사본 없음 |
| techblog.woowahan.com/12720 (우아한형제들 멀티모듈 2023) | Cloudflare 403. 2019 구 블로그 글로 대체 |
| 카카오스타일 PDP 2025, 오늘의집 2022 MSA | 원문 위치를 찾지 못함 |

</details>
