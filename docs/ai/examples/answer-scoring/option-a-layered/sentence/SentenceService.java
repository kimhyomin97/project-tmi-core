// 선택지 A′ — 위치: src/main/java/com/tmi/core/sentence/service/SentenceService.java
// 변경점: 타 기능용 getActive 추가 (반환은 record — 엔티티는 서비스 밖으로 나가지 않음, R5 준수).
//        getRandom 의 DTO 반환·쿼리 메서드 개명은 conventions §3.1·§1.2 (FIX-R5) — 구조와 무관해 여기선 AS-IS 유지.
package com.tmi.core.sentence.service;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import com.tmi.core.sentence.dto.SentenceDetail;
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
    public SentenceEntity getRandom(Level level, Category category) {   // FIX-R5 대상 (AS-IS 그대로)
        log.info("문장 출제: level={} category={}", level, category);
        return sentenceRepository.findRandomBy(level, category)   // conventions §1.2 의 개명(findRandomActiveByLevelAndCategory)은 FIX-R5 에서 — 구조 무관
                .orElseThrow(() -> new SentenceException.NotFound("level=" + level + ", category=" + category));
    }

    /** 다른 기능용. 다른 빈(AnswerService)에서 프록시를 거쳐 호출 → 트랜잭션 실제 적용 (F4 해소). */
    @Transactional(readOnly = true)
    public SentenceDetail getActive(String sentenceId) {
        return sentenceRepository.findByIdAndDeletedAtIsNull(sentenceId)
                .map(SentenceDetail::from)
                .orElseThrow(() -> new SentenceException.NotFound(sentenceId));
    }
}
