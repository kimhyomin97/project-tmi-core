# 배포 설계 (Oracle Cloud 무료 VM + GitHub Actions + AI 운영자)

> 상태: **제안 — 미확정**. §13 의 결정 8건을 정하면 §12 순서대로 반영한다.
> 조사일 2026-10-17. 근거는 [§14 출처](#14-출처) 의 번호. "확인" = 원문 직접 확인, "(추론)" = 확인한 사실에서 끌어낸 판단.
> 범위: TMI Core API + PostgreSQL. 외부 Similarity 서버는 범위 밖 (`SIMILARITY_BASE_URL` 로만 연결).

---

## 0. 한 장 요약

```
 개발자 / AI(Claude Code, 구독)                       GitHub (public 저장소, 0원)
 ───────────────────────────────                     ─────────────────────────────────────────────
  PR 작성 ─────────────────────────────────────────▶  CI: 빌드·테스트(Testcontainers)·ArchUnit  [ubuntu-24.04-arm]
  merge ────────────────────────────────────────────▶  Release: arm64 이미지 → ghcr.io  태그 = git SHA
  AI: `gh workflow run deploy` (Actions: write 만) ──▶  Deploy 워크플로
                                                        ① plan   : 변경 요약·위험 플래그 (비밀값 없음)
  사람: GitHub 앱/웹에서 "Approve and deploy" ──────▶  ② deploy : environment=production (승인 1회 후에만 SSH 키 접근)
                                                              │  SSH (deploy 사용자, sudo 로 deploy.sh 만)
  AI: 결과 확인·배포 기록 작성 ◀──────────────────────  ③ verify : 외부 스모크 → 실패 시 자동 롤백
                                                              ▼
 Oracle Cloud Always Free — A1 VM 1대 (ARM64, 2 OCPU / 12 GB, Ubuntu)
 ┌───────────────────────────────────────────────────────────────────────────────┐
 │  Caddy :80/:443 (자동 HTTPS) ──▶ app-blue  :8080 ┐  (한 번에 하나만 트래픽)   │
 │                                  app-green :8080 ┘                           │
 │  PostgreSQL (내부 네트워크만)   백업 cron → 암호화 → S3 호환 저장소          │
 └───────────────────────────────────────────────────────────────────────────────┘
 VM 밖: 외부 업타임 체크(UptimeRobot) · 백업 생존 신호(Healthchecks.io)
```

- **벤더 중립**: VM + Docker Compose + cloud-init + S3 호환 API 만 쓴다. Oracle 고유 기능(Autonomous DB, OKE, OCI DevOps, Resource Manager)은 쓰지 않는다. 다른 클라우드 VM 으로 옮길 때 바뀌는 것은 VM 생성 방법과 방화벽 규칙뿐이다.
- **0원**: GitHub Actions(public 저장소 표준 러너, ARM 포함)·Environments 승인·GHCR public 이미지는 무료 [G1][G2][G3]. AI 는 GitHub Actions 안이 아니라 **사용자의 Claude Code 세션(구독)** 에서 돌려 API 비용을 만들지 않는다.
- **AI 는 실행까지, 승인은 사람 1회**: AI 토큰은 배포를 *시작*할 수 있지만 *승인*할 수 없다. 운영 서버 접근 키는 승인 뒤에만 열리는 environment secret 에만 있다. 배포 자체는 AI 가 아니라 **결정적 스크립트**가 한다.

---

## 1. 목표와 제약

| # | 요구 | 출처 |
|---|---|---|
| R1 | 비용 0원 (Oracle Always Free 범위 안) | 사용자 |
| R2 | 클라우드 고유 기능에 종속되지 않는다 — VM 구조만 사용 | 사용자 |
| R3 | AI 가 배포 실행까지, 사람 승인 1회 | 사용자 |
| R4 | 테스트는 로컬에서도 돈다 (운영과 같은 compose 로 로컬 기동 가능) | 사용자 |
| R5 | 국내 IT 기업 실무에 가까운 운영 역량을 학습한다 (무중단 배포, 롤백, 백업·복구, 관측, 보안) | 이전 대화, `docs/study/be-standards/06` |
| R6 | Similarity 서버는 범위 밖 | 사용자 |

비목표: 다중 VM·쿠버네티스·오토스케일링·멀티 리전. 1인 서비스 규모에서 비용과 학습 대비 이득이 작다. 필요해지면 §12 이후 단계로 둔다.

---

## 2. 설계를 바꾼 조사 결과 (반드시 알아야 할 것)

| # | 사실 | 설계 영향 | 근거 |
|---|---|---|---|
| K1 | **Oracle A1 무료 한도가 2 OCPU / 12 GB 로 줄었다** (월 1,500 OCPU-시간·9,000 GB-시간). 2026년 블로그 다수가 아직 4/24 로 적는다 | 자원 예산을 12 GB 로 잡는다 (§4.2). 체험 기간 크레딧으로 더 크게 만들면 종료 시 A1 전체가 비활성화되고 30일 뒤 삭제 | [O1][O2] 확인 |
| K2 | 유휴 회수: 7일 동안 CPU p95·네트워크·메모리(A1) 가 **모두** 20% 미만이면 회수 대상 | JVM + PostgreSQL 이 메모리 2.4 GB 이상을 상시 쓰면 조건에서 벗어날 가능성이 크다 (추론). 메모리 사용률을 모니터링한다 | [O1] 확인 |
| K3 | 포트는 **VCN 보안 규칙과 OS 방화벽 둘 다** 열어야 한다. OCI Ubuntu 이미지는 UFW 가 아니라 iptables 기본 규칙(REJECT)이 있다. UFW 를 쓰면 부팅 문제가 날 수 있다 | cloud-init 에서 iptables 규칙을 REJECT 앞에 삽입 + 저장 | [O3][O4] 확인 |
| K4 | Always Free 는 **홈 리전에서만**. A1 은 춘천(South Korea North) 예외 문구가 있다 | 홈 리전은 서울 권장. 가입 시 한 번 정하면 바뀌지 않는다 | [O1] 확인 |
| K5 | GitHub Environments 의 required reviewers 는 Free 플랜에서 **public 저장소만** 된다. 승인 전에는 job 이 environment secret 에 접근할 수 없다 | 승인 게이트 = `production` environment. 저장소를 public 으로 유지해야 한다 | [G2] 확인 |
| K6 | 승인 API(`pending_deployments`)는 "Deployments: write" 권한, 배포 시작(`workflow_dispatch`)은 "Actions: write" 권한이다 | **AI 토큰에는 Actions: write 만** 준다 → AI 는 시작만, 승인은 못 한다 | [G4] 확인 |
| K7 | public 저장소에 self-hosted 러너를 쓰면 안 된다 (누구나 PR 로 러너를 오염시킬 수 있다) | VM 에 러너를 두지 않는다. GitHub 호스티드 러너 → SSH 로 배포 | [G5] 확인 |
| K8 | GHCR 에 처음 push 하면 기본이 **private** 이다 | 첫 push 후 public 으로 바꾼다 → VM 은 자격 증명 없이 pull | [G3] 확인 |
| K9 | Spring Boot 4 는 graceful shutdown 이 기본(유예 30초)이고, Docker Compose `stop_grace_period` 기본은 **10초** 다 | 10초 뒤 SIGKILL 로 유예가 잘린다 → `stop_grace_period: 40s` | [S1][D1] 확인 |
| K10 | Docker 가 게시한 포트(`-p`)는 호스트 방화벽(ufw 등)을 **우회**한다. docker 그룹은 **root 권한과 같다** | Caddy 만 80/443 게시, 나머지는 내부 네트워크. 배포 사용자는 docker 그룹에 넣지 않는다 | [D2][D3] 확인 |
| K11 | Caddy 자동 HTTPS 는 도메인이 필요하다. IP 만 쓰면 자체서명 인증서(브라우저 경고). Let's Encrypt IP 인증서는 6일짜리만 있다 | 도메인 필요 (§13 DP1) | [C1][C2] 확인 |
| K12 | Claude 구독 OAuth 토큰은 개인의 일반 사용 범위에서 쓸 수 있고, 제품·서비스용 자동화나 여러 저장소 공유는 API 키를 쓰라고 한다 | AI 는 사용자의 Claude Code 세션에서 돌린다. CI 안에서 Claude 를 돌리는 것은 선택 사항 (§6.5) | [A1][A2] 확인 |

---

## 3. 전체 흐름 — 한 번의 배포

| 단계 | 누가 | 무엇을 | 결과물 |
|---|---|---|---|
| 1 | AI | 기능 PR → CI 통과 → main merge (현행 규칙) | main 커밋 |
| 2 | GitHub Actions | `release` : main 커밋마다 arm64 이미지 빌드, `ghcr.io/kimhyomin97/tmi-core:<sha>` push | 이미지 + digest |
| 3 | AI (skill `deploy`) | 배포 대상 digest 를 골라 `gh workflow run deploy -f digest=…` | 배포 실행 1건 |
| 4 | `plan` job | 직전 배포 대비 변경 요약과 **위험 플래그**를 실행 요약(Job summary)에 쓴다 (§6.3) | 승인 화면에 보이는 요약 |
| 5 | **사람** | GitHub 앱/웹에서 요약을 보고 **Approve and deploy** (또는 Reject) | 승인 기록 (감사 로그) |
| 6 | `deploy` job | SSH → `sudo /opt/tmi/bin/deploy.sh <digest>` : 대기 슬롯 기동 → 헬스 대기 → 스모크 → Caddy 전환 → drain → 이전 슬롯 정지 | 배포 상태 파일 |
| 7 | `verify` job | 외부에서 공개 URL 스모크 (헬스·출제 API 1회). 실패하면 `rollback.sh` 자동 실행 | 성공/롤백 |
| 8 | AI | 실행 결과·로그를 읽고 `docs/ai/deploys/<날짜>-<sha>.md` 배포 기록 PR | 배포 기록 |

- 사람이 하는 일은 **5단계의 승인 1회**뿐이다. 롤백은 같은 실행 안에서 자동이고, 이미 승인된 배포의 일부로 본다.
- AI 가 배포 서버에 직접 접근하지 않는다. AI 의 행동은 모두 GitHub 에 기록된다 (누가 언제 무엇을 시작했고, 누가 승인했는지).

---

## 4. 런타임 구성 (VM 안)

### 4.1 컨테이너

| 서비스 | 이미지 | 게시 포트 | 역할 |
|---|---|---|---|
| `caddy` | `caddy:2` (digest 고정) | **80, 443 만** | TLS 종단, 리버스 프록시, blue/green 전환 |
| `app-blue` / `app-green` | `ghcr.io/kimhyomin97/tmi-core@sha256:…` | 없음 (내부 네트워크) | API. 한 번에 한 슬롯만 트래픽을 받는다 |
| `postgres` | `postgres:16` (digest 고정, §13 DP2) | 없음 | DB. 데이터는 블록 볼륨의 named volume |
| `backup` (cron 컨테이너 또는 호스트 cron) | 스크립트 | 없음 | 일 1회 `pg_dump` → 암호화 → S3 호환 저장소 |

- 네트워크: `edge`(caddy ↔ app), `data`(app ↔ postgres) 두 개로 나눈다. postgres 는 `edge` 에 붙지 않는다.
- 공통 하드닝: non-root 실행, `security_opt: [no-new-privileges:true]`, `mem_limit`·`cpus` 지정, Docker 소켓 마운트 금지 [D3][D4].
- 로그: Docker `local` 드라이버 또는 `json-file` + `max-size: 10m, max-file: 3`. 기본은 로테이션이 없어 디스크가 찬다 [D5].

### 4.2 자원 예산 (A1 2 OCPU / 12 GB)

| 항목 | 메모리 상한 | 비고 |
|---|---|---|
| app 슬롯 1개 | 1.5 GB (`-XX:MaxRAMPercentage=75`) | 전환 순간에는 2개가 동시에 뜬다 → 3 GB |
| PostgreSQL | 3 GB (`shared_buffers` ≈ 768 MB) | `shm_size: 256m` [P1] |
| Caddy | 0.2 GB | |
| (2단계) Prometheus + Grafana | 1.5 GB | §8.2 — 처음에는 두지 않는다 |
| OS·페이지 캐시·여유 | 나머지 (약 4~5 GB) | |

- 블록 스토리지: 부트 50 GB + 데이터 볼륨 50 GB 로 시작 (무료 합산 200 GB) [O1].
- 회수(K2) 관점: 평시 JVM + PostgreSQL 이 3 GB 안팎을 쓰면 메모리 20%(2.4 GB) 를 넘는다 (추론). 첫 2주는 메모리 사용률을 기록해 확인한다.

### 4.3 앱 헬스 체크

| 경로 | 용도 | 포함하는 것 |
|---|---|---|
| `/livez` | 컨테이너 healthcheck (죽었는지) | 앱 자체만. **DB 를 넣지 않는다** (DB 장애가 앱 재시작 연쇄로 번진다) [S2] |
| `/readyz` | 배포 게이트, Caddy 액티브 헬스체크 | 앱 + DB 연결 |
| `/api/v1/health` | 외부 공개 헬스 (`docs/public/07` §4 에 이미 설계됨) | DB·Similarity 상태 요약 |

- Actuator 의존성을 추가하고 `management.endpoint.health.probes.add-additional-paths=true` 로 메인 포트에 `/livez`·`/readyz` 를 둔다. 노출 엔드포인트는 `health` 만 [S2].
- Caddy 는 `/actuator/**` 를 외부에서 막는다.

### 4.4 blue/green 전환 (`deploy.sh` 의 순서)

```
1. 현재 활성 슬롯 확인 (상태 파일 /opt/tmi/state/active = blue|green)
2. 대기 슬롯 이미지를 새 digest 로 바꾸고 기동: docker compose up -d --wait --wait-timeout 180 app-green
3. 스모크: 내부에서 /readyz + 출제 API 1회 (실패 → 대기 슬롯 정지, 종료 코드 1, 활성 슬롯은 그대로)
4. Caddyfile upstream 을 green 으로 바꾸고 caddy reload (무중단, 설정이 틀리면 기존 설정 유지) [C3]
5. drain 10초 대기 (진행 중 요청 처리)
6. 이전 슬롯 정지 (stop_grace_period 40s → Spring graceful shutdown 30s 보장)
7. 상태 파일 갱신: active=green, previous=<이전 digest>, current=<새 digest>, 시각, 실행 URL
```

- **롤백** (`rollback.sh`): 이전 슬롯을 `previous` digest 로 다시 띄우고(정지만 했으므로 빠르다) 2~7 을 반대로 한다.
- 비율 기반 카나리는 하지 않는다. 인스턴스가 1개라 의미가 작다. 대신 "새 슬롯에서 스모크 통과 후 전환 → 외부 스모크 → 실패 시 즉시 원복" 으로 토스·LINE 의 "작게 검증 후 확대, 이상 시 즉시 원복" 원칙을 축소 적용한다 [KR1][KR2].

---

## 5. 빌드와 이미지

| 항목 | 설계 | 근거 |
|---|---|---|
| 이미지 빌드 방식 | **멀티스테이지 Dockerfile + `jarmode=tools extract --layers`** (레이어 분리). 베이스는 Java 21 JRE arm64 (Eclipse Temurin 또는 Liberica, digest 고정) | Spring 공식 예시 [S3]. 내부 구조를 직접 다뤄 학습 가치가 크다. 대안 `bootBuildImage` 는 §13 DP7 |
| 실행 사용자 | `USER 10001` (non-root). 공식 예시에는 USER 가 없어 직접 추가 | [S3][D4] |
| JVM 메모리 | `-XX:MaxRAMPercentage=75` 명시 (기본 25% 는 너무 작다) | [J1] |
| 빌드 러너 | `ubuntu-24.04-arm` 에서 `linux/arm64` **네이티브** 빌드. QEMU 불필요, 멀티 아키텍처 manifest 불필요 (대상이 ARM VM 하나) | [G1] |
| 태그 | `ghcr.io/kimhyomin97/tmi-core:<git sha 12자리>`. `latest` 쓰지 않음. 배포는 **digest(`@sha256:…`)** 로 고정 | [D6] |
| 보존 | GHCR: 최근 20개 + 운영 배포된 것. VM: 최근 3개 이미지 유지 후 prune | 공식 권장 수치 없음 — 자체 규칙 |
| 액션 고정 | 서드파티 액션은 **전체 commit SHA 로 고정** | [G5] |
| CI 테스트 | GitHub 러너에는 Docker 가 있으므로 **Testcontainers 로 PostgreSQL 통합 테스트**를 돌린다 → 결정 D3(a)가 CI 에서 바로 성립 | `docs/ai/proposals/decisions-needed.md` D3 |

---

## 6. AI 의 역할과 경계 (핵심)

### 6.1 원칙

1. **결정은 결정적으로, 해석은 AI 로.** 배포·롤백·백업은 스크립트가 한다. AI 는 스크립트를 *작성*하고(PR), *실행을 요청*하고, 결과를 *해석*한다. 카카오 KRIS 가 같은 이유로 판정 로직과 LLM 을 분리했다 ("같은 코드 판정이 매번 다르면 아무도 믿지 않는다") [A3]. Anthropic·OWASP 도 고위험 동작에는 사람 승인과 하위 시스템의 강제를 권한다 [A4][A5].
2. **AI 는 운영 비밀값을 갖지 않는다.** SSH 키·DB 비밀번호는 `production` environment secret 과 VM 에만 있다.
3. **AI 는 시작만, 승인은 사람만.** 토큰 권한으로 강제한다 (K6). 프롬프트로 "승인하지 마" 라고 하는 것은 통제가 아니다.
4. **모든 AI 행동은 기록된다.** GitHub 실행 이력·environment 배포 이력·`docs/ai/deploys/` 기록.

### 6.2 역할 표

| AI 역할 | 하는 일 | 권한 | 하지 않는 일 |
|---|---|---|---|
| 인프라 작성자 | Dockerfile·compose·Caddyfile·워크플로·스크립트·cloud-init 을 PR 로 작성 | 저장소 쓰기 (현행) | 인프라 파일 PR 을 **스스로 merge 하지 않는다** (§6.4) |
| 배포 운영자 (skill `deploy`) | 대상 digest 선택 → 배포 실행 요청 → 승인 대기 상태 확인 → 결과 확인 → 배포 기록 | fine-grained PAT: **Actions: write, Contents: read, Metadata: read** | 승인, SSH 접속, VM 명령 실행 |
| 배포 리뷰어 | `plan` job 이 만든 위험 플래그를 읽고 사람에게 위험도 설명 (승인 판단 보조) | 읽기 | 위험 판정 자체 (판정은 스크립트) |
| 장애 분석가 | 실패한 배포의 로그·`diagnose` 결과를 읽고 원인과 수정 PR 제안 | 읽기 + `diagnose` 워크플로 실행 | 운영 서버 직접 수정 |
| 복구 리허설 담당 | 월 1회 백업 복원 리허설 워크플로 실행, 결과 기록 | Actions: write | 운영 DB 덮어쓰기 |

### 6.3 `plan` job 의 위험 플래그 (결정적 규칙)

직전 배포 digest 의 커밋과 이번 커밋 사이 diff 로 계산한다. 승인 화면(Job summary)에 표로 나온다.

| 플래그 | 규칙 | 의미 |
|---|---|---|
| `SCHEMA` | `docs/public/init.sql`, `db/changes/**`, `**/*Entity.java` 변경 | DB 변경 동반 — §7.3 확인 필요 |
| `INFRA` | `deploy/**`, `Dockerfile`, `.github/workflows/**` 변경 | 배포 방식 자체가 바뀜 — 가장 주의 |
| `CONFIG` | `application*.properties` 의 키 추가·삭제 | VM 의 환경 변수·시크릿 갱신 필요 여부 |
| `DEPS` | `build.gradle` 의존성 변경 | |
| `API` | `*Controller.java`, `*Api.java`, `docs/public/03_API명세.md` 변경 | 외부 계약 변경 가능성 |
| `ARCHUNIT` | `com.tmi.core.architecture` 의 `@Disabled` 개수 증가 | 구조 규칙을 끈 변경 |

- AI 는 이 표를 받아 "무엇이 바뀌었고, 무엇을 보고 승인하면 되는지" 3줄로 요약해 실행에 코멘트한다 (skill `deploy`). 판정은 표가 하고, AI 는 설명만 한다.

### 6.4 AI 가 배포 통제를 우회하지 못하게 하는 장치

| 위험 | 막는 장치 |
|---|---|
| AI 가 승인 API 를 호출 | AI 토큰에 Deployments: write 없음 (K6) |
| AI 가 워크플로를 고쳐 승인 단계를 빼고 merge | SSH 키가 **environment secret 에만** 있으므로 승인 없는 job 은 VM 에 접근할 수 없다. 저장소 수준 secret 에는 배포 키를 두지 않는다 (K5) |
| AI 가 `deploy.sh` 를 악의 없이 잘못 고침 | 인프라 파일 PR 은 **사람 리뷰 후 merge** (현행 "AI 가 merge" 규칙의 예외로 CLAUDE.md 에 추가). 그리고 `plan` 의 `INFRA` 플래그로 승인 화면에 드러난다 |
| AI 세션에 사람의 `gh` 로그인이 그대로 있음 | skill `deploy` 는 전용 PAT(`GH_TOKEN_DEPLOY_AGENT`)만 쓴다. 사람 토큰을 쓰는 세션에서는 배포 skill 이 시작을 거부한다 (§12 DEP-6 에서 구현) |
| 프롬프트 인젝션 (PR 본문·로그에 숨긴 지시) | AI 의 권한이 "시작·읽기" 뿐이라 최악의 결과는 "불필요한 배포 요청" 이고, 그것도 사람 승인에서 멈춘다 |

### 6.5 CI 안에서의 Claude (선택, 2단계)

- `anthropics/claude-code-action` 으로 인프라 PR 리뷰를 자동화할 수 있다. 인증은 구독 OAuth 토큰(`claude setup-token`, 1년 유효)이면 추가 비용이 없다 [A1].
- 다만 약관상 개인의 일반 사용 범위여야 하고 [A2], public 저장소라 트리거를 쓰기 권한자로 제한하는 기본값을 유지해야 한다 [A6]. **배포 워크플로와 Claude 워크플로를 분리**해 Claude job 에는 배포 secret 이 보이지 않게 한다.
- 1단계에서는 쓰지 않는다. AI 는 사용자의 Claude Code 세션에서만 돈다 (§13 DP8).

---

## 7. 데이터베이스 운영

### 7.1 초기화

- 데이터 볼륨이 비어 있을 때만 `docs/public/init.sql` 을 `/docker-entrypoint-initdb.d/` 로 적용한다 (공식 이미지 동작) [P1].
- PostgreSQL 16 은 데이터 볼륨을 `/var/lib/postgresql/data` 에 마운트해야 한다 (17 이하. 18 부터 경로가 바뀐다) [P1].

### 7.2 백업과 복구

| 단계 | 방식 | RPO | 비고 |
|---|---|---|---|
| 1단계 (처음부터) | 일 1회 `pg_dump -Fc` → `age` 로 암호화 → **S3 호환 저장소** 업로드 | 24시간 | OCI Object Storage 의 S3 호환 API 를 쓰되, 도구(rclone 또는 aws cli)는 S3 표준만 쓴다 → 저장소 교체 가능 [O5]. 무료 20 GB·월 5만 요청 안에서 일 7개 + 주 4개 보관 |
| 생존 신호 | 백업 성공 시 Healthchecks.io 에 ping. ping 이 끊기면 알림 | | 백업이 "조용히 안 도는" 상황을 잡는다 [M1] |
| 복구 리허설 | **월 1회** 별도 컨테이너에 최신 백업을 복원 → 앱을 `ddl-auto=validate` 로 붙여 기동 확인 → 소요 시간 기록 | | "원하는 것은 백업이 아니라 복구" [M2]. AI 가 워크플로로 실행·기록 |
| 2단계 (선택) | pgBackRest + WAL 아카이빙 (PITR) | 분 단위 | 커스텀 PG 이미지·`pg_wal` 적체 감시가 추가된다 [P2] |

- 블록 볼륨 백업(무료 5개)은 VM 통째 복구용 보조 수단으로만 쓴다. 벤더 기능이므로 주 수단으로 삼지 않는다 (R2).
- 암호화 키(age private key)는 VM 과 저장소 밖(사용자 PC·비밀번호 관리자)에 둔다.

### 7.3 스키마 변경 — blue/green 에서 지켜야 할 규칙

- blue/green 전환 중에는 **구 버전과 신 버전이 같은 DB 를 동시에 쓴다.** 그래서 DDL 은 구 버전과 호환되는 *확장*만 먼저 적용하고, 컬럼 삭제·NOT NULL 추가 같은 *축소*는 다음 릴리스로 미룬다 (expand/contract) [P3]. 이 규칙이 있어야 이미지 롤백이 성립한다.
- 현재 팀 결정은 Flyway 미도입(`docs/public/05` §3.7)이고 `ddl-auto=validate` 다. 그대로 가면 DDL 적용 순서·이력을 사람이 관리해야 한다. 최소 장치:
  - `db/changes/NNNN_설명.sql` 순번 파일 + 적용 이력 표(`schema_changes`)
  - 배포 전에 사람이 DDL 을 적용하는 별도 워크플로 `db-change` (승인 필요, `SCHEMA` 플래그가 있으면 `deploy` 전에 실행)
- 이 수작업 비용이 크면 Flyway 도입이 실무 표준에 더 가깝다 → §13 DP3.

---

## 8. 관측과 알림

### 8.1 1단계 (처음부터, 추가 자원 거의 없음)

| 무엇 | 어떻게 | 왜 |
|---|---|---|
| 앱이 살아 있나 | **외부** 업타임 체크 (UptimeRobot 무료, 5분 간격) → `/api/v1/health` | VM 안의 모니터링은 VM·네트워크·Caddy 가 죽으면 같이 죽는다 [M2][M3] |
| 백업이 돌았나 | Healthchecks.io 생존 신호 | §7.2 |
| 로그 | Docker 로그 로테이션 + `docker compose logs` (diagnose 워크플로가 수집) | |
| 자원 | cloud-init 에 `node_exporter` 없이 시작. 대신 `diagnose` 가 `free`, `df`, `docker stats` 스냅샷 수집 | 회수(K2) 대비 메모리 확인 |
| 알림 채널 | 이메일 (UptimeRobot·Healthchecks 기본 기능) | |

### 8.2 2단계 (선택)

- Micrometer + Prometheus(보존 15일) + Grafana 를 같은 VM 에. 메모리 약 1.5 GB [M4]. Grafana 는 127.0.0.1 바인딩 + SSH 터널로만 접근.
- 배포 판단 지표(에러율·p95 지연)를 대시보드로 보고, 3단계에서 `verify` job 이 지표 비교로 롤백을 판단하게 확장할 수 있다.

### 8.3 `diagnose` 워크플로 (AI 의 읽기 전용 눈)

- environment `production-readonly`: 승인 없이 실행, **진단 전용 SSH 키**만 들어 있다.
- VM 의 `authorized_keys` 에서 이 키는 `command="/opt/tmi/bin/diagnose.sh",no-pty,no-port-forwarding` 로 **고정 명령만** 실행된다. 다른 명령은 불가.
- `diagnose.sh` 는 컨테이너 상태, 최근 로그 N줄(비밀값 패턴 마스킹), 자원 스냅샷, 배포 상태 파일을 출력한다 → 실행 artifact 로 저장 → AI 가 읽는다.

---

## 9. 보안

| 영역 | 설계 | 근거 |
|---|---|---|
| 네트워크 (VCN) | 인그레스: 443, 80 (인증서 발급·리다이렉트), 22. 5432 는 열지 않는다 | [O3] |
| 호스트 방화벽 | OCI Ubuntu 의 iptables 에 80/443 을 REJECT 앞에 삽입 후 저장 (cloud-init). UFW 쓰지 않음 | K3 |
| SSH | 키 전용, root 로그인 금지, `AllowUsers deploy`. 22 번은 GitHub 러너 IP 가 고정되지 않아 넓게 열린다 → fail2ban 은 보조 수단 | [D7][G5] / 대안은 §13 DP5 |
| 배포 사용자 | `deploy` 계정은 **docker 그룹 아님**. `sudo` 로 `/opt/tmi/bin/deploy.sh`, `rollback.sh` 만 비밀번호 없이 허용 | K10 |
| 시크릿 | DB 비밀번호 등은 VM 의 `/opt/tmi/secrets/*` (root 소유, 600) → Compose `secrets:` 로 컨테이너에 파일 마운트. 환경 변수로 넘기지 않는다 (로그·`docker inspect` 로 샌다) | [D8] / 대안은 §13 DP6 |
| 업데이트 | unattended-upgrades (보안 업데이트 자동). Docker 저장소는 자동 대상이 아니므로 월 1회 수동 | [U1] |
| 컨테이너 | non-root, no-new-privileges, 메모리·CPU 제한, 이미지 digest 고정 | [D4] |
| 공급망 | 액션 SHA 고정, 베이스 이미지 digest 고정, (선택) Dependabot | [G5][D6] |

현재 `application.properties` 에 DB 계정이 평문(`postgres/postgres`)으로 들어 있다. 운영 전에 프로필 분리와 환경 변수·시크릿 주입으로 바꾼다 (§12 DEP-0, `docs/study/be-standards/07` §4.2 와 같은 지적).

---

## 10. 설정과 환경

| 환경 | 어디서 | 구성 | 용도 |
|---|---|---|---|
| `local` | 개발자 PC | `deploy/compose.yaml` + `compose.local.yaml` (app 1슬롯, Caddy 없이 8080 직접, 로컬 PostgreSQL) | 로컬 테스트·디버깅 (R4) |
| `ci` | GitHub 러너 | Testcontainers | 자동 테스트 |
| `prod` | Oracle A1 VM | `deploy/compose.yaml` + `compose.prod.yaml` | 운영 |

- 별도 staging 은 두지 않는다. 자원(12 GB)과 비용 때문이다. 대신 **새 슬롯을 운영 DB 에 붙인 채 트래픽 없이 스모크** 하는 단계(§4.4 의 3)가 사전 검증 역할을 한다. 대가: 새 버전이 기동 중 DB 에 쓰는 동작이 있으면 운영 데이터에 영향을 준다 → 기동 시 쓰기 금지 규칙(현재 코드는 해당 없음).
- 프로필: `application.properties`(공통) + `application-local.properties` + `application-prod.properties`. 운영 값은 모두 환경 변수·시크릿 파일로 주입 (`docs/public/07` §3 의 변수 목록을 따른다).

---

## 11. 저장소 구조 (추가될 파일)

```
Dockerfile                          # 멀티스테이지, non-root
.dockerignore
deploy/
├── compose.yaml                    # 공통: caddy, app-blue, app-green, postgres
├── compose.local.yaml              # 로컬 덮어쓰기
├── compose.prod.yaml               # 운영 덮어쓰기 (자원 제한, 로그 드라이버, secrets)
├── Caddyfile.tmpl                  # upstream 을 deploy.sh 가 채운다
├── cloud-init.yaml                 # VM 최초 구성 (Docker, 방화벽, 사용자, sshd, 디렉터리)
└── bin/
    ├── deploy.sh  rollback.sh      # §4.4 (VM 의 /opt/tmi/bin 에 설치)
    ├── backup.sh  restore-check.sh # §7.2
    └── diagnose.sh                 # §8.3
db/changes/                         # §7.3 (DP3 결과에 따라 Flyway 로 대체)
.github/workflows/
├── ci.yml                          # PR: 빌드·테스트·ArchUnit·이미지 빌드(푸시 안 함)
├── release.yml                     # main: 이미지 빌드·푸시
├── deploy.yml                      # plan → deploy(production, 승인) → verify(실패 시 롤백)
├── rollback.yml                    # 수동 롤백 (§13 DP4)
├── db-change.yml                   # 승인 필요 DDL 적용 (§7.3)
├── restore-check.yml               # 월 1회 복구 리허설
└── diagnose.yml                    # 읽기 전용 진단
.claude/skills/deploy/SKILL.md      # AI 배포 운영자 절차 (§6.2)
.claude/agents/infra-reviewer.md    # 인프라 PR 리뷰어 (보안·롤백 가능성 관점)
docs/ai/deploys/                    # 배포 기록 (AI 작성)
docs/ai/runbooks/                   # VM 생성·복구·키 교체 절차 (사람용)
```

- `docs/public/07_배포·운영.md` 는 팀 문서다. 이 설계가 확정되면 요약을 반영한다 (수정 전 사람 확인 — CLAUDE.md 규칙).

---

## 12. 단계 계획 (feature_list 항목)

| id | 내용 | 선행 | 검증 | 사람 |
|---|---|---|---|---|
| `DEP-0` | 프로필 분리, DB·외부 URL 을 환경 변수로, Actuator + `/livez`·`/readyz`, `show-sql` 운영 off | FIX-F1 (채점 API 가 500 인 채로 배포하지 않기 위해) | `--tests` 단위 테스트 + 로컬 기동 | 계획 승인 (src/main 변경) |
| `DEP-1` | Dockerfile + `deploy/compose*.yaml` 로컬 기동 | DEP-0 | `docker compose up --wait` 후 `/readyz` 200 | |
| `DEP-2` | `ci.yml` (Testcontainers 통합 테스트 포함 → SIG-1 해소) | DEP-1 | PR 에서 CI 통과 | D3 결정 |
| `DEP-3` | Oracle 계정·VM 생성 (cloud-init), 도메인 연결 | DP1 | SSH 접속, `https://<도메인>` 에서 Caddy 응답 | **사람** (계정·결제 정보·도메인) |
| `DEP-4` | `release.yml` + `deploy.yml` + `deploy.sh`·`rollback.sh` + environment `production` | DEP-2, DEP-3 | 의도적 실패 이미지로 자동 롤백 확인 | environment·secret 등록 |
| `DEP-5` | 백업·복구 리허설·Healthchecks | DEP-4 | 리허설 1회 성공·소요 시간 기록 | 저장소 키 발급 |
| `DEP-6` | skill `deploy` + 전용 PAT + 첫 운영 배포 | DEP-4 | AI 가 시작 → 사람 승인 → 배포 기록 | PAT 발급, 승인 |
| `DEP-7` | `diagnose.yml`, 외부 업타임 체크 | DEP-4 | 알림 1회 수신 테스트 | 알림 계정 |
| `DEP-8` (선택) | Prometheus·Grafana, 지표 기반 롤백 | DEP-7 | | |

학습 노트 후보 (skill `study-note`): blue/green 과 graceful shutdown, 컨테이너 JVM 메모리, expand/contract, 최소 권한 배포 토큰.

---

## 13. 결정 필요

| # | 질문 | 선택지 | 추천 | 이유 |
|---|---|---|---|---|
| DP1 | 도메인 | (a) 무료 DDNS 하위 도메인(duckdns 등) (b) 저가 도메인 구입(연 1~2만원) (c) IP 만 사용 | **(a)로 시작, 필요 시 (b)** | (a)는 0원이고 Caddy 자동 HTTPS 가 된다 (DDNS 도메인은 Public Suffix List 에 있어 발급 한도 문제도 작다) [C1][C4]. 가용성 보장은 없다. (c)는 브라우저 경고 |
| DP2 | PostgreSQL 버전 | 16 / 18 | **16** | 로컬·문서가 15~16 기준이고 EOL 2028-11 로 충분하다. 18 은 볼륨 경로가 바뀌어(§7.1) 지금 바꾸면 로컬 문서도 바꿔야 한다 [P4] |
| DP3 | 스키마 변경 방식 | (a) 현행 유지 + `db/changes` 수동 + `db-change` 워크플로 (b) Flyway 도입 (팀 결정 재검토) | **(b) 재검토 권장** | 배포 자동화와 blue/green 롤백에는 "어떤 DDL 이 어디까지 적용됐나" 의 기계적 기록이 필요하다. 국내 사례(우테코 팀, LINE)도 마이그레이션 도구를 쓴다 (`docs/study/be-standards/04`). 팀 결정이므로 사람이 정한다 |
| DP4 | 롤백에 승인이 필요한가 | (a) 자동 롤백·수동 롤백 모두 승인 없음 (이전에 승인된 digest 로만) (b) 수동 롤백은 승인 필요 | **(a)** | 장애 중 승인 대기는 복구를 늦춘다. 대상이 "이전에 승인·배포된 digest" 로 제한되면 위험이 작다 |
| DP5 | SSH 22 번 노출 | (a) 0.0.0.0/0 + 키 전용 + fail2ban (b) VPN/터널(Tailscale 등) 뒤로 숨김 | **(a)로 시작** | GitHub 러너 IP 가 고정되지 않는다. (b)는 외부 서비스 의존이 생긴다 |
| DP6 | 시크릿 관리 | (a) VM 파일(600) + Compose secrets, 원본은 비밀번호 관리자 (b) sops + age 로 암호화해 저장소에 커밋 | **(a)로 시작, (b)는 2단계** | 1인·단일 VM 에서 단순함 우선. (b)는 키 교체·재현성이 좋아진다 [D9] |
| DP7 | 이미지 빌드 | (a) Dockerfile (b) `bootBuildImage` (Buildpacks) | **(a)** | 레이어·사용자·JVM 옵션을 직접 다뤄 학습 가치가 크다. (b)는 관리 부담이 적지만 내부가 블랙박스 [S3][S4] |
| DP8 | CI 안의 Claude (claude-code-action) | 도입 / 보류 | **보류** | 구독 OAuth 로 0원이 가능하지만 약관 범위·secret 관리가 추가된다. 1단계는 로컬 Claude Code 세션만 |

---

## 14. 출처

모두 2026-10-17 에 원문을 직접 열어 확인했다 (예외는 "미확인" 표시).

| 번호 | 출처 |
|---|---|
| O1 | Oracle — Always Free Resources (A1 1,500 OCPU-시간·9,000 GB-시간, 회수 조건, 블록 200 GB, Object Storage 20 GB, 홈 리전) https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm |
| O2 | Oracle — Free Tier, FAQ (체험 종료 시 A1 비활성화) https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm · https://www.oracle.com/cloud/free/faq/ |
| O3 | Oracle — Security Lists / Security Rules https://docs.oracle.com/en-us/iaas/Content/Network/Concepts/securitylists.htm |
| O4 | Oracle — Ubuntu 이미지 방화벽(iptables) https://docs.oracle.com/en-us/iaas/Content/developer/apache-on-ubuntu/01oci-ubuntu-apache-summary.htm · 이미지 주의사항 https://docs.oracle.com/en-us/iaas/Content/Compute/References/images.htm |
| O5 | Oracle — Object Storage S3 호환 API https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/s3compatibleapi.htm |
| G1 | GitHub — Actions 과금 (public 저장소 표준 러너 무료) https://docs.github.com/en/billing/concepts/product-billing/github-actions · arm64 러너 GA (2025-08-07) https://github.blog/changelog/2025-08-07-arm64-hosted-runners-for-public-repositories-are-now-generally-available/ |
| G2 | GitHub — Deployments and environments (required reviewers 는 Free 플랜에서 public 만, 승인 전 secret 접근 불가) https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments |
| G3 | GitHub — Packages 과금 / Container registry (public 무료, 첫 push 는 private) https://docs.github.com/en/billing/concepts/product-billing/github-packages · https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry |
| G4 | GitHub REST — workflow dispatch (Actions: write), pending deployments 승인 (Deployments: write) https://docs.github.com/en/rest/actions/workflows · https://docs.github.com/en/rest/actions/workflow-runs |
| G5 | GitHub — Secure use reference (self-hosted 러너 public 금지, SHA 고정) https://docs.github.com/en/actions/reference/security/secure-use |
| A1 | Claude Code — GitHub Actions (구독 OAuth 토큰 인증, 비용) https://code.claude.com/docs/en/github-actions |
| A2 | Claude Code — Legal and compliance (구독 인증의 사용 범위) https://code.claude.com/docs/en/legal-and-compliance |
| A3 | 카카오 — 배포 리스크 감지 에이전트 KRIS (2026-08-31) https://tech.kakao.com/posts/831 |
| A4 | Anthropic — Our framework for developing safe and trustworthy agents https://www.anthropic.com/news/our-framework-for-developing-safe-and-trustworthy-agents · Building effective agents https://www.anthropic.com/engineering/building-effective-agents |
| A5 | OWASP — LLM06 Excessive Agency https://genai.owasp.org/llmrisk/llm062025-excessive-agency/ |
| A6 | anthropics/claude-code-action — security https://raw.githubusercontent.com/anthropics/claude-code-action/main/docs/security.md |
| S1 | Spring Boot 4.0 — Graceful shutdown https://docs.spring.io/spring-boot/4.0/reference/web/graceful-shutdown.html |
| S2 | Spring Boot 4.0 — Actuator endpoints (probes, add-additional-paths, liveness 에 외부 의존 금지) https://docs.spring.io/spring-boot/4.0/reference/actuator/endpoints.html |
| S3 | Spring Boot 4.0 — Dockerfiles (layer extract) https://docs.spring.io/spring-boot/4.0/reference/packaging/container-images/dockerfiles.html |
| S4 | Spring Boot 4.0 Gradle plugin — OCI image (`imagePlatform`) https://docs.spring.io/spring-boot/4.0/gradle-plugin/packaging-oci-image.html |
| J1 | Java 21 `java` 명령 (MaxRAMPercentage 기본 25%) https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html |
| D1 | Docker Compose services 스펙 (stop_grace_period 기본 10s, healthcheck) https://docs.docker.com/reference/compose-file/services/ |
| D2 | Docker — Packet filtering and firewalls (게시 포트가 ufw 우회) https://docs.docker.com/engine/network/packet-filtering-firewalls/ |
| D3 | Docker — Linux post-install (docker 그룹 = root 권한) https://docs.docker.com/engine/install/linux-postinstall/ |
| D4 | OWASP — Docker Security Cheat Sheet https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/Docker_Security_Cheat_Sheet.md |
| D5 | Docker — Logging configure (기본 로테이션 없음) https://docs.docker.com/engine/logging/configure/ |
| D6 | Docker — Build best practices (digest 고정) https://docs.docker.com/build/building/best-practices/ · Kubernetes Images (`latest` 지양) https://kubernetes.io/docs/concepts/containers/images/ |
| D7 | OpenSSH sshd_config https://man.openbsd.org/sshd_config |
| D8 | Docker Compose — Use secrets https://docs.docker.com/compose/how-tos/use-secrets/ |
| D9 | sops https://github.com/getsops/sops |
| C1 | Caddy — Automatic HTTPS https://caddyserver.com/docs/automatic-https |
| C2 | Let's Encrypt — 6일·IP 인증서 GA (2026-01-15) https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability |
| C3 | Caddy — command line (reload 무중단) https://caddyserver.com/docs/command-line |
| C4 | Public Suffix List https://publicsuffix.org/list/public_suffix_list.dat · Let's Encrypt rate limits https://letsencrypt.org/docs/rate-limits/ |
| P1 | Docker 공식 postgres 이미지 문서 (볼륨 경로, initdb, shm) https://github.com/docker-library/docs/blob/master/postgres/content.md |
| P2 | PostgreSQL 16 — Backup / Continuous archiving https://www.postgresql.org/docs/16/backup.html · pgBackRest https://pgbackrest.org/user-guide.html |
| P3 | Martin Fowler — Parallel Change / Evolutionary Database Design https://martinfowler.com/bliki/ParallelChange.html · https://martinfowler.com/articles/evodb.html |
| P4 | PostgreSQL 버전 정책 (16 EOL 2028-11-09) https://www.postgresql.org/support/versioning/ |
| M1 | Healthchecks.io pricing https://healthchecks.io/pricing/ · UptimeRobot pricing https://uptimerobot.com/pricing/ |
| M2 | Google SRE — Data integrity (복구 리허설) https://sre.google/sre-book/data-integrity/ |
| M3 | Google SRE — Monitoring distributed systems (블랙박스 모니터링) https://sre.google/sre-book/monitoring-distributed-systems/ |
| M4 | Prometheus storage https://prometheus.io/docs/prometheus/latest/storage/ · Grafana 요구사항 https://grafana.com/docs/grafana/latest/setup-grafana/installation/ |
| U1 | Ubuntu — Automatic updates https://documentation.ubuntu.com/server/how-to/software/automatic-updates/ |
| KR1 | 토스 — 결제 SDK 카나리 https://toss.tech/article/engineering-note-9 |
| KR2 | LINE 광고 서버 DevOps (단계적 배포, 장애 버전 태그 삭제) https://engineering.linecorp.com/ko/blog/line-ads-devops-culture |

**미확인·주의**
- A1 한도 축소의 시행일과 PAYG 계정 적용 범위는 공식 공지가 없다 (기사·커뮤니티 보고만 있음).
- PAYG 전환 시 유휴 회수 예외는 2023년 Oracle 메일(2차 출처)에만 있고 현재 문서에는 없다.
- Reserved 공인 IP 가 Always Free 범위에서 무료인지 문서에 명시가 없다 → VM 생성 후 콘솔의 Limits/Usage 에서 확인.
- `gh workflow run` 을 사람 PAT 로 시작했을 때 "prevent self-review" 가 켜져 있으면 본인이 승인할 수 없다 → 1인 운영에서는 이 옵션을 끈다.
