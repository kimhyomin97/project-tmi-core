package com.tmi.core.sentence.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Level {

    LEVEL_1(1, 3),      // STARTER
    LEVEL_2(4, 8),      // BEGINNER
    LEVEL_3(9, 15),     // INTERMEDIATE
    LEVEL_4(16, 25),    // ADVANCED
    LEVEL_5(26, 35);    // CHALLENGE

    private final int minWords;
    private final int maxWords;
}
