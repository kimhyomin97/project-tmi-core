package com.tmi.core.word.adapter.out.persistence;

import com.tmi.core.word.domain.model.Difficulty;
import com.tmi.core.word.domain.model.Sentence;
import com.tmi.core.word.domain.port.out.LoadSentencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SentencePersistenceAdapter implements LoadSentencePort {

    private final SentenceRepository sentenceRepository;

    @Override
    public List<Sentence> findByDifficultyAndCategory(Difficulty difficulty, String category) {
        SentenceEntity.DifficultyType entityDifficulty = toEntityDifficulty(difficulty);

        List<SentenceEntity> entities = sentenceRepository.findByDifficultyAndCategory(entityDifficulty, category);

        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public long countByDifficultyAndCategory(Difficulty difficulty, String category) {
        SentenceEntity.DifficultyType entityDifficulty = toEntityDifficulty(difficulty);
        return sentenceRepository.countByDifficultyAndCategory(entityDifficulty, category);
    }

    private Sentence toDomain(SentenceEntity entity) {
        return Sentence.builder()
                .id(Long.valueOf(entity.getId()))
                .englishText(entity.getEnglishText())
                .koreanRef(entity.getKoreanRef())
                .difficulty(toDomainDifficulty(entity.getDifficulty()))
                .category(entity.getCategory())
                .subcategory(entity.getSubcategory())
                .build();
    }

    private Difficulty toDomainDifficulty(SentenceEntity.DifficultyType type) {
        return Difficulty.valueOf(type.name());
    }

    private SentenceEntity.DifficultyType toEntityDifficulty(Difficulty difficulty) {
        return SentenceEntity.DifficultyType.valueOf(difficulty.name());
    }
}
