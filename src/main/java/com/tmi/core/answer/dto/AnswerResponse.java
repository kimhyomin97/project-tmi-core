package com.tmi.core.answer.dto;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.sentence.domain.SentenceEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "답안 채점 응답 — 점수·등급·모범 답안")
public record AnswerResponse (

    @Schema(description = "채점한 문장 ID", example = "01HTM3ABCXYZ")
    String sentenceId,

    @Schema(description = "영어 원문", example = "I had a rough day at work today.")
    String englishText,

    @Schema(description = "사용자가 제출한 한국어 번역", example = "오늘 회사에서 힘든 하루였다")
    String userAnswer,

    @Schema(description = "모범 답안 (한국어)", example = "오늘 회사에서 고단한 하루를 보냈다.")
    String correctAnswer,

    @Schema(description = "의미 유사도 (0.0 ~ 1.0)", example = "0.85")
    double score,

    @Schema(description = "점수 등급 — EXCELLENT(≥0.9) · GOOD(0.7~) · FAIR(0.5~) · POOR(<0.5)",
            example = "GOOD",
            allowableValues = {"EXCELLENT", "GOOD", "FAIR", "POOR"})
    Grade grade
) {
    public static AnswerResponse of(SentenceEntity sentence, String userAnswer, double score) {
        return new AnswerResponse(
                sentence.getId(),
                sentence.getEnglishText(),
                userAnswer,
                sentence.getKoreanRef(),
                score,
                Grade.fromScore(score)
        );
    }
}
