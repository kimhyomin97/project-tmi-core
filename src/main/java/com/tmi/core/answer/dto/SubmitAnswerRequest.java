package com.tmi.core.answer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "답안 제출 요청")
public record SubmitAnswerRequest(

        @Schema(description = "출제받은 문장 ID (36자 UUID)",
                example = "01HTM3ABCXYZ",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 36)
        String sentenceId,

        @Schema(description = "사용자가 입력한 한국어 번역 (1~500자)",
                example = "오늘 회사에서 힘든 하루였다",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1, maxLength = 500)
        @NotBlank
        @Size(min = 1, max = 500)
        String userAnswer
) {
}
