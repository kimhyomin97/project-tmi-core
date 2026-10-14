// 선택지 C — 위치: src/main/java/com/tmi/core/sentence/adapter/out/persistence/SentencePersistenceAdapter.java
// answer 의 LoadSentencePort 구현. JPA 엔티티(SentenceJpaEntity, AS-IS SentenceEntity 를 개명)를 answer 소유 record 로 매핑.
// 의존 방향: sentence(어댑터) → answer(포트) 단방향. answer 는 sentence 를 모른다.
package com.tmi.core.sentence.adapter.out.persistence;

import com.tmi.core.answer.application.port.out.LoadSentencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SentencePersistenceAdapter implements LoadSentencePort {

    private final SentenceJpaRepository repository;    // AS-IS SentenceRepository 를 개명

    @Override
    @Transactional(readOnly = true)
    public Optional<SentenceForScoring> loadActive(String sentenceId) {
        return repository.findByIdAndDeletedAtIsNull(sentenceId)
                .map(e -> new SentenceForScoring(e.getId(), e.getEnglishText(), e.getKoreanRef()));
    }
}
