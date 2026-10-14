// 선택지 A′ — 위치: src/main/java/com/tmi/core/answer/dto/AnswerResponse.java
// option-b-modular/answer/AnswerResponse.java 와 동일하고 import 만 com.tmi.core.sentence.dto.SentenceDetail.
package com.tmi.core.answer.dto;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.sentence.dto.SentenceDetail;

public record AnswerResponse(
        String sentenceId, String englishText, String userAnswer, String correctAnswer, double score, Grade grade) {

    public static AnswerResponse from(SentenceDetail sentence, String userAnswer, double score, Grade grade) {
        return new AnswerResponse(sentence.id(), sentence.englishText(), userAnswer, sentence.koreanRef(), score, grade);
    }
}
