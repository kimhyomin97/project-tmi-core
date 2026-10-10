// 선택지 A — 위치: src/main/java/com/tmi/core/sentence/service/SentenceService.java
// 변경점: 타 기능이 쓸 "활성 문장 단건 조회" 를 sentence 가 제공 (answer 의 리포지토리 직접 접근 제거, P1 부분 해결).
//        반환 타입은 엔티티 그대로 — 선택지 A 는 기능 간 엔티티 공유를 허용한다.
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SentenceService {

    private final SentenceRepository sentenceRepository;

    @Transactional(readOnly = true)
    public SentenceEntity getRandom(Level level, Category category) {
        log.info("문장 출제: level={} category={}", level, category);
        return sentenceRepository.findRandomBy(level, category)
                .orElseThrow(() -> new SentenceException.NotFound("level=" + level + ", category=" + category));
    }

    @Transactional(readOnly = true)    // 다른 빈(AnswerService)에서 프록시를 거쳐 호출 → 실제로 적용됨 (F4 해소)
    public SentenceEntity getActive(String sentenceId) {
        return sentenceRepository.findByIdAndDeletedAtIsNull(sentenceId)
                .orElseThrow(() -> new SentenceException.NotFound(sentenceId));
    }
}
