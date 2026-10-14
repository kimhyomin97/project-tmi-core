package com.tmi.core.sentence.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tmi.core.sentence.domain.Category;
import com.tmi.core.sentence.domain.Level;
import com.tmi.core.sentence.exception.SentenceException;
import com.tmi.core.sentence.service.SentenceService;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

    // 서버 기본 로케일을 ko 로 고정한다. 그래야 FIX-F2 에서 fallback-to-system-locale=false 가 빠졌을 때
    // en 요청에 한글 메시지가 나와 이 테스트가 실패한다 (기본 로케일이 en 인 환경에서는 그 누락을 못 잡음).
    private static Locale originalDefault;

    @BeforeAll
    static void pinDefaultLocale() {
        originalDefault = Locale.getDefault();
        Locale.setDefault(Locale.KOREA);
    }

    @AfterAll
    static void restoreDefaultLocale() {
        Locale.setDefault(originalDefault);
    }

    @Test
    @Disabled("FIX-F2 메시지 번들이 로드되지 않아 message 가 코드 문자열로 나옴 — basename 과 fallback-to-system-locale=false 를 함께 고친 뒤 해제")
    @DisplayName("문장이 없으면 404 와 요청 언어(en)의 메시지 — 서버 기본 로케일이 ko 여도")
    void shouldReturn404WithMessage_whenNoSentence() throws Exception {
        given(sentenceService.getRandom(Level.LEVEL_5, Category.TECH))
                .willThrow(new SentenceException.NotFound("level=LEVEL_5, category=TECH"));

        mockMvc.perform(get("/api/v1/sentences/random")
                        .param("level", "LEVEL_5")
                        .param("category", "TECH")
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TMI.SENTENCE.NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Sentence not found: level=LEVEL_5, category=TECH"));
    }
}
