package com.tmi.core.sentence.repository;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SentenceRepository extends JpaRepository<SentenceEntity, String> {

    @Query(value = """
              SELECT * FROM sentence
              WHERE level = :#{#level.name()}
                AND category = :#{#category.name()}
                AND deleted_at IS NULL
              ORDER BY RANDOM()
              LIMIT 1
              """, nativeQuery = true)
    Optional<SentenceEntity> findRandomBy(@Param("level") Level level, @Param("category") Category category);
}
