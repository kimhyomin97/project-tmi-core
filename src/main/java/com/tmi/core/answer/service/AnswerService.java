package com.tmi.core.answer.service;

import com.tmi.core.answer.dto.AnswerResponse;
import com.tmi.core.answer.dto.SubmitAnswerRequest;
import com.tmi.core.infra.similarity.SimilarityClient;
import com.tmi.core.infra.similarity.dto.SimilarityRequest;
import com.tmi.core.infra.similarity.dto.SimilarityResponse;
import com.tmi.core.infra.similarity.exception.SimilarityException;
import com.tmi.core.sentence.domain.SentenceEntity;
import com.tmi.core.sentence.exception.SentenceException;
import com.tmi.core.sentence.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final SentenceRepository sentenceRepository;
    private final SimilarityClient similarityClient;

    public AnswerResponse score(SubmitAnswerRequest request) {
        SentenceEntity sentence = loadSentence(request.sentenceId());

        double similarityScore = callSimilarity(request.userAnswer(), sentence.getKoreanRef());

        return AnswerResponse.of(sentence, request.userAnswer(), similarityScore);
    }

    @Transactional(readOnly = true)
    protected SentenceEntity loadSentence(String sentenceId) {
        return sentenceRepository.findByIdAndDeletedAtIsNull(sentenceId)
                .orElseThrow(() -> new SentenceException.NotFound(sentenceId));
    }

    private double callSimilarity(String userAnswer, String reference) {
        log.info("Similarity 호출 : userAnswerLength={} referenceLength={}", userAnswer.length(), reference.length());
        try {
            SimilarityResponse response = similarityClient.score(new SimilarityRequest(userAnswer, reference));
            log.info("Similarity 응답: score={}", response.score());
            return response.score();
        } catch(HttpStatusCodeException e) {
            if(e.getStatusCode().is4xxClientError()) {
                throw new SimilarityException.RequestInvalid(e);
            }
            throw new SimilarityException.Unavailable(e);
        } catch(ResourceAccessException e) {
            log.error("Similarity 네트워크 오류", e);
            throw new SimilarityException.Unavailable(e);
        }
    }
}
