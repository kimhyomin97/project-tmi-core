// 선택지 B — 위치: src/main/java/com/tmi/core/answer/dto/AnswerResponse.java
// 변경점: SentenceEntity → SentenceDetail (sentence 모듈의 공개 record). 엔티티 컬럼 변경이 이 API 로 새지 않는다.
//        @Schema 어노테이션은 AS-IS 와 동일하므로 생략.
package com.tmi.core.answer.dto;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.sentence.SentenceDetail;

public record AnswerResponse(
        String sentenceId,
        String englishText,
        String userAnswer,
        String correctAnswer,
        double score,
        Grade grade
) {
    public static AnswerResponse from(SentenceDetail sentence, String userAnswer, double score, Grade grade) {
        return new AnswerResponse(sentence.id(), sentence.englishText(), userAnswer,
                sentence.koreanRef(), score, grade);
    }
}
