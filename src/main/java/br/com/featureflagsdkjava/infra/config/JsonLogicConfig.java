package br.com.featureflagsdkjava.infra.config;

import io.github.jamsesso.jsonlogic.JsonLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonLogicConfig {

    @Bean
    public JsonLogic buildJsonLogicBean() {
        return new JsonLogic();
    }
}
