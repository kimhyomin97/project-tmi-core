package com.tmi.core.sentence.controller;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import com.tmi.core.sentence.dto.SentenceResponse;
import com.tmi.core.sentence.service.SentenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.version}/sentences")
@RequiredArgsConstructor
public class SentenceController {

    private final SentenceService sentenceService;

    @GetMapping("/random")
    public ResponseEntity<SentenceResponse> getNext(@RequestParam Level level, @RequestParam Category category) {
        SentenceEntity sentence = sentenceService.getRandom(level, category);
        return ResponseEntity.ok(SentenceResponse.from(sentence));
    }

}
