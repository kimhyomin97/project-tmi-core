# 막힌 지점·환경 이슈 (blockers)

> 작업 중 만난 환경·접근 문제와 우회 방법. 코드 결함은 [current-state.md](./current-state.md) 에 있다.

---

## 1. 미커밋 WIP 변경 (작업 트리)

| 항목 | 내용 |
|---|---|
| 현상 | 저장소 수령 시 `src/main` 4개 파일 + `.gitattributes` 에 미커밋 변경이 있었고, 그중 2개 파일이 **존재하지 않는 클래스**(`toss.holdings.infra.TossHoldingsClient`, `toss.holdings.dto.HoldingsApiResponse`)를 import → 컴파일 불가 |
| 제약 | 작업 규칙상 `src/main` 수정 금지 → 커밋도, 되돌림도 하지 않음 |
| 조치 | ① 패치 전문을 [`wip-snapshot.patch`](./wip-snapshot.patch) 로 보존 ② 빌드·테스트 실행 동안만 `git stash` 로 치움 ③ 작업 종료 시 `git stash pop` 으로 작업 트리에 복원 |
| 사람이 할 일 | WIP 를 이어서 완성할지, 브랜치로 분리할지 결정. 복원: `git apply docs/ai/wip-snapshot.patch` (작업 트리에 이미 복원돼 있으면 불필요) |

## 2. `./gradlew build` 환경

| # | 현상 | 원인 | 조치 | 소요 |
|---|---|---|---|---|
| 1 | `./gradlew: Permission denied` | `gradlew` 가 git 에 `100644` 로 저장됨 (`git ls-files -s gradlew`) | `bash ./gradlew ...` 로 실행. 저장소는 수정하지 않음 (`git update-index --chmod=+x gradlew` 는 사람 결정 사항) | 1분 |
| 2 | 의존성 해석 실패 `Received status code 429 from server: Too Many Requests` (`repo.maven.apache.org`) | 세션 프록시 경유 Maven Central 레이트 리밋 (간헐) | 저장소 밖 `~/.gradle/init.d/mirror.gradle` 로 Google Cloud 미러 `https://maven-central.storage-download.googleapis.com/maven2/` 지정. **`build.gradle` 변경 없음** | 약 10분 |
| 3 | `CoreApplicationTests.contextLoads` 실패 — `PSQLException: Connection refused` | `@SpringBootTest` + `ddl-auto=validate` 가 `localhost:15432` PostgreSQL 필요 | 컨테이너 내 PostgreSQL 16 바이너리로 `localhost:15432` 기동, DB `tmi_word` 생성, `docs/public/init.sql` 적용 (아래 명령) | 약 5분 |

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
