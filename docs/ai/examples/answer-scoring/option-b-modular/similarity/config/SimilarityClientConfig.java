// 선택지 B — 위치: src/main/java/com/tmi/core/similarity/config/SimilarityClientConfig.java (internal)
// 내용은 선택지 A 의 SimilarityClientConfig 와 같다. 차이는 패키지뿐 (similarity 모듈 내부).
// common/config/RestClientConfig 는 Similarity·Toss 빈이 각 모듈로 빠지면 삭제된다.
package com.tmi.core.similarity.config;

import com.tmi.core.similarity.client.SimilarityClient;
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
class SimilarityClientConfig {

    @ConfigurationProperties("similarity")
    record SimilarityProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {}

    @Bean
    SimilarityClient similarityClient(RestClient.Builder builder, SimilarityProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        RestClient restClient = builder.baseUrl(props.baseUrl()).requestFactory(factory).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(SimilarityClient.class);
    }
}
