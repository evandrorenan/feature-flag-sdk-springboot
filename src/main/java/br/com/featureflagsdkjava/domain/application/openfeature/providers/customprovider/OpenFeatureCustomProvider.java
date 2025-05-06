package br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider;

import br.com.featureflagsdkjava.domain.model.Flag;
import br.com.featureflagsdkjava.domain.ports.FeatureFlagQueryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class OpenFeatureCustomProvider implements FeatureProvider {

    private final FeatureFlagQueryPort featureFlagQueryPort;
    private final JsonLogic jsonLogic;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public OpenFeatureCustomProvider(FeatureFlagQueryPort featureFlagService, JsonLogic jsonLogic) {
        this.featureFlagQueryPort = featureFlagService;
        this.jsonLogic = jsonLogic;
    }

    @Override
    public Metadata getMetadata() {
        return () -> "CustomProvider";
    }

    private interface ValueConverter<T> {
        T convert(String value) throws OpenFeatureError;
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String flagName, Boolean defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Boolean::valueOf);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<String> getStringEvaluation(String flagName, String defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, String::valueOf);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Integer> getIntegerEvaluation(String flagName, Integer defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Integer::valueOf);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Double> getDoubleEvaluation(String flagName, Double defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Double::valueOf);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Value> getObjectEvaluation(String flagName, Value defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Value::new);
    }

    private <T> ProviderEvaluation<T> evaluateFlag(String flagName, T defaultValue, EvaluationContext evaluationContext, ValueConverter<T> converter) {
        Optional<Flag> flag = findValidFlag(flagName);
        if (flag.isEmpty()) return buildErrorEvaluation(defaultValue);
        return processFlag(flag.get(), defaultValue, evaluationContext, converter);
    }

    private Optional<Flag> findValidFlag(String flagName) {
        Optional<Flag> flag = featureFlagQueryPort.findByFlagName(flagName);
        if (flag.isEmpty()) return logAndReturnEmpty(flagName, "not found");
        if (isInvalidFlag(flagName, flag.get())) return Optional.empty();
        return flag;
    }

    private Optional<Flag> logAndReturnEmpty(String flagName, String message) {
        log.warn("Flag {} is {}", flagName, message);
        return Optional.empty();
    }

    private boolean isInvalidFlag(String flagName, Flag flag) {
        if (flag == null) return logInvalidFlag(flagName, "null");
        if (!flag.getState().equals(Flag.State.ENABLED)) return logInvalidFlag(flagName, "disabled");
        if (isDefaultVariantMissing(flag)) return logInvalidFlag(flagName, "missing default variant");
        return false;
    }

    private boolean isDefaultVariantMissing(Flag flag) {
        return flag.getDefaultVariant() == null
                || flag.getVariants() == null
                || !flag.getVariants().containsKey(flag.getDefaultVariant());
    }

    private boolean logInvalidFlag(String flagName, String reason) {
        log.warn("Flag {} is invalid: {}", flagName, reason);
        return true;
    }

    private <T> ProviderEvaluation<T> processFlag(Flag flag, T defaultValue, EvaluationContext evaluationContext, ValueConverter<T> converter) {
        if (isStaticEvaluation(flag, evaluationContext)) {
            return staticEvaluation(flag, defaultValue, converter);
        }
        Map<String, String> context = extractContext(evaluationContext);
        return evaluateTargeting(flag, defaultValue, converter, context);
    }

    private boolean isStaticEvaluation(Flag flag, EvaluationContext context) {
        return flag.getTargeting() == null || flag.getTargeting().isEmpty() || context == null;
    }

    private Map<String, String> extractContext(EvaluationContext evaluationContext) {
        Map<String, String> context = new LinkedHashMap<>();
        evaluationContext.asMap().forEach((key, value) -> context.put(key, value.asString()));
        return context;
    }

    private <T> ProviderEvaluation<T> evaluateTargeting(Flag flag, T defaultValue, ValueConverter<T> converter, Map<String, String> context) {
        try {
            String variant = findMatchingVariant(flag, context);
            if (variant == null || !flag.getVariants().containsKey(variant)) {
                log.error("Variant not found or null: {}", variant);
                return buildErrorEvaluation(defaultValue);
            }
            return convertVariant(flag.getVariants().get(variant), converter)
                    .map(value -> buildTargetingEvaluation(value, variant))
                    .orElse(buildErrorEvaluation(defaultValue));
        } catch (Exception e) {
            log.error("Error evaluating targeting for flag {}", flag.getFlagName(), e);
            return buildErrorEvaluation(defaultValue);
        }
    }

    private String findMatchingVariant(Flag flag, Map<String, String> context) throws JsonLogicException, JsonProcessingException {
        Object result = jsonLogic.apply(flag.getTargeting(), context);
        if (result != null) return result.toString();
        for (String value : context.values()) {
            result = jsonLogic.apply(flag.getTargeting(), mapper.readValue(value, Map.class));
            if (result != null) return result.toString();
        }
        log.warn("Targeting evaluated to null for flag: {}", flag.getFlagName());
        return flag.getDefaultVariant();
    }

    private <T> ProviderEvaluation<T> staticEvaluation(Flag flag, T defaultValue, ValueConverter<T> converter) {
        return convertVariant(flag.getVariants().get(flag.getDefaultVariant()), converter)
                .map(this::buildStaticEvaluation)
                .orElse(buildErrorEvaluation(defaultValue));
    }

    private <T> Optional<T> convertVariant(String variantValue, ValueConverter<T> converter) {
        try {
            return Optional.ofNullable(converter.convert(variantValue));
        } catch (Exception e) {
            log.error("Error converting variant value: {}", variantValue, e);
            return Optional.empty();
        }
    }

    private <T> ProviderEvaluation<T> buildErrorEvaluation(T defaultValue) {
        return ProviderEvaluation.<T>builder()
                                 .reason(Reason.ERROR.toString())
                                 .value(defaultValue)
                                 .build();
    }

    private <T> ProviderEvaluation<T> buildStaticEvaluation(T value) {
        return ProviderEvaluation.<T>builder()
                                 .reason(Reason.STATIC.toString())
                                 .value(value)
                                 .build();
    }

    private <T> ProviderEvaluation<T> buildTargetingEvaluation(T value, String variant) {
        return ProviderEvaluation.<T>builder()
                                 .value(value)
                                 .variant(variant)
                                 .reason(Reason.TARGETING_MATCH.toString())
                                 .build();
    }
}