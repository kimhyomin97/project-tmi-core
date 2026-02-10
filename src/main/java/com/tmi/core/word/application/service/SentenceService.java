package com.tmi.core.word.application.service;

import com.tmi.core.word.domain.model.Difficulty;
import com.tmi.core.word.domain.model.Sentence;
import com.tmi.core.word.domain.port.in.GetSentenceUseCase;
import com.tmi.core.word.domain.port.out.LoadSentencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SentenceService implements GetSentenceUseCase {

    private final LoadSentencePort loadSentencePort;

    @Override
    public Sentence getRandomSentence(Difficulty difficulty, String category) {
        List<Sentence> sentences = loadSentencePort.findByDifficultyAndCategory(difficulty, category);

        if(sentences.isEmpty()) {
            throw new IllegalArgumentException(
                    "조건에 맞는 문장이 없습니다: " + difficulty + ", " + category
            );
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(sentences.size());
        return sentences.get(randomIndex);
    }

    @Override
    public List<Sentence> getSentences(Difficulty difficulty, String category, int size) {
        List<Sentence> sentences = loadSentencePort.findByDifficultyAndCategory(difficulty, category);

        if(sentences.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(sentences);
        return sentences.subList(0, Math.min(size, sentences.size()));
    }
}
