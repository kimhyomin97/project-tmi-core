// 선택지 A′ — 위치: src/main/java/com/tmi/core/sentence/dto/SentenceDetail.java (신규)
// 다른 기능(answer)에 넘기는 읽기 전용 record. B 의 sentence.SentenceDetail 과 내용이 같고 위치만 다르다.
package com.tmi.core.sentence.dto;

import com.tmi.core.sentence.domain.SentenceEntity;

public record SentenceDetail(String id, String englishText, String koreanRef) {

    public static SentenceDetail from(SentenceEntity entity) {   // public: A′ 에서는 service 패키지가 호출
        return new SentenceDetail(entity.getId(), entity.getEnglishText(), entity.getKoreanRef());
    }
}
