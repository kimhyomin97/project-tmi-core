package com.tmi.core.word.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Sentence {
    private final Long id;
    private final String englishText;
    private final String koreanRef;
    private final Difficulty difficulty;
    private final String category;
    private final String subcategory;
}
