# 0001. AI 컨텍스트·문서 구조

- 날짜: 2026-10-14
- 상태: 결정
- 관련: [../proposals/context-structure-design.md](../proposals/context-structure-design.md) §7 E1~E4

## 맥락

`CLAUDE.md` 한 파일에 모든 규칙이 있고, `docs/ai/` 에 현행 지식과 끝난 작업 기록이 섞여 있었다. 쓰고 버릴 파일의 위치도 정해져 있지 않았다.

## 결정

사용자가 "단계별로 알아서 진행" 을 지시해 설계안의 추천을 그대로 채택했다.

| # | 결정 |
|---|---|
| E1 | AGENTS.md 는 지금 두지 않는다. 다른 AI 도구를 함께 쓰게 되면 AGENTS.md 를 원본으로 만들고 `CLAUDE.md` 첫 줄에 `@AGENTS.md` |
| E2 | 임시 디렉터리 이름은 `.scratch/` (gitignore) |
| E3 | `docs/ai` 재배치를 기능 개발 전에 한다 |
| E4 | `.claude/rules` 는 testing · persistence · external-client 3개부터 |

## 결과

- `docs/ai/` 를 `context/ · proposals/ · decisions/ · plans/ · archive/` 로 재배치. 파일 이름은 유지 (본문에 이름으로 언급된 곳이 많음).
- `scripts/check-doc-links.py` 로 문서 링크를 검사한다.
- `.claude/` 아래 설정·hook·rules·skills 변경은 사람이 검토해 반영한다 (에이전트가 자기 설정을 바꾸는 것은 세션 권한 검사에서 막힌다). 반영할 내용은 [../plans/active/context-structure.md](../plans/active/context-structure.md) 에 있다.
