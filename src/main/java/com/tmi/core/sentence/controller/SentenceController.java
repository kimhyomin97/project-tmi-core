package com.tmi.core.sentence.controller;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.domain.SentenceEntity;
import com.tmi.core.sentence.service.SentenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sentences")
@RequiredArgsConstructor
public class SentenceController {

    private final SentenceService sentenceService;

    @GetMapping("/random")
    public ResponseEntity<SentenceEntity> getRandomSentence(
            @RequestParam Level level,
            @RequestParam(defaultValue = "DAILY") Category category) {

        SentenceEntity sentence = sentenceService.getRandomSentence(level, category);
        return ResponseEntity.ok(sentence);
    }

    @GetMapping
    public ResponseEntity<List<SentenceEntity>> getSentences(
            @RequestParam Level level,
            @RequestParam(defaultValue = "DAILY") Category category,
            @RequestParam(defaultValue = "10") int size) {

        List<SentenceEntity> sentences = sentenceService.getSentences(level, category, size);
        return ResponseEntity.ok(sentences);
    }
}
