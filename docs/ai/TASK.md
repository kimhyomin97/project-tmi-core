# AI 기반 개발 준비 작업 지시서

이 Spring Boot 프로젝트의 AI 기반 개발 준비 작업을 수행한다.
목표는 "결정"이 아니라 **"내가 아침에 결정할 수 있는 고품질 제안서"**를 만드는 것이다.
속도보다 깊이와 정확성이 중요하다. 각 단계를 충분히 검증하며 진행한다.

작업 중 맥락이 흐려지면 언제든 이 파일(docs/ai/TASK.md)을 다시 읽고 이어서 진행한다.

---

## 규칙

- `src/main` 코드는 수정하지 않는다. 변경 허용 범위는 다음뿐이다.
    - `docs/ai/` 하위 파일
    - `CLAUDE.md`
    - ArchUnit 테스트 (`src/test`)
    - 빌드 설정의 포맷/정적분석 플러그인 추가
- 질문하지 않는다. 판단이 필요한 사항은 `docs/ai/decisions-needed.md`에
  선택지, 장단점, 추천을 적고 진행한다.
- 모든 제안에는 현재 코드의 실제 파일·클래스 예시를 근거로 붙인다.
- 외부 자료를 참고한 주장에는 출처 URL을 붙인다.
  웹 접근이 막히면 접근 가능한 공식 문서(spring.io 등) 위주로 조사하고,
  막힌 내용은 `docs/ai/blockers.md`에 기록한다.
- 빌드 환경 문제로 30분 이상 막히면 원인과 시도한 내용을
  `docs/ai/blockers.md`에 정리하고, 분석 가능한 범위까지 진행한다.
- 빌드 명령은 `./gradlew build`를 기준으로 한다.
  (Maven 프로젝트라면 `./mvnw verify`로 대체한다.)

---

## 1단계: 계획

`docs/ai/checklist.json`을 아래 형식으로 만들고, 아래 2~7단계의 산출물을 항목으로 등록한다.

```json
{
  "items": [
    {
      "id": "current-state",
      "artifact": "docs/ai/current-state.md",
      "done": false,
      "review_rounds": 0,
      "rubric_scores": {}
    }
  ]
}
```

---

## 2단계: 전수 분석 → `docs/ai/current-state.md`

- `src/main`의 **모든 클래스**를 표로 정리한다.
    - 열: 패키지, 클래스, 역할, 레이어, 의존 대상, 컨벤션 불일치 사항
- 패키지 간 의존 방향을 Mermaid 다이어그램으로 그린다.
- 반복되는 패턴과 일관성이 깨진 지점을 실제 코드 인용과 함께 정리한다.

---

## 3단계: 리서치 → `docs/ai/research.md`

- 다음 주제를 조사한다.
    - Spring Boot 공식 권장 구조
    - Hexagonal / Layered 아키텍처 실무 사례
    - 예외 처리, DTO 변환, 트랜잭션 경계, 테스트 전략의 대표적 접근법
- 각 접근법을 이 프로젝트에 적용했을 때의 적합성을 평가한다.

---

## 4단계: 제안서 작성

- `docs/ai/architecture-proposal.md`
    - 구조 선택지 2~3개, 장단점, 마이그레이션 비용, 추천안
- `docs/ai/conventions-proposal.md`
    - 네이밍, 예외 처리, DTO 변환, 트랜잭션 경계, 로깅, 테스트 규칙
    - 항목마다 현재 코드의 Before 예시와 제안 After 예시를 포함
- `docs/ai/ai-workflow.md`
    - 작업 단위 설정 방법
    - 계획 → 구현 → 리뷰 흐름과 사람이 확인할 지점
    - `feature_list.json` 기반 장시간 루프 운영 방법
- `docs/ai/examples/`
    - 기존 기능 하나를 골라, 구조 선택지별로 바뀐 형태의 예시 코드를 작성한다.
    - 컴파일 대상이 아니며, 비교와 판단을 돕는 용도다.

---

## 5단계: 독립 리뷰 루프 (가장 중요)

다음 5개 산출물 각각에 대해 리뷰 루프를 수행한다.

- `current-state.md`
- `architecture-proposal.md`
- `conventions-proposal.md`
- `ai-workflow.md`
- `examples/`

각 산출물마다:

1. 서브에이전트를 새로 띄워, **"이 문서를 승인하기 전에 반드시 문제를 찾아내는
   까다로운 시니어 Spring 엔지니어"** 역할로 리뷰시킨다.
   평가 기준(rubric)은 각 1~5점이다.
    - **근거성**: 주장마다 실제 코드나 출처가 있는가
    - **구체성**: 바로 적용 가능한 수준인가
    - **일관성**: 다른 문서와 모순이 없는가
    - **판단 가능성**: 사람이 읽고 결정을 내릴 수 있는가
2. 리뷰 결과를 `docs/ai/reviews/<문서명>-round-<N>.md`로 저장한다.
3. 지적 사항을 반영해 문서를 수정하고 커밋한다.
4. **모든 항목이 4점 이상이고 최소 2라운드**를 거칠 때까지 반복한다. (최대 4라운드)
5. `checklist.json`에 `review_rounds`와 `rubric_scores`를 기록한다.

---

## 6단계: ArchUnit 검증

- 추천안 기준 구조 규칙을 ArchUnit 테스트로 작성하고 실행한다.
- 오탐(false positive)이 있으면 규칙을 다듬어 재실행한다.
- 최종 위반 목록을 `current-state.md`에 추가한다.
- 위반으로 실패하는 테스트는 `@Disabled` 처리하고, 사유를 주석으로 남긴다.

---

## 7단계: 마무리

- `CLAUDE.md` 초안 작성
    - 확정 전 상태임을 상단에 명시
    - 빌드·테스트 명령 포함
- 전체 문서 간 일관성을 최종 점검한다.
- `docs/ai/decisions-needed.md` 최종 정리
    - 결정 항목을 중요도 순으로 나열
    - 각 항목에 추천안과 "이걸 결정하면 바로 시작할 수 있는 다음 작업"을 포함

---

## 완료 기준

- `checklist.json` 전 항목 `done=true`, 모든 문서 rubric 4점 이상
- `docs/ai/reviews/`에 리뷰 파일 최소 10개
- `./gradlew build`가 기존 테스트 기준으로 성공
- 단계마다 커밋하고, 마지막에 PR을 생성한다.
  PR 본문에는 전체 요약과 리뷰 라운드별 주요 개선 사항을 정리한다.