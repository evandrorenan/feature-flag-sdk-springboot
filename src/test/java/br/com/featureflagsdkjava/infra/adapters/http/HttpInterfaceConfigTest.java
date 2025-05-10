package br.com.featureflagsdkjava.infra.adapters.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HttpInterfaceConfigTest {

    @Test
    void shouldReturnFeatureFlagProxy() {
        FeatureFlagServiceProxy featureFlagServiceProxy = new HttpInterfaceConfig().featureFlagClient();
        assertNotNull(featureFlagServiceProxy);
    }
}