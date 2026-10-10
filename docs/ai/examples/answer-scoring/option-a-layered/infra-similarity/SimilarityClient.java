// 선택지 A′ — 위치: src/main/java/com/tmi/core/infra/similarity/SimilarityClient.java
// 변경점: F1 수정 (@RequestBody) 만. 위치·dto 패키지는 AS-IS 그대로 (FIX-F1 — 구조와 무관).
package com.tmi.core.infra.similarity;

import com.tmi.core.infra.similarity.dto.SimilarityRequest;
import com.tmi.core.infra.similarity.dto.SimilarityResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface SimilarityClient {

    @PostExchange("/similarity")
    SimilarityResponse score(@RequestBody SimilarityRequest request);
}
