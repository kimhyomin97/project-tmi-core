package com.tmi.core.sentence.service;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import com.tmi.core.sentence.exception.SentenceException;
import com.tmi.core.sentence.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentenceService {

    private final SentenceRepository sentenceRepository;

    @Transactional(readOnly = true)
    public SentenceEntity getRandom(Level level, Category category) {
        log.info("문장 출제 : level={} category={}", level, category);
        return sentenceRepository.findRandomBy(level, category)
                .orElseThrow(() -> new SentenceException.NotFound("level="+level+ ", category="+category));
    }
    
}
