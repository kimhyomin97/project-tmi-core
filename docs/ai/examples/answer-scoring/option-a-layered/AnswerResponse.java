// 선택지 A — 위치: src/main/java/com/tmi/core/answer/dto/AnswerResponse.java
// 변경점: 팩토리 이름 of → from, 등급 계산(Grade.fromScore)을 DTO 밖으로 (conventions-proposal §3 공통 규칙).
//        구조는 그대로 — 여전히 sentence 의 엔티티에 의존한다 (선택지 A 의 한계).
package com.tmi.core.answer.dto;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.sentence.domain.SentenceEntity;   // ← 엔티티 컬럼 변경이 answer API 로 전파될 수 있는 지점

public record AnswerResponse(
        String sentenceId,
        String englishText,
        String userAnswer,
        String correctAnswer,
        double score,
        Grade grade
) {
    public static AnswerResponse from(SentenceEntity sentence, String userAnswer, double score, Grade grade) {
        return new AnswerResponse(sentence.getId(), sentence.getEnglishText(), userAnswer,
                sentence.getKoreanRef(), score, grade);
    }
}
