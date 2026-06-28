package com.tmi.core.sentence.dto;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;

public record SentenceResponse(
        String sentenceId,
        String englishText,
        Level level,
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
