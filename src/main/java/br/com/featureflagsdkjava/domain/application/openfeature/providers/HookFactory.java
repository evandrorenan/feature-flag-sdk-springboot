package br.com.featureflagsdkjava.domain.application.openfeature.providers;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.Hook;
import dev.openfeature.sdk.HookContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HookFactory {

    public static final String STARTING_FLAG_EVALUATION = "Starting flag {} evaluation.";
    public static final String FLAG_EVALUATED_SUCCESSFULLY = "Flag {} evaluated successfully to {}.";
    public static final String FLAG_EVALUATION_FAILED_EXCEPTION = "Flag {} evaluation failed. Exception: {}";
    public static final String FINALIZING_FLAG_EVALUATION = "Finalizing flag {} evaluation.";

    public static <T> Hook<T> createHook() {
        return new Hook<T>() {
            @Override
            public Optional<EvaluationContext> before(HookContext ctx, Map hints) {
                log.info(STARTING_FLAG_EVALUATION, ctx == null ? "null" : ctx.getFlagKey());
                log.debug(formatHookContext(ctx));
                log.debug("Hints: {}", hints);
                return Optional.empty();
            }

            @Override
            public void after(HookContext<T> ctx, FlagEvaluationDetails<T> details, Map<String, Object> hints) {
                log.info(FLAG_EVALUATED_SUCCESSFULLY, details.getFlagKey(), details.getValue());
                log.debug(formatHookContext(ctx));
                log.debug("Hints: {}", hints);
            }

            @Override
            public void error(HookContext ctx, Exception error, Map hints) {
                log.error(FLAG_EVALUATION_FAILED_EXCEPTION, ctx == null ? "null" : ctx.getFlagKey(), error.getMessage());
                log.error(formatHookContext(ctx));
                log.error("Hints: {}", hints);
            }

            @Override
            public void finallyAfter(HookContext ctx, Map hints) {
                log.debug(FINALIZING_FLAG_EVALUATION, ctx == null ? "null" : ctx.getFlagKey());
            }
        };
    }

    private static String formatHookContext(HookContext ctx) {
        if (ctx == null) return "null";
        return String.format("HookContext{flagKey='%s', type='%s', defaultValue=%s, context=%s}",
                ctx.getFlagKey(), ctx.getType(), ctx.getDefaultValue(), ctx.getCtx().asMap());
    }
}
