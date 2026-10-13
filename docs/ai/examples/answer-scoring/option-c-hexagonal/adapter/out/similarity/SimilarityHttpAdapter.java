// 선택지 C — 위치: src/main/java/com/tmi/core/answer/adapter/out/similarity/SimilarityHttpAdapter.java
// ScoreSimilarityPort 구현. 본문 = option-b-modular/similarity/SimilarityScorer.java 의 score() 와 **동일**
// (catch 3개, 빈 응답 → Unavailable, 허용 오차 보정). 예시 중복을 줄이려고 본문은 생략 — 그대로 복사해야 하며,
// 아래 한 줄만 쓰면 AS-IS 결함(timeout 500, 빈 응답 NPE)이 남는다.
// SimilarityClient 는 B 처럼 요청·응답을 중첩 record 로 가진 형태를 전제한다.
package com.tmi.core.answer.adapter.out.similarity;

import com.tmi.core.answer.application.port.out.ScoreSimilarityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SimilarityHttpAdapter implements ScoreSimilarityPort {

    private final SimilarityClient similarityClient;

    @Override
    public double score(String answer, String reference) {
        // TODO(예시): option-b-modular/similarity/SimilarityScorer.java 의 score() 본문 복사
        throw new UnsupportedOperationException("example — copy SimilarityScorer.score body");
    }
}
