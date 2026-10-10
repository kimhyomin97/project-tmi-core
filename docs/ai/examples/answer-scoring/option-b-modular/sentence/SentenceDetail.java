// 선택지 B — 위치: src/main/java/com/tmi/core/sentence/SentenceDetail.java (신규, 모듈 공개 record)
// 엔티티(SentenceEntity) 대신 다른 모듈에 노출하는 읽기 전용 값. 타 모듈이 실제로 쓰는 필드만 담는다.
package com.tmi.core.sentence;

import com.tmi.core.sentence.domain.SentenceEntity;

public record SentenceDetail(String id, String englishText, String koreanRef) {

    static SentenceDetail from(SentenceEntity entity) {     // package-private: 변환은 sentence 모듈 안에서만
        return new SentenceDetail(entity.getId(), entity.getEnglishText(), entity.getKoreanRef());
    }
}
