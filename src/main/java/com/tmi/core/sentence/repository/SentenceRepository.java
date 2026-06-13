package com.tmi.core.sentence.repository;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceRepository extends JpaRepository<SentenceEntity, String> {

    List<SentenceEntity> findByLevelAndCategory(Level level, Category category);

    long countByLevelAndCategory(Level level, Category category);
}
