package com.tmi.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ETmiErrorType implements IErrorType{

    // 공통
    INVALID_REQUEST     ("TMI.COMMON.INVALID_REQUEST"),
    INTERNAL_ERROR      ("TMI.COMMON.INTERNAL_ERROR"),
    NOT_FOUND_URL       ("TMI.COMMON.NOT_FOUND_URL"),
    METHOD_NOT_SUPPORTED("TMI.COMMON.METHOD_NOT_SUPPORTED"),
    MISSING_PARAMETER   ("TMI.COMMON.MISSING_PARAMETER"),
    BAD_REQUEST         ("TMI.COMMON.BAD_REQUEST"),

    // Sentence
    SENTENCE_NOT_FOUND  ("TMI.SENTENCE.NOT_FOUND"),

    // Similarity
    SIMILARITY_REQUEST_INVALID("TMI.SIMILARITY.REQUEST_INVALID"),
    SIMILARITY_UNAVAILABLE    ("TMI.SIMILARITY.UNAVAILABLE"),
    ;

    private final String errorCode;
}
