// 선택지 A — 위치: src/main/java/com/tmi/core/answer/service/AnswerService.java
// 변경점: SentenceRepository·SentenceException 직접 의존 제거 → SentenceService 경유.
//        self-invocation 되던 @Transactional loadSentence 삭제 (트랜잭션은 SentenceService.getActive 가 담당).
//        외부 예외 변환은 여전히 이 서비스에 있다 (선택지 A 의 한계 — B 에서는 similarity 모듈로 이동).
package com.tmi.core.answer.service;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.answer.dto.AnswerResponse;
import com.tmi.core.answer.dto.SubmitAnswerRequest;
import com.tmi.core.infra.similarity.SimilarityClient;
import com.tmi.core.infra.similarity.dto.SimilarityRequest;
import com.tmi.core.infra.similarity.exception.SimilarityException;
import com.tmi.core.sentence.domain.SentenceEntity;          // ← A 에서 남는 기능 간 엔티티 결합
import com.tmi.core.sentence.service.SentenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final SentenceService sentenceService;
    private final SimilarityClient similarityClient;

    // 트랜잭션 없음 — 조회(짧은 readOnly 트랜잭션, 다른 빈) → 외부 호출(트랜잭션 밖)
    public AnswerResponse score(SubmitAnswerRequest request) {
        SentenceEntity sentence = sentenceService.getActive(request.sentenceId());
        double score = callSimilarity(request.userAnswer(), sentence.getKoreanRef());
        return AnswerResponse.from(sentence, request.userAnswer(), score, Grade.fromScore(score));
    }

    private double callSimilarity(String userAnswer, String reference) {
        log.info("Similarity 호출: userAnswerLength={} referenceLength={}", userAnswer.length(), reference.length());
        try {
            double score = similarityClient.score(new SimilarityRequest(userAnswer, reference)).score();
            log.info("Similarity 응답: score={}", score);
            return score;
        } catch (RestClientResponseException e) {               // AS-IS HttpStatusCodeException → UnknownHttpStatusCodeException 까지 포함
            throw e.getStatusCode().is4xxClientError()
                    ? new SimilarityException.RequestInvalid(e)
                    : new SimilarityException.Unavailable(e);
        } catch (ResourceAccessException e) {
            throw new SimilarityException.Unavailable(e);       // 로그는 GlobalExceptionHandler 단일 지점 (이중 로깅 제거)
        }
    }
}
