// 선택지 C — 위치: src/main/java/com/tmi/core/answer/exception/AnswerException.java (AS-IS 빈 홀더에 중첩 클래스 추가)
// answer 가 sentence 의 예외를 쓰지 않도록 answer 소유 예외를 둔다. 오류 코드는 기존 SENTENCE_NOT_FOUND 재사용 → 응답 계약 불변.
package com.tmi.core.answer.exception;

import com.tmi.core.common.exception.BaseException;
import com.tmi.core.common.exception.ETmiErrorType;
import org.springframework.http.HttpStatus;

public class AnswerException {

    private AnswerException() {}

    public static class SentenceNotFound extends BaseException {
        public SentenceNotFound(String sentenceId) {
            super(HttpStatus.NOT_FOUND, ETmiErrorType.SENTENCE_NOT_FOUND, sentenceId);
        }
    }
}
