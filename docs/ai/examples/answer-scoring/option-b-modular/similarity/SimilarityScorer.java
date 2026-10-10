// 선택지 B — 위치: src/main/java/com/tmi/core/similarity/SimilarityScorer.java (신규, 모듈 공개 API)
// 기존 infra.similarity 를 독립 모듈 similarity 로 승격. HTTP Interface 와 요청·응답 record 는 similarity.client (internal).
// 외부 예외 → 도메인 예외 변환을 이 클래스가 전담 (AS-IS 는 AnswerService 가 담당). 호출자는 double 만 받는다.
package com.tmi.core.similarity;

import com.tmi.core.similarity.client.SimilarityClient;
import com.tmi.core.similarity.client.SimilarityClient.SimilarityRequest;
import com.tmi.core.similarity.exception.SimilarityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityScorer {

    private final SimilarityClient similarityClient;

    /** 0.0 ~ 1.0. 트랜잭션 안에서 호출하지 않는다 (conventions-proposal §4). */
    public double score(String answer, String reference) {
        log.info("Similarity 호출: answerLength={} referenceLength={}", answer.length(), reference.length());
        try {
            double score = similarityClient.score(new SimilarityRequest(answer, reference)).score();
            log.info("Similarity 응답: score={}", score);
            return score;
        } catch (RestClientResponseException e) {        // 4xx/5xx + 비표준 상태 코드
            throw e.getStatusCode().is4xxClientError()
                    ? new SimilarityException.RequestInvalid(e)
                    : new SimilarityException.Unavailable(e);
        } catch (ResourceAccessException e) {            // 연결 거부·타임아웃
            throw new SimilarityException.Unavailable(e);
        } catch (RestClientException e) {                // 응답 역직렬화 실패 등 — AS-IS 는 500 으로 누수 (current-state §3.3f)
            throw new SimilarityException.Unavailable(e);
        }
    }
}
