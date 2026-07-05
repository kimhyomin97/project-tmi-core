package com.tmi.core.infra.similarity;

import com.tmi.core.infra.similarity.dto.SimilarityRequest;
import com.tmi.core.infra.similarity.dto.SimilarityResponse;
import org.springframework.web.service.annotation.PostExchange;

public interface SimilarityClient {

    @PostExchange("/similarity")
    SimilarityResponse score(SimilarityRequest request);
}
