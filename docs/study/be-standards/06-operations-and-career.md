[← be-standards](./README.md)

# 06. 운영·배포·관측, 그리고 국내 BE 채용 역량

> 조사 기준일 2026-10-11. 근거 등급과 한계는 [README](./README.md#근거를-읽는-법-중요) 참조. 채용 공고는 수시로 바뀐다. 이 문서의 기업별 도구·채용 요구는 모두 [C]다.

## 1. 한 줄 요약

**계측은 Micrometer/Actuator로 공통, 수집 도구는 회사마다 다르다.** 배포는 "조금 내보내고 → 지표를 보고 → 이상하면 되돌린다". 채용 공고의 공통 필수는 **Java/Kotlin + Spring Boot + JPA + RDBMS**, 공통 우대는 **대용량 트래픽, Redis, Kafka, 모니터링, 장애 해결**이다.

## 2. 공개 근거로 본 관례 (근거 등급 표시)

| 관례 | 등급 | 근거 | 링크 |
|---|---|---|---|
| 점진 배포 → 지표 관찰 → 롤백. 위험이 큰 변경은 Canary 또는 Blue-Green | C | 우아한형제들, 토스뱅크(SLASH 24 세션 제목) | 7절 미확인 표 |
| Redis 캐시는 Look-aside. 무효화는 커밋 이후, Redis 장애 시 DB로 우회. 잘 안 변하는 데이터는 로컬 캐시 | C | 토스뱅크, 카카오페이 발췌 | 7절 미확인 표 |
| 서비스 간 이벤트는 Kafka. 유실 방지는 Transactional Outbox, 순서는 같은 키 → 같은 파티션 | C | 우아한형제들, 카카오페이, 토스 발췌 | 7절 미확인 표 |
| 장애 대응: 탐지·전파 → 분류·해결 → 복구·보고 → 비난 없는(blameless) 회고 | C | LINE 2021, 카카오, 토스 발췌 | 7절 미확인 표 |
| 채용 필수: Java 또는 Kotlin, Spring Boot, JPA, RDBMS. 우대: 대용량 트래픽, Redis, Kafka, 모니터링, 장애 해결, MSA, 테스트, CI/CD | C | 토스, 우아한형제들, 카카오뱅크, 당근, 네이버 공고 발췌. 공고는 수시로 바뀐다 | 7절 미확인 표 |
| 인증·인가(Spring Security), 보안 기본기 | D | 공고 우대 사항에 자주 보인다는 인상 (estimated). [07 문서](./07-security-and-db.md) | — |

Spring Boot 4.0.x 공식으로 확인한 운영 기본값 [A]:

| 기본값 | 의미 |
|---|---|
| HTTP로 노출되는 actuator 엔드포인트는 `health` 하나뿐 | 필요한 것만 `include`로 연다 |
| `/actuator/prometheus`로 Prometheus 수집 | `micrometer-registry-prometheus` 필요 |
| liveness·readiness 그룹은 **Kubernetes에서만 자동** | 로컬·VM에서는 `management.endpoint.health.probes.enabled=true` |
| 트레이싱 샘플링 기본 **10%** | 로컬 실습은 `management.tracing.sampling.probability=1.0` |
| 자동 구성된 `RestClient.Builder` 없이 클라이언트를 직접 만들면 **트레이스가 전파되지 않는다** | 이 저장소에 직접 걸린다 (4.2) |
| 트레이싱을 켜면 로그에 traceId·spanId가 기본으로 들어간다 | MDC 기반 |
| graceful shutdown 기본 활성 | 유예 시간은 `spring.lifecycle.timeout-per-shutdown-phase` |
| Boot 4.0 신규 `spring-boot-starter-opentelemetry` | OTel 브리지 + OTLP 레지스트리 포함. Spring 팀이 가장 선호하는 경로 |
| Spring Framework 7에 `@Retryable`, `@ConcurrencyLimit` 내장, **서킷브레이커는 없음** | 서킷브레이커는 Resilience4j |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 메트릭 백엔드 | Prometheus + Grafana + Slack: 우아한형제들 / Datadog: 당근, 무신사 | 앱 코드(Micrometer)는 같다. 학습에는 무료로 재현되는 Prometheus + Grafana |
| APM·분산 추적 | Pinpoint(네이버 출신, 코드 수정 없음, 오버헤드 약 3%, HBase 운영 부담) / Datadog(SaaS) / Micrometer Tracing + OTel(사람인, Spring 선호) | 벤더 중립이라 어디로든 옮길 수 있는 OTel |
| CI/CD | Jenkins: 배민 레거시 / CI + ArgoCD(GitOps): 배민 AI 서빙, 무신사 / GitHub Actions 단독: 국내 근거 없음 | 이 저장소는 GitHub에 있으므로 GitHub Actions가 설정 비용이 가장 낮다 (비용 판단[D]이지 국내 관례라서가 아니다) |
| 배포 전략 | Rolling: 배민 Minor·Patch / Blue-Green: 배민 Major / Canary: 배민, 토스뱅크 | 변경 위험도, 롤백 속도, 관측 성숙도로 고른다 |
| 부하 테스트 | nGrinder(네이버, README 공지가 2022년 문구 그대로, 최신 3.5.9) / k6(Grafana Labs, JS 스크립트, thresholds, CI 통합) | CI 통합과 유지보수 상태를 보면 k6. 국내 대기업의 k6 채택 근거는 없다 |
| 재시도·장애 격리 | Spring 7 `@Retryable` / Resilience4j(배민, 토스) | 단순 재시도는 내장, 서킷브레이커와 상태 메트릭은 Resilience4j |
| 언어 | Java: 카카오(2022), 네이버, 배민 다수 / Kotlin: 카카오페이(신규 대부분), 토스, 당근, 배민 일부(2025) | 공고 대부분이 "Java 또는 Kotlin"이다. 기본기는 Java로, 후반에 Kotlin 실습 |

## 4. 이 프로젝트에 적용

### 4.1 현재 상태

| 항목 | 현재 |
|---|---|
| actuator, micrometer | `build.gradle`에 없다 |
| 헬스체크 | `docs/public/07_배포·운영.md` §4가 커스텀 `GET /api/v1/health`를 정의했지만 미구현 (current-state F9) |
| 배포 | 07 §1: 대상 미정, 로컬 실행만 |
| CI | 없음. `spotless`(enforceCheck=false)와 ArchUnit은 있음 |
| 외부 클라이언트 | `common/config/RestClientConfig`가 `RestClient.builder()` 정적 호출로 만든다. `RestClient.Builder` 빈이 없다 (`spring-boot-starter-restclient` 미포함, 제안 D7) |
| 캐시, 메시지 큐 | 없음 |

### 4.2 바꿀 것

| 순서 | 할 일 | 주의 |
|---|---|---|
| 1 | `spring-boot-starter-actuator` + `micrometer-registry-prometheus`. `management.endpoints.web.exposure.include=health,info,prometheus`, `management.metrics.tags.application=${spring.application.name}` | 07 §4의 커스텀 health는 actuator `HealthIndicator`(DB, Similarity)로 대체할 수 있다. 외부 계약이므로 사람 결정이 필요하다 |
| 2 | GitHub Actions: PR마다 `./gradlew build`(spotless, ArchUnit, 테스트) | 테스트가 DB 없이 통과하거나 Testcontainers로 돌아야 한다 (05 문서) |
| 3 | docker-compose로 Prometheus + Grafana. RED 대시보드, `percentiles-histogram`으로 p95·p99. 비즈니스 메트릭 1~2개(채점 수, 등급 분포) | |
| 4 | OSIV off와 트랜잭션 수정(04 문서) 후 Similarity 호출에 `@Retryable`(대상 예외 지정), 필요하면 Resilience4j 서킷브레이커 | 비멱등 호출은 재시도하지 않는다 |
| 5 | k6로 `POST /api/v1/answers` 부하 테스트. 개선 전후 수치를 PR에 남긴다 | |
| 6 | 문장 조회에 Redis Look-aside(커밋 이후 무효화, TTL jitter) 또는 Caffeine 비교 | |
| 7 | 답안 이력 같은 쓰기 기능에서 outbox 테이블 → Kafka 릴레이, 멱등 컨슈머 | |
| 8 | `spring-boot-starter-opentelemetry` + Jaeger/Tempo. 이때 **Similarity 클라이언트를 자동 구성된 `RestClient.Builder`로 만들어야** 트레이스가 이어진다 | D7(`spring-boot-starter-restclient` 추가)이 선행 조건이다 |

### 4.3 기존 제안과 다른 점

| 항목 | 기존 | 이번 조사 |
|---|---|---|
| `RestClient.Builder` 빈 부재 | current-state §4.6, D7에서 "의존성 추가 여부" 문제로 다룸 | 트레이싱 전파 조건이기도 하다. 관측을 배우려면 D7을 (추가)로 결정할 근거가 하나 더 생긴다 |
| 운영·관측 | 기존 제안 범위 밖 | 위 8단계 로드맵 |
| 장애 회고 | 없음 | `docs/`에 회고 템플릿(타임라인, 영향, 근본 원인, 액션 아이템, blameless)을 두고 실습 중 버그를 기록 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 관측성 3요소 | 메트릭(집계), 트레이스(요청 경로), 로그(사건). Micrometer Observation 하나로 메트릭과 트레이스를 함께 만든다 |
| RED / USE | RED는 요청률·에러율·지연(서비스 관점), USE는 사용률·포화도·에러(자원 관점) |
| p95/p99 | 평균은 느린 꼬리를 숨긴다. 사용자가 체감하는 건 꼬리 지연이다 |
| traceId 전파 | W3C `traceparent` 헤더로 서비스 사이를 잇는다. 샘플링 비율과 전파는 별개다 |
| liveness vs readiness | liveness 실패는 재시작, readiness 실패는 트래픽 제외. 배포 중 요청 유실을 readiness와 graceful shutdown으로 막는다 |
| Rolling / Blue-Green / Canary | 자원 비용, 롤백 속도, 검증 범위가 다르다 |
| Look-aside 캐시 | 읽을 때 캐시 → 없으면 DB → 캐시 저장. 무효화 타이밍과 Cache Stampede(TTL jitter, 락)가 쟁점 |
| Kafka 기본 | 파티션 단위 순서, 컨슈머 그룹과 오프셋, at-least-once라서 컨슈머는 멱등이어야 한다 |
| Transactional Outbox | DB 변경과 이벤트 기록을 한 트랜잭션에 묶고, 별도 릴레이가 발행한다 |
| SLO와 burn rate | 목표(예: 99.9%)와 에러 예산을 얼마나 빨리 쓰는지로 알림을 건다 (무신사 사례, 미확인) |
| 부하 테스트 종류 | 부하, 스트레스, 스파이크, 내구. vUser, TPS, 지연의 관계 |
| JVM·WAS 기초 | GC 로그, 힙·스레드 덤프, Tomcat 스레드 풀과 HikariCP 크기의 관계, Virtual Thread의 장단점 |

## 6. 기억할 것

1. 앱은 **Micrometer로 한 번 계측**하면 백엔드는 바꿀 수 있다.
2. Boot 기본값(health만 노출, 샘플링 10%, probe는 k8s에서만)을 알고 시작한다.
3. "대용량 트래픽 경험"은 작게라도 **수치(부하 테스트 전후 비교)로** 증명한다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://raw.githubusercontent.com/spring-projects/spring-boot/4.0.x/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/endpoints.adoc | Spring | 2025~2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/4.0.x/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/metrics.adoc | Spring | 2025~2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/4.0.x/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/tracing.adoc | Spring | 2025~2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/4.0.x/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/web/graceful-shutdown.adoc | Spring | 2025~2026 | 공식 |
| https://raw.githubusercontent.com/spring-projects/spring-boot/4.0.x/starter/spring-boot-starter-opentelemetry/build.gradle | Spring | 2025~2026 | 공식 소스 |
| https://raw.githubusercontent.com/spring-projects/spring-framework/main/framework-docs/modules/ROOT/pages/core/resilience.adoc | Spring | 2025~2026 | 공식 |
| https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/ | Spring 블로그 | 2025 | 공식 |
| https://raw.githubusercontent.com/pinpoint-apm/pinpoint/master/README.md | pinpoint-apm | 2026 (v3.1.1) | 오픈소스 |
| https://raw.githubusercontent.com/naver/ngrinder/master/README.md | NAVER | 공지 문구 2022 | 기업 오픈소스 |
| https://raw.githubusercontent.com/grafana/k6/master/README.md | Grafana Labs | 2026 | 오픈소스 |

### 미확인 (직접 읽어 볼 후보, 상위 5개)

<details>
<summary>펼치기</summary>

| URL | 발행처 | 연도 | 구분 |
|---|---|---|---|
| https://toss.tech/article/34481 | 토스 (TPS 1만, 캐시) | 2024 (추정) | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/17386/ | 우아한형제들 (Kafka) | 2024 (추정) | 기업 블로그 (미확인) |
| https://tech.kakaopay.com/post/local-caching-in-distributed-systems/ | 카카오페이 (로컬 캐시) | 2025 | 기업 블로그 (미확인) |
| https://engineering.linecorp.com/ko/blog/line-platform-server-outage-process-and-dev-culture | LINE (장애 대응) | 2021 | 기업 블로그 (미확인) |
| https://techblog.woowahan.com/20763/ | 우아한형제들 (점진 배포) | 2025 (추정) | 기업 블로그 (미확인) |

그 밖에 검색으로만 본 글: 우아한 15694·19548·15236·15084·2587·22586·2675, 토스 SLASH 23·24, 토스피드, 카카오페이 2023, 카카오 2022 인턴·재발 방지 계획, 무신사 SLO, 사람인 OTel 2025, 당근·토스·카카오뱅크·원티드 채용 공고, Outsider k6.

</details>
