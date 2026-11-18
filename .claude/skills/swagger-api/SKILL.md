---
name: swagger-api
description: Controller 신설·수정 시 {Feature}Api 인터페이스(springdoc-openapi) 를 함께 작성·갱신. @Tag / @Operation / @ApiResponses 규약 통일, 에러 응답 예시는 messages*.properties 와 1:1 대조. 기능 추가 시 feature-item, 에러 추가 시 new-error-type 과 조합.
---

# Swagger API 인터페이스 작성

Controller 는 명세(인터페이스) 와 로직(구현) 을 분리한다. **`{Feature}Api` 인터페이스에 모든 Swagger 어노테이션**, `{Feature}Controller implements {Feature}Api` 가 로직만. 레퍼런스: `com.tmi.core.sentence.controller.SentenceApi`.

## 언제 사용

- 새 Controller 메서드 추가 (기존·신규)
- 새 기능 모듈의 Controller + Api 쌍 신설
- Swagger 응답 예시(`@ApiResponse`) 를 `messages*.properties` 와 동기화

## 역할 분담

| 작업 | 담당 스킬 |
|---|---|
| 에러 코드 신설 (`ETmiErrorType` + 예외 + messages + 03 API 명세 표) | `new-error-type` |
| **Swagger `@ApiResponse` 예시 반영** (에러 코드가 노출되는 엔드포인트) | **swagger-api (이 스킬)** |
| 기능 전체 플로우 (설계 → 테스트 → 구현 → verify) | `feature-item` |

## 절차

### 1. 인터페이스 위치·이름

- 패키지: `com.tmi.core.{feature}.controller`
- 파일: `{Feature}Api.java` (PascalCase, 예: `SentenceApi`, `AnswerApi`)
- Controller 는 같은 패키지에 `{Feature}Controller implements {Feature}Api`

### 2. 클래스 레벨 — `@Tag`

```java
@Tag(name = "Sentence", description = "영어 문장 출제")
public interface SentenceApi {
```

- `name` = PascalCase 영어 (그룹 표시용)
- `description` = 한글 한 줄 (기능 요약)

### 3. 메서드 레벨 — `@Operation`

```java
@Operation(
        summary = "랜덤 문장 출제",
        description = """
                난이도·카테고리 조건에 맞는 영어 문장 1건을 무작위 반환.
                모범 답안(koreanRef)은 응답에 포함되지 않음 (정답 노출 방지).
                """
)
```

- `summary` 한글, 동사형 명사구
- `description` text block, 2-4줄, **왜/제약** 위주

### 4. `@ApiResponses` — 응답 명세

**200/201**: `SentenceResponse` 등 DTO 의 `@Schema` + 성공 예시 JSON
**에러 응답**: `ErrorResponse` 스키마 + `messages*.properties` 메시지 1:1 복사

```java
@ApiResponse(
        responseCode = "404", description = "조건에 맞는 문장 없음",
        content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(name = "not-found", value = """
                        {
                          "error": {
                            "code": "TMI.SENTENCE.NOT_FOUND",
                            "message": "level=LEVEL_5, category=TECH"
                          }
                        }
                        """)
        )
),
```

### 5. 응답 코드 선택

| 상황 | 코드 | `error.code` |
|---|---|---|
| 요청 형식·필수값 오류 | 400 | `TMI.COMMON.INVALID_REQUEST` |
| 도메인 리소스 없음 | 404 | `TMI.{DOMAIN}.NOT_FOUND` |
| 외부 시스템 4xx | 502 | `TMI.INFRA.{SYSTEM}.BAD_RESPONSE` |
| 외부 시스템 장애·timeout | 503 | `TMI.INFRA.{SYSTEM}.UNAVAILABLE` |
| 서버 내부 오류 | 500 | `TMI.COMMON.INTERNAL` |

**POST 는 보통 201**. 200 으로 두는 건 문서화된 예외(`AnswerApi` 처럼). 임의로 바꾸지 않는다.

### 6. 파라미터 — `@Parameter`

```java
ResponseEntity<SentenceResponse> getRandom(
        @Parameter(description = "난이도", example = "LEVEL_3", required = true) Level level,
        @Parameter(description = "카테고리", example = "DAILY", required = true) Category category);
```

Validation 어노테이션(`@NotBlank` 등) 은 구현(Controller) 이 아니라 **Api 인터페이스의 파라미터** 에 둔다.

### 7. 메시지 정합성 검증 (필수)

`@ApiResponse` 예시의 `message` 값은 `messages*.properties` 와 **글자 단위 일치**.

- `src/main/resources/messages/messages.properties`
- `src/main/resources/messages/messages_ko.properties`

`{0}`, `{1}` 플레이스홀더가 들어간 메시지는 예시에서 실제 치환값으로 채운다 (예: `"level=LEVEL_5, category=TECH"`).

**현재 알려진 결함** (`docs/ai/context/current-state.md`):
- `AnswerApi.java:86` vs `messages.properties:13` — 마침표 유무 차이 (F 결함)
- `SentenceApi.java:50` — 400 예시 `"level: must be one of LEVEL_1 ~ LEVEL_5"` 가 실제 응답과 다름

→ 새로 작성하는 예시는 **열어서 대조 후 복사**. 어긋나면 결함 누적.

### 8. Controller 쪽

```java
@RestController
@RequestMapping("/api/v1/sentences")
@RequiredArgsConstructor
public class SentenceController implements SentenceApi {

    private final SentenceService service;

    @Override
    @GetMapping("/random")
    public ResponseEntity<SentenceResponse> getRandom(Level level, Category category) {
        return ResponseEntity.ok(service.getRandom(level, category));
    }
}
```

- Controller 에 Swagger 어노테이션 중복 금지 (Api 인터페이스가 유일 원천)
- `@RequestMapping` 경로는 Controller 에서 지정 (Api 인터페이스는 경로 무지)
- `implements` 뒤 공백 확인 (`SentenceController.java:17` 공백 누락 결함 참고)

## 체크리스트

- [ ] `{Feature}Api` 인터페이스 생성, 패키지 = `{feature}.controller`
- [ ] `@Tag` (name 영문, description 한글)
- [ ] 각 메서드 `@Operation` (summary 한글, description text block)
- [ ] `@ApiResponses` — 200/201 + 발생 가능한 모든 에러
- [ ] 에러 예시 `message` 값 = `messages*.properties` 와 1:1 동일
- [ ] `@Parameter` 로 모든 파라미터 설명
- [ ] Controller 는 `implements {Feature}Api`, Swagger 어노테이션 중복 없음
- [ ] 로컬 부팅 후 `http://localhost:8080/swagger-ui.html` 에서 눈으로 확인

## 참고

- 레퍼런스 인터페이스: `SentenceApi.java`, `AnswerApi.java`
- 에러 응답 계약: `docs/public/03_API명세.md` §1.4
- 컨벤션: `docs/public/05_개발컨벤션.md` §1.3
- 결함 목록: `docs/ai/context/current-state.md`
