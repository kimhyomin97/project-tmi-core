package com.tmi.core.common.config;

import com.tmi.core.infra.similarity.SimilarityClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${similarity.base-url}")
    private String similarityBaseUrl;

    @Value("${similarity.timeout-ms}")
    private long similarityTimeoutMs;

    @Bean
    public SimilarityClient similarityClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofMillis(similarityTimeoutMs));

        RestClient restClient = RestClient.builder()
                .baseUrl(similarityBaseUrl)
                .requestFactory(factory)
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(SimilarityClient.class);
    }
}
