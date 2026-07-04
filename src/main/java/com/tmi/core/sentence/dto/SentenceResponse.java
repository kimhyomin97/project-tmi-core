package com.tmi.core.sentence.dto;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출제된 영어 문장 응답")
public record SentenceResponse(
        @Schema(description = "문장 ID (36자 UUID)", example = "01HTM3ABCXYZ")
        String sentenceId,

        @Schema(description = "영어 문장 (사용자가 번역할 원문)",
                example = "I had a rough day at work today.")
        String englishText,

        @Schema(description = "난이도 코드", example = "LEVEL_3",
                allowableValues = {"LEVEL_1", "LEVEL_2", "LEVEL_3", "LEVEL_4", "LEVEL_5"})
        Level level,

        @Schema(description = "카테고리 코드", example = "DAILY",
                allowableValues = {"DAILY", "BUSINESS", "TECH"})
        Category category
) {
    public static SentenceResponse from(SentenceEntity sentence) {
        return new SentenceResponse(
                sentence.getId(),
                sentence.getEnglishText(),
                sentence.getLevel(),
                sentence.getCategory()
        );
    }
}
