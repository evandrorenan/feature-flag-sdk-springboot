package br.com.featureflagsdkjava.infra.config;

import br.com.featureflagsdkjava.infra.adapters.http.FeatureFlagServiceProxy;
import br.com.featureflagsdkjava.infra.adapters.http.HttpInterfaceConfig;
import io.github.jamsesso.jsonlogic.JsonLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonLogicConfigTest {

    @Test
    void shouldReturnJsonLogicBean() {
        JsonLogic jsonLogic = new JsonLogicConfig().buildJsonLogicBean();
        assertNotNull(jsonLogic);
    }
}