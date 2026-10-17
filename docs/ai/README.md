# docs/ai — 에이전트용 지식 색인

이 저장소에서 AI 에이전트(와 사람)가 참고하는 **오래 가는 지식**의 원본(system of record)이다.
루트 `CLAUDE.md` 는 이 색인을 가리키는 지도만 갖고, 상세는 여기서 필요할 때 읽는다.
구조의 근거: [proposals/context-structure-design.md](proposals/context-structure-design.md)

| 위치 | 상태 | 내용 |
|---|---|---|
| [context/current-state.md](context/current-state.md) | 현행 | 코드 현황, 결함 F1~F10, ArchUnit 위반 목록, 빌드·프로브 결과 |
| [context/research.md](context/research.md) | 현행 | 외부 근거 (Spring 동작, 도구 버전, AI 루프) |
| [proposals/decisions-needed.md](proposals/decisions-needed.md) | 결정 대기 | 사람이 정할 결정 D1~D13 (중요도 순, 추천안 포함) |
| [proposals/architecture-proposal.md](proposals/architecture-proposal.md) | 제안 | 구조 A′/B/C 비교, 추천 B |
| [proposals/conventions-proposal.md](proposals/conventions-proposal.md) | 제안 | 네이밍·예외·DTO·트랜잭션·로깅·테스트 컨벤션 |
| [proposals/ai-workflow.md](proposals/ai-workflow.md) | 제안 | 작업 단위, 사람 확인 지점, feature_list 루프, hook |
| [proposals/context-structure-design.md](proposals/context-structure-design.md) | 적용 완료 | 이 디렉터리·`.claude/` 구조 설계 |
| [proposals/deployment-design.md](proposals/deployment-design.md) | 결정 대기 | 클라우드 배포 설계 (OCI 무료 VM, GitHub Actions, AI 운영자 + 사람 승인 1회), 결정 DP0~DP10 (리뷰 3회 반영) |
| [reviews/](reviews/) | 참고 | 설계 문서 독립 리뷰 기록 (라운드별) |
| [proposals/examples/](proposals/examples/) | 참고 | 답안 채점을 A′/B/C 로 재구성한 예시 (빌드 대상 아님) |
| [decisions/](decisions/) | 현행 | 결정된 사항 (1건 = 1파일) |
| [plans/active/](plans/active/) · [plans/completed/](plans/completed/) | 현행 | 기능·리팩터링별 실행 계획 |
| [blockers.md](blockers.md) | 현행 | 환경 문제와 우회 방법 |
| [feature_list.json](feature_list.json) | 현행 | 루프 작업 목록. 에이전트는 `passes`·`blocked`·`awaiting_human` 만 수정 (ai-workflow §4.1) |
| [progress.md](progress.md) | 현행 | 세션별 진행 기록 (추가만) |
| [feature_list.example.json](feature_list.example.json) | 보관 예정 | 초안. `feature_list.json` 과 같은 내용 |
| [archive/](archive/) | 보관 | 끝난 작업 기록. 현행 규칙으로 읽지 않는다 |

규칙
- 결정이 나면: `proposals/decisions-needed.md` 의 해당 항목에 "결정됨 → decisions/NNNN-…md" 를 적고, `decisions/` 에 결과를 남긴다.
- 계획이 끝나면: `plans/active/` → `plans/completed/` 로 옮기고 회고 3줄을 붙인다.
- 문서를 옮기거나 지우면: `python3 scripts/check-doc-links.py` 로 깨진 링크가 없는지 확인한다.
