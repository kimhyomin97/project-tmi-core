// 선택지 C — 위치: src/main/java/com/tmi/core/sentence/domain/Sentence.java (신규, 순수 도메인 모델)
// AS-IS SentenceEntity 는 JPA 어노테이션을 가진 채 도메인 역할을 겸한다. C 에서는 둘을 분리하고 매퍼로 잇는다.
// 이 파일은 answer 예시에 필요한 범위만 보여준다 (sentence 기능 전체가 같은 방식으로 분리돼야 함).
package com.tmi.core.sentence.domain;

public record Sentence(String id, String englishText, String koreanRef, Level level, Category category) {}
