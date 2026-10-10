// 선택지 C — 위치: src/main/java/com/tmi/core/answer/application/service/ScoreAnswerService.java
// 유스케이스 구현. 포트에만 의존 — JPA·HTTP 클라이언트 타입을 모른다.
package com.tmi.core.answer.application.service;

import com.tmi.core.answer.application.port.in.ScoreAnswerUseCase;
import com.tmi.core.answer.application.port.out.LoadSentencePort;
import com.tmi.core.answer.application.port.out.ScoreSimilarityPort;
import com.tmi.core.answer.domain.AnswerScore;
import com.tmi.core.sentence.domain.Sentence;
import com.tmi.core.sentence.exception.SentenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ScoreAnswerService implements ScoreAnswerUseCase {

    private final LoadSentencePort loadSentencePort;
    private final ScoreSimilarityPort scoreSimilarityPort;

    @Override
    public Result score(Command command) {
        Sentence sentence = loadSentencePort.loadActive(command.sentenceId())
                .orElseThrow(() -> new SentenceException.NotFound(command.sentenceId()));
        AnswerScore score = AnswerScore.of(scoreSimilarityPort.score(command.userAnswer(), sentence.koreanRef()));
        return new Result(sentence.id(), sentence.englishText(), command.userAnswer(), sentence.koreanRef(), score);
    }
}
