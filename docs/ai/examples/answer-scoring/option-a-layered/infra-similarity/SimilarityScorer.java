// 선택지 A′ — 위치: src/main/java/com/tmi/core/infra/similarity/SimilarityScorer.java (신규)
// option-b-modular/similarity/SimilarityScorer.java 와 본문 동일, 패키지·import 만 infra.similarity.
// 외부 예외 → 도메인 예외 변환을 이 클래스가 전담 (AS-IS 는 AnswerService 가 담당). 호출자는 0.0~1.0 double 만 받는다.
package com.tmi.core.infra.similarity;

import com.tmi.core.infra.similarity.dto.SimilarityRequest;
import com.tmi.core.infra.similarity.dto.SimilarityResponse;
import com.tmi.core.infra.similarity.exception.SimilarityException;
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

    private static final double SCORE_TOLERANCE = 1e-6;

    private final SimilarityClient similarityClient;

    /** 0.0 ~ 1.0. 트랜잭션 안에서 호출하지 않는다 (conventions-proposal §4). */
    public double score(String answer, String reference) {
        log.info("Similarity 호출: answerLength={} referenceLength={}", answer.length(), reference.length());
        SimilarityResponse response;
        try {
            response = similarityClient.score(new SimilarityRequest(answer, reference));
        } catch (RestClientResponseException e) {        // 4xx / 5xx 응답 (DefaultResponseErrorHandler 기준)
            throw e.getStatusCode().is4xxClientError()
                    ? new SimilarityException.RequestInvalid(e)
                    : new SimilarityException.Unavailable(e);
        } catch (ResourceAccessException e) {            // 연결 거부 등 I/O 오류 (연결 단계)
            throw new SimilarityException.Unavailable(e);
        } catch (RestClientException e) {
            // SimpleClientHttpRequestFactory 에서는 **read timeout 이 여기로 온다** ("Error while extracting response"
            // → SocketTimeoutException, examples-round-1 리뷰 C1 실행 확인). 응답 역직렬화 실패도 여기.
            // 이 catch 를 지우면 타임아웃이 503 → 500 으로 회귀한다 — 회귀 테스트: 지연 응답 + 짧은 readTimeout.
            throw new SimilarityException.Unavailable(e);
        }
        if (response == null) {                          // 200/204 + 빈 본문 → HTTP Interface 가 null 반환
            throw new SimilarityException.Unavailable(new IllegalStateException("empty similarity response"));
        }
        // 점수 범위 정책 (세 선택지 공통, README §2, decisions-needed D13):
        // 허용 오차(1e-6) 안의 범위 밖 값만 [0, 1] 로 보정하고, 그 밖은 계약(03 §5.1 score 0.0~1.0) 위반으로 Unavailable.
        // 원래 값은 예외 메시지로 남긴다 — 핸들러가 5xx 로 ERROR + 스택 기록 (throw 직전 로그 금지, conventions §5.1).
        double raw = response.score();
        double score = Math.clamp(raw, 0.0, 1.0);
        if (Math.abs(raw - score) > SCORE_TOLERANCE) {
            throw new SimilarityException.Unavailable(new IllegalStateException("similarity score out of range: " + raw));
        }
        log.info("Similarity 응답: score={}", score);
        return score;
    }
}
