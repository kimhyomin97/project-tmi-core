package com.tmi.core.answer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GradeTest {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("점수 경계값마다 등급이 바뀐다")
    @CsvSource({"0.9, EXCELLENT", "0.8999, GOOD", "0.7, GOOD", "0.6999, FAIR", "0.5, FAIR", "0.4999, POOR", "0.0, POOR"})
    void shouldReturnGradeByThreshold_whenScoreIsOnBoundary(double score, Grade expected) {
        assertThat(Grade.fromScore(score)).isEqualTo(expected);
    }
}
