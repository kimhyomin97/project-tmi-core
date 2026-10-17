# deployment-design 리뷰 — Round 1

> 대상: `docs/ai/proposals/deployment-design.md` 초안 (커밋 02ac313). 리뷰어: 독립 서브에이전트 (까다로운 시니어 DevOps/SRE 역할). 2026-10-17.
> 원문 확인 12건 (Oracle Always Free, GitHub Environments·REST·수동 실행, Spring Boot actuator·graceful shutdown, Docker packet filtering, Caddy, Let's Encrypt).

## 점수

| 기준 | 점수 | 한 줄 근거 |
|---|---|---|
| 정확성 | 4 | 확인한 사실 대부분 정확. 추론·구현 디테일 오류: `/readyz` 의 DB 포함, iptables INPUT 의 효과, 메모리 회수 추론, "기동 시 쓰기 없음", DP1 (c) 평가 |
| 완결성 | 2 | VM 으로 인프라 파일 전달 경로, 첫 배포 부트스트랩, 동시 실행 방지, Toss 시크릿·스케줄러, 장애 시나리오, 로그 보존, 0원이 깨지는 지점 누락 |
| 보안·AI 통제 | 2 | 사람 자격 증명 공유, 브랜치 워크플로 실행, 관리자 우회 기본 허용, 배포 키의 셸 접근, 이미지 출처 미검증, public 저장소로 운영 로그 노출 |
| 실현 가능성 | 3 | 자원·단계는 현실적. 승인 구조와 인프라 배포 경로는 설계대로 구현되지 않음 |
| 판단 가능성 | 3 | 가장 중요한 결정(AI 전용 계정, ruleset, pull 기반, 로그 공개 범위) 누락 |
| 학습 가치 | 4 | 메트릭·SLO·런북·포스트모템 부재 |

## 치명

- **C1** AI 와 사람의 자격 증명이 분리되지 않음 — AI 가 사람 토큰으로 승인 API 호출 가능, 관리자 우회 기본 허용. → AI 전용 계정(write), prevent self-review 켬, 관리자 우회 끔. DP0 로.
- **C2** `--ref <branch>` 로 브랜치의 워크플로를 실행해 승인 화면(plan 요약)을 위조 가능, environment 브랜치 제한 없음. → 모든 environment 를 main 으로 제한, ruleset + CODEOWNERS, job 안에서 ref 검증.
- **C3** 승인은 job 단위 — verify job 의 자동 롤백이 두 번째 승인에 막힘, db-change 도 별도 승인. → 배포·스모크·롤백·DDL 을 승인된 job 하나에, 또는 VM 측 dead-man's switch.
- **C4** 배포 SSH 키로 셸 접근 가능, sudo 인자 미검증, 스크립트 소유권 규칙 없음. → 모든 키에 강제 명령 게이트, 인자 허용 목록, root 소유, known_hosts 고정.
- **C5** 이미지 출처 미검증 — 어떤 브랜치든 같은 패키지에 push 가능. → 입력은 main 커밋 SHA, build provenance attestation 검증.
- **C6** public 저장소에 diagnose artifact·로그 공개 (사용자 답안·보유 내역). → 암호화 artifact, 집계값만, 공개 범위를 결정 항목으로.

## 주요

- M1 VM 으로 인프라 파일 전달 경로 없음 → root 스크립트가 승인 SHA 의 `deploy/` 를 받아 설치.
- M2 Toss 시크릿·`@EnableScheduling` 미고려, blue/green 에서 스케줄 중복 → ShedLock 또는 활성 슬롯만.
- M3 readiness 에 DB 기본 미포함, `/livez`·`/readyz` 공개 노출, 외부 체크가 Similarity 포함 경로.
- M4 Caddyfile 단일 파일 마운트·reload 실행 방식·`/data` 볼륨·`--no-deps`·슬롯 이미지 상태·restart 정책.
- M5 Docker 게시 포트는 INPUT 우회 → rules.v4 직접 관리, DOCKER-USER, Docker 기동 후 save 금지.
- M6 백업이 같은 벤더에만, 리허설과 키 원칙 충돌, 계정 종류별 무료 한도 → 2곳, 쓰기 전용 키, 승인 environment 에서 리허설.
- M7 동시 실행 방지 없음 → concurrency group + flock.
- M8 Flyway 를 앱 기동 시 실행하면 스모크 단계에서 DDL 적용 → apply job 의 step, 파괴적 DDL 플래그.
- M9 회수 추론 약함, 인스턴스 재생성 시 용량 위험 → 실측, terminate 금지.
- M10 0원·승인 게이트가 깨지는 지점(저장소 private 전환 등) 미정리 → 불변 조건.
- M11 테스트한 빌드와 배포 빌드가 다름 → CI 성공 SHA 만 release.
- M12 관측 1단계가 빈약 (메트릭 0, 로그가 컨테이너와 함께 삭제, SLO·런북·포스트모템 없음).
- M13 부트스트랩·비상 접근·장애 시나리오·RTO·재구축 리허설 없음.
- M14 Actions: write 로 실행 기록 삭제 가능 → 별도 계정, VM·저장소에도 원장.

## 경미

DP1 (c) 평가 부정확(LE IP 인증서 가능), DP5 에 pull 기반 누락, 시간대 규칙, DB 커넥션 수, healthcheck 도구·exec form ENTRYPOINT·이미지 정리 수단, unattended-upgrades 재시작 정책, 외부 스모크만 실패 시 알림 분기, SCHEMA 플래그 범위, 통제 설정을 첫 운영 배포보다 먼저.

## 반영 (개정판)

모든 치명·주요를 반영했다. 주요 변경: §1.1 불변 조건, §3 단일 apply job, §4.1 root 소유 디렉터리와 인프라 전달, §4.4 dead-man's switch, §5.3 출처 증명, §6.2~6.3 계정·GitHub 설정, §8 관측 1단계 확대·암호화 진단, §9.1 SSH 강제 명령, §11 부트스트랩·장애 시나리오, §12 0원 깨지는 지점, §15 DP0·DP9·DP10 추가.
