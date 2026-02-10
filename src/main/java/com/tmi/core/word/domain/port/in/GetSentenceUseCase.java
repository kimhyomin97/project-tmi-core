package com.tmi.core.word.domain.port.in;

import com.tmi.core.word.domain.model.Difficulty;
import com.tmi.core.word.domain.model.Sentence;

import java.util.List;

public interface GetSentenceUseCase {
    Sentence getRandomSentence(Difficulty difficulty, String category);
    List<Sentence> getSentences(Difficulty difficulty, String category, int size);
}
