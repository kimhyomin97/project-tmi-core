package com.tmi.core.word.repository;

import com.tmi.core.word.domain.Difficulty;
import com.tmi.core.word.domain.SentenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceRepository extends JpaRepository<SentenceEntity, Long> {
    List<SentenceEntity> findByDifficultyAndCategory(Difficulty difficulty, String category);
    long countByDifficultyAndCategory(Difficulty difficulty, String category);
}
