// 선택지 C — 위치: src/main/java/com/tmi/core/answer/application/port/out/LoadSentencePort.java (아웃바운드 포트)
// answer 가 필요로 하는 것을 answer 쪽에서 정의한다 (의존성 역전, "required interface").
// 반환 타입도 answer 소유 record — sentence.domain 을 import 하지 않으므로 의존은 sentence → answer(포트) 단방향.
// (examples-round-2 M1: 처음 예시는 sentence.domain.Sentence 를 반환해 answer ↔ sentence 순환이었다.)
package com.tmi.core.answer.application.port.out;

import java.util.Optional;

public interface LoadSentencePort {

    Optional<SentenceForScoring> loadActive(String sentenceId);

    /** 채점에 필요한 필드만. sentence 의 도메인 모델과 독립. */
    record SentenceForScoring(String id, String englishText, String koreanRef) {}
}
