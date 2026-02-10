package com.tmi.core.word.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceRepository extends JpaRepository<SentenceEntity, Integer> {
    List<SentenceEntity> findByDifficultyAndCategory(SentenceEntity.DifficultyType difficulty, String category);
    long countByDifficultyAndCategory(SentenceEntity.DifficultyType difficulty, String category);
}
