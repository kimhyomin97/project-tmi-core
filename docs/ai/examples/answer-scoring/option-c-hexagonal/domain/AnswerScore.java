// 선택지 C — 위치: src/main/java/com/tmi/core/answer/domain/AnswerScore.java (신규, 순수 도메인 — Spring/JPA import 0)
package com.tmi.core.answer.domain;

public record AnswerScore(double value, Grade grade) {

    public AnswerScore {   // 불변식. 어댑터가 A′·B 와 같은 정책(허용 오차 보정, 그 밖은 Unavailable)을 거친 값만 들어온다 (README §2)
        if (value < 0.0 || value > 1.0) throw new IllegalArgumentException("score out of range: " + value);
    }

    public static AnswerScore of(double value) {
        return new AnswerScore(value, Grade.fromScore(value));   // Grade 는 AS-IS 그대로 (domain/Grade.java)
    }
}
