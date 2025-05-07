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
    public static <T> Hook<T> createHook() {
        return new Hook<T>() {
            @Override
            public Optional<EvaluationContext> before(HookContext ctx, Map hints) {
                log.debug("Starting flag evaluation.");
                log.debug(ctx.toString());
                log.debug("Hints: {}", hints);
                return Optional.empty();
            }

            @Override
            public void after(HookContext<T> ctx, FlagEvaluationDetails<T> details, Map<String, Object> hints) {
                log.info("Flag evaluated successfully.");
                log.info(ctx.toString());
                log.debug("Hints: {}", hints);
            }

            @Override
            public void error(HookContext ctx, Exception error, Map hints) {
                log.error("Flag evaluation failed. Exception: {}", error.getMessage());
            }

            @Override
            public void finallyAfter(HookContext ctx, Map hints) {
                log.debug("Finalizing flag evaluation.");
            }
        };
    }
}
