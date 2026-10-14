// 선택지 C — 위치: src/main/java/com/tmi/core/answer/application/port/out/ScoreSimilarityPort.java (아웃바운드 포트)
package com.tmi.core.answer.application.port.out;

public interface ScoreSimilarityPort {
    double score(String answer, String reference);
}
