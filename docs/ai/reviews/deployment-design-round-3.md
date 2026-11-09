# deployment-design 리뷰 — Round 3

> 대상: `docs/ai/proposals/deployment-design.md` v3 (커밋 25812a2). 리뷰어: 독립 서브에이전트 (까다로운 시니어 DevOps/SRE 역할). 2026-10-17.
> 원문 확인 7건 (GITHUB_TOKEN 이벤트, prevent self-review, 재사용 워크플로, rulesets bypass, `gh attestation verify`, claude-code-action 보안, issue_comment).

## 점수

| 기준 | 2차 | 3차 | 한 줄 근거 |
|---|---|---|---|
| 정확성 | 3 | 3 | N2·N5·N6 사실은 맞음. "재사용 워크플로를 한 job 안에서" 불가, "bot 시작자 → AI 승인 불가" 는 논리 오류 |
| 완결성 | 4 | 4 | 상태 기계·INFRA·비상 롤백 추가. 전환 후 상태 확인, 연속 rollback, infra 원복 미정의 |
| 보안·AI 통제 | 2 | 2 | 통제 수준 구분은 정직. 그러나 수준 1 의 ruleset bypass 가 CODEOWNERS·승인 화면 무결성을 무력화, operator 의 작성자 검증 없음 |
| 실현 가능성 | 3 | 3 | release 단일 job 불가, plan 이 VM 원장을 읽을 경로 없음, claude-code-action 의 bot 차단 |
| 판단 가능성 | 4 | 4 | 수준 1 의 보증 범위를 과대평가 |
| 학습 가치 | 5 | 5 | |

Round 2 판정: N2·N5·N6·N8·N9·N11·N12 해소, N1·N3·N4·N7·N10·N13 부분. 경미 항목은 모두 해소.

## 치명

- **R3-C1** 수준 1 에서 ruleset bypass(개인 저장소에선 사실상 Repository admin 역할)는 admin 신원의 AI 가 `gh pr merge --admin` 으로 `deploy.yml`·compose·`.claude/` 를 무리뷰 merge 하는 경로 → 승인 화면 위조 재발. → bypass 비움, 하네스에 merge·ruleset 차단, 사람의 인프라 PR 은 웹 UI 에서 bypass 를 잠시 켜는 runbook, 수준 2 를 DEP-5 전으로.

## 주요

- R3-M1 bot 시작자는 승인 교착 회피 장치일 뿐 AI 승인을 막지 않음 (시작자가 bot 이면 막히는 사람이 없다). → 서술 정정, AI 승인 차단은 하네스 규칙뿐이라고 명시.
- R3-M2 재사용 워크플로는 job 수준에서만 호출 가능 → `test` job → `build` job(`needs`), `push-to-registry: true`.
- R3-M3 기준 배포를 Deployments API 에서 읽으면 자동 생성 기록과 섞이고 admin 신원이 가짜 success 를 만들 수 있음 → 기준은 VM 원장(baseline job 의 `status`), Deployments 는 감사용·creator/run_id 필터, 하네스에 `deployments` 차단.
- R3-M4 상태 기계 구멍: 전환 후 내부 상태를 볼 명령 없음, INFRA 변경의 외부 실패를 엣지로 오판, plan 은 SSH 불가, 연속 rollback 이 나쁜 버전으로 복귀, `current`·infra 미원복, 7~9 사이 장애 시 타이머 미동작 → `status <SHA>`, INFRA 시 엣지 판정 금지, CAS `rollback <expected-active-SHA>`, 전환 전 pending-confirm.
- R3-M5 operator 에 요청자 검증·입력 위생 없음 (외부인의 `/deploy` 남발로 대기 배포 취소 DoS, `run:` 인젝션) → `author_association == 'OWNER'`, SHA 를 env + 정규식, 권한 `actions: write` 만.

## 경미

해설자 위치 통일(별도 `explain` job), claude-code-action 의 `allowed_bots`, job summary 조회 API 없음 → 태그→digest 해석 + 출처 검증, release 권한에 `contents: read`, 승인 기록의 클라이언트 출처는 확인 불가 → 감사 방식 변경, "또는 일정" 삭제, DOWNGRADE 의 동일 SHA 처리, 하네스 hook 자기 수정 위험, VM 의 OCI attestation 검증은 레지스트리 인증 필요.

## 반영 (v4)

치명·주요·경미를 모두 반영했다. 주요 변경: §3 baseline/plan/explain/apply 구조와 operator 검증, §4.4 전환 전 pending-confirm·`status`·CAS rollback, §4.5 INFRA 시 엣지 판정 금지·기준 = VM 원장, §5.2 두 job, §6.2 수준 1 의 보증 범위 정정·차단 목록 확장·수준 2 를 DEP-5 전으로, §6.3 bypass 비움·실측 ⑥, §6.7 위험 3건 추가, §9.1 gate 명령, §11.4 bypass runbook, §14 DEP-3b, DP0 추천 변경.

남은 한계: 수준 1 기간(DEP-0~4)의 통제는 여전히 정책 수준이다. 이 기간에는 배포 자동화가 없으므로 운영 영향은 인프라 PR 의 리뷰 품질로 한정된다 (추론).
