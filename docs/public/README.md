# TMI Core

> 영어 문장 번역 + AI 유사도 채점 학습 서비스의 백엔드 API 서버

---

## 한눈에 보기

영어 문장을 한국어로 옮긴 답안을 받아, AI가 모범 답안과의 **의미적 유사도**로 채점해 점수와 피드백을 돌려주는 학습 서비스.

문자열 매칭이 아닌 의미 기반 비교 → 정답이 여러 표현으로 나뉘는 한국어 번역에도 활용 가능.

**이 레포(`tmi-core`)는 전체 시스템 중 API 서버** 담당 — 문제 출제, 답안 접수, 유사도 서버 호출, 결과 조립.

## 시스템 구성

📊 **[시스템 구성도 (HTML)](./assets/diagrams/system-overview.html)** — 브라우저에서 열기

| 컴포넌트 | 책임 | 위치 |
|---|---|---|
| Web Client | 사용자 UI | `project-tmi-web` (별도 레포) |
| **TMI Core** | **API · 비즈니스 로직 · DB 접근 · 유사도 서버 호출** | **현재 레포** |
| TMI Similarity | 문장 임베딩, 유사도 계산 | `project-tmi-similarity` (별도 레포) |
| PostgreSQL | 문장·답안 이력 저장 | 인프라 |

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.0.2 (Web MVC · Data JPA · Validation) |
| DB | PostgreSQL |
| ORM | JPA / Hibernate |
| 빌드 | Gradle |
| 코드 보조 | Lombok |

## 문서

| 문서 | 설명 |
|---|---|
| [01. 기획](./01_기획.md) | 무엇을 왜 만드는가 — 기능 정의, 사용자 시나리오, 범위 |
| [02. 설계](./02_설계.md) | 어떻게 만드는가 — 아키텍처, 패키지 구조, 핵심 흐름 |
| [03. API 명세](./03_API명세.md) | 외부 계약 — 엔드포인트, 요청/응답, 에러 코드 |
| [04. 데이터 모델](./04_데이터모델.md) | 데이터 형태 — ERD, 테이블, 제약, 인덱스 |
| [05. 개발 컨벤션](./05_개발컨벤션.md) | 앞으로의 규칙 — 네이밍, DB·API·에러·로깅 컨벤션 |
| [06. 배포·운영](./06_배포·운영.md) | 어디에 어떻게 띄우는가 — 배포 구성, 환경 변수, 로그, 헬스체크 |

## 시작하기

### 사전 요구 사항

- JDK 21
- Docker (PostgreSQL 컨테이너 실행용)

### 1. PostgreSQL 기동

```bash
docker run -d --name tmi-postgres -p 15432:5432 \
  -e POSTGRES_DB=tmi_word \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15
```

### 2. 스키마 준비

스키마 구조와 준비 방법 → [04. 데이터 모델](./04_데이터모델.md) 참고.

### 3. 애플리케이션 기동

```bash
./gradlew bootRun
```

기본 포트: `http://localhost:8080`
