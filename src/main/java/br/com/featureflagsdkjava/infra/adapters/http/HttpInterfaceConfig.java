package br.com.featureflagsdkjava.infra.adapters.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpInterfaceConfig {

    @Value("${feature.flag.service.url}")
    private String featureFlagServiceUrl;

    @Bean
    public FeatureFlagServiceClient featureFlagClient() {
        WebClient webClient = WebClient.builder()
                                       .baseUrl(featureFlagServiceUrl)
                                       .build();

        return HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build()
                .createClient(FeatureFlagServiceClient.class);
    }
}