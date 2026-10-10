// 선택지 A — 위치: src/main/java/com/tmi/core/infra/similarity/SimilarityClientConfig.java (신규)
// 변경점: common/config/RestClientConfig 의 Similarity 빈을 외부 시스템 패키지로 이동 → common → infra 역방향 의존 제거 (P3).
//        @Value 필드 주입 → @ConfigurationProperties record.
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
    SimilarityClient similarityClient(SimilarityProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());

        // Boot 4 + 현재 의존성에는 RestClient.Builder 빈이 없다 (current-state §4.6). spring-boot-starter-restclient 를
        // 추가하기로 결정하면(decisions-needed D7) 파라미터로 RestClient.Builder 를 주입받는 형태로 바꾼다.
        RestClient restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(SimilarityClient.class);
    }
}
