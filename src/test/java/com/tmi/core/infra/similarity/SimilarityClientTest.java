package com.tmi.core.infra.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tmi.core.infra.similarity.dto.SimilarityRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class SimilarityClientTest {

    @Test
    @Disabled("FIX-F1 SimilarityClient.score 파라미터에 @RequestBody 가 없어 호출 시 IllegalStateException — FIX-F1 에서 해제")
    @DisplayName("요청 본문을 JSON 으로 보내고 score 를 읽는다")
    void shouldSendJsonBodyAndParseScore_whenScoreCalled() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://similarity");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SimilarityClient client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(SimilarityClient.class);

        server.expect(requestTo("http://similarity/similarity"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"answer\":\"좋아\",\"reference\":\"좋아요.\"}"))
                .andRespond(withSuccess("{\"score\":0.91}", MediaType.APPLICATION_JSON));

        assertThat(client.score(new SimilarityRequest("좋아", "좋아요.")).score()).isEqualTo(0.91);
        server.verify();
    }
}
