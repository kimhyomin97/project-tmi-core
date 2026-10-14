package com.tmi.core.sentence.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.exception.SentenceException;
import com.tmi.core.sentence.service.SentenceService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SentenceController.class)
class SentenceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SentenceService sentenceService;

    @Test
    @Disabled("FIX-F2 메시지 번들(spring.messages.basename)이 로드되지 않아 message 가 코드 문자열로 나옴 — FIX-F2 에서 해제")
    @DisplayName("문장이 없으면 404 와 영문 메시지")
    void shouldReturn404WithMessage_whenNoSentence() throws Exception {
        given(sentenceService.getRandom(Level.LEVEL_5, Category.TECH))
                .willThrow(new SentenceException.NotFound("level=LEVEL_5, category=TECH"));

        mockMvc.perform(get("/api/v1/sentences/random")
                        .param("level", "LEVEL_5")
                        .param("category", "TECH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TMI.SENTENCE.NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Sentence not found: level=LEVEL_5, category=TECH"));
    }
}
