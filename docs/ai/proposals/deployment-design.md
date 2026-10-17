# 배포 설계 (Oracle Cloud 무료 VM + GitHub Actions + AI 운영자)

> 상태: **제안 — 미확정**. §15 의 결정을 정하면 §14 순서대로 반영한다.
> 조사일 2026-10-17. 근거는 [§16 출처](#16-출처) 의 번호. "확인" = 원문 직접 확인, "(추론)" = 확인한 사실에서 끌어낸 판단, "(미확인)" = 원문으로 확인하지 못함.
> 범위: TMI Core API + PostgreSQL. 외부 Similarity 서버는 범위 밖 (`SIMILARITY_BASE_URL` 로만 연결).
> 개정: 독립 리뷰 1차([../reviews/deployment-design-round-1.md](../reviews/deployment-design-round-1.md)) 반영 — AI 전용 계정·ruleset·environment 브랜치 제한(C1·C2), 승인 1회 안에서 배포·검증·롤백(C3), SSH 강제 명령(C4), 커밋 SHA 입력 + 빌드 출처 검증(C5), 운영 로그 비공개(C6), 인프라 파일 전달 경로·부트스트랩·장애 시나리오·관측 1단계 확대 등(M1~M14).

---

## 0. 한 장 요약

```
 AI 전용 GitHub 계정(write, admin 아님)          GitHub (public 저장소 — 무료 승인 게이트의 전제)
 Claude Code 세션(사용자 구독)                    ──────────────────────────────────────────────────
  PR 작성 → (인프라 경로가 아니면) merge ──────▶  ruleset: main 은 PR 필수, 인프라 경로는 사람(code owner) 리뷰 필수
                                                 CI   : 빌드·테스트(Testcontainers)·ArchUnit         [ubuntu-24.04-arm]
                                                 Release: CI 성공한 main 커밋만 → arm64 이미지 + 빌드 출처 증명 → ghcr.io
  `gh workflow run deploy -f sha=<main 커밋>` ─▶  Deploy (main 의 워크플로만 실행 가능)
                                                   plan  : 변경 요약·위험 플래그 (비밀값 없음)
  사람(별도 계정): "Approve and deploy" ───────▶   apply : environment=production, 승인 1회 → 이 job 안에서
                                                           출처 검증 → (DDL) → 배포 → 외부 스모크 → 실패 시 롤백
  AI: 결과 확인·배포 기록 PR ◀─────────────────────────────┘
                                                          │ SSH (강제 명령 게이트: "deploy <sha>" / "rollback" 만)
 Oracle Cloud Always Free — A1 VM 1대 (ARM64, 2 OCPU / 12 GB, Ubuntu)
 ┌──────────────────────────────────────────────────────────────────────────────────┐
 │ root 소유 /opt/tmi : 승인된 커밋의 deploy/ 를 받아 설치, 스크립트는 root 만 수정    │
 │ Caddy :80/:443 ──▶ app-blue | app-green (한 번에 하나만 트래픽)                    │
 │ PostgreSQL (내부망)  · 백업 cron → 암호화 → 오프사이트 2곳                        │
 │ node_exporter · Prometheus · Grafana (127.0.0.1)                                 │
 └──────────────────────────────────────────────────────────────────────────────────┘
 VM 밖: 외부 업타임 체크 · 백업 생존 신호 · 알림 웹훅
```

설계 원칙 다섯 가지:

1. **벤더 중립** — VM + Docker Compose + cloud-init + S3 호환 API 만 쓴다. 다른 클라우드로 옮길 때 바뀌는 것은 VM 생성과 VCN 방화벽 규칙뿐이다 (R2).
2. **0원** — public 저장소의 GitHub Actions·Environments·GHCR 와 Oracle Always Free 안에서 끝난다. AI 는 사용자의 Claude Code 구독 세션에서 돈다 (R1). 0원이 깨지는 지점은 §12 에 모았다.
3. **AI 는 시작, 사람은 승인 1회, 실행은 스크립트** — 권한으로 강제한다. 프롬프트로 강제하지 않는다 (§6).
4. **승인된 것만 배포된다** — 배포 입력은 main 커밋 SHA 이고, 이미지는 그 커밋의 release 워크플로가 만들었다는 출처 증명을 검증한다 (§5.3).
5. **실패해도 혼자 돌아온다** — 같은 승인 안에서 외부 스모크·자동 롤백까지 끝내고, VM 쪽에도 원복 장치를 둔다 (§4.4).

---

## 1. 목표와 제약

| # | 요구 | 출처 |
|---|---|---|
| R1 | 비용 0원 (Oracle Always Free 범위 안) | 사용자 |
| R2 | 클라우드 고유 기능에 종속되지 않는다 — VM 구조만 사용 | 사용자 |
| R3 | AI 가 배포 실행까지, 사람 승인 1회 | 사용자 |
| R4 | 테스트는 로컬에서도 돈다 (운영과 같은 compose 로 로컬 기동) | 사용자 |
| R5 | 국내 IT 기업 실무에 가까운 운영 역량을 학습한다 — 무중단 배포·롤백·백업과 복구·관측과 SLO·보안·장애 대응 | 사용자, `docs/study/be-standards/06` |
| R6 | Similarity 서버는 범위 밖 | 사용자 |

비목표: 다중 VM·쿠버네티스·오토스케일링·멀티 리전. 1인 서비스에서 비용·학습 대비 이득이 작다. 확장 방향만 §14 끝에 적는다.

### 1.1 불변 조건 (깨지면 설계가 무너지는 것)

| # | 조건 | 깨지면 |
|---|---|---|
| I1 | **저장소는 public** | Free 플랜에서 environment 의 required reviewers·environment secrets 가 사라진다 → 승인 게이트 소멸 [G2] |
| I2 | **GHCR 패키지는 public** | VM 이 자격 증명 없이 pull 할 수 없다 → VM 에 GitHub 토큰을 둬야 함 |
| I3 | **운영 SSH 키는 승인이 필요한 environment 에만** (저장소 수준 secret 금지) | 승인 없는 워크플로가 VM 에 접근 |
| I4 | **AI 는 사람 계정의 자격 증명을 갖지 않는다** | AI 가 승인 API 를 호출할 수 있다 |
| I5 | **Oracle A1 은 2 OCPU / 12 GB 를 넘지 않는다** | 과금 또는 체험 종료 시 A1 전체 비활성화 [O1][O2] |

---

## 2. 설계를 바꾼 조사 결과

| # | 사실 | 설계 영향 | 근거 |
|---|---|---|---|
| K1 | **Oracle A1 무료 한도가 2 OCPU / 12 GB** (월 1,500 OCPU-시간·9,000 GB-시간). 2026년 블로그 다수가 아직 4/24 로 적는다 | 자원 예산 12 GB (§4.2) | [O1][O2] 확인 |
| K2 | 유휴 회수: 7일 동안 CPU p95·네트워크·메모리(A1) 가 **모두** 20% 미만이면 회수 대상 | 실제 사용률을 측정한다. 평시 사용량이 낮을 수 있다 (§4.2) | [O1] 확인 |
| K3 | VCN 보안 규칙과 OS 방화벽을 **둘 다** 열어야 한다. OCI Ubuntu 는 iptables 기본 규칙이 있고 UFW 는 쓰지 않는다 | 방화벽 설계 §9.2 | [O3][O4] 확인 |
| K4 | Always Free 는 **홈 리전에서만**, A1 은 춘천 예외. "out of host capacity" 로 생성이 막힐 수 있다 | 홈 리전 서울, 인스턴스를 지우지 않는 운영 규칙 (§11) | [O1] 확인 |
| K5 | Environments 의 required reviewers 는 Free 플랜에서 public 저장소만. 승인은 **environment 를 참조하는 job 단위**이고, 승인 전 job 은 environment secret 에 접근하지 못한다. 기본값으로 **관리자는 보호 규칙을 우회할 수 있다**. environment 마다 배포 가능한 브랜치를 제한할 수 있다 | 승인 1회 = job 1개 (§3). 관리자 우회 끄기, 브랜치 = main (§6.3) | [G2] 확인 |
| K6 | 배포 시작(`workflow_dispatch`)은 Actions: write, 승인(`pending_deployments`)은 Deployments: write. **Actions: write 로 실행 기록·로그를 지울 수도 있다** | AI 는 별도 계정 + 최소 권한 (§6.2). 감사 기록을 VM·저장소에도 남긴다 | [G4] 확인 |
| K7 | `workflow_dispatch` 는 `--ref` 로 **다른 브랜치의 워크플로**를 실행할 수 있다 | environment 브랜치 제한 + job 안에서 ref 검증 (§6.3) | [G6] 확인 |
| K8 | public 저장소에 self-hosted 러너를 쓰면 안 된다 | VM 에 러너를 두지 않는다 | [G5] 확인 |
| K9 | Spring Boot 4 graceful shutdown 기본 활성, Compose `stop_grace_period` 기본 10초 | `stop_grace_period: 40s` | [S1][D1] 확인 |
| K10 | Docker 가 게시한 포트는 호스트 방화벽 INPUT 규칙을 **우회**한다. docker 그룹은 **root 와 같다** | 게시 포트는 Caddy 80/443 만, 컨테이너 쪽 제한은 `DOCKER-USER` 체인. 배포 계정은 docker 그룹 아님 | [D2][D3] 확인 |
| K11 | Actuator 의 readiness 그룹에는 **기본으로 DB 가 들어가지 않는다** | `group.readiness.include=readinessState,db` 명시 (§4.3) | [S2] 확인 |
| K12 | Caddy 는 인증서·키를 `/data` 에 두며 쓰기 가능·영속이어야 한다. `caddy reload` 는 admin API 로 설정을 넘긴다 | `/data` named volume, 설정은 디렉터리 마운트, reload 는 컨테이너 안에서 실행 (§4.4) | [C1][C3] 확인 |
| K13 | Claude 구독 OAuth 토큰은 개인 일반 사용 범위. 제품·서비스용이나 여러 저장소 공유는 API 키 | AI 는 사용자의 Claude Code 세션에서만 (§6.6) | [A1][A2] 확인 |

---

## 3. 한 번의 배포 — 흐름

| 단계 | 누가 | 무엇을 | 남는 기록 |
|---|---|---|---|
| 1 | AI 계정 | 기능 PR → CI 통과 → merge. **인프라 경로를 건드린 PR 은 사람 리뷰가 있어야 merge 된다** (ruleset, §6.3) | PR |
| 2 | `release.yml` | main 커밋의 CI 성공을 확인한 뒤에만 arm64 이미지 빌드 → GHCR push → **빌드 출처 증명(attestation)** 생성 | 이미지, digest, attestation |
| 3 | AI (skill `deploy`) | 배포할 **main 커밋 SHA** 를 골라 `gh workflow run deploy.yml --ref main -f sha=<SHA>` | 실행 1건 (시작자 = AI 계정) |
| 4 | `plan` job (비밀값 없음) | ref 가 main 인지, SHA 가 main 의 조상인지 확인 → 그 SHA 의 release 실행에서 digest 조회 → 직전 배포 SHA 와 비교해 **위험 플래그** 계산(§6.4) → Job summary 에 표시 | 승인 화면의 요약 |
| 5 | **사람** (별도 계정) | 요약을 보고 **Approve and deploy** 또는 Reject. 시작자(AI)와 승인자(사람)가 다르므로 "자기 승인 금지"가 켜진 채 성립 | 승인 기록 |
| 6 | `apply` job (environment `production`, 승인 후에만 SSH 키 접근) | 6-1 ref·SHA 재검증 → 6-2 `gh attestation verify` 로 이미지 출처 확인 → 6-3 (`SCHEMA` 이면) DDL 적용 → 6-4 SSH `deploy <SHA>` → 6-5 외부 스모크 → 6-6 실패 시 `if: failure()` 로 SSH `rollback` → 6-7 결과 요약 | 실행 로그, VM 배포 원장 |
| 7 | AI | 결과를 읽고 `docs/ai/deploys/<날짜>-<짧은 SHA>.md` 배포 기록 PR | 배포 기록 |

- 사람의 일은 **5단계 승인 1회**다. DDL·배포·검증·롤백이 모두 승인된 `apply` job 하나 안에서 끝나므로 두 번째 승인이 생기지 않는다 (K5).
- 외부 스모크만 실패하고 VM 내부 스모크(§4.4 3단계)는 통과했다면 DNS·인증서 같은 **엣지 문제**일 가능성이 높다. 이때는 롤백 대신 알림을 보낸다 (롤백해도 고쳐지지 않으므로).
- 동시 실행 방지: 운영을 건드리는 모든 워크플로(`deploy`·`rollback`·`db-change`·`restore-check`·`diagnose`)는 `concurrency: { group: prod-vm, cancel-in-progress: false }` 를 공유하고, VM 스크립트는 `flock /opt/tmi/state/lock` 으로 한 번에 하나만 돈다. 백업 cron 도 같은 잠금을 쓴다.

---

## 4. 런타임 구성 (VM 안)

### 4.1 디렉터리와 소유권

```
/opt/tmi/                         root:root 0755 — 배포 사용자는 쓸 수 없다
├── bin/                          ssh-gate, deploy.sh, rollback.sh, backup.sh, diagnose.sh (root 0755)
├── releases/<SHA>/deploy/        승인된 커밋의 deploy/ 디렉터리 사본 (root 소유)
├── current -> releases/<SHA>     심볼릭 링크 (원자적 전환)
├── state/                        root 0700 — active 슬롯, 슬롯별 이미지 .env, 배포 원장(ledger.jsonl), lock
├── secrets/                      root 0700 — DB·외부 API 비밀값 파일 (Compose secrets 로 마운트)
└── caddy/                        root — Caddyfile 디렉터리 (컨테이너에 디렉터리로 마운트)
```

- **인프라 파일 전달 경로**: `deploy.sh`(root)가 승인된 SHA 의 `deploy/` 를 public 저장소에서 내려받는다 (`git fetch` 또는 tarball) → 그 SHA 가 `origin/main` 의 조상인지 확인 → `releases/<SHA>/` 에 설치 → `current` 링크 전환. 배포 사용자가 파일을 올리는 경로는 없다.
- 단, `bin/` 의 스크립트 자체는 **VM 부트스트랩 때 한 번** cloud-init 으로 설치하고, 바꿀 때는 사람이 runbook 으로 갱신한다. 배포 경로로 "배포를 실행하는 코드"까지 바꾸지 않는다 (자기 자신을 바꾸는 배포 금지).

### 4.2 컨테이너와 자원 (A1 2 OCPU / 12 GB)

| 서비스 | 이미지 | 게시 포트 | 메모리 상한 | 비고 |
|---|---|---|---|---|
| `caddy` | `caddy:2` (digest 고정) | **80, 443** | 0.2 GB | `/data`·`/config` named volume, 설정은 디렉터리 마운트 |
| `app-blue` / `app-green` | `ghcr.io/kimhyomin97/tmi-core@sha256:…` | 없음 | 각 1.5 GB | 전환 순간 둘 다 떠서 3 GB |
| `postgres` | `postgres:16` (digest 고정, DP2) | 없음 | 3 GB | `/var/lib/postgresql/data` named volume, `shm_size: 256m` [P1] |
| `node_exporter`, `prometheus`, `grafana` | digest 고정 | 없음 (Grafana 는 127.0.0.1) | 1.5 GB | §8 |
| OS·페이지 캐시·여유 | | | 약 3.5 GB | |

- 공통: `restart: unless-stopped` (수동으로 멈춘 대기 슬롯은 재부팅 후에도 멈춘 채 유지), non-root, `security_opt: [no-new-privileges:true]`, `cpus` 제한, Docker 소켓 마운트 금지 [D4].
- 네트워크: `edge`(caddy·app), `data`(app·postgres), `mon`(exporter·prometheus·grafana·app 의 메트릭 포트). postgres 는 `edge` 에 없다.
- DB 커넥션: Hikari `maximum-pool-size=10` × 슬롯 2 + 리허설·관리 여유 → PostgreSQL `max_connections=50`.
- 시간대: VM·컨테이너·로그·cron 은 모두 **UTC**. 사용자에게 보이는 시각과 업무 규칙(예: Toss 스케줄)만 `Asia/Seoul` 을 명시한다.
- **유휴 회수(K2)**: 평시 실제 사용량은 2.4 GB(20%) 미만일 수 있다 — JVM 힙과 `shared_buffers` 는 실제로 쓸 때 메모리를 차지하기 때문이다 (추론). 첫 2주는 OCI 콘솔의 인스턴스 지표(회수 판정이 쓰는 지표)를 기록하고, 필요하면 앱에 `-Xms` = `-Xmx` + `-XX:+AlwaysPreTouch` 를 줘 상시 사용량을 올린다. 부하 생성 도구(lookbusy 등)는 쓰지 않는다.

### 4.3 헬스 체크

| 경로 | 쓰는 곳 | 포함 | 외부 공개 |
|---|---|---|---|
| `/livez` | 컨테이너 healthcheck | 앱 자체만. **DB 를 넣지 않는다** (DB 장애가 재시작 연쇄로 번짐) [S2] | 아니오 |
| `/readyz` | 배포 게이트(§4.4), Caddy 액티브 헬스체크 | `readinessState` + `db` (명시 설정, K11) | 아니오 |
| `/status` (Caddy 경로) | 외부 업타임 체크 | Caddy 가 `/readyz` 를 내부로 프록시하고 상태 코드만 돌려준다 (본문 없음) | 예 |
| `/api/v1/health` | 기존 설계(`docs/public/07` §4) | DB·Similarity 상태 | 예 — 단, 외부 업타임 알림에는 쓰지 않는다 (Similarity 는 범위 밖이라 오탐) |

- Actuator 노출은 `health`, `prometheus` 만. Caddy 는 `/actuator/**`, `/livez`, `/readyz` 를 외부에서 404 로 막는다. Prometheus 는 `mon` 네트워크로 앱 메트릭을 직접 긁는다.
- 컨테이너 healthcheck 명령: JRE 이미지에 curl 이 있는지 보장되지 않으므로 (미확인) 이미지 빌드 시 `curl` 을 명시적으로 설치하거나 작은 정적 바이너리를 넣는다 (§5.1).

### 4.4 `deploy.sh <SHA>` — blue/green 전환

```
0. flock 획득. 인자가 40자리 16진수인지 검사. 이미지 저장소 이름은 스크립트에 고정 (ghcr.io/kimhyomin97/tmi-core).
1. 승인된 SHA 의 deploy/ 를 받아 releases/<SHA>/ 에 설치, SHA 가 origin/main 의 조상인지 확인.
2. 이미지 digest 확인: 이미지 라벨 org.opencontainers.image.revision == <SHA> (빌드 출처는 apply job 이 이미 검증, 여기서는 이중 확인).
3. 대기 슬롯(예: green)의 .env 에 새 digest 를 쓰고 기동:
     docker compose -p tmi up -d --no-deps --wait --wait-timeout 180 app-green
   → 내부 스모크: /readyz 200 + 출제 API 1회. 실패하면 대기 슬롯 정지, 종료 코드 1 (활성 슬롯은 그대로).
4. caddy/ 디렉터리의 Caddyfile 을 새 upstream 으로 원자적 교체(새 파일 쓰고 rename) 후
     docker compose -p tmi exec -T caddy caddy reload --config /etc/caddy/Caddyfile
   → reload 가 실패하면 기존 설정이 유지된다. 즉시 롤백 경로로.
5. drain 10초.
6. 이전 슬롯 정지 (stop_grace_period 40s — Spring graceful shutdown 30s 보장).
7. state 갱신: active=green, previous=<이전 SHA·digest>, current=<새 SHA·digest>. ledger.jsonl 에 한 줄 추가(시각, SHA, digest, 실행 URL, 결과).
8. 확인 대기 타이머 시작: 10분 안에 apply job 이 "confirm" 을 보내지 않으면 VM 이 스스로 rollback (dead-man's switch).
```

- `rollback.sh`: 이전 슬롯을 `previous` digest 로 다시 띄우고 3~7 을 반대로 한다. 이전 슬롯 컨테이너는 정지만 했으므로 빠르다. 이미지 롤백이 성립하려면 DB 변경이 expand 만이어야 한다 (§7.3).
- 8번은 apply job 이 중간에 끊겨(러너 장애·네트워크) 외부 스모크도 롤백도 못 한 경우를 위한 장치다. apply job 이 외부 스모크 성공 후 SSH 로 `confirm` 을 보낸다.
- 첫 배포(부트스트랩): active 슬롯이 없으면 blue 로 기동, Caddy 는 처음부터 blue 를 가리키는 설정으로 시작한다 (§11.1).

---

## 5. 빌드·이미지·출처

### 5.1 이미지

| 항목 | 설계 | 근거 |
|---|---|---|
| 빌드 방식 | 멀티스테이지 Dockerfile + `jarmode=tools extract --layers` (DP7) | [S3] |
| 베이스 | Java 21 JRE arm64 (Temurin 또는 Liberica), digest 고정 | [D6] |
| 실행 | `USER 10001`, `ENTRYPOINT ["java", …]` (exec form — PID 1 이 SIGTERM 을 받는다), `-XX:MaxRAMPercentage=75` | [S3][J1] |
| 라벨 | `org.opencontainers.image.source`, `org.opencontainers.image.revision=<SHA>` | [G3] |
| 빌드 러너 | `ubuntu-24.04-arm` 에서 `linux/arm64` 네이티브 빌드 (QEMU 불필요) | [G1] |
| 태그 | `:<SHA 40자리>` 만. `latest` 없음. 배포는 digest 로 고정 | [D6] |

### 5.2 CI → release 연결

- `ci.yml`: PR·main push 에서 빌드, 단위·슬라이스 테스트, **Testcontainers 로 PostgreSQL 통합 테스트**(GitHub 러너에는 Docker 가 있다 → 결정 D3(a)가 CI 에서 성립), ArchUnit, spotlessCheck, 이미지 빌드(푸시 안 함).
- `release.yml`: `workflow_run`(ci 성공, 브랜치 main)에서만 동작 → 같은 SHA 를 다시 빌드하지 않고, ci 가 만든 이미지 tar 를 artifact 로 넘겨받아 push 한다 (테스트한 빌드 = 배포하는 빌드). artifact 용량이 부담되면 재빌드 + 같은 SHA 확인으로 대체 (설계 단계 선택).
- 권한: 워크플로 기본 `permissions: read-all`, release job 만 `packages: write`, `id-token: write`, `attestations: write`.

### 5.3 빌드 출처 증명 — "승인된 코드로 만든 이미지인가"

- 문제: 저장소의 어떤 브랜치 워크플로든 `GITHUB_TOKEN` 으로 같은 패키지에 이미지를 push 할 수 있다. digest 만 받으면 리뷰되지 않은 코드의 이미지가 배포될 수 있다.
- 해결:
  1. 배포 입력은 **main 커밋 SHA**. digest 는 그 SHA 의 `release.yml` 실행 결과에서 찾는다.
  2. `release.yml` 은 `actions/attest-build-provenance` 로 출처 증명을 만든다.
  3. `apply` job 이 `gh attestation verify oci://… --repo kimhyomin97/project-tmi-core --signer-workflow kimhyomin97/project-tmi-core/.github/workflows/release.yml` 로 검증한다.
  4. VM 은 라벨의 revision 이 SHA 와 같은지 이중 확인한다 (§4.4 2).
- public 저장소의 attestation 은 무료다 (미확인 — DEP-4 에서 확인).

### 5.4 이미지 정리

- GHCR: 주 1회 `cleanup.yml` 이 운영 원장에 없는 태그 중 최근 20개를 넘는 것을 삭제.
- VM: `deploy.sh` 끝에 현재·이전 digest 와 최근 3개를 남기고 `docker image prune`.

---

## 6. AI 의 역할과 통제

### 6.1 원칙

1. **결정은 결정적으로, 해석은 AI 로.** 배포·롤백·백업·위험 판정은 스크립트가 한다. AI 는 스크립트를 *작성*하고(PR), 배포를 *요청*하고, 결과를 *해석*한다. 카카오 KRIS 가 같은 이유로 판정과 LLM 을 분리했다 [A3]. Anthropic·OWASP 도 고위험 동작은 사람 승인 + 하위 시스템 강제를 권한다 [A4][A5].
2. **AI 와 사람은 다른 계정이다.** 권한·감사 기록이 계정 단위로 갈린다.
3. **통제는 GitHub 설정과 VM 권한으로 강제한다.** 문서·프롬프트의 "하지 마" 는 통제가 아니다.
4. **AI 는 운영 비밀값을 갖지 않는다.**

### 6.2 계정과 권한

| 주체 | GitHub 권한 | 갖는 것 | 갖지 않는 것 |
|---|---|---|---|
| 사람 (`kimhyomin97`) | admin | 승인 권한(production reviewer), code owner | — |
| **AI 전용 계정** (예: `tmi-ai-bot`, 별도 GitHub 계정) | **write** collaborator | 브랜치 push, PR, merge(ruleset 이 허용하는 것만), `workflow_dispatch` | admin, 승인, environment secret, VM 접근 |
| `apply` job | — | 승인 후 `production` environment secret (배포 SSH 키, known_hosts) | — |
| `diagnose` job | — | `production-readonly` secret (진단 SSH 키, 결과 암호화용 age 공개키) | 배포 키 |

- AI 실행 환경(Claude Code 세션)에는 **AI 계정의 토큰만** 둔다. 사람의 `gh` 로그인·git 자격 증명이 있는 셸에서 AI 를 돌리지 않는다 (I4). 로컬이면 OS 사용자를 분리하거나, 클라우드 세션을 쓴다.
- AI 계정 토큰: fine-grained PAT 를 이 저장소 하나로 제한 — Contents: write, Pull requests: write, Actions: write, Metadata: read. Deployments·Administration·Secrets 권한 없음.
- 별도 머신 계정이 GitHub 약관상 허용되는 범위인지는 DEP-4 전에 확인한다 (미확인). 불가하면 GitHub App(설치 토큰)으로 같은 권한 분리를 한다.

### 6.3 GitHub 설정 (승인 게이트를 실제로 만드는 것)

| 설정 | 값 | 막는 것 |
|---|---|---|
| environment `production` — required reviewers | 사람 계정 1명 | — |
| `production` — **Prevent self-reviews** | 켬 (시작자 AI ≠ 승인자 사람이라 성립) | 시작한 계정의 자기 승인 |
| `production` — **Allow administrators to bypass** | **끔** | 관리자 우회 (K5) |
| 모든 environment — deployment branches | **`main` 만** | 다른 브랜치의 워크플로가 secret 을 받는 것 (K7) |
| ruleset (main) | PR 필수, force push·삭제 금지, required status checks = ci, **code owner 리뷰 필수**, 필요 승인 수 0 | 인프라 경로의 무리뷰 merge. 일반 PR 은 AI 가 merge 할 수 있게 승인 수는 0 |
| `CODEOWNERS` | `.github/**`, `deploy/**`, `Dockerfile`, `db/**`, `build.gradle`, `CODEOWNERS` → 사람 계정 | AI 가 배포 방식을 바꾸고 스스로 merge |
| `apply` job 첫 step | `github.ref == 'refs/heads/main'` 와 SHA ⊂ main 검증, `github.workflow_sha` 를 요약에 표시 | 설정 실수에 대한 이중 방어 |
| 저장소 수준 secret | 배포 관련 없음 (I3) | — |

### 6.4 `plan` job 의 위험 플래그 (결정적 규칙)

직전 배포 SHA(원장)와 이번 SHA 의 diff 로 계산해 Job summary 표로 보여 준다. AI 는 이 표를 3줄로 풀어 실행에 코멘트한다 — 판정은 표가 하고 AI 는 설명만 한다.

| 플래그 | 규칙 | 승인 전에 볼 것 |
|---|---|---|
| `SCHEMA` | `db/**`, `docs/public/init.sql`, `@Entity`·`@Embeddable`·`@Table` 이 있는 파일, `spring.jpa.hibernate.ddl-auto` 값 변경 | §7.3 expand 규칙 위반이 없는지 |
| `DESTRUCTIVE_DDL` | 이번 DDL 에 `DROP`, `ALTER … DROP`, `SET NOT NULL`, `RENAME` | **기본 차단** — 이전 릴리스에서 expand 를 끝낸 contract 단계라는 표시(PR 라벨)가 있어야 통과 |
| `INFRA` | `deploy/**`, `Dockerfile`, `.github/**` | ruleset 으로 사람 리뷰를 이미 거쳤는지 (PR 링크) |
| `CONFIG` | `application*.properties` 의 키 추가·삭제 | VM `secrets/`·환경 변수 갱신 필요 여부 |
| `DEPS` | `build.gradle` 의존성 변경 | |
| `API` | `*Controller.java`, `*Api.java`, `docs/public/03_API명세.md` | 외부 계약 변경 |
| `ARCHUNIT` | `com.tmi.core.architecture` 의 `@Disabled` 증가 | |

### 6.5 AI 역할 (skill · 서브에이전트)

| 역할 | 언제 | 하는 일 |
|---|---|---|
| 인프라 작성자 | 개발 단계 | Dockerfile·compose·워크플로·스크립트 PR. 인프라 리뷰어 서브에이전트(`infra-reviewer`, 보안·롤백 가능성 관점)의 리뷰 후 사람 리뷰 요청 |
| 배포 운영자 (skill `deploy`) | 배포 | 배포할 SHA 선택 근거 정리 → 실행 → 위험 플래그 해설 코멘트 → 승인 대기 상태 확인 → 결과 확인 → 배포 기록 PR. 세션에 AI 계정 토큰이 아니면 중단 (보조 장치 — 주 통제는 §6.3) |
| 장애 분석가 | 실패·알림 | `diagnose` 실행 → 결과 복호화(키는 AI 를 실행하는 기기에만) → 원인과 수정 PR 제안 → 포스트모템 초안 |
| 복구 리허설 담당 | 월 1회 | `restore-check` 실행 요청 → 결과 기록 |

### 6.6 CI 안의 Claude (선택)

`anthropics/claude-code-action` 을 구독 OAuth 토큰으로 쓰면 추가 비용 없이 인프라 PR 자동 리뷰가 가능하다 [A1]. 다만 약관 범위 [A2], public 저장소의 트리거 제한 [A6], 배포 secret 과의 분리가 필요하다. 1단계는 쓰지 않는다 (DP8).

### 6.7 남는 위험 (알고 받아들이는 것)

| 위험 | 이유 | 완화 |
|---|---|---|
| AI 계정이 일반 PR 을 merge → 버그가 main 에 들어감 | 사용자 지시("merge 까지")와 학습 속도를 위해 일반 코드는 사람 리뷰를 강제하지 않는다 | CI 필수 + 배포는 승인 필요 — 운영 반영 전에는 반드시 사람이 본다 |
| AI 계정이 Actions 실행 기록을 삭제 | Actions: write 의 부수 권한 (K6) | 배포 원장은 VM `ledger.jsonl` 과 `docs/ai/deploys/` 에도 남는다. environment 배포 이력은 별도 |
| 프롬프트 인젝션으로 엉뚱한 배포 요청 | AI 가 PR·로그를 읽는다 | 최악의 결과가 "불필요한 승인 요청" 이고, 사람 승인에서 멈춘다 |

---

## 7. 데이터베이스 운영

### 7.1 초기화

- 데이터 볼륨이 비어 있을 때만 `docs/public/init.sql` 을 `/docker-entrypoint-initdb.d/` 로 적용한다 [P1]. PostgreSQL 16 은 `/var/lib/postgresql/data` 에 마운트 (18 부터 경로가 바뀐다) [P1].

### 7.2 백업과 복구

| 항목 | 설계 |
|---|---|
| 주기 | 일 1회 `pg_dump -Fc` (03:00 UTC), `flock` 공유 |
| 암호화 | `age` 공개키로 암호화. **개인키는 VM 에 없다** — 사람의 비밀번호 관리자 + `restore` environment secret |
| 저장 | 오프사이트 **2곳**: (1) Oracle Object Storage (S3 호환 API) (2) 다른 회사의 S3 호환 무료 저장소 (Backblaze B2 또는 Cloudflare R2 — 무료 한도 미확인, DEP-5 에서 확인). 계정 정지 시 VM 과 백업을 같이 잃지 않기 위해 |
| 키 권한 | 업로드 키는 **쓰기 전용**(삭제 불가). 보존은 저장소의 수명주기·버전 관리로 (일 7개 + 주 4개). VM 이 탈취돼도 백업을 지울 수 없게 |
| 한도 | Oracle Object Storage 무료: Always Free 전용 계정 20 GB, 유료·체험 계정 Standard 10 GB [O1]. 덤프 크기를 원장에 기록하고 70% 에서 알림 |
| 생존 신호 | 백업 성공 시 Healthchecks.io ping. 끊기면 알림 [M1] |
| 복구 리허설 | **월 1회** `restore-check.yml` (environment `restore`, 승인 필요 — 개인키가 있으므로): 러너에서 최신 백업을 받아 복호화 → 임시 PostgreSQL 컨테이너에 복원 → 앱을 `ddl-auto=validate` 로 붙여 기동 → 행 수 확인 → 소요 시간 기록. 운영 VM 은 건드리지 않는다 [M2] |
| 2단계 (선택) | pgBackRest + WAL 아카이빙 (PITR, RPO 분 단위) [P2] |

- RPO 24시간, RTO 목표는 §11.3.

### 7.3 스키마 변경 — blue/green 에서 지켜야 할 것

- 전환 중에는 **구·신 버전이 같은 DB 를 동시에 쓴다.** DDL 은 구 버전과 호환되는 *확장*(테이블·nullable 컬럼·인덱스 추가)만 먼저 적용하고, *축소*(삭제·NOT NULL·이름 변경)는 그다음 릴리스에서 한다 (expand/contract) [P3]. 이것이 있어야 이미지 롤백이 성립한다.
- **DDL 은 앱 기동이 아니라 승인된 `apply` job 의 별도 step 에서** 적용한다 (§3 6-3). 앱이 기동하면서 마이그레이션을 실행하면, 새 슬롯을 띄우는 순간(전환 전, 스모크 단계) 운영 DB 가 바뀌고 롤백해도 되돌아가지 않는다.
- 적용 방식은 DP3:
  - (a) 현행 유지 — `db/changes/NNNN_설명.sql` + 적용 이력 표 + apply job 의 `psql` step
  - (b) **Flyway CLI** 를 apply job step 으로 실행 (앱에 내장하지 않음)
- CI 규칙: 같은 PR 에 expand 와 그 contract 가 함께 있으면 실패. `DESTRUCTIVE_DDL` 플래그(§6.4).

---

## 8. 관측·알림·운영 체계

### 8.1 1단계 (처음부터)

| 무엇 | 어떻게 | 비고 |
|---|---|---|
| 외부 가용성 | UptimeRobot 무료(5분 간격) → `/status` | VM 안의 모니터링은 VM 이 죽으면 같이 죽는다 [M3] |
| 백업 | Healthchecks.io 생존 신호 | |
| 메트릭 | Micrometer → Prometheus(보존 15일) + node_exporter, Grafana(127.0.0.1, SSH 터널) | 메모리 약 1.5 GB [M4]. 대시보드: 요청 수·에러율·p95 지연·JVM 힙·DB 커넥션·디스크·메모리(회수 감시) |
| 로그 | Docker 로그 드라이버 `journald` → journald 보존 14일·용량 상한. Caddy access log 켬 | 컨테이너를 지워도 로그가 남아 롤백 뒤 원인 분석 가능 [D5] |
| 알림 | Prometheus 알림 규칙 → (Alertmanager 대신 경량으로) Grafana 알림 → Discord/Slack 웹훅 + 이메일 | 디스크 80%, 에러율, 백업 누락, 인증서 만료 14일 전 |
| SLO | 가용성 99.0% / 28일 (에러 버짓 약 6.7시간) — 학습용 출발점 | 에러 버짓 소진 시 기능 배포 중단·안정화 우선 규칙 |

### 8.2 `diagnose` — AI 의 읽기 전용 눈 (비공개)

- environment `production-readonly`: 승인 없음, 브랜치 `main` 만, **진단 전용 SSH 키** + age 공개키.
- VM `authorized_keys`: `restrict,command="/opt/tmi/bin/diagnose.sh"` — 고정 명령만.
- 출력(컨테이너 상태, 최근 오류 로그 N줄, 자원 스냅샷, 배포 원장 꼬리)은 **age 로 암호화해서** artifact 로 올린다. public 저장소의 artifact 는 누구나 받을 수 있기 때문이다. 개인키는 AI 를 실행하는 기기에만 둔다.
- 워크플로 로그에는 집계값만 찍는다 (`set -x` 금지). 사용자 답안·Toss 보유 내역 같은 개인 데이터는 로그·요약 어디에도 쓰지 않는다.

### 8.3 런북과 포스트모템

- `docs/ai/runbooks/` (사람용 절차): VM 부트스트랩, 배포 실패, 수동 롤백, DB 복구, 키 교체, 인증서 문제, VM 재구축, 계정 정지 시 이전.
- `docs/ai/postmortems/` : 장애마다 템플릿(타임라인·영향·원인·재발 방지)으로 기록. AI 가 초안, 사람이 확정. 국내 기업의 비난 없는 포스트모템 관행 (`docs/study/be-standards/06`).

---

## 9. 보안

### 9.1 SSH

| 항목 | 설계 |
|---|---|
| 계정 | `deploy` (배포·진단 키만, 셸 없음), `ops` (사람의 관리용, sudo 가능). `AllowUsers deploy ops`, root 로그인 금지, 키 전용 [D7] |
| 강제 명령 | `deploy` 의 모든 키에 `restrict,command="/opt/tmi/bin/ssh-gate"`. gate 는 `SSH_ORIGINAL_COMMAND` 를 허용 목록으로만 받는다: `deploy <40자리 16진수>`, `confirm`, `rollback`, (진단 키) 고정 명령 |
| sudo | gate 가 `sudo /opt/tmi/bin/deploy.sh <SHA>` 등 **정확한 인자 형식**만 실행. sudoers 에 `env_reset`, `secure_path`, 해당 스크립트만 NOPASSWD |
| 호스트 키 | 러너는 environment secret 의 `known_hosts` 로 호스트 키를 고정 (TOFU 금지) |
| 노출 | 22 번은 GitHub 러너 IP 가 고정되지 않아 넓게 열린다 → 키 전용 + 강제 명령이 주 방어, fail2ban 은 보조 (DP5) |
| 비상 접근 | `ops` 키 분실·sshd 장애 시 OCI 콘솔의 직렬 콘솔로 접근 (runbook). 벤더 기능이지만 비상용이라 R2 예외로 둔다 |

### 9.2 방화벽

- VCN: 인그레스 22, 80, 443 만.
- 호스트: `/etc/iptables/rules.v4` 를 cloud-init 이 직접 관리한다 — INPUT 은 22 와 OCI iSCSI 규칙만 (UFW 쓰지 않음). Docker 가 기동한 뒤 `netfilter-persistent save` 를 하지 않는다 (Docker 체인이 저장돼 재부팅 시 충돌).
- 80/443 은 Docker 게시 포트이므로 INPUT 규칙이 아니라 Docker 가 만든 규칙으로 열린다 (K10). 컨테이너로 가는 트래픽 제한이 필요하면 `DOCKER-USER` 체인을 쓴다.

### 9.3 시크릿

- VM `/opt/tmi/secrets/*` (root 0600) → Compose `secrets:` 로 파일 마운트 → 앱은 `*_FILE` 또는 Spring `configtree:` 로 읽는다. 환경 변수로 넘기지 않는다 (로그·`docker inspect` 노출) [D8].
- 원본은 사람의 비밀번호 관리자. 2단계에서 sops + age 로 저장소에 암호화 보관 (DP6) [D9].
- 현재 `application.properties` 에 DB 계정이 평문으로 들어 있다 → DEP-0 에서 프로필 분리·외부 주입으로 바꾼다.

### 9.4 그 밖

- unattended-upgrades: 보안 업데이트 자동. **자동 재부팅은 끄고**, 재부팅 필요 알림만 받는다. needrestart 가 Docker 데몬을 재시작하지 않도록 예외 설정. Docker·Caddy·PostgreSQL 이미지는 월 1회 수동 갱신 PR [U1].
- 컨테이너: non-root, no-new-privileges, 자원 제한, digest 고정 [D4]. 액션은 commit SHA 고정 [G5].

---

## 10. 설정과 환경

| 환경 | 어디서 | 구성 | 용도 |
|---|---|---|---|
| `local` | 개발자 PC | `deploy/compose.yaml` + `compose.local.yaml` (app 1개, Caddy·모니터링 없이, 8080 직접) | 로컬 테스트·디버깅 (R4) |
| `ci` | GitHub 러너 | Testcontainers | 자동 테스트 |
| `prod` | Oracle A1 VM | `compose.yaml` + `compose.prod.yaml` | 운영 |

- 별도 staging 은 두지 않는다 (자원 12 GB). 대신 새 슬롯을 **트래픽 없이 운영 DB 에 붙여 스모크**하는 단계가 사전 검증 역할을 한다. 대가: 새 버전이 기동 중 DB 에 쓰거나 스케줄 작업을 돌리면 운영에 영향을 준다 →
  - 기동 시 DB 쓰기 금지 (DDL 은 §7.3 처럼 apply job 에서).
  - **스케줄 작업은 활성 슬롯에서만**: 현재 커밋된 코드에는 `@EnableScheduling` 만 있고 `@Scheduled` 작업은 없다. 미커밋 Toss 작업(매시 보유자산 스냅샷)이 들어오면 ShedLock(JDBC 잠금) 또는 활성 슬롯에만 `SCHEDULING_ENABLED=true` 를 주는 방식 중 하나를 적용한다 (DP9).
- 프로필: `application.properties`(공통) + `application-local.properties` + `application-prod.properties`. 운영 값은 시크릿 파일·환경 변수로 주입. 외부 API 키(Toss 등)도 `secrets/` 에 둔다.

---

## 11. 부트스트랩·장애 대응·재구축

### 11.1 첫 구축 (사람, runbook)

1. Oracle 계정 (홈 리전 서울), 예산 알림·compartment quota 로 A1 2 OCPU / 12 GB 상한 (I5).
2. VCN·보안 규칙 (22/80/443), Reserved 공인 IP (무료 여부 콘솔에서 확인 — 미확인).
3. A1 인스턴스 생성 + `deploy/cloud-init.yaml` (Docker 설치, 사용자·sshd·sudoers·iptables, `/opt/tmi` 구조, `bin/` 스크립트 설치, journald 설정). 생성 실패(용량 부족)면 다른 AD·시간을 두고 재시도 [O1].
4. 도메인 DNS → 공인 IP (DP1).
5. `secrets/` 파일 작성 (사람), environment·ruleset·CODEOWNERS·AI 계정 설정 (§6.3).
6. 첫 `deploy` — active 슬롯이 없으면 blue 로 기동, Caddy 는 blue 를 가리키는 초기 설정으로 시작.

### 11.2 운영 규칙

- **인스턴스를 terminate 하지 않는다.** 용량 부족으로 다시 못 만들 수 있다 (K4). 재부팅·정지만 한다. 부트 볼륨은 "인스턴스 삭제 시 보존" 으로 둔다.
- 30일 이상 계정에 로그인하지 않으면 정지될 수 있다 [O2] → 월 1회 콘솔 로그인 (복구 리허설과 같은 날).

### 11.3 장애 시나리오

| 시나리오 | 감지 | 대응 | 목표 |
|---|---|---|---|
| 새 버전 오류 | 내부·외부 스모크, 에러율 알림 | 자동 롤백 / 수동 `rollback.yml` | 5분 |
| apply job 중단 | 확인 타이머 | VM 이 10분 뒤 자동 원복 (§4.4 8) | 15분 |
| VM 재부팅·회수(정지) | 외부 업타임 | 재시작 → `restart: unless-stopped` 로 복귀. 회수 정지면 콘솔에서 시작 | 30분 |
| 디스크 가득 | 디스크 80% 알림 | 이미지 prune, journald vacuum, 볼륨 확장(200 GB 안) | 1시간 |
| PostgreSQL 장애·손상 | readyz 실패 | 재시작 → 안 되면 최신 백업 복원 (runbook) | 2시간, RPO 24시간 |
| 인증서 갱신 실패 | 만료 14일 전 알림 | DNS·80 포트 확인, Caddy 로그 | 만료 전 |
| DDNS·도메인 장애 | 외부 업타임 | DNS 제공자 전환 (DP1) | 1일 |
| GHCR 장애 | 배포 실패 | 배포 연기 (운영 중 서비스는 영향 없음) | — |
| 계정 정지·VM 소실 | 외부 업타임 | 다른 VM(다른 클라우드 포함)에 cloud-init + 백업 복원으로 재구축 | 1일 (연 1회 재구축 리허설) |

---

## 12. 비용 0원이 깨지는 지점

| 지점 | 예방 |
|---|---|
| 저장소를 private 으로 전환 | I1 — 승인 게이트·environment secret 이 사라지는 것도 동시에 |
| GHCR 패키지가 private 으로 남음 | 첫 push 후 public 전환, 배포 runbook 체크 항목 |
| A1 를 2 OCPU / 12 GB 넘게 생성, 체험 크레딧으로 큰 shape | I5, compartment quota |
| PAYG 전환 후 한도 초과 리소스 생성 | 예산 알림 0원 초과 시 |
| 블록 볼륨 합계 200 GB 초과, 볼륨 백업 5개 초과 | 50+50 GB 로 시작 |
| Object Storage 무료 한도 초과 | 덤프 크기 원장 기록, 70% 알림 |
| Reserved IP 과금 여부 | 생성 후 콘솔 확인 (미확인) |
| 도메인 구입 | DP1 (b) 를 고르면 연 1~2만원 |
| Claude 를 API 키로 CI 에서 실행 | DP8 보류 |

---

## 13. 저장소 구조 (추가될 파일)

```
Dockerfile · .dockerignore
CODEOWNERS (.github/)
deploy/
├── compose.yaml · compose.local.yaml · compose.prod.yaml
├── caddy/Caddyfile.tmpl
├── cloud-init.yaml                 # VM 최초 구성 (bin/ 설치 포함)
├── bin/ ssh-gate deploy.sh rollback.sh backup.sh diagnose.sh
└── monitoring/ prometheus.yml alert-rules.yml grafana/
db/changes/                         # DP3 (a) — (b) 면 Flyway 마이그레이션 디렉터리
.github/workflows/
├── ci.yml · release.yml · cleanup.yml
├── deploy.yml                      # plan → apply(production, 승인 1회)
├── rollback.yml                    # 수동 롤백 (DP4)
├── restore-check.yml               # 월 1회 복구 리허설 (restore environment)
└── diagnose.yml                    # 읽기 전용 진단 (production-readonly)
.claude/skills/deploy/SKILL.md      # AI 배포 운영자 절차
.claude/agents/infra-reviewer.md    # 인프라 PR 리뷰어
docs/ai/deploys/ · docs/ai/runbooks/ · docs/ai/postmortems/
```

`docs/public/07_배포·운영.md` 는 팀 문서다. 이 설계가 확정되면 요약을 반영한다 (수정 전 사람 확인 — CLAUDE.md 규칙).

---

## 14. 단계 계획 (feature_list 항목)

| id | 내용 | 선행 | 검증 | 사람이 할 일 |
|---|---|---|---|---|
| `DEP-0` | 프로필 분리, DB·외부 URL·외부 API 키 외부 주입, Actuator + `/livez`·`/readyz`(db 포함), `show-sql` 운영 off | FIX-F1 (500 인 API 를 배포하지 않기 위해) | 단위 테스트 + 로컬 기동 | 계획 승인 (`src/main` 변경) |
| `DEP-1` | Dockerfile + compose 로컬 기동 | DEP-0 | `docker compose up --wait` 후 `/readyz` 200, 정지 시 graceful 로그 | |
| `DEP-2` | `ci.yml` (Testcontainers 통합 테스트 → SIG-1 해소) | DEP-1 | PR 에서 CI 통과 | D3 결정 |
| `DEP-3` | **GitHub 통제 설정**: AI 계정, ruleset, CODEOWNERS, environments(브랜치 main·관리자 우회 끔·자기 승인 금지) | — | 체크리스트 + AI 계정으로 승인 API 호출이 403 인지 확인 | **사람** (계정·설정) |
| `DEP-4` | Oracle VM 구축 (cloud-init), 도메인, 첫 수동 기동 | DEP-1, DP1 | `https://<도메인>/status` 200 | **사람** (계정·결제 정보·도메인) |
| `DEP-5` | `release.yml`(출처 증명) + `deploy.yml` + VM 스크립트 | DEP-2, DEP-3, DEP-4 | 의도적 실패 이미지로 자동 롤백, apply 중단 시 VM 자동 원복 확인 | secret 등록 |
| `DEP-6` | 관측 1단계 + 알림 + `diagnose` | DEP-5 | 알림 1회 수신, diagnose 결과 복호화 | 알림 계정 |
| `DEP-7` | 백업 2곳 + `restore-check` | DEP-5 | 리허설 1회 성공·소요 시간 기록 | 저장소 계정·키 |
| `DEP-8` | skill `deploy` + 첫 AI 운영 배포 | DEP-5~7 | AI 계정이 시작 → 사람 승인 → 배포 기록 PR | 승인 |
| `DEP-9` (선택) | 지표 기반 자동 롤백, pull 기반 배포(DP5 c), pgBackRest | DEP-8 | | |

확장 방향 (비목표 해제 시): VM 2대 + 로드밸런서, PostgreSQL 복제, 쿠버네티스(k3s) + GitOps.

학습 노트 후보: blue/green 과 graceful shutdown, 컨테이너 JVM 메모리, expand/contract, 최소 권한 배포 토큰과 승인 게이트, 빌드 출처 증명, SLO 와 에러 버짓.

---

## 15. 결정 필요

| # | 질문 | 선택지 | 추천 | 이유 |
|---|---|---|---|---|
| DP0 | **AI 전용 계정을 만들까** | (a) 별도 GitHub 계정(write) (b) GitHub App (c) 사람 계정 공유 | **(a)** (약관 확인 후, 안 되면 (b)) | (c)면 AI 가 승인 API 를 부를 수 있어 승인 게이트가 성립하지 않는다 (I4, K6). (b)는 설정이 더 복잡하지만 약관 문제가 없다 |
| DP1 | 도메인 | (a) 무료 DDNS 하위 도메인 (b) 도메인 구입(연 1~2만원) (c) IP + Let's Encrypt IP 인증서(6일, Caddy 설정 추가) | **(a)로 시작, (b) 권장 이동** | (a) 0원, Caddy 자동 HTTPS. 다만 DNS·CAA 레코드 같은 실무 학습과 가용성은 (b)가 낫다. (c)는 가능하지만 IP 가 바뀌면 끝이고 issuer 설정이 필요하다 [C1][C2][C4] |
| DP2 | PostgreSQL 버전 | 16 / 18 | **16** | 로컬·문서가 16 기준, EOL 2028-11. 18 은 볼륨 경로가 바뀐다 [P4] |
| DP3 | 스키마 변경 방식 | (a) `db/changes` + psql step (b) Flyway CLI step | **(b) 재검토 권장** | 어떤 DDL 이 어디까지 적용됐는지 기계적 기록이 필요하다. 앱 내장이 아니라 apply job step 으로 돌리면 "기동 시 쓰기 금지" 와도 맞다. 팀 결정(Flyway 미도입)을 바꾸는 일이라 사람이 정한다 |
| DP4 | 수동 롤백의 승인 | (a) 승인 없음 — 이전에 승인·배포된 SHA 로만 (b) 승인 필요 | **(b)** | (a)는 승인 없는 environment 에 배포 키가 있어야 해서 우회 경로가 된다. 자동 롤백은 이미 승인된 apply job 과 VM 타이머가 처리하므로, 수동 롤백은 사람이 보는 것이 맞다 |
| DP5 | 배포 경로 | (a) GitHub 러너 → SSH(강제 명령) (b) (a) + VPN/터널 (c) **pull 기반**: VM 에이전트가 승인된 배포를 GitHub API 로 폴링해 스스로 반영 (인바운드 22 불필요) | **(a)로 시작, 2단계 (c)** | (a)가 단순하고 디버깅이 쉽다. (c)는 SSH 를 닫을 수 있고 GitOps 실무 학습 가치가 크지만, VM 이 승인 이력을 검증하는 로직이 추가된다 |
| DP6 | 시크릿 원본 관리 | (a) VM 파일 + 비밀번호 관리자 (b) sops + age 로 저장소에 암호화 | **(a)로 시작, 2단계 (b)** | 1인·단일 VM 에서 단순함 우선 [D9] |
| DP7 | 이미지 빌드 | (a) Dockerfile (b) Buildpacks | **(a)** | 레이어·사용자·JVM 옵션을 직접 다룬다 [S3][S4] |
| DP8 | CI 안의 Claude | 도입 / 보류 | **보류** | 약관 범위·secret 관리가 추가된다 |
| DP9 | 스케줄 작업 중복 방지 (Toss 작업 merge 시) | (a) ShedLock (b) 활성 슬롯에만 스케줄 켬 | **(a)** | 전환 순간 두 슬롯이 겹쳐도 안전하고, 다중 인스턴스로 확장해도 그대로 쓴다. 의존성 추가라 사람 확인 |
| DP10 | 운영 데이터 공개 범위 | (a) 진단·로그는 암호화 artifact, 요약은 집계값만 (b) 저장소를 나눠 운영 워크플로를 private 저장소로 | **(a)** | (b)는 private 저장소에서 승인 게이트가 무료가 아니다 (I1) |

---

## 16. 출처

모두 2026-10-17 에 원문을 직접 열어 확인했다 (예외는 "미확인" 표시).

| 번호 | 출처 |
|---|---|
| O1 | Oracle — Always Free Resources (A1 한도, 회수 조건, 블록 200 GB, Object Storage, 홈 리전, 용량 부족) https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm |
| O2 | Oracle — Free Tier, FAQ (체험 종료 시 A1 비활성화, 30일 유휴 계정) https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm · https://www.oracle.com/cloud/free/faq/ |
| O3 | Oracle — Security Lists / Security Rules https://docs.oracle.com/en-us/iaas/Content/Network/Concepts/securitylists.htm |
| O4 | Oracle — Ubuntu 이미지 방화벽 https://docs.oracle.com/en-us/iaas/Content/developer/apache-on-ubuntu/01oci-ubuntu-apache-summary.htm · 이미지 주의사항 https://docs.oracle.com/en-us/iaas/Content/Compute/References/images.htm |
| O5 | Oracle — Object Storage S3 호환 API https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/s3compatibleapi.htm |
| G1 | GitHub — Actions 과금 https://docs.github.com/en/billing/concepts/product-billing/github-actions · arm64 러너 GA https://github.blog/changelog/2025-08-07-arm64-hosted-runners-for-public-repositories-are-now-generally-available/ |
| G2 | GitHub — Deployments and environments (required reviewers 는 Free 에서 public 만, 승인은 job 단위, 관리자 우회 기본 허용, 브랜치 제한, 자기 승인 금지) https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments |
| G3 | GitHub — Packages 과금 / Container registry https://docs.github.com/en/billing/concepts/product-billing/github-packages · https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry |
| G4 | GitHub REST — workflows (dispatch), workflow runs (pending deployments 승인, 실행·로그 삭제) https://docs.github.com/en/rest/actions/workflows · https://docs.github.com/en/rest/actions/workflow-runs |
| G5 | GitHub — Secure use reference https://docs.github.com/en/actions/reference/security/secure-use |
| G6 | GitHub — Manually running a workflow (`--ref`) https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow |
| A1 | Claude Code — GitHub Actions https://code.claude.com/docs/en/github-actions |
| A2 | Claude Code — Legal and compliance https://code.claude.com/docs/en/legal-and-compliance |
| A3 | 카카오 — 배포 리스크 감지 에이전트 KRIS (2026-08-31) https://tech.kakao.com/posts/831 |
| A4 | Anthropic — Framework for safe and trustworthy agents https://www.anthropic.com/news/our-framework-for-developing-safe-and-trustworthy-agents · Building effective agents https://www.anthropic.com/engineering/building-effective-agents |
| A5 | OWASP — LLM06 Excessive Agency https://genai.owasp.org/llmrisk/llm062025-excessive-agency/ |
| A6 | anthropics/claude-code-action — security https://raw.githubusercontent.com/anthropics/claude-code-action/main/docs/security.md |
| S1 | Spring Boot 4.0 — Graceful shutdown https://docs.spring.io/spring-boot/4.0/reference/web/graceful-shutdown.html |
| S2 | Spring Boot 4.0 — Actuator endpoints (probes, readiness 그룹 기본 구성) https://docs.spring.io/spring-boot/4.0/reference/actuator/endpoints.html |
| S3 | Spring Boot 4.0 — Dockerfiles https://docs.spring.io/spring-boot/4.0/reference/packaging/container-images/dockerfiles.html |
| S4 | Spring Boot 4.0 Gradle plugin — OCI image https://docs.spring.io/spring-boot/4.0/gradle-plugin/packaging-oci-image.html |
| J1 | Java 21 `java` 명령 (MaxRAMPercentage) https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html |
| D1 | Docker Compose services 스펙 https://docs.docker.com/reference/compose-file/services/ |
| D2 | Docker — Packet filtering and firewalls https://docs.docker.com/engine/network/packet-filtering-firewalls/ |
| D3 | Docker — Linux post-install (docker 그룹) https://docs.docker.com/engine/install/linux-postinstall/ |
| D4 | OWASP — Docker Security Cheat Sheet https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/Docker_Security_Cheat_Sheet.md |
| D5 | Docker — Logging https://docs.docker.com/engine/logging/configure/ |
| D6 | Docker — Build best practices https://docs.docker.com/build/building/best-practices/ · Kubernetes Images https://kubernetes.io/docs/concepts/containers/images/ |
| D7 | OpenSSH sshd_config https://man.openbsd.org/sshd_config |
| D8 | Docker Compose — Use secrets https://docs.docker.com/compose/how-tos/use-secrets/ |
| D9 | sops https://github.com/getsops/sops |
| C1 | Caddy — Automatic HTTPS (`/data` 영속, IP 는 기본 내부 CA) https://caddyserver.com/docs/automatic-https |
| C2 | Let's Encrypt — 6일·IP 인증서 GA (2026-01-15) https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability |
| C3 | Caddy — command line (reload 는 admin API) https://caddyserver.com/docs/command-line |
| C4 | Public Suffix List https://publicsuffix.org/list/public_suffix_list.dat · Let's Encrypt rate limits https://letsencrypt.org/docs/rate-limits/ |
| P1 | Docker 공식 postgres 이미지 문서 https://github.com/docker-library/docs/blob/master/postgres/content.md |
| P2 | PostgreSQL 16 — Backup https://www.postgresql.org/docs/16/backup.html · pgBackRest https://pgbackrest.org/user-guide.html |
| P3 | Martin Fowler — Parallel Change / Evolutionary Database Design https://martinfowler.com/bliki/ParallelChange.html · https://martinfowler.com/articles/evodb.html |
| P4 | PostgreSQL 버전 정책 https://www.postgresql.org/support/versioning/ |
| M1 | Healthchecks.io https://healthchecks.io/pricing/ · UptimeRobot https://uptimerobot.com/pricing/ |
| M2 | Google SRE — Data integrity https://sre.google/sre-book/data-integrity/ |
| M3 | Google SRE — Monitoring distributed systems https://sre.google/sre-book/monitoring-distributed-systems/ |
| M4 | Prometheus storage https://prometheus.io/docs/prometheus/latest/storage/ · Grafana 요구사항 https://grafana.com/docs/grafana/latest/setup-grafana/installation/ |
| U1 | Ubuntu — Automatic updates https://documentation.ubuntu.com/server/how-to/software/automatic-updates/ |
| KR1 | 토스 — 결제 SDK 카나리 https://toss.tech/article/engineering-note-9 |
| KR2 | LINE 광고 서버 DevOps https://engineering.linecorp.com/ko/blog/line-ads-devops-culture |

**미확인·주의**
- A1 한도 축소의 시행일·PAYG 적용 범위는 공식 공지가 없다. PAYG 전환 시 유휴 회수 예외는 2023년 2차 출처뿐.
- Reserved 공인 IP 의 Always Free 과금 여부, public 저장소 attestation 무료 여부, 머신 계정의 GitHub 약관 범위, Backblaze B2·Cloudflare R2 무료 한도, JRE 베이스 이미지의 curl 포함 여부 — 각 단계(DEP-3~7)에서 확인한다.
- GHCR 첫 push 기본 private (K8 이전 판) 은 조사 에이전트가 확인했으나 리뷰어는 재확인하지 못했다.
