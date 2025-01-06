package br.com.featureflagsdkjava.domain.application.openfeature.providers.customprovider;

import br.com.featureflagsdkjava.domain.model.Flag;
import br.com.featureflagsdkjava.domain.ports.FeatureFlagQueryPort;
import br.com.featureflagsdkjava.infra.adapters.http.RestFeatureFlagQueryAdapter;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import io.github.jamsesso.jsonlogic.JsonLogic;
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

    private <T> ProviderEvaluation<T> evaluateFlag(
            String flagName,
            T defaultValue,
            EvaluationContext evaluationContext,
            ValueConverter<T> converter
    ) {
        Optional<Flag> flag = findFlag(flagName);

        if (flag.isEmpty()) {
            log.error("Flag not found: {}", flagName);
            return buildErrorEvaluation(defaultValue);
        }

        if (flagIsDisabled(flag.get())) {
            log.error("Flag is disabled: {}", flagName);
            return buildErrorEvaluation(defaultValue);
        }

        return processFlag(flag.get(), defaultValue, evaluationContext, converter);
    }

    private <T> ProviderEvaluation<T> processFlag(
            Flag flag,
            T defaultValue,
            EvaluationContext evaluationContext,
            ValueConverter<T> converter
    ) {
        if (flasHasNoTargetingOrEvaluationHasNoContext(flag, evaluationContext)) {
            return staticEvaluation(flag, defaultValue, converter);
        }

        Map<String, String> context = mapEvaluationContext(evaluationContext);
        return targetEvaluation(flag, defaultValue, converter, context);
    }

    private static Map<String, String> mapEvaluationContext(EvaluationContext evaluationContext) {
        Map<String, String> context = new LinkedHashMap<>();
        evaluationContext.asMap().forEach((key, value) -> context.put(key, value.asString()));
        return context;
    }

    private <T> ProviderEvaluation<T> targetEvaluation(Flag flag, T defaultValue, ValueConverter<T> converter, Map<String, String> context) {
        try {
            Object objTarget = this.jsonLogic.apply(flag.getTargeting(), context);
            if (objTarget == null) {
                return handleNullTarget(flag, converter, context);
            }

            String variant = String.valueOf(objTarget);
            if (!flag.getVariants().containsKey(variant)) {
                log.error("Variant not found: {}", variant);
                return buildErrorEvaluation(defaultValue);
            }

            return convertVariant(flag.getVariants().get(variant), converter)
                    .map(value -> buildTargetingEvaluation(value, variant))
                    .orElse(buildErrorEvaluation(defaultValue));

        } catch (Exception e) {
            log.error("Error evaluating flag targeting: {}", flag.getFlagName(), e);
            return buildErrorEvaluation(defaultValue);
        }
    }

    private <T> ProviderEvaluation<T> handleNullTarget(Flag flag, ValueConverter<T> converter, Map<String, String> context) {
        log.warn("Target was evaluated to null on flag {}. Targeting: {}. Context: {}",
                flag.getFlagName(),
                flag.getTargeting(),
                context
        );
        T value = converter.convert(flag.getVariants().get(flag.getDefaultVariant()));
        return buildDefaultEvaluation(value);
    }

    private static boolean flasHasNoTargetingOrEvaluationHasNoContext(Flag flag, EvaluationContext evaluationContext) {
        return flag.getTargeting() == null
                || flag.getTargeting().isEmpty()
                || evaluationContext == null;
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

    private <T> ProviderEvaluation<T> buildDefaultEvaluation(T value) {
        return ProviderEvaluation.<T>builder()
                                 .reason(Reason.DEFAULT.toString())
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

    private static boolean flagIsDisabled(Flag flag) {
        return flag == null || flag.getState().equals(Flag.State.DISABLED);
    }

    private Optional<Flag> findFlag(String flagName) {
        Optional<Flag> flagOpt = featureFlagQueryPort.findByFlagName(flagName);
        if (flagOpt.isEmpty()) {
            log.info("Flag {} not found.", flagName);
            return Optional.empty();
        }

        if (!isFlagValid(flagName, flagOpt.get())) return Optional.empty();
        return flagOpt;
    }

    private boolean isFlagValid(String flagName, Flag flag) {
        if (flag == null) {
            log.warn("Flag {} is null", flagName);
            return false;
        }

        if (!flag.getState().equals(Flag.State.ENABLED)) {
            log.warn("Flag disabled: {}", flag.getFlagName());
            return false;
        }
        if (flag.getDefaultVariant() == null
        ||  flag.getVariants() == null
        ||  flag.getVariants().get(flag.getDefaultVariant()) == null) {
            log.warn("Flag default variant is missing: {}", flag.getFlagName());
            return false;
        }
        return true;
    }
}
