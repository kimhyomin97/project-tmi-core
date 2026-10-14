// 선택지 A′ — 위치: src/main/java/com/tmi/core/answer/service/AnswerService.java
// import 하는 타 기능 타입: sentence.service.SentenceService, sentence.dto.SentenceDetail, infra.similarity.SimilarityScorer.
// B 와의 차이는 import 경로뿐 — A′ 에서는 sentence 의 service·dto 패키지 전체가 answer 에게 열려 있다 (규칙으로만 제한).
package com.tmi.core.answer.service;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.answer.dto.AnswerResponse;
import com.tmi.core.answer.dto.SubmitAnswerRequest;
import com.tmi.core.infra.similarity.SimilarityScorer;
import com.tmi.core.sentence.dto.SentenceDetail;
import com.tmi.core.sentence.service.SentenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final SentenceService sentenceService;
    private final SimilarityScorer similarityScorer;

    // 트랜잭션 없음: 조회는 SentenceService.getActive 의 짧은 readOnly 트랜잭션, 외부 호출은 트랜잭션 밖.
    public AnswerResponse score(SubmitAnswerRequest request) {
        SentenceDetail sentence = sentenceService.getActive(request.sentenceId());
        double score = similarityScorer.score(request.userAnswer(), sentence.koreanRef());
        return AnswerResponse.from(sentence, request.userAnswer(), score, Grade.fromScore(score));
    }
}
