// 선택지 B — 위치: src/main/java/com/tmi/core/similarity/client/SimilarityClient.java (internal)
// AS-IS: infra/similarity/SimilarityClient.java + infra/similarity/dto/{SimilarityRequest,SimilarityResponse}.java
// 요청·응답 record 는 이 클라이언트 전용이라 중첩 타입으로 합쳤다 (파일 3 → 1). 별도 파일 유지도 무방.
package com.tmi.core.similarity.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface SimilarityClient {

    @PostExchange("/similarity")
    SimilarityResponse score(@RequestBody SimilarityRequest request);    // F1 수정 — ArchUnit R7 이 누락을 잡는다

    record SimilarityRequest(String answer, String reference) {}

    record SimilarityResponse(double score) {}
}
