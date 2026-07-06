package com.tmi.core.answer.controller;

import com.tmi.core.answer.dto.AnswerResponse;
import com.tmi.core.answer.dto.SubmitAnswerRequest;
import com.tmi.core.answer.service.AnswerService;
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
public class AnswerController implements AnswerApi{

    private final AnswerService answerService;

    @Override
    @PostMapping
    public ResponseEntity<AnswerResponse> submit(@Valid @RequestBody SubmitAnswerRequest request) {
        AnswerResponse result = answerService.score(request);
        return ResponseEntity.ok(result);
    }
}
