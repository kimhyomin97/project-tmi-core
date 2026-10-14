// 선택지 B — 위치: src/main/java/com/tmi/core/sentence/SentenceReader.java (신규, 모듈 공개 API)
// 규칙: sentence 모듈의 base package 에 있는 타입만 다른 모듈이 import 할 수 있다 (ArchUnit R2).
//      repository / domain / exception 하위 패키지는 sentence 내부 전용.
package com.tmi.core.sentence;

import com.tmi.core.sentence.exception.SentenceException;
import com.tmi.core.sentence.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SentenceReader {

    private final SentenceRepository sentenceRepository;

    /**
     * 활성(soft delete 되지 않은) 문장 단건.
     * @throws SentenceException.NotFound 없거나 삭제됨 — 호출 측은 잡지 않고 GlobalExceptionHandler 에 맡긴다.
     */
    @Transactional(readOnly = true)    // 다른 모듈의 빈에서 프록시 경유 호출 → 실제 적용 (F4 구조적 해소)
    public SentenceDetail getActive(String sentenceId) {
        return sentenceRepository.findByIdAndDeletedAtIsNull(sentenceId)
                .map(SentenceDetail::from)  // 트랜잭션 안에서 엔티티 → 공개 record 변환. 엔티티는 모듈 밖으로 나가지 않음
                .orElseThrow(() -> new SentenceException.NotFound(sentenceId));
    }
}
