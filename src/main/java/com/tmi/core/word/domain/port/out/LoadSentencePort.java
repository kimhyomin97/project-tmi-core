package com.tmi.core.word.domain.port.out;

import com.tmi.core.word.domain.model.Difficulty;
import com.tmi.core.word.domain.model.Sentence;

import java.util.List;

public interface LoadSentencePort {
    List<Sentence> findByDifficultyAndCategory(Difficulty difficulty, String category);
    long countByDifficultyAndCategory(Difficulty difficulty, String category);
}
