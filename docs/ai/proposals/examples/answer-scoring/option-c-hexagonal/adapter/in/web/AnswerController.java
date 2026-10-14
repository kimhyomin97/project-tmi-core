// 선택지 C — 위치: src/main/java/com/tmi/core/answer/adapter/in/web/AnswerController.java
// 웹 DTO(SubmitAnswerRequest, AnswerResponse)·AnswerApi 는 이 어댑터 패키지로 이동하고, 유스케이스 Command/Result 와 매핑한다.
// package-private 이라 @WebMvcTest(AnswerController.class) 는 같은 패키지의 테스트에서만 쓸 수 있다 (테스트 컨벤션 영향).
package com.tmi.core.answer.adapter.in.web;

import com.tmi.core.answer.application.port.in.ScoreAnswerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.version}/answers")
@RequiredArgsConstructor
class AnswerController implements AnswerApi {

    private final ScoreAnswerUseCase scoreAnswerUseCase;

    @Override
    @PostMapping
    public ResponseEntity<AnswerResponse> submit(@Valid @RequestBody SubmitAnswerRequest request) {
        var result = scoreAnswerUseCase.score(new ScoreAnswerUseCase.Command(request.sentenceId(), request.userAnswer()));
        return ResponseEntity.ok(AnswerResponse.from(result));   // Result → 웹 응답 매핑 (Two-Way 매핑 전략)
    }
}
