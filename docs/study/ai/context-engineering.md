# 컨텍스트 엔지니어링 — AI 에이전트에게 무엇을 언제 읽힐까

## 한 줄 요약

AI 에이전트의 컨텍스트 창은 **한정된 주의력 예산**이다. 많이 넣을수록 잘 기억하지 못한다(context rot). 그래서 "항상 필요한 것만 처음에, 나머지는 필요할 때" 읽히도록 문서를 배치한다.

## 이 프로젝트에서 일어난 일

- `CLAUDE.md` 하나에 빌드 명령부터 학습 노트 작성 절차까지 모두 들어 있다. 테스트 규칙은 테스트 파일을 만질 때만 필요한데, 매 세션 전부 읽힌다.
- `docs/ai/` 에 현행 지식과 끝난 작업 기록(리뷰 21개 등)이 섞여 있어, 에이전트가 낡은 기록을 현행 규칙으로 읽을 수 있다.
- 설계안: `docs/ai/context-structure-design.md`

## 동작 원리

```
세션 시작 ─▶ CLAUDE.md (+ @import 한 파일 전부)      ← 항상 로드: 짧게
   │
   ├─ 테스트 파일을 연다 ─▶ .claude/rules/testing.md  ← paths 가 맞을 때만
   ├─ "에러 타입 추가" 작업 ─▶ skills/new-error-type ← 부를 때만
   └─ 자세한 근거가 필요 ─▶ docs/ai/context/*.md      ← 에이전트가 직접 찾아 읽음
```

## 잘못된 예 / 맞는 예

```markdown
<!-- ✗ CLAUDE.md: import 는 시작할 때 전부 로드된다 -->
@docs/ai/context/current-state.md

<!-- ✓ CLAUDE.md: 위치만 알려 주고 필요할 때 읽게 한다 -->
- 코드 현황·결함 목록: `docs/ai/context/current-state.md`
```

## 기억할 것

1. 루트 지침 파일은 **지도**다. 이 줄을 지우면 실수하는가로 거른다 (200줄 미만, 짧을수록 좋다).
2. 경로 한정 규칙은 rules, 절차는 skill, 반드시 지킬 것은 hook·권한·테스트로 강제한다.
3. 쓰고 버릴 파일은 저장소 밖(scratchpad) 또는 gitignore 된 `.scratch/`, 이어받을 것은 커밋되는 계획·진행 문서에 둔다.

## 더 읽을거리

- Anthropic — Effective context engineering for AI agents: https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents
- Claude Code — Memory: https://code.claude.com/docs/en/memory
- OpenAI — Harness engineering: https://openai.com/index/harness-engineering/
