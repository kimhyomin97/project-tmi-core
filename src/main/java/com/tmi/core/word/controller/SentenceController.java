package com.tmi.core.word.controller;

import com.tmi.core.word.domain.Difficulty;
import com.tmi.core.word.domain.SentenceEntity;
import com.tmi.core.word.service.SentenceService;
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
            @RequestParam Difficulty difficulty,
            @RequestParam(defaultValue = "DAILY") String category) {

        SentenceEntity sentence = sentenceService.getRandomSentence(difficulty, category);
        return ResponseEntity.ok(sentence);
    }

    @GetMapping
    public ResponseEntity<List<SentenceEntity>> getSentences(
            @RequestParam Difficulty difficulty,
            @RequestParam(defaultValue = "DAILY") String category,
            @RequestParam(defaultValue = "10") int size) {

        List<SentenceEntity> sentences = sentenceService.getSentences(difficulty, category, size);
        return ResponseEntity.ok(sentences);
    }
}
