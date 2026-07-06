package com.tmi.core.answer.domain;

public enum Grade {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR;

    public static Grade fromScore(double score) {
        if (score >= 0.9) return EXCELLENT;
        if (score >= 0.7) return GOOD;
        if (score >= 0.5) return FAIR;
        return POOR;
    }
}
