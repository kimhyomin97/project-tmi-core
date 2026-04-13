package com.tmi.core.word.service;

import com.tmi.core.word.domain.Difficulty;
import com.tmi.core.word.domain.SentenceEntity;
import com.tmi.core.word.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SentenceService {

    private final SentenceRepository sentenceRepository;

    public SentenceEntity getRandomSentence(Difficulty difficulty, String category) {
        List<SentenceEntity> sentences = sentenceRepository.findByDifficultyAndCategory(difficulty, category);

        if (sentences.isEmpty()) {
            throw new IllegalArgumentException(
                    "조건에 맞는 문장이 없습니다: " + difficulty + ", " + category
            );
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(sentences.size());
        return sentences.get(randomIndex);
    }

    public List<SentenceEntity> getSentences(Difficulty difficulty, String category, int size) {
        List<SentenceEntity> sentences = sentenceRepository.findByDifficultyAndCategory(difficulty, category);

        if (sentences.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(sentences);
        return sentences.subList(0, Math.min(size, sentences.size()));
    }
}
