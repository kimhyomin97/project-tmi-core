# 배포 설계 (Oracle Cloud 무료 VM + GitHub Actions + AI 운영자)

> 상태: **제안 — 미확정**. §15 의 결정을 정하면 §14 순서대로 반영한다.
> 조사일 2026-10-17. 근거는 [§16 출처](#16-출처) 의 번호. "확인" = 원문 직접 확인, "(추론)" = 확인한 사실에서 끌어낸 판단, "(미확인)" = 원문으로 확인하지 못함 → 해당 단계의 검증 항목으로 둔다.
> 범위: TMI Core API + PostgreSQL. 외부 Similarity 서버는 범위 밖 (`SIMILARITY_BASE_URL` 로만 연결).
> 개정 이력:
> - 1차 리뷰([../reviews/deployment-design-round-1.md](../reviews/deployment-design-round-1.md)): AI 전용 계정, environment 브랜치 제한, 승인 1회 안의 배포·롤백, SSH 강제 명령, 빌드 출처, 운영 로그 비공개, 부트스트랩·장애·관측 확대.
> - 2차 리뷰([../reviews/deployment-design-round-2.md](../reviews/deployment-design-round-2.md)): **AI 세션이 사람(admin) 신원을 쓰는 문제(N1)를 통제 수준으로 정리**, 출처 검증을 main·SHA·digest 에 고정(N2), `workflow_run` 제거(N3), ruleset 의 오래된 승인 무효화(N4), 계정·토큰 방식 재정리(N5), DDL 을 VM 에서 적용(N6), 배포 상태 기계 단일화(N7·N8·N13), `deploy/bin` 경로 문제(N9), 앱 외 서비스 반영(N10), 사람의 비상 경로(N11), AI 의 로그 열람 범위(N12).
> - 3차 리뷰([../reviews/deployment-design-round-3.md](../reviews/deployment-design-round-3.md)): **수준 1 에서 ruleset bypass 를 비움**(R3-C1 — admin 신원의 AI 가 `--admin` merge 로 CODEOWNERS 를 우회), bot 시작자의 효과 정정(R3-M1), release 를 test/build 두 job 으로(R3-M2), 기준 배포를 VM 원장에서 읽음(R3-M3), `status`·CAS `rollback`·전환 전 pending-confirm(R3-M4), operator 의 요청자 검증·입력 검증(R3-M5).

---

## 0. 한 장 요약

```
 operator 워크플로 (GitHub Actions 안, 시작자 github-actions[bot])   사람 (kimhyomin97)
  사람이 이슈에 "/deploy <SHA>" ──▶ operator 워크플로                   ▲
  (Claude: 변경 해설만, 도구 제한)     │ workflow_dispatch (GITHUB_TOKEN, actions:write)
                                      ▼                                │ "Approve and deploy" (1회)
 GitHub (public 저장소)          deploy.yml (main 의 워크플로만)         │
  release.yml: push(main) → 테스트 → arm64 빌드 → push → 출처 증명      │
                                plan  : 기준 배포와 비교한 위험 플래그 ─┘
                                apply : environment=production — 승인 후에만 SSH 키
                                        출처 검증(main·SHA·digest) → SSH "deploy <SHA> <digest>"
                                        → 외부 스모크 → 상태 기계(§4.5)대로 confirm / rollback
                                                 │
 Oracle Cloud Always Free — A1 VM 1대 (ARM64, 2 OCPU / 12 GB, Ubuntu)
 ┌───────────────────────────────────────────────────────────────────────────────┐
 │ ssh-gate (허용 명령만) → root 스크립트: 승인 SHA 의 deploy/ 설치 → (DDL) →        │
 │ blue/green 전환 → 원장 기록 · 확인 타이머(미확인 시 자동 원복 + 알림)            │
 │ Caddy :80/:443 ─▶ app-blue | app-green   PostgreSQL(내부망)                      │
 │ 백업 → 암호화 → 오프사이트 2곳     node_exporter · Prometheus · Grafana(127.0.0.1)│
 └───────────────────────────────────────────────────────────────────────────────┘
 VM 밖: 외부 업타임 체크 · 백업 생존 신호 · 알림 웹훅
```

원칙:

1. **벤더 중립** — VM + Docker Compose + cloud-init + S3 호환 API 만. 옮길 때 바뀌는 것은 VM 생성과 VCN 방화벽 규칙뿐 (R2).
2. **0원** — public 저장소의 GitHub Actions·Environments·GHCR·attestation, Oracle Always Free, 사용자의 Claude 구독. 깨지는 지점은 §12.
3. **AI 는 시작·해설, 사람은 승인 1회, 실행은 스크립트** — 권한과 신원으로 강제한다 (§6). 통제의 강도는 AI 가 어느 신원으로 도는지에 달려 있으므로 수준을 나눠 명시한다 (§6.2).
4. **승인된 것만 배포된다** — 입력은 main 커밋 SHA + 그 커밋을 main 의 release 워크플로가 빌드했다는 출처 증명이 있는 digest (§5.3).
5. **실패해도 혼자 돌아오고, 돌아오면 알린다** — 상태 기계 하나로 정의한다 (§4.5).

---

## 1. 목표와 제약

| # | 요구 | 출처 |
|---|---|---|
| R1 | 비용 0원 (Oracle Always Free 범위 안) | 사용자 |
| R2 | 클라우드 고유 기능에 종속되지 않는다 — VM 구조만 사용 | 사용자 |
| R3 | AI 가 배포 실행까지, 사람 승인 1회 | 사용자 |
| R4 | 테스트는 로컬에서도 돈다 (운영과 같은 compose 로 로컬 기동) | 사용자 |
| R5 | 국내 IT 기업 실무에 가까운 운영 역량 — 무중단 배포·롤백·백업과 복구·관측과 SLO·보안·장애 대응 | 사용자, `docs/study/be-standards/06` |
| R6 | Similarity 서버는 범위 밖 | 사용자 |

비목표: 다중 VM·쿠버네티스·오토스케일링·멀티 리전 (확장 방향만 §14 끝에).

### 1.1 불변 조건

| # | 조건 | 깨지면 |
|---|---|---|
| I1 | **저장소는 public** | Free 플랜에서 environment 의 required reviewers·environment secrets·artifact attestation 이 사라진다 → 승인 게이트·출처 증명 소멸 [G2][G7] |
| I2 | **GHCR 패키지는 public** | VM 이 자격 증명 없이 pull 할 수 없다 |
| I3 | **운영 SSH 키는 승인이 필요한 environment 에만** | 승인 없는 워크플로가 VM 에 접근 |
| I4 | **배포를 승인할 수 있는 신원(사람 계정)의 자격 증명이 AI 실행 환경에 없다** — 이것이 지켜지는 정도가 통제 수준이다 (§6.2) | AI 가 승인 API 를 호출할 수 있다 |
| I5 | **Oracle A1 은 2 OCPU / 12 GB 를 넘지 않는다** | 과금 또는 체험 종료 시 A1 전체 비활성화 [O1][O2] |

---

## 2. 설계를 바꾼 조사 결과

| # | 사실 | 설계 영향 | 근거 |
|---|---|---|---|
| K1 | Oracle A1 무료 한도 **2 OCPU / 12 GB** (월 1,500 OCPU-시간·9,000 GB-시간). 블로그 다수가 아직 4/24 | 자원 예산 12 GB (§4.2) | [O1][O2] 확인 |
| K2 | 유휴 회수: 7일간 CPU p95·네트워크·메모리(A1) **모두** 20% 미만 | 실측, 필요 시 상시 사용량 확보 (§4.2) | [O1] 확인 |
| K3 | VCN 보안 규칙과 OS 방화벽 둘 다. OCI Ubuntu 는 iptables 기본 규칙, UFW 쓰지 않음 | §9.2 | [O3][O4] 확인 |
| K4 | Always Free 는 홈 리전에서만, A1 은 춘천 예외. 용량 부족으로 생성이 막힐 수 있음 | 홈 리전 서울, terminate 금지 (§11.2) | [O1] 확인 |
| K5 | Environments: Free 는 public 만. 승인은 **job 단위**. 승인 전 secret 접근 불가. 관리자 우회 기본 허용(끌 수 있음). 브랜치 제한 가능. **prevent self-review 를 켜면 시작한 사람은 승인할 수 없다** | 승인 1회 = apply job 1개. 시작자는 사람이 아닌 신원 (§6.2). 사람의 비상 경로는 별도 (§11.4) | [G2] 확인 |
| K6 | 배포 시작은 Actions: write, 승인은 Deployments: write. Actions: write 로 실행 기록·로그 삭제 가능 | 원장을 VM 과 Deployments API 에 남긴다 (§4.5) | [G4] 확인 |
| K7 | `workflow_dispatch` 는 `--ref` 로 다른 브랜치의 워크플로를 실행할 수 있다 | environment 브랜치 = main + job 안 검증 | [G6] 확인 |
| K8 | `workflow_run` 은 앞 워크플로 결과와 무관하게 실행되고 write 토큰·secret 에 접근하며, GITHUB_SHA 는 기본 브랜치의 최신 커밋이다 — 신뢰할 수 없는 코드를 다루면 캐시·artifact 오염 위험 | release 는 `workflow_run` 을 쓰지 않는다 (§5.2) | [G8] 확인 |
| K9 | `gh attestation verify` 의 `--signer-workflow` 는 서명 워크플로 경로만 강제한다. ref·SHA 는 `--source-ref`·`--source-digest`·`--cert-identity` 로 따로 고정해야 한다 | 검증 플래그 고정 (§5.3) | [G9] 확인 |
| K10 | fine-grained PAT 는 **다른 사람 개인 저장소의 collaborator** 로 쓸 수 없다. 머신 계정은 사람이 책임지는 조건으로 **무료 1개** 허용 | 계정·토큰 선택지 (§6.2, DP0) | [G10][G11] 확인 |
| K11 | rulesets 는 Free 플랜 public 저장소에서 쓸 수 있다. "오래된 승인 무효화"·"최신 push 승인 필요" 는 선택 옵션이다. **승인 수 0 + code owner 리뷰 필수** 조합의 강제 여부는 문서에 명시 없음 | 옵션을 켜고 DEP-3 에서 실측 (§6.3) | [G12] 확인 / 조합은 미확인 |
| K12 | concurrency group 에서는 대기 중 실행이 하나만 허용되고 추가 대기 실행은 이전 것을 취소한다 | concurrency 는 apply job 에만, 직렬화는 VM 의 flock (§3) | [G13] 확인 |
| K13 | Spring Boot 4 graceful shutdown 기본(유예 30초), Compose `stop_grace_period` 기본 10초. readiness 그룹에 DB 는 기본 미포함. `/livez`·`/readyz` 는 `probes.add-additional-paths=true` 가 있어야 메인 포트에 생긴다 | §4.3, §4.4 | [S1][S2][D1] 확인 |
| K14 | Docker 게시 포트는 INPUT 규칙을 우회. docker 그룹은 root 와 같다 | §9 | [D2][D3] 확인 |
| K15 | Caddy 는 `/data` 영속 필요, reload 는 admin API | §4.4 | [C1][C3] 확인 |
| K16 | **이 프로젝트의 Claude Code 클라우드 세션은 사용자(admin)의 GitHub 신원으로 동작한다** (2차 리뷰에서 `gh api user` 실측) | AI 개발 세션이 승인 API 를 부를 수 있다 → §6.2 의 통제 수준 | 리뷰 실측 |

---

## 3. 한 번의 배포 — 흐름

| 단계 | 누가 | 무엇을 | 남는 기록 |
|---|---|---|---|
| 1 | AI (개발 세션) | 기능 PR → CI → merge (인프라 경로는 사람 리뷰 필수, §6.3) | PR |
| 2 | `release.yml` (`on: push: branches: [main]`) | 같은 실행 안에서 `test` job(ci 재사용 워크플로) → `build` job(`needs: test`): arm64 빌드 → GHCR push(태그 = SHA) → 출처 증명 (§5.2) | 이미지, digest, attestation |
| 3 | **사람** | 이슈에 `/deploy <SHA>` 코멘트 (AI 개발 세션이 SHA·근거를 준비해 제안 → 사람이 코멘트) → `operator.yml` 이 **작성자 검증**(job `if: comment.author_association == 'OWNER'`) → SHA 를 `env` 로 받아 `^[0-9a-f]{40}$` 검증 (본문을 `run:` 에 직접 넣지 않는다) → `GITHUB_TOKEN`(권한 `actions: write` 만) 으로 `deploy.yml` 을 dispatch. 시작자 = github-actions[bot] | dispatch 기록 |
| 4 | `baseline` job (environment `production-readonly`) → `plan` job (비밀값 없음) → `explain` job | baseline: SSH `status` 로 **VM 원장의 현재 활성 SHA**(= 기준 배포, §4.5) 조회 / plan: ref == main, SHA ⊂ main 확인 → `:<SHA>` 태그를 digest 로 해석하고 출처 검증(§5.3)으로 그 digest 를 확정 → 기준과 비교해 위험 플래그(§6.4) → job summary / explain: Claude 가 플래그를 3줄로 해설해 이슈에 코멘트 (도구 제한, §6.5) | 승인 화면 요약 |
| 5 | **사람** | 요약을 보고 **Approve and deploy** / Reject. 시작자(bot) ≠ 승인자(사람) 라 prevent self-review 가 켜진 채 승인할 수 있다 | 승인 기록 |
| 6 | `apply` job (environment `production`) | ref·SHA 재검증 → **출처 검증**(§5.3) → Deployments API 에 배포 기록 생성(입력 SHA·digest) → SSH `deploy <SHA> <digest>` → SSH `status <SHA>`(전환 후 내부 상태) → 외부 스모크 → 상태 기계(§4.5)대로 SSH `confirm <SHA>` 또는 `rollback <SHA>` → 기록 상태 갱신 | 실행 로그, Deployments 기록, VM 원장 |
| 7 | AI | 결과를 읽고 `docs/ai/deploys/<날짜>-<짧은 SHA>.md` 기록 PR (설명용 — 감사 근거는 VM 원장과 Deployments 기록) | 배포 설명 |

- 사람의 일은 **3단계의 요청(코멘트)과 5단계의 승인 1회**다. AI 개발 세션이 요청 문구까지 준비하므로 사람은 붙여넣기만 한다.
- 시작자를 bot 으로 두는 이유는 **승인 교착을 피하기 위해서**다. 사람이 시작하면 prevent self-review 때문에 그 사람(유일한 승인자)이 승인할 수 없다 (K5). 이 장치는 AI 의 승인을 막지 않는다 — 시작자가 bot 이면 막히는 사람이 없으므로, 사람 신원을 가진 AI 도 승인 API 를 부를 수는 있다. **수준 1 에서 AI 의 승인을 막는 것은 하네스 규칙뿐이다** (§6.2). 수준 2 에서는 AI 가 봇 신원으로 직접 dispatch 하고, 봇에는 승인 권한이 없다.
- 동시 실행: `concurrency: { group: prod-apply, cancel-in-progress: false }` 는 **apply job 에만** 건다 (승인 대기 중인 plan 이 다른 워크플로를 막지 않게, K12). VM 의 모든 쓰기 작업은 `flock -w 300 /opt/tmi/state/lock` 으로 직렬화하고, 300초 안에 잠금을 못 얻으면 이유를 출력하고 실패한다. 읽기 전용 `diagnose` 는 잠금을 쓰지 않는다.

---

## 4. 런타임 구성 (VM 안)

### 4.1 디렉터리와 소유권

```
/opt/tmi/                         root:root 0755 — deploy 사용자는 쓸 수 없다
├── scripts/                      ssh-gate, deploy.sh, migrate.sh, rollback.sh, backup.sh, diagnose.sh (root 0755)
├── releases/<SHA>/deploy/        승인된 커밋의 deploy/ 사본 (root 소유)
├── current -> releases/<SHA>     심볼릭 링크 (원자적 전환)
├── state/                        root 0700 — active 슬롯, 슬롯별 env 파일, ledger.jsonl, pending-confirm, lock
├── secrets/                      root 0700 — DB·외부 API 비밀값 파일
└── caddy/                        root — Caddyfile 디렉터리 (컨테이너에 디렉터리로 마운트)
```

- 저장소 경로는 `deploy/scripts/` 다. `deploy/bin/` 은 `.gitignore` 의 `bin/` 규칙에 걸려 커밋되지 않는다 (2차 리뷰 N9 실측).
- **인프라 파일 전달**: `deploy.sh`(root)가 승인된 SHA 의 `deploy/` 를 public 저장소에서 받아 `releases/<SHA>/` 에 설치하고 `current` 를 바꾼다. SHA 가 `origin/main` 의 조상인지 확인한다.
- **스크립트 자체**(`/opt/tmi/scripts/`)는 부트스트랩 때 cloud-init 으로 설치하고, 갱신은 사람이 runbook 으로 한다 — 배포 경로로 "배포를 실행하는 코드"를 바꾸지 않는다. 대신 `diagnose` 가 VM 스크립트의 sha256 과 main 의 `deploy/scripts/` sha256 을 비교해 **불일치(drift)를 알린다**.

### 4.2 컨테이너와 자원 (A1 2 OCPU / 12 GB)

| 서비스 | 이미지 | 게시 포트 | 메모리 상한 | 비고 |
|---|---|---|---|---|
| `caddy` | `caddy:2` (digest 고정) | **80, 443** | 0.2 GB | `/data`·`/config` named volume, 설정은 디렉터리 마운트 |
| `app-blue` / `app-green` | `ghcr.io/kimhyomin97/tmi-core@sha256:…` | 없음 | 각 1.5 GB | 전환 순간 둘 다 → 3 GB |
| `postgres` | `postgres:16` (digest 고정, DP2) | 없음 | 3 GB | `/var/lib/postgresql/data` named volume, `shm_size: 256m`, `max_connections=50` [P1] |
| `node_exporter`, `prometheus`, `grafana` | digest 고정 | 없음 (Grafana 127.0.0.1) | 1.5 GB | §8 |
| OS·캐시·여유 | | | 약 3.5 GB | |

- 공통: `restart: unless-stopped`, non-root, `no-new-privileges`, `cpus` 제한, Docker 소켓 마운트 금지 [D4].
- 네트워크 `edge`(caddy·app), `data`(app·postgres), `mon`(모니터링·app 메트릭). postgres 는 `edge` 에 없다.
- Hikari `maximum-pool-size=10` × 2 슬롯 + 관리 여유 ≤ 50.
- 시간대: VM·컨테이너·로그·cron 은 UTC. 업무 규칙만 `Asia/Seoul` 명시.
- **유휴 회수(K2)**: 평시 실사용량은 20% 미만일 수 있다 (추론). 첫 2주는 OCI 콘솔 지표로 확인하고, 필요하면 `-Xms` = `-Xmx` + `-XX:+AlwaysPreTouch` 로 상시 메모리 사용을 확보한다. 부하 생성 도구는 쓰지 않는다.

### 4.3 헬스 체크

| 경로 | 쓰는 곳 | 포함 | 외부 공개 |
|---|---|---|---|
| `/livez` | 컨테이너 healthcheck, `compose up --wait` | 앱 자체 (DB 제외 — 장애 연쇄 방지) [S2] | 아니오 |
| `/readyz` | 내부 스모크, Caddy 액티브 헬스체크 | `readinessState` + `db` (`management.endpoint.health.group.readiness.include=readinessState,db`) | 아니오 |
| `/status` (Caddy 경로) | 외부 업타임 체크 | Caddy 가 `/readyz` 를 내부 프록시, 상태 코드만 | 예 |
| `/api/v1/health` | 기존 설계 (`docs/public/07` §4) | DB·Similarity | 예 (외부 알림에는 쓰지 않음 — Similarity 는 범위 밖) |

- 필요한 설정: `spring-boot-starter-actuator`, `micrometer-registry-prometheus` 의존성 (**의존성 추가 — 사람 확인**), `management.endpoint.health.probes.add-additional-paths=true`, 노출은 `health`, `prometheus` 만. Caddy 는 `/actuator/**`, `/livez`, `/readyz` 를 외부 404.
- healthcheck 명령: 베이스 JRE 이미지에 curl 이 있는지 보장되지 않는다 (미확인) → 이미지에 curl 을 명시 설치 (DEP-1 에서 확인).

### 4.4 `deploy.sh <SHA> <DIGEST>` — 전환 절차

```
0. flock -w 300. 인자 검사: SHA = 40자리 16진수, DIGEST = sha256:64자리 16진수. 이미지 저장소 이름은 스크립트에 고정.
1. 승인 SHA 의 deploy/ 를 releases/<SHA>/ 에 설치 (SHA ⊂ origin/main 확인). current 링크는 7단계에서 전환.
2. (선택) VM 에서도 출처 재검증: gh attestation verify oci://…@<DIGEST> --bundle-from-oci … (§5.3 과 같은 플래그)
   — OCI 경로는 레지스트리 인증이 필요하다. VM 은 익명 pull 이므로 가능 여부를 DEP-5 에서 확인 (미확인).
3. 입력 SHA 가 현재 활성 SHA 의 엄격한 조상이면 거부 (DOWNGRADE — 되돌리기는 rollback 경로로만). 같은 SHA 는 재배포로 허용.
4. 앱 외 서비스 맞추기: INFRA 변경이 있으면 caddy·모니터링을 `up -d --wait <svc>` 로 반영.
   postgres 의 설정·이미지 변경은 무중단이 아니므로 여기서 하지 않는다 → 정비 절차(runbook)로 거부·안내.
5. DDL: releases/<SHA>/db/ 에 미적용 변경이 있으면 migrate.sh (VM 안에서 root 가 적용, 원장에 기록, §7.3).
6. 대기 슬롯의 env 파일(state/slot-green.env, IMAGE=…@<DIGEST>)을 쓰고
     docker compose -p tmi --env-file state/slot-green.env up -d --no-deps --wait --wait-timeout 180 app-green
   (--wait 는 /livez 만 본다) → 이어서 내부 스모크: /readyz 200 + 출제 API 1회. 실패하면 대기 슬롯 정지, 종료 1.
   성공하면 **전환 전에** pending-confirm 파일(SHA·기한 = 지금 + 15분)을 쓴다 — 7~9 사이에 끊겨도 타이머가 동작하고, 재부팅에도 남는다.
7. current 링크 전환, caddy/ 의 Caddyfile 을 원자적 교체 후 `docker compose -p tmi exec -T caddy caddy reload --config /etc/caddy/Caddyfile`.
   reload 실패 시 기존 설정 유지 → 대기 슬롯 정지, 종료 1.
8. drain 10초 → 이전 슬롯 정지 (stop_grace_period 40s).
9. state 갱신 (active, previous, current), ledger.jsonl 에 "switched" 기록.
```

- 첫 배포(active 없음): blue 로 기동하고 Caddy 는 처음부터 blue 를 가리키는 설정으로 시작 (§11.1).
- `status <SHA>` (읽기 전용, 잠금 없음): 활성 SHA, 활성 슬롯의 `/readyz`, 내부 스모크 결과, pending-confirm 상태를 출력한다. 활성 SHA 가 인자와 다르면 그 사실을 출력한다.
- `rollback <expected-active-SHA>`: **현재 활성 SHA 가 인자와 같을 때만** 동작한다 (compare-and-swap). 타이머 롤백·apply 의 롤백·사람의 SSH 롤백이 겹쳐도 두 번째는 "이미 다른 SHA" 로 거부돼, 나쁜 버전으로 되돌아가지 않는다. 되돌리는 범위는 slot·Caddy 설정·`current` 링크·4단계에서 바꾼 caddy·모니터링 서비스(이전 release 의 compose 로 `up -d --wait`)다. 이전 슬롯 컨테이너는 정지만 했으므로 빠르다. DDL 이 expand 만이었기에 성립한다 (§7.3).

### 4.5 배포 상태 기계 (외부 스모크·확인·롤백의 단일 정의)

| 상황 | apply job 이 하는 일 | VM 이 하는 일 | 알림 | 결과 기록 |
|---|---|---|---|---|
| `deploy` 가 0~8 중 실패 (내부 스모크 실패 포함) | job 실패 처리 | 활성 슬롯 그대로 (전환 전이므로) | 실패 알림 | Deployments: failure |
| 전환 후 외부 스모크 성공 | `confirm <SHA>` | pending-confirm 삭제, ledger "confirmed" | — | success |
| 전환 후 외부 스모크 실패, `status <SHA>` 의 `/readyz`·내부 스모크는 성공, **그리고 `INFRA` 플래그 없음** (= 엣지: DNS·인증서) | `confirm <SHA> --edge-issue` | 유지, ledger "confirmed-edge-issue" | **엣지 장애 알림** (롤백해도 해결되지 않으므로) | success + 경고 |
| 전환 후 외부·내부 모두 실패, 또는 외부만 실패인데 `INFRA` 플래그 있음 (Caddyfile 변경이 원인일 수 있음) | `rollback <SHA>` | previous 로 원복, ledger "rolled-back" | 롤백 알림 | failure |
| apply job 이 confirm 전에 끊김 (러너 장애·SSH 실패) | — | systemd 타이머가 1분마다 pending-confirm 확인 → **기한이 지나면 자동 `rollback <pending SHA>`** | **VM 이 직접 알림 웹훅 호출** | 다음 배포의 baseline job(`status`)과 diagnose 대조가 감지 |
| confirm 이 다른 SHA 로 옴 | — | 거부 (SHA 일치 필수) | — | — |

- "기준 배포" = **VM 원장의 현재 활성 SHA** (baseline job 이 `status` 로 읽는다). VM 원장은 root 만 쓰므로 AI 가 위조할 수 없다. AI 가 쓰는 `docs/ai/deploys/` 와 Deployments API 는 기준이 아니다 — admin 신원은 Deployments API 에 가짜 success 를 만들 수 있고, environment job 이 자동으로 만드는 deployment 기록(sha = dispatch 시점의 main HEAD, 추론)과도 섞인다.
- Deployments 기록은 **감사·표시용**이다. apply 가 `task: "deploy:tmi"` 로 입력 SHA·digest·run_id 를 기록하고, 대조할 때는 creator 가 github-actions[bot] 이고 run_id 가 main 의 deploy.yml 실행인 기록만 쓴다. 주기적으로 `diagnose` 가 VM 원장과 대조한다 (불일치 시 알림).

---

## 5. 빌드·이미지·출처

### 5.1 이미지

| 항목 | 설계 | 근거 |
|---|---|---|
| 빌드 | 멀티스테이지 Dockerfile + `jarmode=tools extract --layers` (DP7) | [S3] |
| 베이스 | Java 21 JRE arm64 (Temurin 또는 Liberica), digest 고정 | [D6] |
| 실행 | `USER 10001`, exec form `ENTRYPOINT`, `-XX:MaxRAMPercentage=75`, curl 설치(healthcheck) | [S3][J1] |
| 라벨 | `org.opencontainers.image.source`, `…revision=<SHA>` (정보용 — 검증 근거는 attestation) | [G3] |
| 러너 | `ubuntu-24.04-arm` 네이티브 `linux/arm64` | [G1] |
| 태그 | `:<SHA>` (편의용). **배포·pull 은 항상 digest** | [D6] |

### 5.2 CI 와 release

- `ci.yml` (`pull_request`, `push`): 빌드, 단위·슬라이스, **Testcontainers 통합 테스트**(러너에 Docker 있음 → D3(a)), ArchUnit, spotlessCheck, 이미지 빌드(푸시 안 함). 시크릿 없음, `permissions: read-all`.
- `release.yml` (`on: push: branches: [main]` 만): `test` job(`uses: ./.github/workflows/ci.yml`) → `build` job(`needs: test`): 빌드 → push → `actions/attest-build-provenance`(`push-to-registry: true` — VM 의 `--bundle-from-oci` 용). 재사용 워크플로는 job 수준에서만 호출할 수 있어 두 job 으로 나눈다 [G14]. 같은 실행이라 GITHUB_SHA 가 같으므로 테스트한 커밋과 빌드한 커밋이 같고, attestation 의 소스 커밋이 그 SHA 다. `workflow_run` 은 쓰지 않는다 (K8). build job 권한: `contents: read`, `packages: write`, `id-token: write`, `attestations: write` (명시하지 않은 권한은 none).

### 5.3 출처 검증 — "main 에서, 그 SHA 로, release 워크플로가 만든 그 digest 인가"

apply job (그리고 선택적으로 VM) 에서:

```
gh attestation verify oci://ghcr.io/kimhyomin97/tmi-core@<DIGEST> \
  --repo kimhyomin97/project-tmi-core \
  --cert-identity https://github.com/kimhyomin97/project-tmi-core/.github/workflows/release.yml@refs/heads/main \
  --source-ref refs/heads/main \
  --source-digest <SHA> \
  --deny-self-hosted-runners
```

- 이렇게 하면 다른 브랜치에서 `release.yml` 을 고쳐 만든 이미지, 태그를 덮어쓴 이미지, 다른 커밋의 이미지가 모두 걸러진다 (K9).
- VM 은 태그가 아니라 **digest 로만** pull 한다. 그래서 검증한 digest 와 실행하는 digest 가 같다.
- public 저장소의 attestation 은 무료이고 Sigstore 공개 투명성 로그에 기록된다 — 빌드 정보가 공개된다는 뜻이다 (public 저장소라 이미 공개된 정보) [G7].

### 5.4 이미지 정리

- GHCR: 주 1회 `cleanup.yml` — Deployments 기록에 없는 이미지 중 최근 20개 초과분 삭제.
- VM: `deploy.sh` 끝에 current·previous 와 최근 3개 외 `docker image prune`.

---

## 6. AI 의 역할과 통제

### 6.1 원칙

1. **결정은 결정적으로, 해석은 AI 로.** 배포·롤백·백업·DDL·위험 판정은 스크립트. AI 는 작성(PR)·요청 준비·해설·분석 [A3][A4][A5].
2. **승인할 수 있는 신원과 AI 를 떼어 놓는다.** 정도에 따라 통제 수준이 정해진다 (§6.2).
3. **통제는 GitHub 설정·VM 권한·하네스 규칙으로 강제한다.** 문서의 "하지 마" 는 통제가 아니다.
4. **AI 는 운영 비밀값과 개인 데이터를 보지 않는다** (§8.2).

### 6.2 신원과 통제 수준

AI 가 어떤 신원으로 GitHub 에 접근하느냐가 승인 게이트의 실제 강도를 정한다. 지금 이 프로젝트의 Claude Code 클라우드 세션은 **사용자(admin) 신원**으로 동작한다 (K16).

| 수준 | AI 개발 세션의 신원 | 배포 시작 | 승인 게이트를 무엇이 지키나 | 비용·준비 |
|---|---|---|---|---|
| **1 (DEP-0~4 동안)** | 사람(admin) — 현 상태 | 사람이 이슈 코멘트 → `operator.yml` 이 GITHUB_TOKEN 으로 dispatch (시작자 bot — 교착 회피용) | ① GitHub: environment 승인(사람), 브랜치 제한, **ruleset bypass 비움** ② **하네스 규칙**(유일한 AI 승인 차단): AI 세션에서 승인·설정·기록 변경을 차단 (`.claude/settings.json` 의 permissions.deny + PreToolUse hook — `pending_deployments`, `environments`, `rulesets`, `branches/*/protection`, `collaborators`, `actions/secrets`, `repos/*/deployments` 쓰기, `pulls/*/merge` 와 `gh pr merge --admin`) ③ 감사: 월 1회 environment 배포 이력의 승인자·시각을 사람이 자기 기억과 대조 | 추가 계정 없음 |
| **2 (DEP-5 전까지 전환)** | **봇 신원** — 머신 계정(무료 1개, 사람 책임) 또는 GitHub App. 사람의 GitHub 연결이 없는 실행 환경(로컬의 별도 OS 사용자 등)에서 AI 를 돌린다 | AI 가 봇 신원으로 직접 dispatch | GitHub 가 강제: 봇은 write 권한뿐이고 승인 권한이 없다. 하네스 규칙은 보조 | 계정·토큰 관리, 실행 환경 분리 |

- 수준 1 의 한계: AI 세션에 admin 자격 증명이 있는 한, 하네스 규칙을 우회하는 경로(예: 다른 이름의 도구로 같은 API 호출, 세션 안에서 로컬 `.claude/` 파일을 고쳐 hook 무력화 — 추론)를 완전히 막을 수는 없다. 그래서 "AI 가 우회하지 않는다" 는 하네스 규칙 + 감사로 지키는 **정책 수준의 보증**이다. CODEOWNERS 도 수준 1 에서는 같다 — PR 작성자와 code owner 가 같은 사람 계정이라 리뷰가 "다른 사람의 확인" 이 되지 못한다. 수준 2 에서 비로소 GitHub 가 강제하는 **기술적 보증**이 된다. 그래서 **배포 자동화(DEP-5) 전에 수준 2 로 옮긴다.**
- 수준 2 의 토큰 선택 (K10):
  - (a) 저장소를 무료 **조직**으로 옮기고 봇에 fine-grained PAT (저장소 1개, Contents·Pull requests·Actions: write) — 권한이 가장 좁다
  - (b) **GitHub App** (설치 토큰, 1시간 수명) — 개인 저장소 그대로 가능
  - (c) 머신 계정 + classic PAT (`repo`, `workflow`) — 권한이 넓어져 §6.7 에 위험으로 기록
- 어느 수준이든 **운영 SSH 키·DB 비밀값은 AI 가 갖지 않는다** (I3).

### 6.3 GitHub 설정

| 설정 | 값 | 막는 것 |
|---|---|---|
| environment `production` — required reviewers | 사람 1명 | — |
| `production` — prevent self-reviews | 켬 (시작자는 bot) | 시작자의 자기 승인 (수준 2 에서 봇이 시작해도 사람만 승인) |
| `production` — allow administrators to bypass | **끔** | 관리자 우회 |
| 모든 environment — deployment branches | **`main` 만** | 다른 브랜치 워크플로의 secret 접근 (K7) |
| ruleset (main) | PR 필수, force push·삭제 금지, required status checks(ci), **code owner 리뷰 필수**, **오래된 승인 무효화**, **최신 push 승인 필요**, 일반 승인 수 0 | 인프라 경로 무리뷰 merge, 승인 후 추가 push 로 바꿔치기 (2차 N4) |
| ruleset bypass | **비움** (개인 저장소에서 bypass 대상은 사실상 "Repository admin" 역할이고 AI 세션도 그 신원이다 — 수준 1 에서 bypass 를 두면 AI 가 `--admin` merge 로 CODEOWNERS 를 우회한다) [G12] | 무리뷰 merge. 사람이 쓴 인프라 PR 은 §11.4 의 runbook |
| `CODEOWNERS` (수준 1 에서는 정책 수준, §6.2) | `.github/**`, `deploy/**`, `Dockerfile`, `.dockerignore`, `db/**`, `build.gradle`, `settings.gradle`, `gradle/**`, `gradlew*`, `.gitignore`, `.claude/**`, `scripts/**`, `CLAUDE.md` → 사람 | 배포·빌드 방식, 빌드 도구(wrapper `distributionUrl` 변경 = 러너 임의 코드 실행), AI 규칙 자체를 AI 가 바꾸고 merge |
| `apply` job 첫 step | `github.ref == 'refs/heads/main'`, SHA ⊂ main, `github.workflow_sha` 표시 | 설정 실수 이중 방어 |
| 저장소 수준 secret | 배포 관련 없음 | I3 |

- **DEP-3 실측 항목** (문서로 확인하지 못한 동작): ① 승인 수 0 + code owner 리뷰 필수가 `deploy/` 변경 PR 의 merge 를 실제로 막는가 ② 승인 후 push 하면 승인이 무효화되는가 ③ 일반 PR 은 AI 가 merge 할 수 있는가 ④ bot 이 시작한 deploy 를 사람이 승인할 수 있는가 ⑤ AI 세션에서 승인 API 호출이 하네스 규칙으로 막히는가 (수준 1) / 403 인가 (수준 2) ⑥ AI 세션에서 `gh pr merge --admin`·Deployments 기록 생성이 막히는가. ①이 막히지 않으면 일반 승인 수를 1로 올리고, AI 의 일반 PR merge 규칙(CLAUDE.md)을 "사람 승인 후" 로 바꾼다.

### 6.4 `plan` 의 위험 플래그 (결정적 규칙, 기준 = §4.5 의 기준 배포)

| 플래그 | 규칙 | 의미 |
|---|---|---|
| `SCHEMA` | `db/**`, `docs/public/init.sql`, `@Entity`·`@Embeddable`·`@Table` 이 있는 파일, `ddl-auto` 값 변경 | §7.3 확인 |
| `DESTRUCTIVE_DDL` | DDL 에 `DROP`, `TRUNCATE`, `DELETE`, `RENAME`, `SET NOT NULL`, `ALTER COLUMN … TYPE`, `CONCURRENTLY` 없는 `CREATE INDEX` | 승인 화면에 **빨간 경고**로 표시. (PR 라벨 같은 AI 가 붙일 수 있는 표식으로 통과시키지 않는다 — 판단은 승인자) |
| `INFRA` | `deploy/**`, `Dockerfile`, `.github/**` | 앱 외 서비스 반영(§4.4 4) 여부 |
| `CONFIG` | `application*.properties` 의 키 추가·삭제 | VM `secrets/` 갱신 필요 |
| `DEPS` | `build.gradle`, `gradle/**` | |
| `API` | `*Controller.java`, `*Api.java`, `docs/public/03_API명세.md` | 외부 계약 |
| `ARCHUNIT` | `com.tmi.core.architecture` 의 `@Disabled` 증가 | |
| `DOWNGRADE` | 입력 SHA 가 기준 배포의 엄격한 조상 (같은 SHA 는 재배포) | **apply 에서 거부** (되돌리기는 rollback 경로만) |

### 6.5 AI 의 역할

| 역할 | 어디서 | 신원·권한 | 하는 일 |
|---|---|---|---|
| 인프라 작성자 | 개발 세션 | 수준 1: 사람 / 수준 2: 봇 | Dockerfile·compose·워크플로·스크립트 PR. `infra-reviewer` 서브에이전트 리뷰 후 사람 리뷰 요청 (CODEOWNERS 강제) |
| 배포 해설자 | `deploy.yml` 의 `explain` job — plan 뒤, apply 와 별도 (claude-code-action, DP8) | GITHUB_TOKEN: `issues: write`, `contents: read` 만. 도구는 읽기·코멘트로 제한. 배포 secret 없음 | 위험 플래그·변경 요약을 이슈에 3줄로 해설 |
| 배포 요청 준비 | 개발 세션 (skill `deploy`) | 읽기 | 배포할 SHA 선택 근거·체크리스트를 정리해 사람에게 `/deploy <SHA>` 코멘트를 제안 (수준 2 에서는 직접 dispatch) |
| 장애 분석가 | 개발 세션 | 읽기 + `diagnose` 실행 요청 | 마스킹된 진단 결과(§8.2) 분석, 수정 PR, 포스트모템 초안 |
| 복구 리허설 담당 | 개발 세션 | `restore-check` 실행 요청 | 결과 기록 |

### 6.6 CI 안의 Claude (배포 해설자) — DP8

- 구독 OAuth 토큰(`claude setup-token`, 1년)으로 추가 비용 없이 쓸 수 있다. 약관상 개인의 일반 사용 범위이며, 이 저장소 하나에만 쓴다 [A1][A2].
- 트리거는 쓰기 권한자로 제한하는 기본값 유지 [A6]. claude-code-action 은 bot 이 시작한 실행을 기본으로 막으므로(`allowed_bots`) github-actions[bot] 시작 실행에서 동작하는지 DEP-8 에서 확인한다 (추론) [A6]. `explain` job 은 apply 와 **다른 job** 이고 production secret 이 없다.
- 쓰지 않기로 하면 plan 의 job summary 표만으로 승인한다 (해설만 빠진다).

### 6.7 남는 위험

| 위험 | 이유 | 완화 |
|---|---|---|
| (수준 1) AI 가 사람 신원으로 승인·설정 변경 | 클라우드 세션이 admin 신원 (K16) | 하네스 차단 + 승인은 웹/모바일 UI 에서만 한다는 사람 규칙 + 월 1회 감사(environment 배포 이력의 승인자·경로) → 수준 2 로 이전 |
| AI 가 일반 PR 을 merge → 버그가 main 에 | 사용자 지시("merge 까지") | CI 필수, 배포는 사람 승인 |
| Actions 실행 기록 삭제 | Actions: write 의 부수 권한 | VM 원장 + Deployments 기록 |
| (수준 2-c) classic PAT 의 넓은 권한 | `repo`·`workflow` scope | 머신 계정을 이 저장소에만 collaborator 로, 토큰 만료 90일 |
| 프롬프트 인젝션 | AI 가 PR·이슈·로그를 읽는다 | AI 권한이 승인·비밀값에 닿지 않는다 |
| (수준 1) CODEOWNERS 가 기술적 보증이 아님 | PR 작성자 = code owner = 같은 사람 계정 (K16) | bypass 비움 + 하네스의 merge 차단 + DEP-5 전 수준 2 전환 |
| (수준 1) 하네스 hook 자체를 AI 가 세션 안에서 수정 | 로컬 파일은 CODEOWNERS 밖 (추론) | hook 파일 변경을 SessionStart 에서 sha256 대조·알림, 수준 2 전환 |
| 외부인의 `/deploy` 남발 (public 저장소) | 누구나 이슈 코멘트 가능 | `author_association == 'OWNER'` 검사, 승인 없이는 아무것도 실행 안 됨 |

---

## 7. 데이터베이스 운영

### 7.1 초기화

- 데이터 볼륨이 비어 있을 때만 `docs/public/init.sql` 을 `/docker-entrypoint-initdb.d/` 로 적용 [P1]. PostgreSQL 16 은 `/var/lib/postgresql/data` 마운트 [P1].

### 7.2 백업과 복구

| 항목 | 설계 |
|---|---|
| 주기 | 일 1회 `pg_dump -Fc` (03:00 UTC), VM flock 공유 |
| 암호화 | `age` 공개키. **개인키는 VM·AI 에 없다** — 사람의 비밀번호 관리자 + `restore` environment secret |
| 저장 | 오프사이트 2곳: (1) Oracle Object Storage(S3 호환 API) (2) **다른 회사**의 S3 호환 무료 저장소 — 삭제 권한 없는 업로드 키 또는 object lock 을 지원하는 곳 (Backblaze B2 의 키 권한 분리 등. 무료 한도·기능은 DEP-7 에서 확인, 미확인) |
| 키 권한 | 업로드 키는 쓰기 전용 + 저장소 수명주기(일 7 + 주 4). VM 이 탈취돼도 백업을 지울 수 없게 |
| 한도 | Oracle 무료: Always Free 전용 계정 20 GB, 유료·체험 계정 Standard 10 GB [O1]. 덤프 크기를 원장에 기록, 70% 알림 |
| 생존 신호 | Healthchecks.io ping [M1] |
| 복구 리허설 | 월 1회 `restore-check.yml` (environment `restore`, **승인 필요**, 브랜치 main): 러너에서 백업을 받아 복호화 → 임시 PostgreSQL 에 복원 → 앱을 `ddl-auto=validate` 로 기동 → 행 수 확인 → 소요 시간을 **집계값만** 기록. `set -x` 금지, 데이터 출력 금지 (public 저장소 로그) [M2] |
| 2단계 | pgBackRest + WAL 아카이빙 (PITR) [P2] |

### 7.3 스키마 변경

- blue/green 전환 중에는 구·신 버전이 같은 DB 를 쓴다 → **expand 만 먼저, contract 는 다음 릴리스** [P3]. 이것이 이미지 롤백의 전제다.
- **DDL 은 VM 안에서 root 가 적용한다** (`deploy.sh` 5단계의 `migrate.sh`). postgres 는 게시 포트가 없고 SSH 는 포트 포워딩을 막으므로 러너에서 DB 에 닿을 경로가 없고, 만들지도 않는다 (DB 자격 증명을 environment secret 에 두지 않기 위해).
- 앱이 기동하면서 마이그레이션하지 않는다 (새 슬롯 스모크 단계에서 운영 DB 가 바뀌어 롤백이 무의미해진다).
- 방식 (DP3): (a) `db/changes/NNNN_설명.sql` + 적용 이력 표 + `psql` (b) **Flyway CLI 컨테이너**를 `migrate.sh` 가 실행 (앱 내장 아님).
- CI: 같은 PR 에 expand 와 그 contract 가 함께 있으면 실패. `DESTRUCTIVE_DDL` 은 승인 화면 경고.

---

## 8. 관측·알림·운영 체계

### 8.1 1단계 (처음부터)

| 무엇 | 어떻게 |
|---|---|
| 외부 가용성 | UptimeRobot 무료(5분) → `/status` [M3] |
| 백업 | Healthchecks.io 생존 신호 |
| 메트릭 | Micrometer → Prometheus(보존 15일) + node_exporter, Grafana(127.0.0.1, SSH 터널). 대시보드: 요청 수·에러율·p95·JVM 힙·DB 커넥션·디스크·메모리(회수 감시) [M4] |
| 알림 | **Grafana Alerting**(Prometheus 데이터소스) → Discord/Slack 웹훅 + 이메일. Alertmanager 는 두지 않는다. 규칙: 에러율, 디스크 80%, 백업 누락, 인증서 만료 14일 전(DP1 (c) 를 고르면 2일 전), 메모리 사용률 20% 미만 7일(회수 경고) |
| 로그 | Docker `journald` 드라이버 + `tag` 로 슬롯 구분, Caddy access log. journald: `SystemMaxUse=2G`, `MaxRetentionSec=14d`, `RateLimitBurst` 조정 (시스템 로그가 밀려나지 않게) [D5] |
| SLO | 가용성 99.0% / 28일 (에러 버짓 약 6.7시간). 소진 시 기능 배포 중단·안정화 우선 |

### 8.2 `diagnose` — AI 가 보는 범위

- environment `production-readonly` (승인 없음, 브랜치 main), 진단 전용 SSH 키. VM `authorized_keys` 의 모든 키는 `restrict,command="/opt/tmi/scripts/ssh-gate"` 이고, 진단 키는 gate 에서 `diagnose` 만 허용된다.
- `diagnose.sh` 는 sudo 로 실행(sudoers 에 해당 스크립트만)되며 **VM 에서 허용 목록 방식으로 걸러낸 정보만** 출력한다: 컨테이너 상태·재시작 횟수, 자원 수치, 원장 꼬리, 스크립트 sha256(drift), **오류 로그는 로거 이름·예외 클래스·건수만** (메시지 본문·요청 데이터 제외).
- 사용자 답안·Toss 보유 내역 같은 개인 데이터와 비밀값은 출력 단계에서 아예 나오지 않는다. 그래서 암호화 없이도 public artifact 로 안전하고, AI 가 읽어도 된다 (DP10).
- 원문 로그가 필요하면 사람이 `ops` 계정으로 직접 본다.

### 8.3 런북·포스트모템

- `docs/ai/runbooks/`: VM 부트스트랩, 스크립트 갱신, 정비(postgres 업그레이드 등), 배포 실패, 비상 롤백(§11.4), DB 복구, 키 교체, 인증서, VM 재구축, 계정 정지 시 이전.
- `docs/ai/postmortems/`: 장애마다 타임라인·영향·원인·재발 방지. AI 초안, 사람 확정.

---

## 9. 보안

### 9.1 SSH

| 항목 | 설계 |
|---|---|
| 계정 | `deploy` (배포·진단 키, 셸 없음), `ops` (사람 관리용, sudo). `AllowUsers deploy ops`, root 로그인 금지, 키 전용 [D7] |
| 강제 명령 | `deploy` 의 모든 키: `restrict,command="/opt/tmi/scripts/ssh-gate"`. gate 허용 목록: 배포 키 → `deploy <SHA> <DIGEST>`, `status <SHA>`, `confirm <SHA> [--edge-issue]`, `rollback <expected-active-SHA>` / 진단 키 → `diagnose`, `status` |
| sudo | gate 가 정확한 인자 형식으로만 `sudo /opt/tmi/scripts/<script>` 호출. sudoers: `env_reset`, `secure_path`, 해당 스크립트만 NOPASSWD |
| 호스트 키 | 러너는 environment secret 의 `known_hosts` 로 고정 |
| 노출 | 22 번은 러너 IP 가 고정되지 않아 넓게 열린다 → 키 전용 + 강제 명령이 주 방어, fail2ban 보조 (DP5) |
| 비상 접근 | `ops` 키 분실·sshd 장애 시 OCI 직렬 콘솔 (runbook, R2 예외: 비상용) |

### 9.2 방화벽

- VCN 인그레스: 22, 80, 443.
- 호스트: cloud-init 이 `/etc/iptables/rules.v4` 를 직접 관리 — INPUT 은 22 와 OCI iSCSI 규칙만. Docker 기동 후 `netfilter-persistent save` 금지. 80/443 은 Docker 게시 포트라 Docker 규칙으로 열린다. 컨테이너 쪽 제한은 `DOCKER-USER` 체인 [D2].

### 9.3 시크릿

- VM `/opt/tmi/secrets/*` (root 0600) → Compose `secrets:` 파일 마운트 → 앱은 Spring `configtree:` 로 읽는다. 환경 변수로 넘기지 않는다 [D8].
- 원본은 사람의 비밀번호 관리자. 2단계 sops + age (DP6) [D9].
- 현재 `application.properties` 의 평문 DB 계정 → DEP-0 에서 프로필 분리·외부 주입.

### 9.4 그 밖

- unattended-upgrades 보안 업데이트 자동, 자동 재부팅 끔(알림만), needrestart 가 Docker 를 재시작하지 않도록 예외. 이미지는 월 1회 수동 갱신 PR [U1].
- 컨테이너 non-root, no-new-privileges, 자원 제한, digest 고정. 액션은 commit SHA 고정 [D4][G5].

---

## 10. 설정과 환경

| 환경 | 어디서 | 구성 | 용도 |
|---|---|---|---|
| `local` | 개발자 PC | `deploy/compose.yaml` + `compose.local.yaml` (app 1개, 8080 직접) | 로컬 테스트·디버깅 (R4) |
| `ci` | GitHub 러너 | Testcontainers | 자동 테스트 |
| `prod` | Oracle A1 VM | `compose.yaml` + `compose.prod.yaml` | 운영 |

- staging 은 두지 않는다 (12 GB). 새 슬롯을 트래픽 없이 운영 DB 에 붙여 스모크하는 단계가 사전 검증이다. 그러려면:
  - 기동 시 DB 쓰기 금지 (DDL 은 §7.3).
  - **스케줄 작업 중복 방지** (DP9): 현재 커밋된 코드에는 `@EnableScheduling` 만 있고 `@Scheduled` 작업은 없다. 미커밋 Toss 작업(매시 보유자산 스냅샷)이 들어올 때 ShedLock(JDBC 잠금 — 잠금 테이블 DDL 과 의존성 추가 필요)을 함께 도입한다. 스모크 중인 새 슬롯도 잠금을 공유하므로 중복 실행되지 않는다. 작업은 멱등이어야 한다.
  - 외부 API 키(Toss 등)는 `secrets/`. 로컬 프로필에는 빈 기본값을 둬 키 없이도 기동되게 한다 (R4) — 현재 작업 트리의 `toss.client-id=${TOSS_CLIENT_ID}` 는 기본값이 없어 로컬 기동이 깨진다.
- 프로필: `application.properties` + `application-local.properties` + `application-prod.properties`.

---

## 11. 부트스트랩·운영 규칙·장애·비상 경로

### 11.1 첫 구축 (사람, runbook)

1. Oracle 계정(홈 리전 서울), 예산 알림, compartment quota 로 A1 상한 (I5).
2. VCN·보안 규칙, Reserved 공인 IP (무료 여부 콘솔 확인 — 미확인).
3. A1 생성 + `deploy/cloud-init.yaml` (Docker, 사용자·sshd·sudoers·iptables, `/opt/tmi`, `scripts/` 설치, journald, 확인 타이머 systemd unit). 용량 부족이면 다른 AD·시간을 두고 재시도 [O1].
4. 도메인 DNS (DP1).
5. `secrets/` 작성, GitHub 설정(§6.3), 통제 수준 1 하네스 규칙 또는 수준 2 봇 신원 (DP0).
6. 첫 배포 — active 없음 → blue, Caddy 초기 설정은 blue.

### 11.2 운영 규칙

- 인스턴스를 terminate 하지 않는다 (재생성 시 용량 부족 위험, K4). 부트 볼륨은 "인스턴스 삭제 시 보존".
- 월 1회 콘솔 로그인 (30일 유휴 계정 정지 방지) [O2] — 복구 리허설·감사와 같은 날.

### 11.3 장애 시나리오

| 시나리오 | 감지 | 대응 | 목표 |
|---|---|---|---|
| 새 버전 오류 | 내부·외부 스모크, 에러율 | 상태 기계(§4.5) 자동 롤백 | 5분 |
| apply job 중단 | VM 확인 타이머 | 기한 후 자동 원복 + VM 이 직접 알림 | 15분 |
| 엣지(DNS·인증서) 장애 | 외부만 실패 | 롤백하지 않고 알림, runbook | 1시간 |
| VM 재부팅·회수 | 외부 업타임 | 시작 → `unless-stopped` 복귀 | 30분 |
| 디스크 가득 | 80% 알림 | prune, journald vacuum, 볼륨 확장(200 GB 안) | 1시간 |
| PostgreSQL 장애·손상 | readyz 실패 | 재시작 → 최신 백업 복원 | 2시간, RPO 24시간 |
| GHCR·GitHub 장애 | 배포 실패 | 배포 연기 (서비스는 영향 없음) | — |
| 스크립트 drift | diagnose 비교 | runbook 으로 재설치 | 1일 |
| 계정 정지·VM 소실 | 외부 업타임 | 다른 VM 에 cloud-init + 백업 복원 (연 1회 재구축 리허설) | 1일 |

### 11.4 사람의 비상 경로

- prevent self-review 때문에 **사람이 직접 시작한 deploy·rollback 실행은 사람이 승인할 수 없다** (K5).
- 그래서 사람의 비상 롤백은 GitHub 워크플로가 아니라 **`ops` 계정 SSH 로 `sudo /opt/tmi/scripts/rollback.sh <현재 활성 SHA>`** (runbook) 로 한다. CAS 라 자동 롤백과 겹쳐도 한 번만 일어난다. 원장에 "manual-rollback" 으로 남는다.
- 사람이 직접 쓴 인프라 PR 은 code owner 자기 승인이 안 되므로, 사람이 **웹 UI 에서** ruleset bypass 를 잠시 켜고 merge 한 뒤 바로 끈다 (runbook — AI 세션에서는 ruleset 변경이 차단돼 있다). 수준 2 에서는 봇이 작성자이고 사람이 code owner 로 승인하므로 이 절차가 필요 없다.
- 수동 롤백 워크플로(`rollback.yml`)는 두지 않는다 — 자동 롤백은 상태 기계가, 사람의 비상 롤백은 SSH 가 맡는다 (DP4).

---

## 12. 비용 0원이 깨지는 지점

| 지점 | 예방 |
|---|---|
| 저장소 private 전환 | I1 — 승인 게이트·출처 증명도 사라진다 |
| GHCR 패키지 private | 첫 push 후 public, runbook 체크 |
| A1 한도 초과 생성, 체험 크레딧으로 큰 shape | I5, quota |
| PAYG 전환 후 한도 초과 | 예산 알림 |
| 블록 볼륨 200 GB·백업 5개 초과 | 50+50 GB 로 시작 |
| Object Storage 한도 초과 | 덤프 크기 기록, 70% 알림 |
| Reserved IP 과금 | 생성 후 확인 (미확인) |
| 도메인 구입 | DP1 (b) 선택 시 연 1~2만원 |
| Claude 를 API 키로 실행 | 구독 OAuth 만 사용 (DP8) |

---

## 13. 저장소 구조 (추가될 파일)

```
Dockerfile · .dockerignore
.github/CODEOWNERS
deploy/
├── compose.yaml · compose.local.yaml · compose.prod.yaml
├── caddy/Caddyfile.tmpl
├── cloud-init.yaml                       # scripts/ 설치, systemd 확인 타이머 포함
├── scripts/ ssh-gate deploy.sh migrate.sh rollback.sh backup.sh diagnose.sh confirm-timer.sh
└── monitoring/ prometheus.yml grafana/ (alert rules)
db/changes/  (DP3 (b) 면 Flyway 디렉터리)
.github/workflows/
├── ci.yml · release.yml · cleanup.yml
├── operator.yml                          # 이슈 코멘트 "/deploy <SHA>" → deploy.yml dispatch (GITHUB_TOKEN)
├── deploy.yml                            # plan → (해설) → apply(production)
├── restore-check.yml                     # restore environment
└── diagnose.yml                          # production-readonly
.claude/settings.json                     # (수준 1) 승인·설정 API 차단 규칙 — 사람이 반영
.claude/hooks/block-approval-api.sh       # (수준 1) PreToolUse 차단 hook
.claude/skills/deploy/SKILL.md · .claude/agents/infra-reviewer.md
docs/ai/deploys/ · docs/ai/runbooks/ · docs/ai/postmortems/
```

`docs/public/07_배포·운영.md` 는 팀 문서 — 확정 후 요약 반영 (사람 확인).

---

## 14. 단계 계획 (feature_list 항목)

| id | 내용 | 선행 | 검증 | 사람이 할 일 |
|---|---|---|---|---|
| `DEP-0` | 프로필 분리, 외부 주입, Actuator·Prometheus registry, `probes.add-additional-paths`, readiness 에 db, 운영 `show-sql` off | FIX-F1 | 단위 테스트 + 로컬 기동, `/livez`·`/readyz` 200 | 의존성 추가·계획 승인 |
| `DEP-1` | Dockerfile(curl 포함 확인) + compose 로컬 | DEP-0 | `up --wait` 후 `/readyz` 200, 정지 시 graceful 로그 | |
| `DEP-2` | `ci.yml` (Testcontainers → SIG-1 해소) | DEP-1 | PR 에서 CI 통과 | D3 결정 |
| `DEP-3` | **통제 설정**: ruleset(bypass 비움)·CODEOWNERS·environments, 수준 1 하네스 규칙, §6.3 실측 ①~⑥ | — | 실측 결과 표 | **사람** |
| `DEP-3b` | **통제 수준 2**: 봇 신원(DP0) + AI 실행 환경 분리 | DEP-3 | AI 셸에서 `gh api user` = 봇, 승인 API 403, 봇 PR 에 사람 code owner 승인 필요 | **사람** |
| `DEP-4` | Oracle VM 구축(cloud-init), 도메인, 수동 첫 기동 | DEP-1, DP1 | `https://<도메인>/status` 200 | **사람** |
| `DEP-5` | `release.yml`(출처 증명) + `operator.yml` + `deploy.yml` + VM 스크립트·확인 타이머 | DEP-2~4, DEP-3b | 의도적 실패 이미지 → 자동 롤백, apply 강제 중단 → VM 자동 원복·알림, 다른 브랜치 이미지 → 출처 검증 거부, 오래된 SHA → DOWNGRADE 거부, 롤백 2회 연속 → 두 번째 거부, OWNER 아닌 계정의 `/deploy` → 무시 | secret 등록 |
| `DEP-6` | 관측 1단계 + 알림 + `diagnose` | DEP-5 | 알림 수신, diagnose 출력에 개인 데이터 없음 확인 | 알림 계정 |
| `DEP-7` | 백업 2곳 + `restore-check` | DEP-5 | 리허설 성공·소요 시간 기록 | 저장소 계정·키 |
| `DEP-8` | skill `deploy` + 배포 해설자 + 첫 운영 배포 | DEP-5~7 | 사람 코멘트 → bot 시작 → 사람 승인 → 기록 | 승인 |
| `DEP-9` (선택) | 지표 기반 자동 롤백, pull 기반 배포(DP5 c), pgBackRest | DEP-8 | | |

확장 방향: VM 2대 + 로드밸런서, PostgreSQL 복제, k3s + GitOps.

학습 노트 후보: blue/green 과 graceful shutdown, 컨테이너 JVM 메모리, expand/contract, 승인 게이트와 신원 분리, 빌드 출처 증명, 배포 상태 기계, SLO 와 에러 버짓.

---

## 15. 결정 필요

| # | 질문 | 선택지 | 추천 | 이유 |
|---|---|---|---|---|
| DP0 | **AI 신원과 통제 수준** | (1) 수준 1: 현 상태(사람 신원) + 하네스 차단 + bot 시작 (2) 수준 2-a: 무료 조직 이전 + 봇 fine-grained PAT (2-b) GitHub App (2-c) 머신 계정 + classic PAT | **(1)로 DEP-0~4, DEP-5 전에 (2-b)** (DEP-3b) | (1)은 준비가 없지만 보증이 정책 수준이고, CODEOWNERS 도 실질 리뷰가 되지 못한다 (§6.2). 배포 자동화가 생기기 전에 옮긴다. (2-b)는 개인 저장소 그대로, 짧은 수명 토큰으로 GitHub 강제 보증. (2-a)는 권한이 가장 좁지만 저장소 이전이 필요 |
| DP1 | 도메인 | (a) 무료 DDNS (b) 구입(연 1~2만원) (c) IP + LE IP 인증서(6일) | **(a)로 시작, (b) 권장 이동** | (a) 0원·자동 HTTPS. DNS·CAA 실무 학습·가용성은 (b). (c)는 IP 변경·issuer 설정·잦은 갱신 [C1][C2][C4] |
| DP2 | PostgreSQL 버전 | 16 / 18 | **16** | 로컬·문서 기준, EOL 2028-11, 18 은 볼륨 경로 변경 [P4] |
| DP3 | 스키마 변경 | (a) `db/changes` + psql (b) Flyway CLI (VM 의 migrate.sh 가 실행) | **(b) 재검토 권장** | 적용 이력의 기계적 기록. 앱 내장이 아니라 승인된 배포 안의 단계 — "기동 시 쓰기 금지" 와 맞다. 팀 결정 변경이라 사람이 정한다 |
| DP4 | 사람의 비상 롤백 | (a) `ops` SSH runbook (b) 별도 environment 의 rollback 워크플로(자기 승인 허용, 원장의 이전 배포로 제한) | **(a)** | (b)는 승인 없는 경로에 배포 키를 둬 우회 표면이 생긴다 |
| DP5 | 배포 경로 | (a) 러너 → SSH(강제 명령) (b) + VPN/터널 (c) pull 기반 | **(a)로 시작, 2단계 (c)** | (c)는 SSH 를 닫고 GitOps 학습 가치가 크지만 VM 이 승인 이력을 검증하는 로직이 필요 |
| DP6 | 시크릿 원본 | (a) VM 파일 + 비밀번호 관리자 (b) sops + age | **(a)로 시작** | [D9] |
| DP7 | 이미지 빌드 | Dockerfile / Buildpacks | **Dockerfile** | 직접 다뤄 학습 [S3][S4] |
| DP8 | CI 안의 Claude (배포 해설자) | 도입 / 보류 | **도입** (해설 전용, 도구 제한) | 승인 화면 이해를 돕고 0원. 승인·배포 권한과 분리돼 위험이 작다. 보류하면 표만 본다 |
| DP9 | 스케줄 중복 방지 (Toss merge 시) | ShedLock / 활성 슬롯만 스케줄 | **ShedLock** | 슬롯이 겹쳐도 안전, 확장에도 그대로. 의존성·DDL 추가라 사람 확인 |
| DP10 | AI 의 운영 로그 열람 | (a) VM 에서 허용 목록 필터링한 집계·메타데이터만 (b) 원문을 암호화해 AI 가 복호화 | **(a)** | (b)는 AI 가 개인 데이터를 읽고 인젝션 표면이 커진다 |

---

## 16. 출처

모두 2026-10-17 에 원문을 직접 열어 확인했다 (예외는 "미확인").

| 번호 | 출처 |
|---|---|
| O1 | Oracle — Always Free Resources https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm |
| O2 | Oracle — Free Tier, FAQ https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm · https://www.oracle.com/cloud/free/faq/ |
| O3 | Oracle — Security Lists https://docs.oracle.com/en-us/iaas/Content/Network/Concepts/securitylists.htm |
| O4 | Oracle — Ubuntu 방화벽 https://docs.oracle.com/en-us/iaas/Content/developer/apache-on-ubuntu/01oci-ubuntu-apache-summary.htm · 이미지 https://docs.oracle.com/en-us/iaas/Content/Compute/References/images.htm |
| O5 | Oracle — Object Storage S3 호환 API https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/s3compatibleapi.htm |
| G1 | GitHub — Actions 과금 https://docs.github.com/en/billing/concepts/product-billing/github-actions · arm64 러너 GA https://github.blog/changelog/2025-08-07-arm64-hosted-runners-for-public-repositories-are-now-generally-available/ |
| G2 | GitHub — Deployments and environments https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments |
| G3 | GitHub — Packages / Container registry https://docs.github.com/en/billing/concepts/product-billing/github-packages · https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry |
| G4 | GitHub REST — workflows, workflow runs https://docs.github.com/en/rest/actions/workflows · https://docs.github.com/en/rest/actions/workflow-runs |
| G5 | GitHub — Secure use reference https://docs.github.com/en/actions/reference/security/secure-use |
| G6 | GitHub — Manually running a workflow https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow |
| G7 | GitHub — Artifact attestations (Free 는 public 만, Sigstore Public Good) https://docs.github.com/en/actions/concepts/security/artifact-attestations |
| G8 | GitHub — Events that trigger workflows (`workflow_run` 경고) https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows |
| G9 | GitHub CLI — `gh attestation verify` https://cli.github.com/manual/gh_attestation_verify |
| G10 | GitHub — Managing personal access tokens (fine-grained PAT 의 collaborator 제약) https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens |
| G11 | GitHub Terms of Service (머신 계정) https://docs.github.com/en/site-policy/github-terms/github-terms-of-service |
| G12 | GitHub — About rulesets / Available rules https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets |
| G13 | GitHub — Concurrency https://docs.github.com/en/actions/concepts/workflows-and-actions/concurrency |
| G14 | GitHub — Reuse workflows https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows |
| G15 | GitHub — GITHUB_TOKEN (workflow_dispatch·repository_dispatch 는 GITHUB_TOKEN 으로도 실행 생성) https://docs.github.com/en/actions/concepts/security/github_token |
| A1 | Claude Code — GitHub Actions https://code.claude.com/docs/en/github-actions |
| A2 | Claude Code — Legal and compliance https://code.claude.com/docs/en/legal-and-compliance |
| A3 | 카카오 — 배포 리스크 감지 에이전트 KRIS https://tech.kakao.com/posts/831 |
| A4 | Anthropic — Framework for safe and trustworthy agents https://www.anthropic.com/news/our-framework-for-developing-safe-and-trustworthy-agents · Building effective agents https://www.anthropic.com/engineering/building-effective-agents |
| A5 | OWASP — LLM06 Excessive Agency https://genai.owasp.org/llmrisk/llm062025-excessive-agency/ |
| A6 | anthropics/claude-code-action — security https://raw.githubusercontent.com/anthropics/claude-code-action/main/docs/security.md |
| S1 | Spring Boot 4.0 — Graceful shutdown https://docs.spring.io/spring-boot/4.0/reference/web/graceful-shutdown.html |
| S2 | Spring Boot 4.0 — Actuator endpoints https://docs.spring.io/spring-boot/4.0/reference/actuator/endpoints.html |
| S3 | Spring Boot 4.0 — Dockerfiles https://docs.spring.io/spring-boot/4.0/reference/packaging/container-images/dockerfiles.html |
| S4 | Spring Boot 4.0 Gradle plugin — OCI image https://docs.spring.io/spring-boot/4.0/gradle-plugin/packaging-oci-image.html |
| J1 | Java 21 `java` 명령 https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html |
| D1 | Docker Compose services https://docs.docker.com/reference/compose-file/services/ · compose up https://docs.docker.com/reference/cli/docker/compose/up/ |
| D2 | Docker — Packet filtering and firewalls https://docs.docker.com/engine/network/packet-filtering-firewalls/ |
| D3 | Docker — Linux post-install https://docs.docker.com/engine/install/linux-postinstall/ |
| D4 | OWASP — Docker Security Cheat Sheet https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/Docker_Security_Cheat_Sheet.md |
| D5 | Docker — journald logging driver https://docs.docker.com/engine/logging/drivers/journald/ |
| D6 | Docker — Build best practices https://docs.docker.com/build/building/best-practices/ · Kubernetes Images https://kubernetes.io/docs/concepts/containers/images/ |
| D7 | OpenSSH sshd_config https://man.openbsd.org/sshd_config |
| D8 | Docker Compose — Use secrets https://docs.docker.com/compose/how-tos/use-secrets/ |
| D9 | sops https://github.com/getsops/sops |
| C1 | Caddy — Automatic HTTPS https://caddyserver.com/docs/automatic-https |
| C2 | Let's Encrypt — 6일·IP 인증서 GA https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability |
| C3 | Caddy — command line https://caddyserver.com/docs/command-line |
| C4 | Public Suffix List https://publicsuffix.org/list/public_suffix_list.dat · LE rate limits https://letsencrypt.org/docs/rate-limits/ |
| P1 | Docker 공식 postgres 이미지 https://github.com/docker-library/docs/blob/master/postgres/content.md |
| P2 | PostgreSQL 16 Backup https://www.postgresql.org/docs/16/backup.html · pgBackRest https://pgbackrest.org/user-guide.html |
| P3 | Martin Fowler — Parallel Change / Evolutionary Database Design https://martinfowler.com/bliki/ParallelChange.html · https://martinfowler.com/articles/evodb.html |
| P4 | PostgreSQL 버전 정책 https://www.postgresql.org/support/versioning/ |
| M1 | Healthchecks.io https://healthchecks.io/pricing/ · UptimeRobot https://uptimerobot.com/pricing/ |
| M2 | Google SRE — Data integrity https://sre.google/sre-book/data-integrity/ |
| M3 | Google SRE — Monitoring distributed systems https://sre.google/sre-book/monitoring-distributed-systems/ |
| M4 | Prometheus storage https://prometheus.io/docs/prometheus/latest/storage/ · Grafana 요구사항 https://grafana.com/docs/grafana/latest/setup-grafana/installation/ |
| U1 | Ubuntu — Automatic updates https://documentation.ubuntu.com/server/how-to/software/automatic-updates/ |
| KR1 | 토스 — 결제 SDK 카나리 https://toss.tech/article/engineering-note-9 |
| KR2 | LINE 광고 서버 DevOps https://engineering.linecorp.com/ko/blog/line-ads-devops-culture |

**미확인 → 단계별 검증 항목**
- 승인 수 0 + code owner 리뷰 필수 조합의 강제 (DEP-3 ①), bot 이 시작한 실행의 승인 가능 여부 (DEP-3 ④).
- Reserved 공인 IP 과금 (DEP-4), JRE 베이스의 curl (DEP-1), 두 번째 오프사이트 저장소의 무료 한도·삭제 불가 키 (DEP-7).
- A1 한도 축소 시행일·PAYG 적용 범위, PAYG 전환 시 회수 예외 (공식 공지 없음).
