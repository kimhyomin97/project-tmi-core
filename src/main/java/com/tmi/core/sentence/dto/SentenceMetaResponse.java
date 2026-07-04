package com.tmi.core.sentence.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "필터 UI 구성을 위한 메타데이터")
public record SentenceMetaResponse(
        @Schema(description = "지원 난이도 목록")
        List<LevelItem> levels,

        @Schema(description = "지원 카테고리 목록")
        List<CategoryItem> categories
) {
    @Schema(description = "난이도 항목 (코드·라벨·단어 수 범위)")
    public record LevelItem(
            @Schema(description = "난이도 코드", example = "LEVEL_3")
            String code,

            @Schema(description = "표시 라벨", example = "중급")
            String label,

            @Schema(description = "단어 수 최소", example = "9")
            int minWords,

            @Schema(description = "단어 수 최대", example = "15")
            int maxWords
    ) {}

    @Schema(description = "카테고리 항목 (코드·라벨)")
    public record CategoryItem(
            @Schema(description = "카테고리 코드", example = "DAILY")
            String code,

            @Schema(description = "표시 라벨", example = "일상 회화")
            String label
    ) {}
}
