// 선택지 B — 위치: src/main/java/com/tmi/core/similarity/config/SimilarityClientConfig.java (internal)
// 선택지 A′ 와 본문 동일. 차이: 패키지(similarity.config), 가시성(B 는 클래스·record 가 package-private — 모듈 internal).
// **같은 PR 에서 common/config/RestClientConfig.java 를 삭제**한다 — 커밋된 코드에는 Similarity 빈만 있으므로 남길 것이 없고,
//   남기면 빈 이름 similarityClient 중복으로 기동 실패 (examples-round-1 리뷰 M1 실행 확인).
// application.properties 키 변경: similarity.timeout-ms=2000 → similarity.connect-timeout=1s, similarity.read-timeout=2s
//   (키를 안 바꿔도 @DefaultValue 로 기동은 되지만, 의도한 값을 명시한다)
package com.tmi.core.similarity.config;

import com.tmi.core.similarity.client.SimilarityClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
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
    record SimilarityProperties(
            String baseUrl,
            @DefaultValue("1s") Duration connectTimeout,     // 누락 시 "ConnectTimeout must not be null" 방지
            @DefaultValue("2s") Duration readTimeout) {}

    @Bean
    SimilarityClient similarityClient(SimilarityProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        // Boot 4 + 현재 의존성에는 RestClient.Builder 빈이 없다 (current-state §4.6) → 정적 builder().
        // spring-boot-starter-restclient 를 추가하기로 결정하면(decisions-needed D7) Builder 주입으로 바꾼다.
        RestClient restClient = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(SimilarityClient.class);
    }
}
