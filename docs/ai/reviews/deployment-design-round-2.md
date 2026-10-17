# deployment-design 리뷰 — Round 2

> 대상: `docs/ai/proposals/deployment-design.md` 개정판 v2 (커밋 5493e69). 리뷰어: 독립 서브에이전트 (까다로운 시니어 DevOps/SRE 역할). 2026-10-17.
> 원문 확인 15건 (GitHub environments·rulesets·attestation·workflow_run·concurrency·PAT·약관, Docker journald·compose, Oracle Always Free, 저장소 실측).

## 점수

| 기준 | 1차 | 2차 | 한 줄 근거 |
|---|---|---|---|
| 정확성 | 4 | 3 | fine-grained PAT collaborator 불가, workflow_run attestation 이 main HEAD 에 묶임, 러너→DB 경로 없음, environment 배포 이력 SHA ≠ 입력 SHA |
| 완결성 | 2 | 4 | 부트스트랩·장애 표·원장·0원 경계 추가. 앱 외 서비스 반영 경로와 사람의 비상 롤백 경로 없음 |
| 보안·AI 통제 | 2 | 2 | 구조는 맞으나 N1~N4 가 각각 단독으로 "사람 승인 1회" 또는 "승인된 코드만 배포" 를 무력화 |
| 실현 가능성 | 3 | 3 | PAT 방식, DDL 경로, concurrency, prevent self-review 교착이 설계대로 구현되지 않음 |
| 판단 가능성 | 3 | 4 | DP0·DP9·DP10 추가. 계정 구조와 AI 의 운영 로그 열람 결정 누락 |
| 학습 가치 | 4 | 5 | 출처 증명, expand/contract, dead-man's switch, SLO, 포스트모템 |

1차 지적 해소: C4·C6·M3·M4·M5·M9·M10·M12 해소, C1·C2·C3·M1·M2·M6·M7·M8·M11·M13·M14 부분, C5 미해소(형식만).

## 치명

- **N1** 클라우드 세션은 사용자(admin) GitHub 신원으로 동작 (`gh api user` 실측). AI 가 봇으로 dispatch 하고 사람 신원으로 승인하면 prevent self-review 통과, ruleset·environment 설정 변경도 가능. → I4 재작성, 통제 수준 구분, DEP-3 에 `gh api user` 실측.
- **N2** attestation 검증이 `--signer-workflow` 만으로는 main·SHA 에 묶이지 않음, 태그 `:<SHA>` 덮어쓰기 TOCTOU. → `--cert-identity …@refs/heads/main --source-ref --source-digest --deny-self-hosted-runners`, gate 가 digest 를 받고 VM 은 digest 로만 pull.
- **N3** `workflow_run` + CI artifact 넘겨받기 = artifact 오염 경로, GITHUB_SHA 가 빌드한 SHA 가 아님. → release 를 `push: main` 단일 워크플로로, 같은 job 에서 테스트·빌드·서명.
- **N4** ruleset 에 오래된 승인 무효화 없음 — 승인 뒤 AI 가 커밋을 더 올려 merge 가능. "승인 수 0 + code owner" 강제 여부는 문서에 없음. → 두 옵션 켬, DEP-3 실측, CODEOWNERS 에 gradle wrapper·`.claude/**`·`scripts/**` 등 추가.

## 주요

- N5 fine-grained PAT 는 개인 저장소 collaborator 로 사용 불가 → DP0 를 조직 이전 / GitHub App / classic PAT 선택지로.
- N6 러너에서 DB 로 가는 경로 없음 (postgres 미게시, SSH `restrict`) → VM 에서 root 가 `releases/<SHA>/db/` 적용.
- N7 외부 스모크 실패 시 동작이 세 곳에서 모순, `confirm` 이 SHA 에 안 묶임 → 결정표 하나, `confirm <SHA>`, VM 자동 롤백 시 웹훅, 영속 타이머.
- N8 승인 대기 run 이 concurrency group 을 점유해 rollback·diagnose 차단 → apply job 에만 concurrency, diagnose 는 제외, VM flock `-w`.
- N9 `deploy/bin/` 이 `.gitignore:16 bin/` 에 걸려 추적 안 됨 → 경로 변경, diagnose 로 VM 스크립트 sha256 drift 확인.
- N10 `up --no-deps app-*` 만 실행해 caddy·postgres·모니터링 변경 미반영 → `INFRA` 플래그 시 서비스 reconcile, postgres 는 정비 창.
- N11 prevent self-review + 사람 1명 + 우회 금지 → 사람이 시작한 run 은 승인 불가 → 비상 롤백은 `ops` SSH runbook 등으로 결정.
- N12 AI 기기에서 운영 로그 복호화 — 개인정보·프롬프트 인젝션·원칙 충돌, diagnose 의 sudo 누락 → VM 에서 허용 목록 마스킹, DP 로.
- N13 위험 플래그 기준 SHA 출처 불명, environment 배포 이력 SHA 부정확, 다운그레이드 허용 → Deployments API 에 입력 SHA·digest 기록, `DOWNGRADE` 기본 차단.

## 경미

`DESTRUCTIVE_DDL` 라벨은 AI 가 스스로 붙일 수 있어 차단이 아님(표시로만, 정규식 확장), journald 용량·rate limit 명시, `--wait` 는 healthcheck 만 기다림·`--env-file`, DEP-0 에 actuator·micrometer·`add-additional-paths` (의존성 추가 → 사람 확인), IP 인증서와 만료 14일 알림 충돌, workflow run 에는 코멘트 불가, restore-check 로그 위생, §16 미확인 목록 정리(머신 계정 1개·자동화 전용, attestation 은 public 에서만 무료이며 Sigstore 투명성 로그에 공개).

## 반영 (v3)

치명·주요·경미를 모두 반영했다. 주요 변경: §1.1 I4 재작성, §2 K16, §3 `/deploy` 이슈 코멘트 → operator.yml(봇 initiator) → plan → 승인 → 단일 apply, §4.1 `deploy/scripts/`, §4.4 INFRA reconcile·migrate.sh·DOWNGRADE 차단, §4.5 상태 기계 표, §5 push 트리거 release·검증 플래그·digest pull, §6.2 통제 수준 1·2, §6.3 ruleset 옵션·CODEOWNERS·DEP-3 실측 ①~⑤, §8 Grafana Alerting·journald 한도·허용 목록 진단, §11.4 사람 비상 롤백 runbook, §15 DP0·DP10 재작성.

남은 한계: 통제 수준 1(현재 사람 신원 + 하네스 차단)은 AI 가 사람 승인을 흉내 내는 것을 **완전히** 막지 못한다. 강한 보장은 통제 수준 2(봇 신원)부터다 (§6.7).
