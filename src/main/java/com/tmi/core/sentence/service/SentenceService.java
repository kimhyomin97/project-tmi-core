package com.tmi.core.sentence.service;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import com.tmi.core.sentence.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SentenceService {

    private final SentenceRepository sentenceRepository;

    public SentenceEntity getRandomSentence(Level level, Category category) {
        List<SentenceEntity> sentences = sentenceRepository.findByLevelAndCategory(level, category);

        if (sentences.isEmpty()) {
            throw new IllegalArgumentException(
                    "조건에 맞는 문장이 없습니다: " + level + ", " + category
            );
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(sentences.size());
        return sentences.get(randomIndex);
    }

    public List<SentenceEntity> getSentences(Level level, Category category, int size) {
        List<SentenceEntity> sentences = sentenceRepository.findByLevelAndCategory(level, category);

        if (sentences.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(sentences);
        return sentences.subList(0, Math.min(size, sentences.size()));
    }
}
