package br.com.featureflagsdkjava.domain.application.openfeature;

import br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider.OpenFeatureCustomProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up OpenFeature beans.
 */
@Configuration
@Slf4j
public class OpenFeatureConfig {

    /**
     * Configuration class for setting up OpenFeature beans
     */
    @Bean
    public Client buildOpenFeatureClientBean(OpenFeatureCustomProvider openFeatureCustomProvider) throws OpenFeatureError {
        final OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();

        openFeatureAPI.setProviderAndWait(openFeatureCustomProvider);
        return openFeatureAPI.getClient();
    }
}