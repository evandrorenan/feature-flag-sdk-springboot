package br.com.featureflagsdkjava.domain.openfeature.providers.customprovider;

import br.com.featureflagsdkjava.domain.application.openfeature.OpenFeatureConfig;
import br.com.featureflagsdkjava.domain.application.openfeature.providers.HookFactory;
import br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider.OpenFeatureCustomProvider;
import br.com.featureflagsdkjava.domain.model.Flag;
import br.com.featureflagsdkjava.domain.ports.FeatureFlagQueryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import io.github.jamsesso.jsonlogic.JsonLogic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OpenFeatureCustomProviderTest {

    @Mock
    private FeatureFlagQueryPort featureFlagQueryPort;
    private Client client;
    private final ObjectMapper mapper = new ObjectMapper();
    private FeatureProvider customProvider;

    @BeforeEach
    void beforeAll() {
        this.customProvider  =
                new OpenFeatureCustomProvider(featureFlagQueryPort, new JsonLogic());

        client = new OpenFeatureConfig().buildOpenFeatureClientBean(customProvider);
        assertNotNull(client);
    }

    @Test
    void getMetadata_shouldReturnCorrectName() {
        assertEquals("CustomProvider", customProvider.getMetadata().getName());
    }

    @Test
    void getBooleanEvaluationWithHook_flagNotFound_shouldReturnDefaultValueWithErrorReason(CapturedOutput logConsole) {
        String flagName = "testFlag";
        Boolean defaultValue = true;
        EvaluationContext evaluationContext = new ImmutableContext();
        when(featureFlagQueryPort.findByFlagName(flagName)).thenThrow(new FlagNotFoundError("Mocked Exception"));

        Boolean booleanValue = client.getBooleanValue(flagName, defaultValue, evaluationContext);

        assertHooks(flagName, Reason.ERROR, logConsole);
        assertEquals(defaultValue, booleanValue);
    }

    @Test
    void getBooleanStaticEvaluationWithHook_shouldReturnDefaultValueWithStaticReason(CapturedOutput logConsole) throws JsonProcessingException {
        String flagName = "testFlag";
        Boolean defaultValue = true;
        Flag basicBooleanFlag = getBasicBooleanFlag();

        when(featureFlagQueryPort.findByFlagName(flagName)).thenReturn(Optional.of(basicBooleanFlag));

        Boolean booleanValue = client.getBooleanValue(flagName, defaultValue);

        assertHooks(flagName, Reason.STATIC, logConsole);
        assertEquals(false, booleanValue);
    }

    private void assertHooks(String flagName, Reason reason, CapturedOutput logConsole) {
        String logConsoleStr = logConsole.toString();
        assertTrue(logConsoleStr.contains(HookFactory.STARTING_FLAG_EVALUATION.replace("{}", flagName)));
        assertTrue(logConsoleStr.contains(HookFactory.FINALIZING_FLAG_EVALUATION.replace("{}", flagName)));
        if (!reason.equals(Reason.ERROR)) {
            assertTrue(logConsoleStr.contains(
                HookFactory.FLAG_EVALUATED_SUCCESSFULLY
                           .replaceFirst("\\{}", flagName).replace("{}.", "")));
            return;
        }
        assertTrue(logConsoleStr.contains(
            HookFactory.FLAG_EVALUATION_FAILED_EXCEPTION
                       .replaceFirst("\\{}", flagName).replace("{}", "")));
    }

    @Test
    void getBooleanEvaluation_flagDisabled_shouldReturnDefaultValueWithErrorReason() {
        String flagName = "testFlag";
        Boolean defaultValue = true;
        EvaluationContext evaluationContext = new ImmutableContext();

        when(featureFlagQueryPort.findByFlagName(flagName)).thenThrow(new FlagNotFoundError("Disabled flag"));

        FlagEvaluationDetails<Boolean> booleanDetails = client.getBooleanDetails(flagName, defaultValue, evaluationContext);

        assertEquals(defaultValue, booleanDetails.getValue());
        assertEquals(Reason.ERROR.toString(), booleanDetails.getReason());
    }

    @Test
    void getBooleanEvaluation_missingDefaultVariant_shouldReturnDefaultValueWithErrorReason() {
        String flagName = "testFlag";
        Boolean defaultValue = true;
        EvaluationContext evaluationContext = new ImmutableContext();
        Flag flag = Flag.builder().name(flagName).state(Flag.State.ENABLED).variants(Map.of("on", "true")).build();
        when(featureFlagQueryPort.findByFlagName(flagName)).thenReturn(Optional.of(flag));

        FlagEvaluationDetails<Boolean> booleanDetails = client.getBooleanDetails(flagName, defaultValue, evaluationContext);

        assertEquals(defaultValue, booleanDetails.getValue());
        assertEquals(Reason.ERROR.toString(), booleanDetails.getReason());
    }

    @Test
    void getStaticEvaluation_shouldReturnVariantValueWithStaticReason() throws JsonProcessingException {
        String flagName = "testFlag";

        assertEvaluation(flagName, "String", "2", "2");
        assertEvaluation(flagName, "Integer", 2, 2);
        assertEvaluation(flagName, "Double", 2.0, 2.0);
        assertEvaluation(flagName, "Boolean", false, false);
    }

    private <T> void assertEvaluation(String flagName, String type, T defaultValue, T expectedValue) throws JsonProcessingException {
        Flag flag = getBasicStaticFlag(type);
        when(featureFlagQueryPort.findByFlagName(flagName)).thenReturn(Optional.of(flag));

        FlagEvaluationDetails<?> evaluation;
        if (defaultValue instanceof String s) {
            evaluation = client.getStringDetails(flagName, s, null);
        } else if (defaultValue instanceof Integer i) {
            evaluation = client.getIntegerDetails(flagName, i, null);
        } else if (defaultValue instanceof Double d) {
            evaluation = client.getDoubleDetails(flagName, d, null);
        } else if (defaultValue instanceof Boolean b) {
            evaluation = client.getBooleanDetails(flagName, b, null);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }

        assertEquals(expectedValue, evaluation.getValue());
        assertEquals(Reason.DEFAULT.toString(), evaluation.getReason());
    }

    @Test
    void getBooleanEvaluation_staticEvaluation_invalidVariantValue_shouldReturnDefaultValueWithErrorReason() {
        String flagName = "testFlag";
        Boolean defaultValue = false;
        EvaluationContext evaluationContext = new ImmutableContext();
        Flag flag = Flag.builder()
                        .name(flagName)
                        .state(Flag.State.ENABLED)
                        .defaultVariant("invalid")
                        .variants(Map.of("invalid", "not a boolean"))
                        .build();
        when(featureFlagQueryPort.findByFlagName(flagName)).thenReturn(Optional.of(flag));

        ProviderEvaluation<Boolean> evaluation = customProvider.getBooleanEvaluation(flagName, defaultValue, evaluationContext);

        assertEquals(defaultValue, evaluation.getValue());
        assertEquals(Reason.ERROR.toString(), evaluation.getReason());
        assertNull(evaluation.getVariant());
    }

    @Test
    void getBooleanEvaluation_dynamicEvaluation_targetingMatch_shouldReturnVariantValueWithTargetingReasonAndVariant() throws JsonProcessingException {
        String flagName = "testFlag";
        Boolean defaultValue = false;

        ImmutableContext evaluationContext = new ImmutableContext("user", Map.of("locale", new Value("br")));
        Flag flag = getDynamicFlag();
        when(featureFlagQueryPort.findByFlagName(flagName)).thenReturn(Optional.of(flag));

        ProviderEvaluation<Boolean> evaluation = customProvider.getBooleanEvaluation(flagName, defaultValue, evaluationContext);

        assertTrue(evaluation.getValue());
        assertEquals(Reason.TARGETING_MATCH.toString(), evaluation.getReason());
        assertEquals("true", evaluation.getVariant());
    }

    @Test
    void getBooleanEvaluation_dynamicEvaluation_noMatch_shouldReturnDefaultValue() throws JsonProcessingException {
        String flagName = "testFlag";
        Boolean defaultValue = false;

        ImmutableContext evaluationContext = new ImmutableContext("user", Map.of("locale", new Value("ar")));
        Flag flag = getDynamicFlag();
        when(featureFlagQueryPort.findByFlagName(flagName)).thenReturn(Optional.of(flag));

        ProviderEvaluation<Boolean> evaluation = customProvider.getBooleanEvaluation(flagName, defaultValue, evaluationContext);

        assertFalse(evaluation.getValue());
        assertEquals(Reason.DEFAULT.toString(), evaluation.getReason());
        assertEquals("false", evaluation.getVariant());
    }

    private Flag getBasicStaticFlag(String type) throws JsonProcessingException {
        String strFlag = """
                {
                  "id" : 1,
                  "name" : "basic-static",
                  "state" : "ENABLED",
                  "defaultVariant" : "2",
                  "variants" : {
                    "1" : ${default},
                    "2" : ${other}
                  },
                  "targeting" : "{}"
                }
                """;
        switch (type) {
            case "String" -> strFlag = strFlag.replace("${default}", "\"1\"")
                                                  .replace("${other}", "\"2\"");
            case "Integer" -> strFlag = strFlag.replace("${default}", "1")
                                                  .replace("${other}", "2");
            case "Double" -> strFlag = strFlag.replace("${default}", "1.0")
                                                  .replace("${other}", "2.0");
            default -> strFlag = strFlag.replace("${default}", "true")
                                        .replace("${other}", "false");
        }
        return mapper.readValue(strFlag, Flag.class);
    }

    private Flag getBasicBooleanFlag() throws JsonProcessingException {
        String strFlag = """
                {
                  "id" : 1,
                  "name" : "basic-boolean",
                  "state" : "ENABLED",
                  "defaultVariant" : "false",
                  "variants" : {
                    "true" : true,
                    "false" : false
                  },
                  "targeting" : "{ \\"if\\": [ { \\"in\\": [ \\"posts/2\\", { \\"var\\": \\"URI\\" } ] }, \\"CLOUD\\" ] }"
                }
                """;
        return mapper.readValue(strFlag, Flag.class);
    }

    private Flag getDynamicFlag() throws JsonProcessingException {
        String strFlag = """
                 {
                      "state": "ENABLED",
                      "defaultVariant": "false",
                      "variants": {
                        "true": true,
                        "false": false
                      },
                      "targeting": "{ \\"if\\": [ { \\"in\\": [ { \\"var\\": \\"locale\\" }, [ \\"br\\", \\"ca\\" ] ] }, \\"true\\" ] }"
                      }
                """;
        return mapper.readValue(strFlag, Flag.class);
    }
}
