package com.tmi.core.sentence.exception;

import com.tmi.core.common.exception.BaseException;
import com.tmi.core.common.exception.ETmiErrorType;
import com.tmi.core.common.exception.IErrorType;
import org.springframework.http.HttpStatus;

public class SentenceException  {

    private SentenceException() {}

    // 조건에 맞는 영어 문장이 없음
    public static class NotFound extends BaseException {
        public NotFound(String detail) {
            super(HttpStatus.NOT_FOUND, ETmiErrorType.SENTENCE_NOT_FOUND, detail);
        }
    }

}
