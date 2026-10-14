// 선택지 C — 위치: src/main/java/com/tmi/core/answer/application/service/ScoreAnswerService.java
// 유스케이스 구현. 포트와 answer 자신의 타입에만 의존 — sentence·JPA·HTTP 클라이언트 타입을 모른다.
package com.tmi.core.answer.application.service;

import com.tmi.core.answer.application.port.in.ScoreAnswerUseCase;
import com.tmi.core.answer.application.port.out.LoadSentencePort;
import com.tmi.core.answer.application.port.out.LoadSentencePort.SentenceForScoring;
import com.tmi.core.answer.application.port.out.ScoreSimilarityPort;
import com.tmi.core.answer.domain.AnswerScore;
import com.tmi.core.answer.exception.AnswerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ScoreAnswerService implements ScoreAnswerUseCase {

    private final LoadSentencePort loadSentencePort;
    private final ScoreSimilarityPort scoreSimilarityPort;

    @Override
    public Result score(Command command) {
        SentenceForScoring sentence = loadSentencePort.loadActive(command.sentenceId())
                // answer 소유 예외 (기존 빈 홀더 answer/exception/AnswerException 에 중첩 클래스 추가,
                // 코드는 기존 ETmiErrorType.SENTENCE_NOT_FOUND 재사용 — 응답 계약 불변)
                .orElseThrow(() -> new AnswerException.SentenceNotFound(command.sentenceId()));
        AnswerScore score = AnswerScore.of(scoreSimilarityPort.score(command.userAnswer(), sentence.koreanRef()));
        return new Result(sentence.id(), sentence.englishText(), command.userAnswer(), sentence.koreanRef(), score);
    }
}
