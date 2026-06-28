[← README](./README.md)

# 03. API 명세

> TMI Core가 외부에 노출하는 HTTP 엔드포인트와 응답 계약.

**대상 독자**: 클라이언트(웹/모바일) 또는 API를 호출하는 개발자  
**범위**: 외부 계약 — URL · 요청 · 응답 · 에러  
**관련 문서**: [04. 데이터 모델](./04_데이터모델.md) · [05. 개발 컨벤션 §4 API](./05_개발컨벤션.md#4-api-컨벤션)

---

## 1. 공통

### 1.1 Base URL

| 환경 | URL |
|---|---|
| 로컬 | `http://localhost:8080` |
| 운영 | TBD (→ [06. 배포·운영](./06_배포·운영.md)) |

API 경로 prefix: `/api/v1/...` (버저닝 규칙 → 05 §4.1).

### 1.2 인증

MVP 단일 사용자 가정 — **인증 없음**. 멀티 사용자 전환 시 도입 (→ [01. 기획 §4.2](./01_기획.md#42-mvp-이후-검토-사항)).

### 1.3 공통 응답 규칙

- Content-Type: `application/json; charset=UTF-8`
- 시간 필드: ISO 8601 UTC (`2026-06-01T10:00:00Z`)
- 키 형식: camelCase

### 1.4 에러 응답 포맷

```json
{
  "error": {
    "code": "TMI.SENTENCE.NOT_FOUND",
    "message": "Sentence abc-123 was not found."
  }
}
```

- 모든 에러 응답은 `{ error: { code, message } }` 래핑.
- `code` 형식: `TMI.{DOMAIN}.{REASON}` (→ 05 §5.2).
- `message`: 영문 기본, `Accept-Language: ko` 헤더 시 한글 (→ 05 §5.5).

### 1.5 에러 코드 목록

| HTTP | code | 설명 | 발생 시점 |
|---|---|---|---|
| 400 | `TMI.COMMON.INVALID_REQUEST` | 파라미터·body 검증 실패 | Bean Validation 실패 |
| 404 | `TMI.SENTENCE.NOT_FOUND` | 문장 ID 미존재 | 답안 제출 시 잘못된 `sentenceId` |
| 500 | `TMI.COMMON.INTERNAL_ERROR` | 서버 내부 오류 | DB·런타임 예외 |
| 502 | `TMI.SIMILARITY.REQUEST_INVALID` | 외부 호출 요청 형식 오류 | Similarity 4xx 응답 |
| 503 | `TMI.SIMILARITY.UNAVAILABLE` | 외부 호출 실패·타임아웃 | Similarity 다운·네트워크 오류 |

---

## 2. Sentence — 문제 출제

### 2.1 GET /api/v1/sentences/random

조건에 맞는 영어 문장 1건 랜덤 추출. **모범 답안은 포함하지 않음** (정답 노출 방지).

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `level` | string | Y | 난이도 — `LEVEL_1` ~ `LEVEL_5` (→ [01 §6](./01_기획.md#6-난이도-체계)) |
| `category` | string | Y | 카테고리 — `DAILY` · `BUSINESS` · `TECH` (→ [01 §7](./01_기획.md#7-카테고리-체계)) |

**Response 200**

```json
{
  "sentenceId": "01HTM3...",
  "englishText": "I had a rough day at work today.",
  "level": "LEVEL_3",
  "category": "DAILY"
}
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `TMI.COMMON.INVALID_REQUEST` | `level`·`category` 누락·잘못된 값 |
| 404 | `TMI.SENTENCE.NOT_FOUND` | 조건에 맞는 문장 없음 |

### 2.2 GET /api/v1/sentences/meta

필터 UI 구성에 쓰는 메타데이터 — 난이도·카테고리의 코드·라벨 목록.

**Response 200**

```json
{
  "levels": [
    { "code": "LEVEL_1", "label": "입문", "minWords": 1,  "maxWords": 3  },
    { "code": "LEVEL_2", "label": "기초", "minWords": 4,  "maxWords": 8  },
    { "code": "LEVEL_3", "label": "중급", "minWords": 9,  "maxWords": 15 },
    { "code": "LEVEL_4", "label": "고급", "minWords": 16, "maxWords": 25 },
    { "code": "LEVEL_5", "label": "도전", "minWords": 26, "maxWords": 35 }
  ],
  "categories": [
    { "code": "DAILY",    "label": "일상 회화" },
    { "code": "BUSINESS", "label": "업무·이메일·미팅" },
    { "code": "TECH",     "label": "IT·개발 관련" }
  ]
}
```

---

## 3. Answer — 답안 채점

### 3.1 POST /api/v1/answers

사용자 답안 제출 → 의미 유사도 채점 → 점수·등급·모범 답안 반환.

> **MVP 응답 코드: 200 OK.**  
> 컨벤션상 POST는 201이지만(05 §4.2), MVP는 답안을 영속화하지 않고 채점 결과만 반환 — 자원 생성이 아님. 이력 저장 도입 시 `201 Created` + `Location` 헤더로 전환 (→ [01 §4.2](./01_기획.md#42-mvp-이후-검토-사항)).

**Request Body**

```json
{
  "sentenceId": "01HTM3...",
  "userAnswer": "오늘 회사에서 힘든 하루였다"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `sentenceId` | string | Y | 36자 UUID |
| `userAnswer` | string | Y | 1 ~ 500자 |

**Response 200**

```json
{
  "sentenceId": "01HTM3...",
  "englishText": "I had a rough day at work today.",
  "userAnswer": "오늘 회사에서 힘든 하루였다",
  "correctAnswer": "오늘 회사에서 고단한 하루를 보냈다.",
  "score": 0.85,
  "grade": "GOOD"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `score` | number | 의미 유사도, `0.0` ~ `1.0` (소수점 4자리 권장) |
| `grade` | string | 점수 등급 — `EXCELLENT` · `GOOD` · `FAIR` · `POOR` |
| `correctAnswer` | string | 모범 답안 (한국어) |

**점수 → 등급 변환** (Service 책임)

| 점수 범위 | grade | 한글 라벨 |
|---|---|---|
| `>= 0.9` | `EXCELLENT` | 우수 |
| `0.7 ~ 0.9` | `GOOD` | 양호 |
| `0.5 ~ 0.7` | `FAIR` | 보통 |
| `< 0.5` | `POOR` | 미흡 |

> 임계값은 실사용 데이터로 조정 (→ [02 §4.2](./02_설계.md#42-답안-채점)).
> 한글 라벨은 클라이언트가 `grade` 코드를 보고 다국어 메시지로 렌더링.

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `TMI.COMMON.INVALID_REQUEST` | body 검증 실패 (필수 누락, 길이 초과 등) |
| 404 | `TMI.SENTENCE.NOT_FOUND` | `sentenceId`에 해당하는 문장 없음 |
| 502 | `TMI.SIMILARITY.REQUEST_INVALID` | 외부 호출 요청 형식 오류 |
| 503 | `TMI.SIMILARITY.UNAVAILABLE` | Similarity 서버 다운·타임아웃 |

---

## 4. 헬스체크

### 4.1 GET /api/v1/health

서비스 자체 + 의존 시스템(DB · Similarity) 상태.

**Response 200**

```json
{
  "status": "UP",
  "db": "UP",
  "similarity": "UP"
}
```

| 필드 | 값 | 의미 |
|---|---|---|
| `status` | `UP` / `DOWN` | 전체 상태 (db·similarity 모두 `UP`일 때만 `UP`) |
| `db` | `UP` / `DOWN` | `SELECT 1` 성공 여부 |
| `similarity` | `UP` / `DOWN` | Similarity 서버 `/health` 호출 성공 여부 (200ms 캐시) |

> 모니터링 도구 연동·로그 등 운영 측면은 → [06. 배포·운영 §4](./06_배포·운영.md#4-헬스체크).

---

## 5. 내부 API — Similarity Server 호출

TMI Core → 외부 Python Similarity 서버. **외부에 노출되지 않음**, 통합·디버깅 참고용.

### 5.1 POST {SIMILARITY_BASE_URL}/similarity

문장 임베딩 후 의미 유사도 계산.

**Request Body**

```json
{
  "answer": "오늘 회사에서 힘든 하루였다",
  "reference": "오늘 회사에서 고단한 하루를 보냈다."
}
```

**Response 200**

```json
{ "score": 0.85 }
```

### 5.2 TMI Core 측 처리

| Similarity 응답 | 클라이언트로 변환되는 응답 |
|---|---|
| 200 + `score` | `score`·`grade` 계산 후 정상 응답 |
| 4xx | `502 TMI.SIMILARITY.REQUEST_INVALID` |
| 5xx · 타임아웃 (2초) · 네트워크 오류 | `503 TMI.SIMILARITY.UNAVAILABLE` |

> raw HTTP 클라이언트 예외 전파 금지 (→ [05 §5.7](./05_개발컨벤션.md#5-에러-처리)).
> 클라이언트 구성 상세 → [02 §5 외부 시스템 연동](./02_설계.md#5-외부-시스템-연동--similarity).
