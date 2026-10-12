[← be-standards](./README.md)

# 06. 운영·배포·관측, 그리고 국내 BE 채용 역량

> 기준일 2026-10-12. 등급 표기는 [README](./README.md#근거를-읽는-법-중요) 참조.

## 1. 한 줄 요약

**계측은 Micrometer/Actuator로 하고, 수집은 Prometheus + Grafana가 국내에서도 흔하다.** 배포는 "조금 내보내고 → 지표를 보고 → 기준을 넘으면 되돌린다"가 국내 공통이다. 채용 공고(토스·당근, 2026-10 확인)는 **JVM 언어 + Spring + RDBMS를 기본**으로, **Redis·Kafka·대규모 트래픽·장애 대응 경험**을 우대한다.

## 2. 공개 근거로 본 관례

### 2.1 배포·장애 대응

| 관례 | 등급 | 근거 |
|---|---|---|
| 위험한 변경은 Canary(또는 Blue-Green)로 점진 배포하고, 롤백 기준 지표를 미리 정한다 | 국내 공통 C✓ | 토스(2023·2025), 카카오(2024), 카카오페이(2024) |
| 장애 대응: 탐지 → 등급 판정·전파 → 복구 → 보고 → 회고·재발 방지 | 국내 공통 C✓ | LINE(2022), 카카오(2024), 배민(2020) |
| 비난 없는(blameless) 회고 | LINE 사례 C✓ | LINE(2019·2022) |

### 2.2 캐시·메시징

| 관례 | 등급 | 근거 |
|---|---|---|
| Redis 캐시는 Look-aside, 캐시 장애 시 DB로 우회 | 국내 공통 C✓ | 토스(2025), 카카오페이(2025) |
| 캐시 무효화는 커밋 **이후**에 한다 | 토스 사례 C✓ | 토스뱅크(2025) |
| 잘 안 변하는 데이터는 로컬 캐시 + 변경 알림 | 카카오페이 사례 C✓ | 카카오페이(2025) |
| 순서가 필요한 이벤트는 같은 키 → 같은 파티션 | 국내 공통 C✓ | 카카오페이(2024), 토스(2025) |
| 이벤트 유실 방지는 Transactional Outbox | 국내 공통 C✓ | 컬리(2026), 토스(2025) |
| 컨슈머·API는 멱등하게 | 국내 공통 C✓ | 카카오페이(2022·2024), 토스(2025) |

### 2.3 채용 공고가 요구하는 것

| 관례 | 등급 | 근거 |
|---|---|---|
| 필수: JVM 언어(Kotlin/Java) + Spring + RDBMS | 국내 공통 C✓ | 토스 공고(2026-10), 당근 공고(2026-10) |
| 우대: Redis·Kafka, 대규모 트래픽, 장애 해결 경험, 모니터링 지표 개선 | 국내 공통 C✓ | 토스 공고, 당근 공고 |
| MSA 환경에서 여러 서비스와 통신(gRPC 포함), 분산 트랜잭션·동시성 | 국내 공통 C✓ | 토스 공고("MSA 환경"), 당근 공고("당근의 MSA환경", gRPC) |
| 인증·인가(Spring Security), 보안 기본기 | D | 이번에 읽은 공고에는 명시가 없었다. [07 문서](./07-security-and-db.md) |

공고 원문에서 확인한 세부 (공고는 수시로 바뀐다):

| 공고 | 확인한 내용 |
|---|---|
| 토스 Server Developer (Product) | 기술: Kotlin, Spring, MySQL, MongoDB, Hadoop, Redis, Kafka. "고가용성의 확장 가능한 시스템", "대규모 실시간 트래픽", "장애를 경험하고 문제를 해결" |
| 토스페이먼츠 Server Developer (3년 이상) | "Java 기술 생태계(Kotlin/Spring/JPA) 이해, OOP/TDD/Refactoring", Redis·Kafka 경험, RDBMS 이해 우대. 전형에 코딩 테스트 |
| 당근 Backend - 커뮤니티 | "Kotlin, Java 같은 JVM 언어와 Spring", "관계형 DB와 캐시", "모니터링 등 시스템 지표 측정·개선". 기술: Kotlin, Spring, Exposed, gRPC, Kafka, RabbitMQ, MySQL, Redis, Datadog, Grafana |
| 당근 Backend - 당근페이 | Kotlin, Spring Boot, Spring Batch, JPA, gRPC, MySQL, Redis, Kafka. "MSA 환경에서 분산 트랜잭션", 우대 "분산 환경 동시성, 대규모 트래픽" |
| 당근 Backend - 부동산 | TypeScript·Go·Java·Kotlin·Python·Ruby 중 하나로 5년 이상. 팀마다 언어가 다르다 |

Spring Boot 4.0.x 공식으로 확인한 운영 기본값 [A]:

| 기본값 | 의미 |
|---|---|
| HTTP로 노출되는 actuator 엔드포인트는 `health` 하나뿐 | 필요한 것만 `include`로 연다 |
| `/actuator/prometheus`로 Prometheus 수집 | `micrometer-registry-prometheus` 필요 |
| liveness·readiness 그룹은 **Kubernetes에서만 자동** | 로컬은 `management.endpoint.health.probes.enabled=true` |
| 트레이싱 샘플링 기본 **10%** | 로컬 실습은 `management.tracing.sampling.probability=1.0` |
| 자동 구성된 `RestClient.Builder` 없이 클라이언트를 만들면 **트레이스가 전파되지 않는다** | 이 저장소에 직접 걸린다 (4.2) |
| graceful shutdown 기본 활성 | 유예 시간은 `spring.lifecycle.timeout-per-shutdown-phase` |
| Boot 4.0 신규 `spring-boot-starter-opentelemetry` | OTel 브리지 + OTLP 레지스트리 포함 |
| Spring Framework 7에 `@Retryable` 내장, **서킷브레이커는 없음** | 서킷브레이커는 Resilience4j |

## 3. 갈리는 지점과 선택 기준

| 질문 | 선택지와 사례 | 이럴 땐 이것 |
|---|---|---|
| 메트릭 백엔드 | Prometheus + Grafana: 토스(2023), 카카오페이(2024), LY(2025~2026) / Datadog: 컬리(2022), 올리브영(2022) / 둘 다: 당근 커뮤니티 공고(2026) | 앱 코드(Micrometer)는 같다. 학습에는 무료로 재현되는 Prometheus + Grafana |
| APM·분산 추적 | Pinpoint(네이버 오픈소스, 자원 사용 약 3% 증가) / Datadog APM(컬리, 올리브영) / OTel + SigNoz(사람인 2025) / 사내 APM(카카오페이 2023) | 벤더 중립인 OTel |
| CI/CD 도구 | GoCD(토스뱅크 2023) / Jenkins + ArgoCD(카카오페이증권 2024) / AWX(카카오페이 2023) / GitHub Actions(하이퍼커넥트 2021, 당근 공고 2026) | 공통점은 **파이프라인을 코드로 Git에 두는 것**. 이 저장소는 GitHub Actions |
| 배포 전략 | Canary: 토스(소수점 단위 트래픽 2023, 인프라까지 1% 단위 2025), 카카오, 카카오페이증권 / Blue-Green·Rolling: 카카오, 카카오페이증권(옵션) | 어느 쪽이든 롤백 기준 지표와 단계별 롤백 가이드를 배포 계획에 먼저 쓴다(카카오 2024) |
| 장애 보고 일정 | LINE(2022): 1차 보고 1일, 회고 5일 이내, 액션 아이템은 Jira / 카카오(2024): 재발 방지 완료 후 종료 / 배민 SRE(2020): 가설 → 동일 환경 재현 테스트로 근본 원인 범위를 좁힘 | 학습 프로젝트도 회고 템플릿을 둔다 |
| 부하 테스트 | nGrinder: 하이퍼커넥트(2022), 올리브영(2024), 카카오페이 / k6: 카카오페이가 "Groovy가 허들"이라 추가(2024) | 국내에서는 nGrinder가 흔하다. 새로 배운다면 k6, 이름은 둘 다 알아 둔다 |
| 언어 | Java: 카카오 "Java를 가장 많이 사용"(2022), 토스페이먼츠 공고 / Kotlin: 카카오페이 전환(2023), 토스·당근 공고 / 팀마다 다름: 당근(2023 인터뷰, 2026 공고) | 기본기는 Java로, 후반에 Kotlin 실습 |

## 4. 이 프로젝트에 적용

### 4.1 현재 상태

| 항목 | 현재 |
|---|---|
| actuator, micrometer | `build.gradle`에 없다 |
| 헬스체크 | `docs/public/07_배포·운영.md` §4가 `GET /api/v1/health`를 정의했지만 미구현 (F9) |
| 배포 / CI | 대상 미정, 로컬 실행만 / CI 없음 |
| 외부 클라이언트 | `RestClient.builder()` 정적 호출. `RestClient.Builder` 빈이 없다 (D7) |
| 캐시, 메시지 큐 | 없음 |

### 4.2 바꿀 것

| 순서 | 할 일 | 주의 |
|---|---|---|
| 1 | `spring-boot-starter-actuator` + `micrometer-registry-prometheus`. `exposure.include=health,info,prometheus` | 커스텀 health는 `HealthIndicator`로 대체 가능. 외부 계약이므로 사람 결정 필요 |
| 2 | GitHub Actions: PR마다 `./gradlew build` | 테스트가 DB 없이 통과하거나 Testcontainers로 돌아야 한다 (05) |
| 3 | docker-compose로 Prometheus + Grafana. RED 대시보드, p95·p99, 비즈니스 메트릭 1~2개 | |
| 4 | "롤백 기준"을 문서로 정한다: 예) 에러율 1% 초과 또는 p99 2배 초과 시 되돌림 | 카카오 사례 |
| 5 | Similarity 호출에 `@Retryable`(대상 예외 지정), 필요하면 Resilience4j | 비멱등 호출은 재시도하지 않는다 |
| 6 | k6로 `POST /api/v1/answers` 부하 테스트. 전후 수치를 PR에 남긴다 | 공고의 "대규모 트래픽" 경험을 작게라도 수치로 |
| 7 | 문장 조회에 Redis Look-aside(커밋 이후 무효화, 장애 시 DB 우회) 또는 Caffeine 로컬 캐시 비교 | 문장은 잘 안 변하므로 로컬 캐시가 맞을 수 있다 |
| 8 | 답안 이력 저장에서 outbox → Kafka(키는 userId), 멱등 컨슈머 | 공고 우대 항목(Kafka)과 직결 |
| 9 | `spring-boot-starter-opentelemetry` + Jaeger/Tempo. 자동 구성된 `RestClient.Builder` 필요 | D7이 선행 조건 |

### 4.3 기존 제안과 다른 점

| 항목 | 기존 | 이번 조사 |
|---|---|---|
| `RestClient.Builder` 빈 부재 | D7에서 "의존성 추가 여부" 문제 | 트레이싱 전파 조건이기도 하다. D7을 (추가)로 결정할 근거가 하나 더 생긴다 |
| 운영·관측 | 범위 밖 | 위 9단계 |
| 장애 회고 | 없음 | LINE 보고서 형식(요약, 타임라인, 재발 방지·탐지 개선·대응 개선 액션 아이템)을 blameless로 |

## 5. 면접·실무에서 알아야 할 개념

| 개념 | 한두 줄 설명 |
|---|---|
| 관측성 3요소 | 메트릭(집계), 트레이스(요청 경로), 로그(사건). Micrometer Observation 하나로 메트릭과 트레이스를 함께 만든다 |
| RED / USE | RED는 요청률·에러율·지연, USE는 사용률·포화도·에러 |
| p95/p99 | 평균은 느린 꼬리를 숨긴다 |
| traceId 전파 | W3C `traceparent` 헤더. 샘플링 비율과 전파는 별개다 |
| liveness vs readiness | liveness 실패는 재시작, readiness 실패는 트래픽 제외 |
| Rolling / Blue-Green / Canary | 자원 비용, 롤백 속도, 검증 범위가 다르다 |
| Look-aside 캐시 | 읽을 때 캐시 → 없으면 DB → 캐시 저장. 무효화를 커밋 전에 하면 옛 값이 다시 적재된다(토스뱅크) |
| Kafka 기본 | 파티션 단위 순서, 컨슈머 그룹과 오프셋, at-least-once라서 컨슈머는 멱등. 파티션은 줄일 수 없다(카카오페이) |
| Transactional Outbox | DB 변경과 이벤트 기록을 한 트랜잭션에 묶고, 릴레이가 발행한다 |
| SLO와 오류 예산 | 목표(예: 28일 롤링 99.9%)와 남은 오류 예산으로 우선순위를 정한다(LY) |
| gRPC | 공고에 자주 나온다(당근). HTTP/2 + Protocol Buffers, 스키마 우선 계약 |
| JVM·WAS 기초 | GC 로그, 힙·스레드 덤프, Tomcat 스레드 풀과 HikariCP 크기의 관계 |

## 6. 기억할 것

1. 앱은 **Micrometer로 한 번 계측**하면 백엔드는 바꿀 수 있다.
2. 배포는 "조금씩, 지표 보고, 기준 넘으면 롤백". 기준 지표를 먼저 정한다.
3. 공고의 "대용량 트래픽 경험"은 작게라도 **수치(부하 테스트 전후 비교)로** 증명한다.

## 7. 출처

### 확인한 출처 (원문을 열어 읽음)

| URL | 발행처 | 연도 | 확인한 내용 / 방법 |
|---|---|---|---|
| raw.githubusercontent.com/spring-projects/spring-boot/4.0.x/.../actuator/{endpoints,metrics,tracing}.adoc, web/graceful-shutdown.adoc, starter/spring-boot-starter-opentelemetry/build.gradle | Spring | 2025~2026 | 운영 기본값 [A] |
| raw.githubusercontent.com/spring-projects/spring-framework/main/.../core/resilience.adoc, spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/ | Spring | 2025 | [A] |
| pinpoint-apm/pinpoint, naver/ngrinder, grafana/k6 README | 오픈소스 | 2022~2026 | 도구 소개 |
| https://toss.tech/article/34481 | 토스뱅크 | 2025 | Look-aside, 서킷 Open 시 DB 우회, `AFTER_COMMIT` evict, Kafka key userId |
| https://toss.tech/article/slash23-server, /monitoring-traffic, /41149 | 토스, 토스페이먼츠 | 2023·2024·2025 | 카나리, Prometheus |
| https://toss.tech/article/slash23-devops | 토스뱅크 | 2023 | GoCD, Pipeline as Code |
| https://toss.tech/article/payments-legacy-5, /kafka-distribution-3 | 토스페이먼츠, 토스증권 | 2025 | Outbox, 유실·멱등 |
| https://tech.kakao.com/posts/675 | 카카오 "안정성 보고서 4. 품질 관리(배포·롤백 기준)" | 2024 | 롤백 기준 지표, 단계별 롤백 가이드. 페이지가 스크립트 렌더링이라 **본문은 HTML 내장 JSON에서 확인** |
| https://tech.kakao.com/posts/676 | 카카오 "안정성 보고서 5. 장애 대응" | 2024 | 감지 → 전파 → 복구 → 재발 방지 |
| https://tech.kakao.com/2022/04/19/2022-kakao-tech-internship-server/ | 카카오 | 2022 | 서버 기술 스택 |
| https://tech.kakaopay.com/post/ local-caching-in-distributed-systems, ifkakao2024-delayed-transfer, msa-transaction, perftest_zone, sre-re-pylon, kakaopaysec-devops-platform, improve-service-performance, kotlin-migration | 카카오페이(증권 포함) | 2022~2025 | 로컬 캐시, Kafka 키, 멱등, nGrinder·k6, AWX, ArgoCD·배포 옵션, APM, Kotlin 전환 |
| https://engineering.linecorp.com/ko/blog/ line-platform-server-outage-process-and-dev-culture, line-failure-reporting-and-follow-up-process-culture, messaing-hub-troubleshooting | LINE | 2019·2022 | 장애 대응 프로세스, blameless |
| https://techblog.lycorp.co.jp/ko/sli-and-slo-for-improving-reliability-2, /using-sli-slo-for-improving-reliability-1-developing-framework-and-line-status | LY | 2025·2026 | SLO, Prometheus, Grafana |
| https://helloworld.kurly.com/blog/2026-outbox-pattern-and-retry-topic, /datadog-dash-2022/ | 컬리 | 2026·2022 | Outbox, Datadog |
| https://oliveyoung.tech/2022-04-20/Oliveyoung-Order-Payment-Process/, /2024-04-19/pos-modernization/ | 올리브영 | 2022·2024 | Datadog, nGrinder |
| https://hyperconnect.github.io/2021/11/08/github-actions-for-everyone-1.html, /2022/12/13/infra-cost-optimization-with-aws-inferentia.html | 하이퍼커넥트 | 2021·2022 | GitHub Actions, nGrinder |
| https://saramin.github.io/2025-08-01-otel/ | 사람인 | 2025 | OTel + SigNoz |
| https://woowabros.github.io/sre/2020/11/11/sre-troubleshoooting.html | 우아한형제들 SRE | 2020 | 근본 원인 가설·재현 테스트, 재발 방지 |
| https://toss.im/career/job-detail?gh_jid=4071141003, 5028079003 (본문은 페이지가 부르는 `api-public.toss.im/api/v3/ipd-eggnog/career/jobs/{id}` JSON) | 토스, 토스페이먼츠 채용 | 2026-10 | 2.3 표 |
| https://careers.daangn.com/jobs/role/7762062003/, /4511184003/, /5296522003/ | 당근 채용 | 2026-10 | 2.3 표 |
| about.daangn.com 블로그 "3번의 지원, 5배의 성장"(2023), "당근 개발자 10문 10답"(2023) (본문은 사이트 CMS 공개 API) | 당근 | 2023 | 팀마다 다른 백엔드 언어, gRPC |

<details>
<summary>확인하지 못한 출처 [C?]</summary>

| 대상 | 사유 |
|---|---|
| 우아한형제들 techblog.woowahan.com/17386(Kafka), /20763(점진 배포) 등 2021년 이후 글 | Cloudflare 403. 구 블로그 사본은 2021-06까지만 있다. 우아콘 Kafka 발표(2022~2025)는 YouTube 제목만 확인 |
| 당근 Medium 글, "Kafka 뉴비의 마이그레이션"(2024 테크 밋업) | medium 403. 영상은 제목만 확인 |
| 무신사·쿠팡 Medium, coupang.jobs | 403. 대체 사본을 찾지 못함 |
| 네이버·카카오·카카오뱅크 채용 공고 | 이번에는 토스·당근 2곳으로 충분해 시도하지 않음 |

</details>
