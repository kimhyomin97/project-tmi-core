package com.tmi.core.sentence.controller;

import com.tmi.core.common.exception.ErrorResponse;
import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.dto.SentenceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Sentence", description = "영어 문장 출제")
public interface SentenceApi {

    @Operation(
            summary = "랜덤 문장 출제",
            description = """
                    난이도·카테고리 조건에 맞는 영어 문장 1건을 무작위 반환.
                    모범 답안(koreanRef)은 응답에 포함되지 않음 (정답 노출 방지).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "출제 성공",
                    content = @Content(
                            schema = @Schema(implementation = SentenceResponse.class),
                            examples = @ExampleObject(name = "success", value = """
                                    {
                                      "sentenceId": "01HTM3ABCXYZ",
                                      "englishText": "I had a rough day at work today.",
                                      "level": "LEVEL_3",
                                      "category": "DAILY"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400", description = "level·category 누락 또는 잘못된 값",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalid-request", value = """
                                    {
                                      "error": {
                                        "code": "TMI.COMMON.INVALID_REQUEST",
                                        "message": "level: must be one of LEVEL_1 ~ LEVEL_5"
                                      }
                                    }
                                    """)
                    )
            ),
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
            )
    })
    ResponseEntity<SentenceResponse> getRandom(
            @Parameter(description = "난이도", example = "LEVEL_3", required = true) Level level,
            @Parameter(description = "카테고리", example = "DAILY", required = true) Category category);
}
