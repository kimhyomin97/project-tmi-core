// 선택지 A — 위치: src/main/java/com/tmi/core/infra/similarity/SimilarityClientConfig.java (신규)
// 변경점: common/config/RestClientConfig 의 Similarity 빈을 외부 시스템 패키지로 이동 → common → infra 역방향 의존 제거 (P3).
//        @Value 필드 주입 → @ConfigurationProperties record (WIP 의 @ConfigurationPropertiesScan 과 맞물림).
package com.tmi.core.infra.similarity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SimilarityClientConfig.SimilarityProperties.class)   // 스캔 의존 없이 명시 등록
public class SimilarityClientConfig {

    @ConfigurationProperties("similarity")
    public record SimilarityProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
    // application.properties: similarity.base-url / similarity.connect-timeout=1s / similarity.read-timeout=2s
    // (기존 similarity.timeout-ms 는 이름 변경 — 마이그레이션 시 함께 수정)

    @Bean
    SimilarityClient similarityClient(RestClient.Builder builder, SimilarityProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());

        RestClient restClient = builder               // Boot 가 제공하는 Builder 빈 사용 (current-state §4.6)
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(SimilarityClient.class);
    }
}
