package com.tmi.core.sentence.dto;

import java.util.List;

public record SentenceMetaResponse(
        List<LevelItem> levels,
        List<CategoryItem> categories
) {
    public record LevelItem(String code, String label, int minWords, int maxWords) {}
    public record CategoryItem(String code, String label) {}
}
