// 선택지 C — 위치: src/main/java/com/tmi/core/answer/application/port/in/ScoreAnswerUseCase.java (인바운드 포트)
package com.tmi.core.answer.application.port.in;

import com.tmi.core.answer.domain.AnswerScore;

public interface ScoreAnswerUseCase {

    Result score(Command command);

    record Command(String sentenceId, String userAnswer) {}

    record Result(String sentenceId, String englishText, String userAnswer, String correctAnswer, AnswerScore score) {}
}
