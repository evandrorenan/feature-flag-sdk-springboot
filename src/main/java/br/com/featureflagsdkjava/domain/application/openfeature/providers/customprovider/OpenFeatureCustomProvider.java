package br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider;

import br.com.featureflagsdkjava.domain.model.Flag;
import br.com.featureflagsdkjava.domain.ports.FeatureFlagQueryPort;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
public class OpenFeatureCustomProvider implements FeatureProvider {

    private final FeatureFlagQueryPort featureFlagQueryPort;
    private final JsonLogic jsonLogic;

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
        T convert(Object value) throws OpenFeatureError;
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String flagName, Boolean defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Boolean.class::cast);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<String> getStringEvaluation(String flagName, String defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, String.class::cast);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Integer> getIntegerEvaluation(String flagName, Integer defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Integer.class::cast);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Double> getDoubleEvaluation(String flagName, Double defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Double.class::cast);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderEvaluation<Value> getObjectEvaluation(String flagName, Value defaultValue, EvaluationContext evaluationContext) {
        return evaluateFlag(flagName, defaultValue, evaluationContext, Value.class::cast);
    }

    private <T> ProviderEvaluation<T> evaluateFlag(String flagName, T defaultValue, EvaluationContext evaluationContext, ValueConverter<T> converter) {
        Flag flag = findValidFlag(flagName);
        return processFlag(flag, defaultValue, evaluationContext, converter);
    }

    private Flag findValidFlag(String flagName) {
        Optional<Flag> flag = featureFlagQueryPort.findByFlagName(flagName);
        validateFlag(flagName, flag.orElseThrow(() -> getFlagNotFoundError(flagName, " not found")));
        return flag.orElse(null);
    }

    private static FlagNotFoundError getFlagNotFoundError(String flagName, String message) {
        return new FlagNotFoundError("Flag " + flagName + message);
    }

    private void validateFlag(String flagName, Flag flag) {
        if (flag == null) throw getFlagNotFoundError(flagName, " is null");
        if (!flag.getState().equals(Flag.State.ENABLED)) throw getFlagNotFoundError(flagName, "is disabled");
        if (isDefaultVariantMissing(flag)) throw new TargetingKeyMissingError("Default variant is missing on flag " + flagName);
    }

    private boolean isDefaultVariantMissing(Flag flag) {
        return flag.getDefaultVariant() == null
                || flag.getVariants() == null
                || !flag.getVariants().containsKey(flag.getDefaultVariant());
    }

    private <T> ProviderEvaluation<T> processFlag(Flag flag, T defaultValue, EvaluationContext evaluationContext, ValueConverter<T> converter) {
        if (isStaticEvaluation(flag, evaluationContext)) {
            return staticEvaluation(flag, defaultValue, converter);
        }
        return dynamicEvaluation(flag, defaultValue, evaluationContext, converter);
    }

    private <T> ProviderEvaluation<T> dynamicEvaluation(Flag flag, T defaultValue, EvaluationContext evaluationContext, ValueConverter<T> converter) {
        try {
            Object result = jsonLogic.apply(flag.getTargeting(), evaluationContext.asObjectMap());
            if (result == null) return staticEvaluation(flag, defaultValue, converter);

            Object variantValue = flag.getVariants().get(String.valueOf(result));
            if (variantValue == null) {
                log.warn("Variant '{}' not found for flag '{}'", result, flag.getName());
                return buildErrorEvaluation(defaultValue);
            }

            return convertVariant(variantValue, converter)
                    .map(value -> buildTargetingEvaluation(value, String.valueOf(result)))
                    .orElse(buildErrorEvaluation(defaultValue));
        } catch (JsonLogicException e) {
            log.error("Error processing dynamic evaluation of flag: {}, {}", flag.getName(), e);
            return buildErrorEvaluation(defaultValue);
        }
    }

    private boolean isStaticEvaluation(Flag flag, EvaluationContext context) {
        return flag.getTargeting() == null
            || flag.getTargeting().isEmpty()
            || flag.getTargeting().equals("{}")
            || context == null
            || context.asMap().isEmpty();
    }

    private <T> ProviderEvaluation<T> staticEvaluation(Flag flag, T defaultValue, ValueConverter<T> converter) {
        if (flag.getVariants() == null || flag.getVariants().isEmpty()) return this.buildStaticEvaluation(defaultValue);

        Object defaultVariantValue = flag.getVariants().get(flag.getDefaultVariant());
        return convertVariant(defaultVariantValue, converter)
                .map(value -> buildDefaultVariation(flag.getDefaultVariant(), value))
                .orElse(buildErrorEvaluation(defaultValue));
    }

    private <T> Optional<T> convertVariant(Object variantValue, ValueConverter<T> converter) {
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

    private <T> ProviderEvaluation<T> buildDefaultVariation(String variant, T value) {
        return ProviderEvaluation.<T>builder()
                                 .reason(Reason.DEFAULT.toString())
                                 .value(value)
                                 .variant(variant)
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