package com.tmi.core.infra.similarity.exception;

import com.tmi.core.common.exception.BaseException;
import com.tmi.core.common.exception.ETmiErrorType;
import org.springframework.http.HttpStatus;

public class SimilarityException {

    private SimilarityException() {}

    // Similarity 서버 요청 형식 오류 (4xx)
    public static class RequestInvalid extends BaseException {
        public RequestInvalid(Throwable cause) {
            super(HttpStatus.BAD_GATEWAY, ETmiErrorType.SIMILARITY_REQUEST_INVALID, cause);
        }
    }

    // Similarity 서버 다운, 타임아웃, 네트워크 오류 (5xx / 네트워크)
    public static class Unavailable extends BaseException {
        public Unavailable(Throwable cause) {
            super(HttpStatus.SERVICE_UNAVAILABLE, ETmiErrorType.SIMILARITY_UNAVAILABLE, cause);
        }
    }
}
