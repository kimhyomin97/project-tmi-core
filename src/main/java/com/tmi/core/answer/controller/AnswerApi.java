package com.tmi.core.answer.controller;

import com.tmi.core.answer.dto.AnswerResponse;
import com.tmi.core.answer.dto.SubmitAnswerRequest;
import com.tmi.core.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Answer", description = "답안 채점")
public interface AnswerApi {

    @Operation(
            summary = "답안 채점",
            description = """
                    사용자가 제출한 한국어 번역을 모범 답안과 의미 유사도로 채점.

                    처리 흐름:
                    1. `sentenceId` 로 문장 조회 (미존재 시 404)
                    2. Similarity 서버에 `userAnswer` + 모범 답안 전송 → 유사도 점수 (0.0~1.0)
                    3. 점수 → 등급 변환 (EXCELLENT/GOOD/FAIR/POOR)
                    4. 응답 조립 (원문 + 사용자 답안 + 모범 답안 + 점수 + 등급)

                    MVP 는 응답 코드 200 OK — 답안을 영속화하지 않고 채점 결과만 반환.
                    이력 저장 도입 시 201 Created 로 전환 예정.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "채점 성공",
                    content = @Content(
                            schema = @Schema(implementation = AnswerResponse.class),
                            examples = @ExampleObject(name = "success", value = """
                                    {
                                      "sentenceId": "01HTM3ABCXYZ",
                                      "englishText": "I had a rough day at work today.",
                                      "userAnswer": "오늘 회사에서 힘든 하루였다",
                                      "correctAnswer": "오늘 회사에서 고단한 하루를 보냈다.",
                                      "score": 0.85,
                                      "grade": "GOOD"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400", description = "body 검증 실패 (필수 누락·길이 초과 등)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalid-request", value = """
                                    {
                                      "error": {
                                        "code": "TMI.COMMON.INVALID_REQUEST",
                                        "message": "userAnswer: must not be blank"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404", description = "sentenceId 에 해당하는 문장 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "sentence-not-found", value = """
                                    {
                                      "error": {
                                        "code": "TMI.SENTENCE.NOT_FOUND",
                                        "message": "01HTM3ABCXYZ"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502", description = "Similarity 서버 요청 형식 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "similarity-request-invalid", value = """
                                    {
                                      "error": {
                                        "code": "TMI.SIMILARITY.REQUEST_INVALID",
                                        "message": "Invalid request to similarity service"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503", description = "Similarity 서버 다운·타임아웃·네트워크 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "similarity-unavailable", value = """
                                    {
                                      "error": {
                                        "code": "TMI.SIMILARITY.UNAVAILABLE",
                                        "message": "Similarity service is currently unavailable"
                                      }
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<AnswerResponse> submit(SubmitAnswerRequest request);
}
