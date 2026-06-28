package com.tmi.core.sentence.domain;

import com.tmi.core.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name="sentence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentenceEntity extends BaseEntity {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "english_text", nullable = false, columnDefinition = "TEXT")
    private String englishText;

    @Column(name = "korean_ref", nullable = false, columnDefinition = "TEXT")
    private String koreanRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    public static SentenceEntity create(String englishText, String koreanRef, Level level, Category category) {
        SentenceEntity entity = new SentenceEntity();
        entity.id = UUID.randomUUID().toString();
        entity.englishText = englishText;
        entity.koreanRef = koreanRef;
        entity.level = level;
        entity.category = category;
        return entity;
    }

}
