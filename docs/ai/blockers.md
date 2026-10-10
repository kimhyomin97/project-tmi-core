# 막힌 지점·환경 이슈 (blockers)

> 작업 중 만난 환경·접근 문제와 우회 방법. 코드 결함은 [current-state.md](./current-state.md) 에 있다.

---

## 1. 미커밋 WIP 변경 (작업 트리)

| 항목 | 내용 |
|---|---|
| 현상 | 저장소 수령 시 `src/main` 4개 파일(Java 3 + `application.properties`) + `.gitattributes` 에 미커밋 변경이 있었고, 그중 2개 파일이 **존재하지 않는 클래스**(`toss.holdings.infra.TossHoldingsClient`, `toss.holdings.dto.HoldingsApiResponse`)를 import → 컴파일 불가 |
| 제약 | 작업 규칙상 `src/main` 수정 금지 → 커밋도, 되돌림도 하지 않음 |
| 조치 | ① 패치 전문을 [`wip-snapshot.patch`](./wip-snapshot.patch) 로 보존 (`git apply --check` 통과) ② 빌드·테스트 실행 동안 `git stash` 로 치움 ③ 작업 종료 시 **`git apply docs/ai/wip-snapshot.patch`** 로 작업 트리에 복원. `git stash pop` 은 쓰지 않는다 — stash 에 이미 커밋된 `.claude/*`·`docs/ai/TASK.md` 가 섞여 충돌한다 (reviews/current-state-round-1.md M9) |
| 현재 위치 | 마지막 커밋 직후 `git apply docs/ai/wip-snapshot.patch` 로 **작업 트리에 미커밋 상태로 복원함** (push 는 §4 사유로 불가). `git stash`(stash@{0}) 에도 사본이 남아 있다. 복원 상태에서는 WIP 의 import 때문에 `./gradlew build` 가 컴파일 실패한다 — 빌드하려면 `git stash push -- src/main .gitattributes` 로 다시 치운다 |
| 사람이 할 일 | WIP 를 이어서 완성할지, 브랜치로 분리할지 결정. Toss 클라이언트 위치는 architecture-proposal.md 결정(D1)에 따름 |

## 2. `./gradlew build` 환경

| # | 현상 | 원인 | 조치 | 소요 |
|---|---|---|---|---|
| 1 | `./gradlew: Permission denied` | `gradlew` 가 git 에 `100644` 로 저장됨 (`git ls-files -s gradlew`) | `bash ./gradlew ...` 로 실행. 저장소는 수정하지 않음 (`git update-index --chmod=+x gradlew` 는 사람 결정 사항) | 1분 |
| 2 | 의존성 해석 실패 `Received status code 429 from server: Too Many Requests` (`repo.maven.apache.org`) | 세션 프록시 경유 Maven Central 레이트 리밋 (간헐) | 저장소 밖 `~/.gradle/init.d/mirror.gradle` 로 Google Cloud 미러 `https://maven-central.storage-download.googleapis.com/maven2/` 지정. **`build.gradle` 변경 없음** | 약 10분 |
| 3 | `CoreApplicationTests.contextLoads` 실패 — `PSQLException: Connection refused` | `@SpringBootTest` + `ddl-auto=validate` 가 `localhost:15432` PostgreSQL 필요 | 컨테이너 내 PostgreSQL 16 바이너리로 `localhost:15432` 기동, DB `tmi_word` 생성, `docs/public/init.sql` 적용 (아래 명령) | 약 5분 |
| 4 | Docker 데몬 없음 — `docker info` → `failed to connect to the docker API at unix:///var/run/docker.sock` (CLI·`dockerd` 바이너리는 있음) | 클라우드 세션 환경 | 우회 없음. Testcontainers 를 쓰는 `integrationTest`·`FIX-F4`(`requires: ["docker"]`)는 이 환경에서 건너뛰고 Docker 가 있는 환경에서 실행. 이 작업은 로컬 PostgreSQL(#3)로 대체 검증 | — |

재현 명령 (클라우드 세션 기준):

```bash
mkdir -p /var/tmp/pgdata && chown postgres /var/tmp/pgdata
su postgres -c "/usr/lib/postgresql/16/bin/initdb -D /var/tmp/pgdata -U postgres --auth=trust -E UTF8"
su postgres -c "/usr/lib/postgresql/16/bin/pg_ctl -D /var/tmp/pgdata -o '-p 15432 -k /var/tmp' -l /var/tmp/pg.log start"
psql -h localhost -p 15432 -U postgres -c "ALTER USER postgres PASSWORD 'postgres';"
psql -h localhost -p 15432 -U postgres -c "CREATE DATABASE tmi_word;"
psql -h localhost -p 15432 -U postgres -d tmi_word -f docs/public/init.sql
bash ./gradlew build
```

- 30분 이상 막힌 항목 없음.
- 근본 해결(테스트가 외부 DB 없이 돌게 하기)은 Testcontainers 도입 여부 결정에 달림 → [decisions-needed.md](./decisions-needed.md) D3.

## 3. 웹 리서치 접근

리서치 중 접근이 막힌 URL 은 [research.md](./research.md) 의 "접근 실패" 절에 기록한다.

## 4. push · PR 생성 불가

| 항목 | 내용 |
|---|---|
| 현상 | `git push -u origin claude/ai-dev-prep` → `fatal: 'origin' does not appear to be a git repository`. `git remote -v` 가 비어 있다. `gh auth status` → `The token in GH_TOKEN is invalid` |
| 원인 | 이 클라우드 세션의 clone 에 remote 가 설정되지 않았고, GitHub 토큰도 유효하지 않다. 저장소 owner/name 을 저장소 안에서 확인할 방법이 없다 |
| 조치 | 모든 단계를 로컬 브랜치 `claude/ai-dev-prep` 에 커밋했다. TASK.md 가 요구한 PR 본문(전체 요약 + 리뷰 라운드별 주요 개선)은 [`pr-body.md`](./pr-body.md) 로 저장했다 |
| 사람이 할 일 | `git remote add origin <url>` → `git push -u origin claude/ai-dev-prep` → `gh pr create --base main --head claude/ai-dev-prep --title "AI 기반 개발 준비: 현황 분석·제안서·ArchUnit·CLAUDE.md 초안" --body-file docs/ai/pr-body.md` |
