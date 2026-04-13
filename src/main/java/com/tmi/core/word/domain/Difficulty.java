package com.tmi.core.word.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Difficulty {

    STARTER(1, 3),
    BEGINNER(4, 8),
    INTERMEDIATE(9, 15),
    ADVANCED(16, 25),
    CHALLENGE(26, 35);

    private final int minWords;
    private final int maxWords;
}
