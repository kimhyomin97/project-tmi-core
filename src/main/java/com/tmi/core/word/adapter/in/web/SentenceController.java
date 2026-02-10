package com.tmi.core.word.adapter.in.web;

import com.tmi.core.word.domain.model.Difficulty;
import com.tmi.core.word.domain.model.Sentence;
import com.tmi.core.word.domain.port.in.GetSentenceUseCase;
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

    private final GetSentenceUseCase getSentenceUseCase;

    @GetMapping("/random")
    public ResponseEntity<Sentence> getRandomSentence(
            @RequestParam Difficulty difficulty,
            @RequestParam(defaultValue = "DAILY") String category) {

        Sentence sentence = getSentenceUseCase.getRandomSentence(difficulty, category);
        return ResponseEntity.ok(sentence);
    }

    @GetMapping
    public ResponseEntity<List<Sentence>> getSentences(
            @RequestParam Difficulty difficulty,
            @RequestParam(defaultValue = "DAILY") String category,
            @RequestParam(defaultValue = "10") int size) {

        List<Sentence> sentences = getSentenceUseCase.getSentences(difficulty, category, size);
        return ResponseEntity.ok(sentences);
    }
}
