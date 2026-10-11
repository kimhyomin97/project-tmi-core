[← be-standards](./README.md)

# 01. 프로젝트·패키지 구조와 아키텍처

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

**Gradle 모듈 하나, 루트 아래 기능(도메인) 패키지, 기능 안은 레이어드**로 시작한다. 기능 사이 경계는 문서가 아니라 **테스트(ArchUnit 또는 Spring Modulith)로 강제**한다. 헥사고날과 멀티모듈은 필요가 생길 때 도입한다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 | 링크 |
|---|---|---|---|
| 메인 클래스는 루트 패키지에 두고, 그 아래를 기능 패키지로 나눈다 | A | Spring Boot 공식 문서 (국내 기업 아님) | [Structuring Your Code](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/using/structuring-your-code.adoc) |
| 모듈 사이 의존은 순환 없는 단방향으로 두고, 다른 모듈은 루트 패키지만 쓴다 | A | Spring Modulith 공식 규칙 | [Modulith verification](https://raw.githubusercontent.com/spring-projects/spring-modulith/main/src/docs/antora/modules/ROOT/pages/verification.adoc) |
| 배포 단위는 하나로 두고 내부 모듈 경계만 강제한다 (모듈러 모놀리스) | C | 카카오뱅크 2025, 여기어때(연도 미확인) 글 제목·발췌 | 7절 미확인 표 |
| 큰 서비스는 모놀리스에서 MSA로 **점진** 전환한다. 분리 기준은 트랜잭션 경계와 Bounded Context | C | 배민, 토스뱅크 SLASH23, 오늘의집 2022 발췌 | 7절 미확인 표 |

확인된 공식 사실 두 가지:

- Spring Boot 문서는 "특정 레이아웃을 요구하지 않는다"고 적는다. 예시는 `customer/{Customer, CustomerController, CustomerService, CustomerRepository}`처럼 기능 패키지 안을 **평평하게** 둔다.
- Spring Modulith는 루트 바로 아래 패키지 하나를 모듈 하나로 본다. 다른 모듈은 그 모듈의 **기본 패키지(루트)만** 쓸 수 있고, 하위 패키지는 public 타입이어도 internal로 취급한다.

## 3. 갈리는 지점과 선택 기준

아래 표의 기업 사례는 모두 [C](사례 미확인)다. "이럴 땐 이것"은 그 발췌와 공식 문서에서 끌어낸 판단 [D]이다.

### 3.1 기능 내부 아키텍처: 레이어드 vs 헥사고날

| 선택지 | 누가 | 왜 |
|---|---|---|
| 레이어드 + 필요한 곳에만 인터페이스(DIP) | 카카오페이 홈 (헥사고날을 적용했다가 걷어냄, 2025) | 맥락이 맞지 않으면 Port와 매핑 비용이 부채가 된다 |
| 헥사고날 + 멀티모듈 | 카카오뱅크 메시지허브(2023)·홈(2024), 우아한형제들 예시(2023), 카카오스타일 PDP(2025) | 실행 단위 여러 개가 도메인 하나를 공유하거나, 외부 모델이 도메인을 오염시킬 때 효과가 있다 |

**이럴 땐 이것**: 아래 세 질문에 모두 "아니오"면 레이어드로 간다.
1. 같은 도메인을 쓰는 실행 단위(API, 배치, 컨슈머)가 2개 이상인가?
2. 외부 시스템 모델이 도메인 코드로 새어 드는 문제가 실제로 있는가?
3. Port와 DTO 매핑 비용을 감당할 인원이 있는가?

### 3.2 경계를 강제하는 수단: Gradle 멀티모듈 vs 단일 모듈 + 규칙 테스트

| 선택지 | 누가 | 왜 |
|---|---|---|
| Gradle 멀티모듈 | 우아한형제들(2019, 2023), 카카오뱅크 메시지허브, 카카오페이 여신코어 | 컴파일 단계에서 의존 방향을 막는다. 실행 단위 여러 개가 도메인을 재사용한다 |
| 단일 모듈 + Modulith `verify()` 또는 ArchUnit | 카카오뱅크 수신(2025), 여기어때 | 물리적으로 쪼개지 않고 논리 경계를 만든다. 점진 도입이 쉽다 |

**이럴 땐 이것**: 실행 단위가 하나이고 팀이 작으면 단일 모듈과 테스트를 쓴다. 배치나 컨슈머 같은 두 번째 실행 단위가 생기면 Gradle 모듈로 올린다.

### 3.3 시스템 분해: 모놀리스 / 모듈러 모놀리스 / MSA

| 선택지 | 누가 | 왜 |
|---|---|---|
| MSA | 배민(2022), 토스뱅크(2023), 오늘의집(2022) | 장애 격리, 독립 배포, DB 부하 분산 |
| 모듈러 모놀리스 | 카카오뱅크(2025), 여기어때, 우아한모노리스 세미나(2020) | 동시 배포와 분산 트랜잭션 비용을 피한다 |

**이럴 땐 이것**: 학습 단계에서는 모듈러 모놀리스로 경계부터 지킨다. 장애 격리나 배포 충돌 같은 실제 문제가 생긴 뒤에 분리를 검토한다.

멀티모듈(빌드 구조)과 MSA(배포 구조)는 **서로 다른 축**이다. "멀티모듈이니까 MSA"는 틀린 말이다.

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
| 단일 Gradle 모듈 (`settings.gradle`, 실행 단위 1개) | 멀티모듈을 택한 국내 사례의 이유(실행 단위 여러 개)가 아직 없다 |
| `com.tmi.core` 루트의 `CoreApplication` + 기능 패키지 | Spring 공식 권장과 같다 |
| 기능 내부 레이어드, 헥사고날 미도입 | `docs/public/02_설계.md` §2.2에서 이미 헥사고날을 되돌렸다. 카카오페이 회고와 같은 판단이다 |
| ArchUnit 규칙 R1~R7(+R4b) (`src/test/java/com/tmi/core/architecture/`). 현재 R1·R2·R3·R5·R6·R7이 알려진 위반으로 `@Disabled` | "경계는 테스트로 강제"는 Modulith 공식 규칙과 같은 방향이다 [A] |

### 4.2 바꿀 것

`ModuleArchitectureTest`의 R2 위반 19건은 `answer → sentence.*` 10건과 `answer → infra.similarity.*` 9건이다 (current-state §6). 그래서 R2는 아래 세 행을 **모두** 고쳐야 해제된다.

| 현재 | 문제 | 바꿀 방향 | 해제되는 규칙 (작업 ID) |
|---|---|---|---|
| `common/config/RestClientConfig`가 `infra.similarity.SimilarityClient`(WIP에선 `toss.holdings.infra.TossHoldingsClient`도)를 import | `common`이 기능에 의존한다. `common ↔ infra` 순환 | 클라이언트 빈 설정을 각 소유 모듈로 옮긴다. **D1과 무관하게 먼저** | R1·R3 (`ARCH-A1`) |
| `answer/service/AnswerService`가 `sentence.repository.SentenceRepository`, `sentence.domain.SentenceEntity`, `sentence.exception.SentenceException`을 직접 import | 다른 기능의 내부를 직접 쓴다 | `sentence`의 공개 API(`SentenceReader`, `SentenceDetail`)를 거친다 | R2 일부 (`ARCH-B3`) |
| `answer/dto/AnswerResponse.of(SentenceEntity, …)` | 응답 DTO가 다른 기능의 **엔티티**에 결합 (F5) | `SentenceEntity` 대신 `SentenceDetail`을 받는다 | R2 일부 (`ARCH-B3`) |
| `AnswerService`가 `infra.similarity.SimilarityClient`, `.dto.SimilarityRequest/Response`, `.exception.SimilarityException`을 import | B 기준에서 `infra.similarity` 내부 패키지 접근 | `infra/similarity`를 루트 직속 `similarity` 모듈로 올리고 공개 API(`SimilarityScorer`)만 쓴다 | R2 나머지 (`ARCH-B2`) |

### 4.3 기존 제안(`docs/ai/architecture-proposal.md`)과 비교

| 항목 | 기존 제안 | 이번 조사 | 판단 |
|---|---|---|---|
| 구조 | 추천 B: 기능 모듈 + base package만 공개 API | Spring Modulith 공식 규칙과 같다 [A]. 국내 사례 카카오뱅크·여기어때는 사례 미확인 [C] | **일치**. 근거는 공식 도구 규칙이다 |
| 헥사고날(C) | 비추천 | 카카오페이 회고(미확인 [C]), 3.1 기준 [D] | 일치 |
| Modulith | "확장 가능성"으로 언급 | Modulith 2.0 GA(2025-11-21)가 Boot 4 기준선이고 현재 2.1.1이다 | 이번 조사가 더 적극적이다. `verify()`를 학습용으로 한 번 붙여 보는 것을 권한다 |
| 멀티모듈 승격 기준 | 다루지 않음 | "두 번째 실행 단위가 생기면 승격" | 추가 제안 |

Modulith를 붙일 때 주의할 점: `infra`와 `common`도 루트 직속이라 각각 모듈로 잡힌다. `common`은 공유 모듈로 따로 설정해야 한다. 제안 B의 "common = 공유 커널"과 같은 처리다.

### 4.4 부수 관찰 (빌드 상태 확인 필요)

- `toss/holdings/domain/HoldingsSnapshotEntity`가 import하는 `toss.holdings.dto.HoldingsApiResponse` 파일이 없다.
- `common/config/RestClientConfig`가 import하는 `toss.holdings.infra.TossHoldingsClient` 파일도 없다.
- 둘 다 미커밋 Toss WIP와 관련된 것으로 보인다 (`docs/ai/decisions-needed.md` D9) (estimated).

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 루트 패키지와 컴포넌트 스캔 | `@SpringBootApplication`이 있는 패키지가 빈 스캔과 `@Entity` 탐색의 기준이다. 그래서 메인 클래스를 맨 위에 둔다 |
| 계층형 vs 기능형 패키지 | `controller/`, `service/`로 먼저 나누면 기능 경계가 흐려진다. 기능으로 먼저 나누면 경계와 공개 범위를 정하기 쉽다 |
| DIP | 상위 정책이 하위 구현의 인터페이스를 소유하게 해서 의존 방향을 뒤집는다. 외부 API 연동에 국소 적용하기 좋다 |
| 헥사고날의 비용 | Port, Adapter, 매퍼가 늘어난다. 이점은 도메인을 여러 입출력이 공유할 때 생긴다 |
| 순환 의존 끊기 | 새 객체로 분리, 인터페이스로 역전, 도메인 이벤트로 느슨하게. 세 가지를 말할 수 있어야 한다 |
| Gradle `implementation` vs `api` | `implementation`은 의존을 하위 모듈에 노출하지 않는다. 멀티모듈 경계 유지의 기본이다 |
| 모듈러 모놀리스 | 배포는 하나, 내부 모듈 경계는 MSA처럼 엄격하다. MSA 전 단계로 자주 쓰인다 |
| Bounded Context | 같은 단어가 같은 뜻으로 쓰이는 범위. MSA 분리와 패키지 경계의 기준이 된다 |
| DDD 전술 패턴 | 엔티티 안에 규칙 두기, 값 객체, 다른 애그리거트는 ID로 참조하기. 학습 프로젝트에서는 이 정도면 충분하다 |

## 6. 기억할 것

1. 구조는 **실행 단위 수와 팀 규모**로 고른다. 유행으로 고르지 않는다.
2. 경계 규칙은 테스트로 만들어야 유지된다. 먼저 `common → infra` 순환(R1·R3)을 끊고, 그다음 `answer → sentence·infra` 내부 접근(R2)을 끊는다.
3. 멀티모듈(빌드)과 MSA(배포)는 다른 축이다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://raw.githubusercontent.com/spring-projects/spring-boot/v4.0.2/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/using/structuring-your-code.adoc | Spring | 2026 (v4.0.2) | 공식 문서 |
| https://raw.githubusercontent.com/spring-projects/spring-modulith/main/src/docs/antora/modules/ROOT/pages/fundamentals.adoc | Spring | 2026 | 공식 문서 |
| https://raw.githubusercontent.com/spring-projects/spring-modulith/main/src/docs/antora/modules/ROOT/pages/verification.adoc | Spring | 2026 | 공식 문서 |
| https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/ | Spring 블로그 | 2025 | 공식 |

### 미확인 (직접 읽어 볼 후보, 상위 5개)

<details>
<summary>펼치기</summary>

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://tech.kakaopay.com/post/home-hexagonal-architecture/ | 카카오페이 | 2025 | 기업 블로그 (미확인) |
| https://tech.kakaobank.com/posts/2507-legacy-to-modular-monolith-with-spring-modulith/ | 카카오뱅크 | 2025 | 기업 블로그 (미확인) |
| https://techblog.gccompany.co.kr/msa%EB%8F%84-%EB%AA%A8%EB%86%80%EB%A6%AC%EC%8A%A4%EB%8F%84-%EC%95%84%EB%8B%8C-%EC%A0%9C3%EC%9D%98-%EC%84%A0%ED%83%9D-spring-modulith-5f6d1de6399a | 여기어때 | 연도 미확인 | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/12720/ | 우아한형제들 (멀티모듈) | 2023 | 기업 블로그 (미확인) |
| https://toss.tech/article/slash23-corebanking | 토스 SLASH23 | 2023 | 기업 발표 (미확인) |

그 밖에 검색으로만 본 글: 카카오뱅크 2023·2024 헥사고날 글, 우아한 2019 멀티모듈 글, 카카오스타일 2025, 오늘의집 2022 MSA, 우아한모노리스 2020 영상.

</details>
