package com.tmi.core.word.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="sentences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "english_text", nullable = false, columnDefinition = "TEXT")
    private String englishText;

    @Column(name = "korean_ref", nullable =  false, columnDefinition = "TEXT")
    private String koreanRef;

    @Column(name = "difficulty", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DifficultyType difficulty;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "subcategory", nullable = false, length = 100)
    private String subcategory;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    public enum DifficultyType {
        STARTER, BEGINNER, INTERMEDIATE, ADVANCED, CHALLENGE
    }
}
