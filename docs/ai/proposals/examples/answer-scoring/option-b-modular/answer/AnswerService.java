// 선택지 B — 위치: src/main/java/com/tmi/core/answer/service/AnswerService.java
// import 가 다른 모듈의 base package(공개 API) 2개뿐이다: sentence.SentenceReader/SentenceDetail, similarity.SimilarityScorer.
// AS-IS 의 import 7개 (sentence.repository / sentence.domain / sentence.exception / infra.similarity.*) 가 모두 사라짐.
package com.tmi.core.answer.service;

import com.tmi.core.answer.domain.Grade;
import com.tmi.core.answer.dto.AnswerResponse;
import com.tmi.core.answer.dto.SubmitAnswerRequest;
import com.tmi.core.sentence.SentenceDetail;
import com.tmi.core.sentence.SentenceReader;
import com.tmi.core.similarity.SimilarityScorer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final SentenceReader sentenceReader;
    private final SimilarityScorer similarityScorer;

    // 트랜잭션 없음: 조회는 SentenceReader 의 짧은 readOnly 트랜잭션, 외부 호출은 트랜잭션 밖.
    // 이력 저장이 생기면 저장 전용 빈(예: AnswerHistoryWriter.save, @Transactional)을 하나 더 호출한다.
    public AnswerResponse score(SubmitAnswerRequest request) {
        SentenceDetail sentence = sentenceReader.getActive(request.sentenceId());
        double score = similarityScorer.score(request.userAnswer(), sentence.koreanRef());
        return AnswerResponse.from(sentence, request.userAnswer(), score, Grade.fromScore(score));
    }
}
