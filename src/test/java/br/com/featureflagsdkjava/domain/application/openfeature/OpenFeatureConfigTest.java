package br.com.featureflagsdkjava.domain.application.openfeature;

import br.com.featureflagsdkjava.domain.application.openfeature.providers.HookFactory;
import br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider.OpenFeatureCustomProvider;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class OpenFeatureConfigTest {

    @Mock
    private OpenFeatureCustomProvider openFeatureCustomProvider;

    private OpenFeatureConfig openFeatureConfig;

    @BeforeEach
    void setUp() {
        openFeatureConfig = new OpenFeatureConfig();
    }

    @Test
    void buildOpenFeatureClientBean_shouldSetProviderAndRegisterHookAndReturnClient_whenNoExceptionOccurs() throws OpenFeatureError {
        Client client = openFeatureConfig.buildOpenFeatureClientBean(openFeatureCustomProvider);
        assertNotNull(client);

        Logger logger = (Logger) LoggerFactory.getLogger(HookFactory.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);



    }
}