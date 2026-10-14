# CLAUDE.md — TMI Core

영어 문장 번역 → 의미 유사도 채점 학습 서비스의 API 서버. Spring Boot 4.0.2 · Java 21 · Gradle 9.3 · JPA/PostgreSQL · Lombok · springdoc.
기능 모듈: `sentence`(출제) · `answer`(채점) · `infra.similarity`(외부 유사도 서버) · `toss.holdings`(구현 중) · `common`(공통).

> `[제안]` 표시는 `docs/ai/proposals/decisions-needed.md` 의 결정을 기다리는 규칙이다. 결정 전에는 새 코드에만 적용하고, 기존 코드를 이 규칙만으로 고치지 않는다.

## 빌드·테스트

```bash
bash ./gradlew build                 # 컴파일 + 전체 테스트 (gradlew 실행 권한이 없어 bash 로)
bash ./gradlew test --tests '<클래스>'   # 특정 테스트만 — DB 불필요
bash ./gradlew test --tests 'com.tmi.core.architecture.*'   # ArchUnit 구조 규칙
bash ./gradlew spotlessCheck         # 포맷 검사 (build 에 미포함, 기존 코드 위반 있음)
python3 scripts/check-doc-links.py   # 문서 링크 검사 (문서를 옮기거나 지운 뒤)
```

- 전체 `test`·`build` 는 PostgreSQL(`localhost:15432/tmi_word`)이 필요하다. 없으면 `PSQLException: Connection refused` — 코드 문제가 아니다. 기동: `docs/ai/blockers.md` §2.
- `spotlessApply` 를 사람 승인 없이 기존 `src/main` 전체에 돌리지 않는다 (D5).
- `.gitignore` 의 `out/` 규칙이 `adapter/out/` 같은 경로도 무시한다. 그런 경로에 파일을 만들면 `git status` 로 추적 여부를 확인한다.

## 구조·의존 규칙

현재 (`docs/public/05_개발컨벤션.md` §1.3): `{기능}/{controller,service,repository,domain,dto,exception}`, 외부 클라이언트는 `infra/{시스템}/`, 공통은 `common/`.

- controller → service → repository 방향만 (ArchUnit R4). `domain` 은 다른 레이어에 의존하지 않는다 (R4b). `common` 은 기능 모듈에 의존하지 않는다 (R3).
- `[제안]` 다른 기능의 `repository`·`domain`·`exception` 을 직접 import 하지 않는다. 추천안 B 면 다른 모듈은 base package 타입(공개 API)으로만 쓴다 (R2, D1).
- 헥사고날(포트·어댑터) 구조로 바꾸지 않는다 — 팀이 도입했다 되돌렸다 (02 설계 §2.2). 루트 `README.md` 의 패키지 표는 폐기된 헥사고날 구조다.
- `com.tmi.core.architecture` 테스트의 `@Disabled` 는 알려진 위반이다. 새 위반을 `@Disabled` 로 숨기지 않는다.

## 예외·응답

- 예외는 `BaseException` 하위의 "외부 클래스 + 정적 중첩 클래스" (`SentenceException.NotFound`). 새 에러는 skill `new-error-type`.
- `@RestControllerAdvice` 는 `GlobalExceptionHandler` 하나뿐. 컨트롤러에서 try/catch 하지 않는다.
- 에러 응답 `{"error":{"code":"TMI.{DOMAIN}.{REASON}","message":...}}` 은 외부 계약 (03 API 명세 §1.4). 바꾸지 않는다.
- 로그: `@Slf4j`, placeholder `{}`. 토큰·시크릿은 로그에 남기지 않는다.

경로별 상세 규칙은 해당 파일을 다룰 때 `.claude/rules/` 에서 자동으로 로드된다: 테스트(`testing.md`), 영속성·트랜잭션(`persistence.md`), 외부 HTTP 호출(`external-client.md`).

## 작업 방식

- 작업 단위 = 검증 명령이 정해진 항목 1개, 모듈 1개, `src/main` 변경 5파일 이하. 기능 목록 항목은 skill `feature-item` 으로 진행한다.
- 버그 수정은 **실패하는 테스트를 먼저** 커밋하고, 그다음 고친다.
- 외부 API 응답 변경 · 패키지 이동 · 의존성 추가 · `docs/public` 수정 · `.claude/` 설정 변경은 구현 전에 사람에게 계획을 확인한다.
- 성공을 주장할 때는 실행한 명령과 결과를 함께 적는다.
- 작업 브랜치 → PR → verify·리뷰 통과 후 **에이전트가 main 에 merge 까지 한다** (사용자 지시, 2026-10-14). 사용자 응답·설명은 한국어로.
- 작업 중 핵심 개념이 쓰이면 skill `study-note` 로 `docs/study/` 에 학습 노트를 남긴다 (사용자가 이 프로젝트로 BE 를 학습한다).

## 파일 위치

- 임시 파일: 세션 scratchpad. 세션 밖까지 남겨야 하면 `.scratch/` (gitignore). 저장소 루트·`docs/` 에 임시 파일을 만들지 않는다.
- 다음 세션이 이어받을 것: `docs/ai/plans/active/<id>.md` (계획·진행), `docs/ai/progress.md` (세션 메모).

## 문서 지도 (필요할 때 읽는다)

- 색인: `docs/ai/README.md`
- 코드 현황·결함 F1~F10·ArchUnit 위반: `docs/ai/context/current-state.md`
- 결정 대기 D1~D13: `docs/ai/proposals/decisions-needed.md` · 결정된 것: `docs/ai/decisions/`
- 구조·컨벤션·AI 작업 방식 제안: `docs/ai/proposals/` (architecture · conventions · ai-workflow · context-structure-design)
- 팀 공식 설계·규칙: `docs/public/` (01 기획 ~ 07 운영, 99 공통)
- 국내 BE 관례와 학습 노트: `docs/study/`
